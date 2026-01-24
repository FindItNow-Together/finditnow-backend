# Troubleshooting Guide

## Common Issues & Solutions

### Frontend Errors: "Cannot find module"

**Problem:** Dependencies not installed

**Solution:**
```bash
cd frontend
npm cache clean --force
npm install --legacy-peer-deps
```

If still having issues:
```bash
# Remove and reinstall
rm -rf node_modules package-lock.json
npm install --legacy-peer-deps
```

---

### Backend Errors: "Package does not match expected package"

**Problem:** IDE workspace configuration

**Solution:** These are just IDE warnings and **won't affect compilation**. The code is correct.

To fix IDE warnings:
1. Open the project in IntelliJ IDEA or VS Code with Java extensions
2. Let it index and configure the project
3. Or ignore - the code will compile and run fine with Gradle

---

### Frontend TypeScript Errors

**Problem:** Missing node_modules

**Solution:** Run `npm install --legacy-peer-deps` in frontend directory

---

### Backend Won't Compile

**Problem:** Missing Gradle wrapper or dependencies

**Solution:**
```bash
# Give execute permission
chmod +x gradlew

# Build project
./gradlew clean build
```

---

### MySQL Connection Failed

**Problem:** Database not created or wrong credentials

**Solution:**
```bash
mysql -u root -p
CREATE DATABASE review_system_db;
```

Then update `src/main/resources/application.yml` with your MySQL credentials.

---

### JWT Secret Not Found

**Problem:** Environment variable not set

**Solution:**
```bash
export JWT_SECRET="your-secret-key-here"
```

Add to `~/.zshrc` or `~/.bashrc` for permanent setup.

---

### Frontend Won't Start

**Solution:**
```bash
cd frontend
rm -rf .next node_modules package-lock.json
npm install --legacy-peer-deps
npm run dev
```

---

### Port Already in Use

**Backend (8080):**
```bash
lsof -ti:8080 | xargs kill -9
```

**Frontend (3000):**
```bash
lsof -ti:3000 | xargs kill -9
```

---

## Quick Health Check

### Backend
```bash
./gradlew build
# Should build successfully
```

### Frontend
```bash
cd frontend
npm run build
# Should build successfully
```

---

## Still Having Issues?

1. **Frontend:** Make sure you're in the `frontend/` directory when running npm commands
2. **Backend:** Make sure Java 17+ is installed: `java -version`
3. **Database:** Make sure MySQL is running: `mysql -u root -p`

Most errors are due to missing dependencies - **npm install will fix frontend issues**.
