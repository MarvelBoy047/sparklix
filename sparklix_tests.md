# 🧪 Sparklix - API Testing Guide (Postman)

This document outlines the steps to test the key functionalities of the Sparklix microservices platform using Postman.

## Prerequisites

1.  **All Sparklix microservices are running** as per the instructions in `Readme.md` (including Eureka, RabbitMQ, and all Java services).
2.  **Postman Desktop Application** is installed.
3.  **OpenAPI Definitions Imported (Recommended):**
    *   For each service, navigate to `http://localhost:<service-port>/v3/api-docs` (e.g., `http://localhost:8081/v3/api-docs` for User Service).
    *   Save the JSON content.
    *   In Postman, click "Import" -> "Upload Files" and import the saved JSON. This will create a collection with pre-configured requests for that service.
4.  **MailerSend Administrator Email:** For OTP email delivery tests during the MailerSend trial, you **must** use your MailerSend administrator email address as the recipient when registering new users/vendors.

## ⚙️ General Postman Setup

*   **Base URL:** All API calls should be directed through the API Gateway: `http://localhost:8080`
*   **Authentication Header:** For requests requiring authentication, add the following header:
    *   **Key:** `Authorization`
    *   **Value:** `Bearer <YOUR_JWT_TOKEN>` (Replace `<YOUR_JWT_TOKEN>` with the token obtained from a login step).
*   **Content-Type Header:** For `POST` and `PUT` requests with a JSON body:
    *   **Key:** `Content-Type`
    *   **Value:** `application/json`

---

## 👤 User Flow Testing

These steps cover the lifecycle of a regular user (`ROLE_USER`).

###  1. User Registration (Initiate OTP)

*   **Purpose:** Register a new user and trigger OTP email.
*   **Method:** `POST`
*   **URL:** `{{GATEWAY_URL}}/api/auth/register`
    *   (Define `GATEWAY_URL` as a Postman environment variable: `http://localhost:8080`)
*   **Headers:** `Content-Type: application/json`
*   **Body (raw, JSON):**
    ```json
    {
        "name": "Test User One",
        "username": "testuser001", // Ensure this username is unique for each new test
        "email": "YOUR_MAILERSEND_ADMIN_EMAIL@example.com", // To receive OTP
        "password": "password123"
    }
    ```
    <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
*   **Expected Status:** `200 OK`
*   **Expected Body:** `{"message": "User registered successfully! Please check your email for OTP verification."}` (or similar if user was unverified and OTP was resent).
*   **Action:** Check the specified email for the OTP.

### 2. Verify OTP

*   **Purpose:** Activate the newly registered user account.
*   **Method:** `POST`
*   **URL:** `{{GATEWAY_URL}}/api/auth/verify-otp`
*   **Headers:** `Content-Type: application/json`
*   **Body (raw, JSON):**
    ```json
    {
        "email": "YOUR_MAILERSEND_ADMIN_EMAIL@example.com", // Same email as registration
        "otp": "OTP_FROM_EMAIL" 
    }
    ```
    <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
*   **Expected Status:** `200 OK`
*   **Expected Body:** `{"message": "OTP verified successfully. Your account is now active!"}`
*   **Action:** Check email for a welcome message.

### 3. User Login

*   **Purpose:** Log in as the verified user and obtain a JWT.
*   **Method:** `POST`
*   **URL:** `{{GATEWAY_URL}}/api/auth/login`
*   **Headers:** `Content-Type: application/json`
*   **Body (raw, JSON):**
    ```json
    {
        "usernameOrEmail": "testuser001", // Or the email used
        "password": "password123"
    }
    ```
    <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
*   **Expected Status:** `200 OK`
*   **Expected Body (Example):**
    ```json
    {
        "token": "eyJhbGciOiJIUzI1NiJ9...",
        "username": "testuser001",
        "roles": ["ROLE_USER"],
        "message": null
    }
    ```
    <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
*   **Action:** **Save the `token` value.** This will be your `USER_JWT_TOKEN` for subsequent steps.

### 4. Browse Shows (Public)

*   **Purpose:** View available shows.
*   **Method:** `GET`
*   **URL:** `{{GATEWAY_URL}}/api/shows`
*   **Expected Status:** `200 OK`
*   **Action:** Note a `show_id` and a `showtimeId` (from within a show's showtimes array) for later use.

### 5. Post a Review

*   **Purpose:** Allow the logged-in user to post a review.
*   **Method:** `POST`
*   **URL:** `{{GATEWAY_URL}}/api/shows/<YOUR_CHOSEN_SHOW_ID>/reviews`
*   **Headers:**
    *   `Authorization: Bearer <USER_JWT_TOKEN>`
    *   `Content-Type: application/json`
*   **Body (raw, JSON):**
    ```json
    {
        "rating": 5,
        "comment": "This show was absolutely fantastic! A must-see."
    }
    ```
    <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
*   **Expected Status:** `201 Created`

### 6. Create a Booking

*   **Purpose:** User books tickets for a showtime.
*   **Method:** `POST`
*   **URL:** `{{GATEWAY_URL}}/api/bookings`
*   **Headers:**
    *   `Authorization: Bearer <USER_JWT_TOKEN>`
    *   `Content-Type: application/json`
*   **Body (raw, JSON):**
    ```json
    {
        "showtimeId": "<YOUR_CHOSEN_SHOWTIME_ID>", 
        "numberOfTickets": 1
    }
    ```
    <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
*   **Expected Status:** `201 Created`
*   **Expected Body:** Contains `bookingId`, `bookingStatus: "PENDING_PAYMENT"`, `razorpayOrderId`, `amountInPaisaForPayment`.
*   **Action:** **Save `bookingId`, `razorpayOrderId`, and `amountInPaisaForPayment`**.

### 7. Simulate Payment (Webhook)

*   **Purpose:** Simulate a successful payment callback from Razorpay to confirm the booking.
*   **Method:** `POST`
*   **URL:** `http://localhost:8085/api/payments/webhook/razorpay` (*Note: Direct to payment-service*)
*   **Headers:** `Content-Type: application/json`
*   **Body (raw, JSON):**
    ```json
    {
      "event": "payment.captured",
      "payload": {
        "payment": {
          "entity": {
            "id": "pay_test_user_flow_random", 
            "order_id": "<RAZORPAY_ORDER_ID_FROM_STEP_U6>",
            "status": "captured",
            "amount": "<AMOUNT_IN_PAISA_FROM_STEP_U6>",
            "currency": "INR",
            "notes": {
              "internal_booking_id": "<BOOKING_ID_FROM_STEP_U6>"
            }
          }
        }
      }
    }
    ```
    <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
*   **Expected Status:** `200 OK`
*   **Body:** `"Webhook event processed"`
*   **Action:** Wait ~10 seconds. Check email for booking confirmation.

### 8. Check My Bookings

*   **Purpose:** Verify booking status is now `CONFIRMED`.
*   **Method:** `GET`
*   **URL:** `{{GATEWAY_URL}}/api/bookings/my-bookings`
*   **Headers:** `Authorization: Bearer <USER_JWT_TOKEN>`
*   **Expected Status:** `200 OK`
*   **Expected Body:** Array containing the booking from Step U6, now with `bookingStatus: "CONFIRMED"`.

### 9. Cancel Booking

*   **Purpose:** User cancels their confirmed booking.
*   **Method:** `PUT`
*   **URL:** `{{GATEWAY_URL}}/api/bookings/<BOOKING_ID_FROM_STEP_U6>/cancel`
*   **Headers:** `Authorization: Bearer <USER_JWT_TOKEN>`
*   **Expected Status:** `200 OK`
*   **Expected Body:** Booking details with `bookingStatus: "CANCELLED"`.
*   **Action:** Check email for cancellation confirmation.

### 10. Delete Own Account

*   **Purpose:** User deletes their own account.
*   **Method:** `DELETE`
*   **URL:** `{{GATEWAY_URL}}/api/auth/delete-my-account`
*   **Headers:** `Authorization: Bearer <USER_JWT_TOKEN>`
*   **Expected Status:** `200 OK`
*   **Expected Body:** `{"message": "Account for user 'testuser001' has been successfully deleted."}`
*   **Verification:** Attempt Step U3 (Login) again; it should fail (e.g., `401 Unauthorized`).

---

## 🏢 Vendor Flow Testing

These steps cover the lifecycle of a vendor user (`ROLE_VENDOR`).

### 🏢 V1. Vendor Registration (Initiate OTP)

*   **Purpose:** Register a new vendor account.
*   **Method:** `POST`
*   **URL:** `{{GATEWAY_URL}}/api/auth/register-vendor`
*   **Headers:** `Content-Type: application/json`
*   **Body (raw, JSON):**
    ```json
    {
        "name": "Awesome Events Co.",
        "username": "awesome_vendor_01", // Ensure unique
        "email": "YOUR_MAILERSEND_ADMIN_EMAIL_FOR_VENDOR@example.com", // To receive OTP
        "password": "vendorPassSecure1"
    }
    ```
    <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
*   **Expected Status:** `200 OK`
*   **Expected Body:** `{"message": "Vendor registered successfully! Please check your email for OTP verification."}`
*   **Action:** Check the specified email for OTP.

### 🏢 V2. Verify Vendor OTP

*   **Purpose:** Activate the vendor account.
*   **Method:** `POST`
*   **URL:** `{{GATEWAY_URL}}/api/auth/verify-otp`
*   **Headers:** `Content-Type: application/json`
*   **Body (raw, JSON):**
    ```json
    {
        "email": "YOUR_MAILERSEND_ADMIN_EMAIL_FOR_VENDOR@example.com",
        "otp": "OTP_FROM_VENDOR_EMAIL" 
    }
    ```
    <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
*   **Expected Status:** `200 OK`
*   **Expected Body:** `{"message": "OTP verified successfully. Your account is now active!"}`

### 🏢 V3. Vendor Login

*   **Purpose:** Log in as the verified vendor.
*   **Method:** `POST`
*   **URL:** `{{GATEWAY_URL}}/api/auth/login`
*   **Headers:** `Content-Type: application/json`
*   **Body (raw, JSON):**
    ```json
    {
        "usernameOrEmail": "awesome_vendor_01",
        "password": "vendorPassSecure1"
    }
    ```
    <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
*   **Expected Status:** `200 OK`
*   **Expected Body:** JWT token, username, and `roles` should include `ROLE_VENDOR`.
*   **Action:** **Save this `VENDOR_JWT_TOKEN`**.

### 🏢 V4. Vendor Submits a Review (Shared Functionality)

*   (Same as User Step 4 & 5 to get a show_id and then post review, but use `VENDOR_JWT_TOKEN`)
*   **URL:** `{{GATEWAY_URL}}/api/shows/<YOUR_CHOSEN_SHOW_ID>/reviews`
*   **Headers:**
    *   `Authorization: Bearer <VENDOR_JWT_TOKEN>`
    *   `Content-Type: application/json`
*   **Body (raw, JSON):**
    ```json
    {
        "rating": 4,
        "comment": "As a vendor, this show platform is quite good!"
    }
    ```
    <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
*   **Expected Status:** `201 Created`

### 🏢 V5. Vendor Attempts Admin Action (Forbidden)

*   **Purpose:** Verify role segregation.
*   **Method:** `GET`
*   **URL:** `{{GATEWAY_URL}}/api/admin/hello`
*   **Headers:** `Authorization: Bearer <VENDOR_JWT_TOKEN>`
*   **Expected Status:** `403 Forbidden`

### 🏢 V6. Vendor Deletes Own Account

*   **Purpose:** Vendor deletes their own account.
*   **Method:** `DELETE`
*   **URL:** `{{GATEWAY_URL}}/api/auth/delete-my-account`
*   **Headers:** `Authorization: Bearer <VENDOR_JWT_TOKEN>`
*   **Expected Status:** `200 OK`
*   **Expected Body:** `{"message": "Account for user 'awesome_vendor_01' has been successfully deleted."}`

---

## 🛠️ Admin Flow Testing

These steps require `adminuser` credentials and token.

### 🔑 A1. Admin Login

*   **Method:** `POST`
*   **URL:** `{{GATEWAY_URL}}/api/auth/login`
*   **Headers:** `Content-Type: application/json`
*   **Body (raw, JSON):**
    ```json
    {
        "usernameOrEmail": "adminuser",
        "password": "adminpass"
    }
    ```
    <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
*   **Expected Status:** `200 OK`
*   **Action:** **Save this `ADMIN_JWT_TOKEN`**.

### 🛠️ A2. Admin Creates a Show

*   **Purpose:** Admin manages show data.
*   **Method:** `POST`
*   **URL:** `{{GATEWAY_URL}}/api/admin/management/shows`
*   **Headers:**
    *   `Authorization: Bearer <ADMIN_JWT_TOKEN>`
    *   `Content-Type: application/json`
*   **Body (raw, JSON):**
    ```json
    {
        "title": "Admin Special Premiere Event",
        "description": "A new blockbuster managed by admin.",
        "genre": "Action",
        "language": "English",
        "durationMinutes": 150,
        "releaseDate": "2025-12-01",
        "submittedByVendorUsername": "awesome_vendor_01" 
    }
    ```
    <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
    (Ensure `awesome_vendor_01` is a registered and OTP-verified vendor to test notifications properly).
*   **Expected Status:** `201 Created`
*   **Action:** Note the `id` of the created show (this is the `admin_show_id`).

### 🛠️ A3. Admin Approves Vendor's Show

*   **Purpose:** Test show approval workflow and vendor notification.
*   **Method:** `PUT`
*   **URL:** `{{GATEWAY_URL}}/api/admin/management/shows/<ADMIN_SHOW_ID_FROM_A2>/approve`
*   **Headers:** `Authorization: Bearer <ADMIN_JWT_TOKEN>`
*   **Expected Status:** `200 OK`
*   **Verification:** The vendor (`awesome_vendor_01` associated with `YOUR_MAILERSEND_ADMIN_EMAIL_FOR_VENDOR@example.com`) should receive an approval email.

### 🛠️ A4. Admin Triggers Showtime Update (RabbitMQ Event)

1.  **Create Venue & Showtime:**
    *   `POST {{GATEWAY_URL}}/api/admin/management/venues` (Body: `{"name": "MQ Test Venue", "address": "123 Rabbit St", "city": "EventCity", "capacity": 100}`) -> Get `venue_id`.
    *   `POST {{GATEWAY_URL}}/api/admin/management/showtimes` (Body: `{"showId": "<ADMIN_SHOW_ID_FROM_A2>", "venueId": "<venue_id>", "showDateTime": "2025-12-25T20:00:00", "pricePerSeat": 30.00, "totalSeats": 50}`) -> Get `admin_showtime_id`.
2.  **Create a Booking (as a normal user like `testuser001`) for the SYNCED version of this showtime.**
    *   Wait for catalog sync.
    *   Find the `catalog_showtime_id` corresponding to `admin_showtime_id`.
    *   Use `USER_JWT_TOKEN` to `POST {{GATEWAY_URL}}/api/bookings` for this `catalog_showtime_id`.
3.  **Admin Updates Showtime:**
    *   **Method:** `PUT`
    *   **URL:** `{{GATEWAY_URL}}/api/admin/management/showtimes/<ADMIN_SHOWTIME_ID>`
    *   **Headers:** `Authorization: Bearer <ADMIN_JWT_TOKEN>`, `Content-Type: application/json`
    *   **Body (raw, JSON - change the time or price):**
        ```json
        {
            "showId": "<ADMIN_SHOW_ID_FROM_A2>",
            "venueId": "<venue_id>",
            "showDateTime": "2025-12-25T21:00:00", 
            "pricePerSeat": 35.00,
            "totalSeats": 50
        }
        ```
        <button onclick="navigator.clipboard.writeText(this.previousElementSibling.innerText)">Copy Code</button>
    *   **Expected Status:** `200 OK`
    *   **Verification:** `testuser001` (or whoever made the booking) should receive an email notification about the showtime update, triggered by the RabbitMQ event listened to by `booking-service`. Check `booking-service` logs for "Received ShowtimeUpdateEvent".

---

This comprehensive testing guide covers the new OTP features, user/vendor deletion, and provides a structure for ongoing API validation. Remember to replace placeholders and use unique data where necessary for your tests.