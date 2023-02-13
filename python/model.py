import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
import tensorflow as tf
import numpy as np
from sklearn.model_selection import train_test_split

num_var = 2

# Load the input data and binary output features from the csv files
x = np.genfromtxt('../dataset/input' + str(num_var) + '.csv', delimiter=',')
y = np.genfromtxt('../dataset/bof' + str(num_var) + '.csv', delimiter=',')

# Split the data into training and validation sets
x_train, x_val, y_train, y_val = train_test_split(x, y, test_size=0.2, random_state=1)

# Set output-shape to 1 if one-dimensional array, else to number of columns
if y_train.ndim == 1:
    outputshape = 1
else:
    outputshape = y_train.shape[1]

# Define the model architecture
model = tf.keras.models.Sequential([
    tf.keras.layers.Dense(units=512, activation='relu', input_shape=(x_train.shape[1],)),
    tf.keras.layers.Dense(units=64, activation='relu'),
    tf.keras.layers.Dense(units=64, activation='relu'),
    tf.keras.layers.Dense(units=outputshape, activation='sigmoid'),
])

# Compile the model
model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])

# Train the model
model.fit(x_train, y_train, epochs=20, batch_size=100)

# Evaluate the model on the validation set
val_loss, val_accuracy = model.evaluate(x_val, y_val)
print('Validation Loss:', val_loss)
print('Validation Accuracy:', val_accuracy)

# Save the model
model.save('modelv' + str(num_var) + '.h5')

# How to load a model:
# keras.models.load_model()