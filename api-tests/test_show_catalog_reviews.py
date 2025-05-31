import requests
import json
import time

# Base URLs
API_GATEWAY_URL = "http://localhost:8080"
USER_SERVICE_AUTH_URL = f"{API_GATEWAY_URL}/api/auth"
SHOW_CATALOG_URL = f"{API_GATEWAY_URL}/api/shows"

# Credentials
# Ensure pythontestuser is either in your data.sql for user-service OR will be registered by this script
user_creds_for_registration = { "name": "PythonReviewUser", "username": "pythonreviewuser", "email": "pythonreview@example.com", "password": "password123" }
user_creds_for_login = { "usernameOrEmail": "pythonreviewuser", "password": "password123" }

admin_creds = { "usernameOrEmail": "adminuser", "password": "adminpass" } # Assuming adminuser exists

user_token = None
admin_token = None
show_id_for_review = None # Will be populated after fetching shows

def print_step(message):
    print(f"\n>>> {message}")

def print_req_res(description, response, show_json=True):
    print(f"\n--- {description} ---")
    print(f"URL: {response.request.method} {response.request.url}")
    if hasattr(response.request, 'body') and response.request.body: # Check if body exists
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
    print_step("AND ShowDataSyncService in show-catalog-service has run at least once (or admin-service has data).")
    print("Waiting 5 seconds for services to stabilize...")
    time.sleep(5) 

    # 1. Register 'pythonreviewuser' (idempotent)
    print_step("STEP 1: Register 'pythonreviewuser' (via Gateway to User-Service)")
    try:
        reg_response = requests.post(f"{USER_SERVICE_AUTH_URL}/register", json=user_creds_for_registration)
        print_req_res("Register 'pythonreviewuser'", reg_response)
    except requests.exceptions.RequestException as e:
        print(f"ERROR during pythonreviewuser registration: {e}")

    # 2. Login as 'pythonreviewuser' to get ROLE_USER token
    print_step("STEP 2: Login as 'pythonreviewuser'")
    try:
        login_res_user = requests.post(f"{USER_SERVICE_AUTH_URL}/login", json=user_creds_for_login)
        print_req_res("Login 'pythonreviewuser'", login_res_user)
        if login_res_user.status_code == 200 and login_res_user.json().get("token"):
            user_token = login_res_user.json().get("token")
            print("RESULT: 'pythonreviewuser' token obtained successfully.")
        else:
            print("RESULT: 'pythonreviewuser' login FAILED.")
    except requests.exceptions.RequestException as e:
        print(f"ERROR during 'pythonreviewuser' login: {e}")
        print("RESULT: 'pythonreviewuser' login FAILED due to request exception.")

    # 3. (Optional) Login as 'adminuser' to get ROLE_ADMIN token (for testing unauthorized review post)
    print_step("STEP 3: Login as 'adminuser'")
    try:
        login_res_admin = requests.post(f"{USER_SERVICE_AUTH_URL}/login", json=admin_creds)
        print_req_res("Login 'adminuser'", login_res_admin)
        if login_res_admin.status_code == 200 and login_res_admin.json().get("token"):
            admin_token = login_res_admin.json().get("token")
            print("RESULT: 'adminuser' token obtained successfully.")
        else:
            print("RESULT: 'adminuser' login FAILED (Ensure adminuser/adminpass is correct in user-service DB).")
    except requests.exceptions.RequestException as e:
        print(f"ERROR during 'adminuser' login: {e}")
        print("RESULT: 'adminuser' login FAILED due to request exception.")


    # 4. Get all shows to find a show ID to review
    print_step("STEP 4: Get All Shows to find a target show for review")
    try:
        get_shows_response = requests.get(SHOW_CATALOG_URL)
        print_req_res("Get All Shows", get_shows_response)
        if get_shows_response.status_code == 200:
            shows_data = get_shows_response.json()
            if isinstance(shows_data, list) and len(shows_data) > 0:
                show_id_for_review = shows_data[0]['id'] # Use the ID of the first show found
                print(f"RESULT: Will use showId '{show_id_for_review}' for posting reviews.")
            else:
                print("RESULT: No shows found in catalog. Cannot proceed with review tests meaningfully. Ensure data sync from admin-service worked or admin-service has shows.")
        else:
            print(f"RESULT: FAILED to get shows. Status: {get_shows_response.status_code}")
    except requests.exceptions.RequestException as e:
        print(f"ERROR during Get All Shows: {e}")

    if not show_id_for_review:
        print("\nHalting review tests as no show_id was obtained.")
    else:
        review_endpoint_url = f"{SHOW_CATALOG_URL}/{show_id_for_review}/reviews"

        # 5. Attempt to post a review WITHOUT token
        print_step(f"STEP 5: Attempt to POST review to {review_endpoint_url} WITHOUT token")
        review_payload_valid = {"rating": 5, "comment": "This is a fantastic show, loved it a lot!"}
        try:
            res_no_token = requests.post(review_endpoint_url, json=review_payload_valid)
            print_req_res("Post Review (No Token)", res_no_token)
            if res_no_token.status_code == 401:
                print("RESULT: CORRECT - Got 401 Unauthorized as expected.")
            else:
                print(f"RESULT: UNEXPECTED - Expected 401, Got {res_no_token.status_code}")
        except requests.exceptions.RequestException as e:
            print(f"ERROR posting review (no token): {e}")

        # 6. Attempt to post a review WITH ROLE_USER token (pythontestuser) - SUCCESS CASE
        if user_token:
            print_step(f"STEP 6: Attempt to POST review to {review_endpoint_url} WITH USER token")
            headers_user = {"Authorization": f"Bearer {user_token}", "Content-Type": "application/json"}
            try:
                res_user_token = requests.post(review_endpoint_url, json=review_payload_valid, headers=headers_user)
                print_req_res("Post Review (User Token)", res_user_token)
                if res_user_token.status_code == 201: # 201 Created
                    print("RESULT: CORRECT - Review posted successfully by user.")
                else:
                    print(f"RESULT: UNEXPECTED - Expected 201, Got {res_user_token.status_code}")
            except requests.exceptions.RequestException as e:
                print(f"ERROR posting review (user token): {e}")
        else:
            print("Skipping STEP 6 (Post Review with User Token) as user login failed.")
            
        # 7. Attempt to post a review WITH ROLE_ADMIN token (should fail if @PreAuthorize is specific to USER/VENDOR)
        if admin_token:
            print_step(f"STEP 7: Attempt to POST review to {review_endpoint_url} WITH ADMIN token")
            headers_admin = {"Authorization": f"Bearer {admin_token}", "Content-Type": "application/json"}
            try:
                res_admin_token = requests.post(review_endpoint_url, json=review_payload_valid, headers=headers_admin)
                print_req_res("Post Review (Admin Token)", res_admin_token)
                # Depending on your @PreAuthorize("hasAnyRole('USER', 'VENDOR')") this should be 403
                if res_admin_token.status_code == 403: 
                    print("RESULT: CORRECT - Admin correctly forbidden (403) from posting review.")
                elif res_admin_token.status_code == 201:
                    print("RESULT: OK (Admin also has USER/VENDOR role or @PreAuthorize allows ADMIN) - Review posted by admin.")
                else:
                    print(f"RESULT: UNEXPECTED - Expected 403 (or 201 if admin allowed), Got {res_admin_token.status_code}")
            except requests.exceptions.RequestException as e:
                print(f"ERROR posting review (admin token): {e}")
        else:
            print("Skipping STEP 7 (Post Review with Admin Token) as admin login failed.")

        # 8. Attempt to post a review with INVALID DATA (e.g., rating too high) using USER token
        if user_token:
            print_step(f"STEP 8: Attempt to POST review to {review_endpoint_url} with INVALID DATA (User Token)")
            review_payload_invalid = {"rating": 10, "comment": "Too short"} # Rating > 5, comment might be too short
            headers_user = {"Authorization": f"Bearer {user_token}", "Content-Type": "application/json"}
            try:
                res_invalid_data = requests.post(review_endpoint_url, json=review_payload_invalid, headers=headers_user)
                print_req_res("Post Review (Invalid Data)", res_invalid_data)
                if res_invalid_data.status_code == 400: # Bad Request due to validation
                    print("RESULT: CORRECT - Got 400 Bad Request for invalid review data.")
                else:
                    print(f"RESULT: UNEXPECTED - Expected 400, Got {res_invalid_data.status_code}")
            except requests.exceptions.RequestException as e:
                print(f"ERROR posting review (invalid data): {e}")
        else:
            print("Skipping STEP 8 (Post Review with Invalid Data) as user login failed.")

        # 9. Get all reviews for the show
        print_step(f"STEP 9: GET reviews for {review_endpoint_url}")
        try:
            res_get_reviews = requests.get(review_endpoint_url)
            print_req_res("Get Reviews for Show", res_get_reviews)
            if res_get_reviews.status_code == 200:
                print("RESULT: CORRECT - Successfully fetched reviews.")
            else:
                print(f"RESULT: UNEXPECTED - Expected 200, Got {res_get_reviews.status_code}")
        except requests.exceptions.RequestException as e:
            print(f"ERROR getting reviews: {e}")

    print("\n--- Show Catalog Review Test Script Finished ---")