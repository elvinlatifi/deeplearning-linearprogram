import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'
import tensorflow as tf
import numpy as np
from keras.utils import np_utils
from sklearn.model_selection import train_test_split

num_var = 3

# Load the input data and binary output features from the csv files
#x = np.genfromtxt('../dataset/var' + str(num_var) + '/output.csv', delimiter=',')
#y = np.genfromtxt('../dataset/var' + str(num_var) + '/bof.csv', delimiter=',')

x = np.genfromtxt('../dataset/random.output.csv', delimiter=',')
y = np.genfromtxt('../dataset/random.bof.csv', delimiter=',')

y = np_utils.to_categorical(y)  # One-hot encoding

# Split the data into training and validation sets
x_train, x_val, y_train, y_val = train_test_split(x, y, test_size=0.2, random_state=1)

# Set output-shape to 1 if one-dimensional array, else to number of columns
if y_train.ndim == 1:
    outputshape = 1
else:
    outputshape = y_train.shape[1]

inputshape = x_train.shape[1]

model = tf.keras.models.Sequential([
    tf.keras.layers.Dense(units=inputshape, activation='relu', input_shape=(inputshape,)),
    tf.keras.layers.Dense(units=13, activation='relu'),
    tf.keras.layers.Dense(units=outputshape, activation='softmax'),
])

optimizer = tf.keras.optimizers.experimental.RMSprop(
    learning_rate=0.001, #unused LOL
    rho=0.9,
    momentum=0.0,
    epsilon=1e-07,
    centered=False,
    weight_decay=None,
    clipnorm=None,
    clipvalue=None,
    global_clipnorm=None,
    use_ema=False,
    ema_momentum=0.99,
    ema_overwrite_frequency=100,
    jit_compile=True,
    name='RMSprop_CUSTOM1337'
)

# Compile the model
model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])

# Train the model
model.fit(x_train, y_train, epochs=10, batch_size=100)

# Evaluate the model on the validation set
val_loss, val_accuracy = model.evaluate(x_val, y_val)
print('Validation Loss:', val_loss)
print('Validation Accuracy:', val_accuracy)

print('OldValidat Loss: 0.4987562894821167')
print('OldValidat Accuracy: 0.5605109930038452')

# Save the model
model.save('modelv' + str(num_var) + '.h5')

# How to load a model:
# keras.models.load_model()