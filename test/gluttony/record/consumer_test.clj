(ns gluttony.record.consumer-test
  (:require
   [clojure.test :refer :all]
   [gluttony.record.consumer :refer [new-consumer]]
   [gluttony.test-helper :refer [client test-client-fixture]]))

(use-fixtures :once test-client-fixture)

(deftest new-consumer-test
  (testing "Check pre condition work"
    (is (thrown? AssertionError
          (new-consumer {:queue-url ""
                         :consume (fn [m r r'])
                         :client client
                         :given-client? true
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
                         :given-client? true
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
                         :given-client? true
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 10
                         :exceptional-poll-delay-ms 1000}))
        "client must be a instance of cognitect.aws.client.Clinet")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :given-client? nil
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 10
                         :exceptional-poll-delay-ms 1000}))
        "given-client? must be a boolean value")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :given-client? true
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
                         :given-client? true
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
                         :given-client? true
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
                         :given-client? true
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
                         :given-client? true
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
                         :given-client? true
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
                         :given-client? true
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
                         :given-client? true
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
                         :given-client? true
                         :num-workers 1
                         :num-receivers 1
                         :message-channel-size 10
                         :receive-limit 10
                         :consume-limit 0
                         :long-polling-duration 20
                         :exceptional-poll-delay-ms 0
                         :heartbeat 60
                         :heartbeat-timeout 10
                         :visibility-timeout-in-heartbeat 61}))
        "heartbeat is bigger than heartbeat-timeout")
    (is (thrown? AssertionError
          (new-consumer {:queue-url "https://ap..."
                         :consume (fn [_ _ _])
                         :client client
                         :given-client? true
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
                         :given-client? true
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
