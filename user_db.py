from db import Database

import uuid
import hashlib
from datetime import datetime

class UserDatabase(Database):
    def __init__(self, host, port, username, password, database):
        super().__init__(host, port, username, password, database)

    def signup(self, username, password):
        if not self.connection or not self.connection.is_connected():
            print("Not connected to the database. Call 'connect()' first.")
            return False
        
        if self.is_username_taken(username):
            print("Username '{}' is already taken.".format(username))
            return False

        salt = self.generate_salt(username)
        hashed_password = self.hash_password(password, salt)

        insert_query = "INSERT INTO user (username, salt, hashed_password) VALUES (%s, %s, %s)"
        user_data = (username, salt, hashed_password)
        
        self.execute_query(insert_query, user_data)
        print("New user created successfully")
        return True

    def login(self, username, password):
        if not self.connection or not self.connection.is_connected():
            print("Not connected to the database. Call 'connect()' first.")
            return False

        select_query = "SELECT hashed_password, salt FROM user WHERE username = %s"
        cursor = self.execute_query(select_query, (username,))
        result = cursor.fetchone()
        print(result)
        if result:
            hashed_password, salt = result
            entered_password = self.hash_password(password, salt)
            if entered_password == hashed_password:
                print("Login successful")
                return True
            else:
                print("Incorrect password")
        else:
            print("Username '{}' not found".format(username))

        return False

    def is_username_taken(self, username):
        cursor = self.execute_query("SELECT COUNT(*) FROM user WHERE username = %s", (username,))
        if cursor:
            result = cursor.fetchone()
            count=0
            if result:  
                count = result[0]  
            cursor.close()  
            return count > 0  
        return False 

    def generate_salt(self, username):
        return hashlib.sha256(str(username).encode()).hexdigest()

    def hash_password(self, password, salt):
        hashed_password = hashlib.sha256((password + salt).encode()).hexdigest()
        return hashed_password
    
    def add_history_entry(self, username, query):
        if not self.connection or not self.connection.is_connected():
            print("Not connected to the database. Call 'connect()' first.")
            return

        translation_id = str(uuid.uuid4())
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")

        insert_query = "INSERT INTO history (translation_id, username, query, timestamp) VALUES (%s, %s, %s, %s)"
        history_data = (translation_id, username, query, timestamp)

        self.execute_query(insert_query, history_data)
        print("History entry added successfully")

    def retrieve_history(self, username, num_entries):
        if not self.connection or not self.connection.is_connected():
            print("Not connected to the database. Call 'connect()' first.")
            return []

        select_query = """
            SELECT query, timestamp
            FROM history
            WHERE username = %s
            ORDER BY timestamp DESC
            LIMIT %s
        """

        cursor = self.execute_query(select_query, (username, num_entries))
        if cursor:
            history_entries = cursor.fetchall()
            return history_entries
        else:
            return []

# # Usage example:
# db = UserDatabase("110.238.107.235", "3306", "root", "Keepcalm-75", "users")
# db.connect()

# # Create a new user with a unique user_id and hashed password
# db.signup("example_user", "user_password")

# # Login with username and password
# db.login("example_user", "user_password")

# # Add history entries (call this as needed)
# db.add_history_entry("example_user", "Translation query 1")
# db.add_history_entry("example_user", "Translation query 2")
# db.add_history_entry("example_user", "Translation query 3")

# # Retrieve the latest 2 history entries for a specific username
# history_entries = db.retrieve_history("example_user", 2)
# for entry in history_entries:
#     print("Query:", entry[0])
#     print("Timestamp:", entry[1])

# db.disconnect()
