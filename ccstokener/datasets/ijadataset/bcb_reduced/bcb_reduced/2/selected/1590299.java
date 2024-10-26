package com.intersys.bio.paralogs.model;

import com.jalapeno.annotations.Indices;
import com.jalapeno.annotations.Index;
import com.jalapeno.annotations.PropertyParameters;
import com.jalapeno.annotations.PropertyParameter;
import com.intersys.bio.paralogs.db.DAO;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Set;
import java.util.Map;
import javax.persistence.Entity;

@Indices({ @Index(name = "urlIdx", propertyNames = { "url" }, isPrimaryKey = true) })
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
        while (in.ready()) {
            String line = in.readLine();
            if (line == null) break;
            line = line.trim();
            String TREE = "TREE:";
            if (!line.startsWith(TREE)) continue;
            int end = line.indexOf(';');
            if (end < 0) end = line.length();
            toParse = line.substring(TREE.length(), end).trim();
        }
        System.out.print("Parsing... ");
        parse(tree, toParse);
        return tree;
    }

    protected BaseTreeNode createNode() {
        return new SpeciesTreeNode();
    }

    protected int processId(BaseTreeNode n, String toParse, int pos) {
        int column = toParse.indexOf(':', pos);
        String sub = toParse.substring(pos, column);
        SpeciesTreeNode node = (SpeciesTreeNode) n;
        node.species = map(sub);
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
        if (args.length == 0) {
            args = new String[] { "http://hgdownload.cse.ucsc.edu/goldenPath/dm3/phastCons15way/ave.noncons.mod" };
            main(args);
            args = new String[] { "http://hgdownload.cse.ucsc.edu/goldenPath/dm3/phastCons15way/ave.cons.mod" };
            main(args);
            return;
        }
        DAO dao = DAO.createDAO();
        SpeciesTree tree = create(args[0]);
        dao.save(tree);
        dao.cleanUp();
        System.out.println("Done.");
    }

    private static void parse(SpeciesTree tree, String toParse) {
        baseParse(tree, toParse);
        SpeciesTreeNode root = (SpeciesTreeNode) tree.getRoot();
        root.calculateAge();
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
