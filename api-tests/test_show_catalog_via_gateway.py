import requests
import json
import time

# Base URLs
API_GATEWAY_URL = "http://localhost:8080"
SHOW_CATALOG_URL = f"{API_GATEWAY_URL}/api/shows" # Main endpoint to test

# User credentials (optional for these tests if /api/shows is public)
# If you later secure parts of the catalog or add review submission tests, you'll need this.
# USER_SERVICE_AUTH_URL = f"{API_GATEWAY_URL}/api/auth"
# user_creds_for_registration = { "name": "CatalogViewer", "username": "catalogviewer", "email": "catalogviewer@example.com", "password": "password123" }
# user_creds_for_login = { "usernameOrEmail": "catalogviewer", "password": "password123" }
# user_token = None

def print_step(message):
    print(f"\n>>> {message}")

def print_req_res(description, response, show_json=True):
    print(f"\n--- {description} ---")
    print(f"URL: {response.request.method} {response.request.url}")
    if hasattr(response.request, 'body') and response.request.body:
        try:
            body_content = response.request.body
            if isinstance(body_content, bytes):
                body_content = body_content.decode('utf-8')
            body_json = json.loads(body_content)
            print(f"Request Body: {json.dumps(body_json, indent=2)}")
        except (TypeError, json.JSONDecodeError, AttributeError):
            print(f"Request Body: {response.request.body}") 
            
    print(f"Status Code: {response.status_code}")
    try:
        if show_json:
            print(f"Response JSON: {json.dumps(response.json(), indent=2)}")
        else:
            print(f"Response Text: {response.text}")
    except requests.exceptions.JSONDecodeError:
        if response.text: 
            print(f"Response Text (not JSON): {response.text}")
        else:
            print("Response Body: Empty or not JSON")
    print("--------------------")

# --- Test Execution ---
if __name__ == "__main__":
    print_step("ENSURE ALL SERVICES ARE RUNNING: Eureka, User-Service, Admin-Service, Show-Catalog-Service, API-Gateway")
    print_step("AND ShowDataSyncService in show-catalog-service has run AT LEAST ONCE successfully after admin-service has data.")
    print("Waiting 10 seconds for services to stabilize and potential initial sync...")
    time.sleep(10) # Give a bit more time for initial sync if it's on a short delay
    
    # Test 1: Get All Shows from Show Catalog Service (via Gateway)
    print_step("STEP 1: Get All Shows (via Gateway from Show-Catalog-Service)")
    all_shows_data = [] 
    try:
        get_shows_response = requests.get(SHOW_CATALOG_URL)
        print_req_res("Get All Shows", get_shows_response)
        if get_shows_response.status_code == 200:
            all_shows_data = get_shows_response.json()
            if isinstance(all_shows_data, list) and len(all_shows_data) > 0:
                print(f"RESULT: SUCCESS - Received {len(all_shows_data)} show(s) from the catalog.")
                print("First show example:", json.dumps(all_shows_data[0], indent=2))
            elif isinstance(all_shows_data, list) and len(all_shows_data) == 0:
                print("RESULT: OK (No Data) - Received an empty list. Reasons: admin-service had no data, sync hasn't run, or sync failed to populate.")
            else:
                print("RESULT: UNEXPECTED - Response was 200 OK, but data format is not a list or is structured unexpectedly.")
        else:
            print(f"RESULT: FAILED - Expected 200 OK for Get All Shows, Got {get_shows_response.status_code}")

    except requests.exceptions.RequestException as e:
        print(f"ERROR during Get All Shows: {e}")
        print("RESULT: FAILED - Request to Get All Shows failed.")

    # Test 2: Get a Specific Show by ID (using an ID from the previous test if available)
    if all_shows_data and isinstance(all_shows_data, list) and len(all_shows_data) > 0 and 'id' in all_shows_data[0]:
        # IMPORTANT: 'id' here refers to the local ID in show_catalog_service's database
        show_id_to_test = all_shows_data[0]['id'] 
        print_step(f"STEP 2: Get Specific Show by local ID={show_id_to_test} (via Gateway from Show-Catalog-Service)")
        try:
            get_show_by_id_response = requests.get(f"{SHOW_CATALOG_URL}/{show_id_to_test}")
            print_req_res(f"Get Show by ID {show_id_to_test}", get_show_by_id_response)
            if get_show_by_id_response.status_code == 200:
                print(f"RESULT: SUCCESS - Successfully fetched show by local ID {show_id_to_test}.")
            else:
                print(f"RESULT: FAILED - Expected 200 for Get Show by ID, Got {get_show_by_id_response.status_code}")
        except requests.exceptions.RequestException as e:
            print(f"ERROR during Get Show by ID {show_id_to_test}: {e}")
            print(f"RESULT: FAILED - Request to Get Show by ID {show_id_to_test} failed.")
    else:
        print("\nSkipping STEP 2 (Get Specific Show by ID) because no shows were fetched in STEP 1 or first show had no 'id' field.")

    # Test 3: Search Shows by Title
    # You should have created a show with "Adventure" or similar in the title via admin-service
    search_title = "Adventure" 
    print_step(f"STEP 3: Search Shows by Title containing '{search_title}' (via Gateway)")
    try:
        search_response = requests.get(f"{SHOW_CATALOG_URL}/search", params={"title": search_title})
        print_req_res(f"Search Shows by Title '{search_title}'", search_response)
        if search_response.status_code == 200:
            searched_shows = search_response.json()
            if isinstance(searched_shows, list):
                print(f"RESULT: SUCCESS - Search returned {len(searched_shows)} show(s).")
                if len(searched_shows) > 0:
                    print("First search result example:", json.dumps(searched_shows[0], indent=2))
            else:
                print("RESULT: UNEXPECTED - Search response was 200 OK, but data format is not a list.")
        else:
            print(f"RESULT: FAILED - Expected 200 OK for Search Shows, Got {search_response.status_code}")
    except requests.exceptions.RequestException as e:
        print(f"ERROR during Search Shows by Title: {e}")
        print("RESULT: FAILED - Request to Search Shows failed.")

    # Test 4: Get Shows by Genre
    # You should have created shows with "Movie" as genre via admin-service
    search_genre = "Movie" 
    print_step(f"STEP 4: Get Shows by Genre '{search_genre}' (via Gateway)")
    try:
        genre_response = requests.get(f"{SHOW_CATALOG_URL}/genre/{search_genre}")
        print_req_res(f"Get Shows by Genre '{search_genre}'", genre_response)
        if genre_response.status_code == 200:
            genre_shows = genre_response.json()
            if isinstance(genre_shows, list):
                print(f"RESULT: SUCCESS - Get by Genre returned {len(genre_shows)} show(s).")
                if len(genre_shows) > 0:
                    print("First genre result example:", json.dumps(genre_shows[0], indent=2))
            else:
                print("RESULT: UNEXPECTED - Get by Genre response was 200 OK, but data format is not a list.")
        else:
            print(f"RESULT: FAILED - Expected 200 OK for Get by Genre, Got {genre_response.status_code}")
    except requests.exceptions.RequestException as e:
        print(f"ERROR during Get Shows by Genre: {e}")
        print("RESULT: FAILED - Request to Get by Genre failed.")

    print("\n--- Show Catalog Test Script Finished ---")