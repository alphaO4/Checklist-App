#!/usr/bin/env python3
"""
Test backend templates API to verify the data
"""

import requests
import json

def test_templates_api():
    try:
        # Login first
        login_response = requests.post('http://10.20.1.108:8000/auth/login', 
            json={'username': 'admin', 'password': 'admin'})
        
        if login_response.status_code == 200:
            token = login_response.json()['access_token']
            headers = {'Authorization': f'Bearer {token}'}
            
            # Get templates
            templates_response = requests.get('http://10.20.1.108:8000/checklists?template=true', 
                headers=headers)
            
            if templates_response.status_code == 200:
                data = templates_response.json()
                print(f'✅ Found {len(data["items"])} templates:')
                for template in data['items']:
                    print(f'  ID: {template["id"]}, Name: "{template["name"]}", Template: {template["template"]}')
                return data['items']
            else:
                print(f'❌ Templates request failed: {templates_response.status_code}')
                return []
        else:
            print(f'❌ Login failed: {login_response.status_code}')
            return []
            
    except Exception as e:
        print(f'❌ Error: {e}')
        return []

if __name__ == "__main__":
    test_templates_api()