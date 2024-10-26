package org.xmlcml.cml.map;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.ParsingException;
import nu.xom.ValidityException;
import org.xmlcml.cml.base.CMLBuilder;
import org.xmlcml.cml.base.CMLConstants;
import org.xmlcml.cml.base.CMLElement;
import org.xmlcml.cml.base.CMLUtil;
import org.xmlcml.euclid.Util;

/**
 * A container for one or more indexables.
 * 
 * indexableList can contain several indexables. These may be related in many
 * ways and there is are controlled semantics. However it should not be used for
 * a indexable consisting of descendant indexables for which indexable should be
 * used. A indexableList can contain nested indexableLists.
 * 
 */
public class IndexableByIdListManager implements CMLConstants {

    /** duplicate id exception */
    public static final String DUPLICATE_ID = "duplicate id in indexableList: ";

    private IndexableByIdList indexableList;

    private Map<String, Indexable> map;

    private Map<String, Indexable> lowerCaseMap;

    private String indexableLocalName;

    /**
     * constructor.
     * 
     * @param indexableList
     */
    public IndexableByIdListManager(IndexableByIdList indexableList) {
        this.indexableList = indexableList;
        ensureMap();
    }

    private void ensureMap() {
        if (map == null) {
            map = new HashMap<String, Indexable>();
        }
        if (lowerCaseMap == null) {
            lowerCaseMap = new HashMap<String, Indexable>();
        }
    }

    /**
     * index all current indexable children.
     * 
     * @return map
     */
    public Map<String, Indexable> indexList() {
        ensureMap();
        map.clear();
        lowerCaseMap.clear();
        indexableLocalName = indexableList.getIndexableLocalName();
        List<Node> indexables = CMLUtil.getQueryNodes((Node) indexableList, C_E + indexableLocalName, CMLConstants.CML_XPATH);
        for (Node node : indexables) {
            indexableList.addIndexable((Indexable) node);
        }
        return map;
    }

    /**
     * make indexableList from URL. either contains a directory with indexable
     * in *.xml or an indexableList in a single XML file.
     * 
     * @param url
     * @param indexableListClass
     * @return list of indexables
     */
    public static IndexableByIdList createFrom(URL url, Class<?> indexableListClass) {
        IndexableByIdList indexableList = null;
        if ("file".equals(url.getProtocol())) {
            File file;
            try {
                file = new File(url.toURI());
                if (file.isDirectory()) {
                    indexableList = createFromDirectory(file, indexableListClass);
                } else if (file.toString().endsWith(XML_SUFF)) {
                    try {
                        indexableList = (IndexableByIdList) new CMLBuilder().build(file).getRootElement();
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot parse " + file, e);
                    }
                } else {
                    throw new RuntimeException("exptected either a directory ot *.xml; found: " + url);
                }
            } catch (URISyntaxException e1) {
                e1.printStackTrace();
            }
        } else {
            if (url.toExternalForm().endsWith(XML_SUFF)) {
                InputStream in = null;
                try {
                    in = url.openStream();
                    indexableList = (IndexableByIdList) new CMLBuilder().build(in).getRootElement();
                } catch (ValidityException e) {
                    throw new RuntimeException("Problem parsing " + url + " as a CML document: " + e.getMessage(), e);
                } catch (ParsingException e) {
                    throw new RuntimeException("Problem parsing " + url + " as a CML document: " + e.getMessage(), e);
                } catch (IOException e) {
                    throw new RuntimeException("Problem parsing " + url + " as a CML document: " + e.getMessage(), e);
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                            ;
                        }
                    }
                }
            } else {
                throw new RuntimeException("Don't know how to get referenced document(s) from : " + url);
            }
        }
        return indexableList;
    }

    private static IndexableByIdList createFromDirectory(File dir, Class<?> indexableListClass) {
        IndexableByIdList indexableList = null;
        try {
            indexableList = (IndexableByIdList) indexableListClass.newInstance();
        } catch (Exception e1) {
            CMLUtil.BUG("" + e1);
        }
        File[] files = dir.listFiles();
        CMLBuilder builder = new CMLBuilder();
        for (File file : files) {
            if (file.toString().endsWith(XML_SUFF)) {
                try {
                    Document document = builder.build(file);
                    Element element = document.getRootElement();
                    if (element instanceof Indexable) {
                        indexableList.addIndexable((Indexable) element);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    System.err.println("Cannot parse XML file: " + file + " /" + e);
                }
            }
        }
        return indexableList;
    }

    /**
     * get map of id to indexable.
     * 
     * @return map
     */
    public Map<String, Indexable> getIndex() {
        ensureMap();
        return map;
    }

    /**
     * get case-insensitive map of id to indexable.
     * 
     * @return map
     */
    Map<String, Indexable> getLowerCaseIndex() {
        ensureMap();
        return lowerCaseMap;
    }

    /**
     * add indexable.
     * 
     * @param indexable
     *            to add
     * @throws RuntimeException
     *             if id already in map
     */
    public void add(Indexable indexable) throws RuntimeException {
        ensureMap();
        String id = indexable.getId();
        if (id == null) {
            throw new RuntimeException("indexable has no id: " + indexable.getClass());
        }
        if (map.containsKey(id)) {
            throw new RuntimeException(DUPLICATE_ID + id);
        }
        ((Element) indexableList).appendChild((Element) indexable);
        map.put(id, indexable);
    }

    /**
     * insert indexable.
     * 
     * @param indexable
     *            to add
     * @param position
     * @throws RuntimeException
     *             if id already in map
     */
    public void insert(Indexable indexable, int position) throws RuntimeException {
        ensureMap();
        String id = indexable.getId();
        if (id == null) {
            throw new RuntimeException("indexable has no id: " + indexable.getClass());
        }
        if (map.containsKey(id)) {
            throw new RuntimeException(DUPLICATE_ID + id);
        }
        ((Element) indexableList).insertChild((Element) indexable, position);
        map.put(indexable.getId(), indexable);
    }

    /**
     * insert indexable.
     * 
     * @param indexable
     *            to add
     * @throws RuntimeException
     *             if id already in map
     */
    public void insertInOrderOfId(Indexable indexable) throws RuntimeException {
        String id = indexable.getId();
        if (id == null) {
            throw new RuntimeException("indexable has no id: " + indexable.getClass());
        }
        if (map.containsKey(id)) {
            throw new RuntimeException(DUPLICATE_ID + id);
        }
        boolean added = false;
        Elements elements = ((Element) indexableList).getChildElements();
        for (int i = 0; i < elements.size(); i++) {
            if (elements.get(i) instanceof Indexable) {
                Indexable indexable0 = (Indexable) elements.get(i);
                String id0 = indexable0.getId();
                if (id.compareTo(id0) < 0) {
                    ((Element) indexableList).insertChild((Element) indexable, i);
                    added = true;
                    break;
                }
            }
        }
        if (!added) {
            ((Element) indexableList).appendChild((Element) indexable);
        }
        map.put(indexable.getId(), indexable);
    }

    /**
     * get indexable children.
     * 
     * @return indexables
     */
    public List<Indexable> getIndexables() {
        List<Indexable> list = new ArrayList<Indexable>();
        Elements elements = ((Element) indexableList).getChildElements();
        for (int i = 0; i < elements.size(); i++) {
            Node element = elements.get(i);
            if (element instanceof Indexable) {
                list.add((Indexable) element);
            }
        }
        return list;
    }

    /**
     * remove indexable. removes BOTH from map and from parent indexableList
     * 
     * @param indexable
     *            to remove
     */
    public void remove(Indexable indexable) {
        ensureMap();
        String id = indexable.getId();
        if (map.containsKey(id)) {
            ((Element) indexableList).removeChild((Element) indexable);
            map.remove(id);
        }
    }

    /**
     * get indexable by id.
     * 
     * @param id
     * @return indexable or null
     */
    public Indexable getById(String id) {
        ensureMap();
        return map.get(id);
    }

    /**
     */
    public void debug() {
        Util.println("IndexableByIdListManager DEBUG");
        for (Indexable indexable : indexableList.getIndexables()) {
            ((CMLElement) indexable).debug("INDEXABLE");
        }
        Util.println("MAP " + map.size());
        for (String key : map.keySet()) {
            Util.println(key);
            ((CMLElement) map.get(key)).debug("MAP");
        }
        Util.println("LOWERCASE MAP " + map.size());
        for (String key : lowerCaseMap.keySet()) {
            Util.println(key);
            ((CMLElement) map.get(key)).debug("KEY");
        }
        Util.println("indexableLocalName: " + indexableLocalName);
    }
}
