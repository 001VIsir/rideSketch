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
- **AI**: Ollama (localhost:11434, model: qwen3:8b, embedding: nomic-embed-text)

## Development Commands

### Backend (Windows)
```bash
# Compile project
.\mvnw.cmd compile

# Run application
.\mvnw.cmd spring-boot:run

# Run tests
.\mvnw.cmd test

# Run single test class
.\mvnw.cmd test -Dtest=UserAuthTest

# Package
.\mvnw.cmd clean package -DskipTests
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
- Ollama: localhost:11434 (with qwen3:8b and nomic-embed-text models)

## Code Architecture

### Backend Structure
```
src/main/java/org/example/ridesketch/
├── controller/      # REST controllers
│   ├── UserController, AuthController (implied at /api/auth/*)
│   ├── MapController, RouteController
│   ├── CommunityController, RagController, ChromaController
├── service/         # Business logic interfaces
│   └── impl/        # Service implementations
├── mapper/          # MyBatis-Plus data access
├── entity/          # Domain entities (User, Route, Comment, Like)
├── dto/             # Data transfer objects
├── config/          # Spring configurations (SecurityConfig, AIConfig, MyBatisPlusConfig)
├── security/        # JWT & Security (JwtUtils, JwtAuthenticationFilter)
└── common/          # Common utilities (Result)
```

### Frontend Structure
```
frontend/src/
├── views/           # Page components
│   ├── auth/        # LoginPage, RegisterPage
│   ├── map/         # MapPage (main map with route planning)
│   ├── route/       # PatternPage (pattern route generation)
│   └── community/   # CommunityPage, RouteDetailPage
├── components/      # Reusable components
│   ├── common/      # Amap (map wrapper)
│   └── route/       # RoutePanel, AIRoutePanel, RouteResultPanel
├── api/             # API client modules (user, map, route, community)
├── stores/          # Pinia state stores (routeStore, mapStore)
├── router/          # Vue Router configuration
├── utils/           # Utilities (amap.ts for Amap initialization)
└── types/           # TypeScript type definitions
```

## API Endpoints

### Auth & User
- `POST /api/auth/register` - User registration
- `POST /api/auth/login` - Login (use `usernameOrEmail` field, returns JWT)
- `GET/PUT /api/auth/me` - User profile

### Map
- `GET /api/map/geocode` - Address to coordinates
- `GET /api/map/regeo` - Reverse geocoding
- `GET /api/map/search` - POI search

### Route Planning
- `GET /api/route/riding` - Cycling route
- `GET /api/route/walking` - Walking route (may be limited by Amap)
- `POST /api/route/plan` - Multi-waypoint route planning
- `POST /api/route/ai-plan` - AI-powered natural language route planning
- `POST /api/route/pattern` - Generate pattern-shaped routes

### Community
- `GET /api/community/routes` - List published routes
- `POST /api/community/route` - Publish a route
- `GET /api/community/route/{id}` - Route details
- `PUT/DELETE /api/community/route/{id}` - Edit/delete (author only)
- `POST /api/community/route/{id}/like` - Like/unlike
- `GET/POST /api/community/route/{id}/comment` - Comments

### RAG & AI
- `GET /api/rag/search` - Semantic search in knowledge base
- `GET /api/rag/categories` - Get knowledge categories
- `GET /api/rag/health` - Health check

## Feature Status

All core features have been implemented. See `docs/feature_list.md` for detailed status.

| Module | Status |
|--------|--------|
| F001-F003 User (register/login/profile) | ✅ PASSED |
| F101-F104 Map (display/search/picking) | ✅ PASSED |
| F201-F203 Route Planning (basic/AI/multi-waypoint) | ✅ PASSED |
| F301-F302 Pattern Route Generation | ✅ PASSED |
| F401-F406 Community (publish/list/detail/like/comment) | ✅ PASSED |
| F501-F502 RAG Knowledge Base | ✅ PASSED |

## Documentation

- `docs/requirement.md` - Full project requirements
- `docs/feature_list.md` - Feature list with detailed status
- `docs/progress.md` - Development progress log
- `docs/problem.md` - Problems encountered and solutions (READ THIS for known issues)
- `docs/amap-jsapi-v2-docs.md` - Amap API reference
- `docs/init.sql` - Database initialization script

## Configuration

All config in `src/main/resources/application.properties`:
- Server port: 8080
- MySQL/Redis connections
- JWT secret & expiration
- Amap API key (server-side)
- Spring AI Ollama settings

Frontend Amap key configured in `frontend/src/utils/amap.ts`.

## Development Workflow

1. Check `docs/feature_list.md` for pending features
2. Read `docs/problem.md` to understand known issues and patterns
3. Implement following existing code patterns
4. Test and verify the feature works
5. Commit with descriptive message (Chinese)
6. Update `docs/progress.md` with completed work

## Known Issues & Patterns

- **Map Search**: Frontend uses Amap JS API directly (not backend) for accurate results. See `frontend/src/views/map/MapPage.vue`
- **API Format**: Backend community routes return array, not `{list, total}` wrapper
- **Login Field**: Use `usernameOrEmail` not `username` for login API
- **Windows Encoding**: Avoid Chinese in curl commands; use English or file-based requests
- **N+1 Query**: `CommunityServiceImpl.java:72` has N+1 query issue for like status (documented in problem.md)

## Important Notes

- Document language: Chinese (中文)
- Problems should be logged to `docs/problem.md`
- Follow existing code patterns
- Frontend uses Amap JS API directly for search (bypasses backend for accuracy)
