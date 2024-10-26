package net.sf.osadm.docbook;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

/**
 * TODO Check/Test if above corrections are really done, if so, place them here in Javadoc comment. 
 * 
 * 
 */
public class Converter {

    private static final String PATTERN_FILE_SUFFIX = "\\.[^.]*$";

    private static final String CSV_FIELD_SEPARATOR = "\t";

    private static final String CSV_ROW_SEPARATOR = "\n";

    private String issueTitleIdRegExpStr = "^([0-9]{3,5}):.*";

    private Pattern issueTitleIdPattern = Pattern.compile(issueTitleIdRegExpStr);

    private Map<Element, List<DocBookContent>> addElementMap = new HashMap<Element, List<DocBookContent>>();

    private static Map<Element, List<Element>> removeElementMap = new HashMap<Element, List<Element>>();

    private static Map<String, Document> documentMap = new HashMap<String, Document>();

    private static SAXBuilder builder = new SAXBuilder();

    private File documentPath = null;

    private File destinationPath;

    private Element chapterElement = null;

    private List<String> chapterSavedList = new LinkedList<String>();

    private List<DocBookDocument> tableList = new ArrayList<DocBookDocument>();

    private static Map<String, Integer> filenamePrefixIndexMap = new HashMap<String, Integer>();

    public Converter(URL destinationUrl) {
        destinationPath = new File(destinationUrl.getPath());
        if (!destinationPath.isDirectory()) {
            if (destinationPath.mkdirs()) {
                System.out.println("Created dir '" + destinationPath + "'.");
            } else {
                System.out.println("Unable to create dir '" + destinationPath + "'.");
            }
        }
        documentMap.put("xxx", new Document());
        documentMap.put("main", new Document());
    }

    /**
	 * Creates the destination directory for the document, which is split and 
	 * converted. Within that directory several sub directories will be  
	 * created for the different parts of the split document.
	 * <pre>
	 * - ${destinationPath}
	 *   - ${newDocumentName}
	 *     - appendix
	 *     - chapter
	 *     - image
	 *     - table   
	 * </pre>
	 * @param destinationPath - the destination for the document
	 * @param documentName - the name of the document (directory)
	 * @return The created directory, where the document files can be written.
	 * @throws IOException In case the directories can not be created.
	 */
    public File createDirectoryStructure(File destinationPath, String documentName) throws IOException {
        String directoryStructure[] = new String[] { "image" };
        File documentPath = new File(destinationPath, documentName);
        if (!documentPath.isDirectory()) {
            documentPath.mkdir();
        }
        for (String dirName : directoryStructure) {
            File path = new File(documentPath, dirName);
            if (!path.isDirectory()) {
                path.mkdirs();
            }
        }
        return documentPath;
    }

    /**
	 * See {@link Converter}.
	 *  
	 * @param argArray
	 *        argArray[0] - the name of NEW document (directory)
     *        argArray[1] - the URL of the DocBook file to split and convert
	 */
    public static void main(String[] argArray) {
        String workPath = System.getProperty("user.dir");
        System.out.println("working dir: " + workPath);
        URL destinationUrl = null;
        if (argArray.length != 2) {
            System.out.println("Usage: " + Converter.class.getName() + "  <new document name>  <document file>");
            argArray = new String[2];
            argArray[0] = "SNP_CUSIP_DB_10";
            argArray[1] = "file://" + workPath + "/src/main/data/docs/" + "SNP_CUSIP_DB_10_MAd.xml";
            argArray[0] = "reuters_datascope_equities_30";
            argArray[1] = "file://" + workPath + "/src/main/data/docs/" + "REUTERSDATASCOPE30_RNg.xml";
            argArray[0] = "bloomberg_v3.1";
            argArray[1] = "file://" + workPath + "/src/main/data/docs/" + "BLOOMBERG31_UGg.docbook.xml";
            argArray[0] = "occ-encode-dds_v1.0";
            argArray[1] = "file://home/tverhagen/data/eclipse_ws/dev_snp_ratingsxpress/occ-encore-dds/OCC_ENCORE_DDS_1-0_MAb.xml";
            URL documentUrl;
            try {
                destinationUrl = new URL("file://" + workPath + "/target/converted/");
                documentUrl = new URL(argArray[1]);
                File documentFile = new File(documentUrl.getFile());
                if (!documentFile.isFile()) {
                    System.exit(0);
                }
            } catch (MalformedURLException e) {
                System.exit(0);
            }
        }
        System.out.println("doc new name '" + argArray[0] + "'  doc location '" + argArray[1] + "'.");
        try {
            URL documentUrl = new URL(argArray[1]);
            if (destinationUrl == null) {
                File path = new File(documentUrl.getPath());
                if (path.isFile()) {
                    path = path.getParentFile();
                }
                destinationUrl = new URL("file://" + path.getAbsolutePath() + "/converted");
            }
            Converter converter = new Converter(destinationUrl);
            converter.convert(argArray[0], documentUrl);
        } catch (MalformedURLException murle) {
            System.err.println(murle);
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void convert(String newDocumentName, URL url) throws IOException {
        documentPath = createDirectoryStructure(this.destinationPath, newDocumentName);
        try {
            Document doc = builder.build(url.openStream());
            Element elementx = doc.getRootElement();
            convertElement(elementx);
            System.out.println("\n\n");
            XMLOutputter outp = new XMLOutputter(Format.getPrettyFormat());
            System.out.println("as file: " + url.getFile());
            File inputFile = new File(url.getFile());
            File outputFile = new File(documentPath, renameFileExtention(inputFile, "-remaining.xml"));
            System.out.println("outputFile: " + outputFile);
            outp.output(doc, new FileOutputStream(outputFile));
        } catch (JDOMException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String renameFileExtention(File inputFile, String extention) {
        return inputFile.getName().replaceFirst(PATTERN_FILE_SUFFIX, (extention.indexOf(".") > -1 ? extention : "." + extention));
    }

    private void saveChapter() {
        if (chapterElement != null) {
            save(new DocBookDocument(chapterElement, DocBookDocument.TITLE_SEPARATOR_DASH));
        }
    }

    private void convertElement(Element element) {
        convertElement(0, element);
        saveChapter();
        int index = 0;
        for (DocBookDocument doc : tableList) {
            save(doc);
        }
        createMainDocument(chapterSavedList);
        for (String filename : chapterSavedList) {
            System.out.println("  - " + filename);
        }
    }

    private void createMainDocument(List<String> chapterList) {
        Document doc = new Document();
        Element rootElement = new Element("book");
        doc.setRootElement(rootElement);
        for (String filename : chapterList) {
            System.out.println("TODO  Create main document with chapter references?" + filename);
        }
    }

    @SuppressWarnings("unchecked")
    private void convertElement(int depth, Element element) {
        String text = null;
        if ("anchor".equals(element.getName())) {
            addForRemoval(element);
            text = "remove";
        }
        if (element.getName().startsWith("sect")) {
            text = "renamed: " + element.getName();
            if ("sect1".equals(element.getName())) {
                element.setName("chapter");
                saveChapter();
                chapterElement = element;
                addForRemoval(element);
            } else {
                element.setName("section");
                if (element.getChild("title") != null) {
                    Element titleElement = element.getChild("title");
                    String title = titleElement.getTextNormalize();
                    Matcher matcher = issueTitleIdPattern.matcher(title);
                    if (matcher.matches()) {
                        String idStr = matcher.group(1);
                        element.setAttribute("id", "section-bz-" + idStr);
                        titleElement.setText("BZ." + title);
                        element.setName("simplesect");
                    }
                }
            }
        }
        if ("orderedlist".equals(element.getName())) {
            text = "renamed: " + element.getName();
            element.setName("itemizedlist");
        } else if ("informaltable".equals(element.getName())) {
            element.setName("table");
            List parentChilderenList = element.getParentElement().getChildren();
            int tableIndex = parentChilderenList.indexOf(element);
            System.out.println(parentChilderenList.size() + "  " + tableIndex);
            if (tableIndex > 0) {
                Element previousChild = (Element) parentChilderenList.get(tableIndex - 1);
                if ((previousChild).getName().equals("itemizedlist")) {
                    if (previousChild.getChildren().size() == 1 && ((Element) previousChild.getChildren().get(0)).getChildren().size() == 1) {
                        String title = ((Element) previousChild.getChildren().get(0)).getChildText("para");
                        if (title != null) {
                            Element titleElement = new Element("title");
                            element.addContent(0, titleElement);
                            titleElement.setText(title);
                            text = "renamed: 'informaltable';  added title: '" + title + "';  remove: 'itemizedlist'";
                            addForRemoval(previousChild);
                        }
                    }
                }
            }
            DocBookDocument docBookDocument = new DocBookDocument(element, DocBookDocument.TITLE_SEPARATOR_DASH);
            tableList.add(docBookDocument);
            addForAddition(new DocBookContent(element, createDocBookCaution("FIXME: Here a " + element.getName() + " has been extracted, which is placed in the file '" + docBookDocument.getFilename() + "'.")));
            addForRemoval(element);
        } else if ("para".equals(element.getName())) {
            if (element.getTextNormalize().length() > 100) {
                System.out.println("para length: " + element.getTextNormalize().length());
            }
        }
        if (element.getContentSize() == 0 && element.getAttributes().size() == 0) {
            text = "remove";
        }
        String depthStr = createDepthString(depth, "  ");
        if (text == null) {
            if ("title".equals(element.getName())) {
                System.out.println(depthStr + element.getName() + "  " + element.getText());
            } else {
                System.out.println(depthStr + element.getName());
            }
        } else {
            System.out.println(depthStr + element.getName() + "  [" + text + "]");
        }
        if ((depth > 0 && element.getParentElement() != null && element.getChildren().size() > 0) || (depth == 0 && element.getChildren().size() > 0)) {
            Iterator<Element> iter = element.getChildren().iterator();
            while (iter.hasNext()) {
                Element childElement = iter.next();
                convertElement(depth + 1, childElement);
            }
        }
        if (addElementMap.get(element) != null) {
            List list = addElementMap.get(element);
            Iterator<DocBookContent> iter = list.iterator();
            while (iter.hasNext()) {
                DocBookContent nextElement = iter.next();
                nextElement.add();
            }
        }
        if (removeElementMap.get(element) != null) {
            List list = removeElementMap.get(element);
            Iterator<Element> iter = list.iterator();
            while (iter.hasNext()) {
                Element nextElement = iter.next();
                if (element.removeContent(nextElement)) {
                    System.out.println("Succesfully removed element '" + nextElement.getName() + "'");
                } else {
                    System.out.println("Unable to remove element '" + nextElement.getName() + "'");
                }
            }
        }
    }

    private Element createDocBookCaution(String text) {
        Element element = new Element("caution");
        element.addContent(createDocBookPara(text));
        return element;
    }

    private Element createDocBookPara(String text) {
        Element paraElement = new Element("para");
        paraElement.setText(text);
        return paraElement;
    }

    private void addForRemoval(Element element) {
        if (!removeElementMap.containsKey(element.getParentElement())) {
            removeElementMap.put(element.getParentElement(), new LinkedList<Element>());
        }
        List<Element> list = removeElementMap.get(element.getParentElement());
        list.add(element);
    }

    private void addForAddition(DocBookContent docBookContent) {
        if (!addElementMap.containsKey(docBookContent.getElement().getParentElement())) {
            addElementMap.put(docBookContent.getElement().getParentElement(), new LinkedList<DocBookContent>());
        }
        List<DocBookContent> list = addElementMap.get(docBookContent.getElement().getParentElement());
        list.add(docBookContent);
    }

    private void save(DocBookDocument doc) {
        XMLOutputter outp = new XMLOutputter(Format.getPrettyFormat());
        String filename = doc.getFilename();
        System.out.println("Creating new document '" + filename + "'.");
        File outputFile = new File(documentPath, filename);
        FileOutputStream out = null;
        Element rootElement = doc.getDocument().getRootElement();
        try {
            out = new FileOutputStream(outputFile);
            outp.output(doc.getDocument(), out);
            if (rootElement.getName().equals("chapter")) {
                chapterSavedList.add(filename);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if ("table".equals(rootElement.getName())) {
            saveAsCsv(doc.getDocument(), new File(documentPath, filename.replaceAll(PATTERN_FILE_SUFFIX, ".tsv")));
        }
    }

    public static Integer getIndex(Element element) {
        return getIndex(element.getName());
    }

    public static Integer getIndex(String message) {
        Integer index;
        if (filenamePrefixIndexMap.get(message) == null) {
            index = new Integer(1);
        } else {
            index = filenamePrefixIndexMap.get(message) + 1;
        }
        filenamePrefixIndexMap.put(message, index);
        return index;
    }

    public static Integer peekNextIndex(Element element) {
        return peekIndex(element.getName()) + 1;
    }

    public static Integer peekIndex(String message) {
        if (filenamePrefixIndexMap.get(message) == null) {
            return 0;
        }
        return filenamePrefixIndexMap.get(message);
    }

    private static String createDepthString(int depth, String prefixStr) {
        StringBuffer strBuf = new StringBuffer(prefixStr.length() * depth);
        for (int index = 0; index < depth; index++) {
            strBuf.append(prefixStr);
        }
        return strBuf.toString();
    }

    private void saveAsCsv(Document tableDoc, File filename) {
        Element rootElement = tableDoc.getRootElement();
        Element tgroupElement = rootElement.getChild("tgroup");
        Element theadElement = tgroupElement.getChild("thead");
        Element tbodyElement = tgroupElement.getChild("tbody");
        FileWriter writer = null;
        try {
            writer = new FileWriter(filename);
            write(theadElement, writer);
            write(tbodyElement, writer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void write(Element baseElement, FileWriter writer) throws IOException {
        if (baseElement != null && baseElement.getChild("row") != null) {
            List<Element> elementChildren = baseElement.getChildren("row");
            for (Element rowElement : elementChildren) {
                List<Element> rowChildren = rowElement.getChildren();
                StringBuilder builder = new StringBuilder();
                for (Element rowChildElement : rowChildren) {
                    System.out.println("    " + rowChildElement.getName() + "  " + rowChildElement.getTextNormalize());
                    for (Element dataElement : (List<Element>) rowChildElement.getChildren()) {
                        System.out.println("    data " + dataElement.getName() + " " + dataElement.getTextNormalize());
                        if (builder.length() > 0) {
                            builder.append(CSV_FIELD_SEPARATOR);
                        }
                        builder.append(dataElement.getTextNormalize().trim());
                    }
                }
                writer.append(builder.toString()).append(CSV_ROW_SEPARATOR);
            }
        }
    }
}
