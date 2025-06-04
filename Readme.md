# 🎬 Sparklix - BookAShow Microservices Platform

## ✨ Project Objective

Sparklix is a modern, microservices-based application designed to provide a seamless experience for users to browse and book tickets for various entertainment events, including movies, plays, and concerts. Built on a robust Java stack, it leverages Spring Boot and Spring Cloud to ensure high levels of security, scalability, and maintainability through a set of independent, RESTful APIs.

## 🚀 Current Project Status & Implemented Features

The Sparklix platform is organized as a Maven multi-module project. All core microservices are actively running, registering with Eureka for service discovery, and routing requests efficiently via the API Gateway. Inter-service communication is secured using JWT validation, and essential end-to-end flows are fully functional and verified.

### Core Modules:

*   **`sparklix-parent`**:
    *   **Status:** ✅ Complete
    *   **Details:** The foundational Maven Parent POM, managing common dependencies (Spring Boot 3.3.0, Spring Cloud 2023.0.2, Lombok, JJWT 0.11.5, JUnit 5), standardized build plugin configurations, and Java 17 versioning across all child modules.

*   **`sparklix-eureka-server`**:
    *   **Status:** ✅ Complete & Running
    *   **Purpose**: Acts as the central Service Discovery Server, enabling dynamic registration and lookup of microservice instances using Spring Cloud Netflix Eureka.
    *   **Port**: `8761`.

*   **`sparklix-user-service`**:
    *   **Status:** ✅ Core Functionality Complete & Running (with OTP Verification & Account Deletion)
    *   **Purpose**: The primary Authentication Server, handling user identity management. This includes secure user registration (now with email-based OTP verification), login (supports username or email), robust role management (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_VENDOR`), JWT issuance, and user-initiated account deletion (freeing up credentials for re-registration).
    *   **Port**: `8081`.
    *   **Database**: `sparklix_user_db` (MySQL).
    *   **Key Features**: JWT generation, `CustomUserDetailsService`, OTP generation/validation/resend, account activation post-OTP, account self-deletion, email sending via Notification Service for OTP & welcome messages.

*   **`sparklix-admin-service`**:
    *   **Status:** ✅ CRUD Operations, Security & Event Publishing Verified
    *   **Purpose**: Provides administrative functionalities for managing shows, venues, and showtimes. Acts as a Resource Server.
    *   **Port**: `8082`.
    *   **Database**: `sparklix_admin_db` (MySQL).
    *   **Key Features**: Secure CRUD for Shows, Venues, Showtimes. Internal API for catalog data sync. Publishes Showtime Update/Cancellation events to RabbitMQ. Can approve/reject vendor-submitted shows (triggering notifications).

*   **`sparklix-show-catalog-service`**:
    *   **Status:** ✅ Data Sync, GET Endpoints & Review Feature Verified
    *   **Purpose**: Public-facing catalog for users to browse show listings and manage reviews.
    *   **Port**: `8083`.
    *   **Database**: `sparklix_show_db` (MySQL).
    *   **Key Features**: Scheduled data synchronization from `admin-service`. Public GET endpoints for shows, search, genre filtering. Authenticated users (`ROLE_USER`/`ROLE_VENDOR`) can submit reviews with duplicate prevention.

*   **`sparklix-api-gateway`**:
    *   **Status:** ✅ Configured & Running
    *   **Purpose**: Central entry point for all client requests via Spring Cloud Gateway.
    *   **Port**: `8080`.
    *   **Details**: Service discovery-based routing.

*   **`sparklix-booking-service`**:
    *   **Status:** ✅ Core Booking Logic, Payment Integration & Notifications Verified
    *   **Purpose**: Manages ticket reservations and the booking lifecycle.
    *   **Port**: `8084`.
    *   **Database**: `sparklix_booking_db` (MySQL).
    *   **Key Features**: Processes booking requests, integrates with Payment Service, updates booking statuses. Listens to RabbitMQ for showtime changes and notifies users. Secure S2S calls.

*   **`sparklix-payment-service`**:
    *   **Status:** ✅ Payment Order Creation & Webhook Handling Verified
    *   **Purpose**: Handles payment gateway interactions (Razorpay).
    *   **Port**: `8085`.
    *   **Key Features**: Razorpay API integration, creates payment orders, validates webhook signatures, notifies `booking-service` of payment outcomes.

*   **`sparklix-notification-service`**:
    *   **Status:** ✅ Email Sending via MailerSend Verified
    *   **Purpose**: Dedicated service for sending notifications (currently email).
    *   **Port**: `8086`.
    *   **Key Features**: Configured with MailerSend SMTP for reliable email delivery.

## 🔑 Key Features & Verified End-to-End Flows:

*   ✅ **User & Vendor Lifecycle (OTP Secured):**
    *   New users/vendors register via distinct paths.
    *   Receive an OTP via email for account verification.
    *   Accounts are activated only after successful OTP validation.
    *   Login using either username or email.
    *   Users/Vendors can delete their own accounts, freeing up credentials for re-use.
*   ✅ **Role-Based Access Control (RBAC):** Granular access control (`ROLE_USER`, `ROLE_ADMIN`, `ROLE_VENDOR`) enforced across all relevant service endpoints.
*   ✅ **Inter-Service Data Synchronization:** `admin-service` data (shows, showtimes, venues) is successfully synced to `show-catalog-service`.
*   ✅ **Show Browsing & Reviews:** Public browsing of the show catalog, search functionality, and authenticated review submission for users and vendors.
*   ✅ **Full Booking Lifecycle:**
    *   Users create bookings for available showtimes.
    *   Payment is initiated via `payment-service` (Razorpay).
    *   Simulated payment gateway webhooks successfully trigger booking confirmation.
    *   Users can cancel their bookings.
*   ✅ **Event-Driven Notifications (RabbitMQ & Direct Calls):**
    *   **User Service:** OTP & Welcome emails. Account deletion confirmation (optional).
    *   **Booking Service:** Booking confirmation, cancellation, and payment failure emails. Showtime update/cancellation notifications (triggered by events from Admin Service via RabbitMQ).
    *   **Admin Service:** Vendor show approval/rejection notifications.
*   ✅ **Secure Service-to-Service (S2S) Communication:** Internal API calls (e.g., Booking to User, Admin to User, services to Notification) are authenticated using JWTs obtained for a dedicated service account (`catalog-sync-agent`).
*   ✅ **Robust Error Handling & Logging:** Consistent JSON error responses and detailed AOP-based logging across services.

## 🛠️ Tech Stack:

*   **Language**: Java 17
*   **Frameworks**: Spring Boot 3.3.0, Spring Cloud 2023.0.2 (Eureka, Gateway, LoadBalancer, Spring AMQP for RabbitMQ)
*   **Security**: Spring Security, JWT (io.jsonwebtoken:jjwt 0.11.5)
*   **Data Access**: Spring Data JPA, Hibernate
*   **Database**: MySQL 8.x
*   **Messaging**: RabbitMQ (for asynchronous event-driven communication)
*   **HTTP Client**: `RestTemplate` (with `@LoadBalanced`)
*   **Build Tool**: Maven
*   **Development Tools**: STS 4 / IntelliJ IDEA / VS Code, Postman, Newman, Python `requests`
*   **Email Service**: MailerSend (via SMTP)
*   **Utility**: Lombok, AOP for Logging, Springdoc OpenAPI (for Swagger UI)

---

## 🚀 Getting Started: Booting Up the Sparklix Platform

Follow these steps to get all Sparklix microservices up and running on your local machine.

### Prerequisites:

*   **Java 17 JDK** (or newer compatible with Spring Boot 3.3.0) installed and configured (JAVA_HOME set).
*   **Maven 3.6+** installed and configured (MAVEN_HOME set, and `mvn` in PATH).
*   **MySQL Server** (version 8.x recommended) running locally.
    *   Default credentials expected: `root` / `1122`. Adjust `application.properties` in each service if different.
    *   The `admin-service` expects a user `sparklixadmin` with password `adminpass123` for its specific database (`sparklix_admin_db`). Ensure this user and privileges are set up if you don't rely on `ddl-auto: create` to make the DB user.
*   **Docker Desktop** installed and running (this is the easiest way to run RabbitMQ locally).
*   An IDE like **Spring Tool Suite 4 (STS), IntelliJ IDEA, or VS Code with Java extensions.**
*   **Git** for cloning the repository.

### Setup Steps:

1.  **Clone the Repository:**
    ```bash
    git clone <your-repository-url>
    cd sparklix 
    ```
    (If working from a local folder, navigate to `C:\Users\Aniket\Downloads\sparklix`)

2.  **Start RabbitMQ (using Docker):**
    Open your terminal/command prompt and run:
    ```bash
    docker run -d --hostname my-rabbit --name some-rabbit -p 5672:5672 -p 15672:15672 rabbitmq:3-management
    ```
    *   This starts RabbitMQ detached (`-d`).
    *   AMQP port: `localhost:5672`
    *   Management UI: `http://localhost:15672` (default credentials: `guest` / `guest`)

3.  **Database Initialization (User Service - First Time Only):**
    *   Navigate to: `sparklix-user-service/src/main/resources/application.properties`
    *   **For the very first run OR if you need to completely reset `sparklix_user_db`:**
        Set `spring.sql.init.mode=always`
    *   Run `sparklix-user-service` once (see step 5). This will create/update tables and execute `data.sql` to populate roles (`ROLE_ADMIN`, `ROLE_USER`, `ROLE_VENDOR`) and default users.
    *   **IMPORTANT:** After this initial run and successful startup, **stop `sparklix-user-service` and revert `spring.sql.init.mode` back to `update` or `never`** in its `application.properties`. This prevents accidental data wipes on subsequent starts.

4.  **Build All Microservices with Maven:**
    From the root `sparklix` directory (where the parent `pom.xml` is):
    ```bash
    mvn clean install -DskipTests
    ```
    (`-DskipTests` can be used initially to speed up the build; ensure tests pass later).

5.  **Start Microservices (Strict Order is Crucial):**
    Open a **new terminal/command prompt for each service**. Navigate to its module directory (e.g., `cd sparklix-eureka-server`) and run its JAR:
    ```bash
    java -jar target/<service-artifact-name>-0.0.1-SNAPSHOT.jar
    ```
    (Replace `<service-artifact-name>` e.g., `sparklix-eureka-server`)

    *   **1. `sparklix-eureka-server`** (Port: `8761`)
        *   Wait for: `Tomcat started on port 8761` and `Started SparklixEurekaServerApplication`.
        *   Verify: Open `http://localhost:8761` in your browser. You should see the Eureka dashboard.

    *   **2. `sparklix-user-service`** (Port: `8081`)
        *   Wait for: `Tomcat started on port 8081` and successful registration with Eureka (check Eureka dashboard for `SPARKLIX-USER-SERVICE` listed as UP).

    *   **3. `sparklix-admin-service`** (Port: `8082`)
        *   Wait for: `Tomcat started on port 8082` and registration with Eureka (check dashboard for `ADMIN-SERVICE`).

    *   **4. `sparklix-api-gateway`** (Port: `8080`)
        *   Wait for: `Tomcat started on port 8080` and registration with Eureka.
        *   **CRITICAL:** Wait an **additional 30-45 seconds** after the gateway shows as UP in Eureka. This allows its internal client-side load balancer to fully initialize and discover other services.

    *   **5. `sparklix-notification-service`** (Port: `8086`)
        *   Wait for: `Tomcat started on port 8086` and registration with Eureka. Ensure MailerSend credentials are correct in its `application.properties`.

    *   **6. `sparklix-show-catalog-service`** (Port: `8083`)
        *   Wait for: `Tomcat started on port 8083` and registration with Eureka.
        *   Monitor its logs for `SYNC_CYCLE_START` and `SYNC_CYCLE_SUCCESS` (or `SYNC_CYCLE_END_EMPTY` if admin has no shows yet). This happens after an initial delay.

    *   **7. `sparklix-payment-service`** (Port: `8085`)
        *   Wait for: `Tomcat started on port 8085` and registration with Eureka.

    *   **8. `sparklix-booking-service`** (Port: `8084`)
        *   Wait for: `Tomcat started on port 8084` and registration with Eureka.

    🎉 Once all services are running and show as `UP` in the Eureka Dashboard, your Sparklix platform is ready for API testing!

---

## 🧪 API Testing

Refer to the `sparklix_tests.md` file (or the "End-to-End User Flow Testing with Postman" section if you keep it here) for detailed steps on how to test the various functionalities using Postman.
Automated Python scripts for end-to-end testing are available in the `/api-tests` directory.

---

## 📚 Documentation & Diagrams

*   **API Documentation (Swagger UI):** Each microservice exposes its API documentation via Swagger UI. Access it at `http://localhost:<service-port>/swagger-ui.html` (e.g., `http://localhost:8081/swagger-ui.html` for User Service).
*   **(TODO) Formal Design Diagrams:** UML Use Case, Class Diagrams, ER Diagrams, and Microservice Architecture diagrams will be added.

## ⏭️ Next Steps & Future Enhancements

*   Implement comprehensive Unit & Integration Tests (JUnit, Mockito).
*   Develop full Inventory Management for showtime seat availability.
*   Expand Vendor Portal functionalities (show submission, management, reporting).
*   Build out the `sparklix-analytics-service`.
*   Develop the Frontend User Interface.
*   Integrate SonarQube for continuous code quality analysis.
*   Enhance S2S security beyond shared service accounts (e.g., OAuth2 client credentials).
*   Explore SMS notifications via `sparklix-notification-service`.

---