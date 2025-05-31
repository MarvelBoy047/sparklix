# Sparklix - BookAShow Microservices Platform

## 1. Project Objective

Sparklix is a microservices-based application designed to allow users to browse and book tickets for various shows (e.g., movies, plays, events). The project emphasizes a modern Java stack utilizing Spring Boot, Spring Cloud, and related technologies, focusing on security, scalability, and maintainability through RESTful APIs.

## 2. Current Project Status & Modules

The project is organized as a Maven multi-module project. All core services are up, running, and registering with Eureka. The API Gateway is routing requests, and inter-service security (JWT validation) has been successfully tested between key services.

*   **`sparklix-parent`**:
    *   **Status:** ✅ Complete
    *   **Details:** Parent POM managing common dependencies (Spring Boot 3.3.0, Spring Cloud 2023.0.2, Lombok, JJWT 0.11.5, JUnit 5), build plugin configurations, and Java version (17).

*   **`sparklix-eureka-server`**:
    *   **Status:** ✅ Complete & Running
    *   **Purpose**: Service Discovery Server using Spring Cloud Netflix Eureka.
    *   **Port**: `8761`.
    *   **Details**: Standalone configuration; all other services register here.

*   **`sparklix-user-service`**:
    *   **Status:** ✅ Core Functionality Complete & Running
    *   **Purpose**: Handles user identity: registration (with BCrypt hashing), login, role management (`ROLE_USER`, `ROLE_ADMIN`, and a service account `catalog-sync-agent` with `ROLE_ADMIN`), and JWT issuance. Acts as the primary Authentication Server.
    *   **Port**: `8081`.
    *   **Database**: `sparklix_user_db` (MySQL). Initial admin user (`adminuser`), service account, and roles populated via manual SQL execution (formerly `data.sql`).
    *   **Key Features**: JWT generation (including "roles" claim), `CustomUserDetailsService`, `SecurityConfig` with JWT filter chain, AOP logging, Global Exception Handling.

*   **`sparklix-admin-service`**:
    *   **Status:** ✅ CRUD Operations & Security Verified
    *   **Purpose**: For admin-only functionalities (managing shows, venues, showtimes). Acts as a Resource Server, validating JWTs issued by `user-service`.
    *   **Port**: `8082`.
    *   **Database**: `sparklix_admin_db` (MySQL) for storing show management data.
    *   **Key Features**: Secure CRUD endpoints for Shows, Venues, and Showtimes (requiring `ROLE_ADMIN`), including dependency checks for deletion. Internal API (`/api/internal/data/admin/shows-for-catalog`) for data synchronization, secured for `ROLE_ADMIN`. JWT validation, token-claim based `CustomUserDetailsService`, Global Exception Handling.

*   **`sparklix-show-catalog-service`**:
    *   **Status:** ✅ Data Sync, GET Endpoints & Review Feature Implemented & Tested
    *   **Purpose**: Service for users to browse show listings and manage reviews.
    *   **Port**: `8083`.
    *   **Database**: `sparklix_show_db` (MySQL) - data is periodically synced from `admin-service`.
    *   **Key Features**:
        *   Data synchronization from `admin-service` via internal API (using `RestTemplate` and service account token obtained from `user-service`) via a scheduled task.
        *   Public GET endpoints for browsing shows, specific shows, searching by title/genre, and viewing reviews.
        *   User Reviews: Authenticated users (`ROLE_USER`/`ROLE_VENDOR`) can submit reviews; endpoint secured with JWT validation and role checks (`@PreAuthorize`). Duplicate review prevention logic implemented.
        *   JWT validation, token-claim based `CustomUserDetailsService`, Global Exception Handling.

*   **`sparklix-api-gateway`**:
    *   **Status:** ✅ Configured & Running
    *   **Purpose**: Central entry point for all client requests, using Spring Cloud Gateway.
    *   **Port**: `8080`.
    *   **Details**: Configured for service discovery-based routing (e.g., `lb://USER-SERVICE`). Explicit routes defined in `application.properties` (or `.yml`) for all services.

*   **`sparklix-booking-service`**:
    *   **Status:** ✅ Basic Scaffolding & Security Shell Complete
    *   **Purpose**: To handle ticket reservations.
    *   **Port**: `8084`.
    *   **Database**: `sparklix_booking_db` (MySQL).
    *   **Key Features**: Module created with dependencies (Web, Data JPA, Security, JWT, Eureka Client, RestTemplate via `AppConfig`). Basic `Booking` entity, repository, DTOs, and JWT validation security (as a resource server) are in place. Core booking logic is the next implementation step.

## 3. Key Features & Flows Implemented & Tested (End-to-End via API Gateway)

*   ✅ **User Registration & Login (`user-service`):**
    *   `adminuser` and `pythontestuser` (representing `ROLE_USER`) can log in successfully (200 OK) and receive JWTs with correct roles.
    *   New user registration for `ROLE_USER` works, with duplicate prevention (400).
*   ✅ **Role-Based Access Control (RBAC) - Verified:**
    *   **Within `user-service`:**
        *   `/api/auth/test/user` accessible by `pythontestuser` (ROLE_USER token) - 200 OK.
        *   `/api/auth/test/admin` accessed by `pythontestuser` (ROLE_USER token) - 403 Forbidden.
    *   **Across services (`user-service` token to `admin-service`):**
        *   `/api/admin/hello` and Show Management CRUD endpoints (e.g., `POST /api/admin/management/venues`):
            *   No token: `401 Unauthorized`.
            *   `adminuser`'s token (ROLE_ADMIN): `200 OK` or `201 Created`.
            *   `pythontestuser`'s token (ROLE_USER): `403 Forbidden`.
    *   **Across services (`user-service` token to `show-catalog-service` for reviews):**
        *   `POST /api/shows/{showId}/reviews`:
            *   No token: `401 Unauthorized`.
            *   `pythontestuser`'s token (ROLE_USER): `201 Created` (or `409 Conflict` if already reviewed, which is correct behavior).
            *   `adminuser`'s token (ROLE_ADMIN): `403 Forbidden`.
*   ✅ **Inter-Service Data Sync (`admin-service` to `show-catalog-service`):**
    *   `show-catalog-service` successfully authenticates as `catalog-sync-agent` (service account with `ROLE_ADMIN`) to `user-service`.
    *   Uses the obtained token to call a secured internal API on `admin-service` via `RestTemplate`.
    *   Successfully fetches and populates show data into its local database.
*   ✅ **Service Discovery & API Gateway Routing:** All services register with Eureka, and the API Gateway correctly routes requests. `503 Service Unavailable` issues have been resolved.
*   ✅ **Show Browsing & Review Viewing (`show-catalog-service`):** Public GET endpoints return data from the synced local database.
*   ✅ **Structured Error Responses & DTO Validation:** `user-service`, `admin-service`, and `show-catalog-service` provide consistent JSON error responses for various error conditions.
*   ✅ **AOP Logging:** Implemented in `user-service` (template for other services).

## 4. Next Steps (Progress Tracker from Original Plan)

*   ✅ **Step 1 — Building `sparklix-admin-service` endpoints**
    *   **Status:** ✅ CRUD foundation and security for Shows, Venues, Showtimes complete and verified.
*   ✅ **Step 2 — "Adding another microservice"**
    *   **`sparklix-show-catalog-service` Status:** ✅ Data Sync, GET endpoints, and User Review functionality (including security) complete and tested.
    *   **`sparklix-booking-service` Status:** ✅ Basic scaffolding, entity, repository, DTOs, security shell (JWT validation), and `RestTemplate` config complete.
*   🧪 **Step 3 — JUnit/Mockito testing**
    *   **Status:** ❌ Not started.
*   📘 **Step 4 — Formal design diagrams**
    *   **Status:** 🟡 Pending.

## 5. Detailed TODOs (Priority Order)

1.  **`sparklix-booking-service` - Implement Core Booking Logic:**
    *   **Implement `BookingService.createBooking()`:**
        *   Use `@LoadBalanced RestTemplate` to call `show-catalog-service`'s `/api/shows/showtimes/{showtimeId}/details-for-booking` endpoint to get `ShowtimeDetailsDto` (including price and `availableSeats`).
        *   Validate seat availability against `ShowtimeDetailsDto.availableSeats`.
        *   Calculate total price.
        *   Save `Booking` entity with status `PENDING_PAYMENT`.
        *   *(Placeholder for now: Logic to decrement available seats in `show-catalog-service` or manage inventory - this is complex and can be a simplified stub for now).*
    *   **Implement `BookingController.java`:**
        *   `POST /api/bookings`: Calls `createBooking`.
        *   `GET /api/bookings/my-bookings`: Fetches bookings for the authenticated user.
    *   Implement custom exceptions and update `GlobalExceptionHandler` in `booking-service`.
    *   Add API Gateway route for `/api/bookings/**`.
2.  **Implement Vendor Role & Signup in `sparklix-user-service`:**
    *   Add `ROLE_VENDOR` (e.g., ID 3) via manual SQL in `sparklix_user_db`.
    *   Update `UserRegistrationRequest` or create a new DTO/endpoint for vendor signup.
    *   Implement email domain validation (e.g., disallow common free email providers like gmail.com for vendors).
    *   Test vendor registration, login, and access to shared endpoints (e.g., posting reviews, booking tickets if vendors are allowed).
3.  **Unit & Integration Testing (All Services):**
    *   Start with `sparklix-user-service`, then `admin-service`, `show-catalog-service`, then `booking-service`.
4.  **`sparklix-analytics-service` - Basic Scaffolding & Event Ingestion:**
    *   Create module. Endpoint `/api/analytics/events`.
    *   Modify other services to make `RestTemplate` calls to this endpoint for key events.
5.  **`sparklix-notification-service` - Basic Scaffolding & Email Setup.**
6.  **`sparklix-payment-service` - Scaffolding.**
7.  **Design Diagrams.**
8.  **Refine `ShowDataSyncService`:** (Lower priority for now) Implement robust service-to-service auth instead of direct credential use.

## 6. Tech Stack

*   Java 17
*   Spring Boot 3.3.0
*   Spring Security + JWT (io.jsonwebtoken:jjwt 0.11.5)
*   Spring Cloud `2023.0.2` (Eureka, Gateway, LoadBalancer)
*   Spring Data JPA, Hibernate
*   `RestTemplate` for inter-service communication.
*   MySQL
*   Maven
*   STS 4 (IDE), Postman (Manual API Testing), Python `requests` (Automated E2E Flow Testing)

Your microservices (admin-service, user-service, show-catalog-service, booking-service, api-gateway, eureka-server) are all interacting correctly for this entire flow.
The data creation in admin-service is working.
The ShowDataSyncService in show-catalog-service is correctly fetching data from admin-service and populating its local catalog. The timing with the added delay in the Python script seems to have stabilized this.
User authentication and registration in user-service are working.
The booking-service can correctly:
Fetch showtime details from show-catalog-service.
Create a booking.
Retrieve all bookings for a user.
Retrieve a specific booking by its ID, including the authorization check.
The API Gateway is routing all requests correctly.
This is a major milestone! The core end-to-end functionality you set out to test is now verified.
Next Possible Steps (based on your overall project plan):
Review and Refine Exception Handling:
While the happy path works, ensure that all custom exceptions you've defined (like ResourceNotFoundException, and any you might add for InsufficientSeatsException, BookingFailedException, AccessDeniedException) are consistently used and handled by your GlobalExceptionHandler in each service to return meaningful error responses. The Python script currently uses generic RuntimeException checks in some places.
Inventory Management (Decrementing Seats):
This is the "Placeholder for now" in BookingService ((Placeholder for now: Logic to decrement available seats in show-catalog-service or manage inventory)). This is a complex but crucial feature for a real booking system.
Plan:
show-catalog-service: Add an availableSeats field to its local Showtime entity.
show-catalog-service: Create a new internal (secured) API endpoint (e.g., PATCH /api/internal/catalog/showtimes/{showtimeId}/decrement-seats) that booking-service can call. This endpoint would decrement availableSeats.
booking-service: After a booking is confirmed (e.g., after successful payment, if you implement that, or immediately after PENDING_PAYMENT for now if payment is later), call this new endpoint in show-catalog-service. This call needs to be robust (handle retries or use asynchronous messaging like RabbitMQ if strict atomicity across services is hard).
Payment Gateway Integration (sparklix-payment-service):
Implement the basic integration with a payment provider (even a dummy one for now).
booking-service would interact with payment-service after creating a PENDING_PAYMENT booking.
Update booking status to CONFIRMED or FAILED based on payment outcome.
Notification Service (sparklix-notification-service):
Send email/SMS (if feasible) on successful booking confirmation or registration.
This service could consume messages from RabbitMQ published by booking-service or user-service.
Vendor Functionality:
Add ROLE_VENDOR and vendor registration to user-service.
Implement content management for vendors (either in admin-service scoped to the vendor, or a new vendor-portal-service).
Testing Strategy Expansion:
Write more unit tests (JUnit, Mockito) for all services, aiming for good coverage of business logic and edge cases.
Write integration tests within each service.
Consider contract testing (e.g., Spring Cloud Contract) between services.
UI Development: Start planning or developing the frontend that will consume these microservices.
Code Quality & SonarQube: Set up and run SonarQube/SonarLint to analyze code quality and address issues.
Next Steps (Code Review & Refinement Focus, then tackling the skipped error scenarios):
A. Code Review & Refinement (General):
You asked to head towards this. Here's a checklist for each of your services:
Custom Exceptions:
Identify Logic: Go through each service (*Service.java files). Look for places where you throw new RuntimeException("some message") or where a caught exception is simply re-thrown as a generic RuntimeException.
Create Specific Exceptions: If the error condition is specific to your business logic (e.g., "Payment declined by bank", "Show title already exists", "Cannot delete venue with active showtimes"), create a new custom exception class for it (like PaymentFailedException, ShowAlreadyExistsException, VenueDeletionConflictException) in that service's exception package. These typically extend RuntimeException.
@ResponseStatus (Optional): If a custom exception always maps to a specific HTTP status (e.g., ShowAlreadyExistsException might always be a 409 Conflict), you can annotate the exception class with @ResponseStatus(HttpStatus.CONFLICT). This is an alternative to an explicit @ExceptionHandler method if the mapping is straightforward. However, using @ExceptionHandler gives more control over the response body.
Service Layer Throwing:
Modify your service methods to throw these new, specific custom exceptions instead of generic ones.
GlobalExceptionHandler Update:
In each service's GlobalExceptionHandler.java, add new @ExceptionHandler(YourNewCustomException.class) methods to handle these specific exceptions.
These handlers should create an ErrorResponseDto with an appropriate HTTP status code, error short description, and use ex.getMessage() for the detailed message.
Logging in Exception Handlers:
Ensure all @ExceptionHandler methods log the exception, especially for unexpected ones (like the generic RuntimeException and Exception handlers, which should log with ERROR level and include the stack trace ex). For expected business exceptions (like ResourceNotFoundException, InsufficientSeatsException), logging at WARN level with just the message is often sufficient.
RestTemplate Error Handling:
In services that use RestTemplate to call other services (like BookingService calls ShowCatalogService, or ShowDataSyncService calls AdminService and UserService):
Specifically catch HttpClientErrorException.NotFound if you want to treat a 404 from a downstream service as a "resource not found" in the current service's context (and re-throw your local ResourceNotFoundException).
Specifically catch HttpClientErrorException.BadRequest if a 400 from downstream means something specific.
Catch broader HttpClientErrorException (for other 4xx/5xx client/server errors from downstream) or RestClientException (for I/O errors, connection issues, etc.). Decide how to map these. Often, these translate to a 500 or 502 (Bad Gateway) in the calling service, indicating an issue with a dependency. You might create a ServiceDependencyException.
Your ShowDataSyncService already has some of this, which is good. Review other RestTemplate usages.
Clarity of Error Messages:
For messages that go into ErrorResponseDto.message and are seen by the client, try to make them clear and actionable if possible, without revealing internal system details (especially for 500 errors).
For server-side logs, include as much context as possible.
B. Addressing Skipped Error Scenarios (After General Refinement):
Once you're happy with the general exception handling structure:
Test Booking Insufficient Seats:
The Challenge: Reliably setting up a showtime with a specific, low number of available seats in show-catalog-service that booking-service will see.
Possible Solution for Testing:
Temporarily modify ShowController.getShowtimeDetailsForBooking in show-catalog-service. Instead of using st.getTotalSeats() for availableSeats in ShowtimeDetailsDto, have it return a hardcoded low number (e.g., 1) if the requested showtimeId matches a specific test ID you know. This is purely for making the test condition controllable.
Run setup_basic_show_environment in the Python script. This will create a showtime. Note its catalog_showtime_id.
In your modified ShowController, if showtimeId == <noted_catalog_showtime_id>, then set availableSeats = 1 in the DTO.
Then, in the Python test, try to book 2 tickets for this catalog_showtime_id.
Expected: booking-service should get the ShowtimeDetailsDto with availableSeats: 1, see the request for 2 tickets, throw InsufficientSeatsException, and GlobalExceptionHandler should return a 409.
Cleanup: Remember to revert the temporary modification in ShowController after testing.
Test Accessing Another User's Booking:
The Challenge: The setup part of this test in Python (test_access_others_booking) also relies on creating temporary entities and having them sync.
Focus:
First, ensure the core logic in BookingService.getBookingByIdForCurrentUser correctly throws AccessDeniedException when !booking.getUserId().equals(currentUsername) && !isAdmin. This part seems okay from the code.
Ensure GlobalExceptionHandler in booking-service has the @ExceptionHandler(AccessDeniedException.class) method that returns a 403 with the ErrorResponseDto. This also seems okay from the code.
To make the Python test more reliable for this specific scenario:
Instead of creating a completely new show/showtime within the test function, could it reuse a showtime created by setup_basic_show_environment (run once at the start of the script if needed)?
Refined Startup and Test Sequence (AGAIN, WITH MORE EMPHASIS ON WAITS):
Stop ALL Sparklix services.
Action A: Start SparklixEurekaServerApplication ONLY.
Wait until its console is calm and it shows "Tomcat started on port 8761".
Open http://localhost:8761 in your browser. Do not proceed until you see the Eureka dashboard.
Action B: Start SparklixUserServiceApplication.
Wait for its console to show successful registration with Eureka (e.g., "registration status: 204" and it being able to heartbeat).
Refresh Eureka dashboard: SPARKLIX-USER-SERVICE (or USER-SERVICE if you changed the name) must be UP.
Action C: Start SparklixAdminServiceApplication.
Wait for successful Eureka registration.
Refresh Eureka dashboard: ADMIN-SERVICE must be UP.
Action D: Start SparklixApiGatewayApplication.
CRITICAL: Watch its console. It MUST NOT show "Connection refused" to Eureka. It should log that it's fetching the registry and registering itself.
Refresh Eureka dashboard: API-GATEWAY must be UP.
WAIT an extra 30-45 seconds AFTER the Gateway is UP in Eureka. This allows its internal caches and load balancer clients to fully initialize with the instances from Eureka.
Action E: Start SparklixShowCatalogServiceApplication.
Wait for successful Eureka registration.
Refresh Eureka dashboard: SHOW-CATALOG-SERVICE must be UP.
Action F: WAIT for ShowDataSyncService's Sync Cycle:
Your catalog.sync.initial.delay.ms is 20000 (20 seconds).
Your catalog.sync.rate.ms is 300000 (5 minutes).
Wait at least 30-40 seconds after show-catalog-service starts.
Check show-catalog-service logs:
CATALOG-SYNC: Attempting to fetch new service token... -> What is the outcome? Does it get a token or a 503?
Check api-gateway logs: When show-catalog-service attempts the login, does the gateway log show a successful forward to user-service or an error finding user-service?
Check user-service logs: Does it receive the login request for catalog-sync-agent?
If token fetch is successful, then check show-catalog-service and admin-service logs for the data sync call.
Action G: Run python test_full_booking_flow.py (or test_show_catalog_via_gateway.py if you just want to check catalog content).

In the meantime, while you gather those files, remember the startup order:
sparklix-eureka-server (MUST BE FIRST AND FULLY RUNNING)
sparklix-user-service
sparklix-admin-service
sparklix-api-gateway
sparklix-show-catalog-service
sparklix-payment-service
sparklix-booking-service