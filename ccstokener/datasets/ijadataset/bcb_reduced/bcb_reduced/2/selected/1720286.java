package ws.prova.mule.impl;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transformers.AbstractTransformer;
import org.mule.umo.transformer.TransformerException;
import ws.prova.RMessage;

/**
 * <code>RuleML2ProvaTranslator</code> translate a Reaction RuleML message into a Prova message
 * The translator uses the specified XSLT.
 * 
 * If the RuleML message can not be translated, the original RuleML message will be returned
 * 
 * @author <a href="mailto:adrian.paschke@gmx.de">Adrian Paschke</a>
 * @version 
 */
public class RuleML2ProvaTranslator extends AbstractTransformer {

    protected static transient Log logger = LogFactory.getLog(RuleML2ProvaTranslator.class);

    /**
     * Serial version
     */
    private static final long serialVersionUID = -408128452488674866L;

    private TransformerFactory tFactory = TransformerFactory.newInstance();

    private InputStream is = null;

    private Source xmlSource = null;

    private Source xslSource = null;

    private Transformer transformer = null;

    private String xslt = "rrml2prova.xsl";

    public RuleML2ProvaTranslator() {
        super();
        this.registerSourceType(Object.class);
    }

    public RuleML2ProvaTranslator(String _xslt) {
        super();
        this.registerSourceType(Object.class);
        xslt = _xslt;
    }

    public void setXSLT(String _xslt) {
        xslt = _xslt;
    }

    public Object doTransform(Object src, String encoding) throws TransformerException {
        if (src instanceof String) {
            try {
                InputStream in = null;
                try {
                    in = Thread.currentThread().getContextClassLoader().getResourceAsStream(xslt);
                    if (in == null) {
                        in = new FileInputStream(xslt);
                    }
                } catch (Exception ex) {
                    URL url = new URL(xslt);
                    in = url.openStream();
                }
                if (in == null) {
                    logger.error("Can not load XSLT");
                    return src;
                }
                xslSource = new StreamSource(in);
                String message = src.toString();
                if (message.indexOf("'") != -1) {
                    String query = message.substring(message.indexOf("'"), message.lastIndexOf("'") + 1);
                    String encodedQuery = URLEncoder.encode(query.substring(1, query.length() - 1), "UTF-8");
                    message = message.replace(query, encodedQuery);
                }
                if (message != null && message.length() > 0) is = new ByteArrayInputStream(message.getBytes());
                if (is == null) {
                    logger.error("XML input message invalid");
                    return src;
                }
                xmlSource = new StreamSource(is);
                transformer = tFactory.newTransformer(xslSource);
                StringWriter provaMessage = new StringWriter();
                transformer.transform(xmlSource, new StreamResult(provaMessage));
                String output = provaMessage.toString();
                String conv_id = output.substring(output.indexOf(",") + 1, output.indexOf("]"));
                output = output.replaceFirst(conv_id, "%1");
                String protocol = output.substring(output.indexOf("],") + 2).split(",")[0];
                output = output.replace(protocol + ",", "");
                String predicate = output.split(",")[4].substring(1);
                if (predicate.indexOf("=") == -1) {
                    if (predicate.indexOf("[") != -1) predicate = predicate.substring(0, predicate.indexOf("["));
                    output = output.replaceFirst(predicate, "%0");
                } else {
                    String subst = output.substring(output.indexOf(predicate) - 1, output.length() - 1);
                    predicate = "substitutions";
                    String[] bindings = subst.split("=");
                    String args = "";
                    for (int i = 0; i < bindings.length; i++) {
                        if (bindings[i].indexOf(".") != -1) args = args + "," + bindings[i].substring(0, bindings[i].indexOf("."));
                    }
                    output = output.replace(subst, "[%0" + args + "]");
                }
                List<Object> objects = new ArrayList<Object>();
                objects.add(predicate);
                objects.add(conv_id);
                RMessage rmsg = new RMessage(output, objects.toArray());
                rmsg.setProtocol(protocol.substring(1, protocol.lastIndexOf("\"")));
                return rmsg;
            } catch (Exception e) {
                logger.error("Error during translation of RuleML message into RMessage");
                logger.error(e);
                return src;
            }
        }
        return src;
    }
}
