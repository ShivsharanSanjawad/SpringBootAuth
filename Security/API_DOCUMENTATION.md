# Security Application - API Documentation

## Overview
This Spring Boot application implements a two-factor authentication system using OTP (One-Time Password) and JWT tokens.

## Features Implemented
1. **User Registration** - Register new users with username and password
2. **Login with OTP** - Two-step login process
3. **JWT Token Management** - Access and refresh tokens
4. **OTP Management** - Generate, validate, and expire OTPs
5. **Security Configuration** - Spring Security with JWT filters

## API Endpoints

### Base URL
```
http://localhost:8080/api
```

### 1. User Registration
**Endpoint:** `POST /api/signUp`

**Request Body:**
```json
{
  "username": "john_doe",
  "password": "SecurePass123",
  "email": "john@example.com"
}
```

**Response (201 Created):**
```json
{
  "message": "User registered successfully",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "username": "john_doe",
  "email": "john@example.com"
}
```

**Error Responses:**
- `400 Bad Request` - Invalid username/password/email
- `409 Conflict` - Username already exists

---

### 2. Send OTP (New - OTP-based Login Step 1)
**Endpoint:** `POST /api/send-otp`

**Request Body:**
```json
{
  "username": "john_doe"
}
```

**Response (200 OK):**
```json
{
  "message": "OTP sent successfully to registered email"
}
```

**Process:**
1. User provides username
2. System validates user exists
3. Generates 6-digit OTP with 5-minute expiry
4. Sends OTP to registered email
5. User checks email for OTP

**Error Responses:**
- `400 Bad Request` - User not found or email service unavailable
- `500 Internal Server Error` - Failed to send OTP

---

### 3. Verify OTP (New - OTP-based Login Step 2)
**Endpoint:** `POST /api/verify-otp`

**Request Body:**
```json
{
  "username": "john_doe",
  "otp": 123456
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "expiresIn": 3600000,
  "username": "john_doe"
}
```

**Token Details:**
- `accessToken`: Valid for 1 hour (3600000 ms)
- `refreshToken`: Valid for 7 days (604800000 ms)
- Use in Authorization header: `Authorization: Bearer <accessToken>`

**Error Responses:**
- `401 Unauthorized` - Invalid or expired OTP

---

### 4. Login (Direct - Without OTP)
**Endpoint:** `POST /api/login`

**Request Body:**
```json
{
  "username": "john_doe",
  "password": "SecurePass123"
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "expiresIn": 3600000,
  "username": "john_doe"
}
```

**Process:**
1. User provides username and password
2. System validates credentials
3. Returns JWT tokens immediately (no OTP verification)

**Note:** This is the direct login method. For enhanced security, use the OTP flow (`/api/send-otp` → `/api/verify-otp`) instead.

**Error Responses:**
- `401 Unauthorized` - Invalid credentials

---

### 5. Refresh Token
**Endpoint:** `POST /api/refresh-token`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9..."
}
```

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzUxMiJ9...",
  "refreshToken": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600000,
  "username": "john_doe"
}
```

**Use Cases:**
- Obtain new access token without re-authenticating
- Refresh both access and refresh tokens
- Automatically handle token expiration

**Error Responses:**
- `401 Unauthorized` - Invalid refresh token

---

### 6. Logout
**Endpoint:** `POST /api/logout`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Response (200 OK):**
```json
{
  "message": "Logged out successfully"
}
```

**Process:**
1. Extracts the access token from the Authorization header
2. Adds the token to a blacklist with its expiration time
3. The token cannot be used for authentication after logout
4. Token remains blacklisted until its natural expiration

**Important Notes:**
- This endpoint invalidates the current access token only
- The refresh token is NOT automatically invalidated
- For complete logout, the client should also:
  - Delete stored tokens from local storage/cookies
  - Invalidate the refresh token separately if needed
- Once logged out, the same access token cannot be used again
- The blacklisted token is automatically removed from the database after its expiration time

**Error Responses:**
- `400 Bad Request` - Invalid token format
- `401 Unauthorized` - Missing or invalid authorization header
- `500 Internal Server Error` - Logout failed

---

### 7. Get User Info
**Endpoint:** `GET /api/user-info`

**Headers:**
```
Authorization: Bearer <accessToken>
```

**Response (200 OK):**
```json
{
  "username": "john_doe",
  "authorities": [
    {
      "authority": "ROLE_USER"
    }
  ]
}
```

**Error Responses:**
- `401 Unauthorized` - Invalid or missing token

---

### 8. Test Email Sending
**Endpoint:** `GET /api/trySending`

**Response (200 OK):**
```json
{}
```

Used for testing email configuration.

---

## Authentication Flow

### OTP-Based Login Flow (Recommended)

```
1. User Registration
   POST /api/signUp
   ├─ Validate input
   ├─ Hash password with BCrypt
   ├─ Save user to database
   └─ Return JWT tokens for immediate access

2. Send OTP
   POST /api/send-otp
   ├─ Validate user exists
   ├─ Generate OTP (6 digits)
   ├─ Invalidate old OTPs
   ├─ Store OTP in database with 5-minute expiry
   ├─ Send OTP via email
   └─ Return success message

3. Verify OTP
   POST /api/verify-otp
   ├─ Validate OTP exists and not expired
   ├─ Mark OTP as used
   ├─ Generate access token (1 hour expiry)
   ├─ Generate refresh token (7 days expiry)
   └─ Return both tokens

4. Access Protected Resources
   GET /api/user-info (with Authorization header)
   ├─ JwtAuthenticationFilter validates token
   ├─ Extract username from token
   ├─ Load user authorities
   ├─ Set security context
   └─ Allow access to protected resource

5. Token Refresh
   POST /api/refresh-token
   ├─ Validate refresh token
   ├─ Generate new access token
   ├─ Optionally generate new refresh token
   └─ Return updated tokens

6. Logout
   POST /api/logout (with Authorization header)
   ├─ Extract access token from header
   ├─ Get token expiration time
   ├─ Add token to blacklist
   ├─ JwtAuthenticationFilter will reject blacklisted token
   └─ Return success message
```

---

## Security Features

### Password Encryption
- **Algorithm:** BCrypt
- **Strength:** 10 (adaptive strengthening)

### JWT Token Security
- **Algorithm:** HS512 (HMAC with SHA-512)
- **Signing Key:** 256+ bit secret from application.properties
- **Claims:** username, issued time, expiration time

### OTP Security
- **Length:** 6 digits
- **Expiry:** 5 minutes
- **Database Storage:** Marked as used after validation
- **No Reuse:** Once used, OTP cannot be reused
- **Invalidation:** Old OTPs invalidated when new OTP is sent

### Session Management
- **Stateless:** No server-side sessions
- **CSRF Protection:** Disabled for JWT-based API
- **Authentication:** Token-based via JWT

---

## Configuration (application.properties)

```properties
# JWT Configuration
jwt.secret=thisIsAVeryLongSecretKeyForJWTSigningThatShouldBeAtLeast256BitsOrMoreForHS512Algorithm
jwt.expiration=3600000          # 1 hour in milliseconds
jwt.refresh.expiration=604800000  # 7 days in milliseconds

# Database Configuration
spring.datasource.url=jdbc:mysql://localhost:3306/security
spring.datasource.username=root
spring.datasource.password=password
spring.jpa.hibernate.ddl-auto=update

# Mail Configuration
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true

# Server Configuration
server.port=8080
server.servlet.context-path=/api
```

---

## Database Schema

### Users Table
```sql
CREATE TABLE users (
  id VARCHAR(36) PRIMARY KEY,
  username VARCHAR(255) UNIQUE NOT NULL,
  password VARCHAR(255) NOT NULL,
  role ENUM('ADMIN', 'USER') DEFAULT 'USER'
);
```

### OTP Table
```sql
CREATE TABLE otp (
  id VARCHAR(36) PRIMARY KEY,
  user_id VARCHAR(36) NOT NULL,
  otp_value INT NOT NULL,
  expiry_time TIMESTAMP NOT NULL,
  is_used BOOLEAN DEFAULT FALSE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## Error Handling

### Common Error Responses

**400 - Bad Request**
```json
{
  "error": "Username cannot be empty"
}
```

**401 - Unauthorized**
```json
{
  "error": "Invalid credentials"
}
```

**409 - Conflict**
```json
{
  "error": "Username already exists"
}
```

**500 - Internal Server Error**
```json
{
  "error": "An unexpected error occurred"
}
```

---

## Testing the API

### Using cURL

**1. Register User**
```bash
curl -X POST http://localhost:8080/api/signUp \
  -H "Content-Type: application/json" \
  -d '{
    "username":"john_doe",
    "password":"SecurePass123",
    "email":"john@example.com"
  }'
```

**2. Login**
```bash
curl -X POST http://localhost:8080/api/login \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","password":"SecurePass123"}'
```

**3. Verify OTP** (use OTP from email)
```bash
curl -X POST http://localhost:8080/api/verify-otp \
  -H "Content-Type: application/json" \
  -d '{"username":"john_doe","otp":123456}'
```

**4. Get User Info**
```bash
curl -X GET http://localhost:8080/api/user-info \
  -H "Authorization: Bearer <accessToken>"
```

**5. Refresh Token**
```bash
curl -X POST http://localhost:8080/api/refresh-token \
  -H "Content-Type: application/json" \
  -d '{"refreshToken":"<refreshToken>"}'
```

---

## Architecture Overview

### Components

1. **Controller Layer** (UserController)
   - Handles HTTP requests
   - Request validation
   - Response formatting

2. **Service Layer** (UserService, OTPService)
   - Business logic
   - Database operations
   - Email sending

3. **Security Layer**
   - CustomUsernamePasswordAuthProvider - Username/Password authentication
   - CustomOTPAuthProvider - OTP validation
   - JwtAuthenticationFilter - JWT token validation
   - SecurityConfiguration - Spring Security setup

4. **Persistence Layer**
   - UserRepository
   - OTPRepository
   - JPA entities

5. **Utilities**
   - JwtUtility - JWT generation and validation
   - mailService - Email operations

---

## Dependencies

- **Spring Boot 3.5.11**
- **Spring Security 6.x**
- **Spring Data JPA**
- **MySQL Connector**
- **jjwt 0.12.3** - JWT library
- **Lombok** - Annotation processing
- **Spring Mail** - Email sending

---

## Future Enhancements

1. Two-factor authentication with time-based OTP (TOTP)
2. Login attempt limiting and account lockout
3. Password reset functionality
4. User profile management
5. Role-based access control (RBAC)
6. Audit logging
7. API rate limiting
8. Redis integration for token blacklisting

---

## Troubleshooting

### Email not sending
- Check email credentials in application.properties
- Enable "Less secure app access" for Gmail
- Use app-specific passwords for Gmail

### Database connection failed
- Verify MySQL is running
- Check database credentials
- Ensure database "security" exists

### Token validation failing
- Check JWT secret in application.properties (must be 256+ bits)
- Verify token format: "Bearer <token>"
- Check token expiration

---

## Author
Developed as a comprehensive security solution for HackFusion platform.

