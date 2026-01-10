# Calculator Collector

A comprehensive web application for curating and managing a collection of vintage and modern calculators. Built with Spring Boot, this application provides both a web UI and REST APIs for browsing calculators, managing personal collections, wishlists, and sharing collections with others.

## Features

### Core Functionality
- **Calculator Database**: Browse and search through a curated database of calculators
- **Personal Collection**: Track calculators you own with personal notes
- **Wishlist**: Save calculators you want to acquire with search queries for trading sites
- **Manufacturer Management**: Browse by manufacturer with sorting and merge capabilities
- **Calculator Proposals**: Propose new calculators to be added to the database
- **Collection Sharing**: Share your collection with others via public or private links
- **Calculator Enrichment**: Automatic enrichment with web search results, images, and links
- **Labels**: Organize calculators with custom labels and categories
- **Images & Links**: Add images and external links to calculators

### Advanced Features
- **Trading Site Integration**: Auto-generated search queries for Marktplaats.nl, eBay, and Etsy
- **Email Notifications**: Email functionality for sharing collections and notifications
- **Statistics**: View collection statistics and insights
- **Social Media Sharing**: Generate social media posts for calculators
- **Import/Export**: Import and export calculator data
- **Admin Panel**: Administrative interface for managing calculators, users, and proposals

## Technology Stack

- **Backend**: Spring Boot 3.5.9
- **Java**: JDK 21
- **Database**: H2 (file-based)
- **Frontend**: Thymeleaf templates
- **Security**: Spring Security with HTTP Basic Authentication
- **API Documentation**: OpenAPI/Swagger
- **Build Tool**: Maven

## Prerequisites

- Java 21 or higher
- Maven 3.6+ (or use Maven Wrapper)
- Optional: API keys for external services (Google Search, Bing, Brave, OpenAI/Anthropic)

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd CalculatorCollector
```

### 2. Configure Application

Copy the example configuration file:

```bash
cp src/main/resources/application.properties.example src/main/resources/application.properties
```

Edit `src/main/resources/application.properties` and configure:

- **Database**: H2 is pre-configured, no changes needed
- **Server**: Default port is 8080, bound to all interfaces (0.0.0.0)
- **Search APIs** (optional): Add API keys for Google, Bing, Brave, or AI services
- **Email** (optional): Configure SMTP settings for email notifications

### 3. Build and Run

Using Maven:

```bash
mvn clean package
java -jar target/CalCol-0.0.1-SNAPSHOT.jar
```

Or using Spring Boot Maven plugin:

```bash
mvn spring-boot:run
```

### 4. Access the Application

- **Web UI**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **API Docs**: http://localhost:8080/api-docs
- **H2 Console**: http://localhost:8080/h2-console

Default credentials for H2 console:
- JDBC URL: `jdbc:h2:file:./data/calcol`
- Username: `sa`
- Password: (empty)

## Application Flow

For detailed flow documentation, see [APPLICATION_FLOW.md](APPLICATION_FLOW.md).

### User Journey

1. **Browse Calculators**
   - View all calculators or filter by manufacturer
   - Search by model or manufacturer name
   - View calculator details with images, links, and labels

2. **Manage Collection**
   - Add calculators to your personal collection
   - Add personal notes to collection items
   - View collection statistics
   - Export collection data

3. **Wishlist Management**
   - Add calculators to wishlist
   - Auto-generated search queries for trading sites (Marktplaats, eBay, Etsy)
   - Customize search queries per platform
   - Move items from wishlist to collection

4. **Propose New Calculators**
   - Submit proposals for new calculators
   - Admins review and approve/reject proposals

5. **Share Collections**
   - Create shareable links for your collection
   - Set expiration dates and visibility (public/private)

### Admin Functions

- Manage calculators (create, edit, delete)
- Review and approve/reject calculator proposals
- Manage users and permissions
- Manage labels and categories
- Enrich calculators with web search data

## REST API

The application provides a comprehensive REST API for programmatic access. All API endpoints are documented via Swagger UI.

### Base URL
```
http://localhost:8080/api
```

### Authentication

All API endpoints (except public calculator browsing) require HTTP Basic Authentication:

```
Authorization: Basic base64(username:password)
```

### API Endpoints Overview

#### Calculators (Public)
- `GET /api/calculators` - Browse calculators with optional search and manufacturer filter
  - Query params: `search`, `manufacturerId`, `page`, `size`
- `GET /api/calculators/{id}` - Get detailed calculator information
- `GET /api/calculators/manufacturers` - List all manufacturers

#### Collection Management (Authenticated)
- `GET /api/collection` - Get user's collection (paginated)
  - Query params: `page`, `size`
- `POST /api/collection/{calculatorId}` - Add calculator to collection
  - Body: Optional `notes` parameter
- `DELETE /api/collection/{calculatorId}` - Remove calculator from collection
- `PUT /api/collection/{calculatorId}/notes` - Update notes for collection item
  - Body: `notes` (string, optional)
- `GET /api/collection/statistics` - Get collection statistics (counts, by manufacturer, etc.)
- `GET /api/collection/count` - Get total count of calculators in collection

#### Wishlist Management (Authenticated)
- `GET /api/wishlist` - Get user's wishlist (paginated)
  - Query params: `page`, `size`
- `POST /api/wishlist/{calculatorId}` - Add calculator to wishlist
  - Body: Optional `notes` parameter
  - Auto-generates search queries for Marktplaats, eBay, Etsy
- `DELETE /api/wishlist/{calculatorId}` - Remove calculator from wishlist
- `PUT /api/wishlist/{calculatorId}/notes` - Update notes for wishlist item
  - Body: `notes` (string, optional)
- `POST /api/wishlist/{calculatorId}/move-to-collection` - Move wishlist item to collection
- `GET /api/wishlist/count` - Get total count of calculators in wishlist

#### Calculator Resources (Authenticated)
- **Images**:
  - `GET /api/calculators/{calculatorId}/images` - Get all approved images for calculator
  - `POST /api/calculators/{calculatorId}/images` - Upload image (multipart/form-data)
    - Body: `file` (image file)
  - `DELETE /api/calculators/{calculatorId}/images/{imageId}` - Delete image (if you uploaded it)

- **Links**:
  - `GET /api/calculators/{calculatorId}/links` - Get all links for calculator
  - `POST /api/calculators/{calculatorId}/links` - Add external link
    - Body: `url`, `title`, `description` (optional)
  - `DELETE /api/calculators/{calculatorId}/links/{linkId}` - Delete link (if you added it)

- **Labels**:
  - `GET /api/calculators/{calculatorId}/labels` - Get all labels for calculator
  - `GET /api/calculators/{calculatorId}/labels/curated` - Get curated labels only
  - `POST /api/calculators/{calculatorId}/labels` - Add label to calculator
    - Body: `labelName` (string)
  - `DELETE /api/calculators/{calculatorId}/labels/{labelId}` - Remove label from calculator

#### Proposals (Authenticated)
- `POST /api/proposals` - Submit a proposal for a new calculator
  - Body: `manufacturerName`, `model`, `soldFrom`, `soldTo`, `sourceUrl`, `rawRowText`

#### Sharing (Authenticated)
- `POST /api/share` - Create a shareable link for a collection
  - Body: `calculatorIds` (array), `title`, `description`, `daysValid`, `isPublic`
  - Returns: Share token and URL
- `GET /api/share/{token}` - View shared collection (public, no auth required)
  - Returns: Shared calculators with details

#### User Profile (Authenticated)
- `GET /api/user/profile` - Get current user's profile information
- `PUT /api/user/profile/email` - Update user email
  - Body: `email` (string)
- `PUT /api/user/profile/password` - Update user password
  - Body: `currentPassword`, `newPassword`

#### Social Media (Authenticated)
- `POST /api/calculators/{calculatorId}/social-share/generate` - Generate social media post content
  - Returns: Generated post text and metadata

#### Quota Status (Authenticated)
- `GET /api/quota/status` - Get current API quota usage status
  - Returns: Usage statistics for Google, Bing, Brave, and AI APIs

### API Response Format

All API responses follow a standard format:

```json
{
  "success": true,
  "message": "Operation successful",
  "data": { ... }
}
```

Error responses:

```json
{
  "success": false,
  "message": "Error description",
  "data": null
}
```

### Complete API Documentation

For complete API documentation with request/response examples, visit:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI JSON**: http://localhost:8080/api-docs

## Configuration

### Server Configuration

```properties
server.address=0.0.0.0  # Bind to all interfaces (required for emulator access)
server.port=8080
```

### Database Configuration

The application uses H2 file-based database by default:

```properties
spring.datasource.url=jdbc:h2:file:./data/calcol
spring.datasource.username=sa
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
```

Database files are stored in the `data/` directory:
- `calcol.mv.db` - Main database file
- `calcol.trace.db` - Trace log
- `calcol.lock.db` - Lock file

### Search API Configuration (Optional)

```properties
# Google Custom Search
app.search.google.api-key=your-api-key
app.search.google.search-engine-id=your-search-engine-id

# Bing Search (deprecated - retirement scheduled for August 2025)
app.search.bing.api-key=your-api-key

# Brave Search
app.search.brave.api-key=your-api-key

# AI Search (OpenAI or Anthropic)
app.search.ai.api-key=your-api-key
app.search.ai.provider=openai  # or "anthropic"
```

### Email Configuration (Optional)

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
```

For Gmail, use an [App Password](https://myaccount.google.com/apppasswords) instead of your regular password.

### Quota Configuration

Rate limits and monthly limits for search APIs:

```properties
app.quota.brave.rate-limit=1
app.quota.google.rate-limit=10
app.quota.bing.rate-limit=10
app.quota.ai.rate-limit=5

app.quota.brave.monthly-limit=2000
app.quota.google.monthly-limit=10000
app.quota.bing.monthly-limit=10000
app.quota.ai.monthly-limit=5000
```

## Development

### Project Structure

```
src/
├── main/
│   ├── java/com/example/CalCol/
│   │   ├── controller/          # Web controllers (Thymeleaf)
│   │   │   ├── api/            # REST API controllers
│   │   │   ├── CalculatorController.java
│   │   │   └── AdminController.java
│   │   ├── service/             # Business logic
│   │   ├── repository/          # Data access layer
│   │   ├── entity/              # JPA entities
│   │   ├── dto/                 # Data transfer objects
│   │   ├── config/              # Configuration classes
│   │   └── CalculatorCollectorApplication.java
│   └── resources/
│       ├── templates/           # Thymeleaf templates
│       ├── application.properties
│       └── application.properties.example
└── test/
    └── java/com/example/CalCol/
```

### Building

```bash
mvn clean package
```

### Running Tests

```bash
mvn test
```

### Development Mode

The application includes Spring Boot DevTools for hot-reloading during development.

## Deployment

### Building for Production

```bash
mvn clean package -DskipTests
```

The JAR file will be created at: `target/CalCol-0.0.1-SNAPSHOT.jar`

### Running in Production

```bash
java -jar target/CalCol-0.0.1-SNAPSHOT.jar
```

### Environment Variables

You can override configuration using environment variables:

```bash
export SPRING_DATASOURCE_URL=jdbc:h2:file:./data/calcol
export SERVER_PORT=8080
java -jar target/CalCol-0.0.1-SNAPSHOT.jar
```

## Security

- **Authentication**: HTTP Basic Authentication for REST APIs
- **CSRF Protection**: Disabled for `/api/**` endpoints, enabled for web UI
- **CORS**: Configured for `/api/**` endpoints
- **Password Storage**: BCrypt password hashing
- **Session Management**: Spring Security session management

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Submit a pull request

## License

[Add your license here]

## Documentation

- **README.md** (this file): Project overview, setup, and quick start
- **HELP.md**: User-facing help and guide
- **APPLICATION_FLOW.md**: Detailed application flow and data processing
- **REST_API_CLIENT_GUIDE.md**: Comprehensive REST API client integration guide
- **Swagger UI**: Interactive API documentation at `/swagger-ui.html`

## Support

For issues, questions, or contributions, please open an issue on the repository.

## Acknowledgments

Built with Spring Boot and the Spring ecosystem.

