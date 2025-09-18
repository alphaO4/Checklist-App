import requests
import json

base_url = 'http://10.20.1.108:8000'

# Login as admin to get access token
login_response = requests.post(
    f'{base_url}/auth/login',
    json={'username': 'admin', 'password': 'admin'},
    headers={'Content-Type': 'application/json'}
)

if login_response.status_code == 200:
    token = login_response.json()['access_token']
    
    # Remaining test template IDs to delete
    remaining_test_ids = [9, 11, 15, 17, 19, 21, 23, 25]
    
    print(f'Attempting to delete {len(remaining_test_ids)} remaining test templates...')
    
    for template_id in remaining_test_ids:
        try:
            # First, let's try to get details about the template
            get_response = requests.get(
                f'{base_url}/checklists/{template_id}',
                headers={'Authorization': f'Bearer {token}'}
            )
            
            if get_response.status_code == 200:
                template_info = get_response.json()
                print(f'Template ID {template_id}: "{template_info["name"]}"')
                
                # Try to delete
                delete_response = requests.delete(
                    f'{base_url}/checklists/{template_id}',
                    headers={'Authorization': f'Bearer {token}'}
                )
                
                if delete_response.status_code in [200, 204]:
                    print(f'  ✓ Successfully deleted template ID {template_id}')
                else:
                    print(f'  ✗ Failed to delete template ID {template_id}: {delete_response.status_code}')
                    if delete_response.text:
                        print(f'    Error details: {delete_response.text}')
            else:
                print(f'  ✗ Could not get info for template ID {template_id}: {get_response.status_code}')
                
        except Exception as e:
            print(f'  ✗ Error processing template ID {template_id}: {str(e)}')
    
else:
    print(f'Failed to login: {login_response.status_code}')