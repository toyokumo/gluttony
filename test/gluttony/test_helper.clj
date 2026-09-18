(ns gluttony.test-helper
  (:require
   [aero.core :as aero]
   [clojure.core.async :as a]
   [clojure.java.io :as io]
   [cognitect.aws.client.api :as aws]
   [cognitect.aws.credentials :as credentials]
   [gluttony.record.aws-sqs-client :as aws-client]
   [gluttony.record.cognitect-sqs-client :as cognitect-client]
   [unilog.config :as unilog])
  (:import
   (java.net
    URI)
   (software.amazon.awssdk.auth.credentials
    AwsBasicCredentials
    StaticCredentialsProvider)
   (software.amazon.awssdk.regions
    Region)
   (software.amazon.awssdk.services.sqs
    SqsAsyncClient)))

(def config nil)

(def client nil)

(def aws-client nil)

(defn- create-cognitect-client
  []
  (let [{:keys [endpoint]} config]
    (cond-> {:api :sqs
             :region :us-east-1
             :credentials-provider (credentials/basic-credentials-provider {:access-key-id "test"
                                                                            :secret-access-key "test"})}
      endpoint (assoc :endpoint-override endpoint)
      true (aws/client))))

(defn- create-aws-client
  []
  (let [{:keys [endpoint]} config
        cp (StaticCredentialsProvider/create
            (AwsBasicCredentials/create "test" "test"))]
    (-> (SqsAsyncClient/builder)
        (.region Region/US_EAST_1)
        (.credentialsProvider cp)
        (.endpointOverride (URI/create (str (name (:protocol endpoint))
                                            "://"
                                            (:hostname endpoint)
                                            ":"
                                            (:port endpoint)
                                            (:path endpoint))))
        (.build))))

(defn test-client-fixture [f]
  (alter-var-root #'config
                  (constantly (some-> (io/resource "test-config.edn")
                                      (aero/read-config {:profile :dev}))))
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
