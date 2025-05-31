import requests
import json
import time
from datetime import datetime, timedelta

# --- Configuration ---
API_GATEWAY_URL = "http://localhost:8080" # All requests go through this gateway

USER_SERVICE_AUTH_URL = f"{API_GATEWAY_URL}/api/auth"
ADMIN_MANAGEMENT_URL = f"{API_GATEWAY_URL}/api/admin/management"
SHOW_CATALOG_URL = f"{API_GATEWAY_URL}/api/shows"
BOOKING_SERVICE_URL = f"{API_GATEWAY_URL}/api/bookings"

# Credentials
admin_credentials = { "usernameOrEmail": "adminuser", "password": "adminpass" }
booking_user_register_creds = { 
    "name": "BookingFlowUser", 
    "username": "bookingflowuser", 
    "email": "bookingflow@example.com", 
    "password": "password789" 
}
booking_user_login_creds = { 
    "usernameOrEmail": "bookingflowuser", 
    "password": "password789" 
}

# Global state for tokens and created IDs
admin_auth_token = None
user_auth_token = None
created_venue_id_admin = None
created_show_id_admin = None
created_showtime_id_admin = None # This is the original ID from admin-service
local_catalog_showtime_id = None # This is the ID in show-catalog-service for the synced showtime
created_booking_id = None

# --- Helper Functions ---
def print_step(message):
    print(f"\n>>> {message}")

def print_req_res(description, response, show_json=True, is_creation=False):
    print(f"\n--- {description} ---")
    print(f"URL: {response.request.method} {response.request.url}")
    request_body_printed = False
    if hasattr(response.request, 'body') and response.request.body:
        try:
            body_content = response.request.body
            if isinstance(body_content, bytes): body_content = body_content.decode('utf-8')
            body_json = json.loads(body_content)
            print(f"Request Body: {json.dumps(body_json, indent=2)}")
            request_body_printed = True
        except (TypeError, json.JSONDecodeError, AttributeError):
            print(f"Request Body: {response.request.body}")
            request_body_printed = True
    if not request_body_printed:
        print("Request Body: None or not printed")
            
    print(f"Status Code: {response.status_code}")
    resource_id = None
    response_data = None
    try:
        response_data = response.json()
        if show_json: print(f"Response JSON: {json.dumps(response_data, indent=2)}")
        else: print(f"Response Text: {response.text if response.text else 'Empty'}")
        
        if is_creation and response.status_code // 100 == 2: # Check for 2xx status
            if 'id' in response_data: resource_id = response_data['id']
            elif 'bookingId' in response_data: resource_id = response_data['bookingId']
            if resource_id: print(f"CREATED/RETURNED RESOURCE ID: {resource_id}")

    except requests.exceptions.JSONDecodeError:
        if response.text: print(f"Response Text (not JSON): {response.text}")
        else: print("Response Body: Empty or not JSON")
    print("--------------------")
    return resource_id, response_data

# --- Test Functions ---

def admin_login_for_setup():
    global admin_auth_token
    print_step("SETUP PHASE: Admin Login")
    try:
        response = requests.post(f"{USER_SERVICE_AUTH_URL}/login", json=admin_credentials)
        _, data = print_req_res("Admin Login", response)
        if response.status_code == 200 and data and data.get("token"):
            admin_auth_token = data.get("token")
            print("RESULT: Admin token obtained for setup.")
            return True
    except requests.exceptions.RequestException as e:
        print(f"ERROR during admin login for setup: {e}")
    print("RESULT: Admin login FAILED during setup.")
    return False

def create_test_data_in_admin_service():
    global created_venue_id_admin, created_show_id_admin, created_showtime_id_admin
    if not admin_auth_token: return False
    
    headers = {"Authorization": f"Bearer {admin_auth_token}", "Content-Type": "application/json"}
    timestamp_suffix = int(time.time())

    # Create Venue
    print_step("SETUP PHASE: Creating Venue in Admin Service")
    venue_payload = {"name": f"Booking Test Venue {timestamp_suffix}", "address": "789 Booking Ave", "city": "Bookington", "capacity": 50}
    try:
        res_venue = requests.post(f"{ADMIN_MANAGEMENT_URL}/venues", json=venue_payload, headers=headers)
        created_venue_id_admin, _ = print_req_res("Create Venue (Admin)", res_venue, is_creation=True)
        if not created_venue_id_admin: return False
    except requests.exceptions.RequestException as e: print(f"ERROR creating venue: {e}"); return False

    # Create Show
    print_step("SETUP PHASE: Creating Show in Admin Service")
    show_payload = {"title": f"Booking Test Show {timestamp_suffix}", "description": "A show for booking tests.", "genre": "Testable", "language": "Py", "durationMinutes": 70, "releaseDate": (datetime.now() + timedelta(days=10)).strftime('%Y-%m-%d'), "posterUrl": "http://example.com/booking_show.jpg"}
    try:
        res_show = requests.post(f"{ADMIN_MANAGEMENT_URL}/shows", json=show_payload, headers=headers)
        created_show_id_admin, _ = print_req_res("Create Show (Admin)", res_show, is_creation=True)
        if not created_show_id_admin: return False
    except requests.exceptions.RequestException as e: print(f"ERROR creating show: {e}"); return False
        
    # Create Showtime
    print_step("SETUP PHASE: Creating Showtime in Admin Service")
    showtime_datetime = (datetime.now() + timedelta(days=15, hours=int(timestamp_suffix % 23))).strftime('%Y-%m-%dT%H:%M:%S') # Ensure unique time
    showtime_payload = {"showId": created_show_id_admin, "venueId": created_venue_id_admin, "showDateTime": showtime_datetime, "pricePerSeat": 15.00, "totalSeats": 30}
    try:
        res_showtime = requests.post(f"{ADMIN_MANAGEMENT_URL}/showtimes", json=showtime_payload, headers=headers)
        created_showtime_id_admin, _ = print_req_res("Create Showtime (Admin)", res_showtime, is_creation=True)
        if created_showtime_id_admin:
            print(f"RESULT: Test data (VenueID:{created_venue_id_admin}, ShowID:{created_show_id_admin}, ShowtimeID:{created_showtime_id_admin}) created in admin-service.")
            return True
        return False
    except requests.exceptions.RequestException as e: print(f"ERROR creating showtime: {e}"); return False

def find_local_catalog_showtime_id_from_sync(original_admin_showtime_id_to_find):
    global local_catalog_showtime_id
    if not original_admin_showtime_id_to_find: return False
    print_step(f"SYNC VERIFICATION: Looking for original showtime ID {original_admin_showtime_id_to_find} in Show Catalog")
    
    max_retries = 5
    retry_delay = 15 # seconds (adjust based on sync initialDelay + processing)
    
    for attempt in range(max_retries):
        print(f"  Attempt {attempt + 1} of {max_retries} to find synced showtime...")
        try:
            # We need an endpoint in show-catalog that returns showtimes with their originalShowtimeId
            # Let's assume /api/shows/{show_id}/showtimes in catalog returns Showtime entities which include originalShowtimeId
            # First, get all shows from catalog, then iterate. This is not efficient but for testing a small dataset.
            res_all_shows_catalog = requests.get(SHOW_CATALOG_URL) # Get all shows from catalog
            print_req_res(f"Sync Check: Get All Shows from Catalog (Attempt {attempt+1})", res_all_shows_catalog, show_json=False) # Less verbose

            if res_all_shows_catalog.status_code == 200:
                shows_in_catalog = res_all_shows_catalog.json()
                for show_cat in shows_in_catalog:
                    if 'id' not in show_cat: continue # Skip if no id
                    # Now get showtimes for this catalog show ID
                    res_showtimes_for_cat_show = requests.get(f"{SHOW_CATALOG_URL}/{show_cat['id']}/showtimes")
                    if res_showtimes_for_cat_show.status_code == 200:
                        for st_cat in res_showtimes_for_cat_show.json():
                            if st_cat.get('originalShowtimeId') == original_admin_showtime_id_to_find:
                                local_catalog_showtime_id = st_cat.get('id')
                                print(f"RESULT: SUCCESS - Found synced showtime. Local Catalog Showtime ID: {local_catalog_showtime_id} (Original Admin ID: {original_admin_showtime_id_to_find})")
                                return True
        except requests.exceptions.RequestException as e:
            print(f"  Error during sync check (Attempt {attempt + 1}): {e}")
        except json.JSONDecodeError as e:
            print(f"  Error decoding JSON during sync check (Attempt {attempt + 1}): {e}")

        if attempt < max_retries - 1:
            print(f"  Synced showtime not found yet. Waiting {retry_delay} seconds before retrying...")
            time.sleep(retry_delay)
            
    print(f"RESULT: FAILED - Could not find synced showtime with original admin ID {original_admin_showtime_id_to_find} in catalog after {max_retries} attempts.")
    return False

def register_and_login_booking_user():
    global user_token
    print_step("USER PHASE: Registering 'bookingtestuser'")
    try:
        reg_res = requests.post(f"{USER_SERVICE_AUTH_URL}/register", json=booking_user_register_creds)
        print_req_res("Register 'bookingtestuser'", reg_res)
    except requests.exceptions.RequestException as e: print(f"ERROR user registration: {e}")

    print_step("USER PHASE: Logging in as 'bookingtestuser'")
    try:
        login_res = requests.post(f"{USER_SERVICE_AUTH_URL}/login", json=booking_user_login_creds)
        _, data = print_req_res("Login 'bookingtestuser'", login_res)
        if login_res.status_code == 200 and data and data.get("token"):
            user_token = data.get("token")
            print("RESULT: 'bookingtestuser' token obtained.")
            return True
    except requests.exceptions.RequestException as e: print(f"ERROR user login: {e}")
    print("RESULT: 'bookingtestuser' login FAILED.")
    return False

def attempt_booking():
    global created_booking_id
    if not user_token or not local_catalog_showtime_id:
        print("Skipping booking creation: user token or local catalog showtime ID missing.")
        return False

    print_step(f"BOOKING PHASE: Create booking for local catalog showtime ID {local_catalog_showtime_id}")
    booking_payload = {"showtimeId": local_catalog_showtime_id, "numberOfTickets": 2}
    headers = {"Authorization": f"Bearer {user_token}", "Content-Type": "application/json"}
    try:
        response = requests.post(BOOKING_SERVICE_URL, json=booking_payload, headers=headers)
        created_booking_id, data = print_req_res("Create Booking", response, is_creation=True)
        if response.status_code == 201 and created_booking_id:
            print("RESULT: SUCCESS - Booking created.")
            # Further verify booking details from response if needed
            if data.get("originalShowtimeId") != created_showtime_id_admin:
                 print(f"  WARNING: originalShowtimeId in booking response ({data.get('originalShowtimeId')}) does not match admin created one ({created_showtime_id_admin}).")
            if data.get("numberOfTickets") != 2:
                 print(f"  WARNING: numberOfTickets mismatch! Expected 2, got {data.get('numberOfTickets')}")
            return True
        else:
            print(f"RESULT: FAILED - Expected 201 for Create Booking, Got {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"ERROR creating booking: {e}")
        return False

def get_my_bookings():
    if not user_token: return False
    print_step("BOOKING PHASE: Get My Bookings")
    headers = {"Authorization": f"Bearer {user_token}"}
    try:
        response = requests.get(f"{BOOKING_SERVICE_URL}/my-bookings", headers=headers)
        _, bookings_list = print_req_res("Get My Bookings", response)
        if response.status_code == 200 and isinstance(bookings_list, list):
            print(f"RESULT: SUCCESS - Fetched {len(bookings_list)} booking(s).")
            if created_booking_id: # Check if the previously created booking is in the list
                found = any(b.get('bookingId') == created_booking_id for b in bookings_list)
                if found: print(f"  Verified booking ID {created_booking_id} is present in My Bookings.")
                else: print(f"  WARNING: Created booking ID {created_booking_id} NOT found in My Bookings list.")
            return True
        else:
            print(f"RESULT: FAILED - Expected 200 for Get My Bookings, Got {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"ERROR fetching my bookings: {e}")
        return False
def trigger_catalog_sync():
    global admin_auth_token # Assuming admin_auth_token is set by admin_login_for_setup()
    print_step("SYNC TRIGGER: Manually triggering data sync in show-catalog-service")
    
    # The sync trigger endpoint might be secured (e.g., for admin use) or public for testing.
    # If it's secured (e.g., requires ROLE_ADMIN), you'll need to send the admin token.
    # If it's public, you don't need the headers.
    # Let's assume for now it might be admin-protected, so we use admin_token.
    # Adjust if the endpoint /api/catalog/sync/trigger is public.
    
    headers = {}
    if admin_auth_token: # Only add auth header if admin token was obtained
        headers["Authorization"] = f"Bearer {admin_auth_token}"
        headers["Content-Type"] = "application/json" # POST usually needs content type, though empty body here
    else:
        print("WARNING: Admin token not available for triggering sync. Endpoint might fail if secured.")

    try:
        # The endpoint is in show-catalog-service, accessed via Gateway
        # The path was defined as /api/catalog/sync/trigger in SyncController
        sync_trigger_url = f"{API_GATEWAY_URL}/api/catalog/sync/trigger"
        
        response = requests.post(sync_trigger_url, headers=headers) # Empty body for POST
        print_req_res("Trigger Catalog Sync", response, show_json=False) # Response might be plain text or empty
        
        if response.status_code == 200:
            print("RESULT: Catalog sync triggered successfully (or request accepted).")
            print("Waiting for sync to complete (e.g., 15-20 seconds)...")
            time.sleep(20) # Allow some time for the sync to process
            return True
        else:
            print(f"RESULT: FAILED to trigger catalog sync. Status: {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"ERROR triggering catalog sync: {e}")
        return False
# --- Main Test Execution ---
if __name__ == "__main__":
    print_step("Starting Full Booking Flow Test Script...")
    print("Ensure ALL services are running (Eureka, User, Admin, Show-Catalog, Booking, API-Gateway).")
    print("Allow time for ShowDataSyncService in show-catalog-service to perform initial sync.")
    time.sleep(10)  # Initial small wait

    if not admin_login_for_setup():
        print("\nHalting: Admin login for setup failed.")
        exit()

    if not create_test_data_in_admin_service():
        print("\nHalting: Failed to create test data in admin-service.")
        exit()

    # ✅ NEW STEP: Manually trigger the sync in show-catalog-service
    if not trigger_catalog_sync():
        print("\nHalting: Failed to trigger or complete catalog sync.")
        exit()

    if not find_local_catalog_showtime_id_from_sync(created_showtime_id_admin):
        print("\nHalting: Synced showtime could not be verified in show-catalog-service.")
        exit()

    if not register_and_login_booking_user():
        print("\nHalting: Test user registration or login failed.")
        exit()

    if attempt_booking():
        get_my_bookings()

    print("\n--- Full Booking Flow Test Script Finished ---")
