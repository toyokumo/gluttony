(ns gluttony.core-test
  (:require
   [clojure.test :refer :all]
   [gluttony.core :refer :all]
   [gluttony.test-helper :refer [client test-client-fixture]])
  (:import (gluttony.record.consumer Consumer)))

(use-fixtures :once test-client-fixture)

(deftest start-consumer-test
  (testing "No option"
    (let [compute (fn [_ _ _])
          consumer (with-redefs [cognitect.aws.client.api/client
                                 (constantly client)]
                     (start-consumer "https://ap..." compute))]
      (is (instance? Consumer consumer))
      (is (= {:queue-url "https://ap..."
              :compute compute
              :client client
              :given-client? false
              :num-workers (dec (.availableProcessors (Runtime/getRuntime)))
              :num-receivers 1
              :message-channel-size 20
              :receive-limit 10
              :long-polling-duration 20
              :exceptional-poll-delay-ms 10000}
             (dissoc consumer :message-chan)))
      (stop-consumer consumer)))

  (testing "Give some options"
    (let [compute (fn [_ _ _])
          consumer (start-consumer "https://ap..." compute
                                   {:client client
                                    :num-workers 2
                                    :num-receivers 1
                                    :message-channel-size 10
                                    :receive-limit 5})]
      (is (instance? Consumer consumer))
      (is (= {:queue-url "https://ap..."
              :compute compute
              :client client
              :given-client? true
              :num-workers 2
              :num-receivers 1
              :message-channel-size 10
              :receive-limit 5
              :long-polling-duration 20
              :exceptional-poll-delay-ms 10000}
             (dissoc consumer :message-chan)))
      (stop-consumer consumer))))
