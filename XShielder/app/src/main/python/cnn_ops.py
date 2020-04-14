'''
@Description: operations of building a CNN (integrated anti-malware engine version)
@Version: 1.0.4.20200414
@Author: Jichen Zhao
@Date: 2020-04-08 10:14:15
@Last Editors: Jichen Zhao
@LastEditTime: 2020-04-14 13:54:16
'''

import numpy as np
import tensorflow.compat.v1 as tf
import tf_slim as slim # use this to make defining, training, and evaluating a CNN simple


def conv_net(inputs):
    '''
    Build a CNN.

    Parameters
    ----------
    inputs : input data

    Returns
    -------
    net : a CNN architecture
    '''

    # using the scope to avoid mentioning the parameters repeatedly
    with slim.arg_scope([slim.conv2d, slim.fully_connected],
        activation_fn = leaky_relu(0.005),
        weights_initializer = tf.truncated_normal_initializer(0.0, 0.01),
        weights_regularizer = slim.l2_regularizer(0.0005)):

        net = slim.conv2d(inputs, 512, (3, inputs.shape[2]), 1, padding = 'valid', scope = 'conv_1') # (3, dimension_count)
        net = slim.max_pool2d(net, (4, 1), 4, padding = 'valid', scope = 'pool_2')
        net = slim.conv2d(net, 512, (5, 1), 1, scope = 'conv_3')
        net = slim.max_pool2d(net, (4, 1), 4, padding = 'valid', scope = 'pool_4')
        net = slim.flatten(net, scope = 'flatten_5')
        net = slim.fully_connected(net, 2, scope = 'fc_6', activation_fn = tf.nn.softmax)

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
    one_hot_vector[np.arange(size), data] = 1

    return one_hot_vector