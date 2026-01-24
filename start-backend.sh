#!/bin/bash

echo "🚀 Review System - Backend Startup"
echo "==================================="
echo ""

# Set environment variables
export JAVA_HOME=$(/usr/libexec/java_home -v 18)
export JWT_SECRET="your-secret-key-for-development-only-12345"

echo "Starting Spring Boot backend..."
echo "Backend will be available at: http://localhost:8080/api"
echo ""

# Use Gradle 8 (installed via brew install gradle@8)
/opt/homebrew/opt/gradle@8/bin/gradle bootRun
