"""
Alembic Migration: Add enhanced checklist item types

This migration adds support for enhanced checklist items with:
- Different item types (rating, percentage, atemschutz, etc.)
- Validation configurations
- Role-based editing permissions
- TÜV tracking enhancements
"""

from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import sqlite


# revision identifiers, used by Alembic
revision = 'add_enhanced_checklist_items'
down_revision = None  # Replace with previous revision if using Alembic
branch_labels = None
depends_on = None


def upgrade():
    """Add enhanced checklist item columns"""
    
    # Add new columns to checklist_items table
    op.add_column('checklist_items', 
        sa.Column('item_type', sa.String(50), nullable=True, default='standard'))
    op.add_column('checklist_items',
        sa.Column('validation_config', sa.JSON(), nullable=True))
    op.add_column('checklist_items',
        sa.Column('editable_roles', sa.JSON(), nullable=True))
    op.add_column('checklist_items',
        sa.Column('requires_tuv', sa.Boolean(), nullable=False, default=False))
    op.add_column('checklist_items',
        sa.Column('subcategories', sa.JSON(), nullable=True))
    
    # Add new columns to item_ergebnisse table
    op.add_column('item_ergebnisse',
        sa.Column('wert', sa.JSON(), nullable=True))
    op.add_column('item_ergebnisse',
        sa.Column('vorhanden', sa.Boolean(), nullable=True))
    op.add_column('item_ergebnisse',
        sa.Column('tuv_datum', sa.DateTime(), nullable=True))
    op.add_column('item_ergebnisse',
        sa.Column('tuv_status', sa.String(50), nullable=True))
    op.add_column('item_ergebnisse',
        sa.Column('menge', sa.Integer(), nullable=True))
    
    # Set default values for existing records
    op.execute("""
        UPDATE checklist_items 
        SET item_type = 'standard',
            editable_roles = '["organisator", "admin"]',
            requires_tuv = 0
        WHERE item_type IS NULL
    """)


def downgrade():
    """Remove enhanced checklist item columns"""
    
    # Remove columns from item_ergebnisse table
    op.drop_column('item_ergebnisse', 'menge')
    op.drop_column('item_ergebnisse', 'tuv_status')
    op.drop_column('item_ergebnisse', 'tuv_datum')
    op.drop_column('item_ergebnisse', 'vorhanden')
    op.drop_column('item_ergebnisse', 'wert')
    
    # Remove columns from checklist_items table
    op.drop_column('checklist_items', 'subcategories')
    op.drop_column('checklist_items', 'requires_tuv')
    op.drop_column('checklist_items', 'editable_roles')
    op.drop_column('checklist_items', 'validation_config')
    op.drop_column('checklist_items', 'item_type')