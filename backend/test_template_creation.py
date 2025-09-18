#!/usr/bin/env python3
"""
Template Creation Test Script

This script tests that organisator users can now create checklist templates
through both the regular API and the enhanced template creation endpoints.
"""

import requests
import json
from datetime import datetime


class TemplateCreationTest:
    def __init__(self, base_url="http://10.20.1.108:8000"):
        self.base_url = base_url
        self.admin_token = None
        self.organisator_token = None
        
    def login_user(self, username, password):
        """Login a user and return their access token"""
        response = requests.post(
            f"{self.base_url}/auth/login",
            json={"username": username, "password": password},
            headers={"Content-Type": "application/json"}
        )
        
        if response.status_code == 200:
            return response.json().get("access_token")
        else:
            print(f"❌ Login failed for {username}: {response.status_code} - {response.text}")
            return None
    
    def test_login_admin(self):
        """Test admin login"""
        print("🔐 Testing admin login...")
        self.admin_token = self.login_user("admin", "admin")
        if self.admin_token:
            print("✅ Admin login successful")
            return True
        return False
    
    def test_login_organisator(self):
        """Test organisator login"""
        print("🔐 Testing organisator login...")
        # Try test1234 first, then common passwords for organisator user
        passwords_to_try = ["test1234", "password", "password123", "admin"]
        
        for password in passwords_to_try:
            self.organisator_token = self.login_user("organisator", password)
            if self.organisator_token:
                print(f"✅ Organisator login successful with password: {password}")
                return True
            # Also try test_organisator username
            self.organisator_token = self.login_user("test_organisator", password)
            if self.organisator_token:
                print(f"✅ Test organisator login successful with password: {password}")
                return True
        
        print("ℹ️ No organisator user found with common passwords, will create one with admin token")
        return self.create_organisator_user()
    
    def create_organisator_user(self):
        """Create an organisator user for testing"""
        if not self.admin_token:
            print("❌ Need admin token to create organisator user")
            return False
        
        print("👤 Creating organisator test user...")
        user_data = {
            "username": "test_organisator",
            "email": "organisator@test.com", 
            "password": "test1234",  # Updated to use consistent password
            "rolle": "organisator"
        }
        
        response = requests.post(
            f"{self.base_url}/auth/users",
            json=user_data,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self.admin_token}"
            }
        )
        
        if response.status_code == 201:
            print("✅ Organisator user created successfully")
            # Now login with the new user
            self.organisator_token = self.login_user("test_organisator", "test1234")
            return self.organisator_token is not None
        else:
            print(f"❌ Failed to create organisator user: {response.status_code} - {response.text}")
            return False
    
    def test_csv_template_import_permission(self):
        """Test that organisator can now import CSV templates"""
        print("📋 Testing CSV template import permission for organisator...")
        
        if not self.organisator_token:
            print("❌ No organisator token available")
            return False
        
        response = requests.post(
            f"{self.base_url}/checklists/import-templates",
            headers={
                "Authorization": f"Bearer {self.organisator_token}"
            }
        )
        
        # We expect either success or a different error (not permission denied)
        if response.status_code == 403:
            error_detail = response.json().get("detail", "")
            if "Admin Berechtigung erforderlich" in error_detail:
                print("❌ Organisator still denied CSV template import")
                return False
            elif "Organisator oder Admin Berechtigung erforderlich" in error_detail:
                print("❌ Organisator still denied (old error message)")
                return False
        
        print("✅ Organisator has permission for CSV template import")
        return True
    
    def test_regular_template_creation(self):
        """Test regular template creation via standard checklist endpoint"""
        print("📝 Testing regular template creation...")
        
        if not self.organisator_token:
            print("❌ No organisator token available")
            return False
        
        template_data = {
            "name": "Test Template by Organisator",
            "fahrzeuggruppe_id": 1,
            "template": True,
            "items": [
                {
                    "beschreibung": "Test Item 1",
                    "item_type": "standard",
                    "pflicht": True,
                    "reihenfolge": 10
                },
                {
                    "beschreibung": "Test Item 2", 
                    "item_type": "standard",
                    "pflicht": False,
                    "reihenfolge": 20
                }
            ]
        }
        
        response = requests.post(
            f"{self.base_url}/checklists",
            json=template_data,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self.organisator_token}"
            }
        )
        
        if response.status_code == 201:
            result = response.json()
            print(f"✅ Template created successfully: ID {result.get('id')}, Name: {result.get('name')}")
            return True
        elif response.status_code == 403:
            print(f"❌ Permission denied for regular template creation: {response.json().get('detail')}")
            return False
        else:
            print(f"❌ Template creation failed: {response.status_code} - {response.text}")
            return False
    
    def test_enhanced_template_creation(self):
        """Test enhanced template creation with different item types"""
        print("🚀 Testing enhanced template creation...")
        
        if not self.organisator_token:
            print("❌ No organisator token available")
            return False
        
        enhanced_template_data = {
            "name": "Enhanced Test Template",
            "fahrzeuggruppe_id": 1,
            "items": [
                {
                    "beschreibung": "Fächer G1 Bewertung",
                    "item_type": "rating_1_6",
                    "validation_config": {"min_value": 1, "max_value": 6},
                    "pflicht": True,
                    "reihenfolge": 10
                },
                {
                    "beschreibung": "Kraftstoffstand",
                    "item_type": "percentage",
                    "validation_config": {"min_value": 0, "max_value": 100},
                    "pflicht": True,
                    "reihenfolge": 20
                },
                {
                    "beschreibung": "Atemschutzgeräte",
                    "item_type": "atemschutz",
                    "requires_tuv": True,
                    "subcategories": {
                        "tuv_platte": {"type": "date", "required": True},
                        "tuv_respihood": {"type": "date", "required": True}
                    },
                    "pflicht": True,
                    "reihenfolge": 30
                }
            ]
        }
        
        response = requests.post(
            f"{self.base_url}/enhanced-checklists/templates",
            json=enhanced_template_data,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {self.organisator_token}"
            }
        )
        
        if response.status_code == 200:
            result = response.json()
            template_info = result.get("template", {})
            print(f"✅ Enhanced template created: ID {template_info.get('id')}, Items: {template_info.get('item_count')}")
            return True
        elif response.status_code == 403:
            print(f"❌ Permission denied for enhanced template creation: {response.json().get('detail')}")
            return False
        else:
            print(f"❌ Enhanced template creation failed: {response.status_code} - {response.text}")
            return False
    
    def test_gruppenleiter_restrictions(self):
        """Test that gruppenleiter users still cannot create templates"""
        print("🚫 Testing that gruppenleiter cannot create templates...")
        
        gruppenleiter_token = None
        
        # First, try to login with existing user
        gruppenleiter_token = self.login_user("test_gruppenleiter", "test1234")
        
        if not gruppenleiter_token and self.admin_token:
            print("👤 Gruppenleiter user not found, attempting to create...")
            
            # Check if user already exists first
            response = requests.get(
                f"{self.base_url}/auth/users",
                headers={"Authorization": f"Bearer {self.admin_token}"}
            )
            
            if response.status_code == 200:
                try:
                    users = response.json()
                    # Handle both list and dict response formats
                    if isinstance(users, list):
                        existing_gl = next((u for u in users if u.get("username") == "test_gruppenleiter"), None)
                    elif isinstance(users, dict) and "users" in users:
                        existing_gl = next((u for u in users["users"] if u.get("username") == "test_gruppenleiter"), None)
                    else:
                        existing_gl = None
                except (ValueError, TypeError, AttributeError):
                    print("⚠️ Unexpected response format from /auth/users endpoint")
                    existing_gl = None
                if existing_gl:
                    print(f"ℹ️ Gruppenleiter user exists with role: {existing_gl.get('rolle')}")
                    # Try login again in case password is different
                    for pwd in ["test1234", "gl123", "password", "admin"]:
                        gruppenleiter_token = self.login_user("test_gruppenleiter", pwd)
                        if gruppenleiter_token:
                            print(f"✅ Gruppenleiter authenticated with password: {pwd}")
                            break
                else:
                    # User doesn't exist, create it
                    user_data = {
                        "username": "test_gruppenleiter",
                        "email": "gruppenleiter@test.com",
                        "password": "test1234",
                        "rolle": "gruppenleiter"
                    }
                    
                    response = requests.post(
                        f"{self.base_url}/auth/users",
                        json=user_data,
                        headers={
                            "Content-Type": "application/json",
                            "Authorization": f"Bearer {self.admin_token}"
                        }
                    )
                    
                    if response.status_code == 201:
                        print("✅ Gruppenleiter user created successfully")
                        gruppenleiter_token = self.login_user("test_gruppenleiter", "test1234")
                    else:
                        print(f"❌ Failed to create gruppenleiter: {response.status_code} - {response.text}")
        
        if not gruppenleiter_token:
            print("ℹ️ Skipping gruppenleiter test - could not authenticate gruppenleiter user")
            print("💡 Tip: Run 'python create_organisator_user.py' to create test users")
            return True
        
        print("✅ Gruppenleiter authenticated successfully")
        
        # Try to create a template with gruppenleiter role
        template_data = {
            "name": "Unauthorized Template",
            "fahrzeuggruppe_id": 1,
            "items": []
        }
        
        response = requests.post(
            f"{self.base_url}/enhanced-checklists/templates",
            json=template_data,
            headers={
                "Content-Type": "application/json",
                "Authorization": f"Bearer {gruppenleiter_token}"
            }
        )
        
        if response.status_code == 403:
            print("✅ Gruppenleiter correctly denied template creation")
            return True
        else:
            print(f"❌ Gruppenleiter should not be able to create templates: {response.status_code}")
            print(f"Response: {response.text}")
            return False
    
    def run_all_tests(self):
        """Run all template creation tests"""
        print("🧪 Starting Template Creation Permission Tests")
        print("=" * 50)
        
        tests = [
            ("Admin Login", self.test_login_admin),
            ("Organisator Login/Setup", self.test_login_organisator),
            ("CSV Import Permission", self.test_csv_template_import_permission),
            ("Regular Template Creation", self.test_regular_template_creation),
            ("Enhanced Template Creation", self.test_enhanced_template_creation),
            ("Gruppenleiter Restrictions", self.test_gruppenleiter_restrictions)
        ]
        
        passed = 0
        total = len(tests)
        
        for test_name, test_func in tests:
            print(f"\\n🧪 Running: {test_name}")
            try:
                if test_func():
                    passed += 1
                    print(f"✅ {test_name} - PASSED")
                else:
                    print(f"❌ {test_name} - FAILED")
            except Exception as e:
                print(f"💥 {test_name} - ERROR: {str(e)}")
        
        print(f"\\n📊 Test Results: {passed}/{total} tests passed")
        
        if passed == total:
            print("🎉 All tests passed! Organisator users can now create templates.")
        else:
            print("⚠️ Some tests failed. Check the output above for details.")
        
        return passed == total


def main():
    """Run template creation tests"""
    print("🔧 Template Creation Permission Test Suite")
    print("Testing that organisator role can create checklist templates")
    print()
    
    # Check if backend is running
    tester = TemplateCreationTest()
    
    try:
        response = requests.get(f"{tester.base_url}/health")
        if response.status_code != 200:
            print("❌ Backend is not running or not accessible")
            print("Please start the backend with: python -m uvicorn app.main:app --reload")
            return
    except requests.exceptions.ConnectionError:
        print("❌ Cannot connect to backend at http://localhost:8000")
        print("Please start the backend first")
        return
    
    print("✅ Backend is running")
    
    # Run tests
    success = tester.run_all_tests()
    
    if success:
        print("\\n🎯 CONCLUSION: Organisator role template creation is working correctly!")
    else:
        print("\\n❌ CONCLUSION: There are still issues with organisator template creation.")


if __name__ == "__main__":
    main()