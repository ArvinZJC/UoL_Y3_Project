'''
@Description: a directory/path loader (integrated anti-malware engine version)
@Version: 1.0.3.20200414
@Author: Jichen Zhao
@Date: 2020-04-07 11:31:45
@Last Editors: Jichen Zhao
@LastEditTime: 2020-04-14 23:01:45
'''

import os


def get_test_apk_folder_directory() -> str:
    '''
    Get the directory of the "APK folder" for tests on Windows.

    Returns
    -------
    test_apk_folder_directory : the directory of the "APK folder" for tests on Windows
    '''

    test_apk_folder_directory = 'C:\\X Shielder PY Test\\apks'

    if not os.path.exists(test_apk_folder_directory):
        os.makedirs(test_apk_folder_directory)
    
    return test_apk_folder_directory


def get_dictionary_path(apk_folder_directory) -> str:
    '''
    Get the path of the API call dictionary.

    Parameters
	----------
    apk_folder_directory : the directory of the folder containing APKs for scanning

    Returns
    -------
    dictionary_path : the path of the API call dictionary
    '''

    return os.path.join(apk_folder_directory, 'api_call_dictionary.save')


def get_cnn_trainer_saver_path() -> str:
    '''
    Get the path of the checkpoint files from the saver when training a CNN with a stacked encoder.

    Returns
    -------
    cnn_trainer_saver_path : the path of the checkpoint files from the saver when training a CNN with a stacked encoder
    '''
    
    return os.path.join(os.path.dirname(__file__), 'cnn_trainer.ckpt')


# test purposes only
if __name__ == '__main__':
    test_apk_folder_directory = get_test_apk_folder_directory()

    print(test_apk_folder_directory)
    print(get_dictionary_path(test_apk_folder_directory))
    print(get_cnn_trainer_saver_path())