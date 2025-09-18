#!/usr/bin/env python3

import sqlite3

def check_fk_integrity():
    conn = sqlite3.connect('data.db')
    cursor = conn.cursor()
    
    print("=== CHECKING FOREIGN KEY INTEGRITY ===")
    
    # Enable foreign key constraints
    cursor.execute('PRAGMA foreign_keys = ON;')
    
    # Check for violations
    try:
        cursor.execute('PRAGMA foreign_key_check;')
        violations = cursor.fetchall()
        if violations:
            print('Foreign key violations found:')
            for v in violations:
                print(f'  {v}')
        else:
            print('No foreign key violations found.')
    except Exception as e:
        print(f'Error checking foreign keys: {e}')
    
    # Check missing foreign key constraint
    print('\n=== FOREIGN KEY CONSTRAINTS CHECK ===')
    cursor.execute('PRAGMA foreign_key_list(checklist_ausfuehrungen);')
    fks = cursor.fetchall()
    print('Current foreign key constraints:')
    for fk in fks:
        print(f'  {fk}')
    
    # Verify the execution record integrity manually
    print('\n=== MANUAL INTEGRITY CHECK ===')
    cursor.execute('SELECT id, checkliste_id, fahrzeug_id, benutzer_id FROM checklist_ausfuehrungen;')
    executions = cursor.fetchall()
    
    for exec_id, checklist_id, vehicle_id, user_id in executions:
        print(f'Execution {exec_id}:')
        
        # Check if checklist exists
        cursor.execute('SELECT id, name FROM checklisten WHERE id = ?', (checklist_id,))
        checklist_result = cursor.fetchone()
        if checklist_result:
            print(f'  Checklist {checklist_id}: EXISTS ({checklist_result[1]})')
        else:
            print(f'  Checklist {checklist_id}: MISSING')
        
        # Check if vehicle exists  
        cursor.execute('SELECT id, kennzeichen FROM fahrzeuge WHERE id = ?', (vehicle_id,))
        vehicle_result = cursor.fetchone()
        if vehicle_result:
            print(f'  Vehicle {vehicle_id}: EXISTS ({vehicle_result[1]})')
        else:
            print(f'  Vehicle {vehicle_id}: MISSING')
        
        # Check if user exists
        cursor.execute('SELECT id, username FROM benutzer WHERE id = ?', (user_id,))
        user_result = cursor.fetchone()
        if user_result:
            print(f'  User {user_id}: EXISTS ({user_result[1]})')
        else:
            print(f'  User {user_id}: MISSING')
    
    conn.close()

if __name__ == "__main__":
    check_fk_integrity()