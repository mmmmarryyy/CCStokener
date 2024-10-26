package dtd;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import main.Konstanten;

/** 
* Simple parser that extracts element names, attributes and attribute-values from a DTD.
* @author Thomas Wenzel
* @version 0.3
*/
public class DTDTools {

    private URL dtdURL;

    /** Creates a DTDReader to read a DTD from a URL 
	 * @param String The URL of the DTD
	 * */
    public DTDTools(String aDtdURL_) throws IOException {
        try {
            dtdURL = new URL(aDtdURL_);
        } catch (MalformedURLException exc) {
            throw new IOException(Konstanten.InvalidDTDURL + " " + aDtdURL_ + ": " + exc.toString());
        }
    }

    /** 
	 * Returns an array of the names of elements defined in the DTD
	 * @return The elements in the DTD
	 * @throws IOException If there is an error reading the DTD
	 */
    public String[] getElements() throws IOException {
        {
            Vector<String> v = new Vector<String>();
            PushbackInputStream in = null;
            try {
                URLConnection urlConn = dtdURL.openConnection();
                in = new PushbackInputStream(new BufferedInputStream(urlConn.getInputStream()));
                while (scanForLTBang(in)) {
                    String elementType = getString(in);
                    if (elementType.equals("ELEMENT")) {
                        skipWhiteSpace(in);
                        String elementName = getString(in);
                        v.addElement(elementName);
                    }
                }
                in.close();
                String[] elements = new String[v.size()];
                v.copyInto(elements);
                return elements;
            } catch (Exception exc) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (Exception ignore) {
                    }
                }
                throw new IOException(Konstanten.ErrorReadingDTD + ": " + exc.toString());
            }
        }
    }

    /** 
	 * Searches for &lt;! in an input stream.
	 * @param in The input stream to read from
	 * @throws IOException If there is an error reading the stream
	 */
    protected boolean scanForLTBang(PushbackInputStream in) throws IOException {
        int ch;
        while ((ch = in.read()) >= 0) {
            if (ch == '<') {
                ch = in.read();
                if (ch < 0) return false;
                if (ch == '!') {
                    return true;
                }
                if (ch == '<') {
                    in.unread((byte) ch);
                }
            }
        }
        return false;
    }

    /** 
	 * Skips over any whitespace characters in the stream
	 * @param in The input stream to read
	 * @throws IOException If there is an error reading the stream
	 */
    protected void skipWhiteSpace(PushbackInputStream in) throws IOException {
        int ch;
        while ((ch = in.read()) >= 0) {
            if (!Character.isWhitespace((char) ch)) {
                in.unread((byte) ch);
                return;
            }
        }
    }

    /** Reads a whitespace-delimited string from a stream
	 * @param in The input stream to read
	 * @throws IOException If there is an error reading the stream
	 */
    protected String getString(PushbackInputStream in) throws IOException {
        StringBuffer str = new StringBuffer();
        int ch;
        while ((ch = in.read()) >= 0) {
            if (Character.isWhitespace((char) ch)) {
                in.unread((byte) ch);
                return str.toString();
            } else {
                str.append((char) ch);
            }
        }
        return null;
    }

    /**
	     * Method to get the names of all child-elements for a given element.
	     * @param Spring parentElement - The parent element
	     * @param String dtdToCheck - Path to the DTD
	     * @return The child-names of the given element
	     */
    public static String[] getChildElements(String parentElement, String dtdToCheck) {
        Vector<String> childVector = new Vector<String>();
        int stopIndex;
        try {
            BufferedReader b = new BufferedReader(new FileReader(dtdToCheck));
            String Text = null;
            int line = 0;
            try {
                while ((Text = b.readLine()) != null) {
                    line = line + 1;
                    if (Text.length() > 0) {
                        Text = Text.toUpperCase().replaceAll(" ", "");
                        if (Text.contains(("<!ELEMENT") + parentElement.toUpperCase())) {
                            if (Text.contains("(") == true) {
                                int startIndex = Text.indexOf("(");
                                stopIndex = Text.lastIndexOf(")");
                                String partString = Text.substring((startIndex + 1), stopIndex);
                                if (((partString.contains(",") == false)) && ((partString.contains("|") == false))) {
                                    childVector.add(partString);
                                } else {
                                    while (((partString.contains(",") == true)) || ((partString.contains("|") == true))) {
                                        if (((partString.indexOf(",")) > 0) && ((partString.indexOf("|") > 0))) {
                                            if (((partString.indexOf(",")) < (partString.indexOf("|")))) {
                                                stopIndex = partString.indexOf(",");
                                            } else {
                                                stopIndex = partString.indexOf("|");
                                            }
                                        } else if ((((partString.indexOf(",")) > 0) && ((partString.indexOf("|") < 0)))) {
                                            stopIndex = partString.indexOf(",");
                                        } else if ((((partString.indexOf("|")) > 0) && ((partString.indexOf(",") < 0)))) {
                                            stopIndex = partString.indexOf("|");
                                        }
                                        String childElement = partString.substring(0, stopIndex);
                                        partString = partString.substring((stopIndex + 1), partString.length());
                                        childVector.add(childElement);
                                    }
                                    childVector.add(partString);
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
        String[] childList = new String[childVector.size()];
        childVector.copyInto(childList);
        return childList;
    }

    /**
     * Method to get all attributes for a given element.
     * @param String dtdToCheck - Path to the DTD
     * @param String parentElement - The element to lookup for attributes
     * @return The child-names of the given element
     */
    public static String[] getAttributes(String parentElement, String dtdToCheck) {
        Vector<String> attributeVector = new Vector<String>();
        String partString;
        String attValue;
        String attName;
        String attType;
        @SuppressWarnings("unused") String attValueDefault;
        int startIndex;
        try {
            BufferedReader b = new BufferedReader(new FileReader(dtdToCheck));
            String Text = null;
            try {
                while ((Text = b.readLine()) != null) {
                    if (Text.length() > 0) {
                        Text = Text.toUpperCase().replaceAll("  ", " ");
                        if (Text.contains("<!ATTLIST " + parentElement.toUpperCase() + " ")) {
                            if (Text.contains(">")) {
                                startIndex = parentElement.length() + 11;
                                partString = Text.substring(startIndex, Text.indexOf(">"));
                                attName = "";
                                startIndex = 0;
                                while (partString.length() > 1) {
                                    while (partString.indexOf(" ") == 0) {
                                        partString = partString.substring(1, partString.length());
                                    }
                                    attName = partString.substring(0, partString.indexOf(" "));
                                    attributeVector.add(attName);
                                    if ((partString.substring(attName.length() + 1, partString.length())).contains("(")) {
                                        partString = partString.substring(attName.length() + 1, partString.length());
                                        attValue = partString.substring(partString.indexOf("(") + 1, partString.indexOf(")"));
                                    } else {
                                        partString = partString.substring(attName.length() + 1, partString.length());
                                        attValue = partString.substring(0, partString.indexOf(" "));
                                    }
                                    if ((partString.substring(attValue.length() + 1, partString.length())).contains("\"")) {
                                        if ((partString.substring(attValue.length() + 1, partString.length())).contains("#")) {
                                            if (partString.indexOf("\"") < partString.indexOf("#")) {
                                                startIndex = (partString.substring(0, partString.indexOf("\""))).length();
                                                String toRemember = partString.substring(startIndex + 1, partString.length());
                                                attValueDefault = toRemember.substring(0, toRemember.indexOf("\""));
                                                partString = partString.substring((startIndex + toRemember.length()), partString.length());
                                            }
                                        } else {
                                            startIndex = (partString.substring(0, partString.indexOf("\""))).length();
                                            String toRemember = partString.substring(startIndex + 1, partString.length());
                                            attValueDefault = toRemember.substring(0, toRemember.indexOf("\""));
                                            partString = partString.substring((startIndex + toRemember.length()), partString.length());
                                        }
                                    }
                                    if (partString.contains("#")) {
                                        partString = partString.substring(partString.indexOf("#"), partString.length());
                                        if (partString.contains(" ")) {
                                            attType = partString.substring(0, partString.indexOf(" "));
                                        } else {
                                            attType = partString.substring(0, partString.length());
                                        }
                                        partString = partString.substring(attType.length(), partString.length());
                                    }
                                    if ((partString.replaceAll(" ", "").length() > 0) == false) partString = "";
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
        String[] attributeList = new String[attributeVector.size()];
        attributeVector.copyInto(attributeList);
        return attributeList;
    }

    /**
	 * Method to find out the root-element of the given DTD.
	 * @param String dtdToCheck = Path to the DTD
	 * @param DTDTools dtdIn = DTDReader-Object
	 * @return The root-element of the given DTD
	 */
    public String[] getRootElement(DTDTools dtdIn, String dtdToCheck) {
        Vector<String> rootElemVector = new Vector<String>();
        String[] dtdElements = null;
        try {
            dtdElements = dtdIn.getElements();
        } catch (IOException e) {
            System.out.println(e);
        }
        for (int i = 0; i < dtdElements.length; i++) {
            int counter = 0;
            String elementToSearch = dtdElements[i];
            try {
                BufferedReader b = new BufferedReader(new FileReader(dtdToCheck));
                String Text = null;
                try {
                    while ((Text = b.readLine()) != null) {
                        if (Text.length() > 0) {
                            if (Text.contains("<!ELEMENT")) {
                                if ((Text.contains(elementToSearch)) == true) counter = counter + 1;
                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println(e);
                }
            } catch (FileNotFoundException e) {
                System.out.println(e);
            }
            if (counter == 1) rootElemVector.add(elementToSearch);
        }
        String[] rootElemName = new String[rootElemVector.size()];
        rootElemVector.copyInto(rootElemName);
        return rootElemName;
    }

    /**
	 * Method to cleanup the input-dtd. Remove linebreaks, tabs, doubled whitespace.
	 * @param String dtdToCleanUp = Path to the input DTD
	 * @return The path to the cleaned DTD as String
	 */
    public static String cleanUpDTD(String dtdToCleanUp) throws IOException {
        String cleanedDTD = dtdToCleanUp.replace(".dtd", "_cleaned.dtd");
        BufferedWriter writeFile;
        BufferedReader readFile;
        try {
            readFile = new BufferedReader(new FileReader(dtdToCleanUp));
            String Text = null;
            try {
                writeFile = new BufferedWriter(new FileWriter(cleanedDTD));
                while ((Text = readFile.readLine()) != null) {
                    if (Text.length() > 0) {
                        while (Text.contains("\t")) Text = Text.replaceAll("\t", " ");
                        Text = Text.replaceAll("\n", "");
                        Text = Text.replaceAll("\r", "");
                        while (Text.contains("  ")) Text = Text.replaceAll("  ", " ");
                        Text = Text.replaceAll("> ", ">");
                        if (Text.contains("?>") == false) {
                            Text = Text.replaceAll(">", ">\n");
                            writeFile.append(Text);
                        } else {
                            if (Text.contains("'>") == true) {
                                Text = Text.replaceAll("'>", "'>\n");
                                writeFile.append(Text);
                            } else {
                                writeFile.append(Text);
                                writeFile.newLine();
                            }
                        }
                    }
                }
                writeFile.flush();
                writeFile.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
        return cleanedDTD;
    }

    /**
	 * Method to cleanup the input-dtd. Remove all comments written in "<!-- ... -->".
	 * @param String dtdToCleanUp = Path to the input DTD
	 * @return The path to the cleaned DTD as String
	 */
    public static String removeAllComments(String dtdToCleanUp) throws IOException {
        String noCommentsDTD = dtdToCleanUp.replace(".dtd", "_noComments.dtd");
        BufferedWriter writeFile;
        BufferedReader readFile;
        try {
            readFile = new BufferedReader(new FileReader(dtdToCleanUp));
            String Text = null;
            try {
                writeFile = new BufferedWriter(new FileWriter(noCommentsDTD));
                while ((Text = readFile.readLine()) != null) {
                    if (Text.length() > 0) {
                        if (((Text.contains("<!--")) && (Text.contains("-->")))) {
                        } else {
                            writeFile.append(Text);
                            writeFile.newLine();
                        }
                    }
                }
                writeFile.flush();
                writeFile.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
        return noCommentsDTD;
    }

    /**
	 * Method to resolve the entities in a given dtd.
	 * @param String dtdToCleanUp = Path to the DTD
	 * @return String - The path to the new DTD with resolved entities
	 */
    public static String resolveEntitys(String dtdToProcess) throws IOException {
        String dtdResolvedEntitys = dtdToProcess.replace(".dtd", "_resolvedEntitys.dtd");
        Vector<String> entityVector = new Vector<String>();
        BufferedWriter writeFile;
        BufferedReader readFile;
        try {
            readFile = new BufferedReader(new FileReader(dtdToProcess));
            String Text = null;
            while ((Text = readFile.readLine()) != null) {
                if (Text.length() > 0) {
                    if (Text.contains("<!ENTITY %")) {
                        entityVector.add(Text);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
        try {
            readFile = new BufferedReader(new FileReader(dtdToProcess));
            String Text = null;
            try {
                writeFile = new BufferedWriter(new FileWriter(dtdResolvedEntitys));
                while ((Text = readFile.readLine()) != null) {
                    if (Text.length() > 0) {
                        for (int i = 0; i < entityVector.size(); i++) {
                            String vectorString = entityVector.elementAt(i);
                            String toFind = vectorString.substring((vectorString.indexOf("%")), vectorString.indexOf("\""));
                            toFind = toFind.replaceAll(" ", "");
                            if (Text.contains(toFind + ";")) {
                                String replaceString = vectorString.substring((vectorString.indexOf("\"") + 1), vectorString.lastIndexOf("\""));
                                Text = Text.replace((toFind + ";"), replaceString);
                            }
                        }
                        if ((Text.contains("<!ENTITY %") == false)) {
                            writeFile.append(Text);
                            writeFile.newLine();
                        }
                    }
                }
                writeFile.flush();
                writeFile.close();
            } catch (IOException e) {
                System.out.println(e);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
        return dtdResolvedEntitys;
    }

    /**
     * Method to get all attributes for a given element.
     * @param String dtdToCheck - Path to the DTD
     * @param String attributeName - The attribute to lookup for values
     * @return The child-names of the given element
     */
    public static String[] getAttributeValues(String parentElement, String dtdToCheck) {
        Vector<String> attributeVector = new Vector<String>();
        String partString, attValue, attName, attType, attValueDefault;
        int startIndex;
        try {
            BufferedReader b = new BufferedReader(new FileReader(dtdToCheck));
            String Text = null;
            try {
                while ((Text = b.readLine()) != null) {
                    if (Text.length() > 0) {
                        Text = Text.toUpperCase().replaceAll("  ", " ");
                        if (Text.contains("<!ATTLIST " + parentElement.toUpperCase() + " ")) {
                            if (Text.contains(">")) {
                                startIndex = parentElement.length() + 11;
                                partString = Text.substring(startIndex, Text.indexOf(">"));
                                attName = "";
                                startIndex = 0;
                                while (partString.length() > 1) {
                                    while (partString.indexOf(" ") == 0) {
                                        partString = partString.substring(1, partString.length());
                                    }
                                    attName = partString.substring(0, partString.indexOf(" "));
                                    attributeVector.add("AttributeName: " + attName);
                                    if ((partString.substring(attName.length() + 1, partString.length())).contains("(")) {
                                        partString = partString.substring(attName.length() + 1, partString.length());
                                        attValue = partString.substring(partString.indexOf("(") + 1, partString.indexOf(")"));
                                        attributeVector.add("AttributeValue: " + attValue);
                                    } else {
                                        partString = partString.substring(attName.length() + 1, partString.length());
                                        attValue = partString.substring(0, partString.indexOf(" "));
                                        attributeVector.add("AttributeValue: " + attValue);
                                    }
                                    if ((partString.substring(attValue.length() + 1, partString.length())).contains("\"")) {
                                        if ((partString.substring(attValue.length() + 1, partString.length())).contains("#")) {
                                            if (partString.indexOf("\"") < partString.indexOf("#")) {
                                                startIndex = (partString.substring(0, partString.indexOf("\""))).length();
                                                String toRemember = partString.substring(startIndex + 1, partString.length());
                                                attValueDefault = toRemember.substring(0, toRemember.indexOf("\""));
                                                attributeVector.add("DefaultAttributeValue: " + attValueDefault);
                                                partString = partString.substring((startIndex + toRemember.length()), partString.length());
                                            }
                                        } else {
                                            startIndex = (partString.substring(0, partString.indexOf("\""))).length();
                                            String toRemember = partString.substring(startIndex + 1, partString.length());
                                            attValueDefault = toRemember.substring(0, toRemember.indexOf("\""));
                                            attributeVector.add("DefaultAttributeValue: " + attValueDefault);
                                            partString = partString.substring((startIndex + toRemember.length()), partString.length());
                                        }
                                    }
                                    if (partString.contains("#")) {
                                        partString = partString.substring(partString.indexOf("#"), partString.length());
                                        if (partString.contains(" ")) {
                                            attType = partString.substring(0, partString.indexOf(" "));
                                        } else {
                                            attType = partString.substring(0, partString.length());
                                        }
                                        attributeVector.add("AttributeType: " + attType);
                                        partString = partString.substring(attType.length(), partString.length());
                                    }
                                    if ((partString.replaceAll(" ", "").length() > 0) == false) partString = "";
                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            }
        } catch (FileNotFoundException e) {
            System.out.println(e);
        }
        String[] attributeList = new String[attributeVector.size()];
        attributeVector.copyInto(attributeList);
        return attributeList;
    }
}
