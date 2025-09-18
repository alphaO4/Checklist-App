"""
Manual Database Schema Update Script

This script manually updates the existing SQLite database to support enhanced checklist items.
Run this before using the enhanced checklist features.
"""

import sqlite3
from pathlib import Path


def update_database_schema(db_path: str = "data.db"):
    """Update database schema to support enhanced checklist items"""
    
    db_file = Path(__file__).parent / db_path
    
    if not db_file.exists():
        print(f"Database file not found: {db_file}")
        print("Please ensure the backend has been started at least once to create the database.")
        return False
    
    try:
        conn = sqlite3.connect(str(db_file))
        cursor = conn.cursor()
        
        print("Updating database schema for enhanced checklist items...")
        
        # Check if columns already exist
        cursor.execute("PRAGMA table_info(checklist_items)")
        existing_columns = [row[1] for row in cursor.fetchall()]
        
        # Add new columns to checklist_items table if they don't exist
        new_checklist_columns = [
            ("item_type", "TEXT DEFAULT 'standard'"),
            ("validation_config", "TEXT"),  # JSON stored as TEXT in SQLite
            ("editable_roles", "TEXT"),     # JSON stored as TEXT in SQLite  
            ("requires_tuv", "BOOLEAN DEFAULT 0"),
            ("subcategories", "TEXT")       # JSON stored as TEXT in SQLite
        ]
        
        for column_name, column_def in new_checklist_columns:
            if column_name not in existing_columns:
                print(f"Adding column: checklist_items.{column_name}")
                cursor.execute(f"ALTER TABLE checklist_items ADD COLUMN {column_name} {column_def}")
        
        # Check item_ergebnisse table
        cursor.execute("PRAGMA table_info(item_ergebnisse)")
        existing_result_columns = [row[1] for row in cursor.fetchall()]
        
        # Add new columns to item_ergebnisse table if they don't exist
        new_result_columns = [
            ("wert", "TEXT"),               # JSON stored as TEXT in SQLite
            ("vorhanden", "BOOLEAN"),
            ("tuv_datum", "DATETIME"),
            ("tuv_status", "TEXT"),
            ("menge", "INTEGER")
        ]
        
        for column_name, column_def in new_result_columns:
            if column_name not in existing_result_columns:
                print(f"Adding column: item_ergebnisse.{column_name}")
                cursor.execute(f"ALTER TABLE item_ergebnisse ADD COLUMN {column_name} {column_def}")
        
        # Update existing records with default values
        print("Setting default values for existing records...")
        
        cursor.execute("""
            UPDATE checklist_items 
            SET item_type = 'standard',
                editable_roles = '["organisator", "admin"]',
                requires_tuv = 0
            WHERE item_type IS NULL OR item_type = ''
        """)
        
        # Commit changes
        conn.commit()
        
        print("✅ Database schema updated successfully!")
        print("Enhanced checklist features are now available.")
        
        # Show summary
        cursor.execute("SELECT COUNT(*) FROM checklist_items")
        item_count = cursor.fetchone()[0]
        
        cursor.execute("SELECT COUNT(*) FROM item_ergebnisse") 
        result_count = cursor.fetchone()[0]
        
        print(f"📊 Database contains:")
        print(f"   - {item_count} checklist items")
        print(f"   - {result_count} item results")
        
        return True
        
    except sqlite3.Error as e:
        print(f"❌ Database error: {e}")
        return False
    except Exception as e:
        print(f"❌ Unexpected error: {e}")
        return False
    finally:
        if conn:
            conn.close()


def verify_schema_update(db_path: str = "data.db"):
    """Verify that the schema update was successful"""
    
    db_file = Path(__file__).parent / db_path
    
    try:
        conn = sqlite3.connect(str(db_file))
        cursor = conn.cursor()
        
        print("\\n🔍 Verifying schema update...")
        
        # Check checklist_items columns
        cursor.execute("PRAGMA table_info(checklist_items)")
        checklist_columns = [row[1] for row in cursor.fetchall()]
        
        required_checklist_columns = [
            'item_type', 'validation_config', 'editable_roles', 
            'requires_tuv', 'subcategories'
        ]
        
        missing_checklist_columns = [col for col in required_checklist_columns 
                                   if col not in checklist_columns]
        
        if missing_checklist_columns:
            print(f"❌ Missing checklist_items columns: {missing_checklist_columns}")
            return False
        
        # Check item_ergebnisse columns
        cursor.execute("PRAGMA table_info(item_ergebnisse)")
        result_columns = [row[1] for row in cursor.fetchall()]
        
        required_result_columns = [
            'wert', 'vorhanden', 'tuv_datum', 'tuv_status', 'menge'
        ]
        
        missing_result_columns = [col for col in required_result_columns 
                                if col not in result_columns]
        
        if missing_result_columns:
            print(f"❌ Missing item_ergebnisse columns: {missing_result_columns}")
            return False
        
        print("✅ All required columns are present!")
        
        # Test data insertion
        test_validation_config = '{"min_value": 1, "max_value": 6}'
        test_editable_roles = '["organisator", "admin"]'
        
        cursor.execute("""
            INSERT INTO checklist_items 
            (checkliste_id, beschreibung, item_type, validation_config, editable_roles, requires_tuv, pflicht, reihenfolge)
            VALUES (1, 'Test Enhanced Item', 'rating_1_6', ?, ?, 0, 1, 999)
        """, (test_validation_config, test_editable_roles))
        
        test_item_id = cursor.lastrowid
        
        # Clean up test data
        cursor.execute("DELETE FROM checklist_items WHERE id = ?", (test_item_id,))
        
        conn.commit()
        
        print("✅ Schema verification successful - enhanced features ready!")
        return True
        
    except sqlite3.Error as e:
        print(f"❌ Schema verification failed: {e}")
        return False
    except Exception as e:
        print(f"❌ Verification error: {e}")
        return False
    finally:
        if conn:
            conn.close()


def main():
    """Main function to update database schema"""
    print("🔧 Enhanced Checklist Database Schema Updater")
    print("=" * 50)
    
    # Update schema
    if update_database_schema():
        # Verify update
        if verify_schema_update():
            print("\\n🎉 Database is ready for enhanced checklist features!")
            print("\\nYou can now:")
            print("  - Create checklists with different item types")
            print("  - Set validation rules for ratings and percentages")  
            print("  - Configure role-based editing permissions")
            print("  - Track complex TÜV data for Atemschutz equipment")
        else:
            print("\\n⚠️  Schema update completed but verification failed.")
            print("Please check the database manually.")
    else:
        print("\\n❌ Schema update failed. Please check error messages above.")


if __name__ == "__main__":
    main()