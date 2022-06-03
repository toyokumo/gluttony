.PHONY: test
test:
	clojure -M:dev:test

.PHONY: lint
lint:
	cljstyle check
	clj-kondo --parallel --lint src/ --config '{:output {:pattern "::{{level}} file={{filename}},line={{row}},col={{col}}::{{message}}"}}'

.PHONY: install
install:
	clojure -T:build install
