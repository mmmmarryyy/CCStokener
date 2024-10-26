package backend.parser.foaf;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import org.codehaus.stax2.XMLInputFactory2;
import org.codehaus.stax2.XMLOutputFactory2;
import org.codehaus.stax2.XMLStreamReader2;
import org.codehaus.stax2.XMLStreamWriter2;
import backend.core.AbstractONDEXGraph;
import backend.core.security.Session;
import backend.event.type.StatisticalOutput;
import backend.exchange.xml.export.ondex.Export;
import backend.param.args.ArgumentDefinition;
import backend.param.args.generic.FileArgumentDefinition;
import backend.param.args.generic.URLArgumentDefinition;
import backend.parser.AbstractONDEXParser;
import backend.parser.ParserArguments;
import backend.parser.foaf.args.ArgumentNames;
import backend.parser.foaf.sink.Document;
import backend.parser.foaf.sink.Person;
import com.ctc.wstx.exc.WstxEOFException;
import com.ctc.wstx.exc.WstxUnexpectedCharException;

public class Parser extends AbstractONDEXParser {

    private boolean saveRDFs = true;

    private static final boolean DEBUG = true;

    private final XMLInputFactory2 factory;

    public Parser(Session s) {
        super(s);
        System.setProperty("javax.xml.stream.XMLInputFactory", "com.ctc.wstx.stax.WstxInputFactory");
        factory = (XMLInputFactory2) XMLInputFactory.newInstance();
        ((XMLInputFactory2) factory).configureForSpeed();
    }

    private ParserArguments pa;

    @Override
    public String getName() {
        return "FOAF Parser";
    }

    @Override
    public String getVersion() {
        return "1";
    }

    @Override
    public ArgumentDefinition[] getArgumentDefinitions() {
        ArrayList<ArgumentDefinition> args = new ArrayList<ArgumentDefinition>();
        args.add(new FileArgumentDefinition(ArgumentNames.USECACHEFILE_ARG, null, false, true, true));
        args.add(new URLArgumentDefinition(ArgumentNames.ENTRYURLSFILE_ARG, false));
        return null;
    }

    @Override
    public void setParserArguments(ParserArguments pa) {
        this.pa = pa;
    }

    @Override
    public ParserArguments getParserArguments() {
        return pa;
    }

    private final HashSet<Person> people = new HashSet<Person>(100000);

    private final HashSet<String> visitedSites = new HashSet<String>();

    @Override
    public void setONDEXGraph(AbstractONDEXGraph graph) {
        String[] names = new String[] { "name", "title", "nick", "firstName", "family_name", "givenname" };
        for (String name : names) nameTags.add(name);
        String[] contacts = new String[] { "phone", "mbox", "yahooChatID", "msnChatID" };
        for (String contact : contacts) contactTags.add(contact);
        Boolean parsefromFS = ((Boolean) pa.getUniqueValue("ParseFromFileSystemCache"));
        System.getProperties().put("proxySet", "true");
        System.getProperties().put("http.agent", "http://ondex.sourceforge.net; ondex@sourceforge.net");
        System.getProperties().put("proxyHost", "wwwcache.bbsrc.ac.uk");
        System.getProperties().put("proxyPort", "8080");
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection.setDefaultAllowUserInteraction(false);
        String dataDirectory = pa.getInputDir();
        if (!dataDirectory.endsWith("[/|\\]")) dataDirectory = dataDirectory + File.separator;
        String[] targetUrls = new String[0];
        if (pa.getOptions().get("EntryTargetUrl") != null) targetUrls = ((String) pa.getUniqueValue("EntryTargetUrl")).split(";");
        String[] entryTargetUrlFiles = new String[0];
        if (pa.getOptions().get("EntryTargetUrlFile") != null) entryTargetUrlFiles = ((String) pa.getUniqueValue("EntryTargetUrlFile")).split(";");
        List<String> urls = new ArrayList<String>();
        for (String url : targetUrls) {
            url = processURLToFOAFRef(url);
            if (!urls.contains(url)) urls.add(url);
        }
        for (String file : entryTargetUrlFiles) {
            try {
                FileReader fr = new FileReader(dataDirectory + file);
                BufferedReader br = new BufferedReader(fr);
                while (br.ready()) {
                    String url = br.readLine().trim();
                    if (url.length() > 0) {
                        url = processURLToFOAFRef(url);
                        if (!urls.contains(url)) urls.add(url);
                    }
                }
                br.close();
                fr.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        Collections.shuffle(urls);
        if (parsefromFS == null || !parsefromFS) {
            int count = 0;
            Iterator<String> urlIt = urls.iterator();
            while (urlIt.hasNext()) {
                if (count % 10 == 0) PersonMerger.mergeAndMapPeople(people);
                propergateParseURL(urlIt.next());
                count++;
            }
        } else {
            String filename = dataDirectory + "rdfs" + File.separator + "RDF_Index.dat";
            try {
                BufferedReader bfr = new BufferedReader(new FileReader(filename));
                while (bfr.ready()) {
                    String line = bfr.readLine();
                    String[] values = line.split("\t");
                    if (values.length > 0) validRdfFileToURL.put(values[0], values[1]); else throw new IOException("RDF_Index.dat has been corrupted");
                }
                bfr.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            File dir = new File(dataDirectory + File.separator + "rdfs");
            File[] files = dir.listFiles();
            int count = 0;
            for (File file : files) {
                if (count % 10 == 0) PersonMerger.mergeAndMapPeople(people);
                parseFile(file);
                count++;
            }
        }
        if (saveRDFs && (parsefromFS == null || !parsefromFS)) {
            String filename = dataDirectory + "rdfs" + File.separator + "RDF_Index.dat";
            try {
                BufferedWriter bfw = new BufferedWriter(new FileWriter(filename));
                Iterator<String> urlIt = validRdfFileToURL.keySet().iterator();
                while (urlIt.hasNext()) {
                    String url = urlIt.next();
                    bfw.write(url + '\t' + validRdfFileToURL.get(url));
                    bfw.newLine();
                }
                bfw.flush();
                bfw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        PersonMerger.mergeAndMapPeople(people);
        WriteToONDEX wto = new WriteToONDEX(people, graph, s);
        wto.write();
        if (DEBUG) {
            saveOndexGraphToFile("D:/foaf-out.xml", s, graph);
        }
        graph.fireEventOccurred(new StatisticalOutput(people.size() + " People parsed from web"));
    }

    private String processURLToFOAFRef(String webPage) {
        String filename = "foaf.rdf";
        if (webPage.endsWith("/")) {
            webPage = webPage + filename;
        } else {
            String[] split = webPage.split("/");
            String file = split[split.length - 1];
            if (file.length() > 0) {
                if (!file.endsWith("foaf") || !file.endsWith(".rdf") || !file.endsWith(".xml") || file.matches("foaf.[a-z|A-Z|0-9]*$")) {
                    webPage = webPage.substring(0, webPage.length() - file.length()) + filename;
                } else {
                    System.err.println(" ok " + file + " ok");
                }
            }
        }
        return webPage;
    }

    public static String getFileEnding(String webPage) {
        try {
            new URL(webPage);
            String[] split = webPage.split("/");
            String file = split[split.length - 1];
            return file;
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public static String trimFileEnding(String webPage) {
        if (webPage.endsWith("/")) return webPage;
        String[] split = webPage.split("/");
        if (split.length == 0) return webPage;
        String file = split[split.length - 1];
        if (file.length() > 0) return webPage.substring(0, webPage.length() - file.length()); else return webPage;
    }

    private void propergateParseURL(String webPage) {
        if (visitedSites.contains(webPage)) return;
        List<Person> persons = parseURL(webPage);
        if (persons != null) {
            if (persons.size() == 1) persons.get(0).addHomepage(webPage);
            propergatePeople(persons);
        }
    }

    private void propergatePeople(List<Person> persons) {
        HashSet<Person> poolPeople = new HashSet<Person>();
        poolPeople.addAll(persons);
        System.out.println("propergate people");
        while (true) {
            HashSet<Person> newPeople = new HashSet<Person>();
            Iterator<Person> rootPersonIt = poolPeople.iterator();
            while (rootPersonIt.hasNext()) {
                Person rootP = rootPersonIt.next();
                rootPersonIt.remove();
                Iterator<Person> knownPeopleIt = rootP.getKnownPeople().iterator();
                while (knownPeopleIt.hasNext()) {
                    Person knownP = knownPeopleIt.next();
                    Iterator<String> urlIt = knownP.getHomepages().iterator();
                    while (urlIt.hasNext()) {
                        String url = processURLToFOAFRef(urlIt.next());
                        if (visitedSites.contains(url)) continue;
                        System.out.print("\nchild==>");
                        List<Person> newPs = parseURL(url);
                        if (newPs != null) {
                            if (persons.size() == 1) persons.get(0).addHomepage(url);
                            newPeople.addAll(newPs);
                        }
                    }
                    knownP.setVisitedSite(true);
                }
            }
            if (newPeople.size() == 0) break; else System.out.println("people to do => " + newPeople.size());
            poolPeople.clear();
            poolPeople.addAll(newPeople);
            newPeople.clear();
        }
    }

    private List<Person> parseFile(File file) {
        List<Person> people;
        try {
            XMLStreamReader2 staxXmlReader = (XMLStreamReader2) factory.createXMLStreamReader(file);
            people = parsePeople(staxXmlReader, file.getName());
            if (people != null && people.size() == 1) people.get(0).addHomepage(validRdfFileToURL.get(file));
            staxXmlReader.close();
        } catch (XMLStreamException e) {
            return null;
        }
        return people;
    }

    public HashMap<String, String> validRdfFileToURL = new HashMap<String, String>();

    /**
	 * Attempts to parse the rdf at the given location
	 * @param webPage the webpage to try
	 * @return
	 */
    private List<Person> parseURL(String webPage) {
        visitedSites.add(webPage);
        URL url;
        List<Person> people;
        try {
            url = new URL(webPage);
        } catch (MalformedURLException e1) {
            return null;
        }
        HttpURLConnection httpConn = null;
        try {
            httpConn = (HttpURLConnection) url.openConnection();
            httpConn.setConnectTimeout(1000);
            XMLStreamReader2 staxXmlReader = (XMLStreamReader2) factory.createXMLStreamReader(httpConn.getInputStream());
            people = parsePeople(staxXmlReader, webPage);
        } catch (SocketTimeoutException c) {
            return null;
        } catch (ConnectException c) {
            return null;
        } catch (IOException e) {
            return null;
        } catch (XMLStreamException e) {
            return null;
        } finally {
            if (httpConn != null) httpConn.disconnect();
        }
        System.out.println(saveRDFs + " ok:" + (people != null));
        if (people != null) {
            try {
                Thread.sleep(250);
            } catch (InterruptedException e2) {
                e2.printStackTrace();
            }
        }
        if (saveRDFs && people != null) {
            String dataDirectory = pa.getInputDir();
            if (!dataDirectory.endsWith("[/|\\]")) dataDirectory = dataDirectory + File.separator;
            new File(dataDirectory + "rdfs").mkdir();
            String filename = dataDirectory + "rdfs" + File.separator + "FOAF_" + files + ".rdf";
            download(webPage, filename);
            validRdfFileToURL.put(filename, webPage);
        }
        return people;
    }

    public List<String> nameTags = new ArrayList<String>();

    public List<String> contactTags = new ArrayList<String>();

    /**
	 * Parses all the root people (usualy one) from the rdf and other relevent objects
	 * @param staxXmlReader
	 * @param webPage
	 * @return
	 */
    public List<Person> parsePeople(XMLStreamReader2 staxXmlReader, String webPage) {
        List<Person> rootPeople = new ArrayList<Person>();
        try {
            System.out.println("parsing " + webPage);
            Person person = null;
            boolean knowsTag = false;
            boolean pageTag = false;
            while (staxXmlReader.hasNext()) {
                int event = staxXmlReader.next();
                switch(event) {
                    case XMLStreamConstants.START_DOCUMENT:
                        break;
                    case XMLStreamConstants.START_ELEMENT:
                        String element = staxXmlReader.getLocalName();
                        String text = null;
                        if (staxXmlReader.isCharacters()) staxXmlReader.getElementText().trim();
                        if (element.equals("Person")) {
                            if (!knowsTag) {
                                person = new Person();
                                person.setUrlID(webPage);
                                person.setVisitedSite(true);
                                Iterator<String> urlIt = getAtributeURLS(staxXmlReader).iterator();
                                while (urlIt.hasNext()) person.addHomepage(urlIt.next());
                            } else {
                                Person knownPerson = parsePerson(staxXmlReader);
                                knownPerson.setUrlID(webPage);
                                if (person != null) person.addKnownPerson(knownPerson); else System.err.println(webPage + " is malformed and contains a knows outside a person");
                            }
                            continue;
                        }
                        if (person == null) continue;
                        Iterator<String> urls = getAtributeURLS(staxXmlReader).iterator();
                        while (urls.hasNext()) {
                            person.addHomepage(urls.next());
                        }
                        if (person == null) continue;
                        if (element.equals("mbox_sha1sum")) {
                            person.addSha1sum(staxXmlReader.getElementText().trim());
                        }
                        if (nameTags.contains(element.toLowerCase())) {
                            person.addNameAttribute(element, getAtribute(staxXmlReader, "resource"));
                            person.addNameAttribute(element, staxXmlReader.getElementText().trim());
                            continue;
                        }
                        if (isValidURL(text)) {
                            person.addHomepage(text);
                            continue;
                        }
                        if (element.equals("page")) {
                            pageTag = true;
                            continue;
                        }
                        if (element.equals("Document") && pageTag) {
                            person.addDocument(parseDocument(staxXmlReader));
                            continue;
                        }
                        if (contactTags.contains(element.toLowerCase()) || hasNameAndDomain(text)) {
                            person.addContactAttribute(element, staxXmlReader.getElementText().replace("mailto:", ""));
                            continue;
                        }
                        if (element.equals("knows")) {
                            knowsTag = true;
                            continue;
                        }
                        break;
                    case XMLStreamConstants.END_ELEMENT:
                        element = staxXmlReader.getLocalName();
                        if (element.equals("Person") && person != null) {
                            people.add(person);
                            rootPeople.add(person);
                            person = null;
                        } else if (element.equals("knows")) {
                            knowsTag = false;
                        } else if (element.equals("page")) {
                            pageTag = false;
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (WstxEOFException w) {
            return null;
        } catch (WstxUnexpectedCharException w) {
            return null;
        } catch (XMLStreamException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                staxXmlReader.close();
            } catch (XMLStreamException e) {
                e.printStackTrace();
            }
        }
        return rootPeople;
    }

    /**
	 * Parsers a Document from the FOAF rdf
	 * @param staxXmlReader
	 * @return
	 * @throws XMLStreamException
	 */
    private Document parseDocument(XMLStreamReader2 staxXmlReader) throws XMLStreamException {
        Document doc = new Document();
        Iterator<String> urlIt = getAtributeURLS(staxXmlReader).iterator();
        while (urlIt.hasNext()) doc.addURL(urlIt.next());
        while (staxXmlReader.hasNext()) {
            int event = staxXmlReader.next();
            switch(event) {
                case XMLStreamConstants.START_ELEMENT:
                    String element = staxXmlReader.getLocalName();
                    String text = null;
                    if (staxXmlReader.isCharacters()) text = staxXmlReader.getElementText().trim();
                    if (isValidURL(text)) {
                        doc.addURL(text);
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    element = staxXmlReader.getLocalName();
                    if (element.equals("Document")) return doc;
                    break;
                default:
                    break;
            }
        }
        return doc;
    }

    /**
	 * Parses a Person Object from the given knows tag in the FOAF rdf
	 * @param staxXmlReader
	 * @return
	 * @throws XMLStreamException
	 */
    private Person parsePerson(XMLStreamReader2 staxXmlReader) throws XMLStreamException {
        Person person = new Person();
        Iterator<String> urlIt = getAtributeURLS(staxXmlReader).iterator();
        while (urlIt.hasNext()) person.addHomepage(urlIt.next());
        while (staxXmlReader.hasNext()) {
            int event = staxXmlReader.next();
            switch(event) {
                case XMLStreamConstants.START_ELEMENT:
                    String element = staxXmlReader.getLocalName();
                    String text = null;
                    if (staxXmlReader.isCharacters()) staxXmlReader.getElementText().trim();
                    Iterator<String> urls = getAtributeURLS(staxXmlReader).iterator();
                    while (urls.hasNext()) {
                        person.addHomepage(urls.next());
                    }
                    if (isValidURL(text)) {
                        person.addHomepage(text);
                    }
                    if (element.equals("mbox_sha1sum")) {
                        person.addSha1sum(staxXmlReader.getElementText().trim());
                    }
                    if (nameTags.contains(element.toLowerCase())) {
                        person.addNameAttribute(element, getAtribute(staxXmlReader, "resource"));
                        person.addNameAttribute(element, staxXmlReader.getElementText().trim());
                        continue;
                    }
                    if (contactTags.contains(element.toLowerCase()) || hasNameAndDomain(text)) {
                        person.addContactAttribute(element, staxXmlReader.getElementText().replace("mailto:", ""));
                        continue;
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    element = staxXmlReader.getLocalName();
                    if (element.equals("Person")) return person;
                    break;
                default:
                    break;
            }
        }
        return person;
    }

    /**
	 * tests string for URL conformity TODO: optimise this method catching exceptions in this way is expensive
	 * @param text
	 * @return
	 */
    private boolean isValidURL(String text) {
        if (text == null || text.length() == 0) return false;
        try {
            new URL(text);
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }

    /**
	 * Tests the given string for email address
	 * @param aEmailAddress projected text
	 * @return is it a valid e-mail
	 */
    private static boolean hasNameAndDomain(String aEmailAddress) {
        if (aEmailAddress != null && aEmailAddress.length() > 0) {
            String[] tokens = aEmailAddress.split("@");
            return tokens.length == 2 && tokens[0].trim().length() > 0 && tokens[1].trim().length() > 0;
        }
        return false;
    }

    /**
	 * Retrieves all valid URLs from the attribute tag of this element
	 * @param staxXmlReader
	 * @return a set of url strings
	 */
    private Set<String> getAtributeURLS(XMLStreamReader2 staxXmlReader) {
        HashSet<String> urls = new HashSet<String>();
        for (int i = 0; i < staxXmlReader.getAttributeCount(); i++) {
            String value = staxXmlReader.getAttributeValue(i).trim();
            if (isValidURL(value)) {
                urls.add(value);
            }
        }
        return urls;
    }

    /**
	 * Gets the attribute of the given name on this element
	 * @param staxXmlReader
	 * @param name
	 * @return
	 */
    private String getAtribute(XMLStreamReader2 staxXmlReader, String name) {
        for (int i = 0; i < staxXmlReader.getAttributeCount(); i++) {
            if (staxXmlReader.getAttributeName(i).getLocalPart().equalsIgnoreCase(name)) return staxXmlReader.getAttributeValue(i).trim();
        }
        return null;
    }

    /**
	 * Saves the current ONDEX graph as an ONDEX xml file
	 * @param filename the new xml file
	 * @param s the current session
	 * @param graph the graph to export
	 */
    public static void saveOndexGraphToFile(String filename, Session s, AbstractONDEXGraph graph) {
        XMLOutputFactory2 xmlof = (XMLOutputFactory2) XMLOutputFactory2.newInstance();
        xmlof.configureForSpeed();
        StringWriter sw = new StringWriter();
        try {
            XMLStreamWriter2 xmlw = xmlof.createXMLStreamWriter(sw, "UTF-8");
            Export builder = new Export(s);
            builder.buildDocument(xmlw, graph);
            xmlw.flush();
            xmlw.close();
            sw.flush();
            File f = new File(filename);
            f.delete();
            f.createNewFile();
            FileWriter fos = new FileWriter(f);
            fos.write(sw.toString());
            fos.close();
        } catch (XMLStreamException e1) {
            e1.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static int files = 0;

    /**
	 * downloads the file at the specified URL to a sequentialy named file store
	 * @param address
	 * @param localFileName
	 */
    public static void download(String address, String localFileName) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            files++;
            File f = new File(localFileName);
            f.createNewFile();
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(f));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
            System.out.println(localFileName + "\t" + numWritten + " bytes written");
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }
}
