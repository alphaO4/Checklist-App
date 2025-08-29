import requests
import json
from datetime import datetime, timedelta

# Test the comprehensive vehicle editing functionality
BASE_URL = "http://127.0.0.1:8000"

def login_as_admin():
    """Get admin authentication token"""
    response = requests.post(f"{BASE_URL}/auth/login", json={
        "username": "admin", 
        "password": "admin"
    })
    if response.status_code == 200:
        token = response.json()['access_token']
        print(f"✅ Admin login successful")
        return token
    else:
        print(f"❌ Admin login failed: {response.text}")
        return None

def test_vehicle_list(token):
    """Test vehicle listing"""
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.get(f"{BASE_URL}/vehicles", headers=headers)
    
    if response.status_code == 200:
        data = response.json()
        print(f"✅ Vehicle list: {len(data['items'])} vehicles found")
        return data['items']
    else:
        print(f"❌ Vehicle list failed: {response.text}")
        return []

def test_vehicle_types(token):
    """Test vehicle types endpoint"""
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.get(f"{BASE_URL}/vehicle-types", headers=headers)
    
    if response.status_code == 200:
        data = response.json()
        print(f"✅ Vehicle types: {len(data)} types available")
        return data
    else:
        print(f"❌ Vehicle types failed: {response.text}")
        return []

def test_fahrzeuggruppen(token):
    """Test fahrzeuggruppen endpoint"""
    headers = {"Authorization": f"Bearer {token}"}
    response = requests.get(f"{BASE_URL}/fahrzeuggruppen", headers=headers)
    
    if response.status_code == 200:
        data = response.json()
        print(f"✅ Fahrzeuggruppen: {len(data)} groups available")
        return data
    else:
        print(f"❌ Fahrzeuggruppen failed: {response.text}")
        return []

def test_vehicle_edit(token, vehicle_id):
    """Test vehicle editing functionality"""
    headers = {"Authorization": f"Bearer {token}"}
    
    # Test getting a specific vehicle
    response = requests.get(f"{BASE_URL}/vehicles/{vehicle_id}", headers=headers)
    if response.status_code == 200:
        vehicle = response.json()
        print(f"✅ Vehicle {vehicle_id} details retrieved: {vehicle['kennzeichen']}")
        
        # Test updating the vehicle
        update_data = {
            "kennzeichen": f"TEST-{vehicle_id}",
        }
        
        response = requests.put(f"{BASE_URL}/vehicles/{vehicle_id}", 
                              headers=headers, json=update_data)
        if response.status_code == 200:
            updated_vehicle = response.json()
            print(f"✅ Vehicle {vehicle_id} updated: {updated_vehicle['kennzeichen']}")
            
            # Restore original kennzeichen
            restore_data = {"kennzeichen": vehicle['kennzeichen']}
            requests.put(f"{BASE_URL}/vehicles/{vehicle_id}", 
                        headers=headers, json=restore_data)
            print(f"✅ Vehicle {vehicle_id} restored to original kennzeichen")
        else:
            print(f"❌ Vehicle update failed: {response.text}")
    else:
        print(f"❌ Vehicle details failed: {response.text}")

def test_tuv_management(token, vehicle_id):
    """Test TÜV management functionality"""
    headers = {"Authorization": f"Bearer {token}"}
    
    # Test getting TÜV records for a vehicle
    response = requests.get(f"{BASE_URL}/vehicles/{vehicle_id}/tuv", headers=headers)
    print(f"TÜV API response status: {response.status_code}")
    
    if response.status_code == 200:
        tuv_data = response.json()
        print(f"✅ TÜV data for vehicle {vehicle_id}: {tuv_data}")
        
        # Check if TÜV data exists
        if tuv_data.get('tuv_data') and tuv_data['tuv_data'] is not None:
            tuv_record = tuv_data['tuv_data']
            tuv_id = tuv_record['id']
            print(f"Found TÜV record ID: {tuv_id}, Status: {tuv_record.get('status', 'unknown')}")
            
            # Test updating TÜV record via general TÜV endpoint
            future_date = (datetime.now() + timedelta(days=365)).isoformat()
            update_data = {
                "ablauf_datum": future_date,
                "status": "current"
            }
            
            # Use the general TÜV deadlines endpoint instead
            response = requests.put(f"{BASE_URL}/tuv/deadlines/{tuv_id}", 
                                  headers=headers, json=update_data)
            if response.status_code == 200:
                updated_tuv = response.json()
                print(f"✅ TÜV record {tuv_id} updated via /tuv/deadlines")
            else:
                print(f"❌ TÜV update via /tuv/deadlines failed: {response.text}")
                
                # Try the vehicle-specific endpoint
                response = requests.put(f"{BASE_URL}/vehicles/{vehicle_id}/tuv", 
                                      headers=headers, json=update_data)
                if response.status_code == 200:
                    updated_tuv = response.json()
                    print(f"✅ TÜV record updated via /vehicles/{vehicle_id}/tuv")
                else:
                    print(f"❌ TÜV update via vehicles endpoint failed: {response.text}")
        else:
            print("❌ No TÜV data found for this vehicle")
    else:
        print(f"❌ TÜV records failed: {response.text}")

def main():
    print("🚗 Testing Comprehensive Vehicle Editing Functionality")
    print("=" * 60)
    
    # Login as admin
    token = login_as_admin()
    if not token:
        return
    
    # Test all endpoints
    vehicles = test_vehicle_list(token)
    vehicle_types = test_vehicle_types(token)
    fahrzeuggruppen = test_fahrzeuggruppen(token)
    
    if vehicles:
        # Test editing the first vehicle
        vehicle_id = vehicles[0]['id']
        test_vehicle_edit(token, vehicle_id)
        test_tuv_management(token, vehicle_id)
    
    print("\n" + "=" * 60)
    print("🎉 Vehicle editing functionality test completed!")

if __name__ == "__main__":
    main()
