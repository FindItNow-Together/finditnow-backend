# Review System

A complete, production-ready review system built with Spring Boot and Next.js.

## 🚀 Quick Start

### Prerequisites
- Java 18
- Node.js 18+
- PostgreSQL 15
- Gradle 8 (installed via `brew install gradle@8`)

### 1. Database Setup

```bash
./setup-postgres.sh
```

This creates the PostgreSQL database `review_system_db` with user `postgres/postgres`.

### 2. Start Backend

```bash
./start-backend.sh
```

Backend runs at: **http://localhost:8080/api**

### 3. Start Frontend

```bash
cd frontend
npm install  # First time only
npm run dev
```

Frontend runs at: **http://localhost:3000**

## 📦 What's Included

### Backend (Spring Boot + PostgreSQL)
- **Product Reviews** - Multi-criteria ratings (quality, delivery)
- **Shop Reviews** - Owner interaction, shop quality, delivery ratings
- **RESTful API** - 18 endpoints for reviews, products, and shops
- **Auto-seeding** - 4 products & 3 shops populated on first run
- **PostgreSQL Database** - Persistent storage

### Frontend (Next.js + TypeScript)
- **Product Browsing** - View products with ratings and review stats
- **Shop Browsing** - Explore shops and their ratings
- **Review Submission** - Write reviews with multi-criteria ratings
- **My Reviews** - Track all your submitted reviews
- **Real-time Stats** - See rating calculations update instantly

## 📚 Documentation

- **[API_DOCUMENTATION.md](./API_DOCUMENTATION.md)** - Complete API reference
- **[POSTGRES_READY.md](./POSTGRES_READY.md)** - Database setup details
- **[JWT_CONFIGURATION_GUIDE.md](./JWT_CONFIGURATION_GUIDE.md)** - JWT configuration
- **[TROUBLESHOOTING.md](./TROUBLESHOOTING.md)** - Common issues and fixes

## 🎯 Features

- ✅ Multi-criteria review ratings
- ✅ Real-time statistics calculation
- ✅ Review moderation system
- ✅ User authentication ready (JWT configured)
- ✅ Responsive UI design
- ✅ PostgreSQL persistence
- ✅ Auto-seeded demo data

## 🛠️ Tech Stack

**Backend:**
- Spring Boot 3.2.1
- PostgreSQL 15
- JPA/Hibernate
- JWT Authentication
- Lombok

**Frontend:**
- Next.js 14
- TypeScript
- Tailwind CSS
- React

## 📝 Database Schema

- `products` - Product information
- `shops` - Shop information
- `product_reviews` - Product reviews with ratings
- `shop_reviews` - Shop reviews with ratings

All tables auto-created on first backend startup!

## 🔧 Configuration

### Database
Edit `src/main/resources/application.yml`:
```yaml
datasource:
  url: jdbc:postgresql://localhost:5432/review_system_db
  username: postgres
  password: postgres
```

### Frontend API
Edit `frontend/lib/api.ts`:
```typescript
const API_BASE_URL = 'http://localhost:8080/api';
```

## 📄 License

This project is open source and available under the MIT License.

---

**Enjoy your Review System!** 🎉
