import hashlib
from collections import defaultdict

from file_utils.out_file_utils import extract_action_tokens


def build_k_token_index(all_block_data, k):
    """Builds a global k-token inverted index."""
    kIndex = defaultdict(list)
    for (file_path, start_line), block_data in all_block_data.items():
        action_tokens = extract_action_tokens(block_data)
        if len(action_tokens) >= k:
            k_tokens = [tuple(action_tokens[i:i + k]) for i in range(len(action_tokens) - k + 1)]
            for k_token in k_tokens:
                hash_value = hashlib.sha256(str(k_token).encode()).hexdigest()
                kIndex[hash_value].append((file_path, start_line))

    return kIndex
