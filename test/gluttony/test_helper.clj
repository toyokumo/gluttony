(ns gluttony.test-helper
  (:require
   [aero.core :as aero]
   [clojure.core.async :as a]
   [clojure.java.io :as io]
   [cognitect.aws.client.api :as aws]
   [unilog.config :as unilog]))

(def config nil)

(def client nil)

(defn read-config-fixture [f]
  (alter-var-root #'config
                  (constantly (some-> (io/resource "test-config.edn")
                                      (aero/read-config {:profile :dev}))))
  (f))

(defn test-client-fixture [f]
  (let [{:keys [region endpoint]} config]
    (alter-var-root #'client
                    (constantly (cond-> {:api :sqs}
                                  region (assoc :region region)
                                  endpoint (assoc :endpoint-override endpoint)
                                  true (aws/client))))
    (f)
    (aws/stop client)))

(defn start-logging-fixture [f]
  (unilog/start-logging! {:level :debug
                          :overrides {"org.eclipse.jetty" :info}})
  (f))

(defn wait-chan
  [timeout-msec done?]
  (let [start (System/currentTimeMillis)]
    (a/go-loop []
      (when (and (< (- (System/currentTimeMillis) start) timeout-msec)
                 (not (done?)))
        (a/<! (a/timeout 100))
        (recur)))))
