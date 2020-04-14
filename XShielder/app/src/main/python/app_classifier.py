'''
@Description: testing a CNN with a stacked autoencoder (integrated anti-malware engine version)
@Version: 1.0.4.20200414
@Author: Jichen Zhao
@Date: 2020-04-08 10:12:59
@Last Editors: Jichen Zhao
@LastEditTime: 2020-04-14 22:06:39
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
    prediction_dictionary : a dictionary recording classification results (keys for APK names and values for corresponding results - 0
        represents a benign app, while 1 represents malware)
    '''

    data_X, data_Y, package_name_list = load_data(apk_folder_directory)    

    learning_rate = 0.00001
    batch_size = 1
    display_step = 1
    dimension_count = 86645
    input_size = 50
    class_count = 2

    tf.disable_eager_execution()
    X = tf.placeholder(tf.float32, [None, input_size, dimension_count, 1])
    Y = tf.placeholder(tf.float32, [None, class_count])

    prediction = conv_net(X) # build the net
    init_op = tf.global_variables_initializer() # intialise the variables to assign their default values
    saver = tf.train.Saver()

    with tf.Session() as session:
        session.run(init_op)

        saver.restore(session, get_cnn_trainer_saver_path())
        
        batch_count = int(data_X.shape[0] / batch_size)
        prediction_dictionary = {}

        for step in range(batch_count):
            batch_x = data_X[step * batch_size : (step + 1) * batch_size]
            batch_y = data_Y[step * batch_size : (step + 1) * batch_size]
            batch_package_name_list = package_name_list[step * batch_size : (step + 1) * batch_size]

            batch_x = decompress(batch_x, dimension_count)
            batch_y = get_one_hot_vector(batch_size, batch_y)

            batch_result_list = session.run(tf.argmax(prediction, 1), feed_dict = {X: batch_x})

            package_name_index = 0

            for start_index in range(0, len(batch_result_list), input_size):
                prediction_dictionary[batch_package_name_list[package_name_index]] = batch_result_list[start_index : start_index + input_size].max()
                package_name_index += 1
        
        return prediction_dictionary


if __name__ == '__main__':
    from path_loader import get_test_apk_folder_directory

    
    print(classify_apps(get_test_apk_folder_directory()))