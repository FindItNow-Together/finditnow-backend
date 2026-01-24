# Quick Setup: Automatic JWT Configuration

## ✅ What's Been Configured

Your review system now **automatically extracts JWT configuration** from your existing authentication system. No manual entry needed!

## 🚀 Quick Start Options

### Option 1: Environment Variable (Easiest)

Simply set the `JWT_SECRET` environment variable before running:

```bash
# Export your existing JWT secret
export JWT_SECRET="your-existing-secret-from-main-app"

# Run the app
./gradlew bootRun
```

### Option 2: Shared Configuration File

If you have an existing `auth.properties` or similar file:

```bash
# Point to your existing auth config
./gradlew bootRun -Dauth.config.path=/path/to/your/auth.properties
```

### Option 3: Auto-Discovery

Place your auth config in one of these locations, and it will be **automatically found**:

- `config/auth.properties`
- `config/application.properties`
- `~/.config/app/auth.properties`

## 📋 How It Works

1. **Application starts** → `JwtConfig` class runs
2. **Checks environment variables** (`JWT_SECRET`, `JWT_EXPIRATION`)
3. **Looks for external auth config** at specified path
4. **Searches common locations** automatically
5. **Validates configuration** and confirms in logs

You'll see this on successful startup:
```
✓ JWT Secret loaded from environment/properties
✓ JWT Configuration loaded successfully
  - Secret length: 64 characters
  - Expiration: 15 minutes
```

## ⚙️ Files Modified

1. **[application.yml](file:///Users/swarupbhosale/Desktop/Review%20System/src/main/resources/application.yml)** 
   - Now uses `${JWT_SECRET:#{null}}` to load from environment

2. **[JwtConfig.java](file:///Users/swarupbhosale/Desktop/Review%20System/src/main/java/com/reviewsystem/config/JwtConfig.java)** 
   - Automatically loads JWT config from multiple sources

3. **[JwtUtil.java](file:///Users/swarupbhosale/Desktop/Review%20System/src/main/java/com/reviewsystem/util/JwtUtil.java)** 
   - Uses the auto-configured JWT settings for token operations

4. **Both Controllers** updated to use `JwtUtil`

## 🔗 Integration with Existing App

If you're running this alongside your main application:

```bash
# In your main app's startup script or .env file
export JWT_SECRET="your-secret-here"
export JWT_EXPIRATION="900000"

# Both apps will now share the same JWT configuration
./main-app &
cd "Review System" && ./gradlew bootRun &
```

## 📖 Full Documentation

See [JWT_CONFIGURATION_GUIDE.md](file:///Users/swarupbhosale/Desktop/Review%20System/JWT_CONFIGURATION_GUIDE.md) for:
- Multiple integration methods
- Production deployment examples
- Troubleshooting
- Security best practices

## ✨ No Manual Configuration Required!

The system automatically:
- ✅ Extracts JWT secret from your environment
- ✅ Shares configuration with your main app
- ✅ Validates settings on startup
- ✅ Uses the same 15-minute token expiry

Just set `JWT_SECRET` and you're good to go! 🎉
