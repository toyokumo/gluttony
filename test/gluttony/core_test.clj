(ns gluttony.core-test
  (:require
   [clojure.core.async :as a]
   [clojure.edn :as edn]
   [clojure.test :refer :all]
   [clojure.tools.logging :as log]
   [gluttony.core :refer :all]
   [gluttony.test-helper :as th])
  (:import
   (gluttony.record.consumer
    Consumer)
   (java.util
    UUID)
   (software.amazon.awssdk.services.sqs.model
    Message)))

(use-fixtures :each th/read-config-fixture th/test-client-fixture)

(use-fixtures :once th/start-logging-fixture)

(deftest start-consumer-test
  (testing "No option"
    (let [consume (fn [_ _ _])
          consumer (start-consumer "https://ap..." consume th/client)
          num-workers (max 1 (dec (.availableProcessors (Runtime/getRuntime))))]
      (is (instance? Consumer consumer))
      (is (= {:queue-url "https://ap..."
              :consume consume
              :client th/client
              :num-workers num-workers
              :num-receivers (max 1 (int (/ num-workers 10)))
              :message-channel-size (* 20 (max 1 (int (/ num-workers 10))))
              :receive-limit 10
              :consume-limit 0
              :long-polling-duration 20
              :exceptional-poll-delay-ms 10000
              :consume-chan nil
              :heartbeat nil
              :heartbeat-timeout nil
              :visibility-timeout-in-heartbeat nil}
             (dissoc consumer :message-chan :receiver-enabled)))
      (stop-consumer consumer)))

  (testing "Give some options"
    (let [consume (fn [_ _ _])
          consumer (start-consumer "https://ap..." consume th/client
                                   {:num-workers 2
                                    :num-receivers 1
                                    :message-channel-size 10
                                    :receive-limit 5
                                    :consume-limit 10
                                    :heartbeat 60
                                    :heartbeat-timeout 300})]
      (is (instance? Consumer consumer))
      (is (= {:queue-url "https://ap..."
              :consume consume
              :client th/client
              :num-workers 2
              :num-receivers 1
              :message-channel-size 10
              :receive-limit 5
              :consume-limit 10
              :long-polling-duration 20
              :exceptional-poll-delay-ms 10000
              :heartbeat 60
              :heartbeat-timeout 300
              :visibility-timeout-in-heartbeat 61}
             (dissoc consumer :message-chan :consume-chan :receiver-enabled)))
      (stop-consumer consumer))))

(deftest verify-work-of-receiver-and-worker
  (when (:queue-name th/config)
    (testing "cognitect-client"
      (let [queue-url (th/get-queue-url)]
        ;; Make queue empty
        (th/purge-queue queue-url)

        (testing "Gather every data in order"
          ;; Add test data
          (let [uuid (UUID/randomUUID)]
            (dotimes [i 20]
              (th/send-message {:QueueUrl queue-url
                                :MessageBody (pr-str {:id (inc i)})
                                :MessageDeduplicationId (str uuid ":" i)
                                :MessageGroupId (str uuid)})))

          (let [collected (atom [])
                consume (fn [message respond _]
                          (log/infof "start to consume:%s" (:body message))
                          (swap! collected
                                 conj (:id (edn/read-string (:body message))))
                          (respond))
                consumer (start-consumer queue-url consume th/client
                                         {:num-workers 1
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
              (th/send-message {:QueueUrl queue-url
                                :MessageBody (pr-str {:id (inc i)})
                                :MessageDeduplicationId (str uuid ":" i)
                                :MessageGroupId (str uuid)})))

          (let [collected (atom [])
                consume (fn [message respond _]
                          (log/infof "start to consume:%s" (:body message))
                          (swap! collected
                                 conj (:id (edn/read-string (:body message))))
                          (respond))
                consumer (start-consumer queue-url consume th/client
                                         {:num-workers 3
                                          :num-receivers 2
                                          :long-polling-duration 10})]
            (a/<!! (th/wait-chan (* 1000 45) (fn [] (>= (count @collected) 20))))
            (is (= (set (range 1 21))
                   (set @collected)))
            (stop-consumer consumer)))

        (testing "Heartbeat work"
          ;; Add test data
          (let [uuid (UUID/randomUUID)]
            (dotimes [i 1]
              (th/send-message {:QueueUrl queue-url
                                :MessageBody (pr-str {:id (inc i)})
                                :MessageDeduplicationId (str uuid ":" i)
                                :MessageGroupId (str uuid)})))

          (let [collected (atom [])
                consume (fn [message respond _]
                          (log/infof "start to consume:%s" (:body message))
                          (a/go
                            ;; wait 3 seconds
                            (a/<! (a/timeout 3000))
                            (swap! collected
                                   conj (:id (edn/read-string (:body message))))
                            (respond)))
                consumer (start-consumer queue-url consume th/client
                                         {:num-workers 1
                                          :num-receivers 1
                                          :long-polling-duration 10
                                          :heartbeat 1
                                          :heartbeat-timeout 5})]
            (a/<!! (th/wait-chan (* 1000 45) (fn [] (>= (count @collected) 1))))
            (is (= [1]
                   @collected))
            (stop-consumer consumer)))

        (testing "Check consume-limit"
          ;; Add test data
          (let [uuid (UUID/randomUUID)]
            (dotimes [i 3]
              (th/send-message {:QueueUrl queue-url
                                :MessageBody (pr-str {:id (inc i)})
                                :MessageDeduplicationId (str uuid ":" i)
                                :MessageGroupId (str uuid)})))

          (let [collected (atom [])
                consume (fn [message respond _]
                          (log/infof "start to consume:%s" (:body message))
                          (a/go
                            ;(is (instance? gluttony.record.message.SQSMessage message))
                            (swap! collected
                                   conj (:id (edn/read-string (:body message))))
                            (a/<! (a/timeout 10))           ; Make a point of park
                            (swap! collected
                                   conj Integer/MIN_VALUE)
                            (respond)
                            ;; Respond twice on purpose
                            (respond)))
                consumer (start-consumer queue-url consume th/client
                                         {:num-workers 3
                                          :num-receivers 1
                                          :long-polling-duration 10
                                          :consume-limit 1})]
            (a/<!! (th/wait-chan (* 1000 45) (fn [] (>= (count @collected) 6))))
            (is (= (set (range 1 4))
                   (set (keep-indexed (fn [i v]
                                        (when (even? i)
                                          v))
                                      @collected))))
            (is (= [Integer/MIN_VALUE Integer/MIN_VALUE Integer/MIN_VALUE]
                   (keep-indexed (fn [i v]
                                   (when (odd? i)
                                     v))
                                 @collected)))
            (stop-consumer consumer)))))

    (testing "aws-client"
      (let [queue-url (th/get-queue-url)]
        ;; Make queue empty
        (th/purge-queue queue-url)

        (testing "Gather every data in order"
          ;; Add test data
          (let [uuid (UUID/randomUUID)]
            (dotimes [i 20]
              (th/send-message {:QueueUrl queue-url
                                :MessageBody (pr-str {:id (inc i)})
                                :MessageDeduplicationId (str uuid ":" i)
                                :MessageGroupId (str uuid)})))

          (let [collected (atom [])
                consume (fn [^Message message respond _]
                          (log/infof "start to consume:%s" (.body message))
                          (swap! collected
                                 conj (:id (edn/read-string (.body message))))
                          (respond))
                consumer (start-consumer queue-url consume th/aws-client
                                         {:num-workers 1
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
              (th/send-message {:QueueUrl queue-url
                                :MessageBody (pr-str {:id (inc i)})
                                :MessageDeduplicationId (str uuid ":" i)
                                :MessageGroupId (str uuid)})))

          (let [collected (atom [])
                consume (fn [^Message message respond _]
                          (log/infof "start to consume:%s" (.body message))
                          (swap! collected
                                 conj (:id (edn/read-string (.body message))))
                          (respond))
                consumer (start-consumer queue-url consume th/aws-client
                                         {:num-workers 3
                                          :num-receivers 2
                                          :long-polling-duration 10})]
            (a/<!! (th/wait-chan (* 1000 45) (fn [] (>= (count @collected) 20))))
            (is (= (set (range 1 21))
                   (set @collected)))
            (stop-consumer consumer)))

        (testing "Heartbeat work"
          ;; Add test data
          (let [uuid (UUID/randomUUID)]
            (dotimes [i 1]
              (th/send-message {:QueueUrl queue-url
                                :MessageBody (pr-str {:id (inc i)})
                                :MessageDeduplicationId (str uuid ":" i)
                                :MessageGroupId (str uuid)})))

          (let [collected (atom [])
                consume (fn [^Message message respond _]
                          (log/infof "start to consume:%s" (.body message))
                          (a/go
                            ;; wait 3 seconds
                            (a/<! (a/timeout 3000))
                            (swap! collected
                                   conj (:id (edn/read-string (.body message))))
                            (respond)))
                consumer (start-consumer queue-url consume th/aws-client
                                         {:num-workers 1
                                          :num-receivers 1
                                          :long-polling-duration 10
                                          :heartbeat 1
                                          :heartbeat-timeout 5})]
            (a/<!! (th/wait-chan (* 1000 45) (fn [] (>= (count @collected) 1))))
            (is (= [1]
                   @collected))
            (stop-consumer consumer)))

        (testing "Check consume-limit"
          ;; Add test data
          (let [uuid (UUID/randomUUID)]
            (dotimes [i 3]
              (th/send-message {:QueueUrl queue-url
                                :MessageBody (pr-str {:id (inc i)})
                                :MessageDeduplicationId (str uuid ":" i)
                                :MessageGroupId (str uuid)})))

          (let [collected (atom [])
                consume (fn [^Message message respond _]
                          (log/infof "start to consume:%s" (.body message))
                          (a/go
                            ;(is (instance? gluttony.record.message.SQSMessage message))
                            (swap! collected
                                   conj (:id (edn/read-string (.body message))))
                            (a/<! (a/timeout 10))           ; Make a point of park
                            (swap! collected
                                   conj Integer/MIN_VALUE)
                            (respond)
                            ;; Respond twice on purpose
                            (respond)))
                consumer (start-consumer queue-url consume th/aws-client
                                         {:num-workers 3
                                          :num-receivers 1
                                          :long-polling-duration 10
                                          :consume-limit 1})]
            (a/<!! (th/wait-chan (* 1000 45) (fn [] (>= (count @collected) 6))))
            (is (= (set (range 1 4))
                   (set (keep-indexed (fn [i v]
                                        (when (even? i)
                                          v))
                                      @collected))))
            (is (= [Integer/MIN_VALUE Integer/MIN_VALUE Integer/MIN_VALUE]
                   (keep-indexed (fn [i v]
                                   (when (odd? i)
                                     v))
                                 @collected)))
            (stop-consumer consumer)))))))

(deftest disable-and-enable-receivers-test
  (when (:queue-name th/config)
    (let [queue-url (th/get-queue-url)]
      (log/debug queue-url)
      ;; Make queue empty
      (th/purge-queue queue-url)
      ;; wait for finishing long-polling in other tests
      (a/<!! (a/timeout 10000))
      (let [message-count (atom 0)
            consume (fn [message respond _]
                      (log/infof "start to consume:%s" (:body message))
                      (swap! message-count inc)
                      (respond))
            consumer (start-consumer queue-url consume th/client
                                     {:num-workers 1
                                      :num-receivers 1
                                      :long-polling-duration 1})
            send-message (fn []
                           (let [uuid (UUID/randomUUID)]
                             (th/send-message {:QueueUrl queue-url
                                               :MessageBody (pr-str {:id uuid})
                                               :MessageDeduplicationId (str uuid)
                                               :MessageGroupId (str uuid)})
                             (a/<!! (a/timeout 2000))))]
        (send-message)
        (a/<!! (th/wait-chan (* 1000) (fn [] (>= @message-count 1))))
        (is (= 1
               @message-count)
            "message received")
        (stop-receivers consumer)
        ;; wait for finishing long-polling
        (a/<!! (a/timeout 1000))
        (send-message)
        (is (= 1
               @message-count)
            "message not received")
        (start-receivers consumer)
        (a/<!! (th/wait-chan (* 1000) (fn [] (>= @message-count 2))))
        (is (= 2
               @message-count)
            "message received")
        (stop-consumer consumer)))))
