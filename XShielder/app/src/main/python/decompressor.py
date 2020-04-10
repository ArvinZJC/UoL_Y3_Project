'''
@Description: a decompressor (integrated anti-malware engine version)
@Version: 1.0.0.20200408
@Author: Jichen Zhao
@Date: 2020-04-08 20:51:04
@Last Editors: Jichen Zhao
@LastEditTime: 2020-04-08 14:23:15
'''

import numpy as np


def decompress(feature_vector, dimension_count):
    '''
    Decompress features to one-hot form.

    Parameters
    ----------
    feature_vector : features to be decompressed

    dimension_count : the number of dimensions

    Returns
    -------
    one_hot_form : the one-hot representation where each feature is encoded using a one-hot vector
    '''

    one_hot_vector_list = [] # a list of one-hot vectors for converting to the one-hot representation
    
    for feature in feature_vector:
        feature_squeezed = np.squeeze(feature) # the 2D squeezed array of samples where each component is a category label
        one_hot_vector = np.zeros((feature_squeezed.shape[0], feature_squeezed.shape[1], dimension_count)) # the 3D array that will be the one-hot representation
        
        one_hot_vector[np.arange(feature_squeezed.shape[0]).reshape(feature_squeezed.shape[0], 1), # if it is visualised as a stack of layers where each layer is a sample, this first index selects each layer separately
            np.tile(np.arange(feature_squeezed.shape[1]), (feature_squeezed.shape[0], 1)), # this index selects each component separately
            feature_squeezed] = 1 # to get a one-hot vector for each feature, set a specified element to 1 and leave the others to 0
        
        one_hot_vector_list.append(one_hot_vector.transpose(1, 0, 2))

    return np.expand_dims(np.vstack(one_hot_vector_list), 3)


# test purposes only
if __name__ == '__main__':
    data = np.arange(12).reshape(2, 2, 3, 1)
    print('Shape of the one-hot form: ' + str(decompress(data, 12).shape))