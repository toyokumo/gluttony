(ns gluttony.test-helper
  (:require
   [aero.core :as aero]
   [clojure.core.async :as a]
   [clojure.java.io :as io]
   [cognitect.aws.client.api :as aws]
   [gluttony.record.aws-sqs-client :as aws-client]
   [gluttony.record.cognitect-sqs-client :as cognitect-client]
   [unilog.config :as unilog])
  (:import
   (java.net
    URI)
   (software.amazon.awssdk.regions
    Region)
   (software.amazon.awssdk.services.sqs
    SqsAsyncClient)))

(def config nil)

(def client nil)

(def aws-client nil)

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

(defn- create-aws-client
  []
  (let [{:keys [region endpoint]} config]
    (cond-> (SqsAsyncClient/builder)
      region (.region (Region/of (name region)))
      endpoint (.endpointOverride (URI/create (str (name (:protocol endpoint))
                                                   "://"
                                                   (:hostname endpoint)
                                                   ":"
                                                   (:port endpoint)
                                                   (:path endpoint))))
      true (.build))))

(defn test-client-fixture [f]
  (let [cognitect-client (create-cognitect-client)
        aws-client (create-aws-client)]
    (alter-var-root #'client
                    (constantly (cognitect-client/make-client cognitect-client)))
    (alter-var-root #'aws-client
                    (constantly (aws-client/make-client aws-client)))
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
