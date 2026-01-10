# Calculator Collector - Application Flow Documentation

This document describes the flow through the Calculator Collector application, including user interactions, data flow, and system processes.

## Table of Contents

1. [User Registration and Authentication](#user-registration-and-authentication)
2. [Browsing Calculators](#browsing-calculators)
3. [Collection Management](#collection-management)
4. [Wishlist Management](#wishlist-management)
5. [Calculator Enrichment](#calculator-enrichment)
6. [Manufacturer Management](#manufacturer-management)
7. [Calculator Proposals](#calculator-proposals)
8. [Collection Sharing](#collection-sharing)
9. [Admin Operations](#admin-operations)

## User Registration and Authentication

### Flow

1. **User Registration**
   - User visits `/welcome` or `/login`
   - Clicks "Register" or navigates to registration page
   - Fills in username, email, password
   - System validates input and creates user account
   - Password is hashed using BCrypt
   - User is redirected to login page

2. **User Login**
   - User enters username and password
   - Spring Security validates credentials
   - Session is created
   - User is redirected to home or requested page

3. **API Authentication**
   - Client sends HTTP Basic Authentication header
   - Format: `Authorization: Basic base64(username:password)`
   - Spring Security validates credentials
   - Request proceeds if valid, 401 Unauthorized if invalid

## Browsing Calculators

### Flow

1. **Initial Browse**
   ```
   User → GET /calculators
   → CalculatorController.browseCalculators()
   → CalculatorService.getAllCalculators()
   → Returns paginated list (20 per page)
   → Thymeleaf renders browse.html
   ```

2. **Search**
   ```
   User enters search term → GET /calculators?search=HP
   → CalculatorService.searchCalculators()
   → Searches in model and manufacturer name (case-insensitive)
   → Returns matching calculators
   ```

3. **Filter by Manufacturer**
   ```
   User clicks manufacturer → GET /calculators?manufacturerId=123
   → CalculatorService.getCalculatorsByManufacturer()
   → Returns calculators for that manufacturer
   → Pagination preserves manufacturerId parameter
   ```

4. **View Calculator Details**
   ```
   User clicks calculator → GET /calculators/{id}
   → CalculatorController.viewCalculator()
   → Loads calculator, images, links, labels
   → Checks if in user's collection/wishlist
   → Renders detail.html
   ```

## Collection Management

### Adding to Collection

1. **From Browse Page**
   ```
   User clicks "Add to Collection"
   → POST /calculators/collection/add/{id}
   → CalculatorService.addToCollection()
   → Creates UserCalculatorCollection entry
   → Redirects back to browse (preserving filters)
   ```

2. **From Detail Page**
   ```
   User clicks "Add to Collection"
   → POST /calculators/collection/add/{id}
   → Same flow as above
   → Redirects back to detail page
   ```

3. **With Notes**
   ```
   User adds notes → POST /calculators/collection/add/{id}?notes=...
   → Notes are stored with collection item
   ```

### Viewing Collection

```
User → GET /calculators/collection
→ CalculatorService.getUserCollection()
→ Returns paginated collection items
→ StatisticsService.getCollectionStatistics()
→ Renders collection.html with statistics
```

### Managing Collection Items

1. **Edit Notes**
   ```
   User edits notes inline → POST /calculators/collection/{id}/notes
   → CalculatorService.updateCollectionNotes()
   → Updates notes in database
   → Refreshes collection view
   ```

2. **Remove from Collection**
   ```
   User clicks "Remove" → POST /calculators/collection/remove/{id}
   → CalculatorService.removeFromCollection()
   → Deletes UserCalculatorCollection entry
   → Redirects back to collection page
   ```

## Wishlist Management

### Adding to Wishlist

1. **Initial Add**
   ```
   User clicks "Add to Wishlist"
   → POST /calculators/wishlist/add/{id}
   → WishlistService.addToWishlist()
   → Creates WishlistItem
   → Auto-generates search queries:
     - Format: "[vintage] [manufacturer] [model] electronic calculator"
     - "vintage" included if soldFrom <= 2000
   → Sets queries for Marktplaats, eBay, Etsy
   → Redirects back (preserving filters)
   ```

2. **Search Query Generation**
   ```
   WishlistService.generateDefaultSearchQuery()
   → Checks calculator.soldFrom
   → If null or <= 2000: adds "vintage" prefix
   → Appends manufacturer name
   → Appends model name
   → Appends "electronic calculator"
   → Returns formatted query
   ```

### Managing Wishlist

1. **View Wishlist**
   ```
   User → GET /calculators/wishlist
   → WishlistService.getUserWishlist()
   → Returns paginated wishlist items
   → Renders wishlist.html
   → JavaScript generates search URLs from queries
   ```

2. **Edit Search Queries**
   ```
   User clicks "Edit Search Queries"
   → JavaScript shows edit form
   → User edits queries → POST /calculators/wishlist/{id}/search-queries
   → WishlistService.updateWishlistSearchQueries()
   → Updates queries in database
   → Redirects back to wishlist
   ```

3. **Reset to Default**
   ```
   User clicks "Reset to Default"
   → POST /calculators/wishlist/{id}/search-queries/reset
   → WishlistService.resetWishlistSearchQueriesToDefault()
   → Regenerates queries from calculator info
   → Updates all three platforms
   ```

4. **Move to Collection**
   ```
   User clicks "Move to Collection"
   → POST /calculators/wishlist/{id}/move-to-collection
   → CalculatorService.addToCollection()
   → WishlistService.removeFromWishlist()
   → Moves item from wishlist to collection
   ```

## Calculator Enrichment

### Automatic Enrichment Flow

1. **Trigger Enrichment** (Admin)
   ```
   Admin → POST /admin/calculators/{id}/enrich
   → EnrichmentService.enrichCalculator()
   ```

2. **Build Search Query**
   ```
   EnrichmentService.buildEnhancedSearchQuery()
   → Manufacturer + Model + Years + "calculator"
   → Adds "vintage" if soldFrom <= 2000
   → Adds rawRowText
   → Cleans and deduplicates query
   ```

3. **Web Search**
   ```
   → WebSearchService.searchGoogle()
   → WebSearchService.searchBing()
   → WebSearchService.searchBrave()
   → Filters results (must contain "calculator", manufacturer, model)
   → Stores results
   ```

4. **Image Search**
   ```
   → WebSearchService.searchGoogleImages()
   → WebSearchService.searchBingImages()
   → WebSearchService.searchBraveImages()
   → Filters images (must contain "calculator" AND (manufacturer OR model))
   → Stores image URLs
   ```

5. **AI Enhancement** (if configured)
   ```
   → AISearchService.enhanceWithAI()
   → Sends calculator info to OpenAI/Anthropic
   → Generates description
   → Stores in calculator.description
   ```

6. **Link Extraction**
   ```
   → Extracts links from web search results
   → LinkService.addLinkIfNotExists()
   → Creates CalculatorLink entries
   ```

## Manufacturer Management

### Browsing Manufacturers

```
User → GET /calculators/manufacturers
→ CalculatorService.searchManufacturersWithSort()
→ Applies sorting (ID, name A-Z, name Z-A, count)
→ Returns paginated manufacturers
→ Renders manufacturers.html
```

### Editing Manufacturers

1. **Update Name**
   ```
   User → GET /calculators/manufacturers/{id}/edit
   → Shows edit form
   → User updates name → POST /calculators/manufacturers/{id}/update
   → CalculatorService.updateManufacturer()
   → Updates manufacturer name
   ```

2. **Merge Manufacturers**
   ```
   User selects source and target → POST /calculators/manufacturers/merge
   → CalculatorService.mergeManufacturers()
   → Uses direct SQL update: UPDATE calculators SET manufacturer_id = target
   → Clears source manufacturer's calculator list
   → Deletes source manufacturer
   → All calculators now reference target manufacturer
   ```

## Calculator Proposals

### Submission Flow

1. **User Submits Proposal**
   ```
   User → GET /calculators/propose
   → Fills in form (manufacturer, model, years, source URL)
   → POST /calculators/propose
   → CalculatorProposalService.createProposal()
   → Creates CalculatorProposal (pending approval)
   → Redirects to confirmation
   ```

2. **Admin Review**
   ```
   Admin → GET /admin/proposals
   → Views pending proposals
   → Reviews details
   → Approves or rejects
   ```

3. **Approval Flow**
   ```
   Admin → POST /admin/proposals/{id}/approve
   → CalculatorProposalService.approveProposal()
   → Gets or creates manufacturer
   → Creates Calculator entity
   → Marks proposal as approved
   → EmailService sends approval notification (if configured)
   ```

4. **Rejection Flow**
   ```
   Admin → POST /admin/proposals/{id}/reject
   → CalculatorProposalService.rejectProposal()
   → Deletes proposal
   → EmailService sends rejection notification (if configured)
   ```

## Collection Sharing

### Creating Share Link

1. **User Creates Share**
   ```
   User → GET /calculators/share
   → Selects calculators from collection
   → Sets title, description, visibility, expiration
   → POST /calculators/share
   → ShareService.createShare()
   → Generates unique token
   → Creates SharedCollection
   → Creates SharedCollectionCalculator entries
   → Returns share URL
   ```

2. **Viewing Shared Collection**
   ```
   Anyone → GET /share/{token}
   → ShareService.getSharedCollection()
   → Validates token and expiration
   → Returns shared calculators
   → Renders shared-view.html
   ```

3. **Email Sharing** (if configured)
   ```
   User → POST /calculators/share (with email)
   → EmailService.sendCollectionSharedEmail()
   → Sends email with share link
   ```

## Admin Operations

### Calculator Management

1. **Create Calculator**
   ```
   Admin → GET /admin/calculators/new
   → Fills form → POST /admin/calculators
   → AdminService.createCalculator()
   → Creates Calculator entity
   → Redirects to calculator list
   ```

2. **Edit Calculator**
   ```
   Admin → GET /admin/calculators/{id}/edit
   → Updates fields → POST /admin/calculators/{id}
   → AdminService.updateCalculator()
   → Updates calculator
   → Can update manufacturer, description, enriched data
   ```

3. **Delete Calculator**
   ```
   Admin → POST /admin/calculators/{id}/delete
   → AdminService.deleteCalculator()
   → Deletes associated images (files)
   → Deletes calculator
   ```

### Image Management

1. **Upload Image**
   ```
   User/Admin → POST /api/calculators/{id}/images
   → ImageService.uploadImage()
   → Saves file to uploads/ directory
   → Creates CalculatorImage (pending approval)
   → Admin approves → ImageService.approveImage()
   ```

2. **Bulk Image Approval**
   ```
   Admin → GET /admin/proposals/images
   → Views pending images
   → Approves/rejects in bulk
   ```

### Label Management

1. **Add Label**
   ```
   User/Admin → POST /api/calculators/{id}/labels
   → LabelService.addLabelToCalculator()
   → Creates or uses existing label
   → Links calculator to label
   ```

2. **Curated Labels**
   ```
   System → LabelDerivationService.deriveLabels()
   → Analyzes calculator data
   → Automatically assigns labels based on:
     - Model patterns
     - Manufacturer
     - Years
     - Raw text content
   ```

## Data Flow Summary

### Request Flow (Web UI)

```
Browser Request
  ↓
Spring Security (authentication/authorization)
  ↓
Controller (CalculatorController, etc.)
  ↓
Service Layer (CalculatorService, etc.)
  ↓
Repository Layer (CalculatorRepository, etc.)
  ↓
Database (H2)
  ↓
Response (Thymeleaf template)
  ↓
Browser
```

### Request Flow (REST API)

```
Client Request (HTTP Basic Auth)
  ↓
Spring Security (authentication)
  ↓
REST Controller (CalculatorRestController, etc.)
  ↓
Service Layer
  ↓
Repository Layer
  ↓
Database
  ↓
DTO Mapping
  ↓
JSON Response
  ↓
Client
```

## Key Services and Their Responsibilities

- **CalculatorService**: Core calculator operations, collection management
- **WishlistService**: Wishlist operations, search query generation
- **EnrichmentService**: Coordinates web search, image search, AI enhancement
- **WebSearchService**: Google, Bing, Brave search integration
- **ImageService**: Image upload, approval, management
- **LinkService**: External link management
- **LabelService**: Label assignment and management
- **ShareService**: Collection sharing functionality
- **EmailService**: Email notifications (conditional on configuration)
- **StatisticsService**: Collection statistics and analytics
- **ExportService**: Data export functionality
- **ImportService**: Data import functionality

## Database Schema Overview

- **calculators**: Main calculator entities
- **manufacturers**: Calculator manufacturers
- **user_calculator_collections**: User's owned calculators
- **wishlist_items**: User's wishlist with search queries
- **calculator_images**: Calculator images (with approval status)
- **calculator_links**: External links for calculators
- **calculator_labels**: Many-to-many relationship for labels
- **labels**: Label definitions
- **calculator_proposals**: Pending calculator proposals
- **shared_collections**: Shared collection definitions
- **shared_collection_calculators**: Calculators in shared collections
- **users**: User accounts

## Security Flow

1. **Web UI**: Session-based authentication via Spring Security
2. **REST API**: HTTP Basic Authentication
3. **CSRF**: Disabled for `/api/**`, enabled for web UI
4. **CORS**: Enabled for `/api/**` endpoints
5. **Authorization**: Role-based (USER, ADMIN)

This flow documentation provides a comprehensive overview of how the application processes user interactions and manages data throughout the system.

