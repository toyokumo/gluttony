(ns gluttony.record.cognitect-sqs-client
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [clojure.core.async :as a]
   [cognitect.aws.client.api :as aws]
   [gluttony.protocols :as p])
  (:import
   (cognitect.aws.client.impl
    Client)))

(defn- invoke-async
  [client op-map]
  (let [op-map (update op-map :request #(cske/transform-keys csk/->PascalCaseKeyword %))]
    (a/go
      (when-let [res (a/<! (aws/invoke-async client op-map))]
        (cske/transform-keys csk/->kebab-case-keyword res)))))

(defrecord CognitectSQSClient
  ;; AWS SQS client implementation using cognitect.aws.client.api
  ;; The structure of Output of functions are same as `(:response (:ReceiveMessage (aws/ops client)))`,
  ;; but the keys are kebab-case-keyword.
  [client given-client?]
  p/ISqsClient
  (receive-message [_ request]
    (a/go
      (let [request (merge {:message-attribute-names ["All"]
                            :message-system-attribute-names ["All"]}
                           request)
            response (a/<! (invoke-async client {:op :ReceiveMessage :request request}))]
        (if (contains? response :category)
          {:messages nil :error response}
          {:messages (:messages response) :error nil}))))
  (delete-message [_ request]
    (a/go
      (let [response (a/<! (invoke-async client {:op :DeleteMessage :request request}))]
        {:error (when (contains? response :category)
                  response)})))
  (change-message-visibility [_ request]
    (a/go
      (let [response (a/<! (invoke-async client {:op :ChangeMessageVisibility :request request}))]
        {:error (when (contains? response :category)
                  response)})))
  (get-message-id [_ message]
    (:message-id message))
  (get-recipient-handle [_ message]
    (:receipt-handle message))
  (stop [_]
    (when-not given-client?
      (aws/stop client))))

(defn make-client
  ([]
   (make-client nil))
  ([api-client]
   {:pre [(or (instance? Client api-client)
              (nil? api-client))]}
   (->CognitectSQSClient (or api-client
                             (aws/client {:api :sqs}))
                         (some? api-client))))
