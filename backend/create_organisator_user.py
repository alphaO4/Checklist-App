#!/usr/bin/env python3
"""
Create Organisator User Script

This script creates an organisator user directly in the database for testing.
"""

import sqlite3
from pathlib import Path
import bcrypt
from datetime import datetime


def hash_password(password: str) -> str:
    """Hash a password using bcrypt"""
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')


def create_organisator_user(db_path: str = "data.db"):
    """Create an organisator user in the database"""
    
    db_file = Path(__file__).parent / db_path
    
    if not db_file.exists():
        print(f"❌ Database file not found: {db_file}")
        return False
    
    try:
        conn = sqlite3.connect(str(db_file))
        cursor = conn.cursor()
        
        # First, remove any existing organisator users
        cursor.execute("SELECT id, username FROM benutzer WHERE rolle = 'organisator'")
        existing_organisators = cursor.fetchall()
        
        if existing_organisators:
            print(f"🗑️  Removing {len(existing_organisators)} existing organisator user(s):")
            for user_id, username in existing_organisators:
                print(f"  - Removing: ID {user_id}, Username: {username}")
                cursor.execute("DELETE FROM benutzer WHERE id = ?", (user_id,))
        
        # Create new organisator user
        username = "test_organisator"
        email = "organisator@test.com"
        password = "test1234"
        rolle = "organisator"
        
        # Remove any existing user with the same username (in case they had different role)
        cursor.execute("SELECT id FROM benutzer WHERE username = ?", (username,))
        existing_user = cursor.fetchone()
        
        if existing_user:
            print(f"🗑️  Removing existing user with username: {username}")
            cursor.execute("DELETE FROM benutzer WHERE username = ?", (username,))
        
        print(f"👤 Creating new organisator user: {username}")
        
        password_hash = hash_password(password)
        now = datetime.now()
        
        cursor.execute("""
            INSERT INTO benutzer (username, email, password_hash, rolle, created_at)
            VALUES (?, ?, ?, ?, ?)
        """, (username, email, password_hash, rolle, now))
        
        # Also ensure we have a gruppenleiter user for testing restrictions
        gruppenleiter_username = "test_gruppenleiter"
        cursor.execute("SELECT id FROM benutzer WHERE username = ?", (gruppenleiter_username,))
        if not cursor.fetchone():
            print(f"👤 Creating gruppenleiter user: {gruppenleiter_username}")
            password_hash = hash_password("test1234")  # Same password for consistency
            cursor.execute("""
                INSERT INTO benutzer (username, email, password_hash, rolle, created_at)
            VALUES (?, ?, ?, ?, ?)
            """, (gruppenleiter_username, "gruppenleiter@test.com", password_hash, "gruppenleiter", now))
        
        conn.commit()
        
        # Verify users were created
        cursor.execute("SELECT username, rolle FROM benutzer WHERE rolle IN ('organisator', 'gruppenleiter')")
        test_users = cursor.fetchall()
        
        print("✅ Test users created/verified:")
        for username, role in test_users:
            print(f"  - {username}: {role}")
        
        print(f"\n🔑 Login credentials:")
        print(f"  - Organisator: username='test_organisator', password='test1234'")
        print(f"  - Gruppenleiter: username='test_gruppenleiter', password='test1234'")
        
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


def list_all_users(db_path: str = "data.db"):
    """List all users in the database"""
    db_file = Path(__file__).parent / db_path
    
    try:
        conn = sqlite3.connect(str(db_file))
        cursor = conn.cursor()
        
        cursor.execute("SELECT id, username, rolle, email FROM benutzer")
        users = cursor.fetchall()
        
        print("\n👥 All users in database:")
        print("-" * 50)
        for user_id, username, rolle, email in users:
            print(f"ID: {user_id:2d} | {username:15s} | {rolle:12s} | {email}")
        
        return True
        
    except sqlite3.Error as e:
        print(f"❌ Database error: {e}")
        return False
    finally:
        if conn:
            conn.close()


def main():
    """Main function"""
    print("🔧 Organisator User Creation Script")
    print("=" * 40)
    
    # List existing users
    list_all_users()
    
    # Create organisator user
    if create_organisator_user():
        print("\n🎉 Success! Fresh organisator user created with password 'test1234'.")
        print("\nNext steps:")
        print("1. Start the backend: python -m uvicorn app.main:app --reload")
        print("2. Run tests: python test_template_creation.py")
    else:
        print("\n❌ Failed to create organisator user.")


if __name__ == "__main__":
    main()