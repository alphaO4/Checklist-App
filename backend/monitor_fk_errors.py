#!/usr/bin/env python3

import sqlite3
import time
import requests

def monitor_database_operations():
    """Monitor database for FK constraint errors during API operations"""
    
    base_url = 'http://10.20.1.108:8000'
    
    # Login first
    try:
        login_response = requests.post(f'{base_url}/auth/login', 
            json={'username': 'admin', 'password': 'admin'}, timeout=5)
        
        if login_response.status_code != 200:
            print(f"❌ Login failed: {login_response.status_code}")
            return
            
        token = login_response.json()['access_token']
        headers = {'Authorization': f'Bearer {token}'}
        print("✅ Login successful")
        
    except Exception as e:
        print(f"❌ Backend not accessible: {e}")
        print("Please ensure backend is running on http://10.20.1.108:8000")
        return
    
    print("\n=== Testing API Operations for FK Errors ===\n")
    
    # Test various operations that might trigger FK errors
    test_operations = [
        {
            'name': 'List vehicles',
            'method': 'GET',
            'url': f'{base_url}/vehicles'
        },
        {
            'name': 'List checklists',
            'method': 'GET', 
            'url': f'{base_url}/checklists'
        },
        {
            'name': 'Get vehicle checklists',
            'method': 'GET',
            'url': f'{base_url}/vehicles/1/checklists'
        },
        {
            'name': 'Get available checklists',
            'method': 'GET',
            'url': f'{base_url}/vehicles/1/available-checklists'
        },
        {
            'name': 'Get checklist vehicles',
            'method': 'GET',
            'url': f'{base_url}/checklists/1/vehicles'
        }
    ]
    
    # Test each operation
    for operation in test_operations:
        print(f"Testing: {operation['name']}")
        
        try:
            if operation['method'] == 'GET':
                response = requests.get(operation['url'], headers=headers, timeout=10)
            elif operation['method'] == 'POST':
                response = requests.post(operation['url'], headers=headers, json=operation.get('data', {}), timeout=10)
            
            if response.status_code == 200:
                print(f"  ✅ {operation['name']} - Success")
            else:
                print(f"  ❌ {operation['name']} - Status: {response.status_code}")
                if 'foreign key' in response.text.lower() or 'constraint' in response.text.lower():
                    print(f"  🔍 Foreign key error detected: {response.text}")
                else:
                    print(f"  📝 Response: {response.text[:200]}")
                    
        except requests.exceptions.Timeout:
            print(f"  ⏱️  {operation['name']} - Timeout")
        except Exception as e:
            print(f"  ❌ {operation['name']} - Error: {e}")
        
        time.sleep(0.5)  # Small delay between requests
    
    print("\n=== Database Integrity Check ===")
    
    # Check database integrity after operations
    conn = sqlite3.connect('data.db')
    cursor = conn.cursor()
    
    cursor.execute('PRAGMA foreign_key_check;')
    violations = cursor.fetchall()
    
    if violations:
        print("❌ Foreign key violations found after API operations:")
        for v in violations:
            print(f"  {v}")
    else:
        print("✅ No foreign key violations after API operations")
    
    # Check for any new orphaned records
    print("\n=== Orphaned Records Check ===")
    
    # Check executions without valid checklists or vehicles
    cursor.execute('''
        SELECT e.id, e.checkliste_id, e.fahrzeug_id 
        FROM checklist_ausfuehrungen e
        LEFT JOIN checklisten c ON e.checkliste_id = c.id
        LEFT JOIN fahrzeuge f ON e.fahrzeug_id = f.id
        WHERE c.id IS NULL OR f.id IS NULL;
    ''')
    
    orphaned_executions = cursor.fetchall()
    if orphaned_executions:
        print("❌ Orphaned executions found:")
        for exec_id, checklist_id, vehicle_id in orphaned_executions:
            print(f"  Execution {exec_id}: checklist={checklist_id}, vehicle={vehicle_id}")
    else:
        print("✅ No orphaned execution records")
    
    conn.close()
    
    print("\n=== Monitoring Complete ===")
    print("If you're still seeing FK errors, they might be:")
    print("1. Happening in the Android app during offline sync")
    print("2. Occurring during database migrations")
    print("3. Related to concurrent operations")
    print("4. Happening in a code path not tested above")

if __name__ == "__main__":
    monitor_database_operations()