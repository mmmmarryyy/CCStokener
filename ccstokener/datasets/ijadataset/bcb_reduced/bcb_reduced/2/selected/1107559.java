package net.jwde.processor;

import java.io.IOException;
import java.net.URL;
import net.jwde.object.JWDE;
import net.jwde.util.XMLHelper;
import org.exolab.castor.xml.Unmarshaller;
import org.exolab.castor.mapping.Mapping;
import org.exolab.castor.mapping.MappingException;
import org.exolab.castor.xml.ValidationException;
import org.exolab.castor.xml.MarshalException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class OSCMaxProcessor implements EcommerceProcessor<JWDE> {

    private static EcommerceProcessor<JWDE> instance = new OSCMaxProcessor();

    private OSCMaxProcessor() {
    }

    public static EcommerceProcessor<JWDE> getInstance() {
        return instance;
    }

    public JWDE xml2Object(URL url, URL mappingURL) {
        Mapping mapping = new Mapping();
        Unmarshaller unmarshal = new Unmarshaller(JWDE.class);
        JWDE jwde = null;
        unmarshal.setIgnoreExtraElements(true);
        try {
            mapping.loadMapping(mappingURL);
            unmarshal.setMapping(mapping);
            XMLHelper.validate(url);
            InputSource is = new InputSource(url.openStream());
            jwde = (JWDE) unmarshal.unmarshal(is);
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        } catch (MappingException mapEx) {
            mapEx.printStackTrace();
        } catch (ValidationException valEx) {
            valEx.printStackTrace();
        } catch (MarshalException marEx) {
            marEx.printStackTrace();
        } catch (SAXException saxEx) {
            saxEx.printStackTrace();
        }
        return jwde;
    }

    public JWDE document2Object(Document jwdeDoc, URL mappingURL) {
        Mapping mapping = new Mapping();
        Unmarshaller unmarshal = new Unmarshaller(JWDE.class);
        JWDE jwde = null;
        unmarshal.setIgnoreExtraElements(true);
        try {
            mapping.loadMapping(mappingURL);
            unmarshal.setMapping(mapping);
            jwde = (JWDE) unmarshal.unmarshal(XMLHelper.JDOM2DOM(jwdeDoc));
        } catch (IOException ioEx) {
            ioEx.printStackTrace();
        } catch (MappingException mapEx) {
            mapEx.printStackTrace();
        } catch (ValidationException valEx) {
            valEx.printStackTrace();
        } catch (MarshalException marEx) {
            marEx.printStackTrace();
        } catch (JDOMException jdomEx) {
            jdomEx.printStackTrace();
        }
        return jwde;
    }
}
