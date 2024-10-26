package acmsoft.util;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * This class is designed to parse Windows-like .ini files.
 */
public class IniParser {

    protected static final String DEFAULT_TOPIC_NAME = "";

    private static final String ENCODING_MARKER = ";;Encoding=";

    private Hashtable m_hsTopics = new Hashtable();

    private long m_lLastModified = -1;

    /**
     * Whether or not the parser should ignore topics and keys case
     */
    private boolean m_bIgnoreCase = true;

    /**
     * If loaded from file holds original file reference
     */
    private File m_file = null;

    /**
     * If loaded from URL holds original URL reference
     */
    private URL m_url = null;

    /**
     * Contains name of the encoding used to parse input stream (if known) or null
     */
    private String m_strSourceEncoding = null;

    /**
     * Whether source encoding was detected
     */
    private boolean m_bEncodingDetected = false;

    private static Reader getInputStreamReader(InputStream is, String strEncoding) throws IOException {
        if (strEncoding == null) {
            return new InputStreamReader(is);
        } else {
            return new InputStreamReader(is, strEncoding);
        }
    }

    private static boolean isUrl(String strIniPath) {
        return strIniPath.indexOf("://") > 0;
    }

    public IniParser() {
    }

    public IniParser(String strIniPath, String strEncoding) throws IOException {
        init(strIniPath, strEncoding);
    }

    public IniParser(File file) throws IOException {
        this(file, (String) null);
    }

    public IniParser(File file, String strEncoding) throws IOException {
        if (file == null) {
            throw new NullPointerException();
        }
        setSource(file);
        init(new FileInputStream(file), strEncoding, file.lastModified());
    }

    public IniParser(InputStream is, String strEncoding) throws IOException {
        init(is, strEncoding, 0);
    }

    public IniParser(Reader r) throws IOException {
        init(r, 0);
    }

    public void init(String strIniPath, String strEncoding) throws IOException {
        InputStream is;
        long lLastModified;
        if (isUrl(strIniPath)) {
            URL url = new URL(strIniPath);
            URLConnection ucon = url.openConnection();
            lLastModified = ucon.getLastModified();
            is = ucon.getInputStream();
            setSource(url);
        } else {
            File file = new File(strIniPath);
            lLastModified = file.lastModified();
            is = new FileInputStream(file);
            setSource(file);
        }
        init(is, strEncoding, lLastModified);
    }

    private static final int CHAR_BUFFER_SIZE = 4096;

    private void init(InputStream is, String strEncoding, long lLastModified) throws IOException {
        Reader reader = null;
        boolean bSuccess = false;
        try {
            m_bEncodingDetected = false;
            if (strEncoding == null) {
                BufferedInputStream bis = new BufferedInputStream(is, 3 * CHAR_BUFFER_SIZE);
                bis.mark(3 * CHAR_BUFFER_SIZE);
                InputStreamReader isr = new InputStreamReader(bis);
                BufferedReader br = new BufferedReader(isr, CHAR_BUFFER_SIZE);
                String strFirstLine = br.readLine();
                strEncoding = detectEncoding(strFirstLine);
                bis.reset();
                is = bis;
                m_bEncodingDetected = true;
            }
            m_strSourceEncoding = strEncoding;
            reader = new BufferedReader(getInputStreamReader(is, strEncoding));
            bSuccess = true;
        } finally {
            if (!bSuccess && is != null) {
                is.close();
            }
        }
        init(reader, lLastModified);
    }

    private void init(Reader reader, long lLastModified) throws IOException {
        try {
            m_lLastModified = lLastModified;
            if (m_lLastModified == 0) {
                m_lLastModified = System.currentTimeMillis();
            }
            BufferedReader br;
            if (reader instanceof BufferedReader) {
                br = (BufferedReader) reader;
            } else {
                br = new BufferedReader(reader);
            }
            clear();
            parse(br);
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

    public void setIgnoreCase(boolean bIgnoreCase) {
        m_bIgnoreCase = bIgnoreCase;
    }

    public boolean ignoresCase() {
        return m_bIgnoreCase;
    }

    protected void setSource(File file) {
        m_url = null;
        m_file = file;
    }

    protected void setSource(URL url) {
        m_url = url;
        m_file = null;
    }

    public boolean isSourceModified() {
        return (m_lLastModified < getSourceModifiedTime());
    }

    public long getSourceModifiedTime() {
        if (m_file != null) {
            return m_file.lastModified();
        } else if (m_url != null) {
            try {
                URLConnection ucon = m_url.openConnection();
                return ucon.getLastModified();
            } catch (IOException ex) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    public void reload() throws IOException {
        String strEncoding = m_strSourceEncoding;
        if (m_bEncodingDetected) {
            strEncoding = null;
        }
        if (m_file != null) {
            init(new FileInputStream(m_file), strEncoding, m_file.lastModified());
        } else if (m_url != null) {
            URLConnection ucon = m_url.openConnection();
            long lLastModified = ucon.getLastModified();
            InputStream is = ucon.getInputStream();
            init(is, strEncoding, lLastModified);
        }
    }

    protected String detectEncoding(String strFirstLine) {
        String strEncoding = null;
        if (strFirstLine != null) {
            strFirstLine = strFirstLine.trim();
            if (strFirstLine.startsWith(ENCODING_MARKER)) {
                strEncoding = strFirstLine.substring(ENCODING_MARKER.length());
            }
        }
        return strEncoding;
    }

    public long getLastModified() {
        return m_lLastModified;
    }

    private String readNextLine(BufferedReader reader) throws IOException {
        String strLine;
        do {
            strLine = reader.readLine();
            if (strLine == null) {
                break;
            }
        } while (strLine.trim().length() == 0);
        return strLine;
    }

    /**
     * Removes all registered topics
     */
    protected void clear() {
        Enumeration topicsEnum = topicNames();
        while (topicsEnum.hasMoreElements()) {
            String strTopicName = (String) topicsEnum.nextElement();
            removeTopic(strTopicName);
        }
    }

    /**
     * Parses content of given reader, adds information to currently loaded topics
     * @param reader
     * @throws IOException
     */
    protected void parse(BufferedReader reader) throws IOException {
        String strTopicName = DEFAULT_TOPIC_NAME;
        Properties topic = createTopic(strTopicName);
        for (String strLine = readNextLine(reader); strLine != null; strLine = readNextLine(reader)) {
            String strLineTrimmed = strLine.trim();
            if (strLineTrimmed.charAt(0) == '[' && strLineTrimmed.endsWith("]")) {
                strTopicName = strLineTrimmed.substring(1, strLineTrimmed.length() - 1);
                if (m_bIgnoreCase) {
                    strTopicName = strTopicName.toUpperCase();
                }
                topic = getTopic(strTopicName);
                if (topic == null) {
                    topic = createTopic(strTopicName);
                }
            } else {
                if (strLineTrimmed.startsWith(";")) {
                    continue;
                }
                processLine(strLine, strTopicName, topic);
            }
        }
        topic = getTopic(DEFAULT_TOPIC_NAME);
        if (topic.size() == 0) {
            removeTopic(DEFAULT_TOPIC_NAME);
        }
    }

    /**
     * Processes single line of file.
     * Default implementation breaks it into key=value parts and calls putValue()
     * @param strLine
     * @param strTopicName
     * @param topic
     */
    protected void processLine(String strLine, String strTopicName, Properties topic) {
        int posEq = strLine.indexOf('=');
        if (posEq > 0) {
            String strKey = strLine.substring(0, posEq).trim();
            if (m_bIgnoreCase) {
                strKey = strKey.toUpperCase();
            }
            String strValue = strLine.substring(posEq + 1);
            putValue(strTopicName, topic, strKey, strValue);
        }
    }

    protected void putValue(String strTopicName, Properties topic, String strKey, String strValue) {
        topic.put(strKey, strValue);
    }

    protected Properties createTopic(String strTopicName) {
        Properties prop = new Properties();
        m_hsTopics.put(strTopicName, prop);
        return prop;
    }

    protected void removeTopic(String strTopicName) {
        m_hsTopics.remove(strTopicName);
    }

    public Properties getTopic(String strTopicName) {
        return (Properties) m_hsTopics.get(strTopicName);
    }

    public int getTopicsCount() {
        return m_hsTopics.size();
    }

    public Enumeration topicNames() {
        return m_hsTopics.keys();
    }

    public void merge(IniParser other, boolean bOverWriteExisting) {
        if (other == null) {
            return;
        }
        Enumeration topicsEnum = other.topicNames();
        while (topicsEnum.hasMoreElements()) {
            String strTopicName = (String) topicsEnum.nextElement();
            Properties topic = other.getTopic(strTopicName);
            mergeTopic(strTopicName, topic, bOverWriteExisting);
        }
    }

    public void mergeTopic(String strTopicName, Properties otherTopic, boolean bOverWriteExisting) {
        Properties topic = getTopic(strTopicName);
        if (topic == null) {
            topic = createTopic(strTopicName);
        }
        Enumeration propertiesEnum = otherTopic.propertyNames();
        while (propertiesEnum.hasMoreElements()) {
            String strPropName = (String) propertiesEnum.nextElement();
            if ((getProperty(strTopicName, strPropName) == null) || bOverWriteExisting) {
                putValue(strTopicName, topic, strPropName, (String) otherTopic.get(strPropName));
            }
        }
    }

    public String getProperty(String strTopic, String strKey) {
        if (m_bIgnoreCase) {
            strTopic = strTopic.toUpperCase();
            strKey = strKey.toUpperCase();
        }
        String strResult = null;
        Properties topic = getTopic(strTopic);
        if (topic != null) {
            strResult = topic.getProperty(strKey);
        }
        return strResult;
    }

    public String getProperty(String strTopic, String strKey, String strDefaultValue) {
        String strResult = getProperty(strTopic, strKey);
        if (strResult == null) {
            strResult = strDefaultValue;
        }
        return strResult;
    }
}
