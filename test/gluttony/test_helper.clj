(ns gluttony.test-helper
  (:require
   [aero.core :as aero]
   [clojure.core.async :as a]
   [clojure.java.io :as io]
   [cognitect.aws.client.api :as aws]
   [gluttony.record.cognitect-sqs-client :as cognitect-client]
   [unilog.config :as unilog]))

(def config nil)

(def client nil)

(defn read-config-fixture [f]
  (alter-var-root #'config
                  (constantly (some-> (io/resource "test-config.edn")
                                      (aero/read-config {:profile :dev}))))
  (f))

(defn- create-cognitect-client
  []
  (let [{:keys [region endpoint]} config]
    (cond-> {:api :sqs}
      region (assoc :region region)
      endpoint (assoc :endpoint-override endpoint)
      true (aws/client))))

(defn test-client-fixture [f]
  (let [cognitect-client (create-cognitect-client)]
    (alter-var-root #'client
                    (constantly (cognitect-client/make-client cognitect-client)))
    (f)
    (aws/stop cognitect-client)))

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

(defn get-queue-url []
  (:QueueUrl (aws/invoke (:client client) {:op :GetQueueUrl :request {:QueueName (:queue-name config)}})))

(defn purge-queue [queue-url]
  (aws/invoke (:client client) {:op :PurgeQueue :request {:QueueUrl queue-url}}))

(defn send-message [request]
  (aws/invoke (:client client) {:op :SendMessage :request request}))
