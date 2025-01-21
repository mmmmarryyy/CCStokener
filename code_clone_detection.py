import hashlib
import math
import multiprocessing
import os
from datetime import datetime

from code_clone_detection.build_k_token_index import build_k_token_index
from file_utils import utils
from file_utils.out_file_utils import process_out_file
from file_utils.utils import get_list_of_files_with_suffix, get_common_path

report_dir = f"report_{datetime.now()}"  # TODO: maybe move somewhere or pass through arguments


def countSameActionTokens(tokens1, tokens2):
    """Counts the number of same Action tokens between two lists."""
    tokens1_tuples = [tuple(token) for token in tokens1]
    tokens2_tuples = [tuple(token) for token in tokens2]
    return len(set(tokens1_tuples) & set(tokens2_tuples))


def cosine(v1, v2):
    """Calculates the cosine similarity between two vectors."""
    if not v1 or not v2:
        return 0
    dot_product = sum(x * y for x, y in zip(v1, v2))
    magnitude_v1 = math.sqrt(sum(x ** 2 for x in v1))
    magnitude_v2 = math.sqrt(sum(y ** 2 for y in v2))
    if magnitude_v1 == 0 or magnitude_v2 == 0:
        return 0
    return dot_product / (magnitude_v1 * magnitude_v2)


def verifySim(P, Q, phi):  # TODO: assert that phi is negative
    """Calculates the similarity between two vector collections using a threshold-based matching algorithm."""
    totalSim = 0
    MP = [0] * len(P)
    MQ = [0] * len(Q)

    t = 1.0

    while t > 0:
        for i in range(len(P)):
            if MP[i] == 1:
                continue
            for j in range(len(Q)):
                if MQ[j] == 1:
                    continue
                sim = cosine(P[i], Q[j])
                if sim >= t:
                    totalSim += sim
                    MP[i] = 1
                    MQ[j] = 1
        t += phi

    return totalSim / max(len(P), len(Q)) if max(len(P), len(Q)) > 0 else 0


def extract_semantic_vector_collections(data):
    variable_vectors = [item["vector"] for item in data.get("variable_group", [])]
    expression_vectors = [item["vector"] for item in data.get("relation", [])]
    callee_vectors = [item["vector"] for item in data.get("method_group", [])]
    return variable_vectors, expression_vectors, callee_vectors


def clone_detection_worker(all_block_data, k, beta, theta, phi, eta, token_count_threshold, token_count_differ, kIndex,
                           sorted_blocks, start_index, end_index, thread_num, subdirectory):
    print(f"[{start_index}:{end_index}] start")  # TODO: wrap into logger
    candidate_clones = set()

    output_filename = os.path.join(report_dir, f"clone_pairs_{subdirectory}_thread_{thread_num}.txt")

    for index, ((file_path, start_line), block_data) in enumerate(sorted_blocks):
        if index < start_index or index >= end_index:
            continue
        if index % 100 == 0:
            print(f"[{datetime.now()}] [{start_index}:{end_index}] index = {index}")  # TODO: wrap into logger

        candidate_block_set = set()

        if block_data[
            'totalTokenNum'] >= token_count_threshold:  # TODO: maybe it would work better if we will use validTokenNum
            action_tokens = block_data['action_tokens']
            if len(action_tokens) >= k:
                k_tokens = [tuple(action_tokens[i:i + k]) for i in range(len(action_tokens) - k + 1)]
                for k_token in k_tokens:
                    hash_value = hashlib.sha256(str(k_token).encode()).hexdigest()
                    candidate_block_set.update(kIndex[hash_value])
        else:
            for i in range(index + 1, len(sorted_blocks)):
                (probable_clone_file_path, probable_clone_start_line), probable_clone_block_data = sorted_blocks[i]
                if abs(block_data['totalTokenNum'] - probable_clone_block_data['totalTokenNum']) <= token_count_differ:
                    candidate_block_set.add((probable_clone_file_path, probable_clone_start_line))
                else:
                    break

        filtered_clones = []
        for candidate_file_path, candidate_start_line in candidate_block_set:
            if (file_path, start_line) != (candidate_file_path, candidate_start_line):
                candidate_block_data = all_block_data[(candidate_file_path, candidate_start_line)]
                candidate_action_tokens = candidate_block_data['action_tokens']
                sat = countSameActionTokens(block_data['action_tokens'], candidate_action_tokens)
                ato = sat / min(len(block_data['action_tokens']), len(candidate_action_tokens))
                block_total_token_number = block_data['totalTokenNum']
                candidate_block_total_token_number = candidate_block_data['totalTokenNum']
                tr = (min(block_total_token_number, candidate_block_total_token_number) /
                      max(block_total_token_number, candidate_block_total_token_number))
                if ato >= beta and tr >= theta:
                    filtered_clones.append((candidate_file_path, candidate_start_line))

        VT_j, ET_j, CT_j = extract_semantic_vector_collections(block_data)
        for candidate_block_k_id in filtered_clones:
            block_k = all_block_data[candidate_block_k_id]
            pair = ((file_path, start_line), (block_k['filePath'], block_k['startline']))
            if pair not in candidate_clones and (pair[1], pair[0]) not in candidate_clones:
                VT_k, ET_k, CT_k = extract_semantic_vector_collections(block_k)
                simVT = verifySim(VT_j, VT_k, phi)
                simET = verifySim(ET_j, ET_k, phi)
                simCT = verifySim(CT_j, CT_k, phi)
                if (simVT + simET + simCT) >= eta * 3:
                    candidate_clones.add(
                        ((file_path, start_line), (block_k['filePath'], block_k['startline'])))

    with open(output_filename, 'a+') as fileOut:
        for ((left_path, left_start_line), (right_path, right_start_line)) in candidate_clones:
            left_parts = left_path.split(os.sep)
            right_parts = right_path.split(os.sep)

            fileOut.write(
                f"{left_parts[-2]},{left_parts[-1]},{left_start_line},{all_block_data[(left_path, left_start_line)]['endline']},{right_parts[-2]},{right_parts[-1]},{right_start_line},{all_block_data[(right_path, right_start_line)]['endline']}\n")


def clone_detection(out_files, k, beta, theta, phi, eta, token_count_threshold, token_count_differ, subdirectory):
    all_block_data = {}
    for out_file in out_files:
        all_block_data.update(process_out_file(out_file))

    sorted_blocks = sorted(all_block_data.items(), key=lambda item: item[1]['totalTokenNum'])
    print(f"[{datetime.now()}] after extracting data for {subdirectory}")
    print(len(sorted_blocks))
    print(len(all_block_data))  # TODO: delete or wrap into log

    kIndex = build_k_token_index(all_block_data, k)

    num_threads = min(4, multiprocessing.cpu_count())  # TODO: тут начинает не хватать программной памяти для tokens/4
    print(f"[{datetime.now()}] have {num_threads} threads")
    chunk_size = len(sorted_blocks) // num_threads
    print(f"chunk_size = {chunk_size}")

    with multiprocessing.Pool(processes=num_threads) as pool:
        results = []
        for i in range(num_threads):
            start_index = i * chunk_size
            end_index = (i + 1) * chunk_size if i < num_threads - 1 else len(sorted_blocks)
            results.append(pool.apply_async(clone_detection_worker, (
                all_block_data, k, beta, theta, phi, eta, token_count_threshold, token_count_differ, kIndex,
                sorted_blocks,
                start_index, end_index, i, subdirectory)))

        for result in results:
            result.get()


def parse_directory(input_directory):
    out_files = [get_common_path(file, rf"{input_directory}.*") for file in
                 get_list_of_files_with_suffix(f"./{input_directory}")]
    k = 50  # TODO: don't have information about value of this const in article
    beta = 0.5
    theta = 0.4
    phi = -0.1
    eta = 0.65

    # For the blocks with few or no Action tokens, we use their total number of tokens as the principle of locating.
    token_count_threshold = k  # TODO: don't have information about value of this const in article
    token_count_differ = 15  # TODO: don't have information about value of this const in article

    print(f"[{datetime.now()}] start finding clones for {input_directory}")
    print(f"[{datetime.now()}] files num = {len(out_files)}")
    clone_detection(out_files, k, beta, theta, phi, eta, token_count_threshold, token_count_differ,
                    os.path.basename(input_directory))


def parse_directories(input_directory):
    print(f"[{datetime.now()}] begin of parse_directories")
    list_of_subdirectories = sorted(utils.get_list_of_subdirectories(input_directory), key=lambda x: int(
        os.path.basename(x)))  # TODO: delete this key sort or add bcb mode and wrap it into if
    # list_of_subdirectories = list(
    #     filter(lambda x: int(os.path.basename(x)) > 4, list_of_subdirectories))  # TODO: this is temporary filter

    if len(list_of_subdirectories) == 0:
        parse_directory(input_directory)
    else:
        for subdirectory in list_of_subdirectories:
            parse_directory(subdirectory)

    print(f"[{datetime.now()}] end of parse_directories")


if __name__ == "__main__":
    os.makedirs(report_dir, exist_ok=True)  # TODO: check and maybe clear after rerun

    parse_directories("tokens")
