import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
import tensorflow as tf
import numpy as np
from sklearn.model_selection import train_test_split

# Load the input data and binary output features from the csv files
x = np.genfromtxt('input.csv', delimiter=',')
y = np.genfromtxt('bof.csv', delimiter=',')

# Load model
model = tf.keras.models.load_model('model86.h5')

# Evaluate the model on the validation set
val_loss, val_accuracy = model.evaluate(x, y)
print('Validation Loss:', val_loss)
print('Validation Accuracy:', val_accuracy)

