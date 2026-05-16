# AGENTS.md

## Project Structure
- **Backend**: Spring Boot 2.7.18 + Java 8, DDD 四层架构 (interfaces → application → domain → infrastructure)
- **Frontend**: Vue 3 + Vite (port 5173), proxies `/api` to `http://localhost:8080`
- **Monorepo**: root-level `backend/` and `frontend/` directories
- **Bounded contexts**: `com.myblog.*` (core) + `com.myblog.growth.*` (growth module, separate DDD layers)

## Developer Commands

### Backend
```bash
cd backend
mvn spring-boot:run              # Start backend (auto-creates my_blog DB + tables)
mvn spring-boot:run -Dspring-boot.run.profiles=memory  # Use in-memory repository instead
mvn test                         # Run backend unit tests
```

### Frontend
```bash
cd frontend
npm install
npm run dev                       # Dev server on http://localhost:5173
npm run build                      # Production build to dist/
npm run preview                    # Preview production build
npm run test:e2e                   # Playwright e2e tests
npm run test:e2e:headed            # Playwright e2e tests with browser UI
```

## Default Infrastructure
- MySQL: `localhost:3306/my_blog`, user `root`, password `123456`
- Redis: `192.168.80.128:6379`, password `123456`
- Mail: SMTP (QQ), configured in `application.yml`

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
- Aggregates use rich domain models; state changes via business methods (`publish()`, `saveDraft()`, etc.), NOT setters
- Constructor injection required; NO field injection

## Backend Package Structure
```
com.myblog/
  interfaces/rest/controller/   - REST endpoints
  interfaces/rest/dto/          - Request/Response DTOs
  interfaces/rest/mapper/       - DTO <-> Command/Query converters
  application/service/          - Application services (use case orchestration)
  application/command/          - Command objects (write)
  application/query/            - Query objects (read)
  application/assembler/        - DTO <-> Domain converters
  domain/model/aggregate/       - Aggregate roots
  domain/model/valueobject/     - Value objects
  domain/service/               - Domain services
  domain/repository/            - Repository interfaces
  infrastructure/repository/    - MyBatis mappers, DO entities, repository impls
  infrastructure/security/      - JWT, AuthContext
  shared/enums/                 - ArticleStatus, UserRole, UserStatus
  shared/exception/             - BusinessException, DomainException
  shared/result/                - Result<T>, PageResult<T>
  growth/                       - Growth module (same DDD structure)
```

## Frontend Structure
```
frontend/src/
  api/          - Axios API modules
  components/   - Reusable Vue components
  views/        - Page components (HomeView, LoginView, etc.)
  stores/       - Pinia state stores
  router/       - Vue Router config
  composables/  - Composition API reusable logic
  constants/    - Application constants
  styles/       - Global CSS
  utils/        - Utility functions
```

## Database Conventions
- All tables: `utf8mb4` charset, InnoDB engine
- All tables have: `id` (BIGINT UNSIGNED), `deleted_at` (soft delete via `deleted_at IS NULL`), `version` (optimistic lock), `created_at`, `updated_at`
- Physical deletion is forbidden
- Mapper XML must define `BaseResultMap` and `Base_Column_List`
- All queries MUST filter `deleted_at IS NULL`
- Schema auto-init from: `db/schema.sql`, `db/growth/growth-schema.sql`, `db/initdata.sql`

## API Response Format
```json
{ "code": 0, "message": "success", "data": {} }
```
- `code: 0` = success, `400` = param error, `401` = unauthorized, `403` = forbidden, `404` = not found

## Security
- JWT authentication; token contains `userId`, `username`, `role`
- Backend MUST validate user status even if JWT is valid
- Passwords hashed with BCrypt
- File uploads: max 20MB per file, 22MB per request

## Code Style
- 4 spaces indentation (no tabs)
- Max 120 chars per line
- No wildcard imports (e.g., `java.util.*`)
- String comparison: constant on left (`"PUBLISHED".equals(status)`)
- All `public` classes and methods must have Javadoc with `@author` and `@since`
- Wrapper types MUST use `.equals()`, NOT `==`

## Key Files
- Backend entry: `backend/src/main/resources/application.yml`
- Backend main class: `backend/src/main/java/com/myblog/MyBlogApplication.java`
- DB schema auto-init: `backend/src/main/resources/db/schema.sql`
- Frontend entry: `frontend/src/main.js`
- Vite proxy config: `frontend/vite.config.js`
- Dev guidelines: `docs/10-开发规范.md`

## Testing
- Backend: JUnit 5 + AssertJ (`cd backend && mvn test`)
- Frontend E2E: Playwright (`frontend/tests/e2e/`), base URL `http://127.0.0.1:5173`
- Run e2e tests: `npm run test:e2e` from `frontend/` (requires dev server running)
- Test structure: Given / When / Then
- Core domain logic MUST have unit tests

## Article Status Flow
`DRAFT` → `PUBLISHED` → `OFFLINE` / `DELETED`
- Publishing sets `published_at`
- All status transitions validated in `Article` aggregate

## Current Frontend Routes
- `/` - Home
- `/login` - Login
- `/register` - Register
- `/articles/:id` - Article detail
- `/editor/new` - New article (TipTap editor)
- `/users/:id` - User profile
- `/search` - Search results
- `/dashboard/articles` - My articles
- `/dashboard/favorites` - My favorites
- `/admin` - Admin panel
