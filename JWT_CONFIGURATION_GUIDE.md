# JWT Configuration Guide

## Automatic JWT Configuration

The review system automatically loads JWT configuration from multiple sources, allowing seamless integration with your existing authentication system.

## Configuration Priority (Highest to Lowest)

1. **Environment Variables** (Recommended for production)
2. **System Properties**
3. **External Auth Configuration File**
4. **Common Configuration Locations**
5. **application.yml** (Fallback)

---

## Method 1: Environment Variables (Recommended)

### On macOS/Linux:

```bash
# Set environment variables
export JWT_SECRET="your-existing-jwt-secret-from-auth-system"
export JWT_EXPIRATION="900000"  # 15 minutes in milliseconds

# Then run the application
./gradlew bootRun
```

### Permanent Setup (Add to ~/.zshrc or ~/.bashrc):

```bash
echo 'export JWT_SECRET="your-existing-jwt-secret"' >> ~/.zshrc
echo 'export JWT_EXPIRATION="900000"' >> ~/.zshrc
source ~/.zshrc
```

### In Docker/Production:

```yaml
# docker-compose.yml
services:
  review-system:
    environment:
      - JWT_SECRET=${JWT_SECRET}
      - JWT_EXPIRATION=900000
```

---

## Method 2: External Auth Configuration File

If you have an existing auth configuration file, point to it:

### Create/Use existing auth config file:

**Option A: Point to existing auth.properties**
```bash
# Run with existing auth config
./gradlew bootRun -Dauth.config.path=/path/to/your/auth.properties
```

**Option B: Create config/auth.properties in project root**
```properties
# config/auth.properties
jwt.secret=your-existing-jwt-secret
jwt.expiration=900000
```

The system will automatically find it in these locations:
- `config/auth.properties`
- `config/application.properties`
- `../config/auth.properties`
- `~/.config/app/auth.properties`

---

## Method 3: Extract from Existing Session Configuration

If your main application already creates JWT tokens, you can extract the configuration:

### Example: Share configuration with main app

**In your main application:**
```java
// MainApp JWT Config
@Configuration
public class MainAppJwtConfig {
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    public String getSecret() {
        return jwtSecret;
    }
}
```

**Then create a shared config file:**
```bash
# Create shared-config.properties
echo "jwt.secret=your-secret" > /shared/config/auth.properties
echo "jwt.expiration=900000" >> /shared/config/auth.properties
```

**Point both applications to it:**
```bash
# Main App
java -jar main-app.jar --spring.config.additional-location=file:/shared/config/

# Review System
./gradlew bootRun -Dauth.config.path=/shared/config/auth.properties
```

---

## Method 4: Programmatic Configuration

If you need to extract JWT secret programmatically from your existing auth service:

### Create a custom initializer:

```java
// src/main/java/com/reviewsystem/config/JwtInitializer.java
@Component
public class JwtInitializer implements ApplicationListener<ApplicationReadyEvent> {
    
    @Autowired
    private JwtConfig jwtConfig;
    
    // Inject your existing auth service if needed
    // @Autowired
    // private ExistingAuthService existingAuthService;
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // Extract JWT secret from your existing auth service
        // String secret = existingAuthService.getJwtSecret();
        
        // Or read from database
        // String secret = authConfigRepository.findByKey("jwt.secret");
        
        // The JwtConfig will use it automatically
    }
}
```

---

## Method 5: Integration with Existing Auth Module

If you have a separate auth module/microservice:

### Share via HTTP endpoint:

```java
// In Review System - fetch config on startup
@Component
public class AuthConfigFetcher {
    
    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;
    
    @PostConstruct
    public void fetchJwtConfig() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            AuthConfigResponse config = restTemplate.getForObject(
                authServiceUrl + "/api/auth/config",
                AuthConfigResponse.class
            );
            
            // Set as system properties so JwtConfig picks them up
            System.setProperty("jwt.secret", config.getJwtSecret());
            System.setProperty("jwt.expiration", String.valueOf(config.getJwtExpiration()));
        } catch (Exception e) {
            // Fall back to local configuration
        }
    }
}
```

---

## Verification

After configuration, check the logs on startup:

```
✓ JWT Secret loaded from environment/properties
✓ JWT Configuration loaded successfully
  - Secret length: 64 characters
  - Expiration: 15 minutes
```

---

## Using JWT in Controllers

The JWT configuration is automatically available via `JwtUtil`:

```java
@RestController
public class ExampleController {
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @PostMapping("/test")
    public ResponseEntity<?> test(@RequestHeader("Authorization") String bearerToken) {
        String token = bearerToken.substring(7); // Remove "Bearer "
        
        // Automatically uses the configured secret
        if (jwtUtil.validateToken(token)) {
            Long userId = jwtUtil.getUserIdFromToken(token);
            // Use userId
        }
        
        return ResponseEntity.ok("Success");
    }
}
```

---

## Updating Controllers to Use JwtUtil

Update the `extractUserId` method in both controllers:

```java
// ProductReviewController.java and ShopReviewController.java

@Autowired
private JwtUtil jwtUtil;

private Long extractUserId(Authentication authentication) {
    // Option 1: If using custom UserDetails
    if (authentication.getPrincipal() instanceof YourUserDetails) {
        return ((YourUserDetails) authentication.getPrincipal()).getId();
    }
    
    // Option 2: If userId is in the name field
    return Long.parseLong(authentication.getName());
    
    // Option 3: Extract from JWT token directly (if needed)
    // This would require passing the token to this method
}
```

---

## Security Best Practices

1. **Never commit secrets to Git**
   ```bash
   # Add to .gitignore
   echo "config/auth.properties" >> .gitignore
   echo "*.properties" >> .gitignore
   ```

2. **Use strong secrets** (minimum 256 bits)
   ```bash
   # Generate a secure secret
   openssl rand -base64 64
   ```

3. **Rotate secrets regularly**

4. **Use different secrets for dev/staging/production**

---

## Troubleshooting

### Error: "JWT Secret not configured!"

**Solution:** Set JWT_SECRET environment variable:
```bash
export JWT_SECRET="your-secret-here"
```

### Error: "Could not load auth config from: ..."

**Solution:** Check file path and permissions:
```bash
ls -la config/auth.properties
chmod 600 config/auth.properties  # Secure permissions
```

### JWT token not validating

**Solution:** Ensure both apps use the EXACT same secret:
```bash
# In main app
echo $JWT_SECRET

# In review system
./gradlew bootRun -debug | grep "JWT Secret loaded"
```

---

## Example: Complete Integration

```bash
# 1. Create shared auth config
mkdir -p ~/config
cat > ~/config/auth.properties << EOF
jwt.secret=$(openssl rand -base64 64 | tr -d '\n')
jwt.expiration=900000
EOF

# 2. Set environment variable
export AUTH_CONFIG_PATH=~/config/auth.properties

# 3. Run review system - it will automatically load the config
cd "Review System"
./gradlew bootRun

# 4. Verify in logs
# ✓ JWT Secret loaded from auth config: /Users/you/config/auth.properties
```

Now your review system uses the same JWT configuration as your main application!
