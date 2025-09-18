#!/usr/bin/env python3
"""
Direct database cleanup for problematic templates
"""

import sqlite3
import os

def cleanup_orphaned_templates():
    """Clean up templates that are causing 500 errors"""
    
    # Connect to the database
    db_path = os.path.join(os.path.dirname(__file__), 'data.db')
    
    if not os.path.exists(db_path):
        print("❌ Database file not found!")
        return
        
    print(f"📍 Connecting to database: {db_path}")
    try:
        conn = sqlite3.connect(db_path, timeout=30.0)
        cursor = conn.cursor()
        print("✅ Database connection successful")
    except Exception as e:
        print(f"❌ Failed to connect to database: {e}")
        return
    
    try:
        # First, let's see what these problematic templates look like
        problematic_ids = [9, 11, 15, 17, 19, 21, 23, 25]
        
        print("🔍 Investigating problematic templates...")
        for template_id in problematic_ids:
            cursor.execute("SELECT id, name, created_by FROM checklisten WHERE id = ?", (template_id,))
            result = cursor.fetchone()
            if result:
                print(f"  Template ID {template_id}: name='{result[1]}', created_by={result[2]}")
            else:
                print(f"  Template ID {template_id}: NOT FOUND in database")
        
        # Check for dependent records that might prevent deletion
        print("\n🔍 Checking for dependent records...")
        for template_id in problematic_ids:
            # Check checklist items
            cursor.execute("SELECT COUNT(*) FROM checklist_items WHERE checklist_id = ?", (template_id,))
            item_count = cursor.fetchone()[0]
            
            # Check executions
            cursor.execute("SELECT COUNT(*) FROM checklist_ausfuehrungen WHERE checklist_id = ?", (template_id,))
            execution_count = cursor.fetchone()[0]
            
            if item_count > 0 or execution_count > 0:
                print(f"  Template ID {template_id}: {item_count} items, {execution_count} executions")
        
        # Now try to clean them up properly
        print("\n🧹 Attempting database cleanup...")
        for template_id in problematic_ids:
            try:
                # Delete dependent records first
                cursor.execute("DELETE FROM item_ergebnisse WHERE checklist_ausfuehrung_id IN (SELECT id FROM checklist_ausfuehrungen WHERE checklist_id = ?)", (template_id,))
                deleted_results = cursor.rowcount
                
                cursor.execute("DELETE FROM checklist_ausfuehrungen WHERE checklist_id = ?", (template_id,))
                deleted_executions = cursor.rowcount
                
                cursor.execute("DELETE FROM checklist_items WHERE checklist_id = ?", (template_id,))
                deleted_items = cursor.rowcount
                
                cursor.execute("DELETE FROM checklisten WHERE id = ?", (template_id,))
                deleted_templates = cursor.rowcount
                
                if deleted_templates > 0:
                    print(f"  ✅ Deleted template ID {template_id} (removed {deleted_items} items, {deleted_executions} executions, {deleted_results} results)")
                else:
                    print(f"  ❌ Template ID {template_id} not found")
                    
            except Exception as e:
                print(f"  ❌ Error deleting template ID {template_id}: {e}")
        
        # Commit changes
        conn.commit()
        print("\n✅ Database cleanup completed!")
        
        # Verify cleanup
        print("\n🔍 Verifying cleanup...")
        cursor.execute("SELECT COUNT(*) FROM checklisten WHERE name LIKE '%Test Template%'")
        remaining_test_templates = cursor.fetchone()[0]
        print(f"Remaining test templates: {remaining_test_templates}")
        
        cursor.execute("SELECT COUNT(*) FROM checklisten")
        total_templates = cursor.fetchone()[0]
        print(f"Total templates remaining: {total_templates}")
        
    except Exception as e:
        print(f"❌ Database error: {e}")
        conn.rollback()
    finally:
        conn.close()

if __name__ == "__main__":
    cleanup_orphaned_templates()