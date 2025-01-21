import os

from semantic_tokens.parse import parse_directories

if __name__ == '__main__':
    # TODO(mmmmarryyy): change later with getting arguments from sys.argv

    input_dir = '../dataset/IJaDataset/bcb_reduced/'
    # input_dir = 'temp_dataset'
    input_dir = os.path.realpath(input_dir)

    output_dir = 'tokens'
    language = 'java'

    parse_directories(input_dir, output_dir)
