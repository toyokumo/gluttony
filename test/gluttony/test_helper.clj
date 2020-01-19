(ns gluttony.test-helper
  (:require
   [clojure.core.async :as a]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer :all]
   [cognitect.aws.client.api :as aws]))

(def config nil)

(def client nil)

(defn read-config-fixture [f]
  (alter-var-root #'config
                  (constantly (some-> (io/file "dev-resources/config.edn")
                                      (slurp)
                                      (edn/read-string))))
  (f))

(defn test-client-fixture [f]
  (alter-var-root #'client
                  (constantly (cond-> {:api :sqs}
                                (:region config) (assoc :region (:region config))
                                true (aws/client))))
  (f)
  (aws/stop client))

(defn wait-chan
  [timeout-msec done?]
  (let [start (System/currentTimeMillis)]
    (a/go-loop []
      (when (and (< (- (System/currentTimeMillis) start) timeout-msec)
                 (not (done?)))
        (a/<! (a/timeout 100))
        (recur)))))
