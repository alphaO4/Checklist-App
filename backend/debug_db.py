#!/usr/bin/env python3

import sqlite3

def check_database():
    conn = sqlite3.connect('data.db')
    cursor = conn.cursor()
    
    print("=== DATABASE TABLES ===")
    cursor.execute("SELECT name FROM sqlite_master WHERE type='table';")
    tables = cursor.fetchall()
    for table in tables:
        print(f"  {table[0]}")
    
    print("\n=== FAHRZEUGGRUPPEN ===")
    cursor.execute("SELECT id, name FROM fahrzeuggruppen ORDER BY id;")
    for row in cursor.fetchall():
        print(f"  Group {row[0]}: {row[1]}")
    
    print("\n=== FAHRZEUGE ===")
    cursor.execute("SELECT id, kennzeichen, fahrzeuggruppe_id FROM fahrzeuge ORDER BY id;")
    for row in cursor.fetchall():
        print(f"  Vehicle {row[0]}: {row[1]} (Group: {row[2]})")
    
    print("\n=== CHECKLISTEN ===")
    cursor.execute("SELECT id, name, fahrzeuggruppe_id, template FROM checklisten ORDER BY id;")
    for row in cursor.fetchall():
        print(f"  Checklist {row[0]}: {row[1]} (Group: {row[2]}, Template: {row[3]})")
    
    # Check if checklist execution table exists
    print("\n=== EXECUTION TABLES ===")
    for table_name in ['checklist_ausfuehrung', 'checklist_executions']:
        try:
            cursor.execute(f"SELECT COUNT(*) FROM {table_name};")
            count = cursor.fetchone()[0]
            print(f"  {table_name}: {count} records")
            
            # Show schema
            cursor.execute(f"PRAGMA table_info({table_name});")
            columns = cursor.fetchall()
            for col in columns:
                print(f"    {col}")
                
        except sqlite3.OperationalError as e:
            print(f"  {table_name}: {e}")
    
    conn.close()

if __name__ == "__main__":
    check_database()