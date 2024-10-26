package net.atoom.android.l2;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import android.os.Handler;
import android.os.Message;

public class YouTubeFeedLoader extends Thread {

    private final Handler m_Handler;

    private final String m_YouTubeFeedUrl;

    public YouTubeFeedLoader(String youTubeFeedUrl, Handler youTubeFeedEntryHandler) {
        m_YouTubeFeedUrl = youTubeFeedUrl;
        m_Handler = youTubeFeedEntryHandler;
    }

    @Override
    public void run() {
        YouTubeFeedParserHandler parserHandler = new YouTubeFeedParserHandler();
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            URL url = new URL(m_YouTubeFeedUrl);
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            xr.setContentHandler(parserHandler);
            InputStream is = url.openStream();
            InputSource input = new InputSource(is);
            xr.parse(input);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleEntry(YouTubeFeedEntry youTubeFeedEntry) {
        if (youTubeFeedEntry.getUrl() != null) {
            Message msg = new Message();
            msg.what = L2Activity.CMD_NEW_ENTRY;
            msg.obj = youTubeFeedEntry;
            m_Handler.sendMessageDelayed(msg, 100);
        }
    }

    class YouTubeFeedParserHandler extends DefaultHandler {

        private boolean m_EntryFlag = false;

        private boolean m_TitleFlag = false;

        private boolean m_ContentFlag = false;

        private boolean m_PublishedFlag = false;

        private YouTubeFeedEntry m_Entry;

        public YouTubeFeedParserHandler() {
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (localName.equals("entry")) {
                m_EntryFlag = true;
                m_Entry = new YouTubeFeedEntry();
            } else if (m_EntryFlag) {
                if (uri.equals("http://www.w3.org/2005/Atom") && localName.equals("title")) {
                    m_TitleFlag = true;
                } else if (uri.equals("http://www.w3.org/2005/Atom") && localName.equals("content")) {
                    m_ContentFlag = true;
                } else if (uri.equals("http://search.yahoo.com/mrss/") && localName.equals("content")) {
                    String type = attributes.getValue("type");
                    if (type != null && type.equals("video/3gpp")) {
                        m_Entry.setUrl(attributes.getValue("url"));
                    }
                } else if (uri.equals("http://www.w3.org/2005/Atom") && localName.equals("published")) {
                    m_PublishedFlag = true;
                } else if (uri.equals("http://search.yahoo.com/mrss/") && localName.equals("thumbnail")) {
                    if (m_Entry.getThumbnail() == null) {
                        m_Entry.setThumbnail(attributes.getValue("url"));
                    }
                }
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            if (localName.equals("entry")) {
                m_EntryFlag = false;
                handleEntry(m_Entry);
            }
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            if (m_EntryFlag) {
                StringBuilder b = new StringBuilder();
                for (int i = 0; i < length; i++) {
                    b.append(ch[start + i]);
                }
                if (m_TitleFlag) {
                    m_TitleFlag = false;
                    m_Entry.setTitle(b.toString());
                } else if (m_ContentFlag) {
                    m_ContentFlag = false;
                    m_Entry.setContent(b.toString());
                } else if (m_PublishedFlag) {
                    m_PublishedFlag = false;
                    m_Entry.setPublished(b.toString());
                }
            }
        }
    }
}
