# Calculator Collector REST API - Client Integration Guide

## Server Configuration Summary

### Base URL
- **Default Port**: 8080
- **Base Path**: `/api`
- **Full Base URL**: `http://<server-ip>:8080/api`

### For Android Emulator
- Use `http://10.0.2.2:8080/api` to connect to the host machine's localhost

---

## Spring Security Configuration

### Authentication Method
- **HTTP Basic Authentication** is enabled for all `/api/**` endpoints
- **CSRF Protection**: Disabled for `/api/**` endpoints (no CSRF token required)
- **CORS**: Enabled for `/api/**` endpoints (allows all origins)

### Security Rules
1. **Public Endpoints** (no authentication):
   - `/`, `/welcome`, `/login`, `/error`
   - `/calculators`, `/calculators/**`
   - `/uploads/**`
   - `/share/**`

2. **Authenticated Endpoints** (require HTTP Basic Auth):
   - `/api/**` - All REST API endpoints require authentication
   - `/profile/**` - User profile pages

3. **Admin Only Endpoints**:
   - `/admin/**` - Requires ADMIN role

### HTTP Basic Authentication Format
- **Header Name**: `Authorization`
- **Header Value**: `Basic <base64-encoded-credentials>`
- **Encoding**: Base64 encode `username:password`
- **Example**: 
  - Username: `admin`, Password: `admin`
  - Encoded: `YWRtaW46YWRtaW4=`
  - Header: `Authorization: Basic YWRtaW46YWRtaW4=`

### Password Encoding
- Passwords are stored using **BCrypt** hashing
- The server will verify the provided password against the BCrypt hash

---

## REST API Structure

### Standard Response Format
All API endpoints return responses in this format:

```json
{
  "success": true/false,
  "message": "Optional success message",
  "data": { /* Response data */ },
  "error": "Error message if success is false"
}
```

### HTTP Status Codes
- **200 OK**: Request successful
- **400 BAD_REQUEST**: Invalid request parameters
- **401 UNAUTHORIZED**: Authentication required or failed
- **404 NOT_FOUND**: Resource not found
- **500 INTERNAL_SERVER_ERROR**: Server error

---

## Available REST API Endpoints

### 1. User Profile API (`/api/user`)

#### GET `/api/user/profile`
- **Authentication**: Required (HTTP Basic Auth)
- **Description**: Get the authenticated user's profile information
- **Response**: `ApiResponse<UserProfileDTO>`
- **UserProfileDTO Structure**:
  ```json
  {
    "id": 1,
    "username": "admin",
    "email": "admin@example.com",
    "role": "ADMIN",
    "enabled": true,
    "createdAt": "2025-01-01T10:00:00",
    "lastLogin": "2025-01-02T14:00:00"
  }
  ```

#### PUT `/api/user/profile/email`
- **Authentication**: Required
- **Parameters**: `email` (query parameter)
- **Response**: `ApiResponse<Void>`

#### PUT `/api/user/profile/password`
- **Authentication**: Required
- **Parameters**: `oldPassword`, `newPassword` (query parameters)
- **Response**: `ApiResponse<Void>`

---

### 2. Calculators API (`/api/calculators`)

#### GET `/api/calculators`
- **Authentication**: Not required (public endpoint)
- **Query Parameters**:
  - `search` (optional): Search term for model or manufacturer
  - `manufacturerId` (optional): Filter by manufacturer ID
  - `page` (default: 0): Page number (0-indexed)
  - `size` (default: 20): Page size
- **Response**: `ApiResponse<Page<CalculatorDTO>>`

#### GET `/api/calculators/{id}`
- **Authentication**: Not required (public endpoint)
- **Response**: `ApiResponse<CalculatorDTO>`

---

### 3. Collection API (`/api/collection`)

#### GET `/api/collection`
- **Authentication**: Required
- **Query Parameters**: `page` (default: 0), `size` (default: 20)
- **Response**: `ApiResponse<Page<CalculatorDTO>>`

#### POST `/api/collection/{calculatorId}`
- **Authentication**: Required
- **Description**: Add calculator to user's collection

#### DELETE `/api/collection/{calculatorId}`
- **Authentication**: Required
- **Description**: Remove calculator from user's collection

#### GET `/api/collection/statistics`
- **Authentication**: Required
- **Response**: `ApiResponse<CollectionStatisticsDTO>`

---

### 4. Other API Endpoints

All other endpoints follow similar patterns:
- `/api/labels/**` - Label management
- `/api/images/**` - Image management
- `/api/links/**` - Link management
- `/api/proposals/**` - Calculator proposals
- `/api/share/**` - Collection sharing
- `/api/quota/status` - Quota status
- `/api/calculators/{id}/social-share/**` - Social media post generation

**All require HTTP Basic Authentication except `/api/calculators` endpoints.**

---

## Client-Side Implementation Checklist

### 1. HTTP Client Configuration
- [ ] Use HTTP client that supports HTTP Basic Authentication (OkHttp, Retrofit, etc.)
- [ ] Set timeout values appropriately (default 10 seconds may be too short)
- [ ] Configure connection timeout (suggest 30-60 seconds for initial connection)
- [ ] Configure read timeout (suggest 30 seconds)

### 2. Authentication Setup
- [ ] Encode credentials: `Base64.encode("username:password")`
- [ ] Add header: `Authorization: Basic <encoded-credentials>`
- [ ] Include header in ALL requests to `/api/**` endpoints
- [ ] Handle 401 responses (authentication failed)

### 3. Network Configuration
- [ ] Verify server is running on port 8080
- [ ] For Android Emulator: Use `http://10.0.2.2:8080`
- [ ] For Physical Device: Use your computer's IP address (e.g., `http://192.168.1.100:8080`)
- [ ] Check firewall allows port 8080
- [ ] Verify server is accessible from network (not just localhost)

### 4. Error Handling
- [ ] Handle `SocketTimeoutException` (connection timeout)
- [ ] Handle `401 UNAUTHORIZED` (authentication failed)
- [ ] Handle `404 NOT_FOUND` (endpoint not found)
- [ ] Parse `ApiResponse` wrapper structure
- [ ] Check `success` field before accessing `data`

---

## Troubleshooting Connection Issues

### Connection Timeout Symptoms
- `SocketTimeoutException: failed to connect to /10.0.2.2 (port 8080) after 10000ms`
- No response received within timeout period

### Possible Causes & Solutions

#### 1. Server Not Running
- **Check**: Verify Spring Boot application is running
- **Solution**: Start the server with `mvn spring-boot:run` or run the JAR file

#### 2. Server Not Accessible from Network
- **Check**: Test from browser: `http://localhost:8080/api/user/profile` (should prompt for credentials)
- **Check**: Test from command line: `curl http://localhost:8080/api/user/profile -u admin:admin`
- **Solution**: Ensure server binds to all interfaces (default Spring Boot behavior)

#### 3. Firewall Blocking Port 8080
- **Check**: Windows Firewall may be blocking incoming connections
- **Solution**: Add firewall rule to allow port 8080 or temporarily disable firewall for testing

#### 4. Wrong IP Address
- **Android Emulator**: Must use `10.0.2.2` (special IP for host machine)
- **Physical Device**: Use your computer's actual IP address (check with `ipconfig` on Windows)
- **Solution**: Verify IP address matches your network configuration

#### 5. Server Binding to Localhost Only
- **Check**: Server logs should show: `Tomcat started on port(s): 8080 (http)`
- **Solution**: If server only binds to 127.0.0.1, add to `application.properties`:
  ```
  server.address=0.0.0.0
  ```

#### 6. Authentication Header Not Sent
- **Check**: Verify `Authorization` header is included in request
- **Check**: Verify Base64 encoding is correct
- **Solution**: Use HTTP client interceptor or authenticator to add header automatically

---

## Example Request (cURL)

```bash
# Test user profile endpoint
curl -X GET "http://10.0.2.2:8080/api/user/profile" \
  -H "Authorization: Basic YWRtaW46YWRtaW4=" \
  -H "Content-Type: application/json"

# Expected successful response:
# {
#   "success": true,
#   "message": null,
#   "data": {
#     "id": 1,
#     "username": "admin",
#     "email": "admin@example.com",
#     "role": "ADMIN",
#     "enabled": true,
#     "createdAt": "2025-01-01T10:00:00",
#     "lastLogin": "2025-01-02T14:00:00"
#   },
#   "error": null
# }
```

---

## Example Request (OkHttp - Android)

```kotlin
val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

val credentials = Credentials.basic("admin", "admin")
val request = Request.Builder()
    .url("http://10.0.2.2:8080/api/user/profile")
    .header("Authorization", credentials)
    .get()
    .build()

val response = client.newCall(request).execute()
```

---

## Debugging Steps

1. **Test Server Accessibility**:
   - From Android device/emulator browser, try: `http://10.0.2.2:8080/`
   - Should see the welcome page (if accessible)

2. **Test API Endpoint**:
   - From Android device/emulator browser, try: `http://10.0.2.2:8080/api/user/profile`
   - Should prompt for username/password (if server is accessible)

3. **Check Server Logs**:
   - Look for incoming connection attempts
   - If no logs appear, connection isn't reaching the server
   - If 401 errors appear, authentication is the issue (not connection)

4. **Verify Network Configuration**:
   - Android Emulator: `10.0.2.2` is correct
   - Physical Device: Use `ipconfig` to find your computer's IP
   - Ensure both devices are on the same network

5. **Test with Postman/Insomnia**:
   - Use desktop HTTP client to verify server is working
   - Test with same credentials and endpoint
   - Compare request/response with Android app

---

## Common Issues

### Issue: Connection Timeout
- **Cause**: Server not accessible from network
- **Fix**: Check firewall, verify server is running, verify IP address

### Issue: 401 Unauthorized
- **Cause**: Missing or incorrect Authorization header
- **Fix**: Verify Base64 encoding, ensure header is included

### Issue: 404 Not Found
- **Cause**: Wrong endpoint URL
- **Fix**: Verify endpoint path matches exactly (case-sensitive)

### Issue: 500 Internal Server Error
- **Cause**: Server-side error
- **Fix**: Check server logs for detailed error message

---

## Additional Notes

- All timestamps are in ISO 8601 format (e.g., `2025-01-02T14:00:00`)
- Pagination uses 0-based indexing
- All IDs are Long integers
- Boolean values are JSON booleans (true/false)
- Empty responses use `null` for data field
- Error responses have `success: false` and error message in `error` field







