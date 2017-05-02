(ns las-emr.aws
  (:require [amazonica.aws.s3transfer :refer [upload]]
            [amazonica.aws.s3 :refer [delete-bucket does-object-exist]]
            [progrock.core :as pr]
            [clojure.java.io :as io]))

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


; ====

(defn deploy-binaries! [& [version bucket-name]]
  (when (nil? bucket-name)
    (println "Usage: ./bin/deploy.sh <bucket-name>")
    (System/exit 1))
  (when-not (.exists (io/file jar-path))
    (println "JAR binary not found. Build binary first by calling: ./bin/build.sh")
    (System/exit 1))
  (when-not (does-object-exist bucket-name jar-path)
    (println "Upload EMR binary to:" (str "s3://" bucket-name "/" jar-path))
    (upload! bucket-name (str "target/las-emr-" version "-standalone.jar") jar-path))
  (println "Deploy complete!")
  (System/exit 0))


