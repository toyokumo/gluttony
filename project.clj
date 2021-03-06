(defproject toyokumo/gluttony "0.3.3-SNAPSHOT"
  :description "A consumer library using core.async and aws-api based on AWS SQS"
  :url "https://github.com/toyokumo/gluttony"
  :license {:name "Apache, Version 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}
  :deploy-repositories [["releases" {:url "https://repo.clojars.org" :creds :gpg}]
                        ["snapshots" :clojars]]
  :plugins [[lein-ancient "0.6.15"]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/core.async "1.3.610"]
                 [org.clojure/tools.logging "1.1.0"]
                 [camel-snake-kebab "0.4.2"]
                 [com.cognitect.aws/api "0.8.484"]
                 [com.cognitect.aws/endpoints "1.1.11.926"]
                 [com.cognitect.aws/sqs "810.2.817.0"]]
  :repl-options {:init-ns gluttony.core}
  :profiles {:dev {:dependencies [[aero "1.1.6"]
                                  [spootnik/unilog "0.7.27"]]}})
