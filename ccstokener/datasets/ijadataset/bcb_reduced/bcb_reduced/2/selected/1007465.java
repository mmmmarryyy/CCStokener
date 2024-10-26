package mimosa.plugin;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.MessageFormat;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import mimosa.Mimosa;
import mimosa.io.XMLReadException;
import mimosa.io.XMLReader;
import mimosa.io.XMLRecursiveHandler;
import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * A plugin handler is a SAX handler for managing the root "plugin" tag. The plugins file in the root directory
 * contains the list of modules (and their related config files handled by ModuleHandler) which are absolutely necessary for Mimosa to work.
 * All other (open-ended list of) modules can be added by putting them in the plugins directory.
 * 
 * The syntax is simply: <![CDATA[
        <plugins> {<plugin file='filename'/>} </plugins>
]]>
 * 
 * @author Jean-Pierre Muller
 */
public class PluginHandler extends XMLRecursiveHandler {

    private static final String loading = Messages.getString("PluginHandler.0");

    private static final String fileNotFound = Messages.getString("PluginHandler.1");

    private static final String parsingError = Messages.getString("PluginHandler.2");

    private static final String ioError = Messages.getString("PluginHandler.3");

    private static final Class<?> loadingClass = Mimosa.class;

    private static Logger log = Logger.getLogger(PluginHandler.class);

    /**
	 * The empty constructor.
	 */
    public PluginHandler() {
        super();
    }

    /**
	 * @param xmlReader
	 * @param previous
	 * @param atts
	 * @param context
	 * @throws XMLReadException 
	 */
    public PluginHandler(XMLReader xmlReader, XMLRecursiveHandler previous, Attributes atts, Object context) throws XMLReadException {
        initialize(xmlReader, previous, atts, context);
    }

    /**
	 * @see mimosa.io.XMLRecursiveHandler#initialize(mimosa.io.XMLReader, mimosa.io.XMLRecursiveHandler, org.xml.sax.Attributes, java.lang.Object)
	 */
    @Override
    public void initialize(XMLReader xmlReader, XMLRecursiveHandler previous, Attributes atts, Object context) throws XMLReadException {
        super.initialize(xmlReader, previous, atts, context);
    }

    /**
	 * Defines the behavior when a new tag is met.
	 * @param namespace the name space
	 * @param lName the local name
	 * @param qName the qualified name
	 * @param atts the attributes of the tag
	 */
    public void startElement(String namespace, String lName, String qName, Attributes atts) throws SAXException {
        String tag = (lName.equals("")) ? qName : lName;
        if (tag.equals("plugins")) {
            log.info(Messages.getString("PluginHandler.6"));
        }
        if (tag.equals("plugin")) {
            String fileName = atts.getValue("file");
            if (fileName != null) {
                log.info(MessageFormat.format(loading, fileName));
                try {
                    URL url = loadingClass.getResource(fileName);
                    if (url != null) {
                        Reader ir = new InputStreamReader(url.openStream());
                        InputSource is = new InputSource(ir);
                        SAXParserFactory spf = SAXParserFactory.newInstance();
                        SAXParser sp = spf.newSAXParser();
                        ModuleHandler nsh = new ModuleHandler(getReader(), this, null, null);
                        sp.parse(is, nsh);
                        ((Module) nsh.getBuildObject()).load();
                        ir.close();
                    } else {
                        Mimosa.displayErrorMessage(null, MessageFormat.format(fileNotFound, fileName), null);
                    }
                } catch (FileNotFoundException e) {
                    Mimosa.displayErrorMessage(null, MessageFormat.format(fileNotFound, fileName), e);
                } catch (FactoryConfigurationError e) {
                    Mimosa.displayErrorMessage(null, Messages.getString("PluginHandler.9"), e);
                } catch (ParserConfigurationException e) {
                    Mimosa.displayErrorMessage(null, Messages.getString("PluginHandler.9"), e);
                } catch (SAXException e) {
                    Mimosa.displayErrorMessage(null, MessageFormat.format(parsingError, fileName), e);
                } catch (IOException e) {
                    Mimosa.displayErrorMessage(null, MessageFormat.format(ioError, fileName), e);
                }
            }
        }
    }

    /**
	 * Defines the behavior when an end tag is reached.
	 * @param namespace the name space
	 * @param lName the local name
	 * @param qName the qualified name
	 */
    public void endElement(String namespace, String lName, String qName) throws SAXException {
    }

    /**
	 * This is a top call without anything to create.
	 * @see mimosa.io.XMLRecursiveHandler#getBuildObject()
	 */
    public Object getBuildObject() {
        return null;
    }
}
