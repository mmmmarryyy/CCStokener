package org.orbeon.oxf.processor.serializer;

import org.dom4j.Document;
import org.orbeon.oxf.common.OXFException;
import org.orbeon.oxf.pipeline.api.PipelineContext;
import org.orbeon.oxf.processor.ProcessorImpl;
import org.orbeon.oxf.processor.ProcessorInput;
import org.orbeon.oxf.processor.generator.URLGenerator;
import org.orbeon.oxf.resources.ResourceManagerWrapper;
import org.orbeon.oxf.resources.URLFactory;
import org.orbeon.oxf.resources.oxf.Handler;
import org.orbeon.oxf.xml.SAXStore;
import org.orbeon.oxf.xml.TransformerUtils;
import org.orbeon.oxf.xml.XPathUtils;
import org.xml.sax.ContentHandler;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

public class URLSerializer extends ProcessorImpl {

    public URLSerializer() {
        addInputInfo(new org.orbeon.oxf.processor.ProcessorInputOutputInfo(INPUT_CONFIG, URLGenerator.URL_NAMESPACE_URI));
        addInputInfo(new org.orbeon.oxf.processor.ProcessorInputOutputInfo(INPUT_DATA));
    }

    public void start(PipelineContext context) {
        try {
            ProcessorInput dataInput = getInputByName(INPUT_DATA);
            SAXStore store = new SAXStore();
            dataInput.getOutput().read(context, store);
            URL url = (URL) readCacheInputAsObject(context, getInputByName(INPUT_CONFIG), new org.orbeon.oxf.processor.CacheableInputReader() {

                public Object read(PipelineContext context, ProcessorInput input) {
                    try {
                        Document doc = readInputAsDOM4J(context, input);
                        String url = XPathUtils.selectStringValueNormalize(doc, "/config/url");
                        return URLFactory.createURL(url.trim());
                    } catch (MalformedURLException e) {
                        throw new OXFException(e);
                    }
                }
            });
            if (Handler.PROTOCOL.equals(url.getProtocol())) {
                ContentHandler handler = ResourceManagerWrapper.instance().getWriteContentHandler(url.getFile());
                store.replay(handler);
            } else {
                URLConnection conn = url.openConnection();
                conn.setDoOutput(true);
                conn.connect();
                OutputStream os = conn.getOutputStream();
                TransformerHandler identity = TransformerUtils.getIdentityTransformerHandler();
                identity.setResult(new StreamResult(os));
                store.replay(identity);
                os.close();
                if (conn instanceof HttpURLConnection) ((HttpURLConnection) conn).disconnect();
            }
        } catch (Exception e) {
            throw new OXFException(e);
        }
    }
}
