package com.intersys.bio.paralogs.model;

import com.intersys.bio.paralogs.db.DAO;
import com.jalapeno.annotations.Index;
import com.jalapeno.annotations.Indices;
import com.ice.tar.TarInputStream;
import com.ice.tar.TarEntry;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.InputStream;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

@Entity
@Indices({ @Index(name = "ParalogIdx", propertyNames = { "hasParalogs" }, type = "bitmap"), @Index(name = "MFIdx", propertyNames = { "molecularFunction" }, type = "bitmap") })
public class Dataset implements Serializable {

    public static final String BASE_URL = "http://www.indiana.edu/~hahnlab/fly/DfamDB/cgi-bin";

    public static final String ALN_URL = "/fetch_aln.pl?";

    public static final String IDS_URL = "/fetch_ids.pl?";

    public static final String TREE_URL = "/fetch_tree.pl?";

    public static final String DATASET_URL = "dataset=frb";

    public static final String DATA_DIR = "data";

    public static final String TREE_FILE = "trees.tgz";

    public static final String ALN_FILE = "aln.tgz";

    private Alignment[] mAlignment;

    private String[] mProteins;

    private boolean[] mIsParalog;

    private int[] mSubstitutions;

    private PTree mProteinTree;

    private int mFamily;

    private List<ATree> mAminoTrees;

    private boolean mHasParalogs;

    private boolean mProcessed;

    private int mNumberOfSpecies;

    private SpeciesTree mSpeciesTree;

    private String mMolecularFunction;

    public static Dataset createDataset(int family, boolean useFileSerialization, boolean locally, SpeciesTree speciesTree) throws Exception {
        Dataset ds = null;
        if (useFileSerialization) loadFromFile(family);
        if (ds == null) {
            ds = new Dataset(family);
            ds.setSpeciesTree(speciesTree);
            if (locally) ds.fetchDataLocally(); else ds.fetchData();
            if (useFileSerialization) serialize(ds);
            String mf = DAO.getMF(ds.getProteins());
            ds.setMolecularFunction(mf);
        }
        return ds;
    }

    public Dataset() {
        mNumberOfSpecies = -1;
    }

    private Dataset(int family) {
        mFamily = family;
        mNumberOfSpecies = -1;
    }

    public void fetchData() throws IOException {
        fetchIds();
        fetchAlignment();
        fetchTree();
        calculateSubstitutions();
    }

    public void fetchDataLocally() throws IOException {
        readIds();
        readAlignment();
        readTree();
        calculateSubstitutions();
    }

    private static Dataset loadFromFile(int family) throws IOException, ClassNotFoundException {
        File file = new File(DATA_DIR, "family" + family);
        if (file.exists()) {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
            try {
                return (Dataset) in.readObject();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static void serialize(Dataset ds) throws IOException {
        File file = new File(DATA_DIR, "family" + ds.getFamily());
        file.createNewFile();
        try {
            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file));
            out.writeObject(ds);
        } catch (IOException e) {
            file.delete();
            throw e;
        }
    }

    private void calculateSubstitutions() {
        mSubstitutions = new int[mAlignment.length];
        for (int j = 0; j < mAlignment.length; j++) {
            char p = mAlignment[j].ch(0);
            for (int i = 1; i < mProteins.length; i++) {
                if (p != mAlignment[j].ch(i)) {
                    mSubstitutions[j] = 1;
                    break;
                }
            }
        }
    }

    public boolean verify() {
        int bad = 0;
        int[] subs = new int[2];
        if (mAminoTrees != null) for (ATree t : mAminoTrees) {
            if (!t.verify(subs)) bad++;
        }
        System.out.print("Confirmed substitutions: " + subs[0] + "; Unconfirmed substitutions: " + subs[1]);
        System.out.print(". Inconsistent trees: " + bad + ". ");
        return bad == 0;
    }

    public int[] getSubstitutions(int firstIdx) {
        int len = 0;
        for (int j = firstIdx; j < mAlignment.length; j++) {
            if (mSubstitutions[j] != 0) len++;
        }
        int[] subs = new int[len];
        int k = 0;
        for (int j = firstIdx; j < mAlignment.length; j++) {
            if (mSubstitutions[j] != 0) subs[k++] = j;
        }
        return subs;
    }

    public int getNumberOfSpecies() {
        if (mNumberOfSpecies < 0) {
            Set species = new HashSet();
            for (int i = 0; i < mProteins.length; i++) {
                species.add(species(i));
            }
            mNumberOfSpecies = species.size();
        }
        return mNumberOfSpecies;
    }

    private void fetchIds() throws IOException {
        String urlString = BASE_URL + IDS_URL + DATASET_URL + "&family=" + mFamily;
        URL url = new URL(urlString);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        ArrayList<String> ids = new ArrayList<String>();
        List<String> lines = getHTMLLines(in);
        for (String line : lines) {
            line = line.trim();
            if (line.length() == 0) continue;
            if (line.contains("<b>")) continue;
            ids.add(line);
        }
        in.close();
        processIds(ids);
    }

    private void readIds() throws IOException {
        File file = new File("data", "clusters.tbl");
        BufferedReader in = new BufferedReader(new FileReader(file));
        ArrayList<String> ids = new ArrayList<String>();
        String marker = ">" + mFamily;
        boolean found = false;
        while (in.ready()) {
            String line = in.readLine();
            if (line == null) break;
            if (line.length() == 0) continue;
            if (line.charAt(0) != '>') continue;
            if (!line.startsWith(marker)) continue;
            StringTokenizer st = new StringTokenizer(line);
            if (!marker.equals(st.nextToken())) continue;
            found = true;
            break;
        }
        if (!found) {
            processIds(ids);
            return;
        }
        while (in.ready()) {
            String line = in.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.startsWith(">")) break;
            if (line.length() == 0) continue;
            ids.add(line);
        }
        in.close();
        processIds(ids);
    }

    private void processIds(ArrayList<String> ids) {
        int n = ids.size();
        mProteins = new String[n];
        mProteins = ids.toArray(mProteins);
        if (n > 512) {
            mHasParalogs = true;
            return;
        }
        mIsParalog = new boolean[n];
        for (int i = 0; i < n; i++) {
            String str1 = stripNumbers(mProteins[i]);
            for (int j = i + 1; j < n; j++) {
                String str2 = stripNumbers(mProteins[j]);
                if (str1.equals(str2)) {
                    mIsParalog[i] = true;
                    mIsParalog[j] = true;
                    mHasParalogs = true;
                }
            }
        }
    }

    private static String stripNumbers(String str) {
        char[] chars = str.toCharArray();
        int len = 0;
        for (int i = 0; i < chars.length; i++) {
            char ch = chars[i];
            if (!Character.isDigit(ch)) len = i;
        }
        return new String(chars, 0, len);
    }

    public SpeciesTreeNode species(int pId) {
        String species;
        String proteinName = mProteins[pId];
        int underscore = proteinName.indexOf('_');
        if (underscore > 0) species = proteinName.substring(0, underscore); else if (proteinName.startsWith("FBgn")) species = "dmel"; else throw new IllegalArgumentException("Can not determine species for protein: " + proteinName);
        for (BaseTreeNode n : mSpeciesTree.getNodes()) {
            SpeciesTreeNode sn = (SpeciesTreeNode) n;
            if (species.equals(sn.species)) return sn;
        }
        throw new IllegalStateException("Species: " + species + " failed validation");
    }

    private void processAlignment(BufferedReader in) throws IOException {
        StringBuffer bufs[] = new StringBuffer[mProteins.length];
        for (int j = 0; j < bufs.length; j++) bufs[j] = new StringBuffer();
        while (in.ready()) {
            String line = in.readLine();
            if (line == null) break;
            line = line.trim();
            int i = -1;
            StringTokenizer st = new StringTokenizer(line);
            if (st.hasMoreTokens()) {
                String protein = st.nextToken();
                for (int j = 0; j < mProteins.length; j++) {
                    if (mProteins[j].equals(protein)) {
                        i = j;
                        break;
                    }
                }
            }
            if (i < 0) continue;
            line = line.substring(mProteins[i].length() + 1);
            line = line.trim();
            bufs[i].append(line);
        }
        int len = bufs[0].length();
        for (int j = 1; j < bufs.length; j++) {
            if (bufs[j].length() != len) throw new IllegalStateException("Different length: " + " len[" + mProteins[j] + "] = " + bufs[j].length() + "; len[" + mProteins[0] + "] = " + bufs[0].length());
        }
        mAlignment = new Alignment[len];
        char[] buf = new char[mProteins.length];
        for (int k = 0; k < len; k++) {
            mAlignment[k] = new Alignment();
            mAlignment[k].setColumn(k);
            for (int j = 0; j < mProteins.length; j++) {
                buf[j] = bufs[j].charAt(k);
            }
            mAlignment[k].setAlignment(new String(buf));
        }
    }

    private void fetchAlignment() throws IOException {
        String urlString = BASE_URL + ALN_URL + DATASET_URL + "&family=" + mFamily;
        URL url = new URL(urlString);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        processAlignment(in);
    }

    private void fetchTree() throws IOException {
        String urlString = BASE_URL + TREE_URL + DATASET_URL + "&family=" + mFamily;
        URL url = new URL(urlString);
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
        String toParse = in.readLine();
        while (in.ready()) {
            String add = in.readLine();
            if (add == null) break;
            toParse += add;
        }
        if (toParse != null && !toParse.startsWith("No tree available")) mProteinTree = new PTree(this, toParse);
    }

    private void readAlignment() throws IOException {
        String entry = "aln/cluster_" + mFamily + ".phy";
        InputStream inStream = getTGZEntryStream(DATA_DIR, ALN_FILE, entry, null);
        if (inStream == null) {
            mAlignment = new Alignment[0];
            return;
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(inStream));
        processAlignment(in);
    }

    private void readTree() throws IOException {
        String entry = "trees/cluster_" + mFamily + ".newick";
        String toParse = getTGZEntry(DATA_DIR, TREE_FILE, entry);
        if (toParse != null && toParse.trim().length() != 0 && !toParse.startsWith("No tree available")) mProteinTree = new PTree(this, toParse);
    }

    private static InputStream getTGZEntryStream(String dir, String file, String entry, long[] len) throws IOException {
        File zipFile = new File(dir, file);
        FileInputStream zip = new FileInputStream(zipFile);
        GZIPInputStream gz = new GZIPInputStream(zip);
        TarInputStream tar = new TarInputStream(gz);
        TarEntry te = tar.getNextEntry();
        for (; te != null; te = tar.getNextEntry()) {
            if (te.getName().equals(entry)) break;
        }
        if (te == null) return null;
        if (len != null) len[0] = te.getSize();
        return tar;
    }

    private static String getTGZEntry(String dir, String file, String entry) throws IOException {
        long[] llen = new long[1];
        InputStream tar = getTGZEntryStream(dir, file, entry, llen);
        if (llen[0] > Integer.MAX_VALUE) throw new IOException("Entry " + entry + " is too long: " + llen);
        int len = (int) llen[0];
        byte[] buf = new byte[len];
        int read = 0;
        while (read < len) {
            int l = tar.read(buf);
            if (l < 0) break;
            read += l;
        }
        return new String(buf);
    }

    private static List<String> getHTMLLines(BufferedReader in) throws IOException {
        ArrayList<String> lines = new ArrayList<String>();
        StringBuffer curline = new StringBuffer();
        while (in.ready()) {
            String text = in.readLine();
            if (text == null) break;
            String text2 = text.toLowerCase();
            for (int pos = 0; pos >= 0; ) {
                int br = text2.indexOf("<br>", pos);
                String sub;
                if (br < 0) {
                    sub = text.substring(pos);
                    curline.append(sub);
                    break;
                }
                sub = text.substring(pos, br);
                curline.append(sub);
                pos = br + "<br>".length();
                lines.add(curline.toString());
                curline = new StringBuffer();
            }
        }
        if (curline.length() > 0) lines.add(curline.toString());
        return lines;
    }

    public String[] getProteins() {
        return mProteins;
    }

    public void setProteins(String[] proteins) {
        mProteins = proteins;
    }

    public char[] getAlignment(int column) {
        return mAlignment[column].toCharArray();
    }

    public int getAlignmentLength() {
        return mAlignment.length;
    }

    @OneToMany(targetEntity = Alignment.class, mappedBy = "dataset", cascade = CascadeType.REMOVE, fetch = FetchType.EAGER)
    public Alignment[] getAlignment() {
        return mAlignment;
    }

    public void setAlignment(Alignment[] alignment) {
        mAlignment = alignment;
    }

    @Id
    public int getFamily() {
        return mFamily;
    }

    public void setFamily(int family) {
        mFamily = family;
    }

    public SpeciesTree getSpeciesTree() {
        return mSpeciesTree;
    }

    public void setSpeciesTree(SpeciesTree speciesTree) {
        mSpeciesTree = speciesTree;
        if (mProteinTree == null) return;
        Map<String, SpeciesTreeNode> spMap = new HashMap<String, SpeciesTreeNode>();
        for (BaseTreeNode n : mSpeciesTree.getNodes()) spMap.put(n.name, (SpeciesTreeNode) n);
        for (BaseTreeNode n : mProteinTree.getNodes()) {
            PTreeNode node = (PTreeNode) n;
            if (node.species != null) node.species = spMap.get(node.species.name);
        }
    }

    public PTree getProteinTree() {
        return mProteinTree;
    }

    public boolean[] getIsParalog() {
        return mIsParalog;
    }

    public boolean isParalog(int i) {
        return mIsParalog[i];
    }

    public void setIsParalog(boolean[] isParalog) {
        mIsParalog = isParalog;
    }

    public boolean isHasParalogs() {
        return mHasParalogs;
    }

    public void setHasParalogs(boolean hasParalogs) {
        mHasParalogs = hasParalogs;
    }

    public void setHasParalogs() {
        mHasParalogs = false;
        for (boolean p : mIsParalog) if (p) {
            mHasParalogs = true;
            break;
        }
    }

    public String getMolecularFunction() {
        return mMolecularFunction;
    }

    public void setMolecularFunction(String molecularFunction) {
        mMolecularFunction = molecularFunction;
    }

    public boolean isProcessed() {
        return mProcessed;
    }

    public void setProcessed(boolean proccessed) {
        mProcessed = proccessed;
    }

    public void setProteinTree(PTree tree) {
        mProteinTree = tree;
    }

    @OneToMany(targetEntity = ATree.class, mappedBy = "parentDataset")
    public List<ATree> getAminoTrees() {
        return mAminoTrees;
    }

    public void setAminoTrees(List<ATree> aminoTrees) {
        mAminoTrees = aminoTrees;
    }

    public void addTree(ATree tree) {
        mAminoTrees.add(tree);
    }

    public ATree getTree(int column) {
        if (mAminoTrees == null || mAminoTrees.isEmpty()) return null;
        if (column < mAminoTrees.size()) {
            ATree t = mAminoTrees.get(column);
            if (t != null && t.getColumn() == column) return t;
        }
        for (ATree t : mAminoTrees) {
            if (t.getColumn() == column) return t;
        }
        return null;
    }

    public static void main1(String[] args) throws IOException {
        String out = getTGZEntry(DATA_DIR, ALN_FILE, args[0]);
        System.out.println("out = " + out);
    }

    public static void main(String[] args) throws Exception {
        int f = 1;
        int fidx = 150;
        for (int i = 0; i < args.length; i++) {
            if ("-f".equalsIgnoreCase(args[i])) f = Integer.parseInt(args[++i]); else if ("-fidx".equalsIgnoreCase(args[i])) fidx = Integer.parseInt(args[++i]); else throw new IllegalArgumentException("Unknown parameter: " + args[i]);
        }
        Dataset ds = Dataset.createDataset(f, false, true, null);
        String[] ids = ds.getProteins();
        System.out.println("IDs:");
        for (String id : ids) {
            System.out.println(id);
        }
        Alignment[] alignments = ds.getAlignment();
        System.out.println();
        System.out.println("Alignment:");
        for (int i = 0; i < ids.length; i++) {
            char[] buf = new char[alignments.length];
            for (int j = 0; j < alignments.length; j++) buf[j] = alignments[j].ch(i);
            String als = new String(buf);
            System.out.println(ids[i] + ": " + als);
            System.out.println();
        }
        int[] subs = ds.getSubstitutions(fidx);
        System.out.println("Substitutions:");
        for (int s : subs) {
            System.out.print(s + ", ");
        }
        DAO dao = DAO.createDAO();
        dao.saveDataset(ds);
    }
}
