(ns gluttony.test-helper
  (:require
   [clojure.test :refer :all]
   [cognitect.aws.client.api :as aws]))

(def client nil)

(defn test-client-fixture [f]
  (alter-var-root #'client (constantly (aws/client {:api :sqs})))
  (f)
  (aws/stop client))
