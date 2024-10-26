package org.jmol.fah;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Vector;
import java.util.zip.ZipInputStream;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jmol.fah.utils.XMLValue;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

/**
 * Manage project information
 */
public class ProjectInformation {

    Vector _projectInfo;

    private static final boolean _outputEmprotz = false;

    private static final boolean _checkCurrentXyzFiles = true;

    private static final boolean _checkActiveMissing = true;

    private static boolean _local = true;

    private static ProjectInformation _info = null;

    private static final String _txtEM = "(EM) ";

    private static final String _txtPS = "(PS) ";

    private static final String _txtQD = "(QD) ";

    private static final String _txtS = "(S) ";

    private long _emDate;

    private long _emFileDate;

    private long _psDate;

    private long _psFileDate;

    private long _qdDate;

    private long _qdFileDate;

    private long _staticDate;

    /**
   * @return Project information
   */
    public static ProjectInformation getInstance() {
        if (_info == null) {
            _info = new ProjectInformation();
        }
        return _info;
    }

    /**
   * @param projectNum Project number
   * @return Informations for project
   */
    Information getInfo(int projectNum) {
        Information result = null;
        Vector infos = this._projectInfo;
        if ((projectNum >= 0) && (projectNum < infos.size())) {
            Object obj = this._projectInfo.elementAt(projectNum);
            if (obj instanceof Information) {
                result = (Information) obj;
            }
        }
        return result;
    }

    /**
   * @param projectNum Project number
   * @return Informations for project
   */
    Information createInfo(int projectNum) {
        if (this._projectInfo.size() <= projectNum) {
            this._projectInfo.setSize(projectNum + 1);
        }
        Information info = new Information();
        this._projectInfo.set(projectNum, info);
        return info;
    }

    /**
   * Get points value for a project
   * 
   * @param projectNum Project number
   * @return Points value for project
   */
    public Double getProjectValue(int projectNum) {
        Double value = null;
        Information info = getInfo(projectNum);
        if (info != null) {
            value = info._staticValue;
            long date = _info._staticDate;
            if ((info._emValue != null) && ((_info._emDate > date) || (value == null))) {
                value = info._emValue;
                date = _info._emDate;
            }
            if ((info._psValue != null) && ((_info._psDate > date) || (value == null))) {
                value = info._psValue;
                date = _info._psDate;
            }
            if ((info._qdValue != null) && ((_info._qdDate > date) || (value == null))) {
                value = info._qdValue;
                date = _info._qdDate;
            }
        }
        return value;
    }

    /**
   * Get name for a project
   * 
   * @param projectNum Project number
   * @return Name of the project
   */
    public String getProjectName(int projectNum) {
        String name = null;
        Information info = getInfo(projectNum);
        if (info != null) {
            name = info._staticName;
            long date = _info._staticDate;
            if ((info._psName != null) && ((_info._psDate > date) || (name == null))) {
                name = info._psName;
                date = _info._psDate;
            }
        }
        return name;
    }

    /**
   * Constructor for ProjectInformation
   */
    private ProjectInformation() {
        this._projectInfo = new Vector(2000);
        this._emDate = -1;
        this._emFileDate = -1;
        this._psDate = -1;
        this._psFileDate = -1;
        this._qdDate = -1;
        this._qdFileDate = -1;
        this._staticDate = new GregorianCalendar(2004, Calendar.JULY, 10).getTimeInMillis();
        addStaticInformation();
        addEMInformation();
        addPSCInformation();
        addPSInformation();
        addQDInformation();
    }

    /**
   * Add information from emprotz.dat
   */
    private void addEMInformation() {
        try {
            long emDate = System.currentTimeMillis();
            if (_local == true) {
                File emFile = new File("emprotz.dat");
                if (!emFile.exists()) {
                    return;
                }
                emDate = emFile.lastModified();
            }
            if (emDate > this._emFileDate) {
                this._emFileDate = emDate;
                this._emDate = emDate;
                for (int ii = 0; ii < this._projectInfo.size(); ii++) {
                    Information info = getInfo(ii);
                    if (info != null) {
                        info._emDeadline = null;
                        info._emFrames = null;
                        info._emValue = null;
                    }
                }
                Reader reader = null;
                if (_local == true) {
                    reader = new FileReader("emprotz.dat");
                } else {
                    StringBuffer urlName = new StringBuffer();
                    urlName.append("http://home.comcast.net/");
                    urlName.append("~wxdude1/emsite/download/");
                    urlName.append("emprotz.zip");
                    try {
                        URL url = new URL(urlName.toString());
                        InputStream stream = url.openStream();
                        ZipInputStream zip = new ZipInputStream(stream);
                        zip.getNextEntry();
                        reader = new InputStreamReader(zip);
                    } catch (MalformedURLException mue) {
                        mue.printStackTrace();
                    }
                }
                BufferedReader file = new BufferedReader(reader);
                try {
                    String line1 = null;
                    int count = 0;
                    while ((line1 = file.readLine()) != null) {
                        String line2 = (line1 != null) ? file.readLine() : null;
                        String line3 = (line2 != null) ? file.readLine() : null;
                        String line4 = (line3 != null) ? file.readLine() : null;
                        count++;
                        if ((count > 1) && (line1 != null) && (line2 != null) && (line3 != null) && (line4 != null)) {
                            if (line1.length() > 2) {
                                int posBegin = line1.indexOf("\"", 0);
                                int posEnd = line1.indexOf("\"", posBegin + 1);
                                if ((posBegin >= 0) && (posEnd >= 0)) {
                                    String project = line1.substring(posBegin + 1, posEnd - posBegin);
                                    int projectNum = Integer.parseInt(project);
                                    Integer deadline = Integer.valueOf(line2.trim());
                                    Double value = Double.valueOf(line3.trim());
                                    Integer frames = Integer.valueOf(line4.trim());
                                    Information info = getInfo(projectNum);
                                    if (info == null) {
                                        info = createInfo(projectNum);
                                    }
                                    if (info._emValue == null) {
                                        info._emDeadline = deadline;
                                        info._emFrames = frames;
                                        info._emValue = value;
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    file.close();
                }
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    /**
   * Add information from psummaryC.html
   */
    private void addPSCInformation() {
        try {
            long psDate = System.currentTimeMillis();
            if (_local == true) {
                File psFile = new File("psummary.html");
                if (!psFile.exists()) {
                    return;
                }
                psDate = psFile.lastModified();
            }
            if (psDate > this._psFileDate) {
                this._psFileDate = psDate;
                this._psDate = psDate;
                for (int ii = 0; ii < this._projectInfo.size(); ii++) {
                    Information info = getInfo(ii);
                    if (info != null) {
                        info._psAtoms = null;
                        info._psContact = null;
                        info._psCore = null;
                        info._psDeadline = null;
                        info._psFrames = null;
                        info._psName = null;
                        info._psPreferred = null;
                        info._psServer = null;
                        info._psValue = null;
                    }
                }
                Reader reader = null;
                if (_local == true) {
                    reader = new FileReader("psummary.html");
                } else {
                    StringBuffer urlName = new StringBuffer();
                    urlName.append("http://fah-web.stanford.edu/");
                    urlName.append("psummaryC.html");
                    try {
                        URL url = new URL(urlName.toString());
                        InputStream stream = url.openStream();
                        reader = new InputStreamReader(stream);
                    } catch (MalformedURLException mue) {
                        mue.printStackTrace();
                    }
                }
                HTMLDocument htmlDoc = new HTMLDocumentPSummaryC();
                HTMLEditorKit htmlEditor = new HTMLEditorKit();
                htmlEditor.read(reader, htmlDoc, 0);
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } catch (BadLocationException e) {
        }
    }

    /**
   * Add information from psummary.html
   */
    private void addPSInformation() {
        try {
            if (_local == true) {
                return;
            }
            Reader reader = null;
            if (_local == true) {
                reader = new FileReader("psummary.html");
            } else {
                StringBuffer urlName = new StringBuffer();
                urlName.append("http://vspx27.stanford.edu/");
                urlName.append("psummary.html");
                try {
                    URL url = new URL(urlName.toString());
                    InputStream stream = url.openStream();
                    reader = new InputStreamReader(stream);
                } catch (MalformedURLException mue) {
                    mue.printStackTrace();
                }
            }
            HTMLDocument htmlDoc = new HTMLDocumentPSummary();
            HTMLEditorKit htmlEditor = new HTMLEditorKit();
            htmlEditor.read(reader, htmlDoc, 0);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } catch (BadLocationException e) {
        }
    }

    /**
   * Add information from qdinfo.dat
   */
    private void addQDInformation() {
        try {
            long qdDate = System.currentTimeMillis();
            if (_local == true) {
                File qdFile = new File("qdinfo.dat");
                if (!qdFile.exists()) {
                    return;
                }
                qdDate = qdFile.lastModified();
            }
            if (qdDate > this._qdFileDate) {
                this._qdFileDate = qdDate;
                for (int ii = 0; ii < this._projectInfo.size(); ii++) {
                    Information info = getInfo(ii);
                    if (info != null) {
                        info._qdValue = null;
                    }
                }
                Reader reader = null;
                if (_local == true) {
                    reader = new FileReader("qdinfo.dat");
                } else {
                    StringBuffer urlName = new StringBuffer();
                    urlName.append("http://boston.quik.com/rph/");
                    urlName.append("qdinfo.dat");
                    try {
                        URL url = new URL(urlName.toString());
                        InputStream stream = url.openStream();
                        reader = new InputStreamReader(stream);
                    } catch (MalformedURLException mue) {
                        mue.printStackTrace();
                    }
                }
                BufferedReader file = new BufferedReader(reader);
                try {
                    String line = null;
                    while ((line = file.readLine()) != null) {
                        if (line.startsWith("pg ")) {
                            this._qdDate = Long.parseLong(line.substring(3), 16);
                            this._qdDate = (this._qdDate + 946684800) * 1000;
                        } else if (line.startsWith("pt ")) {
                            line = line.substring(3).trim();
                            int pos = -1;
                            while ((line.length() > 0) && ((pos = line.indexOf(' ')) > 0)) {
                                int projectNum = 0;
                                Double value = null;
                                if (pos > 0) {
                                    projectNum = Integer.parseInt(line.substring(0, pos));
                                    line = line.substring(pos).trim();
                                }
                                pos = line.indexOf(' ');
                                if (pos > 0) {
                                    value = new Double((double) Integer.parseInt(line.substring(0, pos)) / 100);
                                    line = line.substring(pos).trim();
                                }
                                Information info = getInfo(projectNum);
                                if (info == null) {
                                    info = createInfo(projectNum);
                                }
                                if (info._qdValue == null) {
                                    info._qdValue = value;
                                }
                            }
                        }
                    }
                } finally {
                    file.close();
                }
            }
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    /**
   * Add static information (hard coded)
   */
    private void addStaticInformation() {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            File file = null;
            if (_local == true) {
                file = new File("fah-projects.xml");
            } else {
                file = new File("../Jmol-web/source/doc/fah/fah-projects.xml");
            }
            Document document = builder.parse(file);
            Node node = document.getFirstChild();
            while (node != null) {
                if (node.getLocalName().equalsIgnoreCase("fah_projects")) {
                    Node child = node.getFirstChild();
                    while (child != null) {
                        if (child.getNodeName().equalsIgnoreCase("date")) {
                        }
                        if (child.getNodeName().equalsIgnoreCase("fah_proj")) {
                            addStaticNode(child);
                        }
                        child = child.getNextSibling();
                    }
                }
                node = node.getNextSibling();
            }
        } catch (SAXException e) {
        } catch (ParserConfigurationException e) {
        } catch (IOException e) {
        }
    }

    /**
   * Add static information about a project
   * 
   * @param node Node for project
   */
    private void addStaticNode(Node node) {
        NamedNodeMap att = node.getAttributes();
        Integer project = XMLValue.getInteger(att, "number");
        if (project == null) {
            return;
        }
        Information info = getInfo(project.intValue());
        if (info == null) {
            info = createInfo(project.intValue());
        }
        if (info != null) {
            info._staticAtoms = XMLValue.getInteger(att, "atoms");
            info._staticContact = XMLValue.getString(att, "contact", null);
            info._staticCore = CoreType.getFromCode(XMLValue.getString(att, "code", null));
            info._staticValue = XMLValue.getDouble(att, "credit");
            info._staticDeadline = XMLValue.getInteger(att, "deadline", 86400);
            info._staticFrames = XMLValue.getInteger(att, "frames");
            info._staticName = XMLValue.getString(att, "name", null);
            info._staticPreferred = XMLValue.getInteger(att, "preferred", 86400);
            info._staticServer = XMLValue.getString(att, "server", null);
            info._staticFile = XMLValue.getYesNo(att, "file");
            info._staticPublic = XMLValue.getYesNo(att, "public");
        }
    }

    /**
   * Information for a project
   */
    private class Information {

        /**
     * Constructor for Information
     */
        Information() {
            this._emDeadline = null;
            this._emFrames = null;
            this._emValue = null;
            this._psAtoms = null;
            this._psCore = null;
            this._psDeadline = null;
            this._psFrames = null;
            this._psName = null;
            this._psPreferred = null;
            this._psServer = null;
            this._psValue = null;
            this._psPublic = null;
            this._qdValue = null;
            this._staticAtoms = null;
            this._staticCore = null;
            this._staticDeadline = null;
            this._staticFrames = null;
            this._staticName = null;
            this._staticPreferred = null;
            this._staticServer = null;
            this._staticValue = null;
            this._staticFile = null;
            this._staticPublic = null;
        }

        Integer _emDeadline;

        Integer _emFrames;

        Double _emValue;

        Integer _psAtoms;

        String _psContact;

        CoreType _psCore;

        Integer _psDeadline;

        Integer _psFrames;

        String _psName;

        Integer _psPreferred;

        String _psServer;

        Double _psValue;

        Boolean _psPublic;

        Double _qdValue;

        Integer _staticAtoms;

        String _staticContact;

        CoreType _staticCore;

        Integer _staticDeadline;

        Integer _staticFrames;

        String _staticName;

        Integer _staticPreferred;

        String _staticServer;

        Double _staticValue;

        Boolean _staticFile;

        Boolean _staticPublic;
    }

    /**
   * HTML Document for PSummaryC
   */
    private class HTMLDocumentPSummaryC extends HTMLDocument {

        public HTMLEditorKit.ParserCallback getReader(int pos) {
            return new PSummaryReaderC();
        }
    }

    /**
   * Reader for PSummaryC
   */
    private class PSummaryReaderC extends HTMLEditorKit.ParserCallback {

        public void handleStartTag(HTML.Tag tag, MutableAttributeSet att, int pos) {
            if (tag.equals(HTML.Tag.TABLE)) {
                this._table++;
                this._column = 0;
            }
            if (tag.equals(HTML.Tag.TR)) {
                this._row++;
                this._column = 0;
            }
            if (tag.equals(HTML.Tag.TD)) {
                this._column++;
            }
            if ((this._table > 0) && (this._row == 1)) {
            }
            super.handleStartTag(tag, att, pos);
        }

        public void handleText(char[] data, int pos) {
            if ((this._table > 0) && (this._row > 1)) {
                switch(this._column) {
                    case 1:
                        this._project = Integer.parseInt(new String(data));
                        break;
                    case 2:
                        this._server = new String(data);
                        break;
                    case 3:
                        this._name = new String(data);
                        break;
                    case 4:
                        this._atoms = null;
                        try {
                            this._atoms = Integer.valueOf(new String(data));
                        } catch (NumberFormatException e) {
                        }
                        break;
                    case 5:
                        this._preferred = null;
                        try {
                            this._preferred = new Integer((int) (86400 * Double.parseDouble(new String(data))));
                        } catch (NumberFormatException e) {
                        }
                        break;
                    case 6:
                        this._deadline = null;
                        try {
                            this._deadline = new Integer((int) (86400 * Double.parseDouble(new String(data))));
                        } catch (NumberFormatException e) {
                        }
                        break;
                    case 7:
                        this._value = null;
                        try {
                            this._value = Double.valueOf(new String(data));
                        } catch (NumberFormatException e) {
                        }
                        break;
                    case 8:
                        this._frames = null;
                        try {
                            this._frames = Integer.valueOf(new String(data));
                        } catch (NumberFormatException e) {
                        }
                        break;
                    case 9:
                        this._core = CoreType.getFromName(new String(data));
                        break;
                    case 10:
                        break;
                    case 11:
                        this._contact = new String(data);
                        break;
                }
            }
            super.handleText(data, pos);
        }

        public void handleEndTag(HTML.Tag tag, int pos) {
            if ((this._table > 0) && (this._row == 1)) {
            }
            if (tag.equals(HTML.Tag.TABLE)) {
                this._table--;
                if (this._table == 0) {
                    this._row = 0;
                }
            }
            if (tag.equals(HTML.Tag.TR)) {
                this._column = 0;
                if (this._project > 0) {
                    Information info = null;
                    if (ProjectInformation.this._projectInfo.size() > this._project) {
                        info = ProjectInformation.this.getInfo(this._project);
                    }
                    if (info == null) {
                        info = ProjectInformation.this.createInfo(this._project);
                    }
                    if (info._psValue == null) {
                        info._psAtoms = this._atoms;
                        info._psContact = this._contact;
                        info._psCore = this._core;
                        info._psDeadline = this._deadline;
                        info._psFrames = this._frames;
                        info._psName = this._name;
                        info._psPreferred = this._preferred;
                        info._psServer = this._server;
                        info._psValue = this._value;
                    }
                }
                this._atoms = null;
                this._contact = null;
                this._core = null;
                this._deadline = null;
                this._frames = null;
                this._name = null;
                this._preferred = null;
                this._project = -1;
                this._server = null;
                this._value = null;
            }
            super.handleEndTag(tag, pos);
        }

        private int _column = 0;

        private int _row = 0;

        private int _table = 0;

        private Integer _atoms = null;

        private String _contact = null;

        private CoreType _core = null;

        private Integer _deadline = null;

        private Integer _frames = null;

        private String _name = null;

        private Integer _preferred = null;

        private int _project = -1;

        private String _server = null;

        private Double _value = null;
    }

    /**
   * HTML Document for PSummary
   */
    private class HTMLDocumentPSummary extends HTMLDocument {

        public HTMLEditorKit.ParserCallback getReader(int pos) {
            return new PSummaryReader();
        }
    }

    /**
   * Reader for PSummary
   */
    private class PSummaryReader extends HTMLEditorKit.ParserCallback {

        public void handleStartTag(HTML.Tag tag, MutableAttributeSet att, int pos) {
            if (tag.equals(HTML.Tag.TABLE)) {
                this._table++;
                this._column = 0;
            }
            if (tag.equals(HTML.Tag.TR)) {
                this._row++;
                this._column = 0;
            }
            if (tag.equals(HTML.Tag.TD)) {
                this._column++;
            }
            if ((this._table > 0) && (this._row == 1)) {
            }
            super.handleStartTag(tag, att, pos);
        }

        public void handleText(char[] data, int pos) {
            if ((this._table > 0) && (this._row > 1)) {
                switch(this._column) {
                    case 1:
                        this._project = Integer.parseInt(new String(data));
                        break;
                }
            }
            super.handleText(data, pos);
        }

        public void handleEndTag(HTML.Tag tag, int pos) {
            if ((this._table > 0) && (this._row == 1)) {
            }
            if (tag.equals(HTML.Tag.TABLE)) {
                this._table--;
                if (this._table == 0) {
                    this._row = 0;
                }
            }
            if (tag.equals(HTML.Tag.TR)) {
                this._column = 0;
                if (this._project > 0) {
                    Information info = null;
                    if (ProjectInformation.this._projectInfo.size() > this._project) {
                        info = ProjectInformation.this.getInfo(this._project);
                    }
                    if (info != null) {
                        info._psPublic = Boolean.TRUE;
                    }
                }
                this._project = -1;
            }
            super.handleEndTag(tag, pos);
        }

        private int _column = 0;

        private int _row = 0;

        private int _table = 0;

        private int _project = -1;
    }

    /**
   * Output data in the same format as emprotz.dat file 
   */
    private void outputEmprotzDatFile() {
        int unknownProjects = 0;
        for (int ii = 0; ii < _projectInfo.size(); ii++) {
            Information info = getInfo(ii);
            if (info != null) {
                if ((info._staticPreferred != null) && (info._staticValue != null) && (info._staticFrames != null)) {
                    System.out.println("\"" + ii + "\"");
                    System.out.println(" " + info._staticPreferred);
                    System.out.println(" " + info._staticValue);
                    System.out.println(" " + info._staticFrames);
                } else {
                    unknownProjects++;
                }
            }
        }
        System.out.println("Unknown: " + unknownProjects);
    }

    private void checkActiveMissing() {
        boolean separator = false;
        for (int ii = 0; ii < _projectInfo.size(); ii++) {
            Information info = getInfo(ii);
            if (info != null) {
                if ((info._psName != null) && (!Boolean.TRUE.equals(info._staticFile))) {
                    if (!separator) {
                        outputText("Active missing beta projects: ");
                    }
                    outputInfo("", "p" + ii, separator);
                    separator = true;
                }
            }
        }
        if (separator) {
            outputNewLine();
        }
        separator = false;
        for (int ii = 0; ii < _projectInfo.size(); ii++) {
            Information info = getInfo(ii);
            if (info != null) {
                if ((info._psName != null) && (!Boolean.TRUE.equals(info._staticFile)) && (Boolean.TRUE.equals(info._staticPublic))) {
                    if (!separator) {
                        outputText("Active missing public projects: ");
                    }
                    outputInfo("", "p" + ii, separator);
                    separator = true;
                }
            }
        }
        if (separator) {
            outputNewLine();
        }
    }

    /**
   * Output a list of problems between fah-projects.xml and existing files
   * 
   * @param projectNumber Project number 
   */
    private void outputCurrentXyzProblems(int projectNumber) {
        Information info = getInfo(projectNumber);
        StringBuffer filePath = new StringBuffer();
        filePath.append("../Jmol-web/source/doc/fah/projects/p");
        filePath.append(projectNumber);
        filePath.append(".xyz.gz");
        File file = new File(filePath.toString());
        if (file.exists()) {
            if ((info == null) || (!Boolean.TRUE.equals(info._staticFile))) {
                System.out.println("Missing file in XML file for project " + projectNumber);
            }
        } else {
            if ((info != null) && (Boolean.TRUE.equals(info._staticFile))) {
                System.out.println("Missing current.xyz file for project " + projectNumber);
            }
        }
    }

    /**
   * @param projectNumber Project number
   * @return Tells if there is missing static information for the project 
   */
    private boolean missingStaticInformation(int projectNumber) {
        boolean different = false;
        Information info = getInfo(projectNumber);
        if (info == null) {
            return false;
        }
        if ((info._psPreferred == null) && (info._staticPreferred == null) && (info._emDeadline != null)) {
            different = true;
        }
        if ((info._psFrames == null) && (info._staticFrames == null) && (info._emFrames != null)) {
            different = true;
        }
        if ((info._psValue == null) && (info._staticValue == null) && (info._emValue != null)) {
            different = true;
        }
        if ((info._psAtoms != null) && (!info._psAtoms.equals(info._staticAtoms))) {
            different = true;
        }
        if ((info._psContact != null) && (!info._psContact.equals("NA")) && (!info._psContact.equals(info._staticContact))) {
            different = true;
        }
        if ((info._psCore != null) && (info._psCore != info._staticCore)) {
            different = true;
        }
        if ((info._psDeadline != null) && (!info._psDeadline.equals(info._staticDeadline))) {
            different = true;
        }
        if ((info._psFrames != null) && (!info._psFrames.equals(info._staticFrames))) {
            different = true;
        }
        if ((info._psName != null) && (!info._psName.equals(info._staticName))) {
            different = true;
        }
        if ((info._psPreferred != null) && (!info._psPreferred.equals(info._staticPreferred))) {
            different = true;
        }
        if ((info._psServer != null) && (!info._psServer.equals(info._staticServer))) {
            different = true;
        }
        if ((info._psValue != null) && (!info._psValue.equals(info._staticValue))) {
            different = true;
        }
        if (Boolean.TRUE.equals(info._psPublic) && !Boolean.TRUE.equals(info._staticPublic)) {
            different = true;
        }
        if ((info._psValue == null) && (info._staticValue == null) && (info._qdValue != null)) {
            different = true;
        }
        return different;
    }

    /**
   * Output text
   * 
   * @param text Text
   */
    private void outputText(String text) {
        System.out.print(text);
    }

    /**
   * Output text
   * 
   * @param text Text
   */
    private void outputTextLn(String text) {
        System.out.println(text);
    }

    /**
   * Output a new line
   */
    private void outputNewLine() {
        System.out.println();
    }

    /**
   * Output information
   * 
   * @param type Type of information
   * @param object Object to output
   * @param separator Indicate if a separator is to be outputed before
   */
    private void outputInfo(String type, Object object, boolean separator) {
        if (object == null) {
            return;
        }
        if (separator) {
            outputText(", ");
        }
        outputText(type);
        outputText(object.toString());
    }

    /**
   * Output static information
   * 
   * @param object Object to output
   * @param separator Indicate if a separator is to be outputed before
   */
    private void outputInfoS(Object object, boolean separator) {
        outputInfo(_txtS, object, separator);
    }

    /**
   * Output psummary information
   * 
   * @param object Object to output
   * @param separator Indicate if a separator is to be outputed before
   */
    private void outputInfoPS(Object object, boolean separator) {
        outputInfo(_txtPS, object, separator);
    }

    /**
   * Output EM information
   * 
   * @param object Object to output
   * @param separator Indicate if a separator is to be outputed before
   */
    private void outputInfoEM(Object object, boolean separator) {
        outputInfo(_txtEM, object, separator);
    }

    /**
   * Output QD information
   * 
   * @param object Object to output
   * @param separator Indicate if a separator is to be outputed before
   */
    private void outputInfoQD(Object object, boolean separator) {
        outputInfo(_txtQD, object, separator);
    }

    /**
   * Outputs missing static informations
   * 
   * @param projectNumber Project number
   */
    private void outputMissingStaticInformation(int projectNumber) {
        Information info = getInfo(projectNumber);
        if (info == null) {
            return;
        }
        outputText("Differences for project ");
        outputTextLn(Integer.toString(projectNumber));
        if ((info._psName != null) && (!info._psName.equals(info._staticName))) {
            outputText("  Name: ");
            boolean separator = false;
            if (info._staticName != null) {
                outputInfoS(info._staticName, separator);
                separator = true;
            }
            if (info._psName != null) {
                outputInfoPS(info._psName, separator);
                separator = true;
            }
            outputNewLine();
        }
        if ((info._psServer != null) && (!info._psServer.equals(info._staticServer))) {
            outputText("  Server: ");
            boolean separator = false;
            if (info._staticServer != null) {
                outputInfoS(info._staticServer, separator);
                separator = true;
            }
            if (info._psServer != null) {
                outputInfoPS(info._psServer, separator);
                separator = true;
            }
            outputNewLine();
        }
        if ((info._psAtoms != null) && (!info._psAtoms.equals(info._staticAtoms))) {
            outputText("  Atoms: ");
            boolean separator = false;
            if (info._staticAtoms != null) {
                outputInfoS(info._staticAtoms, separator);
                separator = true;
            }
            if (info._psAtoms != null) {
                outputInfoPS(info._psAtoms, separator);
                separator = true;
            }
            outputNewLine();
        }
        boolean preferredDifferent = false;
        if (info._psPreferred != null) {
            if (!info._psPreferred.equals(info._staticPreferred)) {
                preferredDifferent = true;
            }
        } else if (info._staticPreferred == null) {
            if (info._emDeadline != null) {
                preferredDifferent = true;
            }
        }
        if (preferredDifferent) {
            outputText("  Preferred: ");
            boolean separator = false;
            if (info._staticPreferred != null) {
                outputInfoS(info._staticPreferred, separator);
                separator = true;
            }
            if (info._emDeadline != null) {
                outputInfoEM(info._emDeadline, separator);
                separator = true;
            }
            if (info._psPreferred != null) {
                outputInfoPS(info._psPreferred, separator);
                outputText(" ");
                outputText(Integer.toString(info._psPreferred.intValue() / 86400));
                separator = true;
            }
            outputNewLine();
        }
        if ((info._psDeadline != null) && (!info._psDeadline.equals(info._staticDeadline))) {
            outputText("  Deadline: ");
            boolean separator = false;
            if (info._staticDeadline != null) {
                outputInfoS(info._staticDeadline, separator);
                separator = true;
            }
            if (info._psDeadline != null) {
                outputInfoPS(info._psDeadline, separator);
                outputText(" ");
                outputText(Integer.toString(info._psDeadline.intValue() / 86400));
                separator = true;
            }
            outputNewLine();
        }
        boolean pointsDifferent = false;
        if (info._psValue != null) {
            if (!info._psValue.equals(info._staticValue)) {
                pointsDifferent = true;
            }
        } else if (info._staticValue == null) {
            if ((info._emValue != null) || (info._qdValue != null)) {
                pointsDifferent = true;
            }
        }
        if (pointsDifferent) {
            outputText("  Points: ");
            boolean separator = false;
            if (info._staticValue != null) {
                outputInfoS(info._staticValue, separator);
                separator = true;
            }
            if (info._emValue != null) {
                outputInfoEM(info._emValue, separator);
                separator = true;
            }
            if (info._psValue != null) {
                outputInfoPS(info._psValue, separator);
                separator = true;
            }
            if (info._qdValue != null) {
                outputInfoQD(info._qdValue, separator);
                separator = true;
            }
            outputNewLine();
        }
        boolean framesDifferent = false;
        if (info._psFrames != null) {
            if (!info._psFrames.equals(info._staticFrames)) {
                framesDifferent = true;
            }
        } else if (info._staticFrames == null) {
            if (info._emFrames != null) {
                framesDifferent = true;
            }
        }
        if (framesDifferent) {
            outputText("  Frames: ");
            boolean separator = false;
            if (info._staticFrames != null) {
                outputInfoS(info._staticFrames, separator);
                separator = true;
            }
            if (info._emFrames != null) {
                outputInfoEM(info._emFrames, separator);
                separator = true;
            }
            if (info._psFrames != null) {
                outputInfoPS(info._psFrames, separator);
                separator = true;
            }
            outputNewLine();
        }
        if ((info._psCore != null) && (info._psCore != info._staticCore)) {
            outputText("  Core: ");
            boolean separator = false;
            if ((info._staticCore != null) && (info._staticCore != CoreType.UNKNOWN)) {
                outputInfoS(info._staticCore.getName(), separator);
                separator = true;
            }
            if (info._psCore != null) {
                outputInfoPS(info._psCore.getName(), separator);
                separator = true;
            }
            outputNewLine();
        }
        if ((info._psContact != null) && (!info._psContact.equals("NA")) && (!info._psContact.equals(info._staticContact))) {
            outputText("  Contact: ");
            boolean separator = false;
            if (info._staticContact != null) {
                outputInfoS(info._staticContact, separator);
                separator = true;
            }
            if (info._psContact != null) {
                outputInfoPS(info._psContact, separator);
                separator = true;
            }
            outputNewLine();
        }
        if (Boolean.TRUE.equals(info._psPublic) && !Boolean.TRUE.equals(info._staticPublic)) {
            outputText("  Public");
            outputNewLine();
        }
    }

    /**
   * Main enabling checking between sources of information
   * 
   * @param args Command line arguments
   */
    public static void main(String[] args) {
        _local = false;
        ProjectInformation projectInfo = getInstance();
        if (_outputEmprotz) {
            projectInfo.outputEmprotzDatFile();
        }
        for (int ii = 0; ii < projectInfo._projectInfo.size(); ii++) {
            if (projectInfo.missingStaticInformation(ii)) {
                projectInfo.outputMissingStaticInformation(ii);
            }
        }
        if (_checkCurrentXyzFiles) {
            for (int ii = 0; ii < projectInfo._projectInfo.size(); ii++) {
                projectInfo.outputCurrentXyzProblems(ii);
            }
        }
        if (_checkActiveMissing) {
            projectInfo.checkActiveMissing();
        }
    }
}
