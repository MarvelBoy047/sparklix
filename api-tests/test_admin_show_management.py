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

# Data to be created - will be modified by responses
created_venue_id = None
created_show_id = None
created_showtime_id = None

def print_step(message):
    print(f"\n>>> {message}")

def print_req_res(description, response, show_json=True, is_creation=False):
    print(f"\n--- {description} ---")
    print(f"URL: {response.request.method} {response.request.url}")
    if response.request.body:
        try:
            body_content = response.request.body
            if isinstance(body_content, bytes):
                body_content = body_content.decode('utf-8')
            body_json = json.loads(body_content)
            print(f"Request Body: {json.dumps(body_json, indent=2)}")
        except (TypeError, json.JSONDecodeError, AttributeError):
            print(f"Request Body: {response.request.body}")
            
    print(f"Status Code: {response.status_code}")
    resource_id = None
    try:
        response_data = response.json()
        if show_json:
            print(f"Response JSON: {json.dumps(response_data, indent=2)}")
        if is_creation and 'id' in response_data:
            resource_id = response_data['id']
            print(f"CREATED RESOURCE ID: {resource_id}")
    except requests.exceptions.JSONDecodeError:
        if show_json: # Only print text if we expected JSON but didn't get it
             print(f"Response Text (not JSON): {response.text}")
        elif response.text: # For 204 No Content, text might be empty
             print(f"Response Text: {response.text}")
    print("--------------------")
    return resource_id


def admin_login():
    global admin_token
    print_step("STEP 0: Admin Login (via User-Service through Gateway)")
    try:
        response = requests.post(f"{USER_SERVICE_AUTH_URL}/login", json=admin_creds)
        if response.status_code == 200 and response.json().get("token"):
            admin_token = response.json().get("token")
            print_req_res("Admin Login", response)
            print("RESULT: Admin token obtained successfully.")
            return True
        else:
            print_req_res("Admin Login", response)
            print("RESULT: Admin login FAILED.")
            return False
    except requests.exceptions.RequestException as e:
        print(f"ERROR during admin login: {e}")
        return False

def create_venue():
    global created_venue_id
    if not admin_token:
        print("Skipping venue creation, admin token not available.")
        return False
    
    print_step("STEP 1: Create a New Venue")
    payload = {
        "name": f"Automated Test Venue {int(time.time())}", # Unique name
        "address": "101 Test Street",
        "city": "Testville",
        "capacity": 120
    }
    headers = {"Authorization": f"Bearer {admin_token}", "Content-Type": "application/json"}
    try:
        response = requests.post(f"{ADMIN_MANAGEMENT_URL}/venues", json=payload, headers=headers)
        created_venue_id = print_req_res("Create Venue", response, is_creation=True)
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
    if not admin_token:
        print("Skipping show creation, admin token not available.")
        return False

    print_step("STEP 2: Create a New Show")
    # Future date for releaseDate
    release_date = (datetime.now() + timedelta(days=30)).strftime('%Y-%m-%d')
    payload = {
        "title": f"Automated Test Show {int(time.time())}", # Unique title
        "description": "A show created by an automated test script.",
        "genre": "Automated Test",
        "language": "Pythonic",
        "durationMinutes": 95,
        "releaseDate": release_date,
        "posterUrl": "http://example.com/automated_poster.jpg"
    }
    headers = {"Authorization": f"Bearer {admin_token}", "Content-Type": "application/json"}
    try:
        response = requests.post(f"{ADMIN_MANAGEMENT_URL}/shows", json=payload, headers=headers)
        created_show_id = print_req_res("Create Show", response, is_creation=True)
        if response.status_code == 201 and created_show_id:
            print("RESULT: Show created successfully.")
            return True
        else:
            print("RESULT: Show creation FAILED.")
            return False
    except requests.exceptions.RequestException as e:
        print(f"ERROR during show creation: {e}")
        return False

def create_showtime(attempt_number=1):
    global created_showtime_id
    if not admin_token or not created_show_id or not created_venue_id:
        print("Skipping showtime creation, admin token, show ID, or venue ID not available.")
        return None # Return None to indicate it couldn't run

    print_step(f"STEP 3.{attempt_number}: Create a New Showtime")
    # Future date for showDateTime
    show_datetime = (datetime.now() + timedelta(days=60, hours=2)).strftime('%Y-%m-%dT%H:%M:%S')
    payload = {
        "showId": created_show_id,
        "venueId": created_venue_id,
        "showDateTime": show_datetime,
        "pricePerSeat": 20.50,
        "totalSeats": 75
    }
    headers = {"Authorization": f"Bearer {admin_token}", "Content-Type": "application/json"}
    try:
        response = requests.post(f"{ADMIN_MANAGEMENT_URL}/showtimes", json=payload, headers=headers)
        if attempt_number == 1: # Only try to get ID on first successful creation
            created_showtime_id = print_req_res("Create Showtime (Attempt 1)", response, is_creation=True)
            if response.status_code == 201 and created_showtime_id:
                print("RESULT: Showtime created successfully.")
        else: # For duplicate attempt
            print_req_res("Create Showtime (Attempt 2 - Duplicate)", response)
            if response.status_code == 409: # Expecting Conflict
                print("RESULT: CORRECT - Duplicate showtime creation resulted in 409 Conflict.")
            else:
                print(f"RESULT: UNEXPECTED - Expected 409, Got {response.status_code} for duplicate showtime.")
        return response # Return the full response object
    except requests.exceptions.RequestException as e:
        print(f"ERROR during showtime creation (Attempt {attempt_number}): {e}")
        return None

def cleanup_resources():
    if not admin_token:
        print("Skipping cleanup, admin token not available.")
        return

    headers = {"Authorization": f"Bearer {admin_token}"}
    
    # Important: Delete in reverse order of creation if there are dependencies,
    # or if showtimes depend on shows/venues.
    # For this test, assuming Showtime deletion is independent or handled by cascades if setup.

    if created_showtime_id:
        print_step(f"CLEANUP: Deleting Showtime ID: {created_showtime_id}")
        try:
            response = requests.delete(f"{ADMIN_MANAGEMENT_URL}/showtimes/{created_showtime_id}", headers=headers)
            print_req_res(f"Delete Showtime {created_showtime_id}", response, show_json=False)
        except requests.exceptions.RequestException as e:
            print(f"ERROR deleting showtime {created_showtime_id}: {e}")
    
    if created_show_id:
        print_step(f"CLEANUP: Deleting Show ID: {created_show_id}")
        try:
            response = requests.delete(f"{ADMIN_MANAGEMENT_URL}/shows/{created_show_id}", headers=headers)
            print_req_res(f"Delete Show {created_show_id}", response, show_json=False)
        except requests.exceptions.RequestException as e:
            print(f"ERROR deleting show {created_show_id}: {e}")

    if created_venue_id:
        print_step(f"CLEANUP: Deleting Venue ID: {created_venue_id}")
        try:
            response = requests.delete(f"{ADMIN_MANAGEMENT_URL}/venues/{created_venue_id}", headers=headers)
            print_req_res(f"Delete Venue {created_venue_id}", response, show_json=False)
        except requests.exceptions.RequestException as e:
            print(f"ERROR deleting venue {created_venue_id}: {e}")


# --- Test Execution ---
if __name__ == "__main__":
    print_step("ENSURE ALL SERVICES ARE RUNNING: Eureka, User-Service, Admin-Service, API-Gateway")
    print("Waiting 5 seconds for services to stabilize...")
    time.sleep(5)

    if admin_login():
        if create_venue(): # Only proceed if venue creation is successful
            if create_show(): # Only proceed if show creation is successful
                # Attempt 1: Create Showtime
                response_showtime1 = create_showtime(attempt_number=1)
                
                # Attempt 2: Try to create the same Showtime again (should conflict)
                if response_showtime1 and response_showtime1.status_code == 201: # Only try duplicate if first was success
                    create_showtime(attempt_number=2) 
                else:
                    print("\nSkipping duplicate showtime test as first creation failed or was not 201.")
        
        # Optional: Cleanup created resources
        # cleanup_resources() 
        # Be careful with cleanup if you want to inspect DB after script runs
    else:
        print("Admin login failed. Halting script.")

    print("\n--- Admin Show Management Test Script Finished ---")