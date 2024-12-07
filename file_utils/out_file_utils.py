import ast
import re


def extract_out_block_attributes(attributes_string):
    """Extracts attributes from the <block> tag."""
    attributes = {}
    for attr in attributes_string.split(', '):
        key, value = attr.split(":")
        key = key.strip()
        value = value.strip()
        try:
            attributes[key] = int(value)
        except ValueError:
            attributes[key] = value
    return attributes


def extract_vectors_from_out(out_block_str, out_block_data):
    """Extracts vectors from the OUT block string."""
    sections = {
        "variable": r"<variable>\n(.*?)</variable>",
        "field": r"<field>\n(.*?)</field>",
        "method": r"<method>\n(.*?)</method>",
        "keyword": r"<keyword>\n(.*?)</keyword>",
        "type": r"<type>\n(.*?)</type>",
        "basic_type": r"<basic type>\n(.*?)</basic type>",
        "variable_group": r"<variable group>\n(.*?)</variable group>",
        "method_group": r"<method group>\n(.*?)</method group>",
        "relation": r"<relation>\n(.*?)</relation>"
    }

    line_pattern = r"([\w-]*),(\d+): (\[.*?\])"

    for section_name, pattern in sections.items():
        match = re.findall(pattern, out_block_str, re.DOTALL)
        if match:
            if len(match) == 1:
                vectors = []
                for line in match[0].splitlines():
                    parts = re.search(line_pattern, line.strip())
                    if parts:
                        vectors.append(
                            {"name": parts.group(1), "count": int(parts.group(2)), "vector": ast.literal_eval(
                                parts.group(3))})

                out_block_data[section_name] = vectors
            else:
                print("len(match) is wrong = ", len(match))
                exit(0)
        else:
            print("doesn't find match for section_name = ", section_name)
            exit(0)

    return out_block_data


def get_common_path(path, pattern):
    match = re.search(pattern, path)
    if match:
        return match.group(0)
    else:
        return None
