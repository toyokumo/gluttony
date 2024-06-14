# Change Log

## [Unreleased]
### Breaking Changes
* Add required "client" arguments to start-consumer

#### Migration
Rewrite start-consumer like below.
```clojure
;; before (using default client)
(gluttony/start-consumer queue-url consume {:num-workers 1})

;; before (using custom client)
(gluttony/start-consumer queue-url consume {:client client :num-workers 1})

;; after (using default client)
(require '[gluttony.record.cognitect-sqs-client :as g.client])
(gluttony/start-consumer queue-url consume (g.client/make-client) {:num-workers 1})

;; after (using custom client)
(require '[gluttony.record.cognitect-sqs-client :as g.client])
(gluttony/start-consumer queue-url consume (g.client/make-client client) {:num-workers 1})
```
And add api packages `com.cognitect.aws/api`, `com.cognitect.aws/endpoints` and `com.cognitect.aws/sqs` to dependencies.

### Added
* AWS API clients other than [aws-api](https://github.com/cognitect-labs/aws-api) (`com.cognitect.aws/api`) are now available.
** [aws-api](https://github.com/cognitect-labs/aws-api) is still available as `gluttony.record.cognitect-sqs-client`.
** [AWS SDK for Java 2.0](https://github.com/aws/aws-sdk-java-v2) is available as `gluttony.record.aws-sdk-client`.
** If you want to use other clients, you can implement `gluttony.protocols/ISqsClient`.

## 0.5.106
### Changed
* Bump library versions.

## 0.5.99
### Added
* Add `:visibility-timeout-in-heartbeat` option to control visibility timeout in heartbeat mode.

## 0.4.91
### Fixed
* Fixed `respond` and `raise` not to take extra signs when these functions are called more than once.

## 0.4.87
### Changed
* Updated to use Clojure CLI instead of Leiningen.
* Add `start-receivers` and `stop-receivers` functions.

## 0.3.2
Update dependencies (#6).

## 0.3.1
Fix order to make a sign of consuming.

## 0.3.0
Bump library versions.
Add `:consume-limit` option.

## 0.2.0
Add heartbeat suppoert.

## 0.1.0
Initial release.
