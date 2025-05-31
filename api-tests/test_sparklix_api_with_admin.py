import requests
import json
import time

# Base URLs
API_GATEWAY_URL = "http://localhost:8080" # All requests go through gateway

# Credentials
admin_creds = { "usernameOrEmail": "adminuser", "password": "adminpass" }
# Ensure 'pythontestuser' is either in your data.sql for user-service or will be successfully registered by this script
user_creds_for_registration = { "name": "Python Test User", "username": "pythontestuser", "email": "pythontest@example.com", "password": "password123" }
user_creds_for_login = { "usernameOrEmail": "pythontestuser", "password": "password123" }

admin_token = None
user_token = None

def print_step(message):
    print(f"\n>>> {message}")

def print_req_res(description, response, show_json=True):
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
            # AttributeError for cases where response.request.body might be None or not what's expected
            print(f"Request Body: {response.request.body}") 
            
    print(f"Status Code: {response.status_code}")
    try:
        if show_json:
            print(f"Response JSON: {json.dumps(response.json(), indent=2)}")
        else:
            print(f"Response Text: {response.text}")
    except requests.exceptions.JSONDecodeError:
        print(f"Response Text (not JSON): {response.text}")
    print("--------------------")

if __name__ == "__main__":
    print_step("ENSURE ALL SERVICES ARE RUNNING: Eureka, User-Service, Admin-Service, API-Gateway")
    print("Waiting 5 seconds for services to stabilize if just started...")
    time.sleep(5)

    # 1. Register pythontestuser (idempotent, might return 400 if exists)
    print_step("STEP 1: Register 'pythontestuser' (via Gateway to User-Service)")
    try:
        reg_response = requests.post(f"{API_GATEWAY_URL}/api/auth/register", json=user_creds_for_registration)
        print_req_res("Register 'pythontestuser'", reg_response)
    except requests.exceptions.RequestException as e:
        print(f"ERROR during pythontestuser registration: {e}")

    # 2. Login as adminuser (via Gateway to User-Service)
    print_step("STEP 2: Login as 'adminuser' (via Gateway to User-Service)")
    try:
        login_response_admin = requests.post(f"{API_GATEWAY_URL}/api/auth/login", json=admin_creds)
        print_req_res("Login 'adminuser'", login_response_admin)
        if login_response_admin.status_code == 200 and login_response_admin.json().get("token"):
            admin_token = login_response_admin.json().get("token")
            print(f"RESULT: Admin token obtained successfully.")
        else:
            print("RESULT: Admin login FAILED.")
    except requests.exceptions.RequestException as e:
        print(f"ERROR during admin login: {e}")
        print("RESULT: Admin login FAILED due to request exception.")


    # 3. Login as pythontestuser (via Gateway to User-Service)
    print_step("STEP 3: Login as 'pythontestuser' (via Gateway to User-Service)")
    try:
        login_response_user = requests.post(f"{API_GATEWAY_URL}/api/auth/login", json=user_creds_for_login)
        print_req_res("Login 'pythontestuser'", login_response_user)
        if login_response_user.status_code == 200 and login_response_user.json().get("token"):
            user_token = login_response_user.json().get("token")
            print(f"RESULT: User token obtained successfully.")
        else:
            print("RESULT: Pythontestuser login FAILED.")
    except requests.exceptions.RequestException as e:
        print(f"ERROR during pythontestuser login: {e}")
        print("RESULT: Pythontestuser login FAILED due to request exception.")

    # 4. Access admin-service endpoint WITHOUT token (via Gateway)
    print_step("STEP 4: Access admin-service '/api/admin/hello' WITHOUT token (via Gateway)")
    try:
        no_token_res = requests.get(f"{API_GATEWAY_URL}/api/admin/hello")
        print_req_res("Access Admin (No Token)", no_token_res, show_json=True) # Show JSON for 401 error
        if no_token_res.status_code == 401:
             print("RESULT: CORRECT - Got 401 Unauthorized as expected.")
        else:
             print(f"RESULT: UNEXPECTED - Expected 401, Got {no_token_res.status_code}")
    except requests.exceptions.RequestException as e:
        print(f"ERROR accessing admin (no token): {e}")


    # 5. Access admin-service endpoint WITH ADMIN token (via Gateway)
    if admin_token:
        print_step("STEP 5: Access admin-service '/api/admin/hello' WITH ADMIN token (via Gateway)")
        headers_admin = {"Authorization": f"Bearer {admin_token}"}
        try:
            admin_access_res = requests.get(f"{API_GATEWAY_URL}/api/admin/hello", headers=headers_admin)
            print_req_res("Access Admin (Admin Token)", admin_access_res, show_json=False) # Expecting plain text
            if admin_access_res.status_code == 200:
                 print("RESULT: CORRECT - Admin accessed successfully (200 OK).")
            else:
                 print(f"RESULT: UNEXPECTED - Expected 200, Got {admin_access_res.status_code}")
        except requests.exceptions.RequestException as e:
            print(f"ERROR accessing admin (admin token): {e}")
    else:
        print("\nSkipping STEP 5 (Access Admin with Admin Token) because admin login failed or token not obtained.")


    # 6. Access admin-service endpoint WITH USER token (via Gateway)
    if user_token:
        print_step("STEP 6: Access admin-service '/api/admin/hello' WITH USER token (via Gateway)")
        headers_user = {"Authorization": f"Bearer {user_token}"}
        try:
            user_access_admin_res = requests.get(f"{API_GATEWAY_URL}/api/admin/hello", headers=headers_user)
            print_req_res("Access Admin (User Token)", user_access_admin_res, show_json=True) # Expecting JSON error
            if user_access_admin_res.status_code == 403:
                 print("RESULT: CORRECT - User correctly forbidden (403).")
            elif user_access_admin_res.status_code == 401 : # If token was somehow seen as invalid by admin-service
                 print("RESULT: UNEXPECTED - User got 401 (Unauthorized), token validation might be failing in admin-service.")
            else:
                 print(f"RESULT: UNEXPECTED - Expected 403, Got {user_access_admin_res.status_code}")
        except requests.exceptions.RequestException as e:
            print(f"ERROR accessing admin (user token): {e}")
    else:
        print("\nSkipping STEP 6 (Access Admin with User Token) because user login failed or token not obtained.")
        
    print("\n--- Test Script Finished ---")