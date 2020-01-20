(ns gluttony.core-test
  (:require
   [clojure.core.async :as a]
   [clojure.edn :as edn]
   [clojure.java.io :as io]
   [clojure.test :refer :all]
   [cognitect.aws.client.api :as aws]
   [gluttony.core :refer :all]
   [gluttony.test-helper :as th]
   [clojure.string :as str]
   [clojure.tools.logging :as log])
  (:import
   (gluttony.record.consumer Consumer)
   (java.util UUID)))

(use-fixtures :each th/read-config-fixture th/test-client-fixture)

(deftest start-consumer-test
  (testing "No option"
    (let [compute (fn [_ _ _])
          consumer (with-redefs [cognitect.aws.client.api/client
                                 (constantly th/client)]
                     (start-consumer "https://ap..." compute))
          num-workers (max 1 (dec (.availableProcessors (Runtime/getRuntime))))]
      (is (instance? Consumer consumer))
      (is (= {:queue-url "https://ap..."
              :compute compute
              :client th/client
              :given-client? false
              :num-workers num-workers
              :num-receivers (max 1 (int (/ num-workers 10)))
              :message-channel-size (* 20 (max 1 (int (/ num-workers 10))))
              :receive-limit 10
              :long-polling-duration 20
              :exceptional-poll-delay-ms 10000}
             (dissoc consumer :message-chan)))
      (stop-consumer consumer)))

  (testing "Give some options"
    (let [compute (fn [_ _ _])
          consumer (start-consumer "https://ap..." compute
                                   {:client th/client
                                    :num-workers 2
                                    :num-receivers 1
                                    :message-channel-size 10
                                    :receive-limit 5})]
      (is (instance? Consumer consumer))
      (is (= {:queue-url "https://ap..."
              :compute compute
              :client th/client
              :given-client? true
              :num-workers 2
              :num-receivers 1
              :message-channel-size 10
              :receive-limit 5
              :long-polling-duration 20
              :exceptional-poll-delay-ms 10000}
             (dissoc consumer :message-chan)))
      (stop-consumer consumer))))

(deftest verify-work-of-receiver-and-worker
  (when (:queue-name th/config)
    (let [req {:QueueName (:queue-name th/config)}
          queue-url (:QueueUrl (aws/invoke th/client {:op :GetQueueUrl :request req}))]
      (assert (str/starts-with? queue-url "https"))
      ;; Make queue empty
      (aws/invoke th/client {:op :PurgeQueue :request {:QueueUrl queue-url}})

      (testing "Gather every data in order"
        ;; Add test data
        (let [uuid (UUID/randomUUID)]
          (dotimes [i 20]
            (aws/invoke th/client {:op :SendMessage
                                   :request {:QueueUrl queue-url
                                             :MessageBody (pr-str {:id (inc i)})
                                             :MessageDeduplicationId (str uuid ":" i)
                                             :MessageGroupId (str uuid)}})))

        (let [collected (atom [])
              compute (fn [message respond _]
                        (log/info (:body message))
                        (is (instance? gluttony.record.message.SQSMessage message))
                        (swap! collected
                               conj (:id (edn/read-string (:body message))))
                        (respond))
              consumer (start-consumer queue-url compute
                                       {:client th/client
                                        :num-workers 1
                                        :num-receivers 1
                                        :long-polling-duration 10})]
          (a/<!! (th/wait-chan (* 1000 45) (fn [] (>= (count @collected) 20))))
          (is (= (vec (range 1 21))
                 @collected))
          (stop-consumer consumer)))

      (testing "Concurrent gathering"
        ;; Add test data
        (let [uuid (UUID/randomUUID)]
          (dotimes [i 20]
            (aws/invoke th/client {:op :SendMessage
                                   :request {:QueueUrl queue-url
                                             :MessageBody (pr-str {:id (inc i)})
                                             :MessageDeduplicationId (str uuid ":" i)
                                             :MessageGroupId (str uuid)}})))

        (let [collected (atom [])
              compute (fn [message respond _]
                        (log/info (:body message))
                        (swap! collected
                               conj (:id (edn/read-string (:body message))))
                        (respond))
              consumer (start-consumer queue-url compute
                                       {:client th/client
                                        :num-workers 3
                                        :num-receivers 2
                                        :long-polling-duration 10})]
          (a/<!! (th/wait-chan (* 1000 45) (fn [] (>= (count @collected) 20))))
          (is (= (set (range 1 21))
                 (set @collected)))
          (stop-consumer consumer))))))
