import mysql.connector

class Database:
    def __init__(self, host, port, username, password, database):
        self.host = host
        self.port = port
        self.username = username
        self.password = password
        self.database = database
        self.connection = None

    def connect(self):
        try:
            self.connection = mysql.connector.connect(
                host=self.host,
                port=self.port,
                user=self.username,
                password=self.password,
                database=self.database
            )
            if self.connection.is_connected():
                print("Connected to MySQL database")
        except mysql.connector.Error as err:
            print("Error: {}".format(err))

    def disconnect(self):
        if self.connection and self.connection.is_connected():
            self.connection.close()
            print("MySQL database connection closed")

    def execute_query(self, query, data=None):
        if not self.connection or not self.connection.is_connected():
            print("Not connected to the database. Call 'connect()' first.")
            return None

        cursor = self.connection.cursor(buffered=True)
        try:
            if data:
                cursor.execute(query, data)
            else:
                cursor.execute(query)
            self.connection.commit()
            return cursor
        except mysql.connector.Error as err:
            print("Error: {}".format(err))
            return None
        finally:
            cursor.close()

# db = Database("110.238.107.235", "3306", "root", "Keepcalm-75", "users")
# db.connect()