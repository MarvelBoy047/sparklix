import requests
import json
import time
import uuid

# --- Configuration ---
GATEWAY_URL = "http://localhost:8080"
ADMIN_USERNAME = "adminuser"
ADMIN_PASSWORD = "adminpass"

# --- Helper Functions ---
# Using a timestamp-based suffix for more readable uniqueness if needed
def get_unique_suffix():
    return str(int(time.time()))[-6:] # Last 6 digits of current epoch time

def print_response(response, action="Response", expected_status=None, return_json=True):
    print(f"\n--- {action} ---")
    print(f"URL: {response.request.method} {response.url}") # Show method and URL
    if response.request.body:
        try:
            # Attempt to pretty print JSON body if it's JSON
            body_json = json.loads(response.request.body)
            print(f"Request Body: {json.dumps(body_json, indent=2)}")
        except (json.JSONDecodeError, TypeError):
            # Otherwise, print as string (might be bytes)
            try:
                print(f"Request Body: {response.request.body.decode()}")
            except:
                print(f"Request Body: {response.request.body}")


    print(f"Status Code: {response.status_code}")
    if expected_status:
        if response.status_code == expected_status:
            print(f"SUCCESS: Expected status {expected_status} received.")
        else:
            print(f"FAILURE: Expected status {expected_status}, but received {response.status_code}.")
    
    response_content = None
    try:
        response_json = response.json()
        print(f"Response JSON: {json.dumps(response_json, indent=2)}")
        if return_json:
            response_content = response_json
        if expected_status and response.status_code == expected_status and "error" in response_json:
             print(f"SUCCESS: Error response structure seems okay for an expected error.")
    except json.JSONDecodeError:
        print(f"Response Text: {response.text}")
        if return_json:
            response_content = response.text # Store text if not JSON
        if expected_status and response.status_code != expected_status and expected_status >= 400:
             print(f"FAILURE: Expected JSON error response, but got non-JSON for a {response.status_code} status.")
    finally:
        print("--------------------")
    return response_content


def login(username, password, role_for_log="User"):
    print(f"\nAttempting login for {role_for_log}: {username}")
    login_payload = {"usernameOrEmail": username, "password": password}
    action_name = f"Login for {username}"
    try:
        response = requests.post(f"{GATEWAY_URL}/api/auth/login", json=login_payload)
        response_data = print_response(response, action_name, expected_status=200 if username != "nonexistentuser" else None) # Don't expect 200 for bad login
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
    username = f"testuser_{username_suffix}"
    email = f"testuser_{username_suffix}@example.com"
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
    
    if response.status_code == 201 or \
       (response.status_code == 200 and response.json().get("message") == "User registered successfully!"):
        print_response(response, action_name, expected_status=response.status_code)
        print(f"User {username} registered successfully.")
        return username, password
    elif response.status_code == 400 and response.json() and "already taken" in response.json().get("message","").lower():
        print_response(response, action_name, expected_status=400)
        print(f"User {username} or email {email} already exists. Will use these credentials.")
        return username, password # Existing user, still usable for tests
    else:
        print_response(response, action_name)
        print(f"User registration failed for {username}.")
        return None, None

def trigger_catalog_sync(admin_headers):
    action = "Trigger Catalog Sync"
    print(f"\n--- {action} ---")
    try:
        response = requests.post(f"{GATEWAY_URL}/api/catalog/sync/trigger", headers=admin_headers)
        # Sync trigger might return 200 OK with simple text or 202 Accepted
        expected_statuses = [200, 202] 
        is_success = response.status_code in expected_statuses
        print_response(response, action, expected_status=response.status_code if is_success else None, return_json=False)
        if is_success:
            print("SUCCESS: Catalog sync triggered/request accepted.")
            print("Waiting a bit for sync to process (e.g., 15-20 seconds based on your faster schedule)...")
            time.sleep(20) # Increased wait time after triggering sync
            return True
        else:
            print(f"FAILURE: Catalog sync trigger failed with status {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"ERROR triggering catalog sync: {e}")
        return False


def setup_basic_show_environment(admin_headers, suffix_prefix="testshow"):
    """Creates a venue, show, and showtime in admin-service and attempts to find it in catalog after sync."""
    suffix = f"{suffix_prefix}_{get_unique_suffix()}"
    venue_name = f"Venue {suffix}"
    show_title = f"Show {suffix}"
    original_show_id_admin = None
    original_venue_id_admin = None
    original_showtime_id_admin = None
    catalog_showtime_id = None

    print(f"\n>>> Setting up test environment with suffix: {suffix}")

    # 1. Create Venue
    venue_payload = {"name": venue_name, "address": "1 Test St", "city": "Testville", "capacity": 10}
    resp_venue = requests.post(f"{GATEWAY_URL}/api/admin/management/venues", headers=admin_headers, json=venue_payload)
    venue_data = print_response(resp_venue, f"Create Venue {venue_name}", expected_status=201)
    if not venue_data or not venue_data.get("id"): return None, None, None, None
    original_venue_id_admin = venue_data["id"]

    # 2. Create Show
    show_payload = {"title": show_title, "description": "Test desc.", "genre": "Test", "language": "Py", "durationMinutes": 60, "releaseDate": "2025-10-10"}
    resp_show = requests.post(f"{GATEWAY_URL}/api/admin/management/shows", headers=admin_headers, json=show_payload)
    show_data = print_response(resp_show, f"Create Show {show_title}", expected_status=201)
    if not show_data or not show_data.get("id"): return None, None, None, None
    original_show_id_admin = show_data["id"]

    # 3. Create Showtime
    # For insufficient seats test, we'll use totalSeats: 1 and try to book 2
    showtime_total_seats = 1 if "insufficient" in suffix_prefix.lower() else 20
    showtime_payload = {"showId": original_show_id_admin, "venueId": original_venue_id_admin,
                        "showDateTime": "2025-12-01T20:00:00", "pricePerSeat": 10.00, "totalSeats": showtime_total_seats}
    resp_showtime = requests.post(f"{GATEWAY_URL}/api/admin/management/showtimes", headers=admin_headers, json=showtime_payload)
    showtime_data = print_response(resp_showtime, "Create Showtime", expected_status=201)
    if not showtime_data or not showtime_data.get("id"): return None, None, None, None
    original_showtime_id_admin = showtime_data["id"]

    print(f"Admin Setup: VenueID={original_venue_id_admin}, ShowAdminID={original_show_id_admin}, ShowtimeAdminID={original_showtime_id_admin}")

    # 4. Trigger sync and poll for the showtime
    if not trigger_catalog_sync(admin_headers):
        print("Skipping showtime search as sync trigger failed.")
        return None, None, None, None

    max_retries = 6  # Poll for up to 30 seconds (6 * 5s)
    retry_delay = 5
    catalog_show_id_for_this_run = None

    print(f"Polling for show with originalAdminId: {original_show_id_admin}")
    for i in range(max_retries):
        resp_cat_shows = requests.get(f"{GATEWAY_URL}/api/shows", headers=admin_headers)
        cat_shows_data = print_response(resp_cat_shows, f"Get Shows from Catalog (Attempt {i+1})")
        if resp_cat_shows.ok and cat_shows_data:
            for show_item in cat_shows_data:
                if show_item.get("originalShowId") == original_show_id_admin:
                    catalog_show_id_for_this_run = show_item.get("id")
                    print(f"Found synced show: LocalCatalogID={catalog_show_id_for_this_run}, OriginalAdminID={original_show_id_admin}")
                    break
        if catalog_show_id_for_this_run:
            break
        time.sleep(retry_delay)

    if not catalog_show_id_for_this_run:
        print(f"ERROR: Show with originalAdminId {original_show_id_admin} not found in catalog after polling.")
        return None, None, None, None

    print(f"Polling for showtime with originalAdminShowtimeId: {original_showtime_id_admin} under catalogShowId: {catalog_show_id_for_this_run}")
    for i in range(max_retries): # Poll again for showtimes of that specific show
        resp_cat_showtimes = requests.get(f"{GATEWAY_URL}/api/shows/{catalog_show_id_for_this_run}/showtimes", headers=admin_headers)
        cat_showtimes_data = print_response(resp_cat_showtimes, f"Get Showtimes for CatalogShowID {catalog_show_id_for_this_run} (Attempt {i+1})")
        if resp_cat_showtimes.ok and cat_showtimes_data:
            for st_item in cat_showtimes_data:
                if st_item.get("originalShowtimeId") == original_showtime_id_admin:
                    catalog_showtime_id = st_item.get("id")
                    print(f"Found synced showtime: LocalCatalogShowtimeID={catalog_showtime_id}, OriginalAdminShowtimeID={original_showtime_id_admin}")
                    break
        if catalog_showtime_id:
            break
        time.sleep(retry_delay)

    if not catalog_showtime_id:
        print(f"ERROR: Showtime with originalAdminId {original_showtime_id_admin} not found in catalog for show {catalog_show_id_for_this_run}.")
        return None, original_show_id_admin, original_venue_id_admin, None
        
    return catalog_showtime_id, original_show_id_admin, original_venue_id_admin, original_showtime_id_admin


# --- Test Scenarios ---

def test_booking_non_existent_showtime(user_headers):
    action = "Test Booking Non-Existent Showtime"
    print(f"\n>>> {action} <<<")
    non_existent_showtime_id = 9999999 # A very unlikely ID
    booking_payload = {"showtimeId": non_existent_showtime_id, "numberOfTickets": 1}
    response = requests.post(f"{GATEWAY_URL}/api/bookings", headers=user_headers, json=booking_payload)
    print_response(response, action, expected_status=404) # Expect 404 from booking service

def test_booking_insufficient_seats(user_headers, admin_headers):
    action = "Test Booking Insufficient Seats"
    print(f"\n>>> {action} <<<")
    
    # Setup a showtime with very few seats (e.g., 1 seat)
    print("Setting up showtime with 1 seat for insufficient seats test...")
    catalog_showtime_id_few_seats, _, _, _ = setup_basic_show_environment(admin_headers, suffix_prefix="insufficient_seats_show")

    if not catalog_showtime_id_few_seats:
        print(f"{action}: SKIPPED - Prerequisite catalog_showtime_id_few_seats not available from setup.")
        return

    # Attempt to book 2 tickets when only 1 is configured via totalSeats (assuming availableSeats mirrors totalSeats for now)
    booking_payload = {"showtimeId": catalog_showtime_id_few_seats, "numberOfTickets": 2} 
    response = requests.post(f"{GATEWAY_URL}/api/bookings", headers=user_headers, json=booking_payload)
    # Expected: 409 Conflict if BookingService throws InsufficientSeatsException based on catalog details
    # OR 400 Bad Request if the number of tickets is more than totalSeats (might be a different validation)
    # Let's aim for 409 as per InsufficientSeatsException
    print_response(response, action, expected_status=409) 

def test_access_others_booking(admin_headers):
    action = "Test Accessing Another User's Booking"
    print(f"\n>>> {action} <<<")
    
    # 1. User A registers and creates a booking
    user_a_suffix = get_unique_suffix()
    user_a_username, user_a_password = register_user(user_a_suffix, name_prefix="UserA")
    if not user_a_username: print(f"{action}: SKIPPED - User A registration failed."); return
    
    user_a_token = login(user_a_username, user_a_password, f"User A ({user_a_username})")
    if not user_a_token: print(f"{action}: SKIPPED - User A login failed."); return
    user_a_headers = {"Authorization": f"Bearer {user_a_token}", "Content-Type": "application/json"}

    # Setup a new showtime for User A to book (using admin_headers for setup)
    print("Setting up showtime for User A's booking...")
    catalog_showtime_id_for_a, _, _, _ = setup_basic_show_environment(admin_headers, suffix_prefix="user_a_show")
    if not catalog_showtime_id_for_a:
        print(f"{action}: SKIPPED - Failed to set up showtime for User A.")
        return

    booking_payload_a = {"showtimeId": catalog_showtime_id_for_a, "numberOfTickets": 1}
    response_booking_a = requests.post(f"{GATEWAY_URL}/api/bookings", headers=user_a_headers, json=booking_payload_a)
    booking_data_a = print_response(response_booking_a, "User A Booking Creation Attempt", expected_status=201)
    if not response_booking_a.ok or not booking_data_a or "bookingId" not in booking_data_a:
        print(f"{action}: SKIPPED - User A failed to create a booking.")
        return
    user_a_booking_id = booking_data_a["bookingId"]
    print(f"User A ({user_a_username}) created booking ID: {user_a_booking_id}")

    # 2. User B registers and tries to access User A's booking
    user_b_suffix = get_unique_suffix() # Ensure different suffix
    user_b_username, user_b_password = register_user(user_b_suffix, name_prefix="UserB")
    if not user_b_username: print(f"{action}: SKIPPED - User B registration failed."); return

    user_b_token = login(user_b_username, user_b_password, f"User B ({user_b_username})")
    if not user_b_token: print(f"{action}: SKIPPED - User B login failed."); return
    user_b_headers = {"Authorization": f"Bearer {user_b_token}", "Content-Type": "application/json"}

    print(f"User B ({user_b_username}) attempting to GET booking ID: {user_a_booking_id} (owned by User A)")
    response_get_booking_b = requests.get(f"{GATEWAY_URL}/api/bookings/{user_a_booking_id}", headers=user_b_headers)
    # Expect 403 Forbidden (Access Denied by BookingService logic) or 404 (if service hides existence)
    # Your BookingService throws AccessDeniedException which GlobalExceptionHandler maps to 403.
    print_response(response_get_booking_b, f"{action} - User B accessing User A's booking", expected_status=403)

def test_invalid_input_create_booking(user_headers, admin_headers): # Added admin_headers for potential setup
    action = "Test Invalid Input for Create Booking"
    print(f"\n>>> {action} <<<")
    
    # Need a valid showtime ID for some tests, even if other parts are invalid
    # Let's use a quickly setup one for this.
    valid_catalog_showtime_id, _, _, _ = setup_basic_show_environment(admin_headers, suffix_prefix="valid_st_for_invalid_input")
    if not valid_catalog_showtime_id:
        print(f"{action}: SKIPPED - Could not set up a valid showtime ID for testing invalid inputs.")
        return

    # Scenario: Number of tickets is zero
    booking_payload_zero_tickets = {"showtimeId": valid_catalog_showtime_id, "numberOfTickets": 0}
    response_zero = requests.post(f"{GATEWAY_URL}/api/bookings", headers=user_headers, json=booking_payload_zero_tickets)
    response_zero_data = print_response(response_zero, f"{action} - Zero Tickets", expected_status=400)
    if response_zero.status_code == 400 and response_zero_data:
        errors = response_zero_data.get("validationErrors", [])
        if any("tickets must be at least 1" in error.lower() or "numberoftickets" in error.lower() for error in errors):
            print("SUCCESS: Validation error related to tickets found as expected.")
        else:
            print(f"FAILURE: Expected validation error for tickets, but not found in errors list. Errors found: {errors}")

    # Scenario: Missing showtimeId
    booking_payload_no_showtime_id = {"numberOfTickets": 1}
    response_no_id = requests.post(f"{GATEWAY_URL}/api/bookings", headers=user_headers, json=booking_payload_no_showtime_id)
    response_no_id_data = print_response(response_no_id, f"{action} - Missing ShowtimeId", expected_status=400)
    if response_no_id.status_code == 400 and response_no_id_data:
        errors = response_no_id_data.get("validationErrors", [])
        if any("showtimeid" in error.lower() for error in errors):
            print("SUCCESS: Validation error for showtimeId found as expected.")
        else:
            print(f"FAILURE: Expected validation error for showtimeId, but not found in errors list. Errors found: {errors}")

    # Scenario: Invalid showtimeId format (e.g., string instead of number, though JSON handles this)
    # This might result in a different error (e.g., 400 due to type mismatch before validation, or 404 if it tries to parse and use it)
    booking_payload_bad_id_type = {"showtimeId": "not-a-number", "numberOfTickets": 1}
    response_bad_id_type = requests.post(f"{GATEWAY_URL}/api/bookings", headers=user_headers, json=booking_payload_bad_id_type)
    # The status code might be 400 due to deserialization error or a specific handler
    print_response(response_bad_id_type, f"{action} - Bad ShowtimeId Type", expected_status=400) 


# --- Main Execution ---
if __name__ == "__main__":
    print("Starting Booking Service Error Scenario Tests...")
    
    admin_token = login(ADMIN_USERNAME, ADMIN_PASSWORD, "Overall Admin")
    if not admin_token:
        print("FATAL: Admin login failed. Cannot run tests requiring admin setup.")
        exit()
    admin_headers = {"Authorization": f"Bearer {admin_token}", "Content-Type": "application/json"}

    # Register a common user for most tests
    main_test_user_suffix = get_unique_suffix()
    main_test_username, main_test_password = register_user(main_test_user_suffix, name_prefix="MainErrUser")
    
    main_user_token = None
    if main_test_username and main_test_password: # Ensure both are not None
        main_user_token = login(main_test_username, main_test_password, f"Main Error Test User ({main_test_username})")
    
    if not main_user_token:
        print("FATAL: Main test user login failed. Cannot run most booking error scenarios.")
        exit()
    main_user_headers = {"Authorization": f"Bearer {main_user_token}", "Content-Type": "application/json"}

    # --- Run Tests ---
    # Database should be cleared before this script for clean test runs for setup_basic_show_environment
    
    test_booking_non_existent_showtime(main_user_headers)
    
    # Test Insufficient Seats - This will now set up its own showtime with 1 seat
    test_booking_insufficient_seats(main_user_headers, admin_headers)
    
    # Test Accessing Another User's Booking - This sets up its own users and showtimes
    test_access_others_booking(admin_headers)
    
    # Test Invalid Input
    test_invalid_input_create_booking(main_user_headers, admin_headers)

    print("\n\nAll Booking Error Scenario Tests Attempted.")