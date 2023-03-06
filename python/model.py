import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
import tensorflow as tf
import numpy as np
from keras.utils import np_utils
from sklearn.model_selection import train_test_split

num_var = 30

# Load the input data and binary output features from the csv files
x = np.genfromtxt('../dataset/var' + str(num_var) + '/output.csv', delimiter=',')
y = np.genfromtxt('../dataset/var' + str(num_var) + '/bof.csv', delimiter=',')

x_train, x_val, y_train, y_val = train_test_split(x, y, test_size=0.2, random_state=1)

inputshape = x_train.shape[1]

model = tf.keras.models.Sequential([
    tf.keras.layers.Dense(units=128, activation='relu', input_shape=(inputshape,)),
    tf.keras.layers.Dense(units=64, activation='relu'),
    tf.keras.layers.Dense(units=32, activation='relu'),
    tf.keras.layers.Dense(units=1, activation='sigmoid'),
])

# Compile the model
model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])

# Train the model
model.fit(x_train, y_train, epochs=10, batch_size=100)

# Evaluate the model on the validation set
val_loss, val_accuracy = model.evaluate(x_val, y_val)
print('Validation Loss:', val_loss)
print('Validation Accuracy:', val_accuracy)

# Save the model
model.save('../dataset/var' + str(num_var) + '/model.h5')

# How to load a model:
# keras.models.load_model()