package org.qsari.effectopedia.data;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;
import org.qsari.effectopedia.base.EffectopediaObject;
import org.qsari.effectopedia.base.EffectopediaObjects;
import org.qsari.effectopedia.base.IndexedObject;
import org.qsari.effectopedia.base.XMLExportable;
import org.qsari.effectopedia.core.Effectopedia;
import org.qsari.effectopedia.core.objects.Link_EffectToEffect;
import org.qsari.effectopedia.core.objects.Link_SubstanceToMIE;
import org.qsari.effectopedia.core.objects.PathwayElement;
import org.qsari.effectopedia.defaults.DefaultServerSettings;
import org.qsari.effectopedia.system.UserKey;

public class XMLFileDS extends DataSource {

    private void replaceExternalIDReferencesWithIDs() {
        liveIndices.ids_ref_list.replaceExternalIDReferencesWithIDs(processedIDs);
        liveIndices.id_ref_list.replaceExternalIDReferencesWithIDs(processedIDs);
        archiveIndices.ids_ref_list.replaceExternalIDReferencesWithIDs(processedArchivedIDs);
        archiveIndices.id_ref_list.replaceExternalIDReferencesWithIDs(processedArchivedIDs);
        System.out.println("replaceExternalIDReferencesWithIDs");
        Iterator<Map.Entry<Long, EffectopediaObject>> it = processedIDs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, EffectopediaObject> entry = it.next();
            EffectopediaObject eo = entry.getValue();
            if (eo instanceof PathwayElement) {
                PathwayElement pe = (PathwayElement) eo;
                System.out.print(pe.getClass() + "ID=\t" + pe.getID() + "\tExternalID=\t" + pe.getExternalID() + "\tDefaultID=\t" + pe.getDefaultID());
                if (pe instanceof Link_SubstanceToMIE) System.out.print("\tsubstance=\t" + ((Link_SubstanceToMIE) pe).getSubstance().getReferenceID() + "\teffect=\t" + ((Link_SubstanceToMIE) pe).getEffect().getReferenceID());
                if (pe instanceof Link_EffectToEffect) System.out.print("\tfrom effect=\t" + ((Link_EffectToEffect) pe).getFromEffect().getReferenceID() + "\tto effect=\t" + ((Link_EffectToEffect) pe).getToEffect().getReferenceID());
                System.out.print("\tincomming=\t" + Arrays.toString(pe.incommingAssociations()));
                System.out.print("\toutgoing=\t" + Arrays.toString(pe.outgoingAssociations()));
                System.out.println("\tpathway=\t" + Arrays.toString(pe.getPathwayIDs().getCachedIDs()));
            }
        }
    }

    public XMLFileDS() {
        super();
        processedIDs = new HashMap<Long, EffectopediaObject>();
        processedArchivedIDs = new HashMap<Long, EffectopediaObject>();
        this.fileName = null;
    }

    public synchronized boolean loadFromXMLFile(String fileName) {
        this.fileName = fileName;
        Document doc = readDocumentFromFile(fileName);
        if (doc == null) return false;
        Effectopedia.EFFECTOPEDIA.setData(this);
        Namespace namespace = Namespace.XML_NAMESPACE;
        Element e = doc.getRootElement();
        externalIDs = Long.parseLong(e.getAttributeValue("max_id", namespace));
        Element annotationElement = e.getChild("annotation", namespace);
        if (annotationElement != null) annotation = annotationElement.getText();
        Element userKeyElement = e.getChild("user_key", namespace);
        if (userKeyElement != null) {
            long userID = Long.parseLong(userKeyElement.getAttributeValue("user_id", namespace));
            fileUserKey = new UserKey(userID);
            validFileUserKey = fileUserKey.isValidKey(userKeyElement.getText(), userID);
        }
        shallowMode = false;
        internalLoad = true;
        createLive = true;
        processedIDs.clear();
        EffectopediaObject.clearDefaultObectsExternalIds();
        livePathwayElements.loadFromXMLElement(e, namespace);
        Element liveComponentsElement = e.getChild("live_components", namespace);
        liveIndices.loadFromXMLElement(liveComponentsElement, namespace);
        createLive = false;
        processedArchivedIDs.clear();
        processedArchivedIDs.putAll(processedIDs);
        Element archivedComponentsElement = e.getChild("archived_components", namespace);
        archiveIndices.loadFromXMLElement(archivedComponentsElement, namespace);
        createLive = true;
        shallowMode = true;
        replaceExternalIDReferencesWithIDs();
        Element editHistoryElement = e.getChild("edit_history", namespace);
        editHistory.loadFromXMLElement(editHistoryElement, namespace);
        internalLoad = false;
        liveIndices.updateParenthood();
        return true;
    }

    public URL getURL(String url) {
        try {
            URL newURL = new URL(url);
            remoteFile = true;
            return newURL;
        } catch (MalformedURLException e) {
            remoteFile = false;
            return null;
        }
    }

    public Document readDocumentFromFile(String fileName) {
        try {
            SAXBuilder builder = new SAXBuilder();
            URL url = getURL(fileName);
            if (fileName.substring(fileName.lastIndexOf("."), fileName.length()).equalsIgnoreCase(".aopz")) {
                FileInputStream fileInputStream = null;
                BufferedInputStream inputStream;
                if (url == null) {
                    fileInputStream = new FileInputStream(fileName);
                    inputStream = new BufferedInputStream(fileInputStream);
                } else {
                    inputStream = new BufferedInputStream(url.openStream());
                    remoteFile = true;
                }
                ZipInputStream zipInputEntry = new ZipInputStream(inputStream);
                ZipEntry entry = zipInputEntry.getNextEntry();
                if (entry != null) {
                    String temp = readZIPEntry(zipInputEntry, (int) entry.getSize());
                    Document doc = builder.build(new StringReader(temp));
                    zipInputEntry.close();
                    if (url == null) fileInputStream.close();
                    return doc;
                }
                return null;
            } else {
                if (url != null) return builder.build(url.openStream()); else return builder.build(new File(fileName));
            }
        } catch (JDOMException e) {
            e.printStackTrace();
            return null;
        } catch (NullPointerException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String readZIPEntry(ZipInputStream zipInputStream, int size) throws IOException {
        int bytesRead;
        BufferedInputStream buffer = new BufferedInputStream(zipInputStream);
        ByteArrayOutputStream content = (size > 0) ? (new ByteArrayOutputStream(size)) : (new ByteArrayOutputStream());
        byte data[] = new byte[BUFFER_SIZE];
        while ((bytesRead = buffer.read(data, 0, BUFFER_SIZE)) != -1) content.write(data, 0, bytesRead);
        return content.toString("UTF-8");
    }

    public boolean saveAsXMLFile(String fileName, boolean saveAsLocalFile) {
        Document doc = new Document();
        Namespace namespace = Namespace.XML_NAMESPACE;
        Element e = new Element("effectopedia", namespace);
        setBrowsing(false);
        Element fileFormatElement = new Element("format", namespace);
        fileFormatElement.setAttribute(new Attribute("version", VERSION, namespace));
        fileFormatElement.setText("Effectopedia Adverse Outcome Pathways Format");
        e.addContent(fileFormatElement);
        Element annotationElement = new Element("annotation", namespace);
        annotationElement.setText(annotation);
        e.addContent(annotationElement);
        Element userKeyElement = new Element("user_key", namespace);
        userKeyElement.setText(userKey.getKey());
        userKeyElement.setAttribute("user_id", Long.toString(Effectopedia.EFFECTOPEDIA.getCurrentUserID()), namespace);
        e.addContent(userKeyElement);
        liveIndices.updateExternalIDs();
        this.shallowMode = false;
        processedIDs.clear();
        livePathwayElements.storeToXMLElement(processedIDs, e, namespace, XMLExportable.NO_VISUAL_ATTRIBUTES);
        e.addContent(liveIndices.storeToXMLElement(processedIDs, new Element("live_components", namespace), namespace, XMLExportable.NO_VISUAL_ATTRIBUTES));
        this.shallowMode = true;
        processedArchivedIDs.clear();
        e.addContent(archiveIndices.storeToXMLElement(processedArchivedIDs, new Element("archived_components", namespace), namespace, XMLExportable.NO_VISUAL_ATTRIBUTES));
        e.addContent(editHistory.storeToXMLElement(new Element("edit_history", namespace), namespace, XMLExportable.NO_VISUAL_ATTRIBUTES));
        e.setAttribute("max_id", Long.toString(externalIDs), namespace);
        e.setAttribute("default_max_id", Long.toString(EffectopediaObject.getDefaultIDs()), namespace);
        doc.setRootElement(e);
        setBrowsing(true);
        this.fileName = fileName;
        return writeToFile(doc, fileName, saveAsLocalFile);
    }

    private void DEBUG_PrintIndices() {
        HashMap<Long, Long> idMap = liveIndices.generateExternalIDToIDMap();
        System.out.println("live Indices");
        Iterator<Map.Entry<Long, Long>> it = idMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Long> entry = it.next();
            System.out.println(entry.getValue() + "\t->\t" + entry.getKey());
        }
        idMap = archiveIndices.generateExternalIDToIDMap();
        System.out.println("archive Indices");
        it = idMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Long, Long> entry = it.next();
            System.out.println(entry.getValue() + "\t->\t" + entry.getKey());
        }
    }

    public boolean writeToFile(Document doc, String fileName, boolean saveAsLocalFile) {
        OutputStreamWriter outputStreamWriter = null;
        FileOutputStream fileStream = null;
        URL url = null;
        BufferedOutputStream outputStream = null;
        boolean saveRemoteFile = !saveAsLocalFile;
        try {
            if (saveRemoteFile) {
                url = new URL(DefaultServerSettings.getFTPURL());
                revision = Long.parseLong(DefaultServerSettings.getResponce());
                URLConnection urlc = url.openConnection();
                outputStream = new BufferedOutputStream(urlc.getOutputStream());
            } else {
                fileStream = new FileOutputStream(fileName);
                outputStream = new BufferedOutputStream(fileStream);
            }
            XMLOutputter XMLOut = new XMLOutputter();
            XMLOut.setFormat(XMLOut.getFormat().setExpandEmptyElements(true));
            if (fileName.substring(fileName.lastIndexOf("."), fileName.length()).equalsIgnoreCase(".aopz")) {
                ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream);
                ZipEntry entry = new ZipEntry("pathways.aop");
                zipOutputStream.putNextEntry(entry);
                XMLOut.output(doc, zipOutputStream);
                zipOutputStream.close();
                outputStream.close();
                System.out.println("entry.size:" + entry.getSize());
                if (!saveRemoteFile) fileStream.close();
            } else {
                if (saveRemoteFile) outputStreamWriter = new OutputStreamWriter(outputStream, "UTF-8"); else outputStreamWriter = new OutputStreamWriter(fileStream, "UTF-8");
                XMLOut.output(doc, outputStreamWriter);
                outputStreamWriter.close();
            }
            return true;
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void add(IndexedObject io) {
        EffectopediaObjects objects = liveIndices.get(io.getClass());
        objects.put(io.getID(), io);
        if (internalLoad) processedIDs.put(io.getExternalID(), (EffectopediaObject) io);
        System.out.println("add " + io.getClass() + " with externalID\t" + io.getExternalID() + "\t->\t" + io.getID() + "\tinto " + objects.getClass() + "\t/\t" + processedIDs.size());
    }

    public void add(Class<? extends IndexedObject> objectClass, IndexedObject io) {
        EffectopediaObjects<? extends EffectopediaObject> objects = liveIndices.get(objectClass);
        ((EffectopediaObjects) objects).put(io.getID(), io);
        if (internalLoad) {
            processedIDs.put(io.getExternalID(), (EffectopediaObject) io);
            if (io instanceof EffectopediaObject) ((EffectopediaObject) io).getContainedExternalIDs(processedIDs);
        }
    }

    public void addToArchive(Class<? extends IndexedObject> objectClass, IndexedObject io) {
        EffectopediaObjects<? extends EffectopediaObject> objects = archiveIndices.get(objectClass);
        ((EffectopediaObjects) objects).put(io.getID(), io);
        if (internalLoad) processedArchivedIDs.put(io.getExternalID(), (EffectopediaObject) io);
        if (io instanceof EffectopediaObject) ((EffectopediaObject) io).getContainedExternalIDs(processedArchivedIDs);
    }

    public IndexedObject remove(Class<? extends IndexedObject> objectClass, long id) {
        EffectopediaObjects<?> objects = liveIndices.get(objectClass);
        if (objects != null) {
            IndexedObject o = (IndexedObject) objects.get(id);
            if (o != null) {
                objects.remove(id);
                return o;
            } else return null;
        } else return null;
    }

    public EffectopediaObject getLiveEffectopediaObjectByExternalID(long externalID) {
        return processedIDs.get(externalID);
    }

    public EffectopediaObject getArchiveEffectopediaObjectByExternalID(long externalID) {
        return processedArchivedIDs.get(externalID);
    }

    public EffectopediaObject getEffectopediaObjectByExternalID(long externalID) {
        if (isCreateLive()) return processedIDs.get(externalID); else return null;
    }

    public long getIDbyExternalID(long ClassID, long externalID) {
        EffectopediaObject eo;
        if (isCreateLive()) eo = processedIDs.get(externalID); else eo = processedArchivedIDs.get(externalID);
        return (eo != null) ? eo.getID() : 0;
    }

    public static <E extends EffectopediaObject> E load(Class<?> cl, E effectopediaObejct, Element element, Namespace namespace) {
        if (element == null) return effectopediaObejct;
        Attribute a = element.getAttribute("id", namespace);
        long externalID = Long.parseLong(a.getValue());
        long DefaultID = EffectopediaObject.getDefaultID(element, namespace);
        EffectopediaObject eo;
        eo = Effectopedia.EFFECTOPEDIA.getData().getEffectopediaObjectByExternalID(externalID);
        Attribute b = element.getAttribute("defaultID", namespace);
        if (eo == null) {
            try {
                if (DefaultID != EffectopediaObject.NON_DEFAULT) {
                    if ((effectopediaObejct != null) && (effectopediaObejct.getDefaultID() == DefaultID)) eo = effectopediaObejct; else {
                        eo = EffectopediaObject.getEffectopediaObjectByDefaultID(DefaultID);
                    }
                    eo.updateExternalIDFromXMLElement(element, namespace);
                } else {
                    if (effectopediaObejct != null) eo = effectopediaObejct; else eo = (EffectopediaObject) cl.newInstance();
                    eo.loadFromXMLElement(element, namespace);
                }
                if (Effectopedia.EFFECTOPEDIA.getData().isCreateLive()) Effectopedia.EFFECTOPEDIA.getData().add(eo.getClass(), eo); else if (Effectopedia.EFFECTOPEDIA.getData().getLiveEffectopediaObjectByExternalID(externalID) == null) Effectopedia.EFFECTOPEDIA.getData().addToArchive(eo.getClass(), eo);
            } catch (InstantiationException e1) {
                e1.printStackTrace();
                return null;
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
                return null;
            }
        }
        return (E) eo;
    }

    public long getRevision() {
        return revision;
    }

    public String getCustomTitle() {
        return customTitle;
    }

    public void setCustomTitle(String customTitle) {
        this.customTitle = customTitle;
    }

    public void setRevision(long revision) {
        this.revision = revision;
    }

    public boolean isRemoteFile() {
        return remoteFile;
    }

    public String toString() {
        if (customTitle != null) return customTitle;
        if (remoteFile) return "Remote XML file:" + fileName; else return "XML file:" + fileName;
    }

    public static final String VERSION = "0.8.5";

    protected String customTitle;

    protected long revision;

    protected boolean remoteFile;

    protected String fileName;

    protected HashMap<Long, EffectopediaObject> processedIDs;

    protected HashMap<Long, EffectopediaObject> processedArchivedIDs;

    protected UserKey fileUserKey;

    protected long fileUserID;

    protected boolean validFileUserKey;

    private final int BUFFER_SIZE = 4096;
}
