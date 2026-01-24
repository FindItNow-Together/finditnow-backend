# Review System Backend API

## Overview
Complete Spring Boot backend for a review system supporting product reviews and shop reviews with JWT authentication.

## Base URL
```
http://localhost:8080/api
```

## Authentication
All endpoints (except GET endpoints for public reviews) require JWT authentication.

Include the JWT token in the Authorization header:
```
Authorization: Bearer <your_jwt_token>
```

---

## Product Review Endpoints

### 1. Create Product Review
**POST** `/reviews/products`

Create a new review for a product.

**Request Body:**
```json
{
  "productId": 1,
  "orderId": 1,
  "rating": 4.5,
  "productQualityRating": 5,
  "deliveryTimeRating": 4,
  "comment": "Great product, fast delivery!"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "userId": 123,
  "productId": 1,
  "orderId": 1,
  "rating": 4.5,
  "productQualityRating": 5,
  "deliveryTimeRating": 4,
  "comment": "Great product, fast delivery!",
  "status": "PENDING",
  "createdAt": "2026-01-22T17:30:00",
  "updatedAt": "2026-01-22T17:30:00"
}
```

**Validations:**
- Rating: 1.0 - 5.0
- Product quality rating: 1 - 5
- Delivery time rating: 1 - 5
- Comment: Max 2000 characters
- User must have ordered the product
- No duplicate reviews per order

---

### 2. Update Product Review
**PUT** `/reviews/products/{id}`

Update an existing product review (only by owner).

**Request Body:** Same as create

**Response:** `200 OK`

---

### 3. Delete Product Review
**DELETE** `/reviews/products/{id}`

Delete a product review (only by owner).

**Response:** `204 No Content`

---

### 4. Get Product Reviews
**GET** `/reviews/products/{productId}/list`

Get all approved reviews for a product (paginated).

**Query Parameters:**
- `page` (default: 0)
- `size` (default: 10)
- `sort` (default: createdAt,desc) - format: `property,direction`

**Example:**
```
GET /api/reviews/products/1/list?page=0&size=10&sort=rating,desc
```

**Response:** `200 OK`
```json
{
  "content": [
    {
      "id": 1,
      "userId": 123,
      "productId": 1,
      "rating": 4.5,
      "productQualityRating": 5,
      "deliveryTimeRating": 4,
      "comment": "Great product!",
      "status": "APPROVED",
      "createdAt": "2026-01-22T17:30:00",
      "updatedAt": "2026-01-22T17:30:00"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 10
  },
  "totalElements": 25,
  "totalPages": 3
}
```

---

### 5. Get Product Review Statistics
**GET** `/reviews/products/{productId}/stats`

Get aggregated review statistics for a product.

**Response:** `200 OK`
```json
{
  "averageRating": 4.3,
  "totalReviews": 25,
  "approvedReviews": 23,
  "ratingDistribution": {
    "5.0": 10,
    "4.0": 8,
    "3.0": 3,
    "2.0": 1,
    "1.0": 1
  },
  "averageQualityRating": 4.5,
  "averageDeliveryRating": 4.2
}
```

---

### 6. Get My Product Reviews
**GET** `/reviews/products/my-reviews`

Get all reviews created by the current user (requires authentication).

**Query Parameters:** Same as Get Product Reviews

**Response:** `200 OK` (paginated)

---

### 7. Moderate Product Review (Admin)
**PUT** `/reviews/products/{id}/moderate`

Approve or reject a review (admin only).

**Request Body:**
```json
{
  "status": "APPROVED",
  "moderationNote": "Looks good"
}
```

**Response:** `200 OK`

---

### 8. Get Pending Product Reviews (Admin)
**GET** `/reviews/products/pending`

Get all pending reviews for moderation (admin only).

**Query Parameters:** page, size, sort

**Response:** `200 OK` (paginated)

---

## Shop Review Endpoints

### 9. Create Shop Review
**POST** `/reviews/shops`

Create a new review for a shop.

**Request Body:**
```json
{
  "shopId": 1,
  "orderId": 1,
  "rating": 5.0,
  "ownerInteractionRating": 5,
  "shopQualityRating": 5,
  "deliveryTimeRating": 4,
  "comment": "Excellent service!"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "userId": 123,
  "shopId": 1,
  "orderId": 1,
  "rating": 5.0,
  "ownerInteractionRating": 5,
  "shopQualityRating": 5,
  "deliveryTimeRating": 4,
  "comment": "Excellent service!",
  "status": "PENDING",
  "createdAt": "2026-01-22T17:30:00",
  "updatedAt": "2026-01-22T17:30:00"
}
```

**Validations:**
- Rating: 1.0 - 5.0
- Owner interaction rating: 1 - 5
- Shop quality rating: 1 - 5
- Delivery time rating: 1 - 5
- Comment: Max 2000 characters
- User must have ordered from the shop
- No duplicate reviews per order

---

### 10. Update Shop Review
**PUT** `/reviews/shops/{id}`

Update an existing shop review (only by owner).

**Request Body:** Same as create

**Response:** `200 OK`

---

### 11. Delete Shop Review
**DELETE** `/reviews/shops/{id}`

Delete a shop review (only by owner).

**Response:** `204 No Content`

---

### 12. Get Shop Reviews
**GET** `/reviews/shops/{shopId}/list`

Get all approved reviews for a shop (paginated).

**Query Parameters:** page, size, sort

**Response:** `200 OK` (paginated)

---

### 13. Get Shop Review Statistics
**GET** `/reviews/shops/{shopId}/stats`

Get aggregated review statistics for a shop.

**Response:** `200 OK`
```json
{
  "averageRating": 4.7,
  "totalReviews": 30,
  "approvedReviews": 28,
  "ratingDistribution": {
    "5.0": 20,
    "4.0": 6,
    "3.0": 2
  },
  "averageOwnerInteractionRating": 4.8,
  "averageShopQualityRating": 4.6,
  "averageDeliveryRating": 4.5
}
```

---

### 14. Get My Shop Reviews
**GET** `/reviews/shops/my-reviews`

Get all shop reviews created by the current user (requires authentication).

**Query Parameters:** page, size, sort

**Response:** `200 OK` (paginated)

---

### 15. Moderate Shop Review (Admin)
**PUT** `/reviews/shops/{id}/moderate`

Approve or reject a shop review (admin only).

**Request Body:**
```json
{
  "status": "APPROVED",
  "moderationNote": "Approved"
}
```

**Response:** `200 OK`

---

### 16. Get Pending Shop Reviews (Admin)
**GET** `/reviews/shops/pending`

Get all pending shop reviews for moderation (admin only).

**Query Parameters:** page, size, sort

**Response:** `200 OK` (paginated)

---

## Error Responses

### Validation Error (400)
```json
{
  "timestamp": "2026-01-22T17:30:00",
  "status": 400,
  "error": "Validation Failed",
  "errors": {
    "rating": "Rating must be at least 1.0",
    "comment": "Comment must not exceed 2000 characters"
  }
}
```

### Not Found (404)
```json
{
  "timestamp": "2026-01-22T17:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Review not found with id: 123",
  "path": "/api/reviews/products/123"
}
```

### Unauthorized (403)
```json
{
  "timestamp": "2026-01-22T17:30:00",
  "status": 403,
  "error": "Forbidden",
  "message": "You are not authorized to update this review",
  "path": "/api/reviews/products/123"
}
```

### Duplicate Review (409)
```json
{
  "timestamp": "2026-01-22T17:30:00",
  "status": 409,
  "error": "Conflict",
  "message": "You have already reviewed this product for this order",
  "path": "/api/reviews/products"
}
```

---

## Review Status Enum
- `PENDING`: Review is awaiting moderation
- `APPROVED`: Review has been approved and is publicly visible
- `REJECTED`: Review has been rejected

---

## Setup Instructions

### 1. Database Setup
Create a MySQL database:
```sql
CREATE DATABASE review_system_db;
```

Run the migration script:
```sql
SOURCE src/main/resources/db/migration/V1__create_review_tables.sql
```

### 2. Configuration
Update `src/main/resources/application.yml`:
- Set your MySQL username/password
- Configure JWT secret key

### 3. Run Application
```bash
./gradlew bootRun
```

The server will start on `http://localhost:8080`

---

## Integration Notes

### JWT User ID Extraction
The controllers currently extract user ID from `authentication.getName()`. 

**You need to update this** in both controllers:
```java
private Long extractUserId(Authentication authentication) {
    // Replace with your actual implementation
    // Example:
    // return ((CustomUserDetails) authentication.getPrincipal()).getId();
    return Long.parseLong(authentication.getName());
}
```

### Order Validation (TODO)
Currently commented in services. To enable:
1. Create `OrderRepository`
2. Verify user owns the order
3. Verify order contains the product/shop

Example validation:
```java
Order order = orderRepository.findById(orderId)
    .orElseThrow(() -> new OrderNotFoundException("Order not found"));
    
if (!order.getUserId().equals(userId)) {
    throw new UnauthorizedReviewException("Order does not belong to you");
}

// For products: check if order contains the product
// For shops: check if order is from the shop
```

---

## Testing with cURL

### Create Product Review
```bash
curl -X POST http://localhost:8080/api/reviews/products \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "productId": 1,
    "orderId": 1,
    "rating": 4.5,
    "productQualityRating": 5,
    "deliveryTimeRating": 4,
    "comment": "Great product!"
  }'
```

### Get Product Reviews
```bash
curl -X GET "http://localhost:8080/api/reviews/products/1/list?page=0&size=10"
```

### Get Product Stats
```bash
curl -X GET "http://localhost:8080/api/reviews/products/1/stats"
```

---

## Next Steps for Frontend Integration

1. **TypeScript API Client**: Create API service functions for all endpoints
2. **Review Components**: Build UI components for displaying and creating reviews
3. **Rating Stars**: Implement star rating input/display components
4. **Pagination**: Handle paginated responses
5. **Error Handling**: Display validation errors and API errors to users
