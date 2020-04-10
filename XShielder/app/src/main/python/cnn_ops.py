'''
@Description: operations of building a CNN (integrated anti-malware engine version)
@Version: 1.0.2.20200411
@Author: Jichen Zhao
@Date: 2020-04-08 10:14:15
@Last Editors: Jichen Zhao
@LastEditTime: 2020-04-11 00:22:17
'''

import numpy as np
import tensorflow as tf
import tf_slim as slim # use this to make defining, training, and evaluating a CNN simple (TODO: needs testing)


# slim = tf.contrib.slim # use this to make defining, training, and evaluating a CNN simple


def conv_net(inputs):
    '''
    Build a CNN.

    Parameters
    ----------
    inputs : input data
    '''

    # using the scope to avoid mentioning the parameters repeatedly
    with slim.arg_scope([slim.conv2d, slim.fully_connected],
        activation_fn = leaky_relu(0.005),
        weights_initializer = tf.truncated_normal_initializer(0.0, 0.01),
        weights_regularizer = slim.l2_regularizer(0.0005)):

        net = slim.conv2d(inputs, 512, (3, 1357), 1, padding = 'valid', scope = 'cnn_conv_1')
        net = slim.max_pool2d(net, (4, 1), 4, padding = 'valid', scope = 'cnn_pool_2')
        net = slim.conv2d(net, 512, (5, 1), 1, scope = 'cnn_conv_3')
        net = slim.max_pool2d(net, (4, 1), 4, padding = 'valid', scope = 'cnn_pool_4')
        net = slim.flatten(net, scope = 'cnn_flatten_5')
        net = slim.fully_connected(net, 2, scope = 'cnn_fc_8', activation_fn = tf.nn.softmax)

    return net


def leaky_relu(rate):
    '''
    Employ a leaky ReLU.

    Parameters
    ----------
    rate : a rate for generating x with input data in the operation of a leaky ReLU

    Returns
    -------
    op : the result of the operation of a leaky ReLU
    '''

    def op(inputs):
        '''
        Define the operation of a leaky ReLU.

        Parameters
        ----------
        inputs : input data

        Returns
        -------
        result : the max of x and y (i.e. `x > y ? x : y`) element-wise (x is `inputs * alpha` and y is `inputs`)
        '''

        return tf.maximum(inputs * rate, inputs, name = 'leaky_relu')
    
    return op


def get_one_hot_vector(size, data):
    '''
    Get a one-hot vector of the specified data.

    Parameters
    ----------
    size : the size for getting a one-hot vector

    data : the data for getting a one-hot vector

    Returns
    -------
    one_hot_vector : a one-hot vector of the specified data
    '''

    # set a specified element to 1 and leave the others to 0
    one_hot_vector = np.zeros((size, 2))
    one_hot_vector[np.range(size), data] = 1

    return one_hot_vector