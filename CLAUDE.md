# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**rideSketch (骑迹)** - 智能骑行路线规划与社区分享平台

- **Backend**: Spring Boot 3.5.x with Java 17
- **Frontend**: Vue 3 + TypeScript, Vite, Element Plus
- **Database**: MySQL 8.x (root/Cqian1231)
- **Cache**: Redis (localhost:6379)
- **ORM**: MyBatis-Plus
- **Auth**: Spring Security + JWT
- **Maps**: Amap (高德地图) JS API 2.0
- **AI**: Ollama (localhost:11434, model: qwen3:8b)

## Development Commands

### Backend
```bash
# Compile project
./mvnw compile

# Run application
./mvnw spring-boot:run

# Run tests
./mvnw test

# Run single test class
./mvnw test -Dtest=UserAuthTest

# Package
./mvnw clean package -DskipTests
```

### Frontend
```bash
cd frontend
npm install
npm run dev
npm run build
```

### Required Services (must be running)
- MySQL: localhost:3306 (database: ridesketch)
- Redis: localhost:6379
- Ollama: localhost:11434

## Code Architecture

```
src/main/java/org/example/ridesketch/
├── controller/      # REST controllers (UserController, MapController, RouteController)
├── service/       # Business logic (UserService, MapService, RouteService, AIRouteService)
│   └── impl/      # Service implementations
├── mapper/        # MyBatis-Plus data access (UserMapper)
├── entity/        # Domain entities (User)
├── dto/           # Data transfer objects (Request/Response)
├── config/        # Spring configurations (SecurityConfig, MyBatisPlusConfig)
├── security/      # JWT & Security (JwtUtils, JwtAuthenticationFilter)
└── common/        # Common utilities (Result)
```

## API Endpoints

- **User**: `POST /api/user/register`, `POST /api/user/login`
- **Map**: `GET /api/map/geocode`, `GET /api/map/regeo`, `GET /api/map/search`
- **Route**: `POST /api/route/plan`, `POST /api/route/ai-plan`, `POST /api/route/pattern`

## Key Features Status

| Feature | Description | Status |
|---------|-------------|--------|
| F001 | User registration | PASSED |
| F002 | User login (JWT) | PASSED |
| F101-F104 | Map display, search, picking | PASSED |
| F201 | Basic route planning (cycling/walking) | PASSED |
| F202 | AI smart route planning (Ollama) | PASSED |
| F203 | Multi-waypoint route | PASSED |
| F301-F302 | Pattern route generation | NOT PASSED |
| F401-F406 | Community features | NOT PASSED |

## Documentation

- `docs/requirement.md` - Full project requirements
- `docs/feature_list.md` - Feature list with status (pick highest priority with `passes: false`)
- `docs/progress.md` - Development progress log
- `docs/amap-jsapi-v2-docs.md` - Amap API reference documentation
- `docs/init.sql` - Database initialization script

## Configuration

All config in `src/main/resources/application.properties`:
- Server port: 8080
- MySQL/Redis connections
- JWT secret & expiration
- Amap (高德地图) API key

## Development Workflow

1. Check `docs/feature_list.md` for pending features (status: "未通过" / not passed)
2. Pick highest priority pending feature
3. Implement following existing code patterns in the codebase
4. Test and verify the feature works
5. Commit with descriptive message (Chinese)
6. Update `docs/progress.md` with completed work

## Important Notes

- Document language: Chinese (中文)
- Problems should be logged to `docs/problem.md`
- Follow existing code patterns (see UserController, MapController, RouteController)
- Amap API reference: see `docs/amap-jsapi-v2-docs.md`
