package com.once.server.block;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.xml.sax.InputSource;
import com.once.server.SingletonKeeper;
import com.once.server.security.SecurityFactory;

/**
 * The DefaultBlock acts as the core for any block. <br />
 * It provides all of the naming, management and default behaviours that blocks require.
 */
public class DefaultBlock extends AbstractBlock {

    private static final Logger m_logger = Logger.getLogger(DefaultBlock.class);

    private static final String strEmpty = "";

    private String strName = null;

    private String strQualification = null;

    private Document docBlock = null;

    private static final String ERROR_BEGIN = "\t<error>\n\t\t";

    private static final String ERROR_END = "\n\t</error>\n";

    private SingletonKeeper skpFunctor = null;

    private String errors;

    protected SingletonKeeper getSingleton() {
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getSingleton() - start");
        }
        SingletonKeeper returnSingletonKeeper = ((skpFunctor == null) ? (skpFunctor = new SingletonKeeper()) : skpFunctor);
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getSingleton() - end - return value=" + returnSingletonKeeper);
        }
        return returnSingletonKeeper;
    }

    /**
     * Provides access to the logging mechanism of this DefaultBlock. The logging mechanism is used to record errors, flags,
     * and other status information.
     * 
     * @return The logging mechanism of this DefaultBlock as a Logger.
     * @see Logger
     */
    protected Logger getLoggingMechanism() {
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getLoggingMechanism() - start");
        }
        Logger returnLogger = (m_logger);
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getLoggingMechanism() - end - return value=" + returnLogger);
        }
        return returnLogger;
    }

    /**
     * Provides access to DefaultBlock's definition of an empty String. This definition is used in comparisons to determine
     * whether another String is empty.
     * 
     * @return A zero-byte long String.
     * @see String
     */
    protected String getEmptyString() {
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getEmptyString() - start");
        }
        String returnString = (strEmpty);
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getEmptyString() - end - return value=" + returnString);
        }
        return returnString;
    }

    /**
     * Creates and initialises a new DefaultBlock.
     * 
     * @return A new DefaultBlock.
     */
    public DefaultBlock() {
    }

    /**
     * Sets the name of a DefaultBlock to the value specified by the String "strNewName".
     * 
     * @param strNewName
     *                A new name for a DefaultBlock.
     * @return If the name of the DefaultBlock was set to the value specified by the String "strNewName", this method
     *         returns True. Otherwise, this method returns False.
     * @see String
     */
    public boolean setName(String strNewName) {
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("setName(String strNewName=" + strNewName + ") - start");
        }
        if (!(strNewName == null || strEmpty.equals(strName))) {
            strName = strNewName;
            boolean returnboolean = (true);
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("setName() - end - return value=" + returnboolean);
            }
            return returnboolean;
        } else {
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("setName() - Could not set a new name.");
            }
            return (false);
        }
    }

    /**
     * Provides access to the name of a DefaultBlock.
     * 
     * @return The name of the DefaultBlock.
     */
    public String getName() {
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getName() - start");
        }
        String returnString = (strName);
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getName() - end - return value=" + returnString);
        }
        return returnString;
    }

    /**
     * Sets the qualification of the Block. <br />
     * 
     * @param fQualification
     *                The qualification to be assigned to the block.
     * @return A success indicator.
     */
    public boolean setQualification(String strNewQualification) {
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("setQualification(String strNewQualification=" + strNewQualification + ") - start");
        }
        if (!(strNewQualification == null || strEmpty.equals(strNewQualification))) {
            strQualification = strNewQualification;
            boolean returnboolean = (true);
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("setQualification() - end - return value=" + returnboolean);
            }
            return returnboolean;
        } else {
            boolean returnboolean = (false);
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("setQualification() - end - return value=" + returnboolean);
            }
            return returnboolean;
        }
    }

    /**
     * Retreives the qualification of the block. <br />
     * 
     * @return The qualification of the block.
     */
    public String getQualification() {
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getQualification() - start");
        }
        String returnString = (strQualification);
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getQualification() - end - return value=" + returnString);
        }
        return returnString;
    }

    /**
     * Retreives the qualified name of the block. <br />
     * This will fail if either of name or qualification have not yet been set or are empty.
     * 
     * @return The fully qualified name of the block. Null if failed.
     */
    public String getQualifiedName() {
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getQualifiedName() - start");
        }
        if (!((strName == null || strEmpty.equals(strName)) || (strQualification == null || strEmpty.equals(strQualification)))) {
            String returnString = (strQualification + strName);
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("getQualifiedName() - end - return value=" + returnString);
            }
            return returnString;
        } else {
            String returnString = (null);
            if (m_logger.isDebugEnabled()) {
                m_logger.debug("getQualifiedName() - end - return value=" + returnString);
            }
            return returnString;
        }
    }

    private Document setBlockFromURL(URL urlSource) {
        String strSource = "";
        try {
            URLConnection conn = urlSource.openConnection();
            InputStreamReader reader = new InputStreamReader(conn.getInputStream(), "UTF-8");
            StringBuffer sb = new StringBuffer();
            int ch = reader.read();
            while (ch != -1) {
                sb.append((char) ch);
                ch = reader.read();
            }
            strSource = sb.toString();
            reader.close();
        } catch (IOException ex) {
            m_logger.error("setBlockFromURL(URL)", ex);
            docBlock = null;
            return null;
        }
        return setBlock(strSource);
    }

    public Document setBlockFromFile(String strSource) {
        URL urlBlockLocation = null;
        try {
            urlBlockLocation = new URL("file:" + getSingleton().getBlockManager().resolveBlockName(strSource));
        } catch (MalformedURLException excDie) {
            m_logger.error("setBlockFromFile(String)", excDie);
            docBlock = null;
            return null;
        }
        return setBlockFromURL(urlBlockLocation);
    }

    public Document setBlock(String strSource) {
        SAXReader sxrWorld = null;
        BlockErrorHandler beh;
        sxrWorld = new SAXReader();
        try {
            InputSource is = new InputSource(new StringReader(SecurityFactory.getInstance().getLicenseManager().dequoteBlockData(strSource)));
            is.setEncoding("UTF-8");
            docBlock = sxrWorld.read(is);
            beh = new BlockErrorHandler();
            sxrWorld.setErrorHandler(beh);
            errors = beh.getErrors();
        } catch (DocumentException excDie) {
            m_logger.error("setBlock(String)", excDie);
            errors = excDie.getMessage() + "\n\nXML dump:\n\n" + strSource;
        }
        return docBlock;
    }

    public String getErrors() {
        return (errors);
    }

    public void setBlock(Document docSource) {
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("setBlock(Document docSource=" + docSource + ") - start");
        }
        docBlock = docSource;
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("setBlock() - end");
        }
    }

    public void setBlockFromError(String strError) {
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("setBlockFromError(String strError=" + strError + ") - start");
        }
        setBlock(ERROR_BEGIN + strError + ERROR_END);
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("setBlockFromError() - end");
        }
    }

    public Document getBlockAsDocument() {
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getBlockAsDocument() - start");
        }
        Document returnDocument = (docBlock);
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getBlockAsDocument() - end - return value=" + returnDocument);
        }
        return returnDocument;
    }

    public String getBlockAsString() {
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getBlockAsString() - start");
        }
        String strOutput = "";
        if (docBlock != null) {
            strOutput = docBlock.asXML();
            if (strOutput != null) {
                String returnString = (strOutput.substring(strOutput.indexOf("\n") + 1, strOutput.length()));
                if (m_logger.isDebugEnabled()) {
                    m_logger.debug("getBlockAsString() - end - return value=" + returnString);
                }
                return returnString;
            }
        }
        String returnString = (null);
        if (m_logger.isDebugEnabled()) {
            m_logger.debug("getBlockAsString() - end - return value=" + returnString);
        }
        return returnString;
    }
}
