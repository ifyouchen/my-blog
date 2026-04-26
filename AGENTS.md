# AGENTS.md

## Project Structure
- **Backend**: Spring Boot 2.7.18 + Java 8, DDD 四层架构 (interfaces → application → domain → infrastructure)
- **Frontend**: Vue 3 + Vite (port 5173), proxies `/api` to `http://localhost:8080`
- **Monorepo**: root-level `backend/` and `frontend/` directories

## Developer Commands

### Backend
```bash
cd backend
mvn spring-boot:run              # Start backend (auto-creates my_blog DB + tables)
mvn spring-boot:run -Dspring-boot.run.profiles=memory  # Use in-memory repository instead
```

### Frontend
```bash
cd frontend
npm install
npm run dev                       # Dev server on http://localhost:5173
npm run build                      # Production build to dist/
npm run test:e2e                   # Playwright e2e tests
```

## Default Infrastructure
- MySQL: `localhost:3306/my_blog`, user `root`, password `123456`
- Redis: `192.168.80.128:6379`, password `123456`

## Architecture Constraints (DDD)

**Dependency direction is strictly one-way:**
```
interfaces → application → domain
infrastructure → domain
infrastructure → application
```

**Hard rules:**
- Domain layer MUST NOT depend on interfaces layer
- Domain layer MUST NOT depend on infrastructure implementation classes
- Controller MUST NOT call Mapper directly
- Controller MUST NOT contain business logic
- Application service methods that mutate data MUST use `@Transactional(rollbackFor = Exception.class)`

## Database Conventions
- All tables: `utf8mb4` charset, InnoDB engine
- All tables have: `deleted_at` (soft delete via `deleted_at IS NULL`), `version` (optimistic lock), `created_at`, `updated_at`
- Physical deletion is forbidden
- Mapper XML must define `BaseResultMap` and `Base_Column_List`

## Code Style
- 4 spaces indentation (no tabs)
- Max 120 chars per line
- No wildcard imports (e.g., `java.util.*`)
- String comparison: constant on left (`"PUBLISHED".equals(status)`)

## Key Files
- Backend entry: `backend/src/main/resources/application.yml`
- DB schema auto-init: `backend/src/main/resources/db/schema.sql`
- Frontend entry: `frontend/src/main.js`
- Vite proxy config: `frontend/vite.config.js`

## Testing
- E2E: Playwright (`frontend/tests/e2e/`), base URL `http://127.0.0.1:5173`
- Run e2e tests: `npm run test:e2e` from `frontend/` (requires dev server running)
