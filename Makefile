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

.PHONY: docker-up
docker-up:
	cd dev-docker && docker compose up -d

.PHONY: docker-down
docker-down:
	cd dev-docker && docker compose down
