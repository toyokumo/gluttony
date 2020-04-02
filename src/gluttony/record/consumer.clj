(ns gluttony.record.consumer
  (:require
   [clojure.core.async :as a]
   [clojure.core.async.impl.protocols :as a.i.p]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [cognitect.aws.client.api :as aws]
   [gluttony.protocols :as p]
   [gluttony.record.message :as r.msg]
   [gluttony.sqs :as sqs])
  (:import
   (cognitect.aws.client Client)))

(defn- start-receivers
  [{:keys [client
           queue-url
           num-receivers
           receive-limit
           long-polling-duration
           exceptional-poll-delay-ms
           message-chan]}]
  (let [req {:queue-url queue-url
             :attribute-names ["All"]
             :message-attribute-names ["All"]
             :max-number-of-messages receive-limit
             :wait-time-seconds long-polling-duration}]
    (dotimes [i num-receivers]
      (a/go-loop []
        (let [res (a/<! (sqs/receive-message client req))]
          (log/debugf "receiver %s receives %s" i res)
          (cond
            (empty? res)
            nil

            (= (set (keys res)) #{:messages})
            (let [messages (->> res
                                :messages
                                (map r.msg/map->SQSMessage))]
              (when (seq messages)
                (a/<! (a/onto-chan message-chan messages false))))

            :else
            (a/<! (a/timeout exceptional-poll-delay-ms)))
          (when-not (a.i.p/closed? message-chan)
            (recur)))))))

(defn- respond*
  [{:keys [client queue-url]} p message]
  (deliver p :respond)
  (sqs/delete-message client {:queue-url queue-url
                              :receipt-handle (:receipt-handle message)}))

(defn- raise*
  [{:keys [client queue-url]} p message & [retry-delay]]
  (deliver p :raise)
  (let [retry-delay (or retry-delay 0)]
    (sqs/change-message-visibility client {:queue-url queue-url
                                           :receipt-handle (:receipt-handle message)
                                           :visibility-timeout retry-delay})))

(defn- heartbeat*
  "When heartbeat parameter is set, a heartbeat process start after the first heartbeat"
  [{:keys [client queue-url heartbeat heartbeat-timeout]} p message]
  (when heartbeat
    (let [heartbeat-msecs (* heartbeat 1000)
          start (System/currentTimeMillis)]
      (a/go
        (a/<! (a/timeout heartbeat-msecs))
        (loop []
          (when (and (not (realized? p))
                     (< (long (/ (- (System/currentTimeMillis) start) 1000)) heartbeat-timeout))
            (log/debugf "message-id:%s heartbeat" (:message-id message))
            (sqs/change-message-visibility client {:queue-url queue-url
                                                   :receipt-handle (:receipt-handle message)
                                                   :visibility-timeout (inc heartbeat)})
            (a/<! (a/timeout heartbeat-msecs))
            (recur)))))))

(defn- start-workers
  [{:as consumer :keys [consume
                        num-workers
                        message-chan]}]
  (dotimes [i num-workers]
    (a/go-loop []
      (when-let [message (a/<! message-chan)]
        (log/debugf "worker %s takes %s" i message)
        (let [p (promise)
              respond (partial respond* consumer p message)
              raise (partial raise* consumer p message)]
          (try
            (heartbeat* consumer p message)
            (consume message respond raise)
            (catch Throwable _
              (raise))))
        (recur)))))

(defrecord Consumer
  [queue-url
   consume
   client
   given-client?
   num-workers
   num-receivers
   message-channel-size
   receive-limit
   long-polling-duration
   exceptional-poll-delay-ms
   message-chan
   heartbeat
   heartbeat-timeout]
  p/IConsumer
  (-start [this]
    (let [this (assoc this :message-chan (a/chan message-channel-size))]
      (start-workers this)
      (start-receivers this)
      this))
  (-stop [_]
    (when message-chan
      (a/close! message-chan))
    (when (and client (not given-client?))
      (aws/stop client))))

(defn new-consumer
  [{:as m :keys [queue-url
                 consume
                 client
                 given-client?
                 num-workers
                 num-receivers
                 message-channel-size
                 receive-limit
                 long-polling-duration
                 exceptional-poll-delay-ms
                 heartbeat
                 heartbeat-timeout]}]
  {:pre [(not (str/blank? queue-url))
         (ifn? consume)
         (instance? Client client)
         (boolean? given-client?)
         (pos? num-workers)
         (pos? num-receivers)
         (pos? message-channel-size)
         (<= 1 receive-limit 10)
         (<= 0 long-polling-duration 20)
         (pos? exceptional-poll-delay-ms)
         (or (= nil heartbeat heartbeat-timeout)
             (and (integer? heartbeat) (integer? heartbeat-timeout)
                  (<= 1 heartbeat 43199) (<= 2 heartbeat-timeout 43200)
                  (< heartbeat heartbeat-timeout)))]}
  (map->Consumer m))
