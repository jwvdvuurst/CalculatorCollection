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
   
   **Note**: Some endpoints like `/api/calculators/{calculatorId}/links` and `/api/calculators/{calculatorId}/labels` (GET methods) may work without authentication, but it's recommended to always include authentication headers for consistency.

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
- **Authentication**: Required (HTTP Basic Auth)
- **Query Parameters**:
  - `search` (optional): Search term for model or manufacturer
  - `manufacturerId` (optional): Filter by manufacturer ID
  - `page` (default: 0): Page number (0-indexed)
  - `size` (default: 20): Page size
- **Response**: `ApiResponse<Page<CalculatorDTO>>`

#### GET `/api/calculators/{id}`
- **Authentication**: Required (HTTP Basic Auth)
- **Description**: Get detailed information about a specific calculator including labels, images, and links
- **Response**: `ApiResponse<CalculatorDTO>`

#### GET `/api/calculators/manufacturers`
- **Authentication**: Required (HTTP Basic Auth)
- **Description**: Get a list of all calculator manufacturers
- **Response**: `ApiResponse<List<ManufacturerDTO>>`

#### POST `/api/calculators/{id}/enrich`
- **Authentication**: Required (HTTP Basic Auth)
- **Authorization**: Admin only
- **Description**: Enrich a calculator with web search data, museum search, and AI enhancement
- **Response**: `ApiResponse<EnrichmentDTO>`

---

### 3. Collection API (`/api/collection`)

#### GET `/api/collection`
- **Authentication**: Required
- **Query Parameters**: `page` (default: 0), `size` (default: 20)
- **Response**: `ApiResponse<Page<CalculatorDTO>>`

#### POST `/api/collection/{calculatorId}`
- **Authentication**: Required
- **Description**: Add calculator to user's collection
- **Parameters**: `notes` (optional, query parameter)
- **Response**: `ApiResponse<Void>`

#### DELETE `/api/collection/{calculatorId}`
- **Authentication**: Required
- **Description**: Remove calculator from user's collection
- **Response**: `ApiResponse<Void>`

#### PUT `/api/collection/{calculatorId}/notes`
- **Authentication**: Required
- **Description**: Update notes for a calculator in the collection
- **Parameters**: `notes` (optional, query parameter)
- **Response**: `ApiResponse<Void>`

#### GET `/api/collection/statistics`
- **Authentication**: Required
- **Description**: Get statistics about the user's collection (by manufacturer, period, labels)
- **Response**: `ApiResponse<CollectionStatisticsDTO>`

#### GET `/api/collection/count`
- **Authentication**: Required
- **Description**: Get the total number of calculators in the user's collection
- **Response**: `ApiResponse<Map<String, Long>>` (contains `count` field)

#### GET `/api/collection/export`
- **Authentication**: Required
- **Description**: Export user's collection as JSON or CSV
- **Parameters**: `format` (optional, default: "json", values: "json" or "csv")
- **Response**: File download (JSON or CSV file)

#### POST `/api/collection/import`
- **Authentication**: Required
- **Description**: Import calculators to user's collection from JSON file
- **Parameters**: `file` (multipart/form-data, JSON file)
- **Response**: `ApiResponse<Map<String, Integer>>` (contains `imported` count)

#### POST `/api/collection/send-summary-email`
- **Authentication**: Required
- **Description**: Send an email with collection statistics to the user's email address
- **Response**: `ApiResponse<Void>`

---

### 4. Wishlist API (`/api/wishlist`)

#### GET `/api/wishlist`
- **Authentication**: Required
- **Query Parameters**: `page` (default: 0), `size` (default: 20)
- **Description**: Get all calculators in the authenticated user's wishlist
- **Response**: `ApiResponse<Page<CalculatorDTO>>`

#### POST `/api/wishlist/{calculatorId}`
- **Authentication**: Required
- **Description**: Add calculator to user's wishlist (auto-generates search queries for Marktplaats, eBay, Etsy)
- **Parameters**: `notes` (optional, query parameter)
- **Response**: `ApiResponse<Void>`

#### DELETE `/api/wishlist/{calculatorId}`
- **Authentication**: Required
- **Description**: Remove calculator from user's wishlist
- **Response**: `ApiResponse<Void>`

#### PUT `/api/wishlist/{calculatorId}/notes`
- **Authentication**: Required
- **Description**: Update notes for a calculator in the wishlist
- **Parameters**: `notes` (optional, query parameter)
- **Response**: `ApiResponse<Void>`

#### POST `/api/wishlist/{calculatorId}/move-to-collection`
- **Authentication**: Required
- **Description**: Move a calculator from wishlist to collection (preserves notes)
- **Response**: `ApiResponse<Void>`

#### GET `/api/wishlist/count`
- **Authentication**: Required
- **Description**: Get the total number of calculators in the user's wishlist
- **Response**: `ApiResponse<Map<String, Long>>` (contains `count` field)

#### PUT `/api/wishlist/{calculatorId}/search-queries`
- **Authentication**: Required
- **Description**: Update search queries for Marktplaats, eBay, and Etsy for a wishlist item
- **Parameters**: `marktplaatsQuery` (optional), `ebayQuery` (optional), `etsyQuery` (optional)
- **Response**: `ApiResponse<Map<String, String>>` (contains updated queries)

#### POST `/api/wishlist/{calculatorId}/search-queries/reset`
- **Authentication**: Required
- **Description**: Reset search queries for a wishlist item to auto-generated defaults
- **Response**: `ApiResponse<Map<String, String>>` (contains reset queries)

---

### 5. Images API (`/api/calculators/{calculatorId}/images`)

#### GET `/api/calculators/{calculatorId}/images`
- **Authentication**: Optional (authenticated users see their own pending proposals)
- **Description**: Get all approved images for a calculator. Authenticated users can also see their own pending proposals.
- **Response**: `ApiResponse<List<ImageDTO>>`

#### POST `/api/calculators/{calculatorId}/images`
- **Authentication**: Required
- **Content-Type**: `multipart/form-data`
- **Description**: Upload an image for a calculator
- **Parameters**: 
  - `file` (required): Image file
  - `propose` (optional, default: true): If true, image is proposed for approval. If false and user is admin, image is auto-approved.
- **Response**: `ApiResponse<ImageDTO>`

#### POST `/api/calculators/{calculatorId}/images/binary`
- **Authentication**: Required
- **Content-Type**: `image/jpeg`, `image/png`, `image/gif`, `image/webp`, or `application/octet-stream`
- **Description**: Upload an image using raw binary data in the request body
- **Parameters**: 
  - Request body: Image binary data
  - `Content-Type` header: Image MIME type
  - `propose` (optional, default: true): If true, image is proposed for approval. If false and user is admin, image is auto-approved.
- **Response**: `ApiResponse<ImageDTO>`

#### GET `/api/calculators/{calculatorId}/images/{imageId}/data`
- **Authentication**: Optional (required for unapproved images that user uploaded)
- **Description**: Get the binary data for an image file. Returns the image file with appropriate content type.
- **Response**: Binary image data with appropriate Content-Type header

#### DELETE `/api/calculators/{calculatorId}/images/{imageId}`
- **Authentication**: Required
- **Description**: Delete an image. Users can only delete their own images.
- **Response**: `ApiResponse<Void>`

#### POST `/api/calculators/{calculatorId}/images/from-url`
- **Authentication**: Required
- **Authorization**: Admin only
- **Description**: Download an image from a URL and add it to a calculator
- **Parameters**: 
  - `imageUrl` (required): URL of the image to download
  - `proposeForRepository` (optional, default: false): If true, image is proposed for repository approval
- **Response**: `ApiResponse<ImageDTO>`

---

### 6. Links API (`/api/calculators/{calculatorId}/links`)

#### GET `/api/calculators/{calculatorId}/links`
- **Authentication**: Not required (public endpoint)
- **Description**: Get all external links for a calculator
- **Response**: `ApiResponse<List<LinkDTO>>`

#### POST `/api/calculators/{calculatorId}/links`
- **Authentication**: Required
- **Description**: Add an external link to a calculator
- **Parameters**: 
  - `url` (required): Link URL
  - `title` (required): Link title
  - `description` (optional): Link description
- **Response**: `ApiResponse<LinkDTO>`

#### PUT `/api/calculators/{calculatorId}/links/{linkId}`
- **Authentication**: Required
- **Description**: Update an existing link on a calculator
- **Parameters**: 
  - `url` (required): Link URL
  - `title` (required): Link title
  - `description` (optional): Link description
- **Response**: `ApiResponse<LinkDTO>`

#### DELETE `/api/calculators/{calculatorId}/links/{linkId}`
- **Authentication**: Required
- **Description**: Delete a link from a calculator (users can only delete links they added)
- **Response**: `ApiResponse<Void>`

#### POST `/api/calculators/{calculatorId}/links/bulk-delete`
- **Authentication**: Required
- **Description**: Delete multiple links from a calculator at once
- **Parameters**: `linkIds` (required): List of link IDs to delete
- **Response**: `ApiResponse<Map<String, Integer>>` (contains `deletedCount`)

---

### 7. Labels API (`/api/calculators/{calculatorId}/labels`)

#### GET `/api/calculators/{calculatorId}/labels`
- **Authentication**: Not required (public endpoint)
- **Description**: Get all labels assigned to a calculator
- **Response**: `ApiResponse<List<LabelDTO>>`

#### GET `/api/calculators/{calculatorId}/labels/curated`
- **Authentication**: Not required (public endpoint)
- **Description**: Get all curated labels available in the system
- **Response**: `ApiResponse<List<LabelDTO>>`

#### POST `/api/calculators/{calculatorId}/labels`
- **Authentication**: Required
- **Description**: Add a label to a calculator. Can use existing label ID or create a new free-form label.
- **Parameters**: 
  - `labelId` (optional): Existing label ID to use
  - `newLabelName` (optional): New label name (if creating new free-form label)
  - Either `labelId` or `newLabelName` must be provided
- **Response**: `ApiResponse<Void>`

#### DELETE `/api/calculators/{calculatorId}/labels/{labelId}`
- **Authentication**: Required
- **Description**: Remove a label from a calculator
- **Response**: `ApiResponse<Void>`

---

### 8. Proposals API (`/api/proposals`)

#### POST `/api/proposals`
- **Authentication**: Required
- **Description**: Submit a proposal for a new calculator to be added to the database
- **Parameters**: 
  - `model` (required): Calculator model
  - `manufacturer` (required): Manufacturer name
  - `soldFrom` (optional): Year sold from
  - `soldTo` (optional): Year sold to
  - `sourceUrl` (optional): Source URL
  - `rawRowText` (optional): Raw row text
  - `notes` (optional): Additional notes
- **Response**: `ApiResponse<Map<String, Long>>` (contains `proposalId`)

---

### 9. Sharing API (`/api/share`)

#### POST `/api/share`
- **Authentication**: Required
- **Description**: Create a shareable link for selected calculators or entire collection
- **Parameters**: 
  - `title` (required): Title for the shared collection
  - `description` (optional): Description
  - `calculatorIds` (optional): Comma-separated list of calculator IDs. If empty, shares entire collection.
  - `daysValid` (optional, default: 30): Days until expiration
- **Response**: `ApiResponse<Map<String, String>>` (contains `token` and `shareUrl`)

#### GET `/api/share/{token}`
- **Authentication**: Not required (public endpoint)
- **Description**: Get calculators from a shared collection
- **Query Parameters**: `page` (default: 0), `size` (default: 20)
- **Response**: `ApiResponse<Page<CalculatorDTO>>`

---

### 10. Social Media API (`/api/calculators/{calculatorId}/social-share`)

#### POST `/api/calculators/{calculatorId}/social-share/generate`
- **Authentication**: Required
- **Description**: Generate a social media post for a calculator, optionally with enrichment
- **Parameters**: 
  - `platform` (required): Social media platform (twitter, facebook, instagram, linkedin, reddit, mastodon)
  - `enableEnrichment` (optional, default: false): Enable enrichment (web search, museum search, AI)
- **Response**: `ApiResponse<Map<String, Object>>` (contains `post` and `enrichment`)

---

### 11. Quota API (`/api/quota`)

#### GET `/api/quota/status`
- **Authentication**: Required
- **Description**: Get quota and rate limit status for all services (Google, Bing, Brave, AI APIs)
- **Response**: `ApiResponse<Map<String, QuotaStatus>>`

---

**All `/api/**` endpoints require HTTP Basic Authentication.**

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
- [ ] Include header in ALL requests to `/api/**` endpoints (required for most endpoints)
- [ ] Handle 401 responses (authentication failed)
- [ ] Handle 403 responses (forbidden - admin access required)

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







