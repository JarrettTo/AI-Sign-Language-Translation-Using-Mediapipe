import cv2
import os
import mediapipe as mp
import numpy as np
from matplotlib import pyplot as plt
import time
from sklearn.model_selection import train_test_split
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import LSTM, Dense
from tensorflow.keras.callbacks import TensorBoard
from sklearn.metrics import multilabel_confusion_matrix, accuracy_score
class pose_est:
    name="null"
    mp_drawing = mp.solutions.drawing_utils
    mp_pose = mp.solutions.pose
    mp_hands = mp.solutions.hands
    mp_holistic = mp.solutions.holistic
    hands = mp_hands.Hands()
    DATA_PATH = os.path.join('MP_Data') 
    model = Sequential()
    colors = [(245,117,16), (117,245,16), (16,117,245)]
    # Actions that we try to detect
    actions = np.array(['hello', 'thanks', 'iloveyou'])

    # Thirty videos worth of data
    no_sequences = 30

    # Videos are going to be 30 frames in length
    sequence_length = 30

    # Folder start
    start_folder = 30
    def __init__(self):
        self=self
        
        self.model.add(LSTM(64,name="layer1", return_sequences=True, activation='relu', input_shape=(30,132)))
        self.model.add(LSTM(128, name="layer2",return_sequences=True, activation='relu'))
        self.model.add(LSTM(64, name="layer3",return_sequences=False, activation='relu'))
        self.model.add(Dense(64, name="layer4",activation='relu'))
        self.model.add(Dense(32, name="layer5",activation='relu'))
        self.model.add(Dense(self.actions.shape[0], name="final",activation='softmax'))
        self.load_model()
    def mediapipe_detection(self, image, model):
        image = cv2.cvtColor(image,cv2.COLOR_BGR2RGB)
        image.flags.writeable = False

        results=model.process(image)
        
        image.flags.writeable = True
        image = cv2.cvtColor(image, cv2.COLOR_RGB2BGR)
        return image, results
    def draw_landmarks(self, image, results):
        self.mp_drawing.draw_landmarks(image, results.face_landmarks, self.mp_holistic.FACEMESH_TESSELATION) # Draw face connections
        self.mp_drawing.draw_landmarks(image, results.pose_landmarks, self.mp_holistic.POSE_CONNECTIONS) # Draw pose connections
        self.mp_drawing.draw_landmarks(image, results.left_hand_landmarks, self.mp_holistic.HAND_CONNECTIONS) # Draw left hand connections
        self.mp_drawing.draw_landmarks(image, results.right_hand_landmarks, self.mp_holistic.HAND_CONNECTIONS) # Draw right hand connections
    def extract_keypoints(self, results):
        pose = np.array([[res.x, res.y, res.z, res.visibility] for res in results.pose_landmarks.landmark]).flatten() if results.pose_landmarks else np.zeros(33*4)
        face = np.array([[res.x, res.y, res.z] for res in results.face_landmarks.landmark]).flatten() if results.face_landmarks else np.zeros(468*3)
        lh = np.array([[res.x, res.y, res.z] for res in results.left_hand_landmarks.landmark]).flatten() if results.left_hand_landmarks else np.zeros(21*3)
        rh = np.array([[res.x, res.y, res.z] for res in results.right_hand_landmarks.landmark]).flatten() if results.right_hand_landmarks else np.zeros(21*3)
        return np.concatenate([pose, face, lh, rh])

    def load_model(self):
        self.model.load_weights('action.h5')

    def prob_viz(self,res, actions, input_frame, colors):
        output_frame = input_frame.copy()
        for num, prob in enumerate(res):
            cv2.rectangle(output_frame, (0,60+num*40), (int(prob*100), 90+num*40), self.colors[num], -1)
            cv2.putText(output_frame, actions[num], (0, 85+num*40), cv2.FONT_HERSHEY_SIMPLEX, 1, (255,255,255), 2, cv2.LINE_AA)
            
        return output_frame

    def detect_action_api(self, points):
        actions = ["hello", "thank you", "i love u"]

        res = self.model.predict(np.expand_dims(points, axis=0))[0]
        
        if res[np.argmax(res)] > 0.8:
            pred = self.actions[np.argmax(res)]

            response_data = {
                "message": "Pose estimation successful",
                "pose_data": pred
            }
            
            return response_data
        else:
            response_data = {
                "message": "Pose estimation successful",
                "pose_data": "N/A"
            }
            
            return response_data
    def detect_action(self):
        self.load_model()
        # 1. New detection variables
        sequence = []
        sentence = []
        predictions = []
        threshold = 0.75

        cap = cv2.VideoCapture(1)
        # Set mediapipe model 
        with self.mp_holistic.Holistic(min_detection_confidence=0.5, min_tracking_confidence=0.5) as holistic:
            while cap.isOpened():

                # Read feed
                ret, frame = cap.read()

                # Make detections
                image, results = self.mediapipe_detection(frame, holistic)
                print(results)
                
                # Draw landmarks
                self.draw_landmarks(image, results)
                
                # 2. Prediction logic
                keypoints = self.extract_keypoints(results)
                sequence.append(keypoints)
                sequence = sequence[-30:]
                
                if len(sequence) == 30:
                    res = self.model.predict(np.expand_dims(sequence, axis=0))[0]
                    print(self.actions[np.argmax(res)])
                    predictions.append(np.argmax(res))
                    
                    
                #3. Viz logic
                    if np.unique(predictions[-10:])[0]==np.argmax(res): 
                        if res[np.argmax(res)] > threshold: 
                            
                            if len(sentence) > 0: 
                                if self.actions[np.argmax(res)] != sentence[-1]:
                                    sentence.append(self.actions[np.argmax(res)])
                            else:
                                sentence.append(self.actions[np.argmax(res)])

                    if len(sentence) > 2: 
                        response_data = {
                            "message": "Pose estimation successful",
                            "pose_data": sentence
                        }
                        return response_data


                    # Viz probabilities
                    image = self.prob_viz(res, self.actions, image, self.colors)
                    
                cv2.rectangle(image, (0,0), (640, 40), (245, 117, 16), -1)
                cv2.putText(image, ' '.join(sentence), (3,30), 
                            cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 255, 255), 2, cv2.LINE_AA)
                
                # Show to screen
                cv2.imshow('OpenCV Feed', image)

                # Break gracefully
                if cv2.waitKey(10) & 0xFF == ord('q'):
                    response_data = {
                            "message": "Pose estimation unsuccessful",
                            "pose_data": "error"
                    }
                    return response_data
            cap.release()
            cv2.destroyAllWindows()    