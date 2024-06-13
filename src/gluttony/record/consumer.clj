(ns gluttony.record.consumer
  (:require
   [clojure.core.async :as a]
   [clojure.core.async.impl.protocols :as a.i.p]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [gluttony.protocols :as p])
  (:import
   (gluttony.protocols
    ISqsClient)))

(defn- start-receivers
  [{:keys [client
           queue-url
           num-receivers
           receive-limit
           long-polling-duration
           exceptional-poll-delay-ms
           message-chan
           receiver-enabled]}]
  (let [req {:queue-url queue-url
             :max-number-of-messages receive-limit
             :wait-time-seconds long-polling-duration}]
    (dotimes [i num-receivers]
      (a/go-loop []
        (if @receiver-enabled
          (let [{:keys [messages error] :as res} (a/<! (p/receive-message client req))]
            (log/debugf "receiver %s receives %s" i res)
            (cond
              messages
              (when (seq messages)
                (a/<! (a/onto-chan! message-chan messages false)))

              error
              (a/<! (a/timeout exceptional-poll-delay-ms)))
            (when-not (a.i.p/closed? message-chan)
              (recur)))
          (do (a/<! (a/timeout 1000))
              (recur)))))))

(defn- respond*
  [{:keys [client queue-url consume-chan]} p message]
  (let [already-realized? (realized? p)]
    (deliver p :respond)
    (a/go
      (when (and consume-chan
                 (not already-realized?))
        ;; takes a sign and make a space in which next consume can work
        (a/<! consume-chan)
        (log/debugf "takes a sign of message-id:%s" (p/get-message-id client message)))
      (a/<! (p/delete-message client {:queue-url queue-url
                                      :receipt-handle (p/get-recipient-handle client message)})))))

(defn- raise*
  [{:keys [client queue-url consume-chan]} p message & [retry-delay]]
  (let [already-realized? (realized? p)]
    (deliver p :raise)
    (a/go
      (when (and consume-chan
                 (not already-realized?))
        ;; takes a sign and make a space in which next consume can work
        (a/<! consume-chan)
        (log/debugf "takes a sign of message-id:%s" (p/get-message-id client message)))
      (let [retry-delay (or retry-delay 0)]
        (a/<! (p/change-message-visibility client {:queue-url queue-url
                                                   :receipt-handle (p/get-recipient-handle client message)
                                                   :visibility-timeout retry-delay}))))))

(defn- heartbeat*
  "When heartbeat parameter is set, a heartbeat process start after the first heartbeat"
  [{:keys [client queue-url heartbeat heartbeat-timeout visibility-timeout-in-heartbeat]} p message]
  (when heartbeat
    (let [heartbeat-msecs (* heartbeat 1000)
          start (System/currentTimeMillis)]
      (a/go
        (a/<! (a/timeout heartbeat-msecs))
        (loop []
          (when (and (not (realized? p))
                     (< (long (/ (- (System/currentTimeMillis) start) 1000)) heartbeat-timeout))
            (log/debugf "message-id:%s heartbeat" (p/get-message-id client message))
            (p/change-message-visibility client {:queue-url queue-url
                                                 :receipt-handle (p/get-recipient-handle client message)
                                                 :visibility-timeout visibility-timeout-in-heartbeat})
            (a/<! (a/timeout heartbeat-msecs))
            (recur)))))))

(defn- start-workers
  [{:as consumer :keys [consume
                        num-workers
                        message-chan
                        consume-chan]}]
  (dotimes [i num-workers]
    (a/go-loop []
      (when consume-chan
        ;; puts a sign which show a worker is now processing a message
        (a/>! consume-chan :consuming))
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
   num-workers
   num-receivers
   message-channel-size
   receive-limit
   consume-limit
   long-polling-duration
   exceptional-poll-delay-ms
   message-chan
   consume-chan
   heartbeat
   heartbeat-timeout
   visibility-timeout-in-heartbeat
   receiver-enabled]
  p/IConsumer
  (-start [this]
    (let [this (assoc this
                      :message-chan (a/chan message-channel-size)
                      :consume-chan (when (pos? consume-limit)
                                      (a/chan consume-limit)))]
      (start-workers this)
      (start-receivers this)
      this))
  (-stop [_]
    (when message-chan
      (a/close! message-chan))
    (when consume-chan
      (a/close! consume-chan))
    (when client
      (p/stop client)))
  (-enable-receivers [this]
    (update this :receiver-enabled (fn [atm]
                                     (reset! atm true)
                                     atm)))
  (-disable-receivers [this]
    (update this :receiver-enabled (fn [atm]
                                     (reset! atm false)
                                     atm))))

(defn new-consumer
  [{:as m :keys [queue-url
                 consume
                 client
                 num-workers
                 num-receivers
                 message-channel-size
                 receive-limit
                 consume-limit
                 long-polling-duration
                 exceptional-poll-delay-ms
                 heartbeat
                 heartbeat-timeout
                 visibility-timeout-in-heartbeat]}]
  {:pre [(not (str/blank? queue-url))
         (ifn? consume)
         (instance? ISqsClient client)
         (pos? num-workers)
         (pos? num-receivers)
         (pos? message-channel-size)
         (<= 1 receive-limit 10)
         (<= 0 consume-limit 1024)
         (<= 0 long-polling-duration 20)
         (pos? exceptional-poll-delay-ms)
         (or (= nil heartbeat heartbeat-timeout)
             (and (integer? heartbeat) (integer? heartbeat-timeout)
                  (<= 1 heartbeat 43199) (<= 2 heartbeat-timeout 43200)
                  (< heartbeat heartbeat-timeout)))
         (or (nil? heartbeat)
             (and (integer? visibility-timeout-in-heartbeat)
                  (< heartbeat visibility-timeout-in-heartbeat)))]}
  (map->Consumer m))
