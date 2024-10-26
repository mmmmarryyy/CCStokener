package com.testonica.kickelhahn.core.formats.common;

import java.net.URL;
import java.io.File;
import java.io.Reader;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * This class is used for reading and processing the streams of textual format.
 * It contains useful functions for reading significant lines, and separating
 * comments.
 * 
 * @see #setComments(String[])
 * 
 * @author Sergei Devadze
 */
public class FormatReader extends BufferedReader {

    /** Last read line */
    private String string = null;

    /** Comments associated with last read string */
    private String comment = new String();

    /** Number of lines that were read till this moment */
    private int count = 0;

    /** Identifiers of comments */
    private String[] comments = new String[0];

    /**
     * Opens a stream to given file and creates a format reader of this stream.
     * 
     * @param file
     *            file to open
     */
    public FormatReader(File file) throws IOException {
        this(new FileReader(file));
    }

    /**
     * Opens a stream to given URL and creates a format reader of this stream.
     * 
     * @param url
     *            URL to open
     */
    public FormatReader(URL url) throws IOException {
        this(new InputStreamReader(url.openStream()));
    }

    /**
     * Creates a format reader
     * 
     * @param r
     *            reader
     * @param comment
     *            strings to identify comments
     */
    public FormatReader(Reader r) {
        super(r);
    }

    /**
     * Reads next line and returns it
     * 
     * @return next line, or <code>null</code> if the end of file was reached
     * @throws IOException
     *             if there were I/O errors during reading
     */
    public String readLine() throws IOException {
        count++;
        return super.readLine();
    }

    /**
     * Reads next significant line and returns it. Significant line is the line
     * that contains any significant characters (not whitespaces), excluding
     * lines that contain only comments
     * 
     * @return read line, or <code>null</code> if the end of file is reached
     * @throws IOException
     *             if errors I/O error occurs during the reading.
     */
    public String readSignificantLine() throws IOException {
        this.comment = new String();
        do {
            string = readLine();
            if (string == null) break;
            int comment1 = Integer.MAX_VALUE, comment2 = 0;
            for (int i = 0; i < comments.length; i++) {
                int index = string.indexOf(comments[i]);
                if ((index < comment1) && (index >= 0)) {
                    comment1 = index;
                    comment2 = index + comments[i].length();
                }
            }
            if ((comment1 < string.length()) && (comment1 >= 0)) {
                if (comment.length() > 0) comment += "\n";
                this.comment += string.substring(comment2).trim();
                string = string.substring(0, comment1);
            }
            string = string.trim();
        } while (string.length() == 0);
        return string;
    }

    /**
     * Sets sequence of characters that indicates comment. All the charactes in
     * the line after comment will be ignored by this reader.
     * 
     * @param s
     *            string that indicate comment
     */
    public void setComment(String s) {
        setComments(new String[] { s });
    }

    /**
     * Sets array of strings that will indicate comment. All the charactes in
     * the line after any of comment string will be ignored by this reader.
     * 
     * @param s
     *            set of string that indicate comment
     */
    public void setComments(String[] s) {
        comments = new String[s.length];
        System.arraycopy(s, 0, comments, 0, s.length);
    }

    /**
     * Returns last read line. The line must be read by calling
     * <code>readSignificatLine()</code> or <code>readLine()</code> methods.
     * 
     * @return last read line, or <code>null</code> if no lines were read or
     *         EOF is reached
     */
    public String getLine() {
        return string;
    }

    /**
     * Get comment associated with previously read line
     * 
     * @return comment string
     */
    public String getComment() {
        return comment;
    }

    /**
     * Returns the total number of lines were read. All the lines are taken into
     * account (including skipped non-significant lines).
     * 
     * @return number of lines that were read
     */
    public int getLineCount() {
        return count;
    }

    /**
     * Checks if the end of file (EOF) is reached by the reader
     * 
     * @return <code>true</code> if EOF is reached
     */
    public boolean isEOF() {
        return ((string == null) && (count > 0));
    }
}
