package org.fetlar.spectatus.model;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.fetlar.spectatus.utilities.IMSMDPrefixMapper;
import org.fetlar.spectatus.utilities.IMSManifestPrefixMapper;
import org.fetlar.spectatus.utilities.IMSQTINamespacePrefixMapper;
import org.fetlar.spectatus.utilities.MiscUtils;
import org.imsglobal.xsd.imscp_v1p1.DependencyType;
import org.imsglobal.xsd.imscp_v1p1.FileType;
import org.imsglobal.xsd.imscp_v1p1.ManifestType;
import org.imsglobal.xsd.imscp_v1p1.MetadataType;
import org.imsglobal.xsd.imscp_v1p1.OrganizationsType;
import org.imsglobal.xsd.imscp_v1p1.ResourceType;
import org.imsglobal.xsd.imscp_v1p1.ResourcesType;
import org.imsglobal.xsd.imsmd_v1p2.DescriptionType;
import org.imsglobal.xsd.imsmd_v1p2.GeneralType;
import org.imsglobal.xsd.imsmd_v1p2.LangstringType;
import org.imsglobal.xsd.imsmd_v1p2.LomType;
import org.imsglobal.xsd.imsqti_v2p1.AssessmentItemType;
import org.imsglobal.xsd.imsqti_v2p1.AssessmentTestType;
import org.imsglobal.xsd.imsqti_v2p1.ShowHideType;
import org.imsglobal.xsd.imsqti_v2p1.TestFeedbackType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Helper class for file operations
 * Our main purpose here is to abstract all the bumpf of JAXB, and as much file IO as possible
 * @author paul
 *
 */
public class FileOps {

    public static final String tempdir = System.getProperty("java.io.tmpdir");

    public static final String sep = System.getProperty("file.separator");

    public static final int PURE = 1;

    public static final int QTIENGINE = 2;

    public static final int HYBRID = 3;

    /** imsqti JAXB objects */
    public Marshaller imsqtiMarshaller = null;

    private Unmarshaller imsqtiUnmarshaller = null;

    private JAXBContext imsqtiJC = null;

    /** The ims manifest JAXB objects */
    private Marshaller imsmanifestMarshaller = null;

    private Unmarshaller imsmanifestUnmarshaller = null;

    private JAXBContext imsmanifestJC = null;

    /** IMS metadata JAXB objects */
    private Marshaller imsMetaMarshaller = null;

    private Unmarshaller imsMetaUnmarshaller = null;

    private JAXBContext imsMetaJC = null;

    /** The taxonomy */
    private ArrayList<String> taxon;

    /**
	 * only gets set if something goes wrong during FileOp helper construction
	 */
    public boolean errorCondition = false;

    /**
	 * Default constructor. Basically, set up all the JAXB bumpf ready to go, and pull in the taxon
	 */
    public FileOps() {
        try {
            imsqtiJC = JAXBContext.newInstance("org.imsglobal.xsd.imsqti_v2p1", getClass().getClassLoader());
            imsqtiMarshaller = imsqtiJC.createMarshaller();
            imsqtiMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            imsqtiMarshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", new IMSQTINamespacePrefixMapper());
            imsqtiMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            imsqtiMarshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.imsglobal.org/xsd/imsqti_v2p1 imsqti_v2p1.xsd");
            imsqtiUnmarshaller = imsqtiJC.createUnmarshaller();
            imsMetaJC = JAXBContext.newInstance("org.imsglobal.xsd.imsmd_v1p2", getClass().getClassLoader());
            imsMetaMarshaller = imsMetaJC.createMarshaller();
            imsMetaMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            imsMetaMarshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", new IMSMDPrefixMapper());
            imsMetaMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            imsMetaUnmarshaller = imsMetaJC.createUnmarshaller();
            imsmanifestJC = JAXBContext.newInstance("org.imsglobal.xsd.imscp_v1p1", getClass().getClassLoader());
            imsmanifestMarshaller = imsmanifestJC.createMarshaller();
            imsmanifestMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            imsmanifestMarshaller.setProperty("com.sun.xml.internal.bind.namespacePrefixMapper", new IMSManifestPrefixMapper());
            imsmanifestMarshaller.setProperty(Marshaller.JAXB_ENCODING, "UTF-8");
            imsmanifestMarshaller.setProperty(Marshaller.JAXB_SCHEMA_LOCATION, "http://www.imsglobal.org/xsd/imscp_v1p1 http://www.imsglobal.org/xsd/imscp_v1p2.xsd http://www.imsglobal.org/xsd/imsmd_v1p2 http://www.imsglobal.org/xsd/imsmd_v1p2p4.xsd http://www.imsglobal.org/xsd/imsqti_v2p1 http://www.imsglobal.org/xsd/imsqti_v2p1.xsd");
            imsmanifestUnmarshaller = imsmanifestJC.createUnmarshaller();
            setTaxon(MiscUtils.getTaxon());
        } catch (JAXBException e) {
            errorCondition = true;
        }
    }

    /**
	 * Read text file from disk
	 * returns null if error
	 */
    public String getTextFile(String filename) {
        try {
            String fileContents = FileUtils.readFileToString(new File(filename), "UTF-8");
            return fileContents;
        } catch (Exception e) {
            return "";
        }
    }

    public Document getDocForAssItem(AssessmentItemType assItem) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder loader = factory.newDocumentBuilder();
            factory.setNamespaceAware(true);
            Document document = loader.newDocument();
            JAXBElement je = new JAXBElement<AssessmentItemType>(new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "assessmentItem"), AssessmentItemType.class, assItem);
            imsqtiMarshaller.marshal(je, document);
            return document;
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Read question from XML file, generate a base (blank) metadata and add it in
	 * a directory of its own to the base test CP folder.
	 * @param filename
	 * @param testLocation
	 * @return
	 */
    public Question getQuestionFromXML(String filename, String testLocation) {
        Question question = new Question();
        try {
            question.setQtiAssItem(getAssessmentItemFromDisk(filename));
        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
        Document qDoc = getDocForAssItem(question.getQtiAssItem());
        question.setSourceAsDoc(qDoc);
        long size = ((new File(filename)).length());
        question.setSize(size);
        String id = "id-" + MiscUtils.miniUUID();
        String newQdir = testLocation + sep + id;
        new File(newQdir).mkdir();
        try {
            FileUtils.copyFileToDirectory(new File(filename), new File(newQdir));
        } catch (IOException e) {
            e.printStackTrace();
        }
        String testBaseDir = new File(testLocation).getAbsolutePath();
        String replacementString;
        if (System.getProperty("os.name").contains("Linux")) {
            replacementString = tempdir + "/spectemp";
        } else {
            replacementString = tempdir + "spectemp";
        }
        String relativeTestDir = testBaseDir.replace(replacementString, "");
        if (sep.equals("\\")) {
            testLocation = testLocation.replaceAll("\\\\", "/");
            testBaseDir = testBaseDir.replaceAll("\\\\", "/");
            relativeTestDir = relativeTestDir.replaceAll("\\\\", "/");
            filename = filename.replaceAll("\\\\", "/");
        }
        if (!relativeTestDir.equals("")) {
            if (relativeTestDir.charAt(0) == '/') relativeTestDir = relativeTestDir.substring(1, relativeTestDir.length());
            if (relativeTestDir.charAt(relativeTestDir.length() - 1) != '/') relativeTestDir = relativeTestDir + "/";
        }
        String[] x;
        x = filename.split("/");
        String justTheFilename = x[x.length - 1];
        question.setMyid(id);
        question.setRefId(id);
        question.setHref(id + "/" + justTheFilename);
        question.setBaseHref(relativeTestDir + question.getHref());
        FileType ft = new FileType();
        ft.setHref(question.getBaseHref());
        question.getFiles().add(ft);
        MetadataType metadata = new MetadataType();
        LomType lom = new LomType();
        GeneralType general = new GeneralType();
        DescriptionType desc = new DescriptionType();
        LangstringType langStr = new LangstringType();
        langStr.setLang("en");
        langStr.setValue("This question is an XML file imported into Spectatus.");
        desc.getLangstring().add(langStr);
        general.getContent().add(desc);
        lom.setGeneral(general);
        try {
            metadata.getAny().add(lomToDoc(lom).getDocumentElement());
        } catch (JAXBException e) {
            e.printStackTrace();
        }
        question.setMetadata(metadata);
        return question;
    }

    /**
	 * Exports a question to a content package.
	 * @param question
	 * @param zipfilename
	 * @param cp
	 * @return true if successful, false if not
	 */
    public boolean saveQuestionToZip(Question question, String zipfilename, AssessmentContentPackage cp) {
        ManifestType cpMani = createRootManifest(cp, FileOps.PURE);
        ResourceType qRes = null;
        for (ResourceType rt : cpMani.getResources().getResource()) {
            if (rt.getHref().contains(question.getHref())) qRes = rt;
            if (rt.getIdentifier().equals(question.getMyid())) qRes = rt;
        }
        if (qRes == null) {
            return false;
        }
        String replacementString;
        String originalString;
        if (System.getProperty("os.name").contains("Linux")) {
            originalString = tempdir + "/spectemp";
            replacementString = tempdir + "/tempqexport";
        } else {
            originalString = tempdir + "spectemp";
            replacementString = tempdir + "tempqexport";
        }
        deleteDirectory(new File(replacementString));
        for (FileType f : qRes.getFile()) {
            String originalFilename = originalString + "/" + f.getHref();
            String newFilename = replacementString + "/" + f.getHref();
            try {
                FileUtils.copyFile(new File(originalFilename), new File(newFilename));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ManifestType qMani = new ManifestType();
        qMani.setMetadata(question.getMetadata());
        qMani.setResources(new ResourcesType());
        qMani.getResources().getResource().add(qRes);
        try {
            saveManifestToDisk(qMani, replacementString + "/imsmanifest.xml");
            zipDirectory(new File(replacementString), new File(zipfilename));
            deleteDirectory(new File(replacementString));
        } catch (Exception e) {
            e.printStackTrace();
            deleteDirectory(new File(replacementString));
            return false;
        }
        return true;
    }

    /**
	 * Read assessmentItem from content package zip file and add it to the 
	 * base test CP folder
	 */
    public Question getQuestionFromCP(String filename, String testLocation) {
        boolean itsAtest = false;
        try {
            String testBaseDir = new File(testLocation).getAbsolutePath();
            String replacementString;
            if (System.getProperty("os.name").contains("Linux")) {
                replacementString = tempdir + "/spectemp";
            } else {
                replacementString = tempdir + "spectemp";
            }
            String relativeTestDir = testBaseDir.replace(replacementString, "");
            if (sep.equals("\\")) {
                testLocation = testLocation.replaceAll("\\\\", "/");
                testBaseDir = testBaseDir.replaceAll("\\\\", "/");
                relativeTestDir = relativeTestDir.replaceAll("\\\\", "/");
            }
            if (!relativeTestDir.equals("")) {
                if (relativeTestDir.charAt(0) == '/') relativeTestDir = relativeTestDir.substring(1, relativeTestDir.length());
                if (relativeTestDir.charAt(relativeTestDir.length() - 1) != '/') relativeTestDir = relativeTestDir + "/";
            }
            String destDir = FileOps.tempdir + FileOps.sep + "tempunzip";
            long size = (new File(filename)).length();
            unzip(new File(filename), new File(destDir));
            String tempqFilename = getQFilenameFromManifest(destDir + sep + "imsmanifest.xml");
            ManifestType qManifest = getManifestFromDisk(destDir + sep + "imsmanifest.xml");
            MetadataType qMetadata = null;
            List<FileType> qFiles = null;
            List<ResourceType> resources = qManifest.getResources().getResource();
            for (ResourceType resource : resources) {
                if ((resource.getType().equals("imsqti_item_xmlv2p0")) || (resource.getType().equals("imsqti_item_xmlv2p1"))) {
                    qMetadata = resource.getMetadata();
                    qFiles = resource.getFile();
                }
                if ((resource.getType().equals("imsqti_test_xmlv2p1")) || (resource.getType().equals("imsqti_assessment_xmlv2p1"))) {
                    itsAtest = true;
                    return null;
                }
            }
            for (int i = 0; i < qMetadata.getAny().size(); i++) {
                try {
                    Element item = (Element) qMetadata.getAny().get(i);
                    if (item.getNodeName().endsWith(":lom")) {
                        JAXBElement<LomType> testLom = (JAXBElement<LomType>) imsMetaUnmarshaller.unmarshal((Element) item);
                        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                        DocumentBuilder loader = factory.newDocumentBuilder();
                        factory.setNamespaceAware(true);
                        Document document = loader.newDocument();
                        imsMetaMarshaller.marshal(testLom, document);
                        qMetadata.getAny().set(i, document.getDocumentElement());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            File ourQuestionFile = new File(tempqFilename);
            String ourQuestionFullFilename = ourQuestionFile.getAbsolutePath();
            String[] x;
            if (sep.equals("\\")) {
                x = ourQuestionFullFilename.split("\\\\");
            } else {
                x = ourQuestionFullFilename.split(sep);
            }
            String ourQuestionFilename = x[x.length - 1];
            String id = "id-" + MiscUtils.miniUUID();
            File originalDir = new File(destDir + sep);
            File dest = new File(testLocation + sep + id + sep);
            FileUtils.moveDirectory(originalDir, dest);
            String qFilename = dest.getAbsolutePath() + FileOps.sep + tempqFilename;
            AssessmentItemType qtiQ = getAssessmentItemFromDisk(qFilename);
            Question ourQuestion = new Question();
            ourQuestion.setSize(size);
            ourQuestion.setQtiAssItem(qtiQ);
            Document qDoc = getDocForAssItem(ourQuestion.getQtiAssItem());
            ourQuestion.setSourceAsDoc(qDoc);
            ourQuestion.setMyid(id);
            ourQuestion.setRefId(id);
            if (qMetadata != null) ourQuestion.setMetadata(qMetadata);
            if (qFiles != null) {
                for (FileType f : qFiles) {
                    f.setHref(relativeTestDir + id + "/" + f.getHref());
                }
                ourQuestion.setFiles(qFiles);
            }
            ourQuestion.setHref(id + "/" + tempqFilename);
            ourQuestion.setBaseHref(relativeTestDir + ourQuestion.getHref());
            return ourQuestion;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Read test from disk
	 * @param filename of the test to load
	 * @return the test object
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws XPathExpressionException 
	 */
    public AssessmentTestType getTestFromDisk(String filename) throws JAXBException {
        String fileContents = getTextFile(filename);
        fileContents = divRubrics(fileContents);
        ByteArrayInputStream fileStream = null;
        try {
            fileStream = new ByteArrayInputStream(fileContents.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
        }
        JAXBElement<AssessmentTestType> testJE = (JAXBElement<AssessmentTestType>) imsqtiUnmarshaller.unmarshal(fileStream);
        AssessmentTestType ourTest = testJE.getValue();
        return ourTest;
    }

    /**
	 * Reads a manifest from disk
	 */
    public ManifestType getManifestFromDisk(String filename) throws JAXBException {
        JAXBElement<ManifestType> manifestJE = (JAXBElement<ManifestType>) imsmanifestUnmarshaller.unmarshal(new File(filename));
        return manifestJE.getValue();
    }

    /**
	 * Reads a question from disk
	 * @param filename filename of the question to load
	 * @return returns an AssItemType
	 * @throws JAXBException
	 */
    public AssessmentItemType getAssessmentItemFromDisk(String filename) throws JAXBException {
        String fileContents = getTextFile(filename);
        fileContents = fileContents.replace("imsqti_v2p0", "imsqti_v2p1");
        ByteArrayInputStream fileStream = null;
        try {
            fileStream = new ByteArrayInputStream(fileContents.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException ex) {
        }
        JAXBElement<AssessmentItemType> assJE = (JAXBElement<AssessmentItemType>) imsqtiUnmarshaller.unmarshal(fileStream);
        return assJE.getValue();
    }

    /**
	 * Saves a specific test to disk
	 * @param test to be saved
	 * @param filename where to save it
	 */
    public void saveTestToDisk(AssessmentTestType test, String filename) throws JAXBException {
        JAXBElement je = new JAXBElement<AssessmentTestType>(new QName("http://www.imsglobal.org/xsd/imsqti_v2p1", "assessmentTest"), AssessmentTestType.class, test);
        imsqtiMarshaller.marshal(je, new File(filename));
    }

    /**
	 * Saves a manifest out to disk
	 * @param manifest
	 * @param filename
	 * @throws JAXBException
	 */
    public void saveManifestToDisk(ManifestType manifest, String filename) throws JAXBException {
        JAXBElement je = new JAXBElement<ManifestType>(new QName("http://www.imsglobal.org/xsd/imscp_v1p1", "manifest"), ManifestType.class, manifest);
        imsmanifestMarshaller.marshal(je, new File(filename));
    }

    /**
	 * Returns a metadata/lom as a document
	 * @param metadata
	 * @param filename
	 * @throws JAXBException
	 */
    public Document lomToDoc(LomType lom) throws JAXBException {
        Document document = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder loader = factory.newDocumentBuilder();
            factory.setNamespaceAware(true);
            document = loader.newDocument();
            JAXBElement<LomType> je = new JAXBElement<LomType>(new QName("http://www.imsglobal.org/xsd/imsmd_v1p2", "lom"), LomType.class, lom);
            imsMetaMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
            imsMetaMarshaller.marshal(je, document);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return document;
    }

    /**
	 * Deletes files that comprise a question.
	 * @param q
	 * @param basePath - path to look for the files in - usually the CP manifest path
	 */
    public void deleteQuestionFiles(Question q, String basePath) {
        for (FileType f : q.getFiles()) {
            new File(basePath + "/" + f.getHref()).delete();
        }
        try {
            deleteEmptyFolders(basePath);
            deleteEmptyFolders(basePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
	 * Delete empty directories
	 * Nabbed from http://binodsuman.blogspot.com/2009/07/how-to-delete-recursively-empty-folder.html
	 * @param folderName
	 * @throws FileNotFoundException
	 */
    public static void deleteEmptyFolders(String folderName) throws FileNotFoundException {
        File aStartingDir = new File(folderName);
        List<File> emptyFolders = new ArrayList<File>();
        findEmptyFoldersInDir(aStartingDir, emptyFolders);
        List<String> fileNames = new ArrayList<String>();
        for (File f : emptyFolders) {
            String s = f.getAbsolutePath();
            fileNames.add(s);
        }
        for (File f : emptyFolders) {
            boolean isDeleted = f.delete();
            if (isDeleted) {
            }
        }
    }

    /**
	 * Find empty directories
	 * Also nabbed from http://binodsuman.blogspot.com/2009/07/how-to-delete-recursively-empty-folder.html
	 * and slightly modified to consider vestigial manifests.
	 * @param folder
	 * @param emptyFolders
	 * @return
	 */
    public static boolean findEmptyFoldersInDir(File folder, List<File> emptyFolders) {
        boolean isEmpty = false;
        File[] filesAndDirs = folder.listFiles();
        List<File> filesDirs = Arrays.asList(filesAndDirs);
        int dirSize = filesDirs.size();
        if (dirSize == 0) {
            isEmpty = true;
        }
        if (dirSize == 1) {
            if (filesDirs.get(0).getAbsolutePath().contains("imsmanifest.xml")) {
                filesDirs.get(0).delete();
                dirSize = 0;
                isEmpty = true;
            }
        }
        if (dirSize > 0) {
            boolean allDirsEmpty = true;
            boolean noFiles = true;
            for (File file : filesDirs) {
                if (!file.isFile()) {
                    boolean isEmptyChild = findEmptyFoldersInDir(file, emptyFolders);
                    if (!isEmptyChild) {
                        allDirsEmpty = false;
                    }
                }
                if (file.isFile()) {
                    noFiles = false;
                }
            }
            if (noFiles == true && allDirsEmpty == true) {
                isEmpty = true;
            }
        }
        if (isEmpty) {
            emptyFolders.add(folder);
        }
        return isEmpty;
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public final void zipDirectory(File directory, File zip) throws IOException {
        ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zip));
        zip(directory, directory, zos);
        zos.close();
    }

    private final void zip(File directory, File base, ZipOutputStream zos) throws IOException {
        File[] files = directory.listFiles();
        byte[] buffer = new byte[8192];
        int read = 0;
        for (int i = 0, n = files.length; i < n; i++) {
            if (files[i].isDirectory()) {
                zip(files[i], base, zos);
            } else {
                FileInputStream in = new FileInputStream(files[i]);
                String entryStr = files[i].getPath().substring(base.getPath().length() + 1);
                entryStr = entryStr.replaceAll("\\\\", "/");
                ZipEntry entry = new ZipEntry(entryStr);
                zos.putNextEntry(entry);
                while (-1 != (read = in.read(buffer))) {
                    zos.write(buffer, 0, read);
                }
                in.close();
            }
        }
    }

    public final void unzip(File zip, File extractTo) throws IOException {
        deleteDirectory(extractTo);
        ZipFile archive = new ZipFile(zip);
        Enumeration e = archive.entries();
        while (e.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) e.nextElement();
            File file = new File(extractTo, entry.getName());
            if (entry.isDirectory() && !file.exists()) {
                file.mkdirs();
            } else {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                InputStream in = archive.getInputStream(entry);
                BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
                byte[] buffer = new byte[8192];
                int read;
                while (-1 != (read = in.read(buffer))) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.close();
            }
        }
    }

    /**
	 * Converts a node to string
	 * @param node to convert
	 * @return XML string of converted node
	 */
    public static String nodeToString(Node node) {
        try {
            Source source = new DOMSource(node);
            StringWriter stringWriter = new StringWriter();
            Result result = new StreamResult(stringWriter);
            TransformerFactory factory = TransformerFactory.newInstance();
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(OutputKeys.MEDIA_TYPE, "UTF-8");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(source, result);
            return stringWriter.getBuffer().toString();
        } catch (TransformerConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * If any rubrics contain plain text then encapsulate this plain text in a DIV before going any further.
	 * This is needed because <rubricBlock>xxx</rubricBlock> is invalid QTI, but seems to be what Constructr
	 * churned out. <rubricBlock><div>xxxx</div></rubricBlock> is what it should be.
	 * @param xmlString string of XML to add divs into
	 * @return same string but with divs added in the right place, or null if procedure failed.
	 */
    private String divRubrics(String xmlString) {
        try {
            xmlString = xmlString.replaceAll("[\\s][\\s+]", " ");
            xmlString = xmlString.replaceAll("\\>[\\s+]\\<", "\\>\\<");
            ByteArrayInputStream fileStream = new ByteArrayInputStream(xmlString.getBytes());
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(fileStream);
            XPathFactory xpfactory = XPathFactory.newInstance();
            XPath xpath = xpfactory.newXPath();
            NamespaceContext nc = new NamespaceContext() {

                @Override
                public String getNamespaceURI(String prefix) {
                    return ("http://www.imsglobal.org/xsd/imsqti_v2p1");
                }

                @Override
                public String getPrefix(String namespaceURI) {
                    return "qti";
                }

                @Override
                public Iterator getPrefixes(String namespaceURI) {
                    return null;
                }
            };
            xpath.setNamespaceContext(nc);
            XPathExpression expr = xpath.compile("//qti:rubricBlock");
            Object result = expr.evaluate(doc, XPathConstants.NODESET);
            NodeList nodes = (NodeList) result;
            for (int i = 0; i < nodes.getLength(); i++) {
                Node bigNode = nodes.item(i);
                NodeList drilling = bigNode.getChildNodes();
                for (int x = 0; x < drilling.getLength(); x++) {
                    if (drilling.item(x).getNodeType() == Node.TEXT_NODE) {
                        Node toKill = drilling.item(x);
                        Node parent = toKill.getParentNode();
                        Node newNode = doc.createElement("div");
                        newNode.setTextContent(toKill.getTextContent());
                        parent.removeChild(toKill);
                        parent.appendChild(newNode);
                    }
                }
            }
            return nodeToString(doc);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Downloads a question from Minibix and stashes it in the temp working directory.
	 * No error trapping to check that you're either downloading a valid ticket no or that
	 * you're pulling a question rather than a test.
	 * @param ticketNo to download
	 */
    public void downloadQFromMinibix(int ticketNo) {
        String minibixDomain = Preferences.userRoot().node("Spectatus").get("MBAddr", "http://mathassess.caret.cam.ac.uk");
        String minibixPort = Preferences.userRoot().node("Spectatus").get("MBPort", "80");
        String url = minibixDomain + ":" + minibixPort + "/qtibank-webserv/deposits/all/" + ticketNo;
        File file = new File(tempdir + sep + "minibix.zip");
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(url);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream instream = entity.getContent();
                int l;
                byte[] tmp = new byte[2048];
                while ((l = instream.read(tmp)) != -1) {
                    out.write(tmp, 0, l);
                }
                out.close();
                instream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 * Reads a manifest from a question CP and returns the filename (actually the HREF)
	 * of where the question XML is within the zip.
	 * @param manifestFilename
	 * @return result
	 * @throws JAXBException
	 */
    public String getQFilenameFromManifest(String manifestFilename) throws JAXBException {
        String result = null;
        ManifestType manifest = getManifestFromDisk(manifestFilename);
        List<ResourceType> resources = manifest.getResources().getResource();
        for (ResourceType res : resources) {
            if (res.getType().contains("imsqti_item")) {
                result = res.getHref();
            }
        }
        return result;
    }

    public LomType getLomFromString(String lomString) throws JAXBException {
        JAXBElement<LomType> metadataJE = (JAXBElement<LomType>) imsMetaUnmarshaller.unmarshal(new ByteArrayInputStream(lomString.getBytes()));
        return metadataJE.getValue();
    }

    public LomType getLomFromDoc(Document lomDoc) throws JAXBException {
        return getLomFromString(nodeToString(lomDoc));
    }

    public ManifestType createRootManifest(AssessmentContentPackage cp, int testType) {
        ManifestType manifest = new ManifestType();
        ResourcesType resource = new ResourcesType();
        try {
            manifest.setOrganizations(new OrganizationsType());
        } catch (Exception e) {
        }
        try {
            manifest.setMetadata(cp.getManifest().getMetadata());
        } catch (Exception e) {
        }
        String testID = cp.getTestResId();
        testID = testID.replace("__QTIE__", "");
        ResourceType testResReal = new ResourceType();
        testResReal.setIdentifier(testID);
        if (testType == PURE) {
            testResReal.setType("imsqti_test_xmlv2p1");
        }
        if ((testType == QTIENGINE) || (testType == HYBRID)) {
            testResReal.setType("imsqti_assessment_xmlv2p1");
        }
        testResReal.setHref(cp.getTestXMLlocationHref());
        FileType testResFile = new FileType();
        testResFile.setHref(cp.getTestXMLlocationHref());
        testResReal.getFile().add(testResFile);
        ArrayList<DependencyType> dependencies = new ArrayList<DependencyType>();
        ArrayList<ResourceType> questionResources = new ArrayList<ResourceType>();
        for (Section s : cp.getSections()) {
            for (Question q : s.getQuestions()) {
                ResourceType qRes = new ResourceType();
                qRes.setIdentifier(q.getRefId());
                qRes.setType("imsqti_item_xmlv2p1");
                qRes.setHref(q.getBaseHref());
                if (q.getMetadata() != null) qRes.setMetadata(q.getMetadata());
                qRes.getFile().addAll(q.getFiles());
                questionResources.add(qRes);
                DependencyType qdep = new DependencyType();
                qdep.setIdentifierref(q.getRefId());
                dependencies.add(qdep);
            }
        }
        testResReal.getDependency().addAll(dependencies);
        resource.getResource().add(testResReal);
        if (testType == HYBRID) {
            JAXBElement<ResourceType> je2 = null;
            ResourceType testResQTIEngine = new ResourceType();
            StringWriter sw = new StringWriter();
            try {
                JAXBElement je = new JAXBElement<ResourceType>(new QName("http://www.imsglobal.org/xsd/imscp_v1p1", "resource"), ResourceType.class, testResReal);
                imsmanifestMarshaller.marshal(je, sw);
                je2 = (JAXBElement<ResourceType>) imsmanifestUnmarshaller.unmarshal(new ByteArrayInputStream(sw.toString().getBytes()));
                testResQTIEngine = je2.getValue();
            } catch (JAXBException e) {
            }
            testResQTIEngine.setType("imsqti_test_xmlv2p1");
            resource.getResource().add(testResQTIEngine);
        }
        resource.getResource().addAll(questionResources);
        manifest.setResources(resource);
        cp.setManifest(manifest);
        return manifest;
    }

    public void setTaxon(ArrayList<String> taxon) {
        this.taxon = taxon;
    }

    public ArrayList<String> getTaxon() {
        return taxon;
    }
}
