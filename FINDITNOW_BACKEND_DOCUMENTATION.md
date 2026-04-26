# FINDITNOW BACKEND DOCUMENTATION
## Minimal Production Build Guide for New Developers

---

## 1. What This System Is

A **Java microservice backend** using:
- Docker Compose for orchestration
- PostgreSQL for data storage
- Redis for caching/sessions
- Nginx as reverse proxy
- gRPC for internal service communication
- HTTP/REST for external APIs

Built as a **Gradle multi-module monorepo**.

---

## 2. Repository Structure

```
finditnow-backend/
│
├── services/              # Microservices
│   ├── auth/             # Authentication (Plain JVM + Undertow)
│   ├── file-gateway/     # File handling (Plain JVM + Undertow)
│   ├── user-service/     # User management (Spring Boot)
│   ├── order-service/    # Orders (Spring Boot)
│   └── shop-service/     # Shops (Spring Boot)
│
├── libs/                 # Shared libraries
│   ├── common/          # Utilities, exceptions
│   ├── config/          # Configuration loading
│   ├── database/        # DB utilities
│   ├── redis/           # Redis client
│   ├── jwt/             # Token handling
│   ├── dispatcher/      # HTTP routing
│   └── proto/           # gRPC definitions
│
├── infra/               # Infrastructure
│   ├── nginx/          # Reverse proxy config
│   ├── postgres/       # DB init scripts
│   └── data/
│       ├── data-dev/   # Dev volumes
│       └── data-prod/  # Prod volumes
│
├── docker-compose.dev.yml
├── docker-compose.prod.yml
├── .env.example
├── settings.gradle.kts
└── build.gradle.kts
```

---

## 3. The Critical Distinction: Two Service Types

### Spring Boot Services (user, order, shop)
- Use Spring Boot framework
- Embedded Tomcat web server
- Auto-configured components
- **Build artifact**: Fat JAR (all dependencies inside)
- **Gradle task**: `bootJar`
- **Run command**: `java -jar app.jar`

### Plain JVM Services (auth, file-gateway)
- No framework, minimal dependencies
- Manual Undertow configuration
- Manual logging setup
- **Build artifact**: Distribution (bin/ scripts + lib/ JARs)
- **Gradle task**: `installDist`
- **Run command**: `sh bin/servicename`

**Why this matters**: Different packaging = different Docker strategies.

---

## 4. How Services Are Built

### Spring Boot Services

**Gradle plugin used:**
```
org.springframework.boot
```

**What `bootJar` produces:**
```
build/libs/user-service-1.0.0-boot.jar
```

This JAR contains:
- Your compiled code
- All dependencies (Spring, PostgreSQL driver, etc.)
- Embedded Tomcat
- Custom launcher

It's **self-contained** and can run anywhere with Java 21+.

### Plain JVM Services

**Gradle plugin used:**
```
application
```

**What `installDist` produces:**
```
build/install/auth/
├── bin/
│   └── auth              # Startup script
└── lib/
    ├── auth.jar          # Your code
    ├── undertow.jar      # Dependencies
    ├── slf4j.jar
    └── grpc-netty.jar
```

The script in `bin/` handles:
- Building the complete classpath
- Including all dependencies
- Running the application

**Critical**: Never run `java -jar auth.jar` directly. The JAR doesn't contain dependencies.

---

## 5. Why Shadow Plugin Is Banned

Shadow plugin creates "fat JARs" for plain services, but:
- Incompatible with Gradle 8+
- Breaks configuration cache
- Causes cryptic build failures
- Unstable for production

**Decision**: Use `installDist` distribution approach instead.

---

## 6. Docker Strategy

### Core Principle
**Docker runs pre-built artifacts, never builds Java code.**

Docker Compose calls Dockerfiles that:
1. Copy already-built artifacts from `build/` directory
2. Set up runtime environment
3. Execute the application

### Spring Boot Dockerfile Pattern
```
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/libs/*-boot.jar app.jar
EXPOSE 8082
CMD ["java", "-jar", "app.jar"]
```

### Plain JVM Dockerfile Pattern
```
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY build/install/auth /app/
EXPOSE 8080
CMD ["sh", "/app/bin/auth"]
```

**Key difference**: Spring Boot copies JAR, Plain JVM copies entire distribution.

---

## 7. Build Commands

### Build Everything
```bash
.\gradlew clean installDist --no-daemon
```

This command:
- Cleans previous builds
- Compiles all libraries
- Builds Spring Boot JARs via `bootJar`
- Builds distributions for plain services via `installDist`

### Build Single Service
```bash
.\gradlew :services:auth:installDist
.\gradlew :services:user-service:bootJar
```

---

## 8. Docker Networking

### The Localhost Problem

Inside Docker, each service is in its own container.
**`localhost` refers to the container itself, not other services.**

### Correct Configuration
```
DB_HOST=postgres           # Service name from docker-compose
REDIS_HOST=redis
USER_SERVICE_HOST=user-service
```

### Incorrect Configuration
```
DB_HOST=localhost          # ❌ Won't work in containers
DB_HOST=127.0.0.1          # ❌ Won't work in containers
```

Docker Compose creates an internal network where services resolve by name.

---

## 9. Port Exposure

### `expose` vs `ports`

**expose**: Internal Docker network only
```yaml
expose:
  - "8080"
```
Services can reach each other, but host machine cannot.

**ports**: External access
```yaml
ports:
  - "8080:8080"
```
Host machine can access via `localhost:8080`.

### Best Practice
Only Nginx should expose ports to the host.
Internal services use `expose` only.

---

## 10. gRPC Communication

### What Failed Before
```
Error: Address types of NameResolver 'unix' for 'user:8082'
```

**Root cause**: gRPC Netty transport missing at runtime.

### Why It Works Now
`installDist` includes all dependencies in `lib/`:
- grpc-netty-shaded.jar
- grpc-core.jar
- All transitive dependencies

Docker DNS resolves service names (e.g., `user-service`) to container IPs.

**No code changes needed** when switching from JAR-only to distribution packaging.

---

## 11. Database Initialization

PostgreSQL container uses:
```
/docker-entrypoint-initdb.d/
```

Scripts placed here:
- Execute on first container startup only
- Create databases, schemas, seed data
- Run in alphabetical order

Located in: `infra/postgres/init/`

---

## 12. Environment Separation

### Development
```
infra/data/data-dev/
docker-compose.dev.yml
```

### Production
```
infra/data/data-prod/
docker-compose.prod.yml
```

**Data directories never overlap.**
Prevents accidental production data corruption during development.

---

## 13. Common Mistakes to Avoid

| Mistake | Why It Fails | Correct Approach |
|---------|--------------|------------------|
| `java -jar auth.jar` | No dependencies in JAR | Use `sh bin/auth` |
| Using Shadow plugin | Gradle incompatibility | Use `installDist` |
| `DB_HOST=localhost` in Docker | Container isolation | Use service names |
| Mixing service types | Different packaging needs | Follow type-specific patterns |
| Building inside Docker | Slow, cache issues | Build with Gradle, copy to Docker |

---

## 14. Deployment Process

### Step 1: Build Locally
```bash
.\gradlew clean installDist --no-daemon
```

### Step 2: Verify Artifacts
**Spring Boot services:**
```
build/libs/*-boot.jar exists
```

**Plain JVM services:**
```
build/install/servicename/ exists
build/install/servicename/bin/ exists
build/install/servicename/lib/ exists
```

### Step 3: Docker Compose
```bash
docker compose -f docker-compose.prod.yml up -d --build
```

### Step 4: Verify
```bash
docker compose ps
docker compose logs -f
```

**Success indicators:**
- All containers show "running"
- No restart loops
- Logs show successful startup
- Database connections established
- gRPC servers listening

---

## 15. Architecture Flow

```
Client Request
    ↓
Nginx (Port 80/443)
    ↓
┌─────────┬─────────┬─────────┬─────────┐
│  Auth   │  File   │  User   │  Shop   │
│  (8080) │ (8081)  │ (8082)  │ (8084)  │
└────┬────┴────┬────┴────┬────┴────┬────┘
     │         │         │         │
     └─────────┴─────────┴─────────┘
               ↓
     ┌─────────────────────┐
     │    PostgreSQL       │
     │      (5432)         │
     └─────────────────────┘
               ↓
     ┌─────────────────────┐
     │       Redis         │
     │      (6379)         │
     └─────────────────────┘
```

**Communication:**
- External → Nginx → Services (HTTP)
- Service → Service (gRPC)
- Services → PostgreSQL (JDBC)
- Services → Redis (Lettuce client)

---

## 16. Key Mental Models

### Model 1: Service Type Determines Build Strategy
- Spring Boot → Fat JAR → `java -jar`
- Plain JVM → Distribution → `sh bin/script`

### Model 2: Docker Is Dumb
Docker doesn't understand:
- Gradle
- Maven
- Build systems

Docker only copies files and runs commands.
**Always build outside Docker.**

### Model 3: Containers Are Islands
Each container is isolated.
Communication happens via:
- Docker network (service names)
- Exposed ports
- Shared volumes

`localhost` means "this container only."

### Model 4: Dependencies Must Be Explicit
A JAR without dependencies is useless.
Either:
- Package dependencies inside (Spring Boot)
- Include dependencies separately (installDist)

No middle ground works reliably.

---

## 17. Troubleshooting Quick Reference

### Problem: ClassNotFoundException at runtime
**Cause**: Dependencies not in classpath
**Solution**: Verify `installDist` was run, check `build/install/service/lib/`

### Problem: Cannot connect to database
**Cause**: Using `localhost` instead of service name
**Solution**: Set `DB_HOST=postgres` in environment

### Problem: gRPC connection refused
**Cause**: Service name resolution or port mismatch
**Solution**: Verify service names in docker-compose, check exposed ports

### Problem: Container keeps restarting
**Cause**: Application crashes on startup
**Solution**: Check logs with `docker compose logs servicename`

### Problem: Gradle build fails with plugin errors
**Cause**: Incorrect plugin for service type
**Solution**: Spring Boot services use `org.springframework.boot`, Plain JVM use `application`

---

## 18. What Makes This Production-Ready

### Build Stability
- No deprecated Gradle plugins
- Configuration cache compatible
- Repeatable builds

### Docker Safety
- No build processes in containers
- Clear separation of concerns
- Predictable artifact locations

### Networking Correctness
- Proper service discovery
- Isolated networks
- Correct port exposure

### Dependency Management
- Spring Boot manages versions automatically
- Plain services use explicit distributions
- No manual classpath manipulation

---

## 19. Final Checklist

Before deploying:

**Build verification:**
- [ ] `.\gradlew clean installDist` succeeds
- [ ] Spring Boot JARs exist in `build/libs/`
- [ ] Plain service distributions exist in `build/install/`

**Docker verification:**
- [ ] All Dockerfiles copy correct artifacts
- [ ] Environment variables use service names
- [ ] Only Nginx exposes external ports

**Runtime verification:**
- [ ] `docker compose up` starts all containers
- [ ] No containers in restart loop
- [ ] Database connections succeed
- [ ] gRPC communication works
- [ ] Health endpoints respond

**If all checks pass, the system is ready.**

---

## 20. Summary

This system works because:

1. **Service types are respected** - Spring Boot and Plain JVM services package differently
2. **Gradle does the building** - Docker only runs pre-built artifacts
3. **installDist solves dependencies** - No fat JAR hacks needed
4. **Docker networking is explicit** - Service names, not localhost
5. **Structure is consistent** - Patterns are predictable and repeatable

**When something breaks:** Verify you're following the correct pattern for the service type.

**When adding features:** Match existing service patterns.

**When debugging:** Check build artifacts first, then Docker configuration, then code.

The system is now stable, correct, and maintainable.
