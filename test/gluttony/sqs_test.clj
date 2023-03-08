(ns gluttony.sqs-test
  (:require
   [clojure.test :refer :all]
   [cognitect.aws.client.api :as aws]
   [gluttony.test-helper :refer [client test-client-fixture]]))

(use-fixtures :once test-client-fixture)

(deftest receive-message-test
  (testing "Assure aws-api schema doesn't change"
    (is (= {:name "ReceiveMessage"
            :request {:AttributeNames [:seq-of [:one-of ["All"
                                                         "Policy"
                                                         "VisibilityTimeout"
                                                         "MaximumMessageSize"
                                                         "MessageRetentionPeriod"
                                                         "ApproximateNumberOfMessages"
                                                         "ApproximateNumberOfMessagesNotVisible"
                                                         "CreatedTimestamp"
                                                         "LastModifiedTimestamp"
                                                         "QueueArn"
                                                         "ApproximateNumberOfMessagesDelayed"
                                                         "DelaySeconds"
                                                         "ReceiveMessageWaitTimeSeconds"
                                                         "RedrivePolicy"
                                                         "FifoQueue"
                                                         "ContentBasedDeduplication"
                                                         "KmsMasterKeyId"
                                                         "KmsDataKeyReusePeriodSeconds"
                                                         "DeduplicationScope"
                                                         "FifoThroughputLimit"
                                                         "RedriveAllowPolicy"
                                                         "SqsManagedSseEnabled"]]]
                      :MaxNumberOfMessages 'integer
                      :MessageAttributeNames [:seq-of 'string]
                      :QueueUrl 'string
                      :ReceiveRequestAttemptId 'string
                      :VisibilityTimeout 'integer
                      :WaitTimeSeconds 'integer}
            :required [:QueueUrl]
            :response {:Messages [:seq-of
                                  {:Attributes [:map-of
                                                [:one-of
                                                 ["SenderId"
                                                  "SentTimestamp"
                                                  "ApproximateReceiveCount"
                                                  "ApproximateFirstReceiveTimestamp"
                                                  "SequenceNumber"
                                                  "MessageDeduplicationId"
                                                  "MessageGroupId"
                                                  "AWSTraceHeader"]]
                                                'string]
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
                       :ReceiptHandle]}
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
                       :VisibilityTimeout]}
           (-> (aws/ops client)
               :ChangeMessageVisibility
               (dissoc :documentation))))))
