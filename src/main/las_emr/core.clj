(ns las-emr.core
  (:require [sparkling.conf :as c]
            [sparkling.core :as s]
            [clojure.string :as string]
            [las-emr.text :refer [process]])
  (:gen-class))


(defn ->config [local?]
  (let [conf (c/app-name (c/spark-conf) "fin-nlp-preprocessor")]
    (if local?
      (-> conf
          (c/master "local[4]")
          (c/set "spark.executor.memory", "2g")))
    conf))

(defn -main [& [remote? input-file tokenized-file lemmatized-file]]
  (println "Run arguments"
           "\n  Remote:" remote?
           "\n  Input file:" input-file
           "\n  Tokenized output:" tokenized-file
           "\n  Lemmatized output:" lemmatized-file)
  (s/with-context sc (->config (not= "true" (str remote?)))
    (let [processed (->> (s/text-file sc input-file)
                         (s/filter (fn [line] (not-empty (string/trim line))))
                         (s/map process)
                         (s/cache))]
      (->> processed
           (s/map (fn [[ok? tokens]] (str (if ok? "ok" "nok") "\t" (string/join " " (map :baseform tokens)))))
           (s/save-as-text-file lemmatized-file))
      (->> processed
           (s/map (fn [[ok? tokens]] (str (if ok? "ok" "nok") "\t" (string/join " " (map :wordform tokens)))))
           (s/save-as-text-file tokenized-file)))))

