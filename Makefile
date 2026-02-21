.PHONY: infra-up infra-down infra-reset infra-status infra-logs

infra-up:
	docker compose --profile infra up -d

infra-down:
	docker compose --profile infra down

infra-reset:
	docker compose --profile infra down -v
	docker compose --profile infra up -d

infra-status:
	docker compose --profile infra ps

infra-logs:
	docker compose --profile infra logs -f
