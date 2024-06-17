(ns gluttony.record.aws-sqs-client
  (:require
   [clojure.core.async :as a]
   [gluttony.protocols :as p])
  (:import
   (java.util.function
    BiConsumer)
   (software.amazon.awssdk.services.sqs
    SqsAsyncClient)
   (software.amazon.awssdk.services.sqs.model
    ChangeMessageVisibilityRequest
    DeleteMessageRequest
    Message
    MessageSystemAttributeName
    ReceiveMessageRequest
    ReceiveMessageResponse)))

(defrecord AwsSqsClient [^SqsAsyncClient client
                         given-client?]
  p/ISqsClient
  (receive-message [_ {:keys [queue-url
                              max-number-of-messages
                              wait-time-seconds]}]
    (let [chan (a/chan 1)
          request (-> (ReceiveMessageRequest/builder)
                      (.queueUrl queue-url)
                      (.messageAttributeNames ["All"])
                      (.messageSystemAttributeNames [MessageSystemAttributeName/ALL])
                      (.maxNumberOfMessages (int max-number-of-messages))
                      (.waitTimeSeconds (int wait-time-seconds))
                      (.build))]
      (-> (.receiveMessage client request)
          (.whenComplete (reify BiConsumer
                           (accept [_ message-resp error]
                             (a/>!! chan (if message-resp
                                           {:messages (vec (.messages ^ReceiveMessageResponse message-resp))
                                            :error nil}
                                           {:message nil :error error}))
                             (a/close! chan)))))
      chan))
  (delete-message [_ {:keys [queue-url receipt-handle]}]
    (let [chan (a/chan 1)
          request (-> (DeleteMessageRequest/builder)
                      (.queueUrl queue-url)
                      (.receiptHandle receipt-handle)
                      (.build))]
      (-> (.deleteMessage client request)
          (.whenComplete (reify BiConsumer
                           (accept [_ _ error]
                             (a/>!! chan {:error error})
                             (a/close! chan)))))
      chan))
  (change-message-visibility [_ {:keys [queue-url receipt-handle visibility-timeout]}]
    (let [chan (a/chan 1)
          request (-> (ChangeMessageVisibilityRequest/builder)
                      (.queueUrl queue-url)
                      (.receiptHandle receipt-handle)
                      (.visibilityTimeout (int visibility-timeout))
                      (.build))]
      (-> (.changeMessageVisibility client request)
          (.whenComplete (reify BiConsumer
                           (accept [_ _ error]
                             (a/>!! chan {:error error})
                             (a/close! chan)))))
      chan))
  (get-message-id [_ message]
    (.messageId ^Message message))
  (get-recipient-handle [_ message]
    (.receiptHandle ^Message message))
  (stop [_]
    (when-not given-client?
      (.close client))))


(defn make-client
  ([]
   (make-client nil))
  ([api-client]
   {:pre [(or (instance? SqsAsyncClient api-client)
              (nil? api-client))]}
   (->AwsSqsClient (or api-client
                       (SqsAsyncClient/create))
                   (some? api-client))))
