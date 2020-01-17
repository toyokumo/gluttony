(ns gluttony.sqs-test
  (:require
   [cognitect.aws.client.api :as aws]
   [clojure.test :refer :all]
   [gluttony.sqs :refer [get-queue-url]]
   [gluttony.test-helper :refer [client test-client-fixture]]))

(use-fixtures :once test-client-fixture)

(deftest get-queue-url-test
  (testing "Assure aws-api schema doesn't change"
    (is (= {:name "GetQueueUrl"
            :request {:QueueName 'string
                      :QueueOwnerAWSAccountId 'string}
            :required [:QueueName]
            :response {:QueueUrl 'string}}
           (-> (aws/ops client)
               :GetQueueUrl
               (dissoc :documentation))))))

(deftest receive-message-test
  (testing "Assure aws-api schema doesn't change"
    (is (= {:name "ReceiveMessage"
            :request {:AttributeNames [:seq-of 'string]
                      :MaxNumberOfMessages 'integer
                      :MessageAttributeNames [:seq-of 'string]
                      :QueueUrl 'string
                      :ReceiveRequestAttemptId 'string
                      :VisibilityTimeout 'integer
                      :WaitTimeSeconds 'integer}
            :required [:QueueUrl]
            :response {:Messages [:seq-of
                                  {:Attributes [:map-of 'string 'string]
                                   :Body 'string
                                   :MD5OfBody 'string
                                   :MD5OfMessageAttributes 'string
                                   :MessageAttributes [:map-of 'string
                                                       {:BinaryListValues [:seq-of 'blob]
                                                        :BinaryValue 'blob
                                                        :DataType 'string
                                                        :StringListValues [:seq-of 'string]
                                                        :StringValue 'string}]
                                   :MessageId 'string
                                   :ReceiptHandle 'string}]}}
           (-> (aws/ops client)
               :ReceiveMessage
               (dissoc :documentation))))))

(deftest delete-message-test
  (testing "Assure aws-api schema doesn't change"
    (is (= {:name "DeleteMessage"
            :request {:QueueUrl 'string
                      :ReceiptHandle 'string}
            :required [:QueueUrl
                       :ReceiptHandle]
            :response nil}
           (-> (aws/ops client)
               :DeleteMessage
               (dissoc :documentation))))))

(deftest change-message-visibility-test
  (testing "Assure aws-api schema doesn't change"
    (is (= {:name "ChangeMessageVisibility"
            :request {:QueueUrl 'string
                      :ReceiptHandle 'string
                      :VisibilityTimeout 'integer}
            :required [:QueueUrl
                       :ReceiptHandle
                       :VisibilityTimeout]
            :response nil}
           (-> (aws/ops client)
               :ChangeMessageVisibility
               (dissoc :documentation))))))
