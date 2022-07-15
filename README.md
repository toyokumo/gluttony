# Gluttony
A consumer library using [core.async](https://github.com/clojure/core.async)
and [aws-api](https://github.com/cognitect-labs/aws-api) based on AWS SQS.

You can use this library with Standard queue but it is almost designed for FIFO queue.

[![Build and Test](https://github.com/toyokumo/gluttony/actions/workflows/build-and-test.yml/badge.svg)](https://github.com/toyokumo/gluttony/actions/workflows/build-and-test.yml)
[![cljdoc badge](https://cljdoc.org/badge/toyokumo/gluttony)](https://cljdoc.org/d/toyokumo/gluttony/CURRENT)
[![Clojars Project](https://img.shields.io/clojars/v/toyokumo/gluttony.svg)](https://clojars.org/toyokumo/gluttony)

## Usage
### Basis
Gluttony mainly offer two APIs, `start-consumer` and `stop-consumer`.

`start-consumer` makes a consumer instance.
It has some `go-loop` processes internally and they receive messages from AWS SQS
and invoke your `consume` function.

`stop-consumer` takes a consumer created by `start-consumer`, and stop all processes and the client
if you don't provide it.

After calling `stop-consumer`, the consumer doesn't make a new `consume` function call,
but it doesn't immediately stop long polling access to AWS SQS.
The message acquired by long polling is not processed by the consumer and becomes an "invisible message" until the visibility timeout elapses.

To prevent that, call `stop-receivers` before calling `stop-consumer` and wait for the number of seconds specified by `long-polling` option (default: 20).

```clojure
(require '[clojure.core.async :as a]
         '[gluttony.core :as gluttony])

(defn consume
  "Your consume function takes three arguments.
  `message` is a instance received from SQS.
  You MUST call `respond` which is 2nd argument or `raise` which is 3rd argument.
  respond delete the message from the SQS, so call it when your process has done successfully.
  raise doesn't delete the message but change the limit of time that the message can be seen
  from other receivers. raise takes zero or one argument, which control the limit of time.
  default limit is zero, which means that retry will be executed as soon as possible."
  [^gluttony.record.message.SQSMessage message respond raise]
  (let [success? (do-my-computation-use-cpu message)]
    (if success?
      (do (respond)
          (println "success!"))
      ;; You can pass to the raise a integer determines retry delay seconds
      (raise))))

(def queue-url "https://...")

(defonce consumer (atom nil))

;; Start consumer connects to assigned queue
(reset! consumer (gluttony/start-consumer queue-url consume))

;; Stop receiver and worker
(when @consumer
  (gluttony/stop-consumer @consumer)
  (reset! consumer nil))
```

`start-consumer` can take some optional arguments which control consumer work
such as the number of worker processes, long polling duration and so on.
See [API docs](https://cljdoc.org/d/toyokumo/gluttony/CURRENT) for detail.

### Heartbeat
If you don't know how long it takes to process a message, pass `:hearbeat` and `:heartbeat-timeout` options.

Then Gluttony extends the message visibility per `:hearbeat` seconds to `:heartbeat-timeout` seconds.

See [AWS documents](https://docs.aws.amazon.com/AWSSimpleQueueService/latest/SQSDeveloperGuide/working-with-messages.html) 
for more detail. 

## What for?
There already has been [Squeedo](https://github.com/TheClimateCorporation/squeedo) for the same purpose
and it provides nice APIs and work as intended. Gluttony use it as reference so much.

Gluttony deffer from Squeedo in following points.

First, Gluttony only uses asynchronous http requests with aws-api, which uses Jetty http client internally,
but Squeedo depends on [AmazonSQS](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/index.html) interface
instead of [AmazonSQSAsync](https://docs.aws.amazon.com/AWSJavaSDK/latest/javadoc/index.html) and
doesn't depend on AWS SDK Ver2 but Ver1.11.

Second, Gluttony does focus on being as a consumer, so doesn't have APIs such as creating a new queue
or setting attributes.

## Test
You may have to fix `dev-resources/test-config.edn`.

## License

Copyright 2019 TOYOKUMO,Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
