from flask import Flask, render_template, url_for, request, redirect, jsonify
from flask_sqlalchemy import SQLAlchemy
from datetime import datetime
from pose_estimation_function import pose_est
from keras.backend import clear_session

model=pose_est()

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
if __name__ == "__main__":
    app.run(debug=True)