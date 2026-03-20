SHELL := /bin/bash

.PHONY: deps-up dev-up dev-verify dev-down dev-reset dev-seed-likes

deps-up:
	docker compose -f docker-compose.es-local.yml up -d --build

dev-up:
	./scripts/dev-up.sh

dev-verify:
	./scripts/dev-verify.sh

dev-seed-likes:
	./scripts/dev-seed-likes.sh

dev-down:
	./scripts/dev-down.sh

dev-reset:
	./scripts/dev-reset.sh
