# Calculator Collector — Product Specification

This document describes the Calculator Collector application in an implementation-agnostic way: its goal, business logic, data model, and user interface. It is intended for use in later prompts without referring to the current codebase.

---

## 1. Goal

Calculator Collector is a **calculator collection management application**. Its purpose is to:

- Maintain a **shared catalog** of calculators (vintage and modern), with manufacturer, model, production years, description, and optional metadata.
- Let **users** browse this catalog and manage:
  - A **personal collection** of calculators they own (with notes).
  - A **wishlist** of calculators they want to acquire, with search queries for trading/marketplace sites.
- Support **collaboration** by allowing users to propose new calculators for the catalog and by letting admins approve or reject proposals.
- Support **sharing**: users can create shareable links for a subset of their collection, with optional expiration and visibility (public/private).
- **Enrich** calculator entries using external sources (web search, image search, optional AI) and optional price data, subject to configurable quotas and rate limits.
- Provide **organizational tools**: labels/categories for calculators, images, external links, manufacturer management (including merging), and optional email notifications.

The application is aimed at **collectors and enthusiasts** who want to catalogue, track, and share their calculator collections and wishlists.

---

## 2. Business Logic

### 2.1 Users and Access

- **User accounts** have: unique username, unique email, hashed password, role (e.g. USER or ADMIN), enabled flag, and timestamps (e.g. created, last login).
- **Registration**: users provide username, email, and password; the system creates an account (passwords are stored in hashed form).
- **Authentication**: users sign in with username and password; a session is established. Programmatic access uses a standard HTTP authentication scheme (e.g. username/password in headers).
- **Authorization**:
  - **Public (unauthenticated)**: Browsing the calculator catalog (list, search, filter, detail), viewing manufacturers, viewing a shared collection via its link, and auth-related pages (login, register, forgot password, reset password).
  - **Authenticated users**: Personal collection, wishlist, sharing, proposing new calculators, profile (email/password), and optionally uploading images/links/labels for calculators. They can also use export/import and social post generation for their data.
  - **Admins**: Everything above plus: full calculator create/edit/delete, proposal approval/rejection, user management, label management, manufacturer merge, optional bulk image approval, and triggering enrichment.

### 2.2 Calculator Catalog

- The **catalog** is a set of calculator records. Each calculator has:
  - A **manufacturer** (required).
  - **Model** name (required).
  - Optional **production period**: sold-from and sold-to years.
  - Optional **source URL** and **raw text** (e.g. from a reference list).
  - Optional **description** (can be human- or system-generated, e.g. from AI enrichment).
  - Optional **enriched data** (e.g. structured JSON from web/AI enrichment).
  - Optional **current price** and **price currency**, with a last-updated timestamp (can be updated by a scheduled or manual process).
- **Browsing**: Users can list calculators with pagination, **search** by model or manufacturer name (case-insensitive), and **filter** by manufacturer.
- **Detail view**: For a single calculator, the system shows its data plus: approved images, external links, labels, and whether it is in the current user’s collection and/or wishlist (when logged in).

### 2.3 Manufacturers

- Each calculator belongs to exactly one **manufacturer**. Manufacturers have a unique name.
- Users can **browse** manufacturers (e.g. with sorting by id, name A–Z, name Z–A, or calculator count).
- **Editing** a manufacturer (e.g. renaming) is an authenticated/admin capability.
- **Merging**: An admin can merge two manufacturers (e.g. source into target). All calculators of the source manufacturer are reassigned to the target; the source manufacturer is removed. This is a single logical operation.

### 2.4 Personal Collection

- Each user has a **personal collection**: a set of calculator–user pairs with optional **notes** and an **added-at** timestamp.
- A calculator can appear at most once per user in the collection.
- **Add to collection**: User selects a calculator from the catalog; optional notes can be stored with the collection entry.
- **Remove from collection**: User removes a calculator from their collection.
- **Update notes**: User can change the notes for a collection item without removing it.
- **Statistics**: The system can compute collection statistics for the current user, e.g. total count, count by manufacturer, by time period (e.g. pre-1970, 1970s, 1980s, 1990s, 2000s, unknown), and by label.
- **Export**: The user’s collection (and optionally related data) can be exported (e.g. JSON or CSV). Import may be supported to bring data back in.

### 2.5 Wishlist

- Each user has a **wishlist**: a set of calculator–user pairs with optional **notes**, an **added-at** timestamp, and **per-platform search queries** (e.g. for Marktplaats, eBay, Etsy).
- A calculator can appear at most once per user in the wishlist.
- **Add to wishlist**: User selects a calculator; the system may **auto-generate default search queries** for the configured platforms.
- **Default search query rule**: A single default query string is derived from the calculator (e.g. “[vintage] manufacturer model type calculator”). The “vintage” term is included when the calculator has no production start year or its start year is ≤ 2000. The “type” can be derived from labels (e.g. electronic, mechanical, electromechanical) if available; otherwise a default (e.g. “electronic”) is used. This default can be applied to all platforms (e.g. Marktplaats, eBay, Etsy).
- **Edit search queries**: User can override the search query per platform.
- **Reset to default**: User can reset platform queries back to the auto-generated default.
- **Remove from wishlist**: User removes a calculator from the wishlist.
- **Move to collection**: User can move a wishlist item to the personal collection in one action (add to collection, remove from wishlist); notes can be preserved or merged as defined by the product.

### 2.6 Sharing

- A user can **create a share** by selecting a subset of calculators **from their own collection**, plus optional title, description, validity (e.g. number of days), and visibility (public vs private).
- The system generates a **unique share token** and a share link. Only calculators that belong to the user’s collection can be included.
- **Viewing a shared collection**: Anyone with the link can view the shared list (subject to visibility). The share is invalid if the token is wrong or the share has **expired** (based on the validity period).
- Optionally, the system can send an email containing the share link when a share is created.

### 2.7 Calculator Proposals

- **Proposal**: An authenticated user can submit a **proposal** for a new calculator with: manufacturer name, model, optional sold-from/sold-to years, optional source URL, optional raw text, optional notes. Proposals are stored with proposer and timestamp.
- **Review**: Admins see a list of pending (not yet approved/rejected) proposals and can **approve** or **reject** each.
- **Approval**: On approval, the system creates or finds a manufacturer by name, creates a new calculator from the proposal data, and marks the proposal as approved (with approver and timestamp). Optionally, the proposer is notified by email.
- **Rejection**: On rejection, the proposal is marked rejected or removed; optionally the proposer is notified by email.

### 2.8 Labels

- **Labels** are named tags (e.g. “Vintage”, “Mechanical”, “Electromechanical”). Each label has a unique name and can have an optional description. Labels can be marked as **curated** (system- or admin-defined) or not.
- **Calculator–label relationship**: Calculators can have multiple labels; labels can be attached to many calculators (many-to-many). A calculator–label pair is unique.
- **Curated labels**: Some labels are derived or maintained by the system/admin (e.g. from model patterns, manufacturer, years). These can be used for filtering, statistics, and default search-query type.
- **User-added labels**: Authenticated users (or admins) can attach existing labels to calculators or create new labels and attach them, depending on product rules.

### 2.9 Images and Links

- **Images**: Calculators can have multiple images. Each image has a storage path, uploader, upload time, and an **approval** state (e.g. pending/approved). Only approved images are shown in the public/default detail view. Admins (or configured roles) can approve or reject uploaded images.
- **Links**: Calculators can have multiple **external links**, each with URL, title, optional description, and who added it and when. These are shown on the calculator detail (e.g. for references, manuals, sales).

### 2.10 Enrichment

- **Enrichment** is a process that augments a calculator using external services:
  - **Web search**: Query built from manufacturer, model, and optionally years and “calculator” (and e.g. “vintage” if sold-from is null or ≤ 2000). Results are filtered for relevance (e.g. must mention calculator and manufacturer/model). Results can be used to extract links or descriptions.
  - **Image search**: Similar query; results are filtered (e.g. must relate to “calculator” and manufacturer or model). Image URLs or references can be stored or used for suggestions.
  - **AI enhancement**: Optional step that uses an external AI service to generate or improve the calculator description from existing data.
- **Quotas and rate limits**: Use of external search and AI APIs is subject to configurable **rate limits** (e.g. requests per second) and **monthly quotas** (e.g. per provider). The system checks these before calling an external service and may expose current usage (e.g. per provider) to authenticated users or admins.
- Enrichment is typically triggered by an admin (or by an authenticated “generate social post” flow that optionally enriches first). Enrichment results (links, description, enriched data, etc.) are stored on the calculator as defined by the product.

### 2.11 Price and Optional Scheduler

- Calculators can have an optional **current price** and **currency**. A separate process (e.g. scheduled job or manual update) may update prices from an external source; the system records when the price was last updated.

### 2.12 Social Media Post Generation

- Authenticated users can request **generation of a social media post** for a calculator (e.g. for Twitter or other platforms). The system may optionally run enrichment first, then build post content from calculator data, labels, description, and images. The generated text (and metadata) is returned for the user to copy or use.

### 2.13 Email (Optional)

- If email is configured, the system may send:
  - **Password reset**: Link with a time-limited token to set a new password.
  - **Collection shared**: When a user creates a share, send the share link to a specified email.
  - **Proposal approved / rejected**: Notify the proposer when their calculator proposal is approved or rejected.
- All email features are conditional on configuration; the application works without email.

### 2.14 Password Reset

- User requests reset with email; system creates a **password-reset token** tied to the user and an expiration (e.g. 24 hours). User receives a link; using the link allows setting a new password. Tokens are single-use and invalid after use or expiration.

---

## 3. Data Model (Conceptual)

The following describes the main **conceptual entities** and their relationships. Attribute types (e.g. string, number, date) are logical; no storage or API format is implied.

### 3.1 Core Entities

- **User**  
  - Id, username (unique), email (unique), password (hashed), role, enabled, created-at, last-login.

- **Manufacturer**  
  - Id, name (unique).  
  - Relationship: One manufacturer has many calculators.

- **Calculator**  
  - Id, manufacturer (reference), model, sold-from (year), sold-to (year), source-url, raw-row-text, description, enriched-data (e.g. JSON), current-price, price-currency, price-last-updated.  
  - Relationships: Many images, many links, many labels (via join), many collection entries, many wishlist entries, many shared-collection entries.

- **User collection entry** (user’s owned calculators)  
  - Id, user (username or id), calculator, added-at, notes.  
  - Uniqueness: (user, calculator) at most once.

- **Wishlist entry**  
  - Id, user, calculator, added-at, notes, marktplaats-query, ebay-query, etsy-query (or generic platform query fields).  
  - Uniqueness: (user, calculator) at most once.

### 3.2 Sharing

- **Shared collection**  
  - Id, share-token (unique), shared-by (user), shared-at, expires-at, is-public, title, description.  
  - Relationship: Has many “shared collection calculator” entries.

- **Shared collection calculator**  
  - Id, shared-collection, calculator.  
  - Represents one calculator in one shared collection.

### 3.3 Proposals and Moderation

- **Calculator proposal**  
  - Id, model, manufacturer-name, sold-from, sold-to, source-url, raw-row-text, proposed-by, proposed-at, is-approved (or status), approved-by, approved-at, notes.

### 3.4 Labels and Tagging

- **Label**  
  - Id, name (unique), is-curated, description.

- **Calculator–label** (join)  
  - Id, calculator, label.  
  - Uniqueness: (calculator, label) at most once.

### 3.5 Media and Links

- **Calculator image**  
  - Id, calculator, image-path, uploaded-by, uploaded-at, is-proposal (or pending), is-approved, approved-by, approved-at.

- **Calculator link**  
  - Id, calculator, url, title, description, added-by, added-at.

### 3.6 Auth and Security

- **Password reset token**  
  - Id, token (unique), user, expires-at, used.  
  - Used for one-time password reset links.

### 3.7 Summary of Relationships

- **Manufacturer** → many **Calculators**.
- **Calculator** → many **Calculator images**, many **Calculator links**, many **Calculator–labels** (→ **Label**).
- **User** → many **User collection entries** (→ **Calculator**), many **Wishlist entries** (→ **Calculator**), many **Shared collections** (as shared-by).
- **Shared collection** → many **Shared collection calculators** (→ **Calculator**).
- **Calculator proposal** is standalone; on approval, a **Calculator** (and possibly **Manufacturer**) is created.

---

## 4. User Interface (Screens and Flows)

The following is a **screen- and flow-oriented** description. It does not assume a specific UI technology.

### 4.1 Public and Auth

- **Welcome / home**: Entry point; options to log in or register. If already logged in, show greeting and main navigation.
- **Login**: Username and password; submit to authenticate; redirect to home or previously requested page.
- **Register**: Username, email, password; submit to create account; then redirect to login or home.
- **Forgot password**: Enter email; system sends reset link if email exists (when email is configured).
- **Reset password**: Page reached via email link; user sets new password; token is consumed and invalidated.
- **Error page**: Shown on generic errors (e.g. 404, 500) with a way back to a safe page.

### 4.2 Calculator Browsing (Public)

- **Browse calculators**: Paginated list of calculators; search box (model/manufacturer); filter by manufacturer; each row/card links to detail.
- **Calculator detail**: Full calculator data, approved images, links, labels; if logged in, indicators for “in my collection” / “in my wishlist” and actions to add to collection or wishlist.
- **Manufacturers list**: Paginated list of manufacturers (e.g. with sort options and calculator count); select one to filter the calculator browse by that manufacturer.
- **Manufacturer edit** (authenticated/admin): Form to change manufacturer name; submit to save. (Admin-only if only admins can edit.)

### 4.3 Personal Collection (Authenticated)

- **My collection**: Paginated list of the user’s collection items (calculator + notes + added-at). Optional summary or statistics (total, by manufacturer, by period, by label). Actions: edit notes, remove from collection, optional “share” entry point.
- **Add to collection**: Triggered from browse or detail; optional notes field; submit adds the calculator to the collection and redirects back.
- **Edit notes**: In-place or form to update notes for one collection item.
- **Remove**: Confirm and remove calculator from collection.
- **Export**: Button or link to download collection (e.g. JSON/CSV) from “My collection” or a dedicated export page.

### 4.4 Wishlist (Authenticated)

- **My wishlist**: Paginated list of wishlist items (calculator + notes + per-platform search queries). Links or buttons to open each platform’s search (using the stored query). Actions: edit notes, edit search queries, reset queries to default, move to collection, remove from wishlist.
- **Add to wishlist**: From browse or detail; optional notes; submit adds calculator and may set default search queries.
- **Edit search queries**: Form with one field per platform (e.g. Marktplaats, eBay, Etsy); save updates the wishlist item.
- **Reset to default**: One action to recompute and save default queries for that item.
- **Move to collection**: One action to add the calculator to the collection and remove it from the wishlist.

### 4.5 Sharing (Authenticated)

- **Create share**: User selects which calculators from their collection to include; optional title, description, validity (e.g. days), and public/private. Submit generates share link and optionally sends email. Result screen shows the link and optionally “copy” and “email” actions.
- **View shared collection** (public link): Page that shows the shared title, description, and list of calculators (with basic detail). No edit; optional “browse catalog” or similar CTA.

### 4.6 Proposals (Authenticated)

- **Propose calculator**: Form with manufacturer name, model, sold-from, sold-to, source URL, raw text, optional notes. Submit creates a proposal and shows confirmation.
- **Admin – Proposals**: List of pending proposals with key fields; each row has Approve and Reject. Approve creates the calculator (and manufacturer if needed) and marks proposal approved; Reject marks rejected or deletes. Optional email to proposer in both cases.

### 4.7 Profile (Authenticated)

- **Profile**: Show username, email; form to change email; form to change password (current + new). Submit updates and shows success or validation errors.

### 4.8 Calculators – Images and Links (Authenticated)

- **Upload image**: From calculator detail (or dedicated upload UI); user selects file; submit uploads and creates a pending image (unless auto-approved by role).
- **Add link**: From calculator detail; user enters URL, title, optional description; submit adds link to that calculator.
- **Delete image/link**: Optional delete for images/links the user added (or admin for any).

### 4.9 Labels (Authenticated / Admin)

- **Add label to calculator**: From calculator detail (or API); user selects or types label name; system creates label if missing and attaches it to the calculator.
- **Remove label**: Action to remove a label from a calculator.
- **Admin – Labels**: List of all labels (name, curated flag, description); create/edit/delete labels as needed.

### 4.10 Admin

- **Admin dashboard**: Overview and links to calculators, proposals, users, labels, and optionally pending images.
- **Admin – Calculators**: List of all calculators with create/edit/delete. **Create**: form with manufacturer (select or new), model, years, URL, raw text, description, etc. **Edit**: same form for existing calculator. **Delete**: confirm then remove calculator (and handle images/links/labels as per business rules).
- **Admin – Calculator proposals**: As in § 4.6.
- **Admin – Users**: List users; create/edit/disable; set role (e.g. USER, ADMIN).
- **Admin – Manufacturers**: List manufacturers; edit name; **merge**: select source and target manufacturer, confirm; all calculators of source move to target, source is removed.
- **Admin – Enrichment**: From calculator detail or list, action “Enrich” triggers web search, image search, optional AI; result is stored and optionally shown (e.g. links added, description updated). Subject to quota/rate limits.
- **Admin – Image approval**: List of pending images; approve or reject per image or in bulk.

### 4.11 Social and Export (Authenticated)

- **Social post generation**: From calculator detail, user chooses “Generate social post” (and optionally platform/enrichment); system returns generated text (and metadata) for copying.
- **Quota status**: Optional page or section showing current usage of external APIs (e.g. per provider, per month) for the tenant or user.

### 4.12 Navigation and Layout

- **Global navigation**: Links to Browse, Manufacturers, (when logged in) My Collection, Wishlist, Share, Propose, Profile; (when admin) Admin area. Logout when authenticated.
- **Consistent layout**: Header/nav, main content, optional footer across all screens. Error and validation messages shown in a consistent way.

---

## 5. Document Purpose and Use

This specification is **implementation-agnostic**: it does not refer to specific frameworks, languages, databases, or APIs. It can be used to:

- Reimplement the application in another stack.
- Derive API contracts (REST or other) and data formats.
- Design or refine the database schema in any technology.
- Onboard product or engineering without reading the existing codebase.
- Generate or validate tests and documentation from a single source of truth.

When using this document in a later prompt, you can refer to sections by number (e.g. “§2.5 Wishlist”, “§3 Data Model”, “§4.4 Wishlist UI”) to specify scope or constraints without opening the current implementation.
