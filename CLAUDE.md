# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

`my-blog` is a blog community platform for technical creators and readers. It has separate frontend (Vue 3) and backend (Spring Boot) with DDD architecture.

## Build Commands

### Backend
```bash
cd backend
mvn spring-boot:run
```
- MySQL: `localhost:3306/my_blog` (root/123456)
- Redis: `192.168.80.128:6379` (password: 123456)
- Runs on port 8080

### Frontend
```bash
cd frontend
npm install
npm run dev
```
- Runs on port 5173
- Vite proxies `/api` requests to `http://localhost:8080`

### Running Tests
```bash
# Backend unit tests
cd backend && mvn test

# Frontend e2e tests
cd frontend && npm run test:e2e
```

## Architecture

### Backend: DDD Four-Layer Architecture

```
interfaces -> application -> domain -> infrastructure
```

**Layer responsibilities:**
- `interfaces/rest/controller/` - HTTP handling, parameter validation, DTO conversion, response wrapping
- `application/service/` - Use case orchestration, transaction boundaries, domain event publishing
- `domain/model/` - Entities, aggregate roots, value objects, domain services, repository interfaces
- `infrastructure/repository/persistence/` - MyBatis mappers, DO entities, repository implementations

**Package structure (`com.myblog`):**
```
interfaces/rest/controller/  - REST endpoints
interfaces/rest/dto/         - Request/Response DTOs
application/service/         - Application services
application/command/         - Command objects (write operations)
application/query/           - Query objects (read operations)
application/assembler/       - DTO <-> Domain object converters
domain/model/aggregate/     - Aggregate roots (Article, User, Comment, etc.)
domain/model/valueobject/    - Value objects (ArticleId, UserId, Email, etc.)
domain/service/              - Domain services
domain/repository/           - Repository interfaces
infrastructure/repository/persistence/entity/    - MyBatis DO entities
infrastructure/repository/persistence/mapper/    - MyBatis mapper interfaces
infrastructure/repository/persistence/converter/ - DO <-> Domain converters
infrastructure/security/     - JWT, AuthContext
shared/enums/                 - ArticleStatus, UserRole, UserStatus
shared/exception/             - BusinessException, DomainException
shared/result/                - Result, PageResult
```

### Domain Model Patterns

**Aggregates** (e.g., `Article`):
- No setter methods; state changes via business methods (`publish()`, `saveDraft()`, `offline()`, `delete()`)
- Factory methods (`create()`, `restore()`) for construction
- Validation logic inside the aggregate

**Repository pattern:**
- Interfaces defined in `domain/repository/`
- Implementations in `infrastructure/repository/persistence/repository/`

### Frontend: Vue 3 SPA

```
frontend/src/
  api/          - Axios API modules
  components/   - Vue components
  views/        - Page components
  stores/       - Pinia stores
  router/       - Vue Router config
```

## Key Conventions

### Database
- Soft delete: `deleted_at IS NULL` means not deleted
- All tables have `id`, `created_at`, `updated_at`, `deleted_at`, `version`
- Use `BIGINT UNSIGNED` for IDs
- Use `utf8mb4` charset

### API Response Format
```json
{ "code": 0, "message": "success", "data": {} }
```
- `code: 0` = success, `400` = param error, `401` = unauthorized, `403` = forbidden, `404` = not found

### Security
- JWT for authentication; include `userId`, `username`, `role` in token
- Backend must validate user status even if JWT is valid
- Passwords hashed with Spring Security BCrypt

### Request Flow
1. Controller validates input, converts DTO
2. Application service orchestrates use case
3. Domain model enforces business rules
4. Repository persists changes through MyBatis

### Article Status Flow
`DRAFT` -> `PUBLISHED` -> `OFFLINE` / `DELETED`
- Publishing sets `published_at`
- All status transitions validate business rules in the `Article` aggregate
