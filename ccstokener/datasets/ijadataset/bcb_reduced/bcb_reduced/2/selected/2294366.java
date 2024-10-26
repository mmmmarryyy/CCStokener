package de.thesuntoucher.jigg.net;

import java.net.URL;
import javax.xml.parsers.SAXParserFactory;
import de.thesuntoucher.jigg.JiggException;
import de.thesuntoucher.jigg.handler.ResponseHandler;

public class DefaultHttpClient implements JiggHttpClient {

    @Override
    public void fetch(URL url, ResponseHandler handler) {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.newSAXParser().parse(url.openStream(), handler);
        } catch (Exception e) {
            throw new JiggException(e);
        }
    }
}
