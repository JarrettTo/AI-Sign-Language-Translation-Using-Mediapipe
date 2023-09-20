import atexit
from flask import Flask, render_template, url_for, request, redirect, jsonify
from flask_sqlalchemy import SQLAlchemy
from datetime import datetime
from pose_estimation_function import pose_est
from keras.backend import clear_session
from user_db import UserDatabase
model=pose_est()
db = UserDatabase("110.238.107.235", "3306", "root", "Keepcalm-75", "users")
db.connect()


app = Flask(__name__)
#app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///test.db'
#db = SQLAlchemy(app)
@app.route('/', methods=['POST', 'GET'])
def index():
    return "Hello World"
@app.route('/pose', methods=['POST'])
def pose(): 
    clear_session()
    
    try:
        json_data = request.get_json()
        # Assuming the JSON data contains a 2D array
        # You can process the array as needed
     

        res= model.detect_action_api(json_data["data"])
        print(res)
        return jsonify(res)
    except Exception as e:
        # Handle the exception and print the error message
        print(f"An error occurred: {e}")
        print("ERROR")
        return "error"
@app.route('/signup', methods=['POST'])
def signup():
    data = request.get_json()
    username = data.get('username')
    password = data.get('password')
    
    if not username or not password:
        return jsonify({'message': 'Username and password are required'}), 400

    result = db.signup(username, password)
    if result:
        return jsonify({'message': 'New user created successfully'})
    else:
        return jsonify({'message': 'Failed to create user'}), 400

@app.route('/login', methods=['POST'])
def login():
    data = request.get_json()
    username = data.get('username')
    password = data.get('password')
    
    if not username or not password:
        return jsonify({'message': 'Username and password are required'}), 400

    if db.login(username, password):
        return jsonify({'message': 'Login successful'})
    else:
        return jsonify({'message': 'Login failed'}), 401

@app.route('/history', methods=['GET'])
def retrieve_history():
    username = request.args.get('username')
    num_entries = int(request.args.get('num_entries', 10))  # Default to 10 entries if not provided
    
    if not username:
        return jsonify({'message': 'Username is required'}), 400

    history_entries = db.retrieve_history(username, num_entries)
    return jsonify({'history': history_entries})

@app.route('/add_entry', methods=['POST'])
def add_history_entry():
    data = request.get_json()
    username = data.get('username')
    query = data.get('query')
    
    if not username or not query:
        return jsonify({'message': 'Username and query are required'}), 400

    db.add_history_entry(username, query)
    return jsonify({'message': 'History entry added successfully'})

if __name__ == "__main__":
    app.run(debug=True) 

atexit.register(db.close)