import requests
import json

# Test the backend API with the new relationship structure
BASE_URL = "http://10.20.1.108:8000"

def test_login():
    """Test authentication"""
    response = requests.post(f"{BASE_URL}/auth/login", json={
        "username": "admin", 
        "password": "admin"
    })
    print(f"Login response: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"Token: {data['access_token'][:20]}...")
        return data['access_token']
    else:
        print(f"Login failed: {response.text}")
        return None

def test_groups(token):
    """Test groups endpoint"""
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.get(f"{BASE_URL}/groups", headers=headers)
    print(f"\nGroups response: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"Groups found: {len(data['items'])}")
        for group in data['items']:
            print(f"  - {group['name']} (ID: {group['id']}, FahrzeugGruppe: {group.get('fahrzeuggruppe_id')})")
    else:
        print(f"Groups failed: {response.text}")

def test_users(token):
    """Test users endpoint to see the new gruppe_id field"""
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.get(f"{BASE_URL}/auth/users", headers=headers)
    print(f"\nUsers response: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"Users found: {len(data['items'])}")
        for user in data['items']:
            print(f"  - {user['username']} (Rolle: {user['rolle']}, Gruppe: {user.get('gruppe_id')})")
    else:
        print(f"Users failed: {response.text}")

def test_fahrzeuggruppen(token):
    """Test fahrzeuggruppen endpoint"""
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.get(f"{BASE_URL}/fahrzeuggruppen", headers=headers)
    print(f"\nFahrzeuggruppen response: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"Fahrzeuggruppen found: {len(data)}")
        for fg in data:
            print(f"  - {fg['name']} (ID: {fg['id']})")
    else:
        print(f"Fahrzeuggruppen failed: {response.text}")

def test_vehicles(token):
    """Test vehicles endpoint to see fahrzeuggruppe assignments"""
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.get(f"{BASE_URL}/vehicles", headers=headers)
    print(f"\nVehicles response: {response.status_code}")
    if response.status_code == 200:
        data = response.json()
        print(f"Vehicles found: {len(data['items'])}")
        for vehicle in data['items']:
            print(f"  - {vehicle['kennzeichen']} (FahrzeugGruppe: {vehicle['fahrzeuggruppe_id']})")
    else:
        print(f"Vehicles failed: {response.text}")

if __name__ == "__main__":
    print("Testing Backend API with new relationship structure")
    print("=" * 60)
    
    token = test_login()
    if token:
        test_groups(token)
        test_users(token)
        test_fahrzeuggruppen(token)
        test_vehicles(token)
    
    print("\n" + "=" * 60)
    print("Test completed!")
