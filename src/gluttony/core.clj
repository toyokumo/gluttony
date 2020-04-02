(ns gluttony.core
  (:require
   [cognitect.aws.client.api :as aws]
   [gluttony.record.consumer :as c]
   [gluttony.protocols :as p])
  (:import
   (gluttony.record.consumer Consumer)))

(defn start-consumer
  "Creates a consumer and run it.

   The consumer mainly run two type of processes based on core.async/go-loop.

   1. Receiver
   Receiver receives messages from AWS SQS and push them into a local buffer, core.async/channel,
   as quickly as possible up to the configured buffer size.

   2. Worker
   Worker takes a message which the receivers push and invokes the consume function which you pass
   as the argument.

   The client MUST call stop-consumer when you no longer want to process messages.

   Input:
    queue-url - the url of an AWS SQS queue used as the producer.
    consume - consume is a function that takes three arguments:
              1. a 'message' is a record of gluttony.record.message/SQSMessage which contains the
              body of the sqs message.
              2. a function when it MUST be invoked when the consume success. it takes no arguments.
              3. a function when it MUST be invoked when the consume fail. it takes zero or one
              argument. the argument decides how long to delay in seconds for retrying. 0 to 43200.
              Maximum: 12 hours. when you pass no delay time, retrying as soon as possible.
              like this:
              (defn consume [message respond raise]
                (let [success? (...your computation uses the message...)]
                  (if success?
                    (respond)
                    (raise 10))))

   Optional arguments:
    :client                    - the SQS client, which is the instance of cognitect.aws.client.Client.
                                 if missing, cognitect.aws.client.api/client would be called.
    :num-workers               - the number of workers processing messages concurrently.
                                 default: (Runtime/availableProcessors) - 1
    :num-receivers             - the number of receivers polling from sqs.
                                 default: (num-workers / 10) because each receiver is able to receive
                                          up to 10 messages at a time.
    :message-channel-size      - the number of messages to prefetch from sqs.
                                 default: 20 * num-receivers
    :receive-limit             - the number of messages to receive at a time. 1 to 10.
                                 default: 10
    :long-polling-duration     - the duration (in seconds) for which the call waits for a message to
                                 arrive in the queue before returning. 0 to 20.
                                 default: 20
    :exceptional-poll-delay-ms - when an Exception is received while polling, receiver wait for the
                                 number of ms until polling again.
                                 default: 10000 (10 seconds).
    :heartbeat                 - the duration (in seconds) for which the consumer extends message
                                 visibility if the message is being processed. 1 to 43199.
                                 default: nil
                                 If it isn't set, heartbeat doesn't work.
                                 If it's set, :heartbeat-timeout is required.
                                 Refer to AWS documents for more detail: https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/working-with-messages.html
    :heartbeat-timeout         - the timeout (in seconds) of heartbeat.
                                 If your consume function doesn't call respond or raise within heartbeat
                                 timeout, the consumer doesn't extend message visibility any more.
                                 2 to 43200.
                                 default: nil
                                 :heartbeat-timeout must be longer than :heartbeat.
   Output:
    a instance of gluttony.record.consumer.Consumer"
  ^Consumer [queue-url consume & [opts]]
  (let [client (or (:client opts)
                   (aws/client {:api :sqs}))
        given-client? (some? (:client opts))
        num-workers (or (:num-workers opts)
                        (max 1 (dec (.availableProcessors (Runtime/getRuntime)))))
        num-receivers (or (:num-receivers opts)
                          (max 1 (int (/ num-workers 10))))
        message-channel-size (or (:message-channel-size opts)
                                 (* 20 num-receivers))
        receive-limit (or (:receive-limit opts)
                          10)
        long-polling-duration (or (:long-polling-duration opts)
                                  20)
        exceptional-poll-delay-ms (or (:exceptional-poll-delay-ms opts)
                                      10000)
        consumer (c/new-consumer {:queue-url queue-url
                                  :consume consume
                                  :client client
                                  :given-client? given-client?
                                  :num-workers num-workers
                                  :num-receivers num-receivers
                                  :message-channel-size message-channel-size
                                  :receive-limit receive-limit
                                  :long-polling-duration long-polling-duration
                                  :exceptional-poll-delay-ms exceptional-poll-delay-ms
                                  :heartbeat (:heartbeat opts)
                                  :heartbeat-timeout (:heartbeat-timeout opts)})]
    (p/-start consumer)))

(defn stop-consumer
  "Takes a consumer created by start-consumer and stop all processes and the client if it is created
   in start-consumer. This should be called to stopped consuming messages."
  [^Consumer consumer]
  (p/-stop consumer))
