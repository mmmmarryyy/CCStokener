package com.intersys.acidminer.model;

import com.jalapeno.annotations.Indices;
import com.jalapeno.annotations.Index;
import com.jalapeno.annotations.PropertyParameters;
import com.jalapeno.annotations.PropertyParameter;
import com.intersys.acidminer.db.DAO;
import static com.intersys.acidminer.Toolkit.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import java.util.Properties;
import javax.persistence.Entity;

@Indices({ @Index(name = "urlIdx", propertyNames = { "url" }, isPrimaryKey = true), @Index(name = "Extent3Idx", isExtent = true, type = "bitmap") })
@Entity
public class SpeciesTree extends BaseTree {

    private String mURL;

    private Map<BaseTreeNode, Set<String>> mInnerSets;

    public SpeciesTree() {
    }

    public static SpeciesTree create(String url) throws IOException {
        SpeciesTree tree = new SpeciesTree();
        tree.setUrl(url);
        System.out.println("Fetching URL:  " + url);
        BufferedReader in = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        String toParse = null;
        Properties properties = new Properties();
        properties.load(in);
        String line = properties.getProperty("TREE");
        if (line == null) return null;
        int end = line.indexOf(';');
        if (end < 0) end = line.length();
        toParse = line.substring(0, end).trim();
        System.out.print("Parsing... ");
        parse(tree, toParse, properties);
        return tree;
    }

    public static void update(DAO dao, String url) throws Exception {
        SpeciesTree newTree = create(url);
        SpeciesTree oldTree = (SpeciesTree) dao.getDBObject(SpeciesTree.class, url);
        Map<String, SpeciesTreeNode> nodes = new HashMap();
        for (BaseTreeNode n : oldTree.getNodes()) nodes.put(n.getName(), (SpeciesTreeNode) n);
        for (BaseTreeNode n : newTree.getNodes()) {
            SpeciesTreeNode newNode = (SpeciesTreeNode) n;
            SpeciesTreeNode oldNode = nodes.get(newNode.getName());
            if (oldNode == null) {
                throw new IllegalStateException("Update Failed: Node " + newNode.getName() + " not found.");
            }
            oldNode.setAge(newNode.getAge());
            oldNode.setLength(newNode.getLength());
        }
        dao.shallowSave(nodes.values());
    }

    protected BaseTreeNode createNode() {
        return new SpeciesTreeNode();
    }

    protected int processId(BaseTreeNode n, String toParse, int pos) {
        int column = toParse.indexOf(':', pos);
        String sub = toParse.substring(pos, column);
        SpeciesTreeNode node = (SpeciesTreeNode) n;
        node.setSpecies(map(sub));
        pos = column;
        return pos;
    }

    @PropertyParameters({ @PropertyParameter(name = "MAXLEN", value = "512") })
    public String getUrl() {
        return mURL;
    }

    public void setUrl(String url) {
        this.mURL = url;
    }

    protected String getRootPath() {
        String id;
        if (mURL.contains(".noncons.")) id = "NON-CONSERVATIVE"; else if (mURL.contains(".cons.")) id = "CONSERVATIVE"; else if (mURL.startsWith("file")) id = "SP:"; else id = mURL;
        return id + super.getRootPath();
    }

    protected Map<BaseTreeNode, Set<String>> innerSets() {
        if (mInnerSets == null) {
            mInnerSets = new HashMap<BaseTreeNode, Set<String>>();
            SpeciesTreeNode root = (SpeciesTreeNode) getRoot();
            root.calculateInnerSet(mInnerSets);
        }
        return mInnerSets;
    }

    public static void main(String[] args) throws Exception {
        String url = SPECIES_URL;
        if (args.length == 0) {
        }
        boolean update = false;
        for (int i = 0; i < args.length; i++) {
            if ("-url".equals(args[i])) url = args[++i]; else if ("update".equals(args[i])) update = true; else if ("create".equals(args[i])) update = false;
        }
        DAO dao = DAO.createDAO();
        if (update) {
            update(dao, url);
            dao.cleanUp();
            return;
        }
        SpeciesTree tree = create(url);
        dao.save(tree);
        dao.cleanUp();
        System.out.println("Tree created.");
    }

    private static void parse(SpeciesTree tree, String toParse, Properties p) {
        baseParse(tree, toParse);
        boolean agesRead = true;
        for (BaseTreeNode n : tree.getNodes()) {
            SpeciesTreeNode node = (SpeciesTreeNode) n;
            if (node.isTerminal()) {
                node.setAge(0);
                continue;
            }
            String name = node.isRoot() ? "root" : node.getName();
            String agestr = p.getProperty(name);
            if (agestr == null) {
                System.out.println("Age not found for " + name);
                agesRead = false;
                continue;
            }
            node.setAge(Double.parseDouble(agestr));
        }
        if (!agesRead) {
            SpeciesTreeNode root = (SpeciesTreeNode) tree.getRoot();
            root.calculateAge();
        }
    }

    private static String map(String species) {
        if (species.charAt(0) == 'd') {
            String mapped = frb_hgdow_Map.get(species);
            if (mapped == null) return species; else return mapped;
        } else return "d" + species;
    }

    public static final HashMap<String, String> frb_hgdow_Map = new HashMap<String, String>();

    static {
        frb_hgdow_Map.put("dm3", "dmel");
        frb_hgdow_Map.put("droSim1", "dsim");
        frb_hgdow_Map.put("droSec1", "dsec");
        frb_hgdow_Map.put("droYak2", "dyak");
        frb_hgdow_Map.put("droEre2", "dere");
        frb_hgdow_Map.put("droAna3", "dana");
        frb_hgdow_Map.put("dp4", "dpse");
        frb_hgdow_Map.put("droPer1", "dper");
        frb_hgdow_Map.put("droWil1", "dwil");
        frb_hgdow_Map.put("droVir3", "dvir");
        frb_hgdow_Map.put("droMoj3", "dmoj");
        frb_hgdow_Map.put("droGri2", "dgri");
    }
}
