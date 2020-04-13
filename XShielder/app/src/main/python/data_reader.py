'''
@Description: a data reader (integrated anti-malware engine version)
@Version: 1.0.3.20200413
@Author: Jichen Zhao
@Date: 2020-04-08 10:43:42
@Last Editors: Jichen Zhao
@LastEditTime: 2020-04-13 17:52:57
'''

import numpy as np
import os
import pickle


def get_engine_version():
    '''
    Get the version of the integrated anti-malware engine.

    Returns
    -------
    engine_version : the version of the integrated anti-malware engine
    '''

    return '1.0.0.20200413'


def load_data(apk_folder_directory):
    '''
    Load all pickled features of APKs for scanning.

    Parameters
	----------
    apk_folder_directory : the directory of the folder containing APKs for scanning

    Returns
    -------
    X : part X of the data

    Y : part Y of the data

    package_name : the corresponding package name of each data
    '''

    X = []
    Y = []
    package_name = []

    for file in os.listdir(apk_folder_directory):
        file_path = os.path.join(apk_folder_directory, file)

        if file_path.endswith('.apk.save'):
            data_point = pickle.load(open(file_path, 'rb'), encoding = 'latin1') # change the encoding if there is a UnicodeDecodeError
            X.append(data_point['x'])
            Y.append(data_point['y'])
            package_name.append(file.replace('.apk.save', ''))

    return np.asarray(X), np.asarray(Y), package_name


# test purposes only
if __name__ == '__main__':
    from path_loader import get_test_apk_folder_directory

    
    print('Anti-malware engine version: ' + get_engine_version())

    X, Y, package_name = load_data(get_test_apk_folder_directory())
    print('Amount of data in part X: ' + str(len(X)))
    print('Amount of data in part Y: ' + str(len(Y)))
    print('Number of labels in the data: ' + str(X.max() + 1))
    print('Package names: ' + str(package_name))