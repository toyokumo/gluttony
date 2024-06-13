(ns gluttony.protocols)

(defprotocol IConsumer
  (-start [this])
  (-stop [this])
  (-enable-receivers [this])
  (-disable-receivers [this]))

(defprotocol ISqsClient
  (receive-message [this {:keys [queue-url
                                 max-number-of-messages
                                 wait-time-seconds]}]
    "Receive messages from AWS SQS.
    Input:
      queue-url - String.
                  The URL of the Amazon SQS queue from which messages are received.
      max-number-of-messages - Number.
                               The maximum number of messages to return.
                               This value is the value of the consumer's \"receive-limit\" option.
      wait-time-seconds - String.
                          The duration (in seconds) for which the call waits for a message to arrive in the queue before returning.
                          This value is the value of the consumer's \"long-polling-duration\" option.
    Output:
      a channel which will receive the result of the api when the operation is completed
      The result is a map with the following
        :messages - a coll of messages or nil (an error occurred).
                    Each message type is variable.
                    They are passed to the consumer's consume function.
        :error - An error data or nil (no error occurred).
                 The error type is variable.")
  (delete-message [this {:keys [queue-url receipt-handle]}]
    "Delete the specified message from the specified queue.
    Input:
      queue-url - String.
                  The URL of the Amazon SQS queue from which messages are deleted.
      receipt-handle - String.
                       The receipt handle associated with the message to delete.
    Output:
    a channel which will receive the result of the api when the operation is completed.
    The result is a map with the following
      :error - An error data or nil (no error occurred).
               The error type is variable.")
  (change-message-visibility [this {:keys [queue-url receipt-handle visibility-timeout]}]
    "Changes the visibility timeout of a specified message in a queue to a new value.
    The default visibility timeout for a message is 30 seconds.
    The minimum is 0 seconds. The maximum is 12 hours.
    Input:
      queue-url - String.
                  The URL of the Amazon SQS queue from which messages are deleted.
      receipt-handle - String.
                       The receipt handle associated with the message to delete.
      visibility-timeout - Number.
                           The new value for the message's visibility timeout.
    Output:
    a channel which will receive the result of the api when the operation is completed.
    The result is a map with the following
      :error - An error data or nil (no error occurred).
               The error type is variable.")
  (get-message-id [this message]
    "Get id of the message.
    Input:
      message - a message.
                The element of the :messages of the result of the receive-message.")
  (get-recipient-handle [this message]
    "Get the receipt handle of the message.
    Input:
      message - a message.
                The element of the :messages of the result of the receive-message.")
  (stop [this]
    "Stop the client. This should be called to stop the client when it is no longer needed."))
