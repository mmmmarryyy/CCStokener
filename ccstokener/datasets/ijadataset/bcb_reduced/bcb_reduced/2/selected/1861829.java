package uk.co.massycat.appreviewsfinder.google;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 *
 * @author ben
 */
public class Translator {

    static final String HOST = "ajax.googleapis.com";

    static final String PROTOCOL = "http";

    static final String FILE_START_STRING = "/ajax/services/language/translate?v=1.0&langpair=";

    static final String TEXT_VAR = "&q=";

    private static String unescapeCharacters(String original) {
        StringBuffer safe_string = new StringBuffer();
        for (int i = 0; i < original.length(); i++) {
            char cur_char = original.charAt(i);
            if (cur_char == '\\') {
                char next_char = original.charAt(i + 1);
                char actual_char = 0;
                i += 1;
                switch(next_char) {
                    case '"':
                        actual_char = '"';
                        break;
                    case '\\':
                        actual_char = '\\';
                        break;
                    case 't':
                        actual_char = '\t';
                        break;
                    case 'n':
                        actual_char = '\n';
                        break;
                    case 'r':
                        actual_char = '\r';
                        break;
                    case 'b':
                        actual_char = '\b';
                        break;
                    case 'f':
                        actual_char = '\f';
                        break;
                    case 'u':
                        String uni_str = original.substring(i + 1, i + 1 + 4);
                        try {
                            actual_char = (char) Integer.parseInt(uni_str, 16);
                        } catch (Exception e) {
                        }
                        i += 4;
                        break;
                }
                if (actual_char != 0) {
                    safe_string.append(actual_char);
                }
            } else {
                safe_string.append(cur_char);
            }
        }
        return safe_string.toString();
    }

    public static String spaceEncode(String orig_text) {
        return orig_text.replace(" ", "%20");
    }

    public static String urlencode(String orig_text) {
        StringBuffer safe_string = new StringBuffer();
        String[] escape_chars = { "%", "$", "&", "+", ",", "/", ":", ";", "=", "?", "@", "\'", "<", ">", "#", "{", "}", "|", "^", "~", "[", "]", "`", "(", ")" };
        for (int i = 0; i < escape_chars.length; i++) {
            String escape_seq = "%" + Integer.toHexString(escape_chars[i].charAt(0));
            orig_text = orig_text.replace(escape_chars[i], escape_seq);
        }
        orig_text = orig_text.replace(" ", "+");
        for (int i = 0; i < orig_text.length(); i++) {
            char cur_char = orig_text.charAt(i);
            int char_val = orig_text.codePointAt(i);
            boolean escape = false;
            if (char_val <= 0x1F || char_val >= 0x7f) {
                escape = true;
            }
            if (escape) {
                String char_str = orig_text.substring(i, i + 1);
                try {
                    byte[] utf8_bytes = char_str.getBytes("UTF-8");
                    for (int j = 0; j < utf8_bytes.length; j++) {
                        String escape_string = Integer.toHexString(((int) utf8_bytes[j]) & 0xff);
                        if (escape_string.length() == 1) {
                            escape_string = "0" + escape_string;
                        }
                        safe_string.append("%" + escape_string);
                    }
                } catch (Exception e) {
                }
            } else {
                safe_string.append(cur_char);
            }
        }
        return safe_string.toString();
    }

    public static String translate(String orig_text, String orig_google_code, String trans_google_code) {
        String trans_string = null;
        try {
            String safe_text = orig_text;
            safe_text = urlencode(safe_text);
            String file_string = FILE_START_STRING + orig_google_code + "%7C" + trans_google_code + TEXT_VAR + safe_text;
            URL trans_url = new URL(PROTOCOL, HOST, file_string);
            HttpURLConnection connection = (HttpURLConnection) trans_url.openConnection();
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream input = connection.getInputStream();
                byte[] buffer = new byte[100000];
                int offset = 0;
                while (true) {
                    int read = input.read(buffer, offset, buffer.length - offset);
                    if (read > 0) {
                        offset += read;
                    } else {
                        break;
                    }
                }
                if (offset > 0) {
                    String response = new String(buffer, 0, offset, "UTF-8");
                    String RESPONSE_STATUS = "\"responseStatus\":";
                    int response_status_index = response.indexOf(RESPONSE_STATUS);
                    if (response_status_index < 0) {
                        return null;
                    }
                    int end_index = response.indexOf("}", response_status_index);
                    int response_code = 0;
                    try {
                        String response_code_str = response.substring(response_status_index + RESPONSE_STATUS.length(), end_index);
                        response_code_str = response_code_str.trim();
                        response_code = Integer.parseInt(response_code_str);
                    } catch (Exception e) {
                    }
                    if (response_code != 200) {
                        return null;
                    }
                    String TRANSLATION_TAG = "\"translatedText\":";
                    int trans_start = response.indexOf(TRANSLATION_TAG);
                    if (trans_start < 0) {
                        return null;
                    }
                    int trans_end = response.indexOf("}", trans_start);
                    if (trans_end < 0) {
                        return null;
                    }
                    String trans_text = response.substring(trans_start + TRANSLATION_TAG.length(), trans_end);
                    trans_text = trans_text.trim();
                    if (trans_text.length() <= 2) {
                        return null;
                    }
                    trans_text = trans_text.substring(1, trans_text.length() - 1);
                    trans_string = unescapeCharacters(trans_text);
                }
            } else {
                System.err.println("Failed to get translation: " + connection.getResponseCode());
            }
            connection.disconnect();
        } catch (Exception e) {
            System.err.println("Translation error: " + e);
        }
        return trans_string;
    }
}
