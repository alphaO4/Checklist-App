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
    
    # Get all templates
    templates_response = requests.get(
        f'{base_url}/checklists?page=1&per_page=100',
        headers={'Authorization': f'Bearer {token}'}
    )
    
    if templates_response.status_code == 200:
        checklists = templates_response.json()['items']
        templates = [c for c in checklists if c.get('template', False)]
        
        print(f'Found {len(templates)} templates:')
        for template in templates:
            name = template['name']
            template_id = template['id']
            print(f'  ID: {template_id}, Name: "{name}"')
            
            # Mark test templates for deletion
            if ('test' in name.lower() or 
                'enhanced' in name.lower() or 
                name.startswith('Test Template') or 
                name.startswith('Enhanced Test')):
                print(f'    -> MARKED FOR DELETION (test template)')
    else:
        print(f'Failed to get templates: {templates_response.status_code}')
else:
    print(f'Failed to login: {login_response.status_code}')