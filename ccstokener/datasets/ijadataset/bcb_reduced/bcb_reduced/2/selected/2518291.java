package eu.irreality.age;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarFile;
import javax.xml.parsers.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import eu.irreality.age.debug.Debug;
import eu.irreality.age.debug.ExceptionPrinter;
import eu.irreality.age.filemanagement.Paths;
import eu.irreality.age.i18n.UIMessages;
import eu.irreality.age.messages.Messages;
import eu.irreality.age.spell.AGESpellChecker;
import eu.irreality.age.util.VersionComparator;

public class World implements Informador, SupportingCode {

    private boolean debugMode = true;

    /**Nombre del mundo.*/
    private String worldname;

    /**Nombre "bonito" del mundo.*/
    private String modulename;

    /**Directorio del mundo.*/
    private String worlddir;

    /**To locate resources.*/
    private URL worldurl;

    /**M�ximo n�mero de habitaciones del mundo.*/
    private int maxroom;

    /**M�ximo n�mero de objetos del mundo.*/
    private int maxitem;

    /**M�ximo n�mero de bichos del mundo.*/
    private int maxmob;

    /**M�ximo n�mero de entidades abstractas del mundo.*/
    private int maxabsent;

    /**M�ximo n�mero de hechizos del mundo.*/
    private int maxspell;

    /**Habitaciones*/
    private Room[] room;

    /**Items*/
    private Item[] item;

    /**Bichos*/
    private Mobile[] mob;

    /**Efectos*/
    private AbstractEntity[] absent;

    /**Hechizos*/
    private Spell[] spell;

    /**Salida del mundo*/
    private InputOutputClient io;

    private List playerList = new Vector();

    /**Su c�digo*/
    private ObjectCode itsCode;

    /**Autor del mundo.*/
    private String author = null;

    /**Versi�n del mundo.*/
    private String version = null;

    /**Versi�n del parser.*/
    private String parserVersion = null;

    /**Fecha.*/
    private String date = null;

    /**Tipo de mundo. (RPG, aventura...)*/
    private String type = null;

    /**Generador de n�meros aleatorios*/
    private java.util.Random aleat;

    private long semilla;

    /**Lenguaje*/
    private NaturalLanguage lenguaje;

    /**Tabla de nombres, para convertir nombres a n�meros y viceversa*/
    private Map nameTable;

    /**Sincronizaci�n: ha sido ejecutada la serverintro?*/
    public boolean serverIntroExeccedFlag;

    public Object serverIntroSyncObject = new Object();

    /**Tabla de nodos de las entidades creada en la primera fase de las cargas, a la vez que la tabla de nombres.
	(se consultara para las cargas diferidas)*/
    private org.w3c.dom.Element[] itemNode;

    private org.w3c.dom.Element[] mobNode;

    private org.w3c.dom.Element[] roomNode;

    private org.w3c.dom.Element[] absentNode;

    private org.w3c.dom.Element[] spellNode;

    private VisualConfiguration vc;

    private List fileList = new ArrayList();

    private List playerTemplateNodes = new ArrayList();

    private HashMap playerTemplateNodesByName = new HashMap();

    private List playersToAdd = new Vector();

    private BufferedReader logReader;

    boolean from_log;

    String languageCode = NaturalLanguage.DEFAULT_LANGUAGE_CODE;

    private Messages messages;

    public Messages getMessages() {
        return messages;
    }

    /**
	 * Loads a set of messages for this world from an URL.
	 * @param u Location that contains the messages.
	 * @throws IOException If the message file is not found.
	 */
    public void loadMessages(URL u) throws IOException {
        Messages m = new Messages(u);
        m.setWorld(this);
        messages = m;
    }

    public void endOfLog() {
        from_log = false;
        List jugadores = getPlayerList();
        for (int i = 0; i < jugadores.size(); i++) {
            Player jugador = (Player) jugadores.get(i);
            jugador.endOfLog();
        }
    }

    /**
	 * Opens a log file either in the path directly specified or, if not found, in the saves directory.
	 * @param s
	 * @return
	 * @throws java.io.FileNotFoundException
	 */
    public FileInputStream openLogFile(String s) throws java.io.FileNotFoundException {
        FileInputStream logInput = null;
        try {
            logInput = new FileInputStream(s);
        } catch (FileNotFoundException exc) {
            try {
                logInput = new FileInputStream(new File(Paths.SAVE_PATH, s));
            } catch (FileNotFoundException exc2) {
                throw (exc);
            }
        }
        return logInput;
    }

    public void prepareLog(String s) throws java.io.FileNotFoundException {
        from_log = true;
        FileInputStream logInput = openLogFile(s);
        prepareLog(logInput);
    }

    /**
	 * Prepare (multiplayer, in theory) log from input stream.
	 * @param is
	 */
    public void prepareLog(InputStream logInput) {
        from_log = true;
        logReader = new BufferedReader(Utility.getBestInputStreamReader(logInput));
        try {
            logReader.readLine();
            logReader.readLine();
        } catch (java.io.IOException exc) {
            write(io.getColorCode("error") + "Excepci�n I/O al leer el log" + io.getColorCode("reset"));
        }
        List jugadores = getPlayerList();
        for (int i = 0; i < jugadores.size(); i++) {
            Player jugador = (Player) jugadores.get(i);
            jugador.prepareLog(logReader);
        }
    }

    private static Document dummyDoc = null;

    public static Node getDetachedCopy(Node n) {
        try {
            if (dummyDoc == null) dummyDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
            return dummyDoc.importNode(n, true);
        } catch (Exception exc) {
            exc.printStackTrace();
            return n;
        }
    }

    /**
	* Toma la informaci�n del mundo del documento XML.
	*
	*/
    public void loadWorldFromXML(org.w3c.dom.Node n, final InputOutputClient io, boolean noSerCliente) throws XMLtoWorldException {
        this.io = io;
        boolean jugadorAsignadoACliente = false;
        write(io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.info") + "\n" + io.getColorCode("reset"));
        if (!(n instanceof org.w3c.dom.Element)) {
            throw (new XMLtoWorldException("World node not Element"));
        }
        org.w3c.dom.Element e = (org.w3c.dom.Element) n;
        worldname = UIMessages.getInstance().getMessage("load.world.default.name");
        version = UIMessages.getInstance().getMessage("load.world.default.version");
        parserVersion = UIMessages.getInstance().getMessage("load.world.default.required");
        modulename = UIMessages.getInstance().getMessage("load.world.default.modulename");
        maxroom = 0;
        maxitem = 0;
        maxmob = 0;
        author = UIMessages.getInstance().getMessage("load.world.default.author");
        date = UIMessages.getInstance().getMessage("load.world.default.date");
        type = UIMessages.getInstance().getMessage("load.world.default.type");
        if (e.hasAttribute("worldDir") && !e.getAttribute("worldDir").equals(".")) {
            worlddir = e.getAttribute("worldDir");
            try {
                worldurl = new URL(worlddir);
            } catch (MalformedURLException e1) {
                try {
                    worldurl = new File(worlddir).toURI().toURL();
                } catch (MalformedURLException e2) {
                    System.err.println("worldDir attribute seems neither pathname nor URL:");
                    e1.printStackTrace();
                    e2.printStackTrace();
                }
            }
        }
        if (!e.hasAttribute("worldName")) throw (new XMLtoWorldException("World node lacks attribute worldName"));
        if (!e.hasAttribute("moduleName")) throw (new XMLtoWorldException("Item node lacks attribute moduleName"));
        if (!e.hasAttribute("maxroom")) {
            maxroom = e.getElementsByTagName("Room").getLength();
        }
        if (!e.hasAttribute("maxitem")) {
            maxitem = e.getElementsByTagName("Item").getLength();
        }
        if (!e.hasAttribute("maxmob")) {
            maxmob = e.getElementsByTagName("Mobile").getLength();
            Debug.println("Max Mob set to " + maxmob);
        }
        if (!e.hasAttribute("maxabsent")) {
            maxabsent = e.getElementsByTagName("AbstractEntity").getLength();
            Debug.println("Max Abstract set to " + maxmob);
        }
        if (!e.hasAttribute("maxspell")) {
            maxspell = e.getElementsByTagName("Spell").getLength();
            Debug.println("Max Spell set to " + maxmob);
        }
        worldname = e.getAttribute("worldName");
        modulename = e.getAttribute("moduleName");
        try {
            if (e.hasAttribute("maxroom")) {
                maxroom = Integer.valueOf(e.getAttribute("maxroom")).intValue();
            }
        } catch (NumberFormatException nfe) {
            ;
        }
        try {
            if (e.hasAttribute("maxitem")) {
                maxitem = Integer.valueOf(e.getAttribute("maxitem")).intValue();
            }
        } catch (NumberFormatException nfe) {
            ;
        }
        try {
            if (e.hasAttribute("maxmob")) {
                maxmob = Integer.valueOf(e.getAttribute("maxmob")).intValue();
            }
        } catch (NumberFormatException nfe) {
            ;
        }
        try {
            if (e.hasAttribute("maxabsent")) {
                maxabsent = Integer.valueOf(e.getAttribute("maxabsent")).intValue();
            }
        } catch (NumberFormatException nfe) {
            ;
        }
        try {
            if (e.hasAttribute("maxspell")) {
                maxmob = Integer.valueOf(e.getAttribute("maxspell")).intValue();
            }
        } catch (NumberFormatException nfe) {
            ;
        }
        if (e.hasAttribute("author")) author = e.getAttribute("author");
        if (e.hasAttribute("version")) version = e.getAttribute("version");
        if (e.hasAttribute("parserVersion")) parserVersion = e.getAttribute("parserVersion");
        if (e.hasAttribute("date")) date = e.getAttribute("date");
        if (e.hasAttribute("type")) type = e.getAttribute("type");
        org.w3c.dom.NodeList confNodes = e.getElementsByTagName("VisualConfiguration");
        if (confNodes.getLength() > 0) {
            org.w3c.dom.Element confNode = (org.w3c.dom.Element) confNodes.item(0);
            vc = new VisualConfiguration(confNode, worldurl.toString());
            if (io instanceof ColoredSwingClient) {
                Debug.println("VISUAL CONFIGURATION SET TO " + vc);
                ((ColoredSwingClient) io).setVisualConfiguration(vc);
            }
        }
        org.w3c.dom.NodeList fileListNodes = e.getElementsByTagName("FileList");
        if (fileListNodes.getLength() > 0) {
            org.w3c.dom.Element fileListNode = (org.w3c.dom.Element) fileListNodes.item(0);
            org.w3c.dom.NodeList fileNodes = fileListNode.getElementsByTagName("File");
            fileList = new ArrayList();
            for (int i = 0; i < fileNodes.getLength(); i++) {
                if (((org.w3c.dom.Element) fileNodes.item(i)).hasAttribute("path")) {
                    fileList.add(getWorldDir() + ((org.w3c.dom.Element) fileNodes.item(i)).getAttribute("path"));
                }
            }
        }
        List playerIDs = new ArrayList();
        org.w3c.dom.NodeList plNodes = e.getElementsByTagName("PlayerList");
        if (plNodes.getLength() > 0) {
            org.w3c.dom.Element plNode = (org.w3c.dom.Element) plNodes.item(0);
            org.w3c.dom.NodeList plidnodes = plNode.getElementsByTagName("Player");
            for (int i = 0; i < plidnodes.getLength(); i++) {
                org.w3c.dom.Element playerIDNode = (org.w3c.dom.Element) plidnodes.item(i);
                if (playerIDNode.hasAttribute("id")) {
                    playerIDs.add(playerIDNode.getAttribute("id"));
                }
            }
        }
        org.w3c.dom.NodeList codeNodes = e.getElementsByTagName("Code");
        if (codeNodes.getLength() > 0) {
            for (int i = 0; i < codeNodes.getLength(); i++) {
                Element codeNode = (Element) codeNodes.item(i);
                if (codeNode.getParentNode() instanceof Element && ((Element) codeNode.getParentNode()).getTagName().equals("World")) {
                    try {
                        itsCode = new ObjectCode(this, codeNode);
                    } catch (XMLtoWorldException ex) {
                        throw (new XMLtoWorldException("Exception at Code node: " + ex.getMessage()));
                    }
                    break;
                }
            }
        }
        write(io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.language") + "\n" + io.getColorCode("reset"));
        if (e.hasAttribute("language")) languageCode = e.getAttribute("language");
        messages = Messages.getDefaultInstance(languageCode);
        lenguaje = NaturalLanguage.getInstance(languageCode);
        Thread.currentThread().yield();
        org.w3c.dom.NodeList playerGenerationNodes = e.getElementsByTagName("PlayerGeneration");
        if (playerGenerationNodes.getLength() > 0) {
            org.w3c.dom.Element playerGenerationNode = (org.w3c.dom.Element) playerGenerationNodes.item(0);
            org.w3c.dom.NodeList templateNodes = playerGenerationNode.getElementsByTagName("Template");
            for (int i = 0; i < templateNodes.getLength(); i++) {
                org.w3c.dom.Element templateElement = (org.w3c.dom.Element) templateNodes.item(i);
                templateElement = (Element) getDetachedCopy(templateElement);
                playerTemplateNodes.add(templateElement);
                if (templateElement.hasAttribute("name")) playerTemplateNodesByName.put(templateElement.getAttribute("name"), templateElement);
            }
        }
        NodeList roomLists = e.getElementsByTagName("Rooms");
        NodeList itemLists = e.getElementsByTagName("Items");
        NodeList mobileLists = e.getElementsByTagName("Mobiles");
        NodeList absentLists = e.getElementsByTagName("AbstractEntities");
        NodeList spellLists = e.getElementsByTagName("Spells");
        Element roomListElement, itemListElement, mobileListElement, absentListElement, spellListElement;
        if (roomLists.getLength() > 0) roomListElement = (Element) roomLists.item(0); else roomListElement = null;
        if (itemLists.getLength() > 0) itemListElement = (Element) itemLists.item(0); else itemListElement = null;
        if (mobileLists.getLength() > 0) mobileListElement = (Element) mobileLists.item(0); else mobileListElement = null;
        if (absentLists.getLength() > 0) absentListElement = (Element) absentLists.item(0); else absentListElement = null;
        if (spellLists.getLength() > 0) spellListElement = (Element) spellLists.item(0); else spellListElement = null;
        NodeList roomNodes = roomListElement != null ? roomListElement.getElementsByTagName("Room") : null;
        NodeList itemNodes = itemListElement != null ? itemListElement.getElementsByTagName("Item") : null;
        NodeList mobileNodes = mobileListElement != null ? mobileListElement.getElementsByTagName("Mobile") : null;
        NodeList absentNodes = absentListElement != null ? absentListElement.getElementsByTagName("AbstractEntity") : null;
        NodeList spellNodes = spellListElement != null ? spellListElement.getElementsByTagName("Spell") : null;
        if (roomNodes != null && roomNodes.getLength() != maxroom) write("Warning: " + roomNodes.getLength() + " room nodes while maxroom is " + maxroom);
        if (itemNodes != null && itemNodes.getLength() != maxitem) write("Warning: " + itemNodes.getLength() + " item nodes while maxitem is " + maxitem);
        if (mobileNodes != null && mobileNodes.getLength() != maxmob) write("Warning: " + mobileNodes.getLength() + " mobile nodes while maxmob is " + maxmob);
        if (absentNodes != null && absentNodes.getLength() != maxabsent) write("Warning: " + absentNodes.getLength() + " abstract entity nodes while maxabsent is " + maxabsent);
        if (spellNodes != null && spellNodes.getLength() != maxspell) write("Warning: " + spellNodes.getLength() + "  spell nodes while maxspell is " + maxspell);
        if (roomNodes != null) {
            roomNode = new Element[roomNodes.getLength()];
            for (int i = 0; i < roomNodes.getLength(); i++) roomNode[i] = (Element) roomNodes.item(i);
        } else {
            roomNode = new Element[0];
        }
        if (itemNodes != null) {
            itemNode = new Element[itemNodes.getLength()];
            for (int i = 0; i < itemNodes.getLength(); i++) itemNode[i] = (Element) itemNodes.item(i);
        } else {
            itemNode = new Element[0];
        }
        if (mobileNodes != null) {
            mobNode = new Element[mobileNodes.getLength()];
            for (int i = 0; i < mobileNodes.getLength(); i++) mobNode[i] = (Element) mobileNodes.item(i);
        } else {
            mobNode = new Element[0];
        }
        if (absentNodes != null) {
            absentNode = new Element[absentNodes.getLength()];
            for (int i = 0; i < absentNodes.getLength(); i++) absentNode[i] = (Element) absentNodes.item(i);
        } else {
            absentNode = new Element[0];
        }
        if (spellNodes != null) {
            spellNode = new Element[spellNodes.getLength()];
            for (int i = 0; i < spellNodes.getLength(); i++) spellNode[i] = (Element) spellNodes.item(i);
        } else {
            spellNode = new Element[0];
        }
        write(io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.nametable") + "\n" + io.getColorCode("reset"));
        int nameTableSize = maxroom + maxitem + maxmob + maxabsent + maxspell;
        nameTable = new Hashtable(nameTableSize > 100 ? nameTableSize : 100);
        int nentries = 0;
        for (int i = 0; i < roomNode.length; i++) {
            Element elt = roomNode[i];
            if (!elt.hasAttribute("name")) throw (new XMLtoWorldException("Room " + i + " without id or name attr"));
            try {
                if (elt.hasAttribute("id")) nameTable.put(elt.getAttribute("name"), Integer.valueOf(elt.getAttribute("id"))); else nameTable.put(elt.getAttribute("name"), new Integer(i + Utility.room_summand));
            } catch (NumberFormatException nfe) {
                throw (new XMLtoWorldException("id attribute not number"));
            }
            nentries++;
        }
        for (int i = 0; i < itemNode.length; i++) {
            Element elt = itemNode[i];
            if (!elt.hasAttribute("name")) throw (new XMLtoWorldException("Item " + i + " without name attr"));
            try {
                if (elt.hasAttribute("id")) nameTable.put(elt.getAttribute("name"), Integer.valueOf(elt.getAttribute("id"))); else nameTable.put(elt.getAttribute("name"), new Integer(i + Utility.item_summand));
            } catch (NumberFormatException nfe) {
                throw (new XMLtoWorldException("id attribute not number"));
            }
            nentries++;
        }
        for (int i = 0; i < mobNode.length; i++) {
            Element elt = mobNode[i];
            if (!elt.hasAttribute("name")) throw (new XMLtoWorldException("Mobile " + i + " without name attr"));
            try {
                if (elt.hasAttribute("id")) nameTable.put(elt.getAttribute("name"), Integer.valueOf(elt.getAttribute("id"))); else nameTable.put(elt.getAttribute("name"), new Integer(i + Utility.mobile_summand));
            } catch (NumberFormatException nfe) {
                throw (new XMLtoWorldException("id attribute not number"));
            }
            nentries++;
        }
        for (int i = 0; i < absentNode.length; i++) {
            Element elt = absentNode[i];
            if (!elt.hasAttribute("name")) throw (new XMLtoWorldException("AbstractEntity " + i + " without name attr"));
            try {
                if (elt.hasAttribute("id")) nameTable.put(elt.getAttribute("name"), Integer.valueOf(elt.getAttribute("id"))); else nameTable.put(elt.getAttribute("name"), new Integer(i + Utility.absent_summand));
            } catch (NumberFormatException nfe) {
                throw (new XMLtoWorldException("id attribute not number"));
            }
            nentries++;
        }
        for (int i = 0; i < spellNode.length; i++) {
            Element elt = spellNode[i];
            if (!elt.hasAttribute("name")) throw (new XMLtoWorldException("Spell " + i + " without name attr"));
            try {
                if (elt.hasAttribute("id")) nameTable.put(elt.getAttribute("name"), Integer.valueOf(elt.getAttribute("id"))); else nameTable.put(elt.getAttribute("name"), new Integer(i + Utility.spell_summand));
            } catch (NumberFormatException nfe) {
                throw (new XMLtoWorldException("id attribute not number"));
            }
            nentries++;
        }
        write(io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.abstract") + "\n" + io.getColorCode("reset"));
        absent = new AbstractEntity[absentNode.length];
        for (int i = 0; i < absentNode.length; i++) {
            absent[i] = AbstractEntity.getInstance(this, absentNode[i]);
            if (absent[i].getID() % 10000000 == 0) {
                absent[i].setID(i + Utility.absent_summand);
            }
        }
        write(io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.spell") + "\n" + io.getColorCode("reset"));
        spell = new Spell[spellNode.length];
        for (int i = 0; i < spellNode.length; i++) {
            spell[i] = Spell.getInstance(this, spellNode[i]);
            if (spell[i].getID() % 10000000 == 0) {
                spell[i].setID(i + Utility.spell_summand);
            }
        }
        write(io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.item") + "\n" + io.getColorCode("reset"));
        item = new Item[itemNode.length];
        for (int i = 0; i < itemNode.length; i++) {
            item[i] = Item.getInstance(this, itemNode[i]);
            if (item[i].getID() % 10000000 == 0) {
                item[i].setID(i + Utility.item_summand);
                Debug.println("&ID SET" + item[i].getID());
            }
        }
        write(io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.mob") + "\n" + io.getColorCode("reset"));
        mob = new Mobile[mobNode.length];
        for (int i = 0; i < mobNode.length; i++) {
            if ((mobNode[i].hasAttribute("id") || mobNode[i].hasAttribute("name")) && (playerIDs.contains(mobNode[i].getAttribute("id")) || playerIDs.contains(mobNode[i].getAttribute("name")))) {
                {
                    Element templateElement = (Element) getDetachedCopy(mobNode[i]);
                    playerTemplateNodes.add(templateElement);
                    if (templateElement.hasAttribute("name")) playerTemplateNodesByName.put(templateElement.getAttribute("name"), templateElement);
                }
                if (!noSerCliente && !jugadorAsignadoACliente) {
                    ReturnValue retval = new ReturnValue(null);
                    boolean endfound = runAssignPlayerCode(retval, io);
                    if (retval.getRetVal() != null && !endfound) {
                        jugadorAsignadoACliente = true;
                        mob[i] = ((Player) retval.getRetVal());
                        playersToAdd.add(mob[i]);
                        mob[i].setNewState(Mobile.DISABLED, 1);
                    } else {
                        mob[i] = new Player(this, io, mobNode[i]);
                        addPlayer((Player) mob[i]);
                        jugadorAsignadoACliente = true;
                    }
                } else {
                    mob[i] = new Player(this, io, mobNode[i]);
                    ((Player) mob[i]).setPlayerName("Player Template");
                }
            } else {
                mob[i] = Mobile.getInstance(this, mobNode[i]);
            }
            if (mob[i].getID() % 10000000 == 0) {
                mob[i].setID(i + Utility.mobile_summand);
            }
        }
        write(io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.room") + "\n" + io.getColorCode("reset"));
        room = new Room[roomNode.length];
        for (int i = 0; i < roomNode.length; i++) {
            room[i] = new Room(this, roomNode[i]);
            if (room[i].getID() % 10000000 == 0) {
                room[i].setID(i + Utility.room_summand);
            }
        }
        write(io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.stats", "$number", String.valueOf(maxroom + maxitem + maxmob + maxspell + maxabsent)) + "\n" + io.getColorCode("reset"));
        write(io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.deferred") + "\n" + io.getColorCode("reset") + "\n");
        for (int i = 0; i < maxitem; i++) {
            item[i].loadInventoryFromXML(this);
            item[i].readRelationshipListFromXML(this, itemNode[i]);
        }
        for (int i = 0; i < maxmob; i++) {
            mob[i].readRelationshipListFromXML(this, mobNode[i]);
        }
        for (int i = 0; i < maxroom; i++) {
            room[i].readRelationshipListFromXML(this, roomNode[i]);
        }
        for (int i = 0; i < maxabsent; i++) {
            absent[i].readRelationshipListFromXML(this, absentNode[i]);
        }
        for (int i = 0; i < maxspell; i++) {
            spell[i].readRelationshipListFromXML(this, spellNode[i]);
        }
        write(io.getColorCode("information") + "\n" + UIMessages.getInstance().getMessage("load.world.done") + io.getColorCode("reset") + "\n");
        write("\n=== === === === === === === === === === === === === === === ===");
        write("\n" + io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.info.intro"));
        if (modulename != null) write("\n" + io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.info.name") + modulename + io.getColorCode("reset"));
        if (type != null) write("\n" + io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.info.type") + type + io.getColorCode("reset"));
        if (author != null) write("\n" + io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.info.author") + author + io.getColorCode("reset"));
        if (version != null) write("\n" + io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.info.version") + version + io.getColorCode("reset"));
        if (date != null) write("\n" + io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.info.date") + date + io.getColorCode("reset"));
        if (parserVersion != null) write("\n" + io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.info.required") + parserVersion + io.getColorCode("reset"));
        write("\n=== === === === === === === === === === === === === === === ===\n\n");
        if (new VersionComparator().compare(parserVersion, "1.0") < 0) this.setCommandMatchingMode(Entity.LEGACY_COMMAND_MATCHING);
        if (new VersionComparator().compare(parserVersion, "1.0") >= 0 && new VersionComparator().compare(parserVersion, "1.1.1") < 0) this.setCommandMatchingMode(Entity.LENIENT_COMMAND_MATCHING);
        if (!jugadorAsignadoACliente && !noSerCliente) {
            Thread th = new Thread() {

                public void run() {
                    try {
                        Debug.println("Add player wait.");
                        synchronized (serverIntroSyncObject) {
                            while (!serverIntroExeccedFlag) {
                                try {
                                    serverIntroSyncObject.wait();
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        Debug.println("Adding player.");
                        addNewPlayerASAP(io);
                    } catch (XMLtoWorldException xml2we) {
                        io.write("XML to World Exception when assigning player.\n");
                        xml2we.printStackTrace();
                    }
                }
            };
            th.start();
        }
        itemNode = null;
        mobNode = null;
        roomNode = null;
        absentNode = null;
        spellNode = null;
    }

    /**
	 * @deprecated This used DAT files. Use XML files instead.
	 * @param modulefile
	 * @param io
	 * @param noSerCliente
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
    void legacyWorldLoad(String modulefile, InputOutputClient io, boolean noSerCliente) throws FileNotFoundException, IOException {
        this.io = io;
        FileInputStream fp = new FileInputStream(modulefile);
        java.io.BufferedReader filein = new java.io.BufferedReader(Utility.getBestInputStreamReader(fp));
        String linea = "";
        String token = "";
        String zipFile = "";
        write(io.getColorCode("information") + "\nCARGA DEL MUNDO EN LA M�QUINA DE ESTADOS DE AETHERIA\n" + io.getColorCode("reset"));
        write(io.getColorCode("information") + "\nObteniendo informaci�n de mundo...\n" + io.getColorCode("reset"));
        Thread.currentThread().yield();
        while (true) {
            linea = filein.readLine();
            if (linea == null) break;
            token = StringMethods.getTok(linea, 1, ' ');
            if (token.equalsIgnoreCase("module")) worldname = StringMethods.getTok(linea, 2, ' '); else if (token.equalsIgnoreCase("maxroom")) maxroom = (Integer.valueOf(StringMethods.getTok(linea, 2, ' ')).intValue()); else if (token.equalsIgnoreCase("maxitem")) maxitem = (Integer.valueOf(StringMethods.getTok(linea, 2, ' ')).intValue()); else if (token.equalsIgnoreCase("maxmob")) maxmob = (Integer.valueOf(StringMethods.getTok(linea, 2, ' ')).intValue()); else if (token.equalsIgnoreCase("printthis")) write(StringMethods.getToks(linea, 2, StringMethods.numToks(linea, ' '), ' ') + "\n"); else if (token.equalsIgnoreCase("modulename")) modulename = StringMethods.getToks(linea, 2, StringMethods.numToks(linea, ' '), ' '); else if (token.equalsIgnoreCase("author")) author = StringMethods.getToks(linea, 2, StringMethods.numToks(linea, ' '), ' '); else if (token.equalsIgnoreCase("version")) version = StringMethods.getToks(linea, 2, StringMethods.numToks(linea, ' '), ' '); else if (token.equalsIgnoreCase("date")) date = StringMethods.getToks(linea, 2, StringMethods.numToks(linea, ' '), ' '); else if (token.equalsIgnoreCase("parserversion")) parserVersion = StringMethods.getToks(linea, 2, StringMethods.numToks(linea, ' '), ' '); else if (token.equalsIgnoreCase("zipfile")) zipFile = StringMethods.getToks(linea, 2, StringMethods.numToks(linea, ' '), ' '); else if (token.equalsIgnoreCase("begin_eva_code")) {
                {
                    String EVACodeString = "";
                    boolean terminamos = false;
                    while (!terminamos) {
                        linea = filein.readLine();
                        String id_linea = StringMethods.getTok(linea, 1, ' ');
                        if (id_linea.equalsIgnoreCase("end_eva_code")) terminamos = true; else {
                            EVACodeString += "\n";
                            EVACodeString += linea;
                        }
                    }
                    itsCode = new ObjectCode(EVACodeString, "EVA", this);
                }
            } else if (token.equalsIgnoreCase("begin_bsh_code")) {
                {
                    String BSHCodeString = "";
                    boolean terminamos = false;
                    while (!terminamos) {
                        linea = filein.readLine();
                        String id_linea = StringMethods.getTok(linea, 1, ' ');
                        if (id_linea.equalsIgnoreCase("end_bsh_code")) terminamos = true; else {
                            BSHCodeString += "\n";
                            BSHCodeString += linea;
                        }
                    }
                    itsCode = new ObjectCode(BSHCodeString, "BeanShell", this);
                }
            }
        }
        write(io.getColorCode("information") + "\nCargando datos ling��sticos..." + io.getColorCode("reset"));
        lenguaje = NaturalLanguage.getInstance();
        messages = Messages.getDefaultInstance(languageCode);
        Thread.currentThread().yield();
        write(io.getColorCode("information") + "\nCreando tabla de nombres...\n" + io.getColorCode("reset"));
        int nameTableSize = maxroom + maxitem + maxmob;
        nameTable = new Hashtable(nameTableSize > 100 ? nameTableSize : 100);
        int nentries = 0;
        try {
            for (int i = 0; i < maxitem; i++) {
                BufferedReader br = new BufferedReader(Utility.getBestInputStreamReader(new FileInputStream(Utility.itemFile(this, i))));
                linea = "";
                int idnumber = i;
                for (int line = 1; line < 100; line++) {
                    linea = br.readLine();
                    String id_linea = StringMethods.getTok(linea, 1, ' ');
                    linea = StringMethods.getToks(linea, 2, StringMethods.numToks(linea, ' '), ' ');
                    if (id_linea != null) {
                        int nlinea = 0;
                        try {
                            nlinea = Integer.valueOf(id_linea).intValue();
                        } catch (NumberFormatException nfe) {
                            ;
                        }
                        if (nlinea == 1) idnumber = Integer.valueOf(linea).intValue();
                        if (nlinea == 4) {
                            nameTable.put(linea, new Integer(Utility.completeItemID(idnumber)));
                            nentries++;
                        }
                    }
                }
            }
        } catch (FileNotFoundException fnfe) {
            ;
        }
        try {
            for (int i = 0; i < maxroom; i++) {
                BufferedReader br = new BufferedReader(Utility.getBestInputStreamReader(new FileInputStream(Utility.roomFile(this, i))));
                linea = "";
                int idnumber = i;
                for (int line = 1; line < 100; line++) {
                    linea = br.readLine();
                    String id_linea = StringMethods.getTok(linea, 1, ' ');
                    linea = StringMethods.getToks(linea, 2, StringMethods.numToks(linea, ' '), ' ');
                    if (id_linea != null) {
                        int nlinea = 0;
                        try {
                            nlinea = Integer.valueOf(id_linea).intValue();
                        } catch (NumberFormatException nfe) {
                            ;
                        }
                        if (nlinea == 1) idnumber = Integer.valueOf(linea).intValue();
                        if (nlinea == 4) {
                            nameTable.put(linea, new Integer(Utility.completeRoomID(idnumber)));
                            nentries++;
                        }
                    }
                }
            }
        } catch (FileNotFoundException fnfe) {
            ;
        }
        try {
            for (int i = 0; i < maxmob; i++) {
                BufferedReader br = new BufferedReader(Utility.getBestInputStreamReader(new FileInputStream(Utility.mobFile(this, i))));
                linea = "";
                int idnumber = i;
                for (int line = 1; line < 100; line++) {
                    linea = br.readLine();
                    String id_linea = StringMethods.getTok(linea, 1, ' ');
                    linea = StringMethods.getToks(linea, 2, StringMethods.numToks(linea, ' '), ' ');
                    if (id_linea != null) {
                        int nlinea = 0;
                        try {
                            nlinea = Integer.valueOf(id_linea).intValue();
                        } catch (NumberFormatException nfe) {
                            ;
                        }
                        if (nlinea == 1) idnumber = Integer.valueOf(linea).intValue();
                        if (nlinea == 4) {
                            nameTable.put(linea, new Integer(Utility.completeMobileID(idnumber)));
                            nentries++;
                        }
                    }
                }
            }
        } catch (FileNotFoundException fnfe) {
            write(io.getColorCode("information") + "Entradas: " + nentries + "\n" + io.getColorCode("reset"));
        }
        write(io.getColorCode("information") + "Inicializando items...\n" + io.getColorCode("reset"));
        item = new Item[maxitem];
        int i = 0;
        try {
            for (; i < maxitem; i++) {
                item[i] = Item.getInstance(this, Utility.itemFile(this, i));
            }
        } catch (FileNotFoundException nomasitems) {
            maxitem = i;
        } catch (IOException npi) {
            System.err.println("No puedo abrir el objeto " + i);
        }
        write(io.getColorCode("information") + "Inicializando bichos...\n" + io.getColorCode("reset"));
        mob = new Mobile[maxmob];
        i = 1;
        try {
            for (; i < maxmob; i++) {
                mob[i] = Mobile.getInstance(this, Utility.mobFile(this, i));
            }
        } catch (FileNotFoundException nomasmobs) {
            maxmob = i;
        } catch (IOException npi) {
            System.err.println("No puedo abrir el bicho " + i);
        }
        write(io.getColorCode("information") + "Inicializando habitaciones...\n" + io.getColorCode("reset"));
        room = new Room[maxroom];
        i = 0;
        try {
            for (; i < maxroom; i++) {
                room[i] = new Room(this, Utility.roomFile(this, i));
            }
        } catch (FileNotFoundException nomashabitaciones) {
            maxroom = i;
        } catch (IOException npi) {
            System.err.println("No puedo abrir la habitaci�n " + i);
        }
        write(io.getColorCode("information") + (maxroom + maxitem + maxmob) + " entidades cargadas.\n");
        write(io.getColorCode("information") + "Completando cargas diferidas..." + io.getColorCode("reset"));
        for (i = 1; i < maxitem; i++) {
            item[i].loadInventory(this);
        }
        write(io.getColorCode("information") + "\nMundo inicializado." + io.getColorCode("reset") + "\n");
        write("\n=== === === === === === === === === === === === === === === ===");
        write("\n" + io.getColorCode("information") + "Informaci�n de Juego:");
        if (modulename != null) write("\n" + io.getColorCode("information") + "[Nombre]           " + modulename + io.getColorCode("reset"));
        if (type != null) write("\n" + io.getColorCode("information") + "[Tipo]             " + type + io.getColorCode("reset"));
        if (author != null) write("\n" + io.getColorCode("information") + "[Autor]            " + author + io.getColorCode("reset"));
        if (version != null) write("\n" + io.getColorCode("information") + "[Versi�n]          " + version + io.getColorCode("reset"));
        if (date != null) write("\n" + io.getColorCode("information") + "[Fecha]            " + date + io.getColorCode("reset"));
        if (parserVersion != null) write("\n" + io.getColorCode("information") + "[Versi�n engine]   " + parserVersion + io.getColorCode("reset"));
        write("\n=== === === === === === === === === === === === === === === ===\n");
    }

    /**
	 * Toma la informaci�n del mundo del stream dado. 
	 * @param is Stream de donde se leen los datos XML del mundo, que puede ser un stream obtenido de un fichero local de mundo, de una URL, etc.
	 * @param io
	 * @param noSerCliente
	 * @throws ParserConfigurationException 
	 * @throws IOException 
	 * @throws SAXException 
	 * @throws XMLtoWorldException 
	 */
    public void loadWorldFromStream(InputStream is, InputOutputClient io, boolean noSerCliente) throws ParserConfigurationException, SAXException, IOException, XMLtoWorldException {
        org.w3c.dom.Document d = null;
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        io.write(io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.tree") + "\n" + io.getColorCode("reset"));
        d = db.parse(is);
        org.w3c.dom.Element n = d.getDocumentElement();
        loadWorldFromXML(n, io, noSerCliente);
    }

    public World(URL url, InputOutputClient io, boolean noSerCliente) throws IOException {
        InputStream is = null;
        if (url.toString().toLowerCase().endsWith(".xml")) {
            if (!url.toString().startsWith("jar:") && !url.toString().startsWith("zip:")) {
                worlddir = url.toString().substring(0, url.toString().lastIndexOf("/") + 1);
                worldurl = new URL(worlddir);
            } else {
                worlddir = url.toString().substring(0, url.toString().lastIndexOf("/") + 1);
                worldurl = new URL(worlddir);
            }
            is = url.openStream();
        } else {
            worlddir = "jar:" + url.toString() + "!/";
            worldurl = new URL(worlddir);
            URLClassLoader ucl = new URLClassLoader(new URL[] { url }, this.getClass().getClassLoader());
            is = ucl.getResourceAsStream("world.xml");
            if (is == null) throw new IOException("Resource world.xml could not be found in URL " + url);
            this.setResourceJarFile(url);
        }
        try {
            loadWorldFromStream(is, io, noSerCliente);
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
            throw new IOException(pce);
        } catch (SAXException se) {
            se.printStackTrace();
            throw new IOException(se);
        } catch (IOException ioe) {
            throw (ioe);
        } catch (XMLtoWorldException x2we) {
            write(UIMessages.getInstance().getMessage("load.world.xml.exception") + " " + x2we.getMessage());
        }
    }

    /**
	 * Toma la informaci�n sobre el mundo del fichero de m�dulo.
	 *
	 * @param modulefile El fichero del que toma la informaci�n para crear el mundo.
	 */
    public World(String modulefile, InputOutputClient io, boolean noSerCliente) throws FileNotFoundException, IOException {
        worlddir = new File(new File(modulefile).getParent()).getPath() + File.separatorChar;
        worldurl = new File(modulefile).getParentFile().toURI().toURL();
        io.write(io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.xml") + "\n" + io.getColorCode("reset"));
        try {
            InputStream is = new FileInputStream(new File(modulefile));
            loadWorldFromStream(is, io, noSerCliente);
        } catch (FileNotFoundException fnfe) {
            throw (fnfe);
        } catch (ParserConfigurationException pce) {
            pce.printStackTrace();
            throw new IOException(pce);
        } catch (SAXException se) {
            se.printStackTrace();
            throw new IOException(se);
        } catch (IOException ioe) {
            throw (ioe);
        } catch (XMLtoWorldException x2we) {
            write(UIMessages.getInstance().getMessage("load.world.xml.exception") + " " + x2we.getMessage());
        }
    }

    /**
	* Devuelve el directorio del mundo. Full pathname.
	*
	*/
    public String getWorldPath() {
        return worlddir;
    }

    public String getWorldDir() {
        return new File(worlddir).getName() + File.separatorChar;
    }

    public int getMaxRoom() {
        return maxroom;
    }

    public int getMaxItem() {
        return maxitem;
    }

    public int getMaxMob() {
        return maxmob;
    }

    public int getMaxAbstractEntity() {
        return maxabsent;
    }

    public int getMaxSpell() {
        return maxspell;
    }

    /**
	* Devuelve la habitaci�n del mundo con la ID dada.
	*
	* @param roomid la ID de habitaci�n.
	*/
    public Room getRoom(int roomid) {
        return room[roomid % 10000000];
    }

    public List getRooms() {
        List rooms = new ArrayList();
        for (int i = 0; i < maxroom; i++) {
            rooms.add(getRoom(i));
        }
        return rooms;
    }

    public Room getRoom(String ident) {
        int oid;
        try {
            return getRoom(Integer.parseInt(ident));
        } catch (NumberFormatException nfe) {
            Integer i = (Integer) nameTable.get(ident);
            if (i != null) {
                return getRoom(i.intValue());
            } else return null;
        }
    }

    public int roomNameToID(String roomName) {
        Integer i = (Integer) nameTable.get(roomName);
        if (i != null) {
            return i.intValue();
        } else return -1;
    }

    /**
	 * Obtain an Entity in the world from its numerical ID.
	 * @param id
	 * @return
	 */
    public Entity getEntity(int id) {
        if (id >= Utility.spell_summand) return getSpell(id); else if (id >= Utility.absent_summand) return getAbstractEntity(id); else if (id >= Utility.item_summand) return getItem(id); else if (id >= Utility.mobile_summand) return getMobile(id); else if (id >= Utility.room_summand) return getRoom(id); else return null;
    }

    /**
	 * Obtiene una entidad del mundo a partir de su nombre.
	 * @param ident
	 * @return
	 */
    public Entity getEntity(String ident) {
        int oid;
        try {
            return getEntity(Integer.parseInt(ident));
        } catch (NumberFormatException nfe) {
            Integer i = (Integer) nameTable.get(ident);
            if (i != null) {
                return getEntity(i.intValue());
            } else return null;
        }
    }

    /**
	* Devuelve el item del mundo con la ID dada.
	*
	* @param la ID del item. Podemos ponerla con el prefijo 3 (8 d�gitos) u omiti�ndolo.
	*/
    public Item getItem(int itemid) {
        return item[itemid % 10000000];
    }

    public Item getItem(String ident) {
        int oid;
        try {
            return getItem(Integer.parseInt(ident));
        } catch (NumberFormatException nfe) {
            Integer i = (Integer) nameTable.get(ident);
            if (i != null) return getItem(i.intValue()); else return null;
        }
    }

    /**
	* Devuelve el bicho del mundo con la ID dada.
	*
	* @param la ID del bicho. Podemos ponerla con el prefijo 4 (8 d�gitos) u omiti�ndolo.
	*/
    public Mobile getMob(int mobid) {
        return mob[mobid % 10000000];
    }

    public Mobile getMobile(String ident) {
        return getMob(ident);
    }

    public Mobile getMobile(int id) {
        return getMob(id);
    }

    public Mobile getMob(String ident) {
        int oid;
        try {
            return getMob(Integer.parseInt(ident));
        } catch (NumberFormatException nfe) {
            Integer i = (Integer) nameTable.get(ident);
            if (i != null) return getMob(i.intValue()); else return null;
        }
    }

    public EntityList getAllMobiles() {
        EntityList el = new EntityList();
        for (int i = 0; i < maxmob; i++) {
            if (mob[i] != null) el.addEntity(mob[i]);
        }
        return el;
    }

    public EntityList getAllItems() {
        EntityList el = new EntityList();
        for (int i = 0; i < maxitem; i++) {
            if (item[i] != null) el.addEntity(item[i]);
        }
        return el;
    }

    public EntityList getAllRooms() {
        EntityList el = new EntityList();
        for (int i = 0; i < maxroom; i++) {
            if (room[i] != null) el.addEntity(room[i]);
        }
        return el;
    }

    /**
	* Devuelve el abstract entity del mundo con la ID dada.
	*
	* @param la ID del abstract entity. Podemos ponerla con el prefijo 4 (8 d�gitos) u omiti�ndolo.
	*/
    public AbstractEntity getAbstractEntity(int absentid) {
        return absent[absentid % Utility.absent_summand];
    }

    public AbstractEntity getAbstractEntity(String ident) {
        int oid;
        try {
            return getAbstractEntity(Integer.parseInt(ident));
        } catch (NumberFormatException nfe) {
            Integer i = (Integer) nameTable.get(ident);
            if (i != null) return getAbstractEntity(i.intValue()); else return null;
        }
    }

    /**
	* Devuelve el hechizo del mundo con la ID dada.
	*
	* @param la ID del abstract entity. Podemos ponerla con el prefijo 4 (8 d�gitos) u omiti�ndolo.
	*/
    public Spell getSpell(int spellid) {
        return spell[spellid % Utility.spell_summand];
    }

    public Spell getSpell(String ident) {
        int oid;
        try {
            return getSpell(Integer.parseInt(ident));
        } catch (NumberFormatException nfe) {
            Integer i = (Integer) nameTable.get(ident);
            if (i != null) return getSpell(i.intValue()); else return null;
        }
    }

    /**
	* Devuelve el objeto del mundo con la ID dada, ya sea habitaci�n, bicho, hechizo, etc.
	*
	* @param la ID del objeto. Evidentemente, ha de venir completa (con sus ocho d�gitos).
	*/
    public Entity getObject(int objectid) {
        if (objectid == 20000000) return getMob(objectid);
        if (objectid < 20000000) return getRoom(objectid);
        if (objectid < 30000000) return getMob(objectid);
        if (objectid < 40000000) return getItem(objectid);
        if (objectid < 50000000) return getAbstractEntity(objectid);
        if (objectid < 60000000) return getSpell(objectid);
        return null;
    }

    /**
	* Devuelve el objeto del mundo con el identificador dada, que puede ser la propia ID
	* del objeto (un entero) dada en forma de string o un string que se convierte a ID
	* mediante la tabla de nombres.
	*/
    public Entity getObject(String ident) {
        int oid;
        try {
            return getObject(Integer.parseInt(ident));
        } catch (NumberFormatException nfe) {
            Integer i = (Integer) nameTable.get(ident);
            if (i != null) return getObject(i.intValue()); else return null;
        }
    }

    public InputOutputClient getIO() {
        return io;
    }

    public org.w3c.dom.Element getItemNode(int itemid) {
        return itemNode[itemid % 10000000];
    }

    public org.w3c.dom.Element getMobileNode(int mobid) {
        return mobNode[mobid % 10000000];
    }

    public org.w3c.dom.Element getRoomNode(int roomid) {
        return roomNode[roomid % 10000000];
    }

    public org.w3c.dom.Element getAbstractEntityNode(int absentid) {
        return absentNode[absentid % 10000000];
    }

    public org.w3c.dom.Element getSpellNode(int spellid) {
        return spellNode[spellid % 10000000];
    }

    public org.w3c.dom.Element getItemNode(String ident) {
        int oid;
        try {
            return getItemNode(Integer.parseInt(ident));
        } catch (NumberFormatException nfe) {
            Integer i = (Integer) nameTable.get(ident);
            if (i != null) return getItemNode(i.intValue()); else return null;
        }
    }

    public org.w3c.dom.Element getMobileNode(String ident) {
        int oid;
        try {
            return getMobileNode(Integer.parseInt(ident));
        } catch (NumberFormatException nfe) {
            Integer i = (Integer) nameTable.get(ident);
            if (i != null) return getMobileNode(i.intValue()); else return null;
        }
    }

    public org.w3c.dom.Element getRoomNode(String ident) {
        int oid;
        try {
            return getRoomNode(Integer.parseInt(ident));
        } catch (NumberFormatException nfe) {
            Integer i = (Integer) nameTable.get(ident);
            if (i != null) return getRoomNode(i.intValue()); else return null;
        }
    }

    public org.w3c.dom.Element getAbstractEntityNode(String ident) {
        int oid;
        try {
            return getAbstractEntityNode(Integer.parseInt(ident));
        } catch (NumberFormatException nfe) {
            Integer i = (Integer) nameTable.get(ident);
            if (i != null) return getAbstractEntityNode(i.intValue()); else return null;
        }
    }

    public org.w3c.dom.Element getSpellNode(String ident) {
        int oid;
        try {
            return getSpellNode(Integer.parseInt(ident));
        } catch (NumberFormatException nfe) {
            Integer i = (Integer) nameTable.get(ident);
            if (i != null) return getSpellNode(i.intValue()); else return null;
        }
    }

    /**
	 * @deprecated Use {@link #write(String)} instead
	 */
    public void escribir(String s) {
        write(s);
    }

    public void write(String s) {
        io.write(s);
    }

    public void setIO(InputOutputClient es) {
        io = es;
    }

    /**Devuelve el nombre "bonito" del mundo.*/
    public String getModuleName() {
        return modulename;
    }

    public boolean execCode(String routine, String dataSegment) throws EVASemanticException {
        if (itsCode != null) return itsCode.run(routine, dataSegment); else return false;
    }

    public boolean execCode(String routine, Object[] args) throws bsh.TargetError {
        if (itsCode != null) return itsCode.run(routine, this, args); else return false;
    }

    public boolean execCode(String routine, Object[] args, ReturnValue retval) throws bsh.TargetError {
        if (itsCode != null) return itsCode.run(routine, this, args, retval); else return false;
    }

    /**
	 * @deprecated Use {@link #getLanguage()} instead
	 */
    public NaturalLanguage getLang() {
        return getLanguage();
    }

    public NaturalLanguage getLanguage() {
        return lenguaje;
    }

    public void setRandomNumberSeed() {
        Debug.println("Setting world's random generator.");
        semilla = (new java.util.Date()).getTime();
        aleat = new java.util.Random(semilla);
        Debug.println("Seed set to " + semilla);
        for (int i = 0; i < maxroom; i++) {
            room[i].loadNumberGenerator(this);
        }
        for (int i = 0; i < maxmob; i++) {
            mob[i].loadNumberGenerator(this);
        }
        for (int i = 0; i < maxitem; i++) {
            item[i].loadNumberGenerator(this);
        }
    }

    public void setRandomNumberSeed(InputStream logStream) {
        BufferedReader logReader = new BufferedReader(Utility.getBestInputStreamReader(logStream));
        try {
            logReader.readLine();
            semilla = Long.valueOf(logReader.readLine()).longValue();
            aleat = new java.util.Random(semilla);
        } catch (java.io.IOException exc) {
            write("Excepci�n I/O al leer el log");
        }
        for (int i = 0; i < maxroom; i++) {
            room[i].loadNumberGenerator(this);
        }
        for (int i = 0; i < maxmob; i++) {
            mob[i].loadNumberGenerator(this);
        }
        for (int i = 0; i < maxitem; i++) {
            item[i].loadNumberGenerator(this);
        }
        for (int i = 0; i < maxabsent; i++) {
            absent[i].loadNumberGenerator(this);
        }
        for (int i = 0; i < maxspell; i++) {
            spell[i].loadNumberGenerator(this);
        }
    }

    public void setRandomNumberSeed(String logfile) throws java.io.FileNotFoundException {
        FileInputStream logInput = openLogFile(logfile);
        setRandomNumberSeed(logInput);
    }

    public long getRandomNumberSeed() {
        return semilla;
    }

    public java.util.Random getRandom() {
        return aleat;
    }

    public List getPlayerList() {
        return playerList;
    }

    public Player getPlayer() {
        return (Player) playerList.get(0);
    }

    public boolean isLoadingLog() {
        return from_log;
    }

    public org.w3c.dom.Document getXMLRepresentation() throws javax.xml.parsers.ParserConfigurationException {
        org.w3c.dom.Document doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        org.w3c.dom.Element suElemento = doc.createElement("World");
        suElemento.setAttribute("worldName", String.valueOf(worldname));
        suElemento.setAttribute("moduleName", String.valueOf(modulename));
        suElemento.setAttribute("worldDir", String.valueOf(worlddir));
        suElemento.setAttribute("maxroom", String.valueOf(maxroom));
        suElemento.setAttribute("maxitem", String.valueOf(maxitem));
        suElemento.setAttribute("maxmob", String.valueOf(maxmob));
        suElemento.setAttribute("author", String.valueOf(author));
        suElemento.setAttribute("version", String.valueOf(version));
        suElemento.setAttribute("parserVersion", String.valueOf(parserVersion));
        suElemento.setAttribute("date", String.valueOf(date));
        suElemento.setAttribute("type", String.valueOf(type));
        if (vc != null) suElemento.appendChild(vc.getXMLRepresentation(doc));
        org.w3c.dom.Element playerListElement = doc.createElement("PlayerList");
        for (int i = 0; i < playerList.size(); i++) {
            org.w3c.dom.Element playerElement = doc.createElement("Player");
            if (((Player) playerList.get(i)).getUniqueName() != null) playerElement.setAttribute("id", String.valueOf(((Player) playerList.get(i)).getUniqueName())); else playerElement.setAttribute("id", String.valueOf(((Player) playerList.get(i)).getID()));
            playerListElement.appendChild(playerElement);
        }
        suElemento.appendChild(playerListElement);
        org.w3c.dom.Element fileListElement = doc.createElement("FileList");
        for (int i = 0; i < fileList.size(); i++) {
            org.w3c.dom.Element fileElement = doc.createElement("File");
            fileElement.setAttribute("path", (String) fileList.get(i));
            fileListElement.appendChild(fileElement);
        }
        suElemento.appendChild(fileListElement);
        if (itsCode != null) suElemento.appendChild(itsCode.getXMLRepresentation(doc));
        org.w3c.dom.Element pgElement = doc.createElement("PlayerGeneration");
        for (int i = 0; i < playerTemplateNodes.size(); i++) {
            org.w3c.dom.Element templateElt = doc.createElement("Template");
            Element theOriginalElt = (org.w3c.dom.Element) playerTemplateNodes.get(i);
            NodeList children = theOriginalElt.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);
                Node importedChild = doc.importNode(child, true);
                templateElt.appendChild(importedChild);
            }
            NamedNodeMap attrs = theOriginalElt.getAttributes();
            for (int j = 0; j < attrs.getLength(); j++) {
                org.w3c.dom.Node attr = (org.w3c.dom.Node) attrs.item(j);
                templateElt.setAttribute(attr.getNodeName(), attr.getNodeValue());
            }
            pgElement.appendChild(templateElt);
        }
        suElemento.appendChild(pgElement);
        org.w3c.dom.Element roomsElement = doc.createElement("Rooms");
        org.w3c.dom.Element itemsElement = doc.createElement("Items");
        org.w3c.dom.Element mobilesElement = doc.createElement("Mobiles");
        org.w3c.dom.Element absentsElement = doc.createElement("AbstractEntities");
        org.w3c.dom.Element spellsElement = doc.createElement("Spells");
        for (int i = 0; i < maxroom; i++) {
            roomsElement.appendChild(room[i].getXMLRepresentation(doc));
        }
        for (int i = 0; i < maxitem; i++) {
            itemsElement.appendChild(item[i].getXMLRepresentation(doc));
        }
        for (int i = 0; i < maxmob; i++) {
            Debug.println("Mob " + i);
            if (mob[i] != null) mobilesElement.appendChild(mob[i].getXMLRepresentation(doc));
        }
        for (int i = 0; i < maxabsent; i++) {
            absentsElement.appendChild(absent[i].getXMLRepresentation(doc));
        }
        for (int i = 0; i < maxspell; i++) {
            spellsElement.appendChild(spell[i].getXMLRepresentation(doc));
        }
        suElemento.appendChild(roomsElement);
        suElemento.appendChild(itemsElement);
        suElemento.appendChild(mobilesElement);
        suElemento.appendChild(absentsElement);
        suElemento.appendChild(spellsElement);
        doc.appendChild(suElemento);
        return doc;
    }

    public void addItemAssigningID(Item newItem) {
        if (item.length <= maxitem) {
            Item[] newArray;
            if (item.length > 2) newArray = new Item[(int) ((double) item.length * (double) 1.5)]; else newArray = new Item[4];
            for (int i = 0; i < maxitem; i++) newArray[i] = item[i];
            item = newArray;
        }
        item[maxitem] = newItem;
        newItem.setID(maxitem);
        newItem.setWorld(this);
        if (newItem.getUniqueName() != null) nameTable.put(newItem.getUniqueName(), new Integer(maxitem + Utility.item_summand));
        maxitem++;
        newItem.loadNumberGenerator(this);
    }

    public Item addCloneOfItem(Item ourItem) {
        return ourItem.createNewInstance(this, true, true);
    }

    public void addMobileAssigningID(Mobile newMob) {
        if (mob.length <= maxmob) {
            Mobile[] newArray;
            if (mob.length > 2) newArray = new Mobile[(int) ((double) mob.length * (double) 1.5)]; else newArray = new Mobile[4];
            for (int i = 0; i < maxmob; i++) newArray[i] = mob[i];
            mob = newArray;
        }
        mob[maxmob] = newMob;
        newMob.setID(maxmob);
        if (newMob.getUniqueName() != null) nameTable.put(newMob.getUniqueName(), new Integer(maxmob + Utility.mobile_summand));
        maxmob++;
        newMob.loadNumberGenerator(this);
    }

    public void addRoomAssigningID(Room newRoom) {
        if (room.length <= maxroom) {
            Room[] newArray;
            if (room.length > 2) newArray = new Room[(int) ((double) room.length * (double) 1.5)]; else newArray = new Room[4];
            for (int i = 0; i < maxroom; i++) newArray[i] = room[i];
            room = newArray;
        }
        room[maxroom] = newRoom;
        newRoom.setID(maxroom);
        if (newRoom.getUniqueName() != null) nameTable.put(newRoom.getUniqueName(), new Integer(maxroom + Utility.room_summand));
        maxroom++;
        newRoom.loadNumberGenerator(this);
    }

    public void addAbstractEntityAssigningID(AbstractEntity newAbsEnt) {
        if (absent.length <= maxabsent) {
            AbstractEntity[] newArray;
            if (absent.length > 2) newArray = new AbstractEntity[(int) ((double) absent.length * (double) 1.5)]; else newArray = new AbstractEntity[4];
            for (int i = 0; i < maxabsent; i++) newArray[i] = absent[i];
            absent = newArray;
        }
        absent[maxabsent] = newAbsEnt;
        newAbsEnt.setID(maxabsent);
        if (newAbsEnt.getUniqueName() != null) nameTable.put(newAbsEnt.getUniqueName(), new Integer(maxabsent + Utility.absent_summand));
        maxabsent++;
        newAbsEnt.loadNumberGenerator(this);
    }

    public Player createPlayerFromTemplate(InputOutputClient io) throws XMLtoWorldException {
        if (playerTemplateNodes == null || playerTemplateNodes.size() < 1) return null; else return new Player(this, io, (org.w3c.dom.Element) playerTemplateNodes.get(0));
    }

    public Player createPlayerFromTemplate(InputOutputClient io, String templateName) throws XMLtoWorldException {
        Element playerTemplateNode = (Element) playerTemplateNodesByName.get(templateName);
        if (playerTemplateNode == null) return null; else return new Player(this, io, (org.w3c.dom.Element) playerTemplateNode);
    }

    /**
	 * Runs the world's BSH routine to assign a player to a client.
	 * Return value of the routine is stored in the retval argument.
	 * @return True if beanshell code hit end(), false if not.
	 */
    public boolean runAssignPlayerCode(ReturnValue retval, InputOutputClient io) {
        boolean ejecutado = false;
        try {
            Debug.println("Before exec code");
            ejecutado = execCode("assignPlayer", new Object[] { io }, retval);
            Debug.println("After exec code");
        } catch (bsh.TargetError bshte) {
            writeError("bsh.TargetError found at assignPlayer routine\n");
            writeError(ExceptionPrinter.getExceptionReport(bshte));
            Debug.println(bshte.printTargetError(bshte));
            writeError(bshte.printTargetError(bshte));
            io.write(ExceptionPrinter.getExceptionReport(bshte));
            System.err.println("ARGH");
            bshte.printStackTrace();
        }
        return ejecutado;
    }

    public void addNewPlayerASAP(InputOutputClient io) throws XMLtoWorldException {
        ReturnValue retval = new ReturnValue(null);
        boolean ejecutado = runAssignPlayerCode(retval, io);
        if (retval.getRetVal() != null) {
            Player p = (Player) retval.getRetVal();
            if (playerList.contains(p)) {
                p.reconnect(io);
                return;
            } else {
                synchronized (this) {
                    playersToAdd.add(p);
                }
                return;
            }
        } else if (ejecutado) {
            return;
        }
        synchronized (this) {
            Player pl = createPlayerFromTemplate(io);
            if (pl != null) playersToAdd.add(pl); else {
                io.write("Player template generated a null player. No player creation code [assignPlayer], player list or player templates defined?");
                io.write("addNewPlayerASAP() was unsuccessful.");
                return;
            }
        }
        io.write("Player enqueued to be added...\n");
    }

    /**
	 * If there is a room with unique name "limbo" or "Limbo", then that's considered the world's Limbo.
	 * If not, then by default the Limbo is the room numbered 0. 
	 * @return
	 */
    public Room getLimbo() {
        Room r = getRoom("limbo");
        if (r != null) return r;
        r = getRoom("Limbo");
        if (r != null) return r;
        return getRoom(0);
    }

    public void executePlayerIntro(Player p) {
        warnVersionIfNeeded(p);
        try {
            execCode("intro", "");
            execCode("intro", new Object[] { p });
            execCode("intro", new Object[] { p, new Boolean(this.comesFromLoadedState()) });
        } catch (EVASemanticException esm) {
            write("EVASemanticException found at intro routine");
        } catch (bsh.TargetError bshte) {
            write("bsh.TargetError found at intro routine\n");
            writeError(ExceptionPrinter.getExceptionReport(bshte));
        }
    }

    public void update() {
        for (int i = 0; i < getMaxRoom(); i++) {
            if (getRoom(i) != null) {
                getRoom(i).update(this);
            }
        }
        for (int i = 0; i < getMaxMob(); i++) {
            if (getMob(i) != null) {
                getMob(i).update(this);
            }
        }
        for (int i = 0; i < getMaxItem(); i++) {
            if (getItem(i) != null) getItem(i).update(this);
        }
        for (int i = 0; i < getMaxAbstractEntity(); i++) {
            if (getAbstractEntity(i) != null) getAbstractEntity(i).update(this);
        }
        for (int i = 0; i < getMaxSpell(); i++) {
            if (getSpell(i) != null) getSpell(i).update(this);
        }
        if (!(playersToAdd.isEmpty())) {
            for (int i = 0; i < playersToAdd.size(); i++) {
                Player p = (Player) playersToAdd.get(i);
                Debug.println("The " + i + "th Player is " + p);
                addMobileAssigningID(p);
                Room startingRoom;
                if (p.getPropertyValueAsString("room") != null) startingRoom = getRoom(p.getPropertyValueAsString("room")); else startingRoom = getRoom(1);
                p.setRoom(startingRoom);
                if (p.getState() == Mobile.DISABLED) p.setNewState(Mobile.IDLE, 1);
                addPlayer(p);
                p.getIO().write("Has sido a�adido al mundo.\n");
                p.getRoom().reportActionAuto(p, null, "De repente, $1 aparece de la nada.\n", false);
                write("New player joined the game.\n");
                executePlayerIntro(p);
            }
            playersToAdd = new Vector();
        }
    }

    /**Indicates whether we have loaded a state in this World.*/
    private boolean loadedState = false;

    public boolean comesFromLoadedState() {
        return loadedState;
    }

    public void loadState(String statefname) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException, XMLtoWorldException {
        org.w3c.dom.Document d = null;
        room = null;
        item = null;
        mob = null;
        absent = null;
        spell = null;
        playerList = new Vector();
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        io.write(io.getColorCode("information") + UIMessages.getInstance().getMessage("load.world.tree.state") + "\n" + io.getColorCode("reset"));
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(new File(statefname));
        } catch (FileNotFoundException fnfe) {
            try {
                fis = new FileInputStream(new File(Paths.SAVE_PATH, statefname));
            } catch (FileNotFoundException fnfe2) {
                throw (fnfe);
            }
        }
        d = db.parse(fis);
        org.w3c.dom.Element n = d.getDocumentElement();
        loadWorldFromXML(n, io, false);
        loadedState = true;
    }

    public int getNumberOfConnectedPlayers() {
        int addedPlayers = 0;
        for (int i = 0; i < playerList.size(); i++) {
            Player cur = (Player) playerList.get(i);
            if (cur.getState() != Mobile.DISABLED) addedPlayers++;
        }
        return addedPlayers + playersToAdd.size();
    }

    public VisualConfiguration getVisualConfiguration() {
        return vc;
    }

    public List getFileList() {
        return fileList;
    }

    public void addPlayer(Player p) {
        playerList.add(p);
        if (from_log) {
            p.prepareLog(logReader);
        }
    }

    public void writeWithTemplate(String colorTemplate, String s) {
        write(io.getColorCode(colorTemplate) + s + io.getColorCode("reset"));
    }

    public void writeImportant(String s) {
        writeWithTemplate("important", s);
    }

    public void writeError(String s) {
        writeWithTemplate("error", s);
        if (debugMode) {
            System.err.print(s);
        }
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("[World: ");
        if (worldname != null && worldname.length() > 0) sb.append(worldname); else sb.append("(unnamed world)");
        sb.append(", internal handle ");
        sb.append(super.toString());
        sb.append("]");
        return sb.toString();
    }

    private ClassLoader resourceLoader;

    /**
	 * Sets the jar file inside which world resources (multimedia files, etc.) can reside.
	 * @param jarFileURL
	 */
    private void setResourceJarFile(URL jarFileURL) {
        URLClassLoader ucl = new URLClassLoader(new URL[] { jarFileURL }, this.getClass().getClassLoader());
        resourceLoader = ucl;
    }

    private ClassLoader getDefaultResourceLoader() {
        try {
            return new URLClassLoader(new URL[] { new File(this.getWorldPath()).toURI().toURL() }, this.getClass().getClassLoader());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * Fetches an URL for a global resource (a resource that is not particular to a worl but shared by all worlds in AGE, such as
	 * language files, etc.)
	 * @param path
	 * @return
	 */
    public URL getGlobalResource(String path) throws Exception {
        try {
            return this.getClass().getClassLoader().getResource(path);
        } catch (Exception e) {
            throw (e);
        }
    }

    public InputStream getGlobalResourceAsStream(String path) {
        return this.getClass().getClassLoader().getResourceAsStream(path);
    }

    public URL getResource(String path) {
        try {
            return new URL(worldurl, path);
        } catch (MalformedURLException e) {
            return null;
        }
    }

    public InputStream getResourceAsStream(String path) {
        try {
            return new URL(worldurl, path).openStream();
        } catch (MalformedURLException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }

    /**
	 * Give an explicit warning if the version required by the world is more recent than the AGE version being used.
	 * Pass a player to warn the player, else the server is warned.
	 */
    public void warnVersionIfNeeded(Player p) {
        if (new VersionComparator().compare(GameEngineThread.getVersionNumber(), parserVersion) < 0) {
            if (p == null) {
                writeError(UIMessages.getInstance().getMessage("age.version.warning.header") + "\n");
                writeError(UIMessages.getInstance().getMessage("age.version.warning.2", "$curversion", GameEngineThread.getVersionNumber(), "$reqversion", parserVersion));
                writeError(" ");
                writeError(UIMessages.getInstance().getMessage("age.download.url") + "\n\n");
            } else {
                p.writeError(UIMessages.getInstance().getMessage("age.version.warning.header") + "\n");
                p.writeError(UIMessages.getInstance().getMessage("age.version.warning.2", "$curversion", GameEngineThread.getVersionNumber(), "$reqversion", parserVersion));
                p.writeError(" ");
                p.writeError(UIMessages.getInstance().getMessage("age.download.url") + "\n\n");
                p.waitKeyPress();
            }
        }
    }

    public String getRequiredAGEVersion() {
        return parserVersion;
    }

    public int commandMatchingMode = Entity.MODERATE_COMMAND_MATCHING;

    /**
	 * values should be static constants from class Entity.
	 * @param matchingMode
	 */
    public void setCommandMatchingMode(int matchingMode) {
        commandMatchingMode = matchingMode;
    }

    public int getCommandMatchingMode() {
        return commandMatchingMode;
    }

    private AGESpellChecker spellChecker;

    public AGESpellChecker getSpellChecker() {
        if (spellChecker == null) spellChecker = new AGESpellChecker(this, getLanguage());
        return spellChecker;
    }

    public ObjectCode getAssociatedCode() {
        return itsCode;
    }

    public String getParserVersion() {
        return parserVersion;
    }

    /**
	 * Returns a String starting with base that has not been used as a unique name in this World.
	 * @param base
	 * @return
	 */
    public String generateUnusedUniqueName(String base) {
        for (; ; ) {
            String candidate = ((base == null) ? "" : base) + UUID.randomUUID().toString();
            if (getEntity(candidate) == null) return candidate;
        }
    }
}
