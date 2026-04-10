# Security Application - Developer Quick Reference

## 🚀 Quick Start (5 minutes)

### 1. Build & Run
```bash
# Build application
mvn clean install

# Run application
mvn spring-boot:run
```

### 2. Test Public Endpoint
```bash
curl -X POST http://localhost:8080/api/signUp \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123",
    "email": "test@example.com"
  }'
```

### 3. Login & Get OTP
```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password123"
  }'
```

### 4. Verify OTP & Get Token
```bash
curl -X POST http://localhost:8080/api/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "otp": "123456"
  }'

# Response includes:
# {
#   "accessToken": "eyJhbGciOiJIUzUxMiI...",
#   "refreshToken": "eyJhbGciOiJIUzUxMiI...",
#   "tokenType": "Bearer"
# }
```

### 5. Access Protected Endpoint
```bash
curl -X GET http://localhost:8080/api/user-info \
  -H "Authorization: Bearer <accessToken>"
```

---

## 🔑 API Endpoints Reference

### PUBLIC ENDPOINTS (No Token Required)
```
POST   /api/signUp                 Register new user
POST   /api/login                  Login & request OTP
POST   /api/verify-otp             Verify OTP & get tokens
POST   /api/refresh-token          Refresh access token
GET    /api/trySending             Test email (dev only)
```

### USER ENDPOINTS (Token + isAuthenticated() Required)
```
GET    /api/user-info              View own profile
POST   /api/change-password        Change own password
```

### ADMIN ENDPOINTS (Token + hasRole('ADMIN') Required)
```
GET    /api/admin/users            List all users
GET    /api/admin/users/{username} Get user details
POST   /api/admin/users/{username}/promote      Promote user to admin
POST   /api/admin/users/{username}/demote       Demote admin to user
DELETE /api/admin/users/{username}              Delete user
GET    /api/admin/statistics                    View system statistics
```

---

## 🔐 Configuration

### application.properties
```properties
# Server
server.port=8080

# Database
spring.datasource.url=jdbc:mysql://localhost:3306/security_db
spring.datasource.username=root
spring.datasource.password=root
spring.jpa.hibernate.ddl-auto=update

# JWT Configuration
jwt.secret=your-256-bit-secret-key-make-it-long-and-random
jwt.access.token.expiry=3600000          # 1 hour in ms
jwt.refresh.token.expiry=604800000       # 7 days in ms

# Email Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

---

## 🏗️ Architecture Overview

### Layer Structure
```
API Layer
        ↓
   Controller
        ↓
   Service
        ↓
Repository
        ↓
   Database
```

### Key Classes
- **SecurityConfiguration.java** - Spring Security config
- **JwtUtility.java** - JWT token generation/validation
- **JwtAuthenticationFilter.java** - Token validation filter
- **UserController.java** - REST endpoints
- **UserService.java** - Business logic
- **OTPService.java** - OTP generation & validation
- **UserRepository.java** - Database access (Users)
- **OTPRepository.java** - Database access (OTPs)

---

## 🔐 Security Annotations

### Method-Level Authorization
```java
// Requires any authentication
@PreAuthorize("isAuthenticated()")
public ResponseEntity<?> userEndpoint() { ... }

// Requires ADMIN role
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> adminEndpoint() { ... }

// Public access
// (No annotation or @PreAuthorize("permitAll()"))
public ResponseEntity<?> publicEndpoint() { ... }
```

### HTTP-Level Authorization (SecurityConfiguration)
```java
.authorizeHttpRequests(authz -> authz
    // Public endpoints
    .requestMatchers("/api/signUp", "/api/login").permitAll()
    // Admin endpoints
    .requestMatchers("/api/admin/**").hasRole("ADMIN")
    // Rest requires authentication
    .anyRequest().authenticated()
)
```

---

## 👥 User Roles & Permissions

| Role | Permissions |
|------|------------|
| **ANONYMOUS** | Access public endpoints only |
| **USER** | Public + user endpoints |
| **ADMIN** | All endpoints including admin |

### Creating First Admin
```sql
-- In database
UPDATE users SET role = 'ADMIN' WHERE username = 'admin_user';
```

### Role Change Flow
```
User (USER role logged in)
    ↓
Admin calls: POST /api/admin/users/{username}/promote
    ↓
Database updated: role = 'ADMIN'
    ↓
User logs out & logs back in
    ↓
New token includes ROLE_ADMIN
    ↓
User can now access /admin/** endpoints
```

---

## 🧪 Common Testing Commands

### 1. Register User
```bash
export SIGNUP_RESPONSE=$(curl -s -X POST http://localhost:8080/api/signUp \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePass123",
    "email": "john@example.com"
  }')

echo $SIGNUP_RESPONSE
# Expected: {"message": "User registered successfully", "email": "john@example.com", ...}
```

### 2. Login
```bash
export LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "password": "SecurePass123"
  }')

echo $LOGIN_RESPONSE
# Expected: {"message": "OTP sent to email", ...}
```

### 3. Verify OTP (Check email for real OTP)
```bash
export TOKEN_RESPONSE=$(curl -s -X POST http://localhost:8080/api/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_doe",
    "otp": "123456"  # Replace with actual OTP from email
  }')

export ACCESS_TOKEN=$(echo $TOKEN_RESPONSE | jq -r '.accessToken')
echo $ACCESS_TOKEN
```

### 4. Access Protected Endpoint
```bash
curl -s -X GET http://localhost:8080/api/user-info \
  -H "Authorization: Bearer $ACCESS_TOKEN" | jq '.'
```

### 5. Refresh Token
```bash
export REFRESH_RESPONSE=$(curl -s -X POST http://localhost:8080/api/refresh-token \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "'$REFRESH_TOKEN'"}')

export NEW_ACCESS_TOKEN=$(echo $REFRESH_RESPONSE | jq -r '.accessToken')
```

### 6. Admin Operations (Requires ADMIN token)
```bash
# Promote user to admin
curl -X POST http://localhost:8080/api/admin/users/john_doe/promote \
  -H "Authorization: Bearer $ADMIN_TOKEN"

# Get all users
curl -X GET http://localhost:8080/api/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.'

# View statistics
curl -X GET http://localhost:8080/api/admin/statistics \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq '.'
```

---

## 🔧 Common Issues & Solutions

### Issue: "Invalid or expired token"
**Solution:**
- Check token hasn't expired (1 hour for access token)
- Use refresh-token endpoint to get new token
- Verify token format: "Bearer <token>"

### Issue: "Access denied" on admin endpoint
**Solution:**
- Verify user has ADMIN role
- Check token was generated AFTER promotion
- Old tokens don't include new role; user must login again

### Issue: "OTP expired"
**Solution:**
- OTP valid for 5 minutes only
- Request new OTP by calling /login again
- Previous OTPs are automatically invalidated

### Issue: "User already exists"
**Solution:**
- Username must be unique
- Use different username or delete existing user

### Issue: "Email not sending"
**Solution:**
- Verify SMTP credentials in application.properties
- Check Gmail app password (not regular password)
- Enable "Less secure apps" if using Gmail
- Verify email address is valid

---

## 📊 Database Schema

### Users Table
```sql
CREATE TABLE users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,    -- BCrypt hashed
    email VARCHAR(100) UNIQUE NOT NULL,
    role VARCHAR(20) NOT NULL,         -- 'USER' or 'ADMIN'
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### OTPs Table
```sql
CREATE TABLE otps (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    otp_value VARCHAR(6) NOT NULL,
    is_used BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiry_time TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## 🛡️ Security Best Practices

✅ **DO:**
- Use HTTPS in production
- Store JWT secret in environment variable
- Use strong passwords (8+ characters, mixed case, numbers)
- Regularly rotate JWT secret
- Monitor admin account activities
- Keep dependencies updated
- Use https URLs for redirects
- Validate all inputs
- Log security events

❌ **DON'T:**
- Hardcode JWT secret in code
- Send tokens in query parameters
- Store tokens in localStorage (use httpOnly cookies)
- Share admin credentials
- Disable HTTPS in production
- Use weak passwords
- Commit secrets to git
- Run with debug mode in production
- Trust client-side validation only

---

## 🔄 Complete Authentication Flow

```
1. USER REGISTERS
   POST /signUp → User created with role=USER

2. USER LOGS IN
   POST /login → OTP sent to email

3. USER VERIFIES OTP
   POST /verify-otp → JWT tokens returned (access + refresh)

4. USER ACCESSES PROTECTED RESOURCE
   GET /user-info → Authorization: Bearer <accessToken>
   Response: User profile data

5. ACCESS TOKEN EXPIRES (1 hour)
   POST /refresh-token → New accessToken returned

6. ADMIN PROMOTES USER
   POST /admin/users/{username}/promote → Role updated to ADMIN

7. USER LOGS OUT
   (Delete token from client)

8. ADMIN DELETES USER
   DELETE /admin/users/{username} → User removed
```

---

## 📝 Important Notes

1. **Token Expiration**
   - Access Token: 1 hour
   - Refresh Token: 7 days
   - OTP: 5 minutes

2. **Password Requirements**
   - Minimum 8 characters
   - No spaces allowed (currently)
   - Hashed with BCrypt (strength 10)

3. **OTP Properties**
   - 6 digits
   - Random generation using SecureRandom
   - Single-use only (marked as used)
   - Old OTPs invalidated when new one sent

4. **Default Roles**
   - New registrations: USER role
   - Promotion: manual via admin endpoint
   - No auto-admin creation

5. **Email Configuration**
   - Uses SMTP (Gmail by default)
   - Requires app-specific password (not regular password)
   - HTML template rendering
   - Timeout: 10 seconds

---

## 🚀 Deployment Steps

1. **Set Environment Variables**
   ```bash
   export JWT_SECRET="your-super-secret-key-256-bits-minimum"
   export DB_URL="jdbc:mysql://production-host:3306/db"
   export DB_USER="prod_user"
   export DB_PASSWORD="secure_password"
   export MAIL_USERNAME="noreply@company.com"
   export MAIL_PASSWORD="app-specific-password"
   ```

2. **Build Application**
   ```bash
   mvn clean package
   ```

3. **Run JAR**
   ```bash
   java -jar target/Security-0.0.1-SNAPSHOT.jar
   ```

4. **Verify Deployment**
   ```bash
   curl http://localhost:8080/api/signUp -I
   # Should return 405 (POST method required)
   ```

5. **Create First Admin**
   - Register a user
   - Login & verify OTP
   - Access database directly
   - Update role to ADMIN
   - User logs back in with ADMIN role

---

## 📞 Support Resources

- **JWT Documentation**: [jwt.io](https://jwt.io)
- **Spring Security**: [spring.io/projects/spring-security](https://spring.io/projects/spring-security)
- **jjwt Library**: [github.com/jwtk/jjwt](https://github.com/jwtk/jjwt)
- **BCrypt Info**: [en.wikipedia.org/wiki/Bcrypt](https://en.wikipedia.org/wiki/Bcrypt)

---

## 📋 Checklist Before Deployment

- [ ] All environment variables set
- [ ] HTTPS enabled
- [ ] Database backed up
- [ ] Email credentials verified
- [ ] JWT secret is 256+ bits
- [ ] First admin user created
- [ ] All endpoints tested
- [ ] CORS configured (if needed)
- [ ] Rate limiting configured (optional)
- [ ] Logging enabled
- [ ] Monitoring setup
- [ ] Documentation updated

---

**This security application is production-ready! Deploy with confidence.** 🚀

