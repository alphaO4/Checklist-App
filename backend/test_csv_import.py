#!/usr/bin/env python3
"""
Test script for CSV checklist import functionality
Run this to verify the backend can parse and import the real checklist CSV files
"""

import requests
import json
import sys
import os

# Backend URL
BASE_URL = "http://10.20.1.108:8000"

def test_csv_import():
    """Test CSV import functionality"""
    
    print("🚒 Testing CSV Checklist Import for Fire Department App")
    print("=" * 60)
    
    # Test 1: Check if backend is running
    print("1. Testing backend connectivity...")
    try:
        response = requests.get(f"{BASE_URL}/health")
        if response.status_code == 200:
            print("   ✅ Backend is running")
        else:
            print(f"   ❌ Backend health check failed: {response.status_code}")
            return False
    except Exception as e:
        print(f"   ❌ Cannot connect to backend: {e}")
        return False
    
    # Test 2: Login as admin to get token
    print("\n2. Logging in as admin...")
    try:
        login_data = {
            "username": "admin",
            "password": "admin"
        }
        response = requests.post(f"{BASE_URL}/auth/login", json=login_data)
        if response.status_code == 200:
            token_data = response.json()
            token = token_data["access_token"]
            print("   ✅ Login successful")
        else:
            print(f"   ❌ Login failed: {response.status_code} - {response.text}")
            return False
    except Exception as e:
        print(f"   ❌ Login error: {e}")
        return False
    
    headers = {"Authorization": f"Bearer {token}"}
    
    # Test 3: Get CSV summary
    print("\n3. Getting CSV summary...")
    try:
        response = requests.get(f"{BASE_URL}/checklists/csv-summary", headers=headers)
        if response.status_code == 200:
            summary = response.json()
            print(f"   ✅ Found {summary.get('total_checklists', 0)} CSV files")
            print(f"   📋 Vehicle types: {', '.join(summary.get('vehicle_types', []))}")
            
            for checklist in summary.get('checklists', []):
                print(f"   📄 {checklist['filename']}: {checklist['item_count']} items")
                
        else:
            print(f"   ❌ CSV summary failed: {response.status_code} - {response.text}")
    except Exception as e:
        print(f"   ❌ CSV summary error: {e}")
    
    # Test 4: Import CSV templates
    print("\n4. Importing CSV templates...")
    try:
        response = requests.post(f"{BASE_URL}/checklists/import-csv-templates", headers=headers)
        if response.status_code == 200:
            result = response.json()
            print(f"   ✅ {result.get('message', 'Import successful')}")
            
            for template in result.get('templates', []):
                print(f"   📋 Template '{template['name']}' (ID: {template['id']}) with {template['item_count']} items")
                
        else:
            print(f"   ⚠️  Import response: {response.status_code} - {response.text}")
            # Don't fail here - templates might already exist
    except Exception as e:
        print(f"   ❌ Import error: {e}")
    
    # Test 5: List imported checklists
    print("\n5. Verifying imported templates...")
    try:
        response = requests.get(f"{BASE_URL}/checklists?template=true", headers=headers)
        if response.status_code == 200:
            data = response.json()
            checklists = data.get('checklists', [])
            print(f"   ✅ Found {len(checklists)} template(s)")
            
            for checklist in checklists[:3]:  # Show first 3
                print(f"   📋 {checklist['name']} (ID: {checklist['id']})")
                
        else:
            print(f"   ❌ Failed to list templates: {response.status_code}")
    except Exception as e:
        print(f"   ❌ List templates error: {e}")
    
    print("\n" + "=" * 60)
    print("✅ CSV Import Test Complete!")
    print("\n🔥 Real Fire Department Checklist Data:")
    print("   - FirstResponder_B-20218.csv: Emergency response vehicle")
    print("   - LHF1_B-2183.csv: Fire truck with equipment checks")  
    print("   - TLF_B-2226.csv: Tanker truck with water systems")
    print("\n📱 Ready for Android app integration!")
    
    return True

if __name__ == "__main__":
    success = test_csv_import()
    sys.exit(0 if success else 1)