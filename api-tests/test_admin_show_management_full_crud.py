import requests
import json
import time
from datetime import datetime, timedelta

# Base URLs
API_GATEWAY_URL = "http://localhost:8080"
USER_SERVICE_AUTH_URL = f"{API_GATEWAY_URL}/api/auth"
ADMIN_MANAGEMENT_URL = f"{API_GATEWAY_URL}/api/admin/management"

# Credentials
admin_creds = { "usernameOrEmail": "adminuser", "password": "adminpass" }

admin_token = None

# To store IDs of created resources for subsequent steps and cleanup
created_venue_id = None
created_show_id = None
created_showtime_id = None

def print_step(message):
    print(f"\n>>> {message}")

def print_req_res(description, response, show_json=True, is_creation=False):
    print(f"\n--- {description} ---")
    print(f"URL: {response.request.method} {response.request.url}")
    if hasattr(response.request, 'body') and response.request.body:
        try:
            body_content = response.request.body
            if isinstance(body_content, bytes):
                body_content = body_content.decode('utf-8')
            body_json = json.loads(body_content) # Try to parse as JSON first
            print(f"Request Body: {json.dumps(body_json, indent=2)}")
        except (TypeError, json.JSONDecodeError, AttributeError):
            # If not JSON or body is None, print as is
            print(f"Request Body: {response.request.body}")
            
    print(f"Status Code: {response.status_code}")
    resource_id = None
    response_data = None
    try:
        response_data = response.json()
        if show_json:
            print(f"Response JSON: {json.dumps(response_data, indent=2)}")
        if is_creation and 'id' in response_data:
            resource_id = response_data['id']
            print(f"CREATED/RETURNED RESOURCE ID: {resource_id}")
    except requests.exceptions.JSONDecodeError:
        if show_json and response.text:
             print(f"Response Text (not JSON): {response.text}")
        elif response.text:
             print(f"Response Text: {response.text}")
        else:
            print("Response Body: Empty")
    print("--------------------")
    return resource_id, response_data


def admin_login():
    global admin_token
    print_step("STEP 0: Admin Login")
    try:
        response = requests.post(f"{USER_SERVICE_AUTH_URL}/login", json=admin_creds)
        _, response_data = print_req_res("Admin Login", response)
        if response.status_code == 200 and response_data and response_data.get("token"):
            admin_token = response_data.get("token")
            print("RESULT: Admin token obtained successfully.")
            return True
        else:
            print("RESULT: Admin login FAILED.")
            return False
    except requests.exceptions.RequestException as e:
        print(f"ERROR during admin login: {e}")
        return False

def create_venue():
    global created_venue_id
    if not admin_token: return False
    print_step("STEP 1: Create a New Venue")
    payload = {
        "name": f"Automated Venue {int(time.time())}",
        "address": "101 Test Street", "city": "Testville", "capacity": 120
    }
    headers = {"Authorization": f"Bearer {admin_token}", "Content-Type": "application/json"}
    try:
        response = requests.post(f"{ADMIN_MANAGEMENT_URL}/venues", json=payload, headers=headers)
        created_venue_id, _ = print_req_res("Create Venue", response, is_creation=True)
        if response.status_code == 201 and created_venue_id:
            print("RESULT: Venue created successfully.")
            return True
        else:
            print("RESULT: Venue creation FAILED.")
            return False
    except requests.exceptions.RequestException as e:
        print(f"ERROR during venue creation: {e}")
        return False

def create_show():
    global created_show_id
    if not admin_token: return False
    print_step("STEP 2: Create a New Show")
    release_date = (datetime.now() + timedelta(days=30)).strftime('%Y-%m-%d')
    payload = {
        "title": f"Automated Show {int(time.time())}", "description": "Test show.",
        "genre": "Test", "language": "TestLang", "durationMinutes": 90,
        "releaseDate": release_date, "posterUrl": "http://example.com/poster.jpg"
    }
    headers = {"Authorization": f"Bearer {admin_token}", "Content-Type": "application/json"}
    try:
        response = requests.post(f"{ADMIN_MANAGEMENT_URL}/shows", json=payload, headers=headers)
        created_show_id, _ = print_req_res("Create Show", response, is_creation=True)
        if response.status_code == 201 and created_show_id:
            print("RESULT: Show created successfully.")
            return True
        else:
            print("RESULT: Show creation FAILED.")
            return False
    except requests.exceptions.RequestException as e:
        print(f"ERROR during show creation: {e}")
        return False

def create_showtime():
    global created_showtime_id
    if not admin_token or not created_show_id or not created_venue_id: return False
    print_step("STEP 3: Create a New Showtime")
    show_datetime = (datetime.now() + timedelta(days=60, hours=3)).strftime('%Y-%m-%dT%H:%M:%S')
    payload = {
        "showId": created_show_id, "venueId": created_venue_id,
        "showDateTime": show_datetime, "pricePerSeat": 25.50, "totalSeats": 50
    }
    headers = {"Authorization": f"Bearer {admin_token}", "Content-Type": "application/json"}
    try:
        response = requests.post(f"{ADMIN_MANAGEMENT_URL}/showtimes", json=payload, headers=headers)
        created_showtime_id, _ = print_req_res("Create Showtime", response, is_creation=True)
        if response.status_code == 201 and created_showtime_id:
            print("RESULT: Showtime created successfully.")
            return True
        else:
            print(f"RESULT: Showtime creation FAILED. Status: {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"ERROR during showtime creation: {e}")
        return False

def update_showtime():
    if not admin_token or not created_showtime_id or not created_show_id or not created_venue_id: return False
    print_step(f"STEP 4: Update Showtime ID: {created_showtime_id}")
    # Example: Change price and total seats. Keep showId, venueId, showDateTime the same for this test.
    # For a more complex test, you could try changing venueId or showId too.
    show_datetime_original_obj = datetime.strptime(ShowtimeInitialData['showDateTime'], '%Y-%m-%dT%H:%M:%S')
    
    updated_payload = {
        "showId": created_show_id, 
        "venueId": created_venue_id,
        "showDateTime": show_datetime_original_obj.strftime('%Y-%m-%dT%H:%M:%S'), # Keep original datetime
        "pricePerSeat": 22.75, # Updated price
        "totalSeats": 60       # Updated seats
    }
    headers = {"Authorization": f"Bearer {admin_token}", "Content-Type": "application/json"}
    try:
        response = requests.put(f"{ADMIN_MANAGEMENT_URL}/showtimes/{created_showtime_id}", json=updated_payload, headers=headers)
        _, response_data = print_req_res(f"Update Showtime {created_showtime_id}", response)
        if response.status_code == 200 and response_data:
            if response_data.get('pricePerSeat') == 22.75 and response_data.get('totalSeats') == 60:
                print("RESULT: Showtime updated successfully and changes verified.")
                return True
            else:
                print("RESULT: Showtime update responded 200 OK, but changes not reflected in response.")
                return False
        else:
            print(f"RESULT: Showtime update FAILED. Status: {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"ERROR during showtime update: {e}")
        return False

def get_showtime_by_id(showtime_id, expect_found=True):
    if not admin_token: return False
    print_step(f"STEP 5.{'1' if expect_found else '3'}: Get Showtime by ID: {showtime_id}")
    headers = {"Authorization": f"Bearer {admin_token}"}
    try:
        response = requests.get(f"{ADMIN_MANAGEMENT_URL}/showtimes/{showtime_id}", headers=headers)
        print_req_res(f"Get Showtime {showtime_id}", response)
        if expect_found:
            if response.status_code == 200:
                print(f"RESULT: Showtime {showtime_id} fetched successfully.")
                return True
            else:
                print(f"RESULT: FAILED to fetch Showtime {showtime_id}. Expected 200, Got {response.status_code}")
                return False
        else: # Expecting Not Found (404)
            if response.status_code == 404:
                print(f"RESULT: CORRECT - Showtime {showtime_id} not found as expected after delete.")
                return True
            else:
                print(f"RESULT: UNEXPECTED - Expected 404 for deleted Showtime {showtime_id}, Got {response.status_code}")
                return False
    except requests.exceptions.RequestException as e:
        print(f"ERROR getting showtime {showtime_id}: {e}")
        return False

def delete_showtime():
    if not admin_token or not created_showtime_id: return False
    print_step(f"STEP 5.2: Delete Showtime ID: {created_showtime_id}")
    headers = {"Authorization": f"Bearer {admin_token}"}
    try:
        response = requests.delete(f"{ADMIN_MANAGEMENT_URL}/showtimes/{created_showtime_id}", headers=headers)
        print_req_res(f"Delete Showtime {created_showtime_id}", response, show_json=False) # 204 No Content
        if response.status_code == 204:
            print("RESULT: Showtime deleted successfully.")
            return True
        else:
            print(f"RESULT: Showtime deletion FAILED. Status: {response.status_code}")
            return False
    except requests.exceptions.RequestException as e:
        print(f"ERROR deleting showtime {created_showtime_id}: {e}")
        return False

def cleanup_resources(): # Optional cleanup for show and venue
    if not admin_token: return
    headers = {"Authorization": f"Bearer {admin_token}"}
    if created_show_id:
        print_step(f"CLEANUP: Deleting Show ID: {created_show_id}")
        requests.delete(f"{ADMIN_MANAGEMENT_URL}/shows/{created_show_id}", headers=headers)
    if created_venue_id:
        print_step(f"CLEANUP: Deleting Venue ID: {created_venue_id}")
        requests.delete(f"{ADMIN_MANAGEMENT_URL}/venues/{created_venue_id}", headers=headers)

# Store initial showtime data to compare after update
ShowtimeInitialData = {}

# --- Test Execution ---
if __name__ == "__main__":
    print_step("ENSURE ALL SERVICES ARE RUNNING: Eureka, User-Service, Admin-Service, API-Gateway")
    print("Waiting 5 seconds for services to stabilize...")
    time.sleep(5)

    if admin_login():
        if create_venue():
            if create_show():
                if create_showtime():
                    # Store data of created showtime for update test verification
                    # This assumes create_showtime() was successful and returned the created showtime object
                    # For simplicity, we'll re-fetch it, though create_showtime could return it.
                    temp_response = requests.get(f"{ADMIN_MANAGEMENT_URL}/showtimes/{created_showtime_id}", headers={"Authorization": f"Bearer {admin_token}"})
                    if temp_response.status_code == 200:
                        ShowtimeInitialData = temp_response.json()
                    else:
                        print("Could not fetch initial showtime data for update comparison. Update test might be less verifiable.")
                        ShowtimeInitialData['showDateTime'] = (datetime.now() + timedelta(days=60, hours=3)).strftime('%Y-%m-%dT%H:%M:%S') # Fallback for test to run


                    if update_showtime(): # This uses ShowtimeInitialData
                        get_showtime_by_id(created_showtime_id, expect_found=True) # Verify update persisted
                    
                    if delete_showtime():
                        get_showtime_by_id(created_showtime_id, expect_found=False) # Verify delete
                
        # Optional: Uncomment to cleanup Venue and Show after tests
        # print_step("Performing optional cleanup of Show and Venue...")
        # cleanup_resources()
    else:
        print("Admin login failed. Halting script.")

    print("\n--- Admin Show Management Full CRUD Test Script Finished ---")