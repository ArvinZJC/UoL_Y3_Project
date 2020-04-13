'''
@Description: testing a CNN with a stacked autoencoder (integrated anti-malware engine version)
@Version: 1.0.3.20200412
@Author: Jichen Zhao
@Date: 2020-04-08 10:12:59
@Last Editors: Jichen Zhao
@LastEditTime: 2020-04-12 17:38:32
'''

import numpy as np
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '1' # disable printing Tensorflow debugging messages with the level INFO
import tensorflow.compat.v1 as tf
import tf_slim as slim  # use "slim" to make defining, training, and evaluating a CNN simple

from cnn_ops import conv_net, get_one_hot_vector
from data_reader import load_data
from decompressor import decompress
from path_loader import get_cnn_trainer_saver_path


def classify_apps(apk_folder_directory):
    '''
    Classify apps as benign or malicious apps.

    Parameters
    ----------
    apk_folder_directory : the directory of the folder containing APKs for scanning

    Returns
    -------
    TODO:
    '''

    data_X, data_Y, package_name = load_data(apk_folder_directory)    

    learning_rate = 0.00001
    batch_size = 2
    display_step = 1
    dimension_count = data_X.max() + 10
    input_size = 20
    class_count = 2

    tf.disable_eager_execution()
    X = tf.placeholder(tf.float32, [None, input_size, dimension_count, 1])
    Y = tf.placeholder(tf.float32, [None, class_count])

    # build the net and define the loss and optimiser
    prediction = conv_net(X)
    loss_op = slim.losses.softmax_cross_entropy(prediction, Y)
    training_op = tf.train.AdagradOptimizer(learning_rate = learning_rate).minimize(loss_op)

    # evaluate the model
    correct_prediction = tf.equal(tf.argmax(prediction, 1), tf.argmax(Y, 1))
    accuracy = tf.reduce_mean(tf.cast(correct_prediction, tf.float32))

    init_op = tf.global_variables_initializer() # intialise the variables to assign their default values

    with tf.Session() as session:
        saver = tf.train.import_meta_graph(get_cnn_trainer_saver_path() + '.meta')
        saver.restore(session, get_cnn_trainer_saver_path())
        
        batch_count = int(data_X.shape[0] / batch_size)
        batch_accuracy_sum = 0.0

        for step in range(batch_count):
            batch_x, batch_y = data_X[step * batch_size : (step + 1) * batch_size], data_Y[step * batch_size : (step + 1) * batch_size]

            batch_x = decompress(batch_x, dimension_count)
            batch_x = session.run(X, feed_dict = {X: batch_x})
            batch_y = get_one_hot_vector(batch_size, batch_y)
            batch_y = np.repeat(batch_y, input_size, axis = 0)
            assert(batch_x.shape[0] == batch_y.shape[0]) # raise exception if they have different sizes

            batch_accuracy = session.run(accuracy, feed_dict = {X: batch_x, Y: batch_y}) # calculate batch accuracy
            batch_accuracy_sum += batch_accuracy
            print('Step: ' + str(step), 'batch accuracy: ' + '{:.3f}'.format(batch_accuracy))

        print('Test accuracy: ' + '{:.3f}'.format(batch_accuracy_sum / batch_count))

if __name__ == '__main__':
    from path_loader import get_test_apk_folder_directory

    
    classify_apps(get_test_apk_folder_directory())