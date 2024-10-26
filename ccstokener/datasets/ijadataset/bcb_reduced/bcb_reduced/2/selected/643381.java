package com.neurogrid.parser;

import gnu.regexp.*;
import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.lang.*;
import java.util.*;
import java.sql.*;
import com.neurogrid.log.*;
import com.neurogrid.database.*;
import java.text.SimpleDateFormat;
import java.net.*;

/**
 * Copyright (C) 2000 NeuroGrid <sam@neurogrid.com><br><br>
 *
 * This is a general parser class that will provide static functions for the operations I
 * want to perform on by chunks of html<br><br>
 *
 * could define static final items here for the left and right braces when I am parsing?<br><br>
 *
 * Change History<br>
 * ------------------------------------------------------------------------------------<br>
 * 0.0   13/Jul/2000    sam       Created file<br>
 * 0.1   19/Jul/2000    sam       Adjusted so that incoming string is mapped to a new one to avoid
 *                                modifying original<br>
 *
 * @author Sam Joseph (sam@neurogrid.com)
 */
public class Parser {

    public static final String cvsInfo = "$Id: Parser.java,v 1.7 2002/01/23 13:31:33 samjoseph Exp $";

    public static String getCvsInfo() {
        return cvsInfo;
    }

    private static long o_time = 0L;

    private static long o_new_time = 0L;

    /**
  * finds repeated occurrence of text enclosed by two chunks and substitutes
  * elements from hashtable: if multiple matches from hashtable -> sent back multiple strings<p>
  *
  * I think this will be able to handle multiple replacements on a single line in
  * conjunction with repeating the line multiple times in response to multiple matches
  */
    public static String[] regenerateHTML(String p_line, String p_openChunk, String p_closeChunk, MultiHashtable p_inserts) throws Exception {
        int index = 0;
        int x_start_index = -1;
        int x_close_index = -1;
        int oc_length = p_openChunk.length();
        int cc_length = p_closeChunk.length();
        String temp_attr = null;
        String x_insert = null;
        StringBuffer x_buf = null;
        String[] x_response = null;
        String x_line = new String(p_line);
        String x_original_match = null;
        while ((index = x_line.indexOf(p_openChunk, index)) >= 0) {
            x_start_index = index;
            x_close_index = x_line.indexOf(p_closeChunk, (x_start_index + oc_length));
            if (x_close_index != -1) {
                temp_attr = x_line.substring(x_start_index + oc_length, x_close_index);
                if (temp_attr != null) {
                    int x_length = 0;
                    x_insert = null;
                    Vector x_matches = (Vector) (p_inserts.get(temp_attr));
                    String x_match = null;
                    boolean x_previous_match = true;
                    if (x_matches != null) {
                        if (x_response == null) {
                            x_previous_match = false;
                            x_response = new String[x_matches.size()];
                        }
                        boolean x_no_spaces = false;
                        if (x_line.indexOf("href") != -1 || x_line.indexOf("img src") != -1) x_no_spaces = true;
                        if (x_previous_match == true) x_length = x_response.length; else x_length = x_matches.size();
                        x_original_match = (String) (x_matches.elementAt(0));
                        for (int i = 0; i < x_length; i++) {
                            try {
                                x_match = (String) (x_matches.elementAt(i));
                            } catch (Exception e) {
                                x_match = (String) (x_matches.elementAt(0));
                            }
                            if (x_match == null) throw new Exception("no match for temp_attr: " + temp_attr);
                            if (x_no_spaces == true) x_match = x_match.replace(' ', '+');
                            if (x_previous_match == true) x_line = x_response[i];
                            x_buf = new StringBuffer(x_line);
                            x_start_index = x_line.indexOf(p_openChunk);
                            x_close_index = x_line.indexOf(p_closeChunk, (x_start_index + oc_length));
                            x_buf.replace(x_start_index, x_close_index + cc_length, x_match);
                            x_response[i] = x_buf.toString();
                        }
                        if (x_matches.size() != 1) {
                            x_line = x_response[0];
                            index = x_line.indexOf(x_original_match, x_start_index) + x_original_match.length();
                        } else if (x_matches.size() == 1) {
                            x_line = x_response[0];
                            index = x_line.indexOf(x_match, x_start_index) + x_match.length();
                        }
                    } else {
                        index = x_close_index + cc_length;
                        x_response = new String[1];
                        x_response[0] = new String("");
                        return x_response;
                    }
                } else {
                    index = x_close_index + cc_length;
                }
            } else {
                if (x_response == null) {
                    x_response = new String[1];
                    x_response[0] = x_line;
                }
                return x_response;
            }
        }
        if (x_response == null) {
            x_response = new String[1];
            x_response[0] = x_line;
        }
        return x_response;
    }

    public static String replaceMultipleEnclosedText(String p_line, String p_openChunk, String p_closeChunk, String[] p_column_names, String[] p_values) {
        int index = 0;
        int x_start_index = -1;
        int x_close_index = -1;
        int oc_length = p_openChunk.length();
        int cc_length = p_closeChunk.length();
        String temp_attr = null;
        String x_insert = null;
        StringBuffer x_buf = null;
        String x_line = new String(p_line);
        while ((index = x_line.indexOf(p_openChunk, index)) >= 0) {
            x_start_index = index;
            x_close_index = x_line.indexOf(p_closeChunk, (x_start_index + oc_length));
            if (x_close_index != -1) {
                temp_attr = x_line.substring(x_start_index + oc_length, x_close_index);
                if (temp_attr != null) {
                    x_insert = null;
                    for (int i = 0; i < p_column_names.length; i++) {
                        if (p_column_names[i].equals(temp_attr)) {
                            x_insert = p_values[i];
                            break;
                        }
                    }
                    if (x_insert != null) {
                        if (x_line.indexOf("href") != -1 || x_line.indexOf("img src") != -1) x_insert = x_insert.replace(' ', '+');
                        x_buf = new StringBuffer(x_line);
                        x_buf.replace(x_start_index, x_close_index + cc_length, x_insert);
                        x_line = x_buf.toString();
                        index = x_line.indexOf(x_insert, x_start_index) + x_insert.length();
                    } else {
                        index = x_close_index + cc_length;
                    }
                } else {
                    index = x_close_index + cc_length;
                }
            } else {
                return x_line;
            }
        }
        return x_line;
    }

    public static String[] extractWordsFromSpacedList(String p_spaced_list) {
        p_spaced_list = p_spaced_list.trim();
        String[] o_array = null;
        Vector o_vector = new Vector();
        int x_space = p_spaced_list.indexOf(" ");
        int start = 0;
        String x_temp = null;
        if (x_space == -1) {
            o_array = new String[1];
            o_array[0] = p_spaced_list;
            return o_array;
        } else {
            do {
                x_temp = p_spaced_list.substring(start, x_space);
                x_temp = x_temp.trim();
                if (x_temp.length() != 0) o_vector.addElement(x_temp);
                p_spaced_list = p_spaced_list.substring(x_space + 1);
                x_space = p_spaced_list.indexOf(" ");
            } while (x_space != -1);
            o_vector.addElement(p_spaced_list);
            o_array = new String[o_vector.size()];
            for (int i = 0; i < o_array.length; i++) {
                o_array[i] = (String) o_vector.elementAt(i);
            }
            return o_array;
        }
    }

    public static void extractAttributeValuePairs(String p_string, Hashtable p_hashtable) {
        int x_start = 0;
        int x_end = -1;
        int x_colon = -1;
        String x_string = null;
        String x_value = null;
        String x_attribute = null;
        x_end = p_string.indexOf('\n', x_start);
        if (x_end == -1 && ((p_string.indexOf(':')) != -1)) x_end = p_string.length();
        while (x_end >= 0) {
            x_string = p_string.substring(x_start, x_end);
            x_colon = x_string.indexOf(':');
            if (x_colon != -1) {
                x_attribute = x_string.substring(0, x_colon);
                x_value = x_string.substring(x_colon + 1);
                if (x_attribute != null && x_value != null) {
                    if (!(x_attribute.equals(""))) {
                        p_hashtable.put(x_attribute.trim(), x_value.trim());
                    }
                }
            }
            x_start = x_end + 1;
            boolean x_bool1 = ((p_string.indexOf(':', x_start)) != -1);
            x_end = p_string.indexOf('\n', x_start);
            boolean x_bool2 = (x_end == -1);
            if ((x_bool1 && x_bool2)) {
                x_end = p_string.length();
            }
        }
    }

    public static void extractAttributeValuePairs(String p_string, Dictionary p_hashtable) {
        int x_start = 0;
        int x_end = -1;
        int x_colon = -1;
        String x_string = null;
        String x_value = null;
        String x_attribute = null;
        x_end = p_string.indexOf('\n', x_start);
        if (x_end == -1 && ((p_string.indexOf(':')) != -1)) x_end = p_string.length();
        while (x_end >= 0) {
            x_string = p_string.substring(x_start, x_end);
            x_colon = x_string.indexOf(':');
            if (x_colon != -1) {
                x_attribute = x_string.substring(0, x_colon);
                x_value = x_string.substring(x_colon + 1);
                if (x_attribute != null && x_value != null) {
                    if (!(x_attribute.equals(""))) {
                        p_hashtable.put(x_attribute.trim(), x_value.trim());
                    }
                }
            }
            x_start = x_end + 1;
            boolean x_bool1 = ((p_string.indexOf(':', x_start)) != -1);
            x_end = p_string.indexOf('\n', x_start);
            boolean x_bool2 = (x_end == -1);
            if ((x_bool1 && x_bool2)) {
                x_end = p_string.length();
            }
        }
    }

    public static String findAndPrefixMultipleEnclosedText(String p_line, String p_openChunk, String p_closeChunk, String p_prefix) {
        int x_index = 0;
        int x_closeIndex = -1;
        int x_oc_length = p_openChunk.length();
        int x_cc_length = p_closeChunk.length();
        int x_insert_no = 0;
        int x_prefix_length = p_prefix.length();
        int x_start = 0;
        String x_temp_attr = null;
        StringBuffer x_buf = new StringBuffer(p_line);
        String x_lower_case_line = p_line.toLowerCase();
        while ((x_start = (x_lower_case_line).indexOf(p_openChunk, x_index)) >= 0) {
            x_closeIndex = (x_lower_case_line).indexOf(p_closeChunk, (x_start + x_oc_length));
            if (x_closeIndex != -1) {
                x_buf.insert(x_start + x_oc_length + (x_insert_no * x_prefix_length), p_prefix);
                x_insert_no++;
                x_index = x_closeIndex + x_cc_length;
            } else {
                return x_buf.toString();
            }
        }
        return x_buf.toString();
    }

    public static void findMultipleEnclosedText(String line, String openChunk, String closeChunk, Vector p_attr_list) {
        int index = 0;
        int closeIndex = -1;
        int oc_length = openChunk.length();
        int cc_length = closeChunk.length();
        String temp_attr = null;
        while ((index = (line.toLowerCase()).indexOf(openChunk, index)) >= 0) {
            closeIndex = (line.toLowerCase()).indexOf(closeChunk, (index + oc_length));
            if (closeIndex != -1) {
                temp_attr = line.substring(index + oc_length, closeIndex);
                if (temp_attr != null) p_attr_list.addElement(temp_attr);
                line = line.substring(closeIndex + cc_length);
                index = 0;
            } else {
                return;
            }
        }
        return;
    }

    public static String findEnclosedText(String line, String openChunk, String closeChunk) {
        int index = 0;
        int closeIndex = -1;
        int oc_length = openChunk.length();
        int cc_length = closeChunk.length();
        String temp_attr = null;
        if ((index = line.indexOf(openChunk, index)) != -1) {
            closeIndex = line.indexOf(closeChunk, (index + oc_length));
            if (closeIndex != -1) {
                temp_attr = line.substring(index + oc_length, closeIndex);
                return temp_attr;
            }
        }
        return null;
    }

    public static String replaceEnclosedText(String p_line, String p_openChunk, String p_closeChunk, String p_replacement) {
        int x_index = 0;
        int x_closeIndex = -1;
        int x_oc_length = p_openChunk.length();
        int x_cc_length = p_closeChunk.length();
        String x_start = null;
        String x_finish = null;
        if ((x_index = p_line.indexOf(p_openChunk, x_index)) != -1) {
            x_closeIndex = p_line.indexOf(p_closeChunk, (x_index + x_oc_length));
            if (x_closeIndex != -1) {
                x_start = p_line.substring(0, x_index);
                x_finish = p_line.substring(x_closeIndex + x_cc_length, p_line.length());
                StringBuffer x_buf = new StringBuffer(p_line.length() + p_replacement.length());
                x_buf.append(x_start).append(p_replacement).append(x_finish);
                return x_buf.toString();
            }
        }
        return p_line;
    }

    public static String replaceText(String p_line, String p_remove, String p_replacement) {
        int x_index = 0;
        int x_closeIndex = -1;
        int x_remove_length = p_remove.length();
        String x_start = null;
        String x_finish = null;
        if ((x_index = p_line.indexOf(p_remove, x_index)) != -1) {
            x_start = p_line.substring(0, x_index);
            x_finish = p_line.substring(x_index + x_remove_length, p_line.length());
            StringBuffer x_buf = new StringBuffer(p_line.length() + p_replacement.length());
            x_buf.append(x_start).append(p_replacement).append(x_finish);
            return x_buf.toString();
        }
        return p_line;
    }

    public static StringBuffer generatePage(Vector p_template, MultiHashtable p_mht, String p_right, String p_left) throws Exception {
        String[] x_array = null;
        String x_line = null;
        StringBuffer x_buf = new StringBuffer(p_template.size() * 30);
        for (int j = 0; j < p_template.size(); j++) {
            x_line = (String) (p_template.elementAt(j));
            x_array = Parser.regenerateHTML(x_line, p_right, p_left, p_mht);
            for (int k = 0; k < x_array.length; k++) {
                x_buf.append(x_array[k]);
                x_buf.append("\n");
            }
        }
        return x_buf;
    }

    public static String replaceSegments(String p_original, String[] p_old, String[] p_new) {
        String x_latest = p_original;
        int x_start = 0;
        StringBuffer x_buf = null;
        for (int i = 0; i < p_old.length; i++) {
            while (x_start != -1) {
                x_start = x_latest.indexOf(p_old[i], x_start);
                if (x_start != -1) {
                    x_buf = new StringBuffer(x_latest.length() + p_new[i].length());
                    x_buf.append(x_latest);
                    x_buf.replace(x_start, x_start + p_old[i].length(), p_new[i]);
                    x_latest = x_buf.toString();
                }
            }
            x_start = 0;
        }
        return x_latest;
    }

    public static String replaceSegment(String p_original, String p_old, String p_new) {
        String x_latest = p_original;
        int x_start = 0;
        StringBuffer x_buf = null;
        while (x_start != -1) {
            x_start = x_latest.indexOf(p_old, x_start);
            if (x_start != -1) {
                x_buf = new StringBuffer(x_latest.length() + p_new.length());
                x_buf.append(x_latest);
                x_buf.replace(x_start, x_start + p_old.length(), p_new);
                x_latest = x_buf.toString();
            }
        }
        x_start = 0;
        return x_latest;
    }

    /**
  * getMetaKeywordsFromURL gets the meta keywords from a url if they 
  * are present
  * 
  * @param p_url          the url to get the meta keywords from 
  *
  * @return Vector        vector returning the extracted keywords
  *
  * @throws Exception
  */
    public static Vector getMetaKeywordsFromURL(String p_url) throws Exception {
        URL x_url = new URL(p_url);
        URLConnection x_conn = x_url.openConnection();
        InputStreamReader x_is_reader = new InputStreamReader(x_conn.getInputStream());
        BufferedReader x_reader = new BufferedReader(x_is_reader);
        String x_line = null;
        String x_lc_line = null;
        int x_body = -1;
        String x_keyword_list = null;
        int x_keywords = -1;
        String[] x_meta_keywords = null;
        while ((x_line = x_reader.readLine()) != null) {
            x_lc_line = x_line.toLowerCase();
            x_keywords = x_lc_line.indexOf("<meta name=\"keywords\" content=\"");
            if (x_keywords != -1) {
                x_keywords = "<meta name=\"keywords\" content=\"".length();
                x_keyword_list = x_line.substring(x_keywords, x_line.indexOf("\">", x_keywords));
                x_keyword_list = x_keyword_list.replace(',', ' ');
                x_meta_keywords = Parser.extractWordsFromSpacedList(x_keyword_list);
            }
            x_body = x_lc_line.indexOf("<body");
            if (x_body != -1) break;
        }
        Vector x_vector = new Vector(x_meta_keywords.length);
        for (int i = 0; i < x_meta_keywords.length; i++) x_vector.add(x_meta_keywords[i]);
        return x_vector;
    }

    /**
  * getKeywordsFromURL gets the raw text from a url and returns it as a 
  * vector of keywords
  * 
  * @param p_url          the url to get the keywords from 
  *
  * @return Vector        vector returning the extracted keywords
  *
  * @throws Exception
  */
    public static Vector getKeywordsFromURL(String p_url) throws Exception {
        URL x_url = new URL(p_url);
        URLConnection x_conn = x_url.openConnection();
        InputStreamReader x_is_reader = new InputStreamReader(x_conn.getInputStream());
        BufferedReader x_reader = new BufferedReader(x_is_reader);
        String x_line = null;
        String x_lc_line = null;
        Vector x_words = new Vector(1000);
        int x_body = -1;
        while ((x_line = x_reader.readLine()) != null) {
            x_lc_line = x_line.toLowerCase();
            x_body = x_lc_line.indexOf("<body");
            if (x_body != -1) {
                x_line = x_line.substring(x_body + 5);
                break;
            }
        }
        boolean x_status = false;
        int x_end = -1;
        if (x_lc_line == null) {
            System.out.println("No <body start");
            return null;
        }
        do {
            x_lc_line = x_line.toLowerCase();
            x_end = x_lc_line.indexOf("</body>");
            if (x_end != -1) {
                extractOutsideText(x_line.substring(0, x_end), "<", ">", x_words, x_status);
                break;
            }
            x_status = extractOutsideText(x_line, "<", ">", x_words, x_status);
        } while ((x_line = x_reader.readLine()) != null);
        return x_words;
    }

    /**
  * extractOutsideText takes a line and removes all the text that occurs 
  * between the two specified elements (open and close chunks)
  * the idea is that these can be comments or html angle brackets
  * and thus we can remove all formatting data in order to get at the 
  * raw data.  The raw data is added to the p_word vector using the
  * addWords() function.  The boolean return value is used so that chunks
  * spanning multiple lines can be handled.
  * 
  * @param p_line         the line of words
  * @param p_open_chunk   the starting angle bracket or whatever
  * @param p_close_chunk  the closing angle bracket or whatever
  * @param p_words        a vector to receive the words
  * @param p_status       whether there is an unclosed chunk from the previous line
  *
  * @return boolean       whether we closed the chunk
  *
  * @throws Exception
  */
    private static boolean extractOutsideText(String p_line, String p_open_chunk, String p_close_chunk, Vector p_words, boolean p_status) throws Exception {
        int x_index = 0;
        int x_close_index = -1;
        int x_oc_length = p_open_chunk.length();
        int x_cc_length = p_close_chunk.length();
        int x_start = -1;
        int x_end = -1;
        while (p_line.length() > 0) {
            x_start = p_line.indexOf(p_open_chunk);
            if (x_start == -1 || p_status == false) {
                x_start = p_line.indexOf(p_close_chunk);
                if (x_start == -1) {
                    if (p_status == true) addWords(p_line, p_words);
                    return p_status;
                } else {
                    x_end = p_line.indexOf(p_open_chunk, x_start + x_cc_length);
                    if (x_end == -1) {
                        addWords(p_line.substring(x_start + x_cc_length), p_words);
                        return true;
                    } else {
                        addWords(p_line.substring(x_start + x_cc_length, x_end), p_words);
                        p_line = p_line.substring(x_end);
                    }
                }
            } else {
                if (p_status == true) addWords(p_line.substring(0, x_start), p_words);
                x_start = p_line.indexOf(p_close_chunk, x_start + x_oc_length);
                if (x_start == -1) {
                    return false;
                } else {
                    p_line = p_line.substring(x_start + x_cc_length);
                }
            }
        }
        return p_status;
    }

    /**
  * getLinksFromURLFast gets the title from a URL
  * 
  * @param p_url          the url to get the keywords from 
  *
  * @return Vector[]      one Vector of links, and one Vector of associated names
  *
  * @throws Exception
  */
    public static Vector[] getLinksFromURLFast(String p_url) throws Exception {
        timeCheck("getLinksFromURLFast ");
        URL x_url = new URL(p_url);
        URLConnection x_conn = x_url.openConnection();
        InputStreamReader x_is_reader = new InputStreamReader(x_conn.getInputStream());
        BufferedReader x_reader = new BufferedReader(x_is_reader);
        String x_line = null;
        RE e = new RE("(.*/)", RE.REG_ICASE);
        System.out.println("RE: " + e.toString());
        REMatch x_match = e.getMatch(p_url);
        String x_dir = p_url.substring(x_match.getSubStartIndex(1), x_match.getSubEndIndex(1));
        e = new RE("(http://.*?)/?", RE.REG_ICASE);
        x_match = e.getMatch(p_url);
        String x_root = p_url.substring(x_match.getSubStartIndex(1), x_match.getSubEndIndex(1));
        e = new RE("<a href=\"?(.*?)\"?>(.*?)</a>", RE.REG_ICASE);
        System.out.println("RE: " + e.toString());
        Vector x_links = new Vector(100);
        Vector x_texts = new Vector(100);
        StringBuffer x_buf = new StringBuffer(10000);
        REMatch[] x_matches = null;
        timeCheck("starting parsing ");
        while ((x_line = x_reader.readLine()) != null) {
            x_buf.append(x_line);
        }
        String x_page = x_buf.toString();
        String x_link = null;
        x_matches = e.getAllMatches(x_page);
        for (int i = 0; i < x_matches.length; i++) {
            x_link = x_page.substring(x_matches[i].getSubStartIndex(1), x_matches[i].getSubEndIndex(1));
            if (x_link.indexOf("mailto:") != -1) continue;
            x_link = toAbsolute(x_root, x_dir, x_link);
            x_links.addElement(x_link);
            x_texts.addElement(x_page.substring(x_matches[i].getSubStartIndex(2), x_matches[i].getSubEndIndex(2)));
        }
        Vector[] x_result = new Vector[2];
        x_result[0] = x_links;
        x_result[1] = x_texts;
        timeCheck("end parsing ");
        return x_result;
    }

    public static String toAbsolute(String p_root, String p_dir, String p_link) {
        if (p_link.indexOf("http://") != -1) return p_link;
        StringBuffer x_buf = new StringBuffer(p_dir.length() + p_link.length());
        if (!p_link.startsWith("/")) x_buf.append(p_dir).append(p_link); else x_buf.append(p_root).append(p_link);
        return x_buf.toString();
    }

    /**
  * getKeywordsFromURLFast gets the title from a URL
  * 
  * @param p_url          the url to get the keywords from 
  *
  * @return String        the url's title
  *
  * @throws Exception
  */
    public static String getTitleFromURLFast(String p_url) throws Exception {
        URL x_url = new URL(p_url);
        URLConnection x_conn = x_url.openConnection();
        InputStreamReader x_is_reader = new InputStreamReader(x_conn.getInputStream());
        BufferedReader x_reader = new BufferedReader(x_is_reader);
        String x_line = null;
        String x_title_line = null;
        String x_lc_line = null;
        int x_title = -1;
        int x_end = -1;
        while ((x_line = x_reader.readLine()) != null) {
            x_lc_line = x_line.toLowerCase();
            x_title = x_lc_line.indexOf("<title");
            if (x_title != -1) {
                x_end = x_lc_line.indexOf("</title>");
                x_title_line = x_line.substring((x_title + 7), (x_end == -1 ? x_line.length() : x_end));
                break;
            }
        }
        return x_title_line;
    }

    /**
  * getKeywordsFromURLFast gets the raw text from a url and returns it as a 
  * vector of keywords
  * 
  * @param p_url          the url to get the keywords from 
  *
  * @return Vector        vector returning the extracted keywords
  *
  * @throws Exception
  */
    public static Vector getKeywordsFromURLFast(String p_url) throws Exception {
        URL x_url = new URL(p_url);
        URLConnection x_conn = x_url.openConnection();
        InputStreamReader x_is_reader = new InputStreamReader(x_conn.getInputStream());
        BufferedReader x_reader = new BufferedReader(x_is_reader);
        String x_line = null;
        String x_title_line = null;
        String x_lc_line = null;
        Vector x_words = new Vector(1000);
        int x_body = -1;
        int x_title = -1;
        boolean x_bod = false;
        int x_end = -1;
        while ((x_line = x_reader.readLine()) != null) {
            x_lc_line = x_line.toLowerCase();
            x_title = x_lc_line.indexOf("<title");
            if (x_title != -1) {
                x_end = x_lc_line.indexOf("</title>");
                x_title_line = x_line.substring((x_title + 7), (x_end == -1 ? x_line.length() : x_end));
            }
            x_body = x_lc_line.indexOf("<body");
            if (x_body != -1) {
                x_bod = true;
                x_line = x_line.substring(x_body + 5);
                break;
            }
        }
        boolean x_status = false;
        x_end = -1;
        String[] x_temp_words;
        if (x_bod == false) {
            if (x_title_line != null) {
                x_words = new Vector();
                x_temp_words = extractWordsFromSpacedList(x_title_line);
                for (int i = 0; i < x_temp_words.length; i++) x_words.addElement(x_temp_words[i]);
                x_words.addElement(x_title_line);
                return x_words;
            } else {
                return null;
            }
        }
        StringBuffer x_buf = new StringBuffer(35);
        do {
            x_lc_line = x_line.toLowerCase();
            x_end = x_lc_line.indexOf("</body>");
            if (x_end != -1) {
                extractOutsideTextFast(x_line.substring(0, x_end), '<', '>', x_words, x_status, x_buf);
                break;
            }
            x_status = extractOutsideTextFast(x_line, '<', '>', x_words, x_status, x_buf);
        } while ((x_line = x_reader.readLine()) != null);
        if (x_title_line != null) x_words.addElement(x_title_line);
        return x_words;
    }

    /**
  * extractOutsideTextFast tries to get through the text faster and does so 
  * by working on a char array and incrementing along it, dealing with the 
  * chars as they come up, and storing the words as the come along, avoiding
  * multiple processing of the data first for brackets and then for spaces
  * 
  * @param p_line         the line of words
  * @param p_open_chunk   the starting angle bracket or whatever
  * @param p_close_chunk  the closing angle bracket or whatever
  * @param p_words        a vector to receive the words
  * @param p_status       whether there is an unclosed chunk from the previous line
  *
  * @return boolean       whether we closed the chunk
  *
  * @throws Exception
  */
    private static boolean extractOutsideTextFast(String p_line, char p_open, char p_close, Vector p_words, boolean p_status, StringBuffer p_buf) throws Exception {
        int x_index = 0;
        int x_close_index = -1;
        int x_start = 0;
        int x_end = -1;
        char[] x_array_in = p_line.toCharArray();
        while (x_start != -1) {
            if (p_status == false) {
                x_start = scrollUntilClose(x_array_in, x_start);
                if (x_start == -1) return false; else p_status = true;
            } else {
                x_start = transcribeUntilOpen(x_array_in, x_start, p_buf, p_words);
                if (x_start == -1) return true; else p_status = false;
            }
        }
        return p_status;
    }

    private static int scrollUntilClose(char[] p_array_in, int p_start) {
        for (int x_in_pointer = p_start; x_in_pointer < p_array_in.length; x_in_pointer++) {
            if (p_array_in[x_in_pointer] != '>') continue; else {
                x_in_pointer++;
                return x_in_pointer;
            }
        }
        return -1;
    }

    private static int transcribeUntilOpen(char[] p_array_in, int p_start, StringBuffer p_buf, Vector p_words) {
        char x_c;
        for (int x_in_pointer = p_start; x_in_pointer < p_array_in.length; x_in_pointer++) {
            if (p_array_in[x_in_pointer] != '<') {
                x_c = p_array_in[x_in_pointer];
                if (x_c != ' ' && x_c != ',' && x_c != ')' && x_c != '(' && x_c != ';' && x_c != ':' && x_c != '"' && x_c != '&' && x_c != '/' && x_c != '\\' && x_c != '!') p_buf.append(p_array_in[x_in_pointer]); else {
                    if (p_buf.length() > 0) {
                        if (p_buf.charAt(p_buf.length() - 1) == '.') p_buf.deleteCharAt(p_buf.length() - 1);
                        p_words.addElement(p_buf.toString());
                        p_buf.delete(0, p_buf.length());
                    }
                }
                continue;
            } else {
                x_in_pointer++;
                return x_in_pointer;
            }
        }
        return -1;
    }

    /**
  * addWords takes a line of words, removes punctuation, and fills up the
  * vector it has been passed with with the individual words from the line
  * 
  * @param p_line   the line of words
  * @param p_words  an empty vector that will receive the words
  *
  * @throws Exception
  */
    private static void addWords(String p_line, Vector p_words) throws Exception {
        if (p_line.length() == 0) return;
        p_line = p_line.replace('(', ' ');
        p_line = p_line.replace(',', ' ');
        p_line = p_line.replace(')', ' ');
        p_line = p_line.replace(';', ' ');
        p_line = p_line.replace(':', ' ');
        p_line = p_line.replace('"', ' ');
        p_line = p_line.replace('&', ' ');
        p_line = p_line.replace('/', ' ');
        p_line = p_line.replace('\\', ' ');
        p_line = p_line.trim();
        if (p_line.length() == 0) return;
        String[] x_words = Parser.extractWordsFromSpacedList(p_line);
        for (int i = 0; i < x_words.length; i++) {
            if (x_words[i] == null) continue;
            if (x_words[i] == "") continue;
            if (x_words[i] == " ") continue;
            if (x_words[i] == ".") continue;
            if (x_words[i].endsWith(".")) p_words.addElement(x_words[i].substring(0, (x_words[i].length() - 1))); else p_words.addElement(x_words[i]);
        }
    }

    /**
  * sortWords takes a list of words which may have multiple instances, 
  * reorganises them creating FrequencyWord objects which store the 
  * individual word in association with the numnber of times that the 
  * word appeared in the list.  Stop words and format words are removed
  * to create a list that has the most frequently occuring words
  * appearing at the top
  * 
  * @param p_words         the words we are going to sort
  * @param p_stop_words    low meanig words
  * @param p_format_words  html format words
  *
  * @returns FrequencyWord[]      the sorted list of high meaning words
  *
  * @throws Exception
  */
    public static FrequencyWord[] sortWords(Vector p_words, Vector p_stop_words, Vector p_format_words) throws Exception {
        String x_word = null;
        Hashtable x_table = new Hashtable(p_words.size() * 2);
        Integer x_int = null;
        for (int i = 0; i < p_words.size(); i++) {
            x_word = (String) (p_words.elementAt(i));
            x_int = (Integer) (x_table.get(x_word));
            if (x_int == null) x_int = new Integer(1); else x_int = new Integer(x_int.intValue() + 1);
            x_table.put(x_word, x_int);
        }
        String x_temp = null;
        char x_char;
        StringBuffer x_buf = new StringBuffer(20);
        for (int k = 0; k < p_stop_words.size(); k++) {
            x_temp = (String) (p_stop_words.elementAt(k));
            if (x_temp == null || x_temp.equals("")) continue;
            x_table.remove(x_temp);
            x_char = x_temp.charAt(0);
            x_char = Character.toUpperCase(x_char);
            x_buf.delete(0, x_buf.length());
            x_buf.append(x_char).append(x_temp.substring(1, x_temp.length()));
            x_table.remove(x_buf.toString());
        }
        x_temp = null;
        x_buf = new StringBuffer(20);
        for (int k = 0; k < p_format_words.size(); k++) {
            x_temp = (String) (p_format_words.elementAt(k));
            if (x_temp == null || x_temp.equals("")) continue;
            x_temp = x_temp.trim();
            x_table.remove(x_temp);
        }
        x_table.remove("");
        int x_words = x_table.size();
        FrequencyWord[] x_array = new FrequencyWord[x_words];
        FreqComparator x_co = new FreqComparator();
        Enumeration x_keys = x_table.keys();
        for (int z = 0; z < x_words; z++) {
            x_word = (String) (x_keys.nextElement());
            x_int = (Integer) (x_table.get(x_word));
            x_array[z] = new FrequencyWord(x_word, x_int.intValue());
        }
        Arrays.sort(x_array, x_co);
        return x_array;
    }

    public static long timeCheck(String p_message) {
        o_new_time = System.currentTimeMillis();
        long x_diff = o_new_time - o_time;
        System.out.println(p_message + x_diff + "ms");
        o_time = o_new_time;
        return x_diff;
    }

    public static void main(String[] args) {
        try {
            Vector[] x_stuff = Parser.getLinksFromURLFast("http://localhost:8080/index.html");
            Vector x_links = x_stuff[0];
            Vector x_texts = x_stuff[1];
            for (int i = 0; i < x_links.size(); i++) {
                System.out.print("link: " + x_links.elementAt(i));
                System.out.println("; text: " + x_texts.elementAt(i));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
