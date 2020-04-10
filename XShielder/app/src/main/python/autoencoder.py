'''
@Description: a stacked autoencoder (integrated anti-malware engine version)
@Version: 1.0.2.20200411
@Author: Jichen Zhao
@Date: 2020-04-08 10:02:21
@Last Editors: Jichen Zhao
@LastEditTime: 2020-04-11 00:20:18
'''

import numpy as np
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '1' # disable printing Tensorflow debugging messages with the level INFO
import pickle
import tensorflow as tf
# import tensorflow_core.contrib.layers as lays

from decompressor import decompress


lays = tf.compat.v1.layers # TODO: simple migration needs testing


def compress(inputs, variable_list = None, is_saver_variable_list = False):
    '''
    Compress input data.

    Parameters
    ----------
    inputs : input data

    variable_list : a list of variables to save (default: `None`)

    is_saver_variable_list : a boolean value indicating if the saver variable list is returned (default: `False`)

    Returns
    -------
    saver_variable_list (the value of `is_saver_variable_list` is `True`) : the saver variable list

    data_points (the value of `is_saver_variable_list` is `False`) : compressed input data
    '''

    learning_rate = 0.00001
    dimension_count = 86796
    input_size = 50

    X = tf.placeholder(tf.float32, [None, input_size, dimension_count, 1])
    reconstruction, compressed, _, _, _, _ = autoencoder(X)
    loss_op = tf.reduce_mean(tf.square(reconstruction - X))
    training_op = tf.train.AdamOptimizer(learning_rate = learning_rate).minimize(loss_op)

    if is_saver_variable_list:
    	return tf.train.Saver()._var_list # add ops to save and restore all variables
    
    saver = tf.train.Saver(var_list = variable_list) # add ops to save and restore all variables
	
    with tf.Session() as session:
        init_op = tf.variables_initializer([variable for variable in tf.global_variables() if variable.name.split(':')[0] in set(session.run(tf.report_uninitialized_variables()))]) # add an op to initialise variables
        session.run(init_op)
        batch_x = decompress(inputs, dimension_count)
        _, data_points = session.run([reconstruction, compressed], feed_dict = {X: batch_x})
        
    return data_points


def autoencoder(inputs):
    '''
    Act as an autoencoder to process input data.

    Parameters
    ----------
    inputs : input data

    Returns
    -------
    reconstruction : part of result of the third (final) decoder (50 * 10850 * 32  ->  50 * 86796 * 1)

    compressed : result of the third (final) encoder (50 * 5825 * 16  ->  50 * 1357 * 8)

    encoder_1 : result of the first encoder (50 * 86796 * 1  ->  50 * 10850 * 32)

    encoder_2 : result of the second encoder (50 * 10850 * 32  ->  50 * 5825 * 16)

    decoder_1 : result of the first decoder (50 * 1357 * 8  ->  50 * 5825 * 16)
    
    decoder_2 : result of the second decoder (50 * 5825 * 16  ->  50 * 10850 * 32)
    '''

    # encoder
    # encoder_1 = lays.conv2d(inputs, 32, [5, 5], stride = (1, 8), padding = 'same') # 50 * 86796 * 1  ->  50 * 10850 * 32
    encoder_1 = lays.conv2d(inputs, 32, [5, 5], strides = (1, 8), padding = 'same', activation = tf.nn.relu)
    # encoder_2 = lays.conv2d(encoder_1, 16, [5, 5], stride = (1, 2), padding = 'same') # 50 * 10850 * 32  ->  50 * 5825 * 16
    encoder_2 = lays.conv2d(encoder_1, 16, [5, 5], strides = (1, 2), padding = 'same', activation = tf.nn.relu)
    # compressed = lays.conv2d(encoder_2, 8, [5, 5], stride = (1, 4), padding = 'same') # 50 * 5825 * 16  ->  50 * 1357 * 8
    compressed = lays.conv2d(encoder_2, 8, [5, 5], strides = (1, 4), padding = 'same', activation = tf.nn.relu)

    # decoder
    # decoder_1 = lays.conv2d_transpose(compressed, 16, [5, 5], stride = (1, 4), padding = 'same') # 50 * 1357 * 8  ->  50 * 5825 * 16
    decoder_1 = lays.conv2d_transpose(compressed, 16, [5, 5], strides = (1, 4), padding = 'same', activation = tf.nn.relu)
    # decoder_2 = lays.conv2d_transpose(decoder_1, 32, [5, 5], stride = (1, 2), padding = 'same') # 50 * 5825 * 16  ->  50 * 10850 * 32
    decoder_2 = lays.conv2d_transpose(decoder_1, 32, [5, 5], strides = (1, 2), padding = 'same', activation = tf.nn.relu)
    # decoder_3 = lays.conv2d_transpose(decoder_2, 1, [5, 5], stride = (1, 8), padding = 'same', activation_fn = tf.nn.tanh) # 50 * 10850 * 32  ->  50 * 86796 * 1
    decoder_3 = lays.conv2d_transpose(decoder_2, 1, [5, 5], strides = (1, 8), padding = 'same', activation_fn = tf.nn.tanh)

    return decoder_3[:, :, 0 : inputs.get_shape().as_list()[2], :], compressed, encoder_1, encoder_2, decoder_1, decoder_2