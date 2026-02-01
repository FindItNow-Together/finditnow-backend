# FindItNow Backend — Technical Project Overview

This document is derived strictly from the current codebase. It is intended for technical interviews, onboarding, and architecture reviews.

---

## 1. PROJECT OVERVIEW

| Item | Description |
|------|-------------|
| **Project name** | FindItNow (findItNow / finditnow-backend) |
| **Core purpose** | Backend for a local discovery and commerce platform: users discover shops and products, manage carts, place orders, and handle delivery. Auth is centralized; other domains (user, shop, cart, order, delivery, file) are separate deployable services. |
| **Problem it solves** | Provides a single backend ecosystem for registration/login (including OAuth), user profiles and addresses, shop and product catalog, cart, orders, payments (Razorpay), delivery assignment and tracking, and file upload/download—with clear service boundaries and shared JWT/Redis usage. |
| **Target users** | End users (customers), shop owners, delivery agents, and admins—differentiated by roles (CUSTOMER, SHOP, DELIVERY_AGENT, ADMIN, UNASSIGNED). |
| **Architecture type** | **Microservices.** Multiple runnable applications (auth, user-service, shop-service, order-service, delivery-service, file-gateway), each with its own build, config, and (where applicable) database. Shared libs (jwt, redis, database, config, common, proto, interservice-caller, dispatcher) and inter-service communication (REST + gRPC + service tokens) justify the classification. |

---

## 2. TECH STACK

| Layer | Technology |
|-------|------------|
| **Programming language** | Java 21 |
| **Frameworks / libraries** | **Auth & file-gateway:** Undertow (HTTP), Flyway, JDBC (Hikari), Jackson, SLF4J/Logback. **Other services:** Spring Boot 3.5.7, Spring Web, Spring Data JPA, Spring Security, Spring Validation; Hibernate Validator 9.0.1; MapStruct 1.6.3 (user-service, shop-service); Lombok; SpringDoc OpenAPI 2.3.0 (user-service). **Shared:** JJWT 0.12.3 / 0.13.0 (libs/jwt), Guava, Dotenv (Config). |
| **Server** | **Undertow** (auth service, file-gateway). **Embedded Tomcat** (Spring Boot for user-service, shop-service, order-service, delivery-service). |
| **Security** | JWT (access + refresh semantics), Redis for refresh tokens and access-token blacklist; BCrypt (libs/common) for password hashing; Spring Security with `JwtAuthFilter` and `@PreAuthorize` in Spring services; Basic auth for internal service-token endpoint; refresh-token cookie for file download when domain is private. |
| **Database** | PostgreSQL. Per-service DBs: `auth_service`, `user_service` (via `Database.setEnv("user_service")`), `order_service`, `delivery_db`, `shop_service`. |
| **ORM / data access** | **Auth:** Raw JDBC + DAOs (AuthCredentialDao, AuthSessionDao, AuthOauthGoogleDao), HikariCP, Flyway migrations. **Other services:** Spring Data JPA (Hibernate), Flyway in user-service; JPA `ddl-auto` (update/validate) in shop, order, delivery. |
| **Build** | Gradle 9.x (Kotlin DSL). Multi-project: root `build.gradle.kts`, `settings.gradle.kts`; each service and lib has its own `build.gradle.kts`. |
| **Environment profiles** | No Spring profiles in code; environment is driven by env vars (e.g. `ENVIRONMENT`, `DB_*`, `*_PORT`, `*_SECRET`) and optional `.env` (Dotenv in Config). |

---

## 3. SYSTEM ARCHITECTURE

### 3.1 Overall flow

- **Client** → HTTP/REST to API (auth, user, shop, order, delivery, file-gateway). Frontend or other clients send `Authorization: Bearer <accessToken>` (or refresh cookie where applicable).
- **Auth service** is the token authority: issues access + refresh tokens, validates sessions, exposes `/internal/service-token` for service-to-service tokens.
- **Downstream services** (user, shop, order, delivery) validate JWT via shared `JwtService` + `RedisStore` (blacklist), and optionally call each other via `InterServiceClient` (Bearer service token).
- **User-service** also exposes gRPC (`UserServiceGrpc`) for auth-service (create user profile, update role) and is called by auth on signup/verify and role update.
- **Database:** Each service connects to its own PostgreSQL database (or shared host with different DB names). Redis is shared for tokens and blacklist.

### 3.2 Request lifecycle (typical)

1. Request hits Undertow or Spring MVC.
2. **Auth:** `RequestLoggingHandler` → `JwtAuthHandler` (if path not `/internal/`) → route handler (e.g. `AuthController`, `OauthController`, `ServiceTokenController`). Private routes require valid JWT; otherwise 401.
3. **Spring services:** `JwtAuthFilter` runs first: extract Bearer token, check blacklist, parse JWT, set `userId` / `profile` (and optionally `credId`) on request; then Spring Security and `@PreAuthorize` enforce roles; controller → service → repository/DAO.
4. Response: JSON (and/or cookies for refresh token in auth and file download).

### 3.3 Layer separation

| Service | Controllers | Services | Repositories / DAOs | Security |
|---------|-------------|----------|---------------------|----------|
| **Auth** | AuthController, OauthController, ServiceTokenController | AuthService, OAuthService | AuthDao (AuthCredentialDao, AuthSessionDao, AuthOauthGoogleDao) | JwtAuthHandler, private routes set, ServiceRegistry (Basic) |
| **User-service** | UserController, UserAddressController | UserService, UserAddressService | UserRepository, UserAddressRepository (JPA); UserDao for gRPC path | JwtAuthFilter, Spring Security (stateless) |
| **Shop-service** | ShopController, ProductController, CartController, CategoryController, SearchController, InventoryController | ShopService, ProductService, CartService, CategoryService, SearchService, ShopInventoryService | JPA repositories (Shop, Product, Cart, CartItem, Category, ShopInventory) | JwtAuthFilter, SecurityFilterChain, @PreAuthorize |
| **Order-service** | OrderController, PaymentController | OrderService, PaymentService | JPA repositories (Order, OrderItem, Payment) | JwtAuthFilter, SecurityFilterChain |
| **Delivery-service** | DeliveryController, DeliveryAgentController | DeliveryService, DeliveryAgentService | JPA repositories (Delivery, DeliveryAgent) | JwtAuthFilter, SecurityFilterChain, @PreAuthorize |
| **File-gateway** | N/A (handlers only) | N/A | N/A | RefreshTokenValidator (cookie) for private download |

### 3.4 Inter-module / inter-service communication

- **REST:** `InterServiceClient` (in libs/interservice-caller) calls auth `/internal/service-token` with Basic auth (service name + secret), then calls target service with `Authorization: Bearer <serviceToken>`. Call graph: user-service → delivery-service, shop-service; order-service → delivery-service, shop-service, user-service; delivery-service → order-service, shop-service, user-service; shop-service → delivery-service, order-service, user-service.
- **gRPC:** Auth → user-service (CreateUserProfile, UpdateUserRole). Proto: `libs/proto` → `user_service.proto`; user-service implements `UserServiceGrpc.UserServiceImplBase`.
- **Service tokens:** Short-lived (60s), issuer `auth-service`, typ `service`, audience from request body; validated only where services accept them (e.g. cart consume, delivery-agent add).

---

## 4. AUTHENTICATION & AUTHORIZATION

### 4.1 Registration flow

1. **POST /signup** (AuthController.signUp): Body `SignUpDto` (email, password, firstName, phone, role). Validates required fields; default role `customer`.
2. AuthService checks existing credential by email; if verified → 409 `user_already_verified`; if unverified → 400 `account_not_verified` + credId.
3. New credential: UUID credId & userId, BCrypt hash password, insert `auth_credentials`, send verification email (OTP stored in Redis `emailOtp:<credId>`), return 201 with credId and (in dev) emailOtp.
4. **POST /verifyemail**: Body credId + verificationCode. Validates OTP from Redis; updates `is_email_verified`; creates session; creates user profile via gRPC `CreateUserProfile`; stores refresh in Redis; returns accessToken + profile + firstName; sets HttpOnly cookie `refresh_token` (sessionToken).

### 4.2 Login flow

1. **POST /signin**: Body email + password. Find credential by identifier (email/phone); if not verified → 409; if no password (OAuth-only) → 409; verify BCrypt; create session, insert `auth_sessions`, generate access token, store refresh in Redis, return accessToken + profile + firstName; set refresh cookie.
2. **GET /oauth/google**: Redirect to Google OAuth with state stored in Redis.
3. **GET /oauth/google/callback**: Exchange code for tokens, decode Google ID token (payload validation), find or create credential and OAuth link; create session; return redirect to frontend with cookies set (refresh); frontend gets access via /refresh if needed.

### 4.3 JWT token generation and validation

- **Access token:** Generated in AuthService (via JwtService): `generateAccessToken(sessionId, credId, userId, authProfile)`. Subject = sessionId; claims: credId, userId, profile. Expiry: 60 * 60 * 1000 ms (1 hour). Signed with HMAC (JWT_SECRET).
- **Refresh token:** Opaque session token (UUID) stored in DB `auth_sessions` and in Redis key `refresh:<token>` with value `sessionId|credId|userId|profile`; TTL from session expiry (e.g. 7 days). Not a JWT.
- **Validation:** Downstream services use `JwtService.parseTokenToUser(token)` or `validateTokenWithBlacklist(token, redis)`; Redis blacklist key `blacklist:access:<token>` used after logout.

### 4.4 Access vs refresh

- **Access:** Short-lived, Bearer in Authorization header; used for API calls.
- **Refresh:** Long-lived, stored in cookie `refresh_token` (HttpOnly, SameSite Lax/None, Secure in non-dev). Used only in auth service: POST /refresh (cookie) → new access token; POST /logout (cookie + optional Bearer) → invalidate session and blacklist access.

### 4.5 Token payload structure (access JWT)

- `sub`: session ID (UUID).
- `credId`: credential ID (UUID).
- `userId`: user ID (UUID).
- `profile`: role string (e.g. CUSTOMER, SHOP, ADMIN, DELIVERY_AGENT).
- `iat`, `exp`: issued at / expiration.

### 4.6 Role-based authorization

- **Auth:** Private routes (e.g. `/updatepassword`, `/updaterole`, `/logout`) require valid JWT; no role check in handler (role used in business logic).
- **Spring services:** `@PreAuthorize("hasRole('SHOP')")`, `hasAnyRole('SHOP','ADMIN')`, `isAuthenticated()`, `hasRole('SERVICE')` on controllers; roles from JWT `profile` mapped to Spring authorities (e.g. ROLE_SHOP). Shop: SHOP/ADMIN for shop/product/inventory/cart; SERVICE for cart consume/get by id. Delivery: DELIVERY_AGENT/ADMIN for status and agent actions.

### 4.7 How protected routes are enforced

- **Auth (Undertow):** `JwtAuthHandler` wraps routes; for paths in `privateRoutes`, missing or invalid/expired JWT → 401; otherwise attaches SESSION_INFO and AUTH_TOKEN to exchange.
- **Spring:** `JwtAuthFilter` (OncePerRequestFilter) parses Bearer token, blacklist check, sets request attributes `userId`, `profile`; Spring Security `SecurityFilterChain` + `@PreAuthorize` deny unauthorized/forbidden.

---

## 5. API DOCUMENTATION

APIs are grouped by module. Base paths assume deployment behind a gateway or per-service port; actual path may include a prefix (e.g. /api). Auth base path is `/` on auth service port.

### 5.1 Auth module (Auth service, Undertow)

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Handler | Auth |
|--------|------|---------|---------|------|---------|------|--------|---------|------|
| POST | /signup | Register | Content-Type: application/json | `{"email","password","firstName","phone","role"}` | credId, message, accessTokenValiditySeconds | error | 201/400/409/500 | AuthController | No |
| POST | /signin | Login | Content-Type: application/json | `{"email","password"}` | accessToken, profile, firstName | error | 200/401/409/500 | AuthController | No |
| POST | /verifyemail | Verify email OTP | Content-Type: application/json | `{"credId","verificationCode"}` | accessToken, profile, firstName | error | 200/400/404/500 | AuthController | No |
| POST | /resendverificationemail | Resend OTP | Content-Type: application/json | `{"credId"}` | message | error | 201/400/500 | AuthController | No |
| POST | /refresh | New access token | Cookie: refresh_token | - | accessToken, profile | error | 200/401 | AuthController | Cookie |
| POST | /logout | Logout | Cookie: refresh_token; optional Authorization: Bearer | - | message | - | 200 | AuthController | Optional Bearer |
| POST | /sendresettoken | Send reset OTP | Content-Type: application/json | `{"email"}` or `{"phone"}` | message, tokenValiditySeconds | error | 200/400/500 | AuthController | No |
| GET | /verifyresettoken | Verify reset token | - | Query: email/phone, resetToken | message | error | 200/400 | AuthController | No |
| PUT | /resetpassword | Reset password (after verify) | Content-Type: application/json | `{"email"\|"phone","newPassword"}` | message | error | 200/400/404/500 | AuthController | No |
| PUT | /updatepassword | Change password (logged in) | Authorization: Bearer; Content-Type: application/json | `{"newPassword"}` | message | error | 200/400/500 | AuthController | Yes |
| PUT | /updaterole | Update role by cred | Authorization: Bearer; Content-Type: application/json | `{"role"}` | message | error | 201/400/500 | AuthController | Yes |
| GET | /oauth/google | Redirect to Google | - | - | 302 | error | 302/401 | OauthController | No |
| GET | /oauth/google/callback | Google callback | - | Query: code, state | 302 redirect | error | 302/400/401/409 | OauthController | No |
| POST | /internal/service-token | Issue service token | Authorization: Basic (service:secret); Content-Type: application/json | `{"audience":"<service>"}` | accessToken, expiresIn | - | 200/401/403 | ServiceTokenController | Basic |
| GET | /health | Health | - | - | `{"status":"ok"}` | - | 200 | Routes | No |

### 5.2 User module (User service)

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Controller | Auth |
|--------|------|---------|---------|------|---------|------|--------|------------|------|
| POST | /add | Create user | Content-Type: application/json | UserDto (firstName, lastName, email, phone, role, etc.) | ApiResponse&lt;UserDto&gt; | - | 201 | UserController | Yes |
| GET | /{id} | Get user by ID | - | - | ApiResponse&lt;UserDto&gt; | - | 200 | UserController | Yes |
| GET | /me | Current user | Authorization: Bearer | - | ApiResponse&lt;UserDto&gt; | - | 200 | UserController | Yes |
| GET | /all | List users (paginated) | - | Query: page, size, sortBy, sortDir | ApiResponse&lt;PagedResponse&lt;UserDto&gt;&gt; | - | 200 | UserController | Yes |
| GET | /role/{role} | List by role | - | Query: page, size, sortBy, sortDir | ApiResponse&lt;PagedResponse&lt;UserDto&gt;&gt; | - | 200 | UserController | Yes |
| GET | /search | Search by name and role | - | Query: name, role, page, size | ApiResponse&lt;PagedResponse&lt;UserDto&gt;&gt; | - | 200 | UserController | Yes |
| PUT | /{id} | Update user | Content-Type: application/json | UserDto | ApiResponse&lt;UserDto&gt; | - | 200 | UserController | Yes |
| DELETE | /{id} | Delete user | - | - | ApiResponse&lt;Void&gt; | - | 200 | UserController | Yes |

### 5.3 User addresses (User service)

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Controller | Auth |
|--------|------|---------|---------|------|---------|------|--------|------------|------|
| POST | /addresses/add | Add address | Authorization: Bearer; Content-Type: application/json | UserAddressDto | ApiResponse&lt;UserAddressDto&gt; | - | 201 | UserAddressController | Yes (userId from token) |
| GET | /addresses/{id} | Get address | - | - | ApiResponse&lt;UserAddressDto&gt; | - | 200 | UserAddressController | Yes |
| GET | /addresses/user/{userId} | List by user | - | - | ApiResponse&lt;List&lt;UserAddressDto&gt;&gt; | - | 200 | UserAddressController | Yes |
| GET | /addresses/user/{userId}/paginated | List by user (paginated) | - | Query: page, size | ApiResponse&lt;PagedResponse&lt;UserAddressDto&gt;&gt; | - | 200 | UserAddressController | Yes |
| GET | /addresses/user/{userId}/primary | Primary address | - | - | ApiResponse&lt;UserAddressDto&gt; | - | 200 | UserAddressController | Yes |
| PUT | /addresses/{id} | Update address | Content-Type: application/json | UserAddressDto | ApiResponse&lt;UserAddressDto&gt; | - | 200 | UserAddressController | Yes |
| DELETE | /addresses/{id} | Delete address | - | - | ApiResponse&lt;Void&gt; | - | 200 | UserAddressController | Yes |

### 5.4 Shop module (Shop service)

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Controller | Auth |
|--------|------|---------|---------|------|---------|------|--------|------------|------|
| POST | /shop/add | Register shop | Authorization: Bearer; Content-Type: application/json | ShopRequest | ShopResponse | - | 201 | ShopController | SHOP/ADMIN |
| GET | /shop/mine | My shops | Authorization: Bearer | - | PagedResponse&lt;ShopResponse&gt; | - | 200 | ShopController | SHOP |
| GET | /shop/all | All shops | Authorization: Bearer | - | PagedResponse&lt;ShopResponse&gt; | - | 200 | ShopController | ADMIN |
| GET | /shop/{id} | Get shop | - | - | ShopResponse | - | 200 | ShopController | No |
| PUT | /shop/{id} | Update shop | Authorization: Bearer; Content-Type: application/json | ShopRequest | ShopResponse | - | 200 | ShopController | SHOP/ADMIN |
| DELETE | /shop/{id} | Delete shop | Authorization: Bearer | - | - | - | 204 | ShopController | SHOP/ADMIN |
| DELETE | /shop/bulk | Delete shops | Authorization: Bearer; Content-Type: application/json | List&lt;Long&gt; shopIds | - | - | 204 | ShopController | SHOP/ADMIN |
| POST | /shop/{shopId}/products | Add product to shop | Authorization: Bearer; Content-Type: application/json | ProductRequest | ProductResponse | - | 201 | ShopController | SHOP/ADMIN |
| GET | /shop/{shopId}/products | Products by shop | - | - | List&lt;ProductResponse&gt; | - | 200 | ShopController | No |
| GET | /shop/search | Search shops | - | Query: name, deliveryOption, lat, lng, page, size | PagedResponse&lt;ShopResponse&gt; | - | 200 | ShopController | No |

### 5.5 Product module (Shop service)

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Controller | Auth |
|--------|------|---------|---------|------|---------|------|--------|------------|------|
| POST | /product/add | Add product | Authorization: Bearer; Content-Type: application/json | ProductRequest | ProductResponse | - | 201 | ProductController | SHOP/ADMIN |
| GET | /product/all | All products (paginated) | - | Query: page, size | PagedResponse&lt;ProductResponse&gt; | - | 200 | ProductController | No |
| GET | /product/search | Search products | - | Query: query | List&lt;ProductResponse&gt; | - | 200 | ProductController | No |
| PUT | /product/{id} | Update product | Authorization: Bearer; Content-Type: application/json | ProductRequest | ProductResponse | - | 200 | ProductController | SHOP/ADMIN |
| DELETE | /product/{id} | Delete product | Authorization: Bearer | - | - | - | 204 | ProductController | SHOP/ADMIN |
| DELETE | /product/bulk | Delete products | Authorization: Bearer; Content-Type: application/json | List&lt;Long&gt; productIds | - | - | 204 | ProductController | SHOP/ADMIN |

### 5.6 Cart module (Shop service)

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Controller | Auth |
|--------|------|---------|---------|------|---------|------|--------|------------|------|
| POST | /cart/add | Add item | Authorization: Bearer; Content-Type: application/json | AddToCartRequest (shopId, inventoryId, quantity, etc.) | CartResponse | - | 201 | CartController | Yes |
| PUT | /cart/item/{itemId} | Update item | Authorization: Bearer; Content-Type: application/json | UpdateCartItemRequest | CartResponse | - | 200 | CartController | Yes |
| DELETE | /cart/item/{itemId} | Remove item | Authorization: Bearer | - | - | - | 204 | CartController | Yes |
| GET | /cart/user/me | My cart | Authorization: Bearer | - | CartResponse | - | 200 | CartController | Yes |
| GET | /cart/user/{userId}/shop/{shopId} | Cart by user/shop (deprecated; auth userId used) | Authorization: Bearer | - | CartResponse | - | 200 | CartController | Yes |
| DELETE | /cart/{cartId}/clear | Clear cart | Authorization: Bearer | - | - | - | 204 | CartController | Yes |
| DELETE | /cart/{cartId}/internal/consume | Consume cart (order flow) | Authorization: Bearer (service) | - | - | - | 204 | CartController | SERVICE |
| GET | /cart/{cartId} | Get cart by ID | Authorization: Bearer (service) | - | CartResponse | - | 200 | CartController | SERVICE |
| GET | /cart/{cartId}/pricing | Cart pricing | Authorization: Bearer | - | CartPricingResponse | - | 200 | CartController | Yes |

### 5.7 Category module (Shop service)

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Controller | Auth |
|--------|------|---------|---------|------|---------|------|--------|------------|------|
| POST | /categories | Create category | Authorization: Bearer; Content-Type: application/json | CategoryRequest | ApiResponse&lt;CategoryResponse&gt; | - | 200 | CategoryController | ADMIN/SHOP |
| GET | /categories | List by type | - | Query: type (CategoryType) | ApiResponse&lt;List&lt;CategoryResponse&gt;&gt; | - | 200 | CategoryController | No |

### 5.8 Search module (Shop service)

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Controller | Auth |
|--------|------|---------|---------|------|---------|------|--------|------------|------|
| GET | /search/products | Search products | - | Query: q, lat, lng, fulfillment, page, size | ApiResponse&lt;PagedResponse&lt;SearchOpportunityResponse&gt;&gt; | - | 200 | SearchController | No |
| GET | /search/global | Global search | - | Query: q, lat, lng, shopLimit, productLimit | ApiResponse&lt;GlobalSearchResponse&gt; | - | 200 | SearchController | No |

### 5.9 Inventory module (Shop service)

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Controller | Auth |
|--------|------|---------|---------|------|---------|------|--------|------------|------|
| GET | /shop/{shopId}/inventory | List inventory | - | - | List&lt;InventoryResponse&gt; | - | 200 | InventoryController | No |
| GET | /shop/{shopId}/inventory/{id} | Get inventory item | - | - | InventoryResponse | - | 200 | InventoryController | No |
| POST | /shop/{shopId}/inventory/add | Add inventory | Authorization: Bearer; Content-Type: application/json | AddInventoryRequest | InventoryResponse | - | 201 | InventoryController | SHOP/ADMIN |
| PUT | /shop/{shopId}/inventory/{id} | Update inventory | Authorization: Bearer; Content-Type: application/json | UpdateInventoryRequest | InventoryResponse | - | 200 | InventoryController | SHOP/ADMIN |
| DELETE | /shop/{shopId}/inventory/{id} | Delete inventory | Authorization: Bearer | - | - | - | 204 | InventoryController | SHOP/ADMIN |
| POST | /shop/{shopId}/inventory/{id}/reserve | Reserve stock | - | - | Query: quantity | InventoryResponse | - | 200 | InventoryController | No |
| POST | /shop/{shopId}/inventory/{id}/release | Release stock | - | - | Query: quantity | InventoryResponse | - | 200 | InventoryController | No |

### 5.10 Order module (Order service)

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Controller | Auth |
|--------|------|---------|---------|------|---------|------|--------|------------|------|
| POST | /orders/from-cart | Create order from cart | Authorization: Bearer; Content-Type: application/json | CreateOrderFromCartRequest | OrderResponse | - | 200 | OrderController | Yes |
| GET | /orders/{orderId} | Get order | Authorization: Bearer | - | OrderResponse | - | 200 | OrderController | Yes |
| GET | /orders/mine | My orders | Authorization: Bearer | - | List&lt;OrderResponse&gt; | - | 200 | OrderController | Yes |
| GET | /orders/quote | Delivery quote | - | Query: shopId, addressId | DeliveryQuoteResponse | - | 200 | OrderController | No (or Yes) |
| PUT | /orders/{orderId}/status | Update order status | Authorization: Bearer; Content-Type: application/json | StatusUpdateRequest | OrderResponse | - | 200 | OrderController | Yes |

### 5.11 Payment module (Order service)

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Controller | Auth |
|--------|------|---------|---------|------|---------|------|--------|------------|------|
| POST | /payments/initiate | Initiate payment | Content-Type: application/json | InitiatePaymentRequest | PaymentInitiationResponse | - | 200 | PaymentController | Yes |
| POST | /payments/webhook/razorpay | Razorpay webhook | Content-Type: application/json; X-Razorpay-Signature | Raw body | status | - | 200 | PaymentController | Signature |
| POST | /payments/callback | Payment callback | Content-Type: application/json | razorpay_order_id, razorpay_payment_id, razorpay_signature | status | - | 200/400 | PaymentController | No |
| GET | /payments/verify/{orderId} | Verify payment | - | - | - | - | 200 | PaymentController | Yes |

### 5.12 Delivery module (Delivery service)

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Controller | Auth |
|--------|------|---------|---------|------|---------|------|--------|------------|------|
| POST | /deliveries/calculate-quote | Quote | Content-Type: application/json | DeliveryQuoteRequest | DeliveryQuoteResponse | - | 200 | DeliveryController | No |
| POST | /deliveries/initiate | Create delivery | Content-Type: application/json | InitiateDeliveryRequest | DeliveryResponse | - | 200 | DeliveryController | Yes |
| GET | /deliveries/order/{orderId} | Get by order | - | - | DeliveryResponse | - | 200 | DeliveryController | Yes |
| PUT | /deliveries/{id}/status | Update status | Authorization: Bearer; Content-Type: application/json | StatusUpdateRequest | DeliveryResponse | - | 200 | DeliveryController | DELIVERY_AGENT/ADMIN |
| GET | /deliveries/mine | My deliveries | Authorization: Bearer | Query: status, page, limit | PagedDeliveryResponse | - | 200 | DeliveryController | DELIVERY_AGENT |
| PUT | /deliveries/{id}/accept | Accept | Authorization: Bearer | - | DeliveryResponse | - | 200 | DeliveryController | DELIVERY_AGENT |
| PUT | /deliveries/{id}/complete | Complete | Authorization: Bearer | - | DeliveryResponse | - | 200 | DeliveryController | DELIVERY_AGENT |
| PUT | /deliveries/{id}/cancel | Cancel | Authorization: Bearer | - | DeliveryResponse | - | 200 | DeliveryController | DELIVERY_AGENT |
| PUT | /deliveries/{id}/opt-out | Opt out | Authorization: Bearer | - | DeliveryResponse | - | 200 | DeliveryController | DELIVERY_AGENT |

### 5.13 Delivery agent module (Delivery service)

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Controller | Auth |
|--------|------|---------|---------|------|---------|------|--------|------------|------|
| POST | /delivery-agent/add | Create agent (inter-service) | Authorization: Bearer (service) | CreateDeliveryAgentRequest | - | - | 200 | DeliveryAgentController | Service |
| PUT | /delivery-agent/{agentId}/status | Update status | - | UpdateAgentStatusRequest | - | - | 200 | DeliveryAgentController | Yes |
| PUT | /delivery-agent/my-status | My status | Authorization: Bearer | UpdateAgentStatusRequest | DeliveryAgentStatus | - | 200 | DeliveryAgentController | Yes |
| GET | /delivery-agent/{agentId} | Get agent | - | - | DeliveryAgent | - | 200 | DeliveryAgentController | Yes |
| GET | /delivery-agent/my-status | My status | Authorization: Bearer | - | DeliveryAgentStatus | - | 200 | DeliveryAgentController | Yes |

### 5.14 File gateway

| Method | Path | Purpose | Headers | Body | Success | Error | Status | Handler | Auth |
|--------|------|---------|---------|------|---------|------|--------|---------|------|
| POST | /upload | Upload file | Content-Type: multipart/form-data | form: file, domain, entityId, purpose | `{"fileKey":"/..."}` | - | 200/400 | UploadHandler | No (in code) |
| GET | /download/{domain}/{entityId}/{purpose}/{file} | Download | Cookie: refresh_token (if domain private) | - | File stream | 400/404 | 200/400/404 | DownloadHandler | Cookie for private (e.g. receipt) |

---

## 6. DATABASE DESIGN

### 6.1 Database type

PostgreSQL. Separate databases per service: `auth_service`, `user_service` (via env), `order_service`, `delivery_db`, `shop_service`. Schema created via Flyway (auth, user-service) or JPA `ddl-auto` (shop, order, delivery).

### 6.2 Auth service (Flyway migrations)

**auth_credentials**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | UUID | PK | |
| user_id | UUID | NOT NULL, UNIQUE | |
| email | TEXT | UNIQUE | |
| phone | TEXT | UNIQUE | |
| first_name | TEXT | NOT NULL DEFAULT 'User' | |
| password_hash | TEXT | | |
| is_email_verified | BOOLEAN | DEFAULT FALSE | |
| is_phone_verified | BOOLEAN | DEFAULT FALSE | |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | |
| role | user_role | NOT NULL DEFAULT 'unassigned' | Enum: admin, customer, shop, delivery_agent, unassigned |

**auth_sessions**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | UUID | PK | |
| cred_id | UUID | NOT NULL | |
| session_token | TEXT | UNIQUE NOT NULL | |
| session_method | VARCHAR(30) | | |
| ip_address | INET | | |
| user_agent | TEXT | | |
| expires_at | TIMESTAMPTZ | | |
| is_valid | BOOLEAN | DEFAULT TRUE | |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | |
| revoked_at | TIMESTAMPTZ | | |

**auth_oauth_google**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | UUID | PK | |
| user_id | UUID | NOT NULL, FK → auth_credentials(user_id) ON DELETE CASCADE | |
| google_user_id | TEXT | NOT NULL UNIQUE | |
| email | TEXT | | |
| created_at | TIMESTAMPTZ | DEFAULT NOW() | |

**user_role** (enum): admin, customer, shop, delivery_agent, unassigned.

### 6.3 User service (Flyway + JPA)

**users** (from V1__create_users_table.sql)

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | UUID | PK DEFAULT gen_random_uuid() | |
| created_at | TIMESTAMPTZ | NOT NULL | |
| email | VARCHAR(255) | NOT NULL, UNIQUE | |
| first_name | VARCHAR(255) | NOT NULL | |
| last_name | VARCHAR(255) | | |
| phone | VARCHAR(255) | UNIQUE | |
| profile_url | VARCHAR(255) | | |
| username | VARCHAR(255) | | |

Entity `User` also has `role` (JPA may add column via ddl-auto if not in migration).

**user_addresses**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | UUID | PK DEFAULT gen_random_uuid() | |
| city | VARCHAR(255) | | |
| country | VARCHAR(255) | | |
| full_address | TEXT | | |
| line1 | VARCHAR(255) | | |
| line2 | VARCHAR(255) | | |
| postal_code | VARCHAR(255) | | |
| state | VARCHAR(255) | | |
| user_id | UUID | FK → users(id) ON DELETE CASCADE | |

Entity `UserAddress` also has isPrimary, latitude, longitude, addressType (JPA may add).

### 6.4 Shop service (JPA-managed schema)

**shop** (table name `shop`)

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | BIGSERIAL | PK | |
| name | VARCHAR | NOT NULL | |
| address | VARCHAR | NOT NULL | |
| phone | VARCHAR | NOT NULL | |
| owner_id | UUID | NOT NULL | |
| latitude | DOUBLE | NOT NULL | |
| longitude | DOUBLE | NOT NULL | |
| open_hours | VARCHAR | NOT NULL | |
| delivery_option | VARCHAR | NOT NULL | |
| category_id | BIGINT | FK → categories | |

**product**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | BIGSERIAL | PK | |
| name | VARCHAR | NOT NULL | |
| description | TEXT | | |
| category_id | BIGINT | NOT NULL, FK → categories | |
| image_url | VARCHAR | | |

**categories**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | BIGSERIAL | PK | |
| name | VARCHAR | NOT NULL | UNIQUE(name, type) |
| description | VARCHAR | | |
| image_url | VARCHAR | | |
| type | VARCHAR (enum) | NOT NULL | CategoryType |
| active | BOOLEAN | NOT NULL | default true |
| created_at | TIMESTAMP | NOT NULL | |

**shop_inventories**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | BIGSERIAL | PK | |
| reserved_stock | INT | >= 0 | |
| price | FLOAT | >= 0 | |
| stock | INT | >= 0 | |
| shop_id | BIGINT | NOT NULL, FK → shop | |
| product_id | BIGINT | FK → product | |

**carts**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | UUID | PK | |
| user_id | UUID | NOT NULL | |
| shop_id | BIGINT | NOT NULL | |
| status | VARCHAR (enum) | NOT NULL | ACTIVE, CONVERTED, ABANDONED |
| created_at | TIMESTAMP | NOT NULL | |
| updated_at | TIMESTAMP | NOT NULL | |

**cart_items**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | UUID | PK | |
| cart_id | UUID | NOT NULL, FK → carts | |
| inventory_id | BIGINT | NOT NULL, FK → shop_inventories | |
| quantity | INT | NOT NULL | |
| added_at | TIMESTAMP | NOT NULL | |

### 6.5 Order service (JPA)

**orders**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | UUID | PK | |
| user_id | UUID | NOT NULL | |
| shop_id | BIGINT | NOT NULL | |
| status | VARCHAR (OrderStatus) | | |
| payment_method | VARCHAR | | |
| payment_status | VARCHAR | | |
| total_amount | DOUBLE | | |
| delivery_address_id | UUID | | |
| delivery_charge | DOUBLE | | |
| instructions | VARCHAR | | |
| delivery_type | VARCHAR | | |
| created_at | TIMESTAMP | | |

**order_items**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | UUID | PK | |
| order_id | UUID | NOT NULL, FK → orders | |
| product_id | BIGINT | | |
| product_name | VARCHAR | | |
| price_at_order | DOUBLE | | |
| quantity | INT | | |

**payments**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | UUID | PK | |
| order_id | UUID | NOT NULL, FK → orders | |
| payment_mode | VARCHAR | | UPI, CARD, etc. |
| provider | VARCHAR | | RAZORPAY, etc. |
| provider_payment_id | VARCHAR | | |
| amount | DOUBLE | | |
| status | VARCHAR | | |
| collected_at | TIMESTAMP | | |
| created_at | TIMESTAMP | | |

### 6.6 Delivery service (JPA)

**deliveries**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| id | UUID | PK | |
| order_id | UUID | NOT NULL UNIQUE | |
| shop_id | BIGINT | NOT NULL | |
| customer_id | UUID | NOT NULL | |
| assigned_agent_id | UUID | | |
| status | VARCHAR (DeliveryStatus) | NOT NULL | |
| type | VARCHAR (DeliveryType) | NOT NULL | |
| pickup_address | TEXT | NOT NULL | |
| delivery_address | TEXT | NOT NULL | |
| instructions | TEXT | | |
| delivery_charge | DOUBLE | | |
| created_at | TIMESTAMP | | |
| updated_at | TIMESTAMP | | |

**delivery_agents**

| Column | Type | Constraints | Notes |
|--------|------|-------------|--------|
| agent_id | UUID | PK | Same as users.id |
| status | VARCHAR | NOT NULL | |
| current_delivery_id | UUID | | |
| zone | VARCHAR(50) | | |
| created_at | TIMESTAMP | | |
| updated_at | TIMESTAMP | | |

**delivery_opted_out_agents** (element collection): delivery_id, agent_id.

### 6.7 Entity ↔ table mapping

- Auth: `AuthCredential` → auth_credentials, `AuthSession` → auth_sessions, `AuthOauthGoogle` → auth_oauth_google.
- User: `User` → users, `UserAddress` → user_addresses.
- Shop: `Shop` → shop, `Product` → product, `Category` → categories, `ShopInventory` → shop_inventories, `Cart` → carts, `CartItem` → cart_items.
- Order: `Order` → orders, `OrderItem` → order_items, `Payment` → payments.
- Delivery: `Delivery` → deliveries, `DeliveryAgent` → delivery_agents.

### 6.8 Fields hidden from API

- **AuthCredential:** `passwordHash` is `transient` and never serialized; not exposed in any API.
- **User / UserDto:** Service and mappers expose only DTO fields; internal entity fields (e.g. lazy relations) are not returned unless mapped.
- **Payment:** Provider IDs and raw payment details are not exposed in generic order responses; payment endpoints return minimal or structured DTOs.

---

## 7. BUSINESS LOGIC & VALIDATIONS

### 7.1 Validation rules (from code)

- **SignUp:** email or phone required; password, firstName required (SignUpDto.isValid()). Role default customer; server-side no regex on email/phone in auth.
- **Password:** BCrypt hash; `PasswordUtil.checkPwdString` used on update/reset (regex in code includes `\n` at end—likely typo).
- **UserDto:** @NotBlank firstName, email; @Size firstName 2–50; @Email; @Pattern phone (E.164-style); @NotBlank role.
- **Shop/Product/Inventory/Cart:** @Valid request DTOs; quantity &gt; 0 (OrderItem); stock/reserve non-negative.
- **Delivery:** @Valid on quote/initiate/status DTOs.

### 7.2 Logic in service layer

- Auth: credential existence, verification state, session creation, user profile creation via gRPC, Redis refresh/blacklist, OTP send and verify.
- Shop: ownership checks (shop/product/inventory) by userId and isAdmin; cart ownership for clear/update/remove; cart consume for order flow; inventory reserve/release.
- Order: create from cart (call shop-service to consume cart, get pricing), status transitions, payment initiation and webhook handling.
- Delivery: quote calculation, assignment, accept/complete/cancel/opt-out by agent.

### 7.3 Key business constraints

- One cart per user/shop (or equivalent); cart items reference shop_inventories; quantity &lt;= available (stock − reserved).
- Order created from cart; cart consumed (status/items) so same cart not reused.
- Delivery: one delivery per order (unique orderId); agent can accept/complete/cancel/opt-out only for assigned delivery.
- Role updates in auth and user-service kept in sync via gRPC; delivery agent creation triggered from user-service when role is DELIVERY_AGENT.

### 7.4 Error handling

- Auth: Controller returns 4xx/5xx with JSON `{"error":"<code>"}`; exceptions logged; transaction rollback on failure.
- Spring services: `@ControllerAdvice` (e.g. user-service ExceptionHandlerAdvice); global handlers return consistent error body; custom exceptions (e.g. ResourceNotFoundException, DuplicateResourceException, ForbiddenException, UnauthorizedException in shop/file-gateway).

### 7.5 Custom exceptions (present in codebase)

- Auth: NoSuchCredentialException, CredentialException.
- JWT lib: JwtExpiredException, JwtInvalidException, JwtValidationException.
- User-service: DuplicateResourceException, ResourceNotFoundException.
- Shop-service: BadRequestException, CartItemNotFoundException, CartNotFoundException, ForbiddenException, NotFoundException, UnauthorizedException.
- File-gateway: UnauthorizedException.

---

## 8. SECURITY CONSIDERATIONS

- **Password hashing:** BCrypt (libs/common `PasswordUtil`) for signup, login, update password, reset password.
- **Entities not exposed:** DTOs and response types used in controllers; entities (and internal IDs where not needed) stay in service/repository layer. Password hash is transient and never serialized.
- **Unauthorized access:** Private auth routes require valid JWT; Spring endpoints use JwtAuthFilter + SecurityFilterChain + @PreAuthorize; cart/order/delivery scoped by userId from token; shop/product/inventory mutations check ownership or ADMIN.
- **Token misuse:** Access tokens blacklisted in Redis on logout; refresh tokens invalidated in DB and Redis; service tokens short-lived (60s) and audience-restricted via ServiceRegistry.
- **Logging/auditing:** RequestLoggingHandler (auth); CommonsRequestLoggingFilter (user-service); SLF4J/Logback across services. No dedicated audit table in code.

---

## 9. CONFIGURATION & ENVIRONMENT

- **Configuration structure:** Env vars preferred; Dotenv loads `.env` from service directory and project root (libs/config). Spring Boot services use `application.properties` (and optionally `application.yml`) with placeholders like `${VAR:default}`.
- **Application properties (examples):** `server.port`, `spring.datasource.url` (JDBC_DATABASE_URL), `spring.datasource.username`/`password`, `spring.jpa.hibernate.ddl-auto`, `cors.allowed-origins`, `logging.level.*`.
- **Environment-specific:** No Spring profiles; same code paths with different env values (e.g. ENVIRONMENT=development vs production, DB_* per environment).
- **Secrets:** JWT_SECRET, OAUTH_CLIENT_ID/SECRET, REDIS_HOST/PORT, DB_PASSWORD, per-service secrets (USER_SERVICE_SECRET, ORDER_SERVICE_SECRET, etc.) from env or .env; not committed (`.env.example` present).
- **Ports and server:** Auth 8080 (AUTH_SERVICE_PORT); user-service 8081 (USER_SERVICE_PORT); user gRPC 8082 (USER_SERVICE_GRPC_PORT); shop 8083 (SHOP_SERVICE_PORT); order 8085 (SERVICE_PORT); delivery 8086 (DELIVERY_SERVICE_PORT); file-gateway 8090 (FILE_GATEWAY_SERVICE_PORT). Database.setEnv sets JDBC_DATABASE_URL, DATABASE_USER, DATABASE_USER_PWD, DATABASE_POOL_SIZE, DATABASE_DDL_MODE, SERVICE_PORT per service.

---

## 10. SCALABILITY & FUTURE IMPROVEMENTS

**What scales well**

- Stateless services; JWT + Redis so no server-side session store per node.
- Separate DBs per service reduce coupling and allow independent scaling and schema evolution.
- Inter-service calls are explicit (InterServiceClient, gRPC); new services can be added with own DB and auth contract.

**Possible bottlenecks**

- Single Redis for refresh tokens and blacklist: failure or latency affects all services.
- Auth service is single point for token issuance; no clustering or replication described in code.
- Order creation and cart consume span services (order → shop cart consume); no distributed transaction, failure handling is application-level.
- User-service gRPC used synchronously from auth; if user-service is down, signup/verify and role update fail.

**Reasonable future improvements (without full redesign)**

- Use Redis cluster or sentinel for availability.
- Add read replicas or connection-pool tuning (DB_POOL_SIZE) where needed.
- Introduce API gateway for single entry point, rate limiting, and CORS.
- Move order-service secret (and others) from hardcoded to Config/env.
- Align Redis key naming (e.g. setKey/getKeyValue append `:`; deleteKey does not—can cause mismatch).
- Fix PasswordUtil.checkPwdString regex (remove trailing `\n`) and ensure it is used on signup as well as update.
- Add idempotency for payment webhook/callback and order-from-cart to avoid double charge or double order.

---

*End of document. All content is based on the current codebase; no speculative features are included.*
