(ns las-emr.aws
  (:require [amazonica.aws.s3transfer :refer [upload]]
            [amazonica.aws.s3 :refer [delete-bucket does-object-exist]]
            [amazonica.aws.elasticmapreduce :refer [run-job-flow]]
            [clojure.walk :refer [postwalk]]
            [clojure.tools.cli :as cli]
            [progrock.core :as pr]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [cheshire.core :as json]
            [clojure.string :as string]))

(def jar-path
  "bin/las-emr.jar")

(defn- update-bar! [pbar {:keys [bytes-transferred]}]
  (-> (swap! pbar pr/tick bytes-transferred)
      (pr/print {:format "\r|:bar| :percent %"}))
  (flush))

(defn- upload! [bucket-name src-filename dest-key]
  (let [src-file (io/file src-filename)
        pbar (atom (pr/progress-bar (.length src-file)))
        upl (upload bucket-name dest-key src-file)]
    ((:add-progress-listener upl) (partial update-bar! pbar))
    ((:wait-for-completion upl))
    (pr/print (pr/done @pbar) {:format "\r|:bar| :percent %"})
    (flush)
    (= "Completed" ((:get-state upl)))))

(defn- run-flow! [flow-spec]
  (->> (reduce concat [] flow-spec)
       (apply run-job-flow)
       (:job-flow-id)))

(defn unfold-placeholders [spec bucket-name]
  (postwalk
    (fn [x] (if (string? x) (string/replace x "$BUCKET" bucket-name) x))
    spec))


; ====

(defn deploy-binaries! [& [version bucket-name]]
  (let [local-jar-path (str "target/las-emr-" version "-standalone.jar")]
    (when (nil? bucket-name)
      (println "Usage: ./bin/deploy.sh <bucket-name>")
      (System/exit 1))
    (when-not (.exists (io/file local-jar-path))
      (println "JAR binary not found. Build binary first by calling: ./bin/build.sh")
      (System/exit 1))
    (when-not (does-object-exist bucket-name jar-path)
      (println "Upload EMR binary to:" (str "s3://" bucket-name "/" jar-path))
      (upload! bucket-name local-jar-path jar-path))
    (println "\nDeploy complete!")
    (System/exit 0)))

(defn process-emr! [& args]
  (let [opts (cli/parse-opts args [["-b" "--bucket FILE" "Bucket where binaries are deployed (required)"
                                    :missing "Bucket name is required"]
                                   ["-i" "--input FILE" "Input text filename (required)"
                                    :missing "Input filename is required"]
                                   ["-l" "--lemmas FILE" "Lemmatized output filename (required)"
                                    :missing "Lemmatized output filename is required"]
                                   ["-t" "--tokens FILE" "Tokenized output filename (required)"
                                    :missing "Tokenized output filename is required"]
                                   ["-c" "--conf CONF" "Custom cluster configurations (.edn, will be merged to defaults)"]
                                   ["-h" "--help"]])]
    (when (:help (:options opts))
      (println "Tokenize and lemmatize the given input text file and store results into S3 bucket"
               "\n"
               "\nUsage: ./bin/process.sh [options]"
               "\n"
               "\nOptions (filenames must follow 's3n://...' notation):"
               (str "\n\n" (:summary opts))))
    (when (not-empty (:errors opts))
      (doseq [e (:errors opts)]
        (println e))
      (System/exit 1))
    (let [{:keys [input lemmas tokens conf bucket]} (:options opts)
          steps [{:name              "copy-las"
                  :action-on-failure "TERMINATE_JOB_FLOW"
                  :hadoop-jar-step   {:jar  "command-runner.jar"
                                      :args ["aws" "s3" "cp" "s3://$BUCKET/bin/las-emr.jar" "/tmp/las.jar"]}}
                 {:name              "run-las"
                  :action-on-failure "TERMINATE_JOB_FLOW"
                  :hadoop-jar-step   {:jar  "command-runner.jar"
                                      :args ["spark-submit"
                                             "--class" "las_emr.core"
                                             "/tmp/las.jar"
                                             "true"
                                             input tokens lemmas]}}]
          defaults (assoc (edn/read-string (slurp "defaults.edn")) :steps steps)
          flow-spec (merge defaults (if conf (edn/read-string (slurp conf)) {}))
          final-spec (unfold-placeholders flow-spec bucket)]
      (println "Using EMR flow configuration:"
               (str "\n" (json/generate-string final-spec {:pretty true}))
               "\nContinue? [Yn]: ")
      (when (re-matches #"^[yY]?$" (string/trim (read-line)))
        (->> (run-flow! final-spec)
             (println "EMR job flow started, id:")))
      (System/exit 0))))
