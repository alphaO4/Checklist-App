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
    
    # Test template IDs to delete (from our analysis above)
    test_template_ids = list(range(7, 27))  # IDs 7-26
    
    print(f'Deleting {len(test_template_ids)} test templates...')
    
    deleted_count = 0
    failed_count = 0
    
    for template_id in test_template_ids:
        try:
            delete_response = requests.delete(
                f'{base_url}/checklists/{template_id}',
                headers={'Authorization': f'Bearer {token}'}
            )
            
            if delete_response.status_code in [200, 204]:
                print(f'  ✓ Deleted template ID {template_id}')
                deleted_count += 1
            else:
                print(f'  ✗ Failed to delete template ID {template_id}: {delete_response.status_code}')
                failed_count += 1
                
        except Exception as e:
            print(f'  ✗ Error deleting template ID {template_id}: {str(e)}')
            failed_count += 1
    
    print(f'\nSummary:')
    print(f'  Successfully deleted: {deleted_count} templates')
    print(f'  Failed to delete: {failed_count} templates')
    
else:
    print(f'Failed to login: {login_response.status_code}')