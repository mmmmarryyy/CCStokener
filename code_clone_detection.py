from code_clone_detection.build_k_token_index import build_k_token_index
from file_utils.out_file_utils import get_common_path
from file_utils.utils import get_list_of_files_with_suffix

if __name__ == "__main__":
    out_files = sorted([get_common_path(file, r"tokens.*") for file in get_list_of_files_with_suffix("./tokens")])
    k = 6  # TODO: find something in article about this constant
    kIndex = build_k_token_index(out_files, k)
    with open("k_index.txt", 'w') as fileOut:
        fileOut.write(str(kIndex))
