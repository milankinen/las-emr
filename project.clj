(defproject las-emr "0.1.0"
  :description "Finnish NLP text preprocessor pipeline with Spark and AWS EMR"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [gorillalabs/sparkling "2.0.0"]
                 [fi.seco/lexicalanalysis "1.5.4"]]
  :aot [#".*" sparkling.serialization sparkling.destructuring]
  :main las-emr.core
  :jar-name "las-emr.jar"
  :source-paths ["src/main"]
  :profiles {:provided
             {:dependencies
              [[org.apache.spark/spark-core_2.10 "2.1.0"]
               [org.apache.spark/spark-sql_2.10 "2.1.0"]]}
             :dev
             {:dependencies [[amazonica "0.3.95"]
                             [progrock "0.1.2"]]
              :source-paths ["src/main" "src/infra"]}}
  :aliases {"deploy-binaries" ["run" "-m" "las-emr.aws/deploy-binaries!" :project/version]})
