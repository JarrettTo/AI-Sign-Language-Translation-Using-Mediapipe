from flask import Flask, render_template, url_for, request, redirect, jsonify
from flask_sqlalchemy import SQLAlchemy
from datetime import datetime
from pose_est import pose_est
app = Flask(__name__)
#app.config['SQLALCHEMY_DATABASE_URI'] = 'sqlite:///test.db'
#db = SQLAlchemy(app)
@app.route('/', methods=['POST', 'GET'])
def index():
    return "Hello World"
@app.route('/pose')
def pose():
    model=pose_est()
    try:
        res= model.detect_action()
        print(res)
        return jsonify(res)
    except Exception as e:
        # Handle the exception and print the error message
        print(f"An error occurred: {e}")
        print("ERROR")
        return "error"
if __name__ == "__main__":
    app.run(debug=True)