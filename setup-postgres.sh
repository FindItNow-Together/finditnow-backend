#!/bin/bash

echo "🐘 PostgreSQL Setup for Review System"
echo "======================================"
echo ""

# Check if PostgreSQL is installed
if ! command -v psql &> /dev/null; then
    echo "❌ PostgreSQL is not installed"
    echo ""
    echo "Install it with Homebrew:"
    echo "   brew install postgresql@15"
    echo "   brew services start postgresql@15"
    exit 1
fi

echo "✅ PostgreSQL found"
echo ""

# Check if PostgreSQL is running
if ! brew services list | grep postgresql | grep started > /dev/null; then
    echo "Starting PostgreSQL..."
    brew services start postgresql
    sleep 3
fi

echo "✅ PostgreSQL is running"
echo ""

# Create database and user
echo "Creating database 'review_system_db'..."

# Try to create database (postgres user usually has no password by default on Mac)
psql postgres << EOF
CREATE DATABASE review_system_db;
CREATE USER postgres WITH PASSWORD 'postgres';
GRANT ALL PRIVILEGES ON DATABASE review_system_db TO postgres;
ALTER DATABASE review_system_db OWNER TO postgres;
\q
EOF

if [ $? -eq 0 ]; then
    echo "✅ Database created successfully!"
else
    echo "⚠️  Database may already exist, trying to connect..."
    if psql -U postgres -d review_system_db -c "SELECT 1;" 2>/dev/null; then
        echo "✅ Database exists and is accessible!"
    else
        echo "❌ Could not connect to database"
        exit 1
    fi
fi

echo ""
echo "🎉 PostgreSQL is ready!"
echo ""
echo "Database: review_system_db"
echo "Username: postgres"
echo "Password: postgres"
echo "Port: 5432"
echo ""
echo "Start your backend with:"
echo "   export JWT_SECRET=\"your-secret-key-for-development-only-12345\""
echo "   ./gradlew bootRun"
