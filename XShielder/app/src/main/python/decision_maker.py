'''
@Description: testing a CNN with a stacked autoencoder (integrated anti-malware engine version)
@Version: 1.0.2.20200411
@Author: Jichen Zhao
@Date: 2020-04-08 10:12:59
@Last Editors: Jichen Zhao
@LastEditTime: 2020-04-11 00:34:15
'''

import numpy as np
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '1' # disable printing Tensorflow debugging messages with the level INFO
import tensorflow as tf
import tf_slim as slim  # use "slim" to make defining, training, and evaluating a CNN simple (TODO: needs testing)

from cnn_ops import conv_net, get_one_hot_vector
from data_reader import load_data
from decompressor import decompress
from path_loader import get_cnn_trainer_saver_path
from autoencoder import autoencoder, compress


learning_rate = 0.00001
epoch_count = 1
batch_size = 1
display_step = 1
dimension_count = 86796
input_size = 50
class_count = 2 

X = tf.placeholder(tf.float32, [None, input_size, dimension_count, 1])
reconstruction, compressed, _, _, _, _ = autoencoder(X)
cnn_X = tf.placeholder(tf.float32, [None, input_size, 1357, 8])
cnn_Y = tf.placeholder(tf.float32, [None, class_count])
cnn_prediction = conv_net(cnn_X)
cnn_loss_op = slim.losses.softmax_cross_entropy(cnn_prediction, cnn_Y)
cnn_training_op = tf.train.AdagradOptimizer(learning_rate = learning_rate).minimize(cnn_loss_op)

cnn_correct_prediction = tf.equal(tf.argmax(cnn_prediction, 1), tf.argmax(cnn_Y, 1))
cnn_accuracy = tf.reduce_mean(tf.cast(cnn_correct_prediction, tf.float32))

cnn_init_op = tf.global_variables_initializer()
test_data_X, test_data_Y = load_data() # TODO:

with tf.Session() as session:
    saver = tf.train.Saver()
    saver.restore(session, get_cnn_trainer_saver_path())
    
    i = 0
    test_accuracy = 0.0

    for step in range(int(test_data_X.shape[0] / batch_size)):
        batch_x, batch_y = test_data_X[step * batch_size : (step + 1) * batch_size], test_data_Y[step * batch_size : (step + 1) * batch_size]
        i += 1

        batch_x = decompress(batch_x, dimension_count)
        batch_x = session.run(compressed, feed_dict = {X: batch_x})
        batch_y = get_one_hot_vector(batch_size, batch_y)
        batch_y = np.repeat(batch_y, input_size, axis = 0)
        assert(batch_x.shape[0] == batch_y.shape[0])
        accuracy = session.run(cnn_accuracy, feed_dict = {cnn_X: batch_x, cnn_Y: batch_y}) # calculate batch accuracy

        test_accuracy += accuracy

    print('Test accuracy: ' + '{:.3f}'.format(test_accuracy / i))