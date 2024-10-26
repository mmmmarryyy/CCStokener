package rj.tools.jcsc.rules;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import org.apache.xerces.parsers.DOMParser;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * <code>RulesHandler</code> is a helper class which is resoponsibel for saving
 * the rules properties.
 *
 * @author Ralph Jocham
 * @version __0.98.2__
 */
public class RulesHandler {

    /**
    * jcsc token
    */
    public static final String JCSC = "jcsc";

    /**
    * rules token
    */
    public static final String RULES = "rules";

    /**
    * rule token
    */
    public static final String RULE = "rule";

    /**
    * type token
    */
    public static final String TYPE = "type";

    /**
    * value token
    */
    public static final String VALUE = "value";

    /**
    * severity token
    */
    public static final String SEVERITY = "severity";

    /**
    * choices token
    */
    public static final String CHOICES = "choices";

    /**
    * choice token
    */
    public static final String CHOICE = "choice";

    private static Map sDefaultRules;

    private PrintStream mPrintStream;

    private int mIndent;

    /**
    * Set the print stream for reading
    *
    * @param ps
    */
    public void setPrintStream(PrintStream ps) {
        mPrintStream = ps;
    }

    /**
    * Persist the rules map into the print stream
    *
    * @param map
    */
    public void persistRules(Map map) {
        mPrintStream.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>");
        mPrintStream.println("<?xml-stylesheet href=\"xsl/rules.xsl\" type=\"text/xsl\"?>");
        openTag(RulesHandler.JCSC, new String[] { "version=\"__0.98.1__\"", "date=\"" + getDate() + "\"" });
        openTag(RulesHandler.RULES);
        for (Iterator iter = map.keySet().iterator(); iter.hasNext(); ) {
            Rule rule = (Rule) map.get(iter.next());
            openTag(RulesHandler.RULE, new String[] { "name=\"" + rule.getName() + "\"", "category=\"" + rule.getCategory() + "\"", "enabled=\"" + rule.isEnabled() + "\"" });
            tag(RulesHandler.TYPE, rule.getType());
            if (rule.getType().equals("multichoice")) {
                openTag(RulesHandler.CHOICES);
                String[] choices = rule.getChoices();
                for (int i = 0, count = choices.length; i < count; i++) {
                    tag(RulesHandler.CHOICE, choices[i]);
                }
                closeTag(RulesHandler.CHOICES);
            }
            tag(RulesHandler.VALUE, rule.getValue());
            tag(RulesHandler.SEVERITY, Integer.toString(rule.getSeverity()));
            closeTag(RulesHandler.RULE);
        }
        closeTag(RulesHandler.RULES);
        closeTag(RulesHandler.JCSC);
    }

    /**
    * Print the indent for the xml file
    */
    public void printIndent() {
        for (int i = 0; i < mIndent; i++) {
            mPrintStream.print("   ");
        }
    }

    /**
    * Print a XML open tag
    *
    * @param tag
    */
    public void openTag(String tag) {
        printIndent();
        mPrintStream.println("<" + tag + ">");
        mIndent++;
    }

    /**
    * Print a XML open tag with attributes
    *
    * @param tag
    * @param attributes
    */
    public void openTag(String tag, String[] attributes) {
        printIndent();
        mPrintStream.print("<" + tag + " ");
        for (int i = 0, count = attributes.length; i < count; i++) {
            mPrintStream.print(attributes[i]);
            if ((i + 1) < count) {
                mPrintStream.print(" ");
            }
        }
        mPrintStream.println(">");
        mIndent++;
    }

    /**
    * Print a XML tag with a value
    *
    * @param tagName
    * @param value
    */
    public void tag(String tagName, String value) {
        printIndent();
        mPrintStream.print("<" + tagName + ">");
        mPrintStream.print(value);
        mPrintStream.println("</" + tagName + ">");
    }

    /**
    * Close a XML tag
    *
    * @param tag
    */
    public void closeTag(String tag) {
        mIndent--;
        printIndent();
        mPrintStream.println("</" + tag + ">");
    }

    private String getDate() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return df.format(new Date());
    }

    /**
    * Returns the InputStream of a rules files from within the JAR file
    *
    * @param fileName
    *
    * @return InputStream of rules files
    *
    * @throws IOException
    */
    public InputStream getInputStreamOfZipRulesFile(String fileName) throws IOException {
        InputStream is = null;
        try {
            URL url = Class.forName("rj.tools.jcsc.rules.RuleConstants").getResource(fileName);
            is = url.openStream();
        } catch (ClassNotFoundException e) {
            throw new IOException("rj.tools.jcsc.rules.RuleConstants class could not be found");
        }
        return is;
    }

    /**
    * Read a rules XML file from a zip/jar file (rj/tools/jcsc/rules is path)
    *
    * @param fileName
    *
    * @return rules map
    *
    * @throws IOException
    */
    public Map readZipRulesFile(String fileName) throws IOException {
        InputStream is = null;
        XMLRulesMap map = null;
        boolean isError = false;
        try {
            map = createRulesMap(getInputStreamOfZipRulesFile(fileName));
        } catch (IOException e) {
            isError = true;
        } catch (SAXException e) {
            isError = true;
        } catch (RuntimeException e) {
            isError = true;
        }
        if (isError) {
            throw new IOException("'" + fileName + "' could not be found in the .jar file");
        }
        return map;
    }

    /**
    * Read a rules XML file from the file system
    *
    * @param fileName
    *
    * @return rules map
    *
    * @throws IOException
    */
    public Map readRulesFile(String fileName) throws IOException {
        final class Foo implements ActionListener {

            /**
          * Invoked when an action occurs.
          */
            public void actionPerformed(ActionEvent e) {
                ;
            }
        }
        XMLRulesMap map = null;
        boolean isError = false;
        File file = new File(System.getProperty("jcsc.home") + File.separator + "rules" + File.separator + fileName);
        if (!file.exists()) {
            file = new File(fileName);
        }
        try {
            map = createRulesMap(new FileInputStream(file));
        } catch (SAXException e) {
            isError = true;
        } catch (IOException e) {
            isError = true;
        }
        if (isError) {
            try {
                return readZipRulesFile(fileName);
            } catch (IOException e2) {
                throw new IOException("'" + fileName + "' could not be found");
            }
        }
        return map;
    }

    private XMLRulesMap createRulesMap(InputStream stream) throws SAXException, IOException {
        ClassLoader savedClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
        DOMParser parser = new DOMParser();
        parser.parse(new InputSource(stream));
        Document document = parser.getDocument();
        Thread.currentThread().setContextClassLoader(savedClassLoader);
        XMLRulesMap rm = new XMLRulesMap(document);
        return rm;
    }

    /**
    * Reads a rules .jcsc properties file; only used for conversion from older
    * JCSC versions
    *
    * @param fileName
    *
    * @return rules properties
    *
    * @throws IOException
    */
    public Properties readPropertiesRulesFile(String fileName) throws IOException {
        try {
            Properties p = new Properties();
            p.load(new FileInputStream(new File(fileName)));
            return p;
        } catch (IOException e) {
            throw new IOException("'" + fileName + "' could not be found");
        }
    }

    /**
    * Get the rule from the map of all rules if the rule was not found the
    * default rule is being returned.
    *
    * @param rules
    * @param name
    *
    * @return rule which is default rule if not found
    */
    public static Rule getRuleFromMap(Map rules, String name) {
        if (null == sDefaultRules) {
            try {
                sDefaultRules = new RulesHandler().readZipRulesFile("default.jcsc.xml");
            } catch (IOException e) {
                e.printStackTrace(System.err);
            }
        }
        return (Rule) (rules.get(name) == null ? sDefaultRules.get(name) : rules.get(name));
    }
}
