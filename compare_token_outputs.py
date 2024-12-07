import re
from collections import Counter

from file_utils.out_file_utils import extract_out_block_attributes, extract_vectors_from_out, get_common_path
from file_utils.utils import get_list_of_files_with_suffix


def compare_outs(my_out_file, original_out_file):
    """Compares a JSON file and an OUT file for equivalent data."""

    try:
        with open(my_out_file, 'r') as f:
            my_out_data = f.read()
    except FileNotFoundError as e:
        print(f"Error loading my out file {original_out_file}: {e}")
        return False

    try:
        with open(original_out_file, 'r') as f:
            original_out_data = f.read()
    except FileNotFoundError as e:
        print(f"Error loading original out file {original_out_file}: {e}")
        return False

    my_out_blocks = re.findall(r'<block (.*?)>(.*?)</block>', my_out_data, re.DOTALL)
    original_out_blocks = re.findall(r'<block (.*?)>(.*?)</block>', original_out_data, re.DOTALL)

    if len(my_out_blocks) != len(original_out_blocks):
        print("Number of blocks mismatch.")
        return False

    for i in range(len(my_out_blocks)):
        original_out_block_attrs_str, original_out_block_content = original_out_blocks[i]
        original_out_block_data = extract_out_block_attributes(original_out_block_attrs_str)
        original_out_block_data = extract_vectors_from_out(original_out_block_content, original_out_block_data)

        my_out_block_attrs_str, my_out_block_content = my_out_blocks[i]
        my_out_block_data = extract_out_block_attributes(my_out_block_attrs_str)
        my_out_block_data = extract_vectors_from_out(my_out_block_content, my_out_block_data)

        if not compare_block(my_out_block_data, original_out_block_data):
            print(f"Block {i + 1} mismatch.")
            return False

    return True


def compare_keys(str1, str2):
    parts1 = str1.split('-')
    parts2 = str2.split('-')

    if len(parts1) != len(parts2):
        return False

    if parts1[0] != parts2[0]:
        return False

    middle_parts1 = parts1[1:]
    middle_parts2 = parts2[1:]

    if len(middle_parts1) != len(middle_parts2):
        return False

    return Counter(middle_parts1) == Counter(middle_parts2)


def compare_block(my_out_block, original_out_block_data):
    """Compares individual blocks from JSON and OUT files."""

    key = 'filePath'
    if get_common_path(my_out_block[key], r"bcb_reduced.*") != get_common_path(original_out_block_data[key],
                                                                               r"bcb_reduced.*"):
        print(
            f"Mismatch in field '{key}'. JSON: {get_common_path(my_out_block[key], r'bcb_reduced.*')}, OUT: {get_common_path(original_out_block_data[key], r'bcb_reduced.*')}")
        return False

    for key in ['startline', 'endline', 'validTokenNum', 'totalTokenNum']:
        if my_out_block[key] != original_out_block_data[key]:
            print(f"Mismatch in field '{key}'.\nJSON: {my_out_block[key]},\nOUT: {original_out_block_data[key]}")
            return False

    for key in ['variable', 'field', 'method', 'keyword', 'type', 'basic_type', 'variable_group', 'method_group',
                'relation']:
        field_keys = list(zip(my_out_block[key], original_out_block_data[key]))
        for pair in field_keys:
            if pair[0]['name'] != pair[1]['name'] and not compare_keys(pair[0]['name'], pair[1]['name']):
                if pair[0]['name'] in pair[1]['name'] or pair[1]['name'] in pair[0]['name']:
                    print("everything is ok; (old mismatch); pair (first is mine, second is original) = ", pair)
                else:
                    print("pair = ", pair)
                    print("my_out_block[key] names = ", [field['name'] for field in my_out_block[key]])
                    print("original_out_block_data[key] names = ",
                          [field['name'] for field in original_out_block_data[key]])
                    print(f"Mismatch in key for '{key}'.\nJSON: {pair[0]['name']},\nOUT: {pair[1]['name']}")
                    return False
            if pair[0]['count'] != pair[1]['count']:
                print(f"Mismatch in count for '{key}'.\nJSON: {pair[0]['count']},\nOUT: {pair[1]['count']}")
                return False
            if pair[0]['vector'] != pair[1]['vector']:
                print(
                    f"Mismatch in vectors for '{key}'.\nJSON: {my_out_block[key][pair[0]]['vector']},\nOUT: {original_out_block_data[key][pair[1]]['vector']}")
                return False

    return True


if __name__ == "__main__":
    tokens_file_list = sorted(
        [get_common_path(file, r"tokens.*") for file in get_list_of_files_with_suffix("./tokens")])
    original_tokens_file_list = sorted(
        [get_common_path(file, r"tokens_original.*") for file in get_list_of_files_with_suffix("./tokens_original")])
    not_equal_counter = 0
    equal_counter = 0

    file_list = list(zip(tokens_file_list, original_tokens_file_list))

    for files in file_list:
        if files[0].split('/')[1].split('.')[0] != files[1].split('/')[1].split('.')[0]:
            print("left = ", files[0].split('/')[1].split('.')[0], " right = ", files[1].split('/')[1].split('.')[0])
            exit(0)
        if compare_outs(files[0], files[1]):
            equal_counter += 1
        else:
            not_equal_counter += 1
            print(f"JSON and OUT files are NOT equivalent.")
            print("left = ", files[0], " right = ", files[1])

    print("equal_counter = ", equal_counter)
    print("not_equal_counter = ", not_equal_counter)
