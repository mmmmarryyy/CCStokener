package witaDataCollect;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class TextDocumentParser {

    private String m_sOriginalDocument = "";

    private String m_sDocumentLC = "";

    private int m_iCursor = 0;

    private int m_iSelector = 0;

    public void setDocument(String sDocument) {
        m_sOriginalDocument = sDocument;
        m_sDocumentLC = sDocument.toLowerCase();
        m_iCursor = 0;
        m_iSelector = 0;
    }

    public String getDocument() {
        return m_sOriginalDocument;
    }

    public void loadFromURL(URL urlToOpen) throws IOException {
        InputStream isConn = urlToOpen.openStream();
        StringBuilder sbBuffer = new StringBuilder();
        int iNextByte = isConn.read();
        while (iNextByte != -1) {
            sbBuffer.append((char) iNextByte);
            iNextByte = isConn.read();
        }
        isConn.close();
        m_sOriginalDocument = sbBuffer.toString();
        m_sDocumentLC = m_sOriginalDocument.toLowerCase();
        m_iCursor = 0;
        m_iSelector = 0;
    }

    public void reset() {
        m_iCursor = 0;
        m_iSelector = 0;
    }

    public int advanceCursor(String sSubstring) {
        sSubstring = sSubstring.toLowerCase();
        m_iCursor = m_sDocumentLC.indexOf(sSubstring, m_iCursor);
        if (m_iCursor == -1) {
            m_iCursor = 0;
            m_iSelector = 0;
            return -1;
        }
        m_iCursor += sSubstring.length();
        m_iSelector = m_iCursor;
        return m_iCursor;
    }

    public int advanceSelector(String sSubstring) {
        sSubstring = sSubstring.toLowerCase();
        m_iSelector = m_sDocumentLC.indexOf(sSubstring, m_iSelector);
        if (m_iSelector == -1) {
            m_iCursor = 0;
            m_iSelector = 0;
            return -1;
        }
        return m_iSelector;
    }

    public String getSelectedText() {
        return m_sOriginalDocument.substring(m_iCursor, m_iSelector);
    }
}
