#!/usr/bin/env python3

import sqlite3

def fix_foreign_key_violations():
    conn = sqlite3.connect('data.db')
    cursor = conn.cursor()
    
    print("=== FIXING FOREIGN KEY VIOLATIONS ===")
    
    # Find checklists with invalid ersteller_id
    print("Checking for invalid ersteller_id references...")
    cursor.execute('''
        SELECT id, name, ersteller_id 
        FROM checklisten 
        WHERE ersteller_id = 0 OR ersteller_id NOT IN (SELECT id FROM benutzer)
    ''')
    invalid_checklists = cursor.fetchall()
    
    if invalid_checklists:
        print("Found checklists with invalid ersteller_id:")
        for cl_id, cl_name, ersteller_id in invalid_checklists:
            print(f"  Checklist {cl_id}: '{cl_name}' (ersteller_id: {ersteller_id})")
        
        # Get the first available user (admin)
        cursor.execute("SELECT id FROM benutzer ORDER BY id LIMIT 1")
        admin_user = cursor.fetchone()
        
        if admin_user:
            admin_id = admin_user[0]
            print(f"Fixing by setting ersteller_id to {admin_id} (admin user)...")
            
            # Update all invalid checklists to reference the admin user
            cursor.execute('''
                UPDATE checklisten 
                SET ersteller_id = ? 
                WHERE ersteller_id = 0 OR ersteller_id NOT IN (SELECT id FROM benutzer)
            ''', (admin_id,))
            
            print(f"Updated {cursor.rowcount} checklists")
            conn.commit()
        else:
            print("ERROR: No users found in database!")
    else:
        print("No checklists with invalid ersteller_id found.")
    
    # Verify the fix
    print("\n=== VERIFICATION ===")
    cursor.execute('PRAGMA foreign_key_check;')
    remaining_violations = cursor.fetchall()
    
    if remaining_violations:
        print("Remaining foreign key violations:")
        for v in remaining_violations:
            print(f"  {v}")
    else:
        print("✅ All foreign key violations fixed!")
    
    conn.close()

if __name__ == "__main__":
    fix_foreign_key_violations()