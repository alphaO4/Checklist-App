#!/usr/bin/env python3

import sqlite3

def comprehensive_fk_check():
    conn = sqlite3.connect('data.db')
    cursor = conn.cursor()
    
    # Enable foreign key constraints to catch violations
    cursor.execute('PRAGMA foreign_keys = ON;')
    
    print("=== COMPREHENSIVE FOREIGN KEY CHECK ===\n")
    
    # Check all tables with foreign keys
    tables_with_fks = [
        'fahrzeuge', 'checklisten', 'checklist_ausfuehrungen', 
        'item_ergebnisse', 'checklist_items', 'tuv_termine'
    ]
    
    for table in tables_with_fks:
        print(f"--- {table.upper()} ---")
        
        try:
            # Get foreign key constraints
            cursor.execute(f'PRAGMA foreign_key_list({table});')
            fks = cursor.fetchall()
            
            if fks:
                print("Foreign key constraints:")
                for fk in fks:
                    print(f"  {fk[3]} -> {fk[2]}.{fk[4]}")
                
                # Check for violations in this table
                cursor.execute(f'PRAGMA foreign_key_check({table});')
                violations = cursor.fetchall()
                
                if violations:
                    print("❌ VIOLATIONS FOUND:")
                    for v in violations:
                        print(f"  {v}")
                else:
                    print("✅ No violations")
            else:
                print("No foreign key constraints")
                
        except Exception as e:
            print(f"Error checking {table}: {e}")
        
        print()
    
    # Check for common FK error patterns
    print("=== CHECKING COMMON FK ERROR PATTERNS ===\n")
    
    # Check for NULL foreign key values where they shouldn't be
    print("1. Checking for NULL foreign keys:")
    
    # fahrzeuge table
    cursor.execute('SELECT COUNT(*) FROM fahrzeuge WHERE fahrzeuggruppe_id IS NULL OR fahrzeugtyp_id IS NULL;')
    null_vehicle_fks = cursor.fetchone()[0]
    if null_vehicle_fks > 0:
        print(f"❌ {null_vehicle_fks} vehicles with NULL foreign keys")
    else:
        print("✅ All vehicles have valid foreign keys")
    
    # checklisten table  
    cursor.execute('SELECT COUNT(*) FROM checklisten WHERE fahrzeuggruppe_id IS NULL;')
    null_checklist_fks = cursor.fetchone()[0]
    if null_checklist_fks > 0:
        print(f"❌ {null_checklist_fks} checklists with NULL fahrzeuggruppe_id")
    else:
        print("✅ All checklists have valid fahrzeuggruppe_id")
    
    # checklist_ausfuehrungen table
    cursor.execute('''
        SELECT COUNT(*) FROM checklist_ausfuehrungen 
        WHERE checkliste_id IS NULL OR fahrzeug_id IS NULL OR benutzer_id IS NULL;
    ''')
    null_execution_fks = cursor.fetchone()[0]
    if null_execution_fks > 0:
        print(f"❌ {null_execution_fks} executions with NULL foreign keys")
    else:
        print("✅ All executions have valid foreign keys")
    
    print("\n2. Checking for dangling references:")
    
    # Check for references to non-existent records
    cursor.execute('''
        SELECT id, fahrzeuggruppe_id FROM fahrzeuge 
        WHERE fahrzeuggruppe_id NOT IN (SELECT id FROM fahrzeuggruppen);
    ''')
    dangling_vehicles = cursor.fetchall()
    if dangling_vehicles:
        print(f"❌ Vehicles with invalid fahrzeuggruppe_id: {dangling_vehicles}")
    
    cursor.execute('''
        SELECT id, fahrzeugtyp_id FROM fahrzeuge 
        WHERE fahrzeugtyp_id NOT IN (SELECT id FROM fahrzeugtypen);
    ''')
    dangling_vehicle_types = cursor.fetchall()
    if dangling_vehicle_types:
        print(f"❌ Vehicles with invalid fahrzeugtyp_id: {dangling_vehicle_types}")
    
    cursor.execute('''
        SELECT id, checkliste_id FROM checklist_ausfuehrungen 
        WHERE checkliste_id NOT IN (SELECT id FROM checklisten);
    ''')
    dangling_exec_checklists = cursor.fetchall()
    if dangling_exec_checklists:
        print(f"❌ Executions with invalid checkliste_id: {dangling_exec_checklists}")
    
    cursor.execute('''
        SELECT id, fahrzeug_id FROM checklist_ausfuehrungen 
        WHERE fahrzeug_id NOT IN (SELECT id FROM fahrzeuge);
    ''')
    dangling_exec_vehicles = cursor.fetchall()
    if dangling_exec_vehicles:
        print(f"❌ Executions with invalid fahrzeug_id: {dangling_exec_vehicles}")
    
    if not (dangling_vehicles or dangling_vehicle_types or dangling_exec_checklists or dangling_exec_vehicles):
        print("✅ No dangling references found")
    
    print("\n=== SUMMARY ===")
    print("If no errors are shown above, the FK error might be:")
    print("1. Occurring during INSERT/UPDATE operations")
    print("2. Related to transaction rollbacks")
    print("3. Happening in a specific code path")
    print("4. Related to SQLAlchemy session management")
    
    conn.close()

if __name__ == "__main__":
    comprehensive_fk_check()