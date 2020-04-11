'''
@Description: a compressed feature extractor (integrated anti-malware engine version)
@Version: 1.0.3.20200411
@Author: Jichen Zhao
@Date: 2020-04-08 10:01:12
@Last Editors: Jichen Zhao
@LastEditTime: 2020-04-11 16:59:39
'''

from androguard.core.analysis import analysis
from androguard.core.bytecodes import apk, dvm
import numpy as np
import os
import pickle
import re
import sys

from path_loader import get_dictionary_path


max_calls = 5 # this value influences the input size in the parts related to CNN


def extract_compressed_features(apk_folder_directory) -> int:
    '''
    Extract compressed features for apps and pickle them.

    Parameters
	----------
    apk_folder_directory : the directory of the folder containing APKs for scanning

    Returns
    -------
    problem_list : a list recording problems (even indexes for APK names and odd indexes for the corresponding problems)
    '''

    api_call_dictionary = pickle.load(open(get_dictionary_path(apk_folder_directory), 'rb'), encoding = 'latin1') # change the encoding if there is a UnicodeDecodeError
    failed_extraction_list = []

    for file in os.listdir(apk_folder_directory):
        file_path = os.path.join(apk_folder_directory, file)

        if file_path.endswith('.apk'):
            try:
                x, recursion_error_count = get_compressed_feature_vector(file_path, api_call_dictionary)
                
                data_point = {}
                data_point['x'] = x
                data_point['y'] = 0 # the category of the app is undecided when this function is called in the integrated anti-malware engine, so simply categorise it as benign apps
                
                feature_stream = open(os.path.join(apk_folder_directory, str(file) + '.save'), 'wb')
                pickle.dump(data_point, feature_stream, protocol = pickle.DEFAULT_PROTOCOL)
                feature_stream.close()

                if recursion_error_count > 0:
                    failed_extraction_list.append(file)
                    failed_extraction_list.append('HasRecursionError(' + str(recursion_error_count) + ')')
            except Exception as e:
                failed_extraction_list.append(file)
                failed_extraction_list.append(repr(e))
    
    return failed_extraction_list


def get_compressed_feature_vector(path, api_call_dictionary):
    '''
    Get a compressed feature vector.

    Parameters
    ----------
    path : the path of the file to get a compressed feature vector
    
    api_call_dictionary : a dictionary of API calls
    
    Returns
    -------
    feature_vector : a compressed feature vector

    recursion_error_count : the number of recursion errors
    '''

    max_sequences = max_calls
    feature_vector = np.zeros((max_calls, max_sequences), dtype = int)

    call_count = 0
    sequence_count = 0

    app = apk.APK(path)
    app_dex = dvm.DalvikVMFormat(app.get_dex())

    app_x = analysis.Analysis(app_dex)
    class_names = [classes.get_name() for classes in app_dex.get_classes()]
    recursion_error_count = 0

    for method in app_dex.get_methods():
        g = app_x.get_method(method)
    
        if method.get_code() == None:
            continue

        for i in g.get_basic_blocks().get():
            if i.childs != [] and sequence_count < max_sequences:
                call_count = 0
                
                for ins in i.get_instructions():
                    output = ins.get_output() # this is a string that contains methods, variables, or anything else
                    match = re.search(r'(L[^;]*;)->[^\(]*\([^\)]*\).*', output)
                    
                    if match and match.group(1) not in class_names and call_count < max_calls:
                        feature_vector[call_count, sequence_count] = api_call_dictionary[match.group()]
                        call_count += 1

                rand_child_selected = np.random.randint(len(i.childs))
                recursion_error_count = traverse_graph(i.childs[rand_child_selected][2], feature_vector, class_names, call_count, sequence_count, recursion_error_count, api_call_dictionary)
                
                sequence_count += 1

    return feature_vector, recursion_error_count


def traverse_graph(node,
    feature_vector,
    class_names,
    call_count,
    sequence_count,
    recursion_error_count,
    api_call_dictionary):
    '''
    Recursively run the analyser to track different possible execution paths of the code (each run with a different random choice at the
    branching points).

    Parameters
    ----------
    node : a branch

    feature_vector : a compressed feature vector

    class_names : class names of the file being processed

    call_count : the number of calls

    sequence_count : the number of sequences

    recursion_error_count : the number of recursion errors

    api_call_dictionary : a dictionary of API calls
    '''

    for ins in node.get_instructions():
        output = ins.get_output()
        match = re.search(r'(L[^;]*;)->[^\(]*\([^\)]*\).*', output)

        if match and match.group(1) not in class_names and call_count < max_calls:
            feature_vector[call_count, sequence_count] = api_call_dictionary[match.group()]
            call_count += 1
    
    if node.childs != [] and call_count < max_calls:
        rand_child_selected = np.random.randint(len(node.childs))

        try:
            recursion_error_count = traverse_graph(node.childs[rand_child_selected][2], feature_vector, class_names, call_count, sequence_count, recursion_error_count, api_call_dictionary)
        # maximum recursion depth exceeded while calling a Python object
        except RecursionError:
            recursion_error_count += 1
    
    return recursion_error_count


# test purposes only
if __name__ == '__main__':
    from path_loader import get_test_apk_folder_directory


    print(extract_compressed_features(get_test_apk_folder_directory()))