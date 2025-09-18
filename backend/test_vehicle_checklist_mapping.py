#!/usr/bin/env python3
"""
Test script for vehicle-checklist mapping functionality
"""

import requests
import json
from datetime import datetime


class VehicleChecklistMappingTester:
    def __init__(self, base_url="http://10.20.1.108:8000"):
        self.base_url = base_url
        self.token = None
        self.session = requests.Session()
    
    def login(self, username="admin", password="admin"):
        """Login and get JWT token"""
        try:
            response = self.session.post(
                f"{self.base_url}/auth/login",
                json={"username": username, "password": password}
            )
            response.raise_for_status()
            data = response.json()
            self.token = data.get("access_token")
            
            # Set auth header for all future requests
            self.session.headers.update({"Authorization": f"Bearer {self.token}"})
            print(f"✅ Login successful - Token: {self.token[:20]}...")
            return True
            
        except requests.exceptions.RequestException as e:
            print(f"❌ Login failed: {e}")
            return False
    
    def test_get_vehicles(self):
        """Test listing all vehicles"""
        try:
            response = self.session.get(f"{self.base_url}/vehicles")
            response.raise_for_status()
            data = response.json()
            
            print(f"✅ Get vehicles successful - Found {len(data.get('items', []))} vehicles")
            for vehicle in data.get('items', [])[:3]:  # Show first 3
                print(f"   - Vehicle: {vehicle['kennzeichen']} (ID: {vehicle['id']}, Group: {vehicle['fahrzeuggruppe_id']})")
            
            return data.get('items', [])
            
        except requests.exceptions.RequestException as e:
            print(f"❌ Get vehicles failed: {e}")
            return []
    
    def test_get_checklists(self):
        """Test listing all checklists"""
        try:
            response = self.session.get(f"{self.base_url}/checklists")
            response.raise_for_status()
            data = response.json()
            
            print(f"✅ Get checklists successful - Found {len(data.get('items', []))} checklists")
            for checklist in data.get('items', [])[:3]:  # Show first 3
                print(f"   - Checklist: {checklist['name']} (ID: {checklist['id']}, Group: {checklist['fahrzeuggruppe_id']}, Template: {checklist['template']})")
            
            return data.get('items', [])
            
        except requests.exceptions.RequestException as e:
            print(f"❌ Get checklists failed: {e}")
            return []
    
    def test_get_vehicle_checklists(self, vehicle_id):
        """Test getting checklists for a specific vehicle"""
        try:
            response = self.session.get(f"{self.base_url}/vehicles/{vehicle_id}/checklists")
            response.raise_for_status()
            data = response.json()
            
            print(f"✅ Get vehicle checklists successful for vehicle {vehicle_id}")
            print(f"   - Vehicle: {data.get('kennzeichen')} (Group: {data.get('fahrzeuggruppe_id')})")
            print(f"   - Available checklists: {len(data.get('checklists', []))}")
            
            for checklist in data.get('checklists', []):
                print(f"     * {checklist['name']} (ID: {checklist['id']}, Template: {checklist['template']})")
            
            return data
            
        except requests.exceptions.RequestException as e:
            print(f"❌ Get vehicle checklists failed: {e}")
            return None
    
    def test_get_available_checklists_for_vehicle(self, vehicle_id):
        """Test getting available checklists for execution on a vehicle"""
        try:
            response = self.session.get(f"{self.base_url}/vehicles/{vehicle_id}/available-checklists")
            response.raise_for_status()
            data = response.json()
            
            print(f"✅ Get available checklists for vehicle {vehicle_id}")
            print(f"   - Vehicle: {data.get('kennzeichen')}")
            print(f"   - Available for execution: {len(data.get('available_checklists', []))}")
            
            for checklist in data.get('available_checklists', []):
                status = "🟢 ACTIVE" if checklist['is_active'] else "⚪ Available"
                print(f"     * {checklist['name']} (ID: {checklist['id']}) - {status}")
                if checklist.get('active_execution_id'):
                    print(f"       Execution ID: {checklist['active_execution_id']}")
            
            return data
            
        except requests.exceptions.RequestException as e:
            print(f"❌ Get available checklists failed: {e}")
            return None
    
    def test_get_checklist_vehicles(self, checklist_id):
        """Test getting vehicles for a specific checklist"""
        try:
            response = self.session.get(f"{self.base_url}/checklists/{checklist_id}/vehicles")
            response.raise_for_status()
            data = response.json()
            
            print(f"✅ Get checklist vehicles successful for checklist {checklist_id}")
            print(f"   - Checklist: {data.get('checklist_name')} (Group: {data.get('fahrzeuggruppe_id')})")
            print(f"   - Available vehicles: {len(data.get('available_vehicles', []))}")
            
            for vehicle in data.get('available_vehicles', []):
                status = "🟢 ACTIVE" if vehicle['is_active'] else "⚪ Available"
                fahrzeugtyp = vehicle.get('fahrzeugtyp', {})
                type_info = f"({fahrzeugtyp.get('code', 'N/A')})" if fahrzeugtyp else ""
                print(f"     * {vehicle['kennzeichen']} {type_info} (ID: {vehicle['id']}) - {status}")
                if vehicle.get('active_execution_id'):
                    print(f"       Execution ID: {vehicle['active_execution_id']}")
            
            return data
            
        except requests.exceptions.RequestException as e:
            print(f"❌ Get checklist vehicles failed: {e}")
            return None
    
    def test_start_checklist_for_vehicle(self, vehicle_id, checklist_id):
        """Test starting a checklist execution for a vehicle"""
        try:
            response = self.session.post(f"{self.base_url}/vehicles/{vehicle_id}/checklists/{checklist_id}/start")
            response.raise_for_status()
            data = response.json()
            
            print(f"✅ Started checklist execution")
            print(f"   - Execution ID: {data.get('id')}")
            print(f"   - Vehicle ID: {data.get('fahrzeug_id')}")
            print(f"   - Checklist ID: {data.get('checkliste_id')}")
            print(f"   - Status: {data.get('status')}")
            print(f"   - Started at: {data.get('started_at')}")
            
            return data
            
        except requests.exceptions.RequestException as e:
            print(f"❌ Start checklist execution failed: {e}")
            if hasattr(e, 'response') and e.response is not None:
                try:
                    error_detail = e.response.json()
                    print(f"   Error detail: {error_detail.get('detail', 'Unknown error')}")
                except:
                    print(f"   Response: {e.response.text}")
            return None
    
    def run_full_test(self):
        """Run complete test suite"""
        print("=" * 60)
        print("🚀 Testing Vehicle-Checklist Mapping")
        print("=" * 60)
        
        # Step 1: Login
        if not self.login():
            return False
        
        print("\n" + "=" * 60)
        print("📋 Testing Basic Data Retrieval")
        print("=" * 60)
        
        # Step 2: Get vehicles and checklists
        vehicles = self.test_get_vehicles()
        print()
        checklists = self.test_get_checklists()
        
        if not vehicles or not checklists:
            print("❌ No test data available")
            return False
        
        print("\n" + "=" * 60)
        print("🔗 Testing Vehicle-Checklist Relationships")
        print("=" * 60)
        
        # Step 3: Test vehicle-checklist relationships
        test_vehicle = vehicles[0] if vehicles else None
        test_checklist = None
        
        # Find a non-template checklist
        for checklist in checklists:
            if not checklist.get('template', False):
                test_checklist = checklist
                break
        
        if not test_vehicle or not test_checklist:
            print("❌ No suitable test data found")
            return False
        
        print(f"\n🎯 Using test vehicle: {test_vehicle['kennzeichen']} (ID: {test_vehicle['id']})")
        print(f"🎯 Using test checklist: {test_checklist['name']} (ID: {test_checklist['id']})")
        
        # Test getting checklists for vehicle
        print(f"\n--- Testing GET /vehicles/{test_vehicle['id']}/checklists ---")
        self.test_get_vehicle_checklists(test_vehicle['id'])
        
        # Test getting available checklists for vehicle
        print(f"\n--- Testing GET /vehicles/{test_vehicle['id']}/available-checklists ---")
        available_data = self.test_get_available_checklists_for_vehicle(test_vehicle['id'])
        
        # Test getting vehicles for checklist
        print(f"\n--- Testing GET /checklists/{test_checklist['id']}/vehicles ---")
        self.test_get_checklist_vehicles(test_checklist['id'])
        
        # Step 4: Test starting checklist execution (if compatible)
        print("\n" + "=" * 60)
        print("▶️ Testing Checklist Execution")
        print("=" * 60)
        
        # Check if the test checklist is available for the test vehicle
        if available_data:
            compatible_checklist = None
            for avail_checklist in available_data.get('available_checklists', []):
                if not avail_checklist['is_active']:  # Find one that's not active
                    compatible_checklist = avail_checklist
                    break
            
            if compatible_checklist:
                print(f"\n--- Testing POST /vehicles/{test_vehicle['id']}/checklists/{compatible_checklist['id']}/start ---")
                execution_data = self.test_start_checklist_for_vehicle(test_vehicle['id'], compatible_checklist['id'])
                
                if execution_data:
                    print(f"\n🔄 Re-testing available checklists after starting execution...")
                    self.test_get_available_checklists_for_vehicle(test_vehicle['id'])
            else:
                print("⚠️ No available checklists to start (all may be active or incompatible)")
        
        print("\n" + "=" * 60)
        print("✅ Test Suite Complete!")
        print("=" * 60)
        return True


def main():
    """Main test runner"""
    tester = VehicleChecklistMappingTester()
    
    try:
        success = tester.run_full_test()
        if success:
            print("\n🎉 All tests completed successfully!")
            return 0
        else:
            print("\n💥 Some tests failed!")
            return 1
            
    except KeyboardInterrupt:
        print("\n\n⏹️ Tests interrupted by user")
        return 1
    except Exception as e:
        print(f"\n💥 Unexpected error: {e}")
        return 1


if __name__ == "__main__":
    exit(main())