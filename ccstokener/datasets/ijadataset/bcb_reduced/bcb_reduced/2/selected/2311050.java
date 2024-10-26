package mvt.help;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Takes an xml file and parses it using the following syntax:
 * <UL>
 *  &lt;help&gt;
 *  <UL>
 *   &lt;topic topic=&quot;a topic to search for and displayed&quot; location=&quot;html file&quot;&gt;
 *   &lt;topic topic=&quot;another topic&quot; location=&quot;another html file&quot;&gt;
 *  </UL>
 * </UL>
 */
public final class HelpParser {

    /** List of nodes. */
    private HelpIndexList list;

    /** Internet address of file. */
    URL url;

    /**
     * Constructor that sets up the location of the xml file.
     * @param uri the uri to the file xml file.
     * @throws MalformedURLException
     */
    public HelpParser(String uri) throws MalformedURLException {
        url = new URL(uri);
    }

    /**
     * Parses the xml file and returns a list of HelpIndexNodes.
     * @return HelpIndexList a list of HelpIndexNode.
     * @throws HelpParserException any and all error messages.
     *
     * @see HelpIndexList
     * @see HelpIndexNode
     */
    public HelpIndexList parseHelp() throws HelpParserException {
        try {
            BufferedReader read = new BufferedReader(new InputStreamReader(url.openStream()));
            String tempLine;
            list = new HelpIndexList();
            int line = 1;
            tempLine = read.readLine();
            while (tempLine != null) {
                tempLine = tempLine.trim();
                if (tempLine.indexOf("<topic") == 0) {
                    int topic = tempLine.indexOf(" topic");
                    int location = tempLine.indexOf("location");
                    if (topic < 0) throw new HelpParserException("Missing topic " + "attribute on line: " + line);
                    if (location < 0) throw new HelpParserException("Missing location " + "attribute on line: " + line);
                    list.addHelpNode(new HelpIndexNode(tempLine.substring(tempLine.indexOf('"', topic) + 1, tempLine.indexOf('"', topic + 8)), tempLine.substring(tempLine.indexOf('"', location) + 1, tempLine.indexOf('"', location + 10))));
                }
                tempLine = read.readLine();
                line++;
            }
            read.close();
        } catch (IOException e) {
            throw new HelpParserException("Error with file " + e);
        }
        return list;
    }

    /**
     * Makes getting the list of objects fast after parsing.  If
     * the xml file has not been parsed before it will be parsed,
     * otherwise the list will be returned.
     * @return HelpIndexList a list of HelpIndexNodes.
     * @throws HelpParserException
     *
     * @see #parseHelp
     * @see HelpIndexList
     * @see HelpIndexNode
     */
    public HelpIndexList getHelp() throws HelpParserException {
        if (list == null) return parseHelp(); else return list;
    }
}
