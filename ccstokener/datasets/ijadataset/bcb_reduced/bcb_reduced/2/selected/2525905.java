package ca.uhn.hl7v2.util.tests;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import ca.uhn.hl7v2.Log;
import ca.uhn.hl7v2.llp.MinLLPWriter;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.parser.DefaultXMLParser;
import ca.uhn.hl7v2.parser.GenericParser;
import ca.uhn.hl7v2.parser.Parser;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Status;

/**
 * Collection of HL7 messages.
 * Parses a text file of HL7 formatted messages into a collection
 * of {@link LibraryEntry} entries.
 * The message file consists of HL7 messages and comments. Each 
 * message should be terminated by one or more blank lines.
 * Single and multi-line comments in the message file are ignored.
 * Single line comments begin with "//".  Multi-line comments consist of all 
 * lines between "C" style comment delimiters.
 * @see LibraryEntry
 * 
 * @author Leslie Mann
 * @author Niranjan Sharma niranjan.sharma@med.ge.com This testcase has been
 *         extended for OSGI environment using Junit4 and PAX-Exam.
 * 
 * 
 */
@SuppressWarnings("deprecation")
public class MessageLibrary extends ArrayList {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private final String MULTI_LINE_COMMENT_START = "/*";

    private final String MULTI_LINE_COMMENT_END = "*/";

    private final String SINGLE_LINE_COMMENT = "//";

    private static final char END_MESSAGE = '';

    private static final char START_MESSAGE = '';

    private static final char LAST_CHARACTER = 13;

    private String messageFilePath;

    private String encoding;

    private int numParsingErrors = 0;

    /**
	 * Library with no message encoding specified.
	 * A GenericParser will be used for message parsing.
	 * 
	 * @param path path to HL7 message text file
	 */
    public MessageLibrary(String messageFilePath) {
        super.addAll(setEntries(messageFilePath));
        this.messageFilePath = messageFilePath;
        this.encoding = null;
    }

    /**
	 * Library using a specific message encoding.
	 * Parser appropriate to requested encoding 
	 * will be used for all messages.
	 * 
	 * @param path path to HL7 message text file
	 * @param encoding message encoding to use
	 */
    public MessageLibrary(String messageFilePath, String encoding) {
        super.addAll(setEntries(messageFilePath));
        this.messageFilePath = messageFilePath;
        this.encoding = encoding;
    }

    @SuppressWarnings("deprecation")
    private ArrayList setEntries(String messageFilePath) {
        ArrayList entries = new ArrayList();
        Parser parser;
        if (encoding == "XML") {
            parser = new DefaultXMLParser();
        } else if (encoding == "VB") {
            parser = new PipeParser();
        } else {
            parser = new GenericParser();
        }
        try {
            URL url = MessageLibrary.class.getClassLoader().getResource(messageFilePath);
            URLConnection conn = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuffer msgBuf = new StringBuffer();
            HashMap segments = new HashMap();
            Message msg = null;
            boolean eof = false;
            boolean inComment = false;
            while (!eof) {
                String line = in.readLine();
                System.out.println("Line Read Result************" + line);
                if (line == null) {
                    eof = true;
                } else {
                    if (line.startsWith(SINGLE_LINE_COMMENT)) {
                        continue;
                    } else if (inComment) {
                        if (line.substring(0, 3).trim().startsWith(MULTI_LINE_COMMENT_END)) {
                            inComment = false;
                        }
                        continue;
                    } else if (line.startsWith(MULTI_LINE_COMMENT_START)) {
                        inComment = true;
                        continue;
                    }
                }
                if (!eof && line.length() > 0) {
                    String[] lineSplit = line.split("\\|");
                    String lineKey = lineSplit[0];
                    try {
                        Integer.parseInt(lineSplit[1]);
                        lineKey = lineKey + "|" + lineSplit[1];
                    } catch (NumberFormatException e) {
                        int stop = 1;
                    } catch (ArrayIndexOutOfBoundsException e) {
                        int stop = 1;
                    }
                    segments.put(lineKey, line);
                    msgBuf.append(line + "\r");
                } else if (msgBuf.length() > 0) {
                    String msgStr = msgBuf.toString();
                    try {
                        msg = parser.parse(msgStr);
                        Status.writeStatus("Message:\n" + msgStr);
                    } catch (Exception e) {
                        ++numParsingErrors;
                        Status.writeStatus("Warning: Parsing errors with message:\n" + msgStr);
                        Log.tryToLog(e, "Parsing errors with message:\n" + msgStr);
                    }
                    entries.add(new LibraryEntry(new String(msgStr), new HashMap(segments), msg));
                    msgBuf.setLength(0);
                    segments.clear();
                    msg = null;
                }
            }
            in.close();
        } catch (FileNotFoundException e) {
            Status.writeStatus("Warning: Message file not found: " + messageFilePath);
            Log.tryToLog(e, "Message file not found " + messageFilePath);
        } catch (IOException e) {
            Status.writeStatus("Warning: Unable to open message file: " + messageFilePath);
            Log.tryToLog(e, "Unable to open message file: " + messageFilePath);
        }
        return entries;
    }

    /**
	 * Returns the message library entry strings as a 
	 * Minimal Lower Layer Protocol encoded stream
	 * (same output as would be generated by {@link MinLLPWriter})
	 * 
	 * @see MinLLPWriter
	 * @return a stream of HL7 messages
	 */
    public ByteArrayInputStream getAsByteArrayInputStream() {
        Iterator msgs = this.iterator();
        StringBuffer inputMessages = new StringBuffer();
        while (msgs.hasNext()) {
            LibraryEntry entry = (LibraryEntry) msgs.next();
            String temp = entry.messageString();
            inputMessages.append(START_MESSAGE + entry.messageString() + END_MESSAGE + LAST_CHARACTER);
        }
        return new ByteArrayInputStream(inputMessages.toString().getBytes());
    }

    /**
	 * Returns the path of the HL7 message text file.
	 * @return message file path
	 */
    public String getMessageFilePath() {
        return messageFilePath;
    }

    /**
	 * Returns the parsingErrors.
	 * @return number of parsing errors
	 */
    public int getNumParsingErrors() {
        return numParsingErrors;
    }

    /**
	 * Returns the parserType.
	 * @return parser encoding used
	 */
    public String getParserType() {
        return encoding;
    }
}
