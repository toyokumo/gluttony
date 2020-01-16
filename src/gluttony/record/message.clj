(ns gluttony.record.message)

(defrecord SQSMessage
  [message-id
   receipt-handle
   md-5-of-body
   body
   attributes
   md-5-of-message-attributes
   message-attributes])
