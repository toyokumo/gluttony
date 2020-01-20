(ns gluttony.sqs
  "wrapper of cognitect.aws.client.api"
  (:require
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [clojure.core.async :as a]
   [cognitect.aws.client.api.async :as aws-async]))

(defn- invoke-async
  [client op-map]
  (let [op-map (update op-map :request #(cske/transform-keys csk/->PascalCaseKeyword %))]
    (a/go
      (when-let [res (a/<! (aws-async/invoke client op-map))]
        (cske/transform-keys csk/->kebab-case-keyword res)))))

(defn receive-message
  "Retrieves one or more messages (up to 10), from the specified queue.

  Input:
    client  - the SQS client, which is the instance of cognitect.aws.client.Client.
    request - a map. See the result of (:request (:ReceiveMessage (aws/ops client))) but the keys can
              be :kebab-case-keyword.

  Output:
    a channel will take a map. See the result of (:response (:ReceiveMessage (aws/ops client))) but the
    keys are kebab-case-keyword."
  [client request]
  (invoke-async client {:op :ReceiveMessage :request request}))

(defn delete-message
  "Deletes the specified message from the specified queue.
  To select the message to delete, use the ReceiptHandle of the message.

  Input:
    client  - the SQS client, which is the instance of cognitect.aws.client.Client.
    request - a map. See the result of (:request (:DeleteMessage (aws/ops client))) but the keys can
              be :kebab-case-keyword.

  Output:
    a channel will take nil."
  [client request]
  (invoke-async client {:op :DeleteMessage :request request}))

(defn change-message-visibility
  "Changes the visibility timeout of a specified message in a queue to a new value.
  The default visibility timeout for a message is 30 seconds.
  The minimum is 0 seconds. The maximum is 12 hours.

  Input:
    client  - the SQS client, which is the instance of cognitect.aws.client.Client.
    request - a map. See the result of (:request (:ChangeMessageVisibility (aws/ops client))) but
              the keys can be :kebab-case-keyword.

  Output:
    a channel will take nil."
  [client request]
  (invoke-async client {:op :ChangeMessageVisibility :request request}))
