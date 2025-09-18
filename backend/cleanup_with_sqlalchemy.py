#!/usr/bin/env python3
"""
Clean up test templates using FastAPI backend database session
"""

import sys
import os

# Add the app directory to the Python path
sys.path.append(os.path.join(os.path.dirname(__file__), 'app'))

from app.db.session import SessionLocal
from app.models.checklist import Checkliste, ChecklistItem, ChecklistAusfuehrung, ItemErgebnis

def cleanup_test_templates():
    """Clean up test templates using SQLAlchemy"""
    
    db = SessionLocal()
    
    try:
        # Get the problematic template IDs
        problematic_ids = [9, 11, 15, 17, 19, 21, 23, 25]
        
        print("🔍 Investigating test templates in database...")
        
        for template_id in problematic_ids:
            template = db.query(Checkliste).filter(Checkliste.id == template_id).first()
            
            if template:
                print(f"\n📋 Found template ID {template_id}: '{template.name}'")
                
                # Count dependent records
                items_count = db.query(ChecklistItem).filter(ChecklistItem.checkliste_id == template_id).count()
                executions_count = db.query(ChecklistAusfuehrung).filter(ChecklistAusfuehrung.checkliste_id == template_id).count()
                
                print(f"  📊 Dependent records: {items_count} items, {executions_count} executions")
                
                # Delete dependent records first
                if executions_count > 0:
                    # Delete item results for executions of this checklist
                    execution_ids = [ex.id for ex in db.query(ChecklistAusfuehrung).filter(ChecklistAusfuehrung.checkliste_id == template_id).all()]
                    for exec_id in execution_ids:
                        results_deleted = db.query(ItemErgebnis).filter(ItemErgebnis.ausfuehrung_id == exec_id).delete()
                        print(f"    🗑️ Deleted {results_deleted} item results for execution {exec_id}")
                    
                    # Delete executions
                    executions_deleted = db.query(ChecklistAusfuehrung).filter(ChecklistAusfuehrung.checkliste_id == template_id).delete()
                    print(f"    🗑️ Deleted {executions_deleted} executions")
                
                if items_count > 0:
                    # Delete checklist items
                    items_deleted = db.query(ChecklistItem).filter(ChecklistItem.checkliste_id == template_id).delete()
                    print(f"    🗑️ Deleted {items_deleted} checklist items")
                
                # Finally delete the template itself
                db.delete(template)
                print(f"    ✅ Deleted template '{template.name}'")
                
            else:
                print(f"\n❌ Template ID {template_id} not found in database")
        
        # Commit all changes
        db.commit()
        print("\n✅ All test templates cleaned up successfully!")
        
        # Verify the cleanup
        remaining_test_templates = db.query(Checkliste).filter(Checkliste.name.like('%Test Template%')).count()
        total_templates = db.query(Checkliste).count()
        
        print(f"\n📊 Cleanup results:")
        print(f"   Remaining test templates: {remaining_test_templates}")
        print(f"   Total templates: {total_templates}")
        
        if remaining_test_templates == 0:
            print("🎉 All test templates successfully removed!")
        
    except Exception as e:
        print(f"❌ Error during cleanup: {e}")
        db.rollback()
        raise
    finally:
        db.close()

if __name__ == "__main__":
    cleanup_test_templates()