import hashlib
import re
from collections import defaultdict

from file_utils.out_file_utils import extract_out_block_attributes, extract_vectors_from_out, get_common_path


def extract_action_tokens(out_block_data):
    """Extracts action tokens from the parsed OUT block data."""
    action_tokens = list()
    for section in ["method", "type", "variable"]:
        for item in out_block_data.get(section, []):
            action_tokens.append(item["vector"])
    return sorted(action_tokens)


def build_k_token_index(out_files, k):
    """Builds a global k-token inverted index."""
    kIndex = defaultdict(list)
    for file_path in out_files:
        try:
            with open(file_path, 'r') as f:
                out_data = f.read()
        except FileNotFoundError:
            print(f"Error: File not found - {file_path}")
            continue

        blocks = re.findall(r'<block (.*?)>(.*?)</block>', out_data, re.DOTALL)
        for block_attrs_str, block_content in blocks:
            block_data = extract_out_block_attributes(block_attrs_str)
            block_data = extract_vectors_from_out(block_content, block_data)
            action_tokens = extract_action_tokens(block_data)

            if len(action_tokens) >= k:
                k_tokens = [tuple(action_tokens[i:i + k]) for i in range(len(action_tokens) - k + 1)]
                for k_token in k_tokens:
                    hash_value = hashlib.sha256(str(k_token).encode()).hexdigest()
                    kIndex[hash_value].append(get_common_path(block_data['filePath'], r"bcb_reduced.*") + ":" + str(
                        block_data['startline']) + "-" + str(block_data['endline']))

    return kIndex
