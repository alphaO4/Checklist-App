#!/usr/bin/env python3

import requests
import json

def test_backend_connectivity():
    """Test backend connectivity for Android app integration"""
    base_url = 'http://10.20.1.108:8000'
    
    try:
        print("=== Testing Backend Connectivity ===")
        
        # Test login
        login_response = requests.post(f'{base_url}/auth/login', 
            json={'username': 'admin', 'password': 'admin'})
        
        if login_response.status_code == 200:
            token = login_response.json()['access_token']
            print(f'✅ Login successful - Token: {token[:20]}...')
            
            # Test vehicles endpoint
            headers = {'Authorization': f'Bearer {token}'}
            vehicles_response = requests.get(f'{base_url}/vehicles', headers=headers)
            
            if vehicles_response.status_code == 200:
                vehicles_data = vehicles_response.json()
                print(f'✅ Vehicles endpoint working - Response: {vehicles_data}')
                
                # Check if it's a paginated response
                if 'items' in vehicles_data:
                    vehicles = vehicles_data['items']
                    print(f'Found {len(vehicles)} vehicles (paginated)')
                else:
                    vehicles = vehicles_data
                    print(f'Found {len(vehicles)} vehicles')
                
                for v in vehicles:
                    print(f'  - {v["kennzeichen"]} (ID: {v["id"]})')
                
                # Test vehicle-checklist endpoints
                if vehicles:
                    vehicle_id = vehicles[0]['id']
                    print(f'\n=== Testing Vehicle-Checklist Endpoints for Vehicle {vehicle_id} ===')
                    
                    # Test vehicle checklists
                    vc_response = requests.get(f'{base_url}/vehicles/{vehicle_id}/checklists', headers=headers)
                    if vc_response.status_code == 200:
                        checklists = vc_response.json()
                        print(f'✅ Vehicle checklists endpoint working - Found {len(checklists)} checklists')
                    else:
                        print(f'❌ Vehicle checklists failed: {vc_response.status_code}')
                    
                    # Test available checklists
                    va_response = requests.get(f'{base_url}/vehicles/{vehicle_id}/available-checklists', headers=headers)
                    if va_response.status_code == 200:
                        available = va_response.json()
                        print(f'✅ Available checklists endpoint working - Found {len(available)} available')
                    else:
                        print(f'❌ Available checklists failed: {va_response.status_code}')
            else:
                print(f'❌ Vehicles endpoint failed: {vehicles_response.status_code}')
                
        else:
            print(f'❌ Login failed: {login_response.status_code}')
            
    except Exception as e:
        print(f'❌ Connection error: {e}')

if __name__ == "__main__":
    test_backend_connectivity()