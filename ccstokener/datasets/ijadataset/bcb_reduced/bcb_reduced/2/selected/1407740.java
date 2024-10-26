package genorm;

import java.io.*;
import java.net.*;
import java.util.*;

public class TextReplace {

    private String m_token;

    private String m_tokenDoc;

    public TextReplace(String fileName, String token) throws IOException {
        this(new FileInputStream(fileName), token);
    }

    public TextReplace(URL url, String token) throws IOException {
        this(url.openStream(), token);
    }

    public TextReplace(InputStream iStream, String token) throws IOException {
        InputStreamReader isr = new InputStreamReader(iStream);
        StringBuilder sb = new StringBuilder(128);
        int inputChar;
        while ((inputChar = isr.read()) != -1) sb.append((char) inputChar);
        isr.close();
        m_tokenDoc = sb.toString();
        m_token = token;
    }

    public TextReplace(String text, String token, boolean bogus) {
        m_tokenDoc = text;
        m_token = token;
    }

    public String replaceTextWith(Map replacements) {
        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(m_tokenDoc, m_token);
        String value;
        String s;
        int i = 0;
        boolean tag = false;
        while (st.hasMoreTokens()) {
            s = st.nextToken();
            if ((value = (String) replacements.get(s)) != null) {
                tag = true;
                sb.append(value);
            } else {
                if ((i != 0) && (!tag)) sb.append(m_token);
                sb.append(s);
                tag = false;
            }
            i++;
        }
        return (sb.toString());
    }
}
