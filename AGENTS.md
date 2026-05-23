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
mvn spring-boot:run              # Start backend (auto-creates my_blog DB + tables; requires Redis on localhost:6379)
mvn test                         # Run backend unit tests
mvn -q -DskipTests compile       # Fast backend compile check
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

## Agent Workflow
- Read this file first, then inspect the local code before changing behavior.
- Prefer small, focused patches that follow the current package/component style.
- Do not revert user changes or unrelated dirty worktree files.
- After backend changes, run at least `mvn -q -DskipTests compile`; run `mvn test` when domain/application logic changes.
- After frontend changes, run `npm run build`; run targeted Playwright e2e when routes or visible workflows change.
- For full local verification, start backend on `8080` and frontend on `5173`, then exercise the affected route through the browser.
- Keep generated artifacts, caches, logs, and uploads out of commits unless explicitly requested.

## Default Infrastructure
- MySQL: `localhost:3306/my_blog`, user `root`, password `123456`
- Redis: `localhost:6379`, no password (Redisson + Spring Data Redis)
- Mail: SMTP (QQ), configured in `application.yml`
- Environment overrides are supported for MySQL, Redis, mail, and frontend base URL in `application.yml`.
- Uploads default to backend `uploads/`; logs default to `backend/logs/application.log`.

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
  application/event/            - Application/domain event objects
  application/listener/         - Event handlers and side-effect orchestration
  application/port/             - Ports for external capabilities such as mail
  domain/model/aggregate/       - Aggregate roots
  domain/model/valueobject/     - Value objects
  domain/service/               - Domain services
  domain/repository/            - Repository interfaces
  infrastructure/repository/persistence/
    entity/                     - DO persistence objects
    mapper/                     - MyBatis mapper interfaces
    repository/                 - Repository implementations
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
  stores/       - Composition API reactive stores (no Pinia dependency currently)
  router/       - Vue Router config
  composables/  - Composition API reusable logic
  constants/    - Application constants
  styles/       - Global CSS
  utils/        - Utility functions
```

## Frontend Conventions
- Use `@` alias for `frontend/src`.
- API modules live in `frontend/src/api/` and should return unwrapped response `data` where existing modules do so.
- Auth state is managed by `frontend/src/stores/session.js`; protected routes use `meta.requiresAuth`.
- Admin routes additionally use `meta.requiresAdmin` and redirect non-admin users to `/403`.
- Keep page-level route views in `views/`; reuse shared UI through `components/` and workflow logic through `composables/`.
- Existing editor stack is TipTap + lowlight/highlight.js + DOMPurify; preserve sanitization for rendered or imported HTML.

## Backend Implementation Notes
- Controllers should depend on application services and REST mappers only.
- REST mappers convert request DTOs into `Command`/`Query`; application services return domain values or response-ready DTOs via assemblers.
- Repository interfaces belong in `domain/repository`; MyBatis implementations belong under `infrastructure/repository/persistence/repository`.
- MyBatis XML files are under `backend/src/main/resources/mapper/` and `mapper/growth/`.
- Application events/listeners are used for notifications, stats, growth rewards, and revenue settlement side effects.
- Public APIs should return `Result<T>` or `PageResult<T>` consistently through existing helpers.

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

## Current Functional Areas
- Core content: articles, drafts, versions, categories, tags, comments, likes, favorites, reading history.
- Discovery: home bootstrap, search, rankings, recommendations, topics, columns, following feed.
- User features: profile, profile settings, notifications, private messages, follows, article export.
- Admin features: overview, users, articles, comments, reports, taxonomy, columns, topics, ads, announcements, sensitive words, logs.
- Growth module: points, sign-in, badges, levels, privileges, invite rewards, article unlock, revenue share, growth admin config.

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
- Redisson config: `backend/src/main/java/com/myblog/infrastructure/config/RedissonConfig.java`
- Two-tier cache (Caffeine L1 + StringRedisTemplate L2 + GenericJackson2JsonRedisSerializer + Redisson RTopic cross-instance invalidation): `backend/src/main/java/com/myblog/application/cache/TwoTierCache.java`
- Cache cleanup on startup (`@PostConstruct clearOldCacheData`): `backend/src/main/java/com/myblog/application/config/CacheConfig.java`
- Cache config (bean definitions): `backend/src/main/java/com/myblog/application/config/CacheConfig.java`
- DB schema auto-init: `backend/src/main/resources/db/schema.sql`
- Growth DB schema: `backend/src/main/resources/db/growth/growth-schema.sql`
- Seed data: `backend/src/main/resources/db/initdata.sql` and `backend/src/main/resources/db/articles/*.sql`
- MyBatis mapper XML: `backend/src/main/resources/mapper/`
- Frontend entry: `frontend/src/main.js`
- Frontend router: `frontend/src/router/index.js`
- Frontend session store: `frontend/src/stores/session.js`
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
- `/auth/forgot-password` - Forgot password
- `/auth/reset-password` - Reset password
- `/403` - Access denied
- `/articles/:id` - Article detail
- `/editor/new` - New article (TipTap editor)
- `/editor/:id` - Edit article
- `/users/:id` - User profile
- `/following` - Following feed
- `/columns` - Columns
- `/columns/:id` - Column detail
- `/topics` - Topics
- `/topics/:id` - Topic detail
- `/explore` - Explore
- `/categories/:id` - Category detail
- `/tags/:id` - Tag detail
- `/ranking` - Ranking
- `/search` - Search results
- `/messages/:conversationId?` - Messages
- `/notifications` - Notifications
- `/history` - Reading history
- `/dashboard/overview` - Creator dashboard overview
- `/dashboard/articles` - My articles
- `/dashboard/favorites` - My favorites
- `/dashboard/columns` - My columns
- `/dashboard/growth` - Growth dashboard
- `/settings/profile` - Profile settings
- `/admin` - Admin layout, redirects to `/admin/overview`
- `/admin/overview` - Admin overview
- `/admin/users` - User management
- `/admin/articles` - Article management
- `/admin/comments` - Comment management
- `/admin/reports` - Report handling
- `/admin/categories` - Category management
- `/admin/tags` - Tag management
- `/admin/columns` - Column management
- `/admin/topics` - Topic management
- `/admin/ads` - Ad management
- `/admin/announcements` - Announcement management
- `/admin/sensitive-words` - Sensitive word management
- `/admin/logs` - Admin logs
- `/admin/growth` - Growth and points admin

## Important API Groups
- Auth/users: `/api/auth/*`, `/api/users/*`, `/api/users/me`
- Articles: `/api/articles`, `/api/articles/{id}`, likes/favorites/comments under `/api/articles/{articleId}/...`
- Home/discovery: `/api/home/*`, `/api/search/*`, `/api/rankings/*`, `/api/recommendations/*`
- Taxonomy/content organization: `/api/categories/*`, `/api/tags/*`, `/api/topics/*`, `/api/columns/*`
- Dashboard/admin: `/api/dashboard/*`, `/api/admin/*`
- Realtime-style streams: `/api/notifications/stream`, `/api/messages/stream`
- Growth: `/api/growth/*`, `/api/points/*`, `/api/badges/*`, `/api/admin/growth/*`, `/api/admin/points/*`, `/api/admin/rewards/*`
