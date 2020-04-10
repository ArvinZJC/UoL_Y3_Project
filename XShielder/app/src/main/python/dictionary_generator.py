'''
@Description: an API call dictionary generator (integrated anti-malware engine version)
@Version: 1.0.1.20200408
@Author: Jichen Zhao
@Date: 2020-04-07 16:45:10
@Last Editors: Jichen Zhao
@LastEditTime: 2020-04-08 17:05:17
'''

from androguard.core.analysis import analysis
from androguard.core.bytecodes import apk, dvm
import numpy as np
import os
import pickle
import re

from path_loader import get_dictionary_path


def generate_dictionary(apk_folder_directory) -> int:
	'''
	Generate a dictionary storing mapping all distinct API calls to numbers and pickle the dictionary.

	Parameters
	----------
	apk_folder_directory : the directory of the folder containing APKs for scanning
	
	Returns
	-------
	dictionary_length : the length of the API call dictionary (-1 if any exception occurs)
	'''

	api_call_dictionary = {}

	try:
		for file in os.listdir(apk_folder_directory):
			file_path = os.path.join(apk_folder_directory, file)
			
			if file_path.endswith('.apk'):
				app = apk.APK(file_path)
				app_dex = dvm.DalvikVMFormat(app.get_dex())
			else: 
				continue

			app_x = analysis.Analysis(app_dex)

			method_list = []
			class_names = [classes.get_name() for classes in app_dex.get_classes()]

			for method in app_dex.get_methods():
				g = app_x.get_method(method)

				if method.get_code() == None:
					continue
				
				for i in g.get_basic_blocks().get():
					for ins in i.get_instructions():
						output = ins.get_output() # this is a string that contains methods, variables, or anything else
						match = re.search(r'(L[^;]*;)->[^\(]*\([^\)]*\).*', output)

						if match and match.group(1) not in class_names:
							method_list.append(match.group())

							if not api_call_dictionary.__contains__(match.group()):
								api_call_dictionary[match.group()] = len(api_call_dictionary)
	except:
		return -1

	dictionary_stream = open(get_dictionary_path(apk_folder_directory), 'wb')
	pickle.dump(api_call_dictionary, dictionary_stream, protocol = pickle.DEFAULT_PROTOCOL)
	dictionary_stream.close()

	return len(api_call_dictionary)


# test purposes only
if __name__ == '__main__':
	from path_loader import get_test_apk_folder_directory
	
	
	print(generate_dictionary(get_test_apk_folder_directory()))