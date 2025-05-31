import requests
import json
import time
import uuid

# --- Configuration ---
GATEWAY_URL = "http://localhost:8080"
ADMIN_USERNAME = "adminuser"
ADMIN_PASSWORD = "adminpass"

# !!! IMPORTANT: SET THIS MANUALLY before running the script !!!
# Get this ID from a showtime that exists in your show-catalog-service
# after ShowDataSyncService has run.
# You can run test_full_booking_flow.py first and use one of the
# `showtime_id_catalog` values it successfully uses for booking.
VALID_CATALOG_SHOWTIME_ID = 828 # <--- REPLACE WITH A VALID ID FROM YOUR SYSTEM

# --- Helper Functions (from previous scripts) ---
def get_unique_suffix():
    return uuid.uuid4().hex[:6]

def print_response(response, action="Response", expected_status=None, return_json=True):
    print(f"\n--- {action} ---")
    print(f"URL: {response.request.method} {response.url}")
    if response.request.body:
        try:
            body_json = json.loads(response.request.body)
            print(f"Request Body: {json.dumps(body_json, indent=2)}")
        except (json.JSONDecodeError, TypeError):
            try:
                print(f"Request Body: {response.request.body.decode()}")
            except:
                print(f"Request Body: {response.request.body}")

    print(f"Status Code: {response.status_code}")
    if expected_status:
        if isinstance(expected_status, list): # Allow list of expected statuses
            if response.status_code in expected_status:
                print(f"SUCCESS: Expected status in {expected_status} received ({response.status_code}).")
            else:
                print(f"FAILURE: Expected status in {expected_status}, but received {response.status_code}.")
        elif response.status_code == expected_status:
            print(f"SUCCESS: Expected status {expected_status} received.")
        else:
            print(f"FAILURE: Expected status {expected_status}, but received {response.status_code}.")
    
    response_content = None
    try:
        response_json = response.json()
        print(f"Response JSON: {json.dumps(response_json, indent=2)}")
        if return_json:
            response_content = response_json
    except json.JSONDecodeError:
        print(f"Response Text: {response.text}")
        if return_json:
            response_content = response.text
    finally:
        print("--------------------")
    return response_content

def login(username, password, role_for_log="User"):
    print(f"\nAttempting login for {role_for_log}: {username}")
    login_payload = {"usernameOrEmail": username, "password": password}
    action_name = f"Login for {username}"
    try:
        response = requests.post(f"{GATEWAY_URL}/api/auth/login", json=login_payload)
        response_data = print_response(response, action_name) # Removed expected_status for general printing
        if response.ok and response_data and response_data.get("token"):
            print(f"Login successful for {username}. Token obtained.")
            return response_data.get("token")
        else:
            print(f"Login failed for {username}.")
            return None
    except requests.exceptions.ConnectionError as e:
        print(f"ERROR: Could not connect to API Gateway at {GATEWAY_URL} for {action_name}. Is it running? {e}")
        return None 
    except Exception as e:
        print(f"An unexpected error occurred during {action_name}: {e}")
        return None

def register_user(username_suffix, name_prefix="Test User"):
    username = f"canceltestuser_{username_suffix}"
    email = f"canceltest_{username_suffix}@example.com"
    password = "password123"
    name = f"{name_prefix} {username_suffix}"

    print(f"\nAttempting to register user: {username}")
    register_payload = {
        "name": name,
        "username": username,
        "email": email,
        "password": password,
        "roles": ["USER"]
    }
    response = requests.post(f"{GATEWAY_URL}/api/auth/register", json=register_payload)
    action_name = f"Registration for {username}"
    
    # Check for successful registration (200 or 201) or if user already exists (400)
    if response.status_code in [200, 201]:
        print_response(response, action_name, expected_status=response.status_code)
        print(f"User {username} processed successfully (Registered or Existed).")
        return username, password
    elif response.status_code == 400:
        response_data = print_response(response, action_name, expected_status=400)
        if response_data and isinstance(response_data, dict) and "already taken" in response_data.get("message","").lower():
            print(f"User {username} or email {email} already exists. Will use these credentials.")
            return username, password
        else:
            print(f"User registration failed with 400, but not due to 'already taken'.")
            return None, None
    else:
        print_response(response, action_name)
        print(f"User registration failed for {username} with status {response.status_code}.")
        return None, None

# --- Test Flow ---
def test_booking_cancellation():
    print(">>> Starting Booking Cancellation Test Script <<<")

    if VALID_CATALOG_SHOWTIME_ID is None:
        print("ERROR: VALID_CATALOG_SHOWTIME_ID is not set. Please set it before running.")
        return

    user_suffix = get_unique_suffix()
    username, password = register_user(user_suffix, name_prefix="CancelUser")
    if not username:
        print("Failed to register/prepare user for cancellation test. Aborting.")
        return

    user_token = login(username, password, f"User ({username})")
    if not user_token:
        print(f"Failed to log in user {username}. Aborting cancellation test.")
        return
    
    headers_user = {"Authorization": f"Bearer {user_token}", "Content-Type": "application/json"}
    
    created_booking_id = None

    # 1. Create a booking
    print(f"\n>>> Creating a booking for user {username} and showtime {VALID_CATALOG_SHOWTIME_ID}...")
    booking_payload = {"showtimeId": VALID_CATALOG_SHOWTIME_ID, "numberOfTickets": 1}
    response_create = requests.post(f"{GATEWAY_URL}/api/bookings", headers=headers_user, json=booking_payload)
    booking_data = print_response(response_create, "Create Booking for Cancellation Test", expected_status=201)

    if not response_create.ok or not booking_data or not booking_data.get("bookingId"):
        print("Failed to create booking for cancellation test. Aborting.")
        return
    created_booking_id = booking_data.get("bookingId")
    print(f"Booking created successfully with ID: {created_booking_id}, Status: {booking_data.get('bookingStatus')}")

    # 2. Attempt to cancel the newly created booking
    print(f"\n>>> Attempting to cancel booking ID: {created_booking_id} (User: {username})")
    cancel_url = f"{GATEWAY_URL}/api/bookings/{created_booking_id}/cancel"
    response_cancel1 = requests.put(cancel_url, headers=headers_user) # Using PUT as per controller
    cancelled_booking_data = print_response(response_cancel1, f"Cancel Booking Attempt 1 (ID: {created_booking_id})", expected_status=200)

    if response_cancel1.ok and cancelled_booking_data and cancelled_booking_data.get("bookingStatus") == "CANCELLED":
        print(f"SUCCESS: Booking ID {created_booking_id} cancelled successfully.")
    else:
        print(f"FAILURE: Failed to cancel booking ID {created_booking_id} or status not updated to CANCELLED.")

    # 3. Attempt to cancel the same booking again
    print(f"\n>>> Attempting to cancel booking ID: {created_booking_id} AGAIN (should fail)")
    response_cancel2 = requests.put(cancel_url, headers=headers_user)
    # Expecting 400 Bad Request due to BookingCancellationException ("Booking is already cancelled.")
    print_response(response_cancel2, f"Cancel Booking Attempt 2 (ID: {created_booking_id})", expected_status=400) 


    # (Optional) Setup for another user's booking to test access denied - more involved
    # For now, this script focuses on the primary cancellation flow.
    # To test access denied for cancellation, you would:
    # 1. Create booking_A with user_A.
    # 2. Login as user_B.
    # 3. user_B attempts to cancel booking_A's ID. Expect 403.
    # This is similar to test_access_others_booking but for the PUT /cancel endpoint.

    print("\n>>> Booking Cancellation Test Script Finished <<<")

if __name__ == "__main__":
    # Ensure services are running and a valid showtime ID is available
    # Run your database clear script before this if you want a fresh state each time.
    if VALID_CATALOG_SHOWTIME_ID == "REPLACE_WITH_VALID_ID" or VALID_CATALOG_SHOWTIME_ID is None: # Added None check
        print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
        print("!!! ERROR: VALID_CATALOG_SHOWTIME_ID is not set in the script.           !!!")
        print("!!! Please edit the script and replace it with a real catalogShowtimeId. !!!")
        print("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!")
    else:
        test_booking_cancellation()


