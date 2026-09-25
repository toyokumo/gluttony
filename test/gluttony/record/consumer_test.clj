(ns gluttony.record.consumer-test
  (:require
   [clojure.core.async :as a]
   [clojure.test :refer :all]
   [gluttony.protocols :as p]
   [gluttony.record.consumer :as consumer :refer [new-consumer]]
   [gluttony.test-helper :refer [client test-client-fixture]]))

(use-fixtures :once test-client-fixture)

(deftest new-consumer-test
  (testing "Check pre condition work"
    (is (thrown? AssertionError
          (new-consumer {:queue-url ""
                         :consume (fn [m r r'])
                         :client client
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 10
                         :exceptional-poll-delay-ms 1000}))
        "queue-url must not be blank")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume "foo"
                         :client client
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 1
                         :consume-limit 0
                         :long-polling-duration 10
                         :exceptional-poll-delay-ms 1000}))
        "consume must be a function")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client {}
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 10
                         :exceptional-poll-delay-ms 1000}))
        "client must be a instance of ISqsClient")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :num-workers 0
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 10
                         :exceptional-poll-delay-ms 1000}))
        "num-workers must be a positive value")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :num-workers 1
                         :num-receivers 0
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 10
                         :exceptional-poll-delay-ms 1000}))
        "num-receivers must be a positive value")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 0
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 10
                         :exceptional-poll-delay-ms 1000}))
        "message-channel-size must be a positive value")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 11
                         :consume-limit 0
                         :long-polling-duration 10
                         :exceptional-poll-delay-ms 1000}))
        "receive-limit must be between zero and ten")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 1025
                         :long-polling-duration 10
                         :exceptional-poll-delay-ms 1000}))
        "consuem-limit must be between 0 and 1024")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 21
                         :exceptional-poll-delay-ms 1000}))
        "long-polling-duration must be between zero and twenty")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 20
                         :exceptional-poll-delay-ms 0}))
        "exceptional-poll-delay-ms must be a positive value")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 20
                         :exceptional-poll-delay-ms 0
                         :heartbeat 60
                         :visibility-timeout-in-heartbeat 61}))
        "heartbeat is set but heartbeat-timeout isn't set")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 20
                         :exceptional-poll-delay-ms 1000
                         :heartbeat 60
                         :heartbeat-timeout 10
                         :visibility-timeout-in-heartbeat 61}))
        "heartbeat is bigger than heartbeat-timeout")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 20
                         :exceptional-poll-delay-ms 1000
                         :heartbeat 60
                         :heartbeat-timeout 300}))
        "heartbeat is set but visibility-timeout-in-heartbeat isn't set")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 20
                         :exceptional-poll-delay-ms 1000
                         :heartbeat 60
                         :heartbeat-timeout 300
                         :visibility-timeout-in-heartbeat 59}))
        "heartbeat is bigger than visibility-timeout-in-heartbeat")))

(deftest heartbeat-test
  (testing "Heartbeat stops when the consumer is stopped"
    (let [visibility-change-count (atom 0)
          message-chan (a/chan)
          client (reify p/ISqsClient
                   (receive-message [_ _] (a/go {:messages nil :error nil}))
                   (delete-message [_ _] (a/go {:error nil}))
                   (change-message-visibility [_ _]
                     (swap! visibility-change-count inc)
                     (a/go {:error nil}))
                   (get-message-id [_ message] (str message))
                   (get-recipient-handle [_ message] (str message))
                   (stop [_]))]
      (#'consumer/heartbeat* {:client client
                              :queue-url "https://ap..."
                              :heartbeat 1
                              :heartbeat-timeout 60
                              :visibility-timeout-in-heartbeat 2
                              :message-chan message-chan}
                             (promise)
                             "message")
      (a/<!! (a/timeout 1500))
      (let [called @visibility-change-count]
        (is (pos? called)
            "heartbeat extends the visibility timeout while the message is not handled")
        ;; `-stop` closes the message-chan
        (a/close! message-chan)
        (a/<!! (a/timeout 1500))
        (is (= called @visibility-change-count)
            "heartbeat does not call the stopped client")))))
