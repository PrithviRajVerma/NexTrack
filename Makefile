.PHONY: help dev db db-down backend frontend test build clean

help: ## Show this help message
	@echo "NexTrack Development Commands:"
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "\033[36m%-16s\033[0m %s\n", $$1, $$2}'

db: ## Start local PostgreSQL container
	docker compose up -d postgres

db-down: ## Stop local PostgreSQL container
	docker compose down

backend: ## Run Spring Boot backend in local profile
	cd backend && ./gradlew bootRun --args='--spring.profiles.active=local'

frontend: ## Run React Vite development server
	cd frontend && npm run dev

dev: db ## Start database, backend, and frontend concurrently
	@echo "Starting development environment..."
	@echo "1. Run 'make backend' in one terminal"
	@echo "2. Run 'make frontend' in another terminal"

test: ## Run backend unit & integration tests
	cd backend && ./gradlew clean test

build: ## Build backend and frontend distribution
	cd backend && ./gradlew build -x test
	cd frontend && npm run build

clean: ## Clean up local build artifacts and containers
	docker compose down -v
	cd backend && ./gradlew clean
