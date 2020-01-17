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
                                :compute (fn [m r r'])
                                :client client
                                :given-client? true
                                :num-workers 1
                                :num-receivers 1
                                :message-channel-size 10
                                :receive-limit 10
                                :long-polling-duration 10
                                :exceptional-poll-delay-ms 1000})))
    (is (thrown? AssertionError
                 (new-consumer {:queue-url "https://ap..."
                                :compute "foo"
                                :client client
                                :given-client? true
                                :num-workers 1
                                :num-receivers 1
                                :message-channel-size 10
                                :receive-limit 10
                                :long-polling-duration 10
                                :exceptional-poll-delay-ms 1000})))
    (is (thrown? AssertionError
                 (new-consumer {:queue-url "https://ap..."
                                :compute (fn [_ _ _])
                                :client {}
                                :given-client? true
                                :num-workers 1
                                :num-receivers 1
                                :message-channel-size 10
                                :receive-limit 10
                                :long-polling-duration 10
                                :exceptional-poll-delay-ms 1000})))
    (is (thrown? AssertionError
                 (new-consumer {:queue-url "https://ap..."
                                :compute (fn [_ _ _])
                                :client client
                                :given-client? nil
                                :num-workers 1
                                :num-receivers 1
                                :message-channel-size 10
                                :receive-limit 10
                                :long-polling-duration 10
                                :exceptional-poll-delay-ms 1000})))
    (is (thrown? AssertionError
                 (new-consumer {:queue-url "https://ap..."
                                :compute (fn [_ _ _])
                                :client client
                                :given-client? true
                                :num-workers 0
                                :num-receivers 1
                                :message-channel-size 10
                                :receive-limit 10
                                :long-polling-duration 10
                                :exceptional-poll-delay-ms 1000})))
    (is (thrown? AssertionError
                 (new-consumer {:queue-url "https://ap..."
                                :compute (fn [_ _ _])
                                :client client
                                :given-client? true
                                :num-workers 1
                                :num-receivers 0
                                :message-channel-size 10
                                :receive-limit 10
                                :long-polling-duration 10
                                :exceptional-poll-delay-ms 1000})))
    (is (thrown? AssertionError
                 (new-consumer {:queue-url "https://ap..."
                                :compute (fn [_ _ _])
                                :client client
                                :given-client? true
                                :num-workers 1
                                :num-receivers 1
                                :message-channel-size 0
                                :receive-limit 10
                                :long-polling-duration 10
                                :exceptional-poll-delay-ms 1000})))
    (is (thrown? AssertionError
                 (new-consumer {:queue-url "https://ap..."
                                :compute (fn [_ _ _])
                                :client client
                                :given-client? true
                                :num-workers 1
                                :num-receivers 1
                                :message-channel-size 10
                                :receive-limit 11
                                :long-polling-duration 10
                                :exceptional-poll-delay-ms 1000})))
    (is (thrown? AssertionError
                 (new-consumer {:queue-url "https://ap..."
                                :compute (fn [_ _ _])
                                :client client
                                :given-client? true
                                :num-workers 1
                                :num-receivers 1
                                :message-channel-size 10
                                :receive-limit 10
                                :long-polling-duration 21
                                :exceptional-poll-delay-ms 1000})))
    (is (thrown? AssertionError
                 (new-consumer {:queue-url "https://ap..."
                                :compute (fn [_ _ _])
                                :client client
                                :given-client? true
                                :num-workers 1
                                :num-receivers 1
                                :message-channel-size 10
                                :receive-limit 10
                                :long-polling-duration 20
                                :exceptional-poll-delay-ms 0})))))
