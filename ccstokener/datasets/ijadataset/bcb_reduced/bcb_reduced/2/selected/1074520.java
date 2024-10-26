package oqube.patchwork.report.source;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import oqube.patchwork.graph.BasicBlock;
import oqube.patchwork.graph.ControlGraph;
import oqube.patchwork.graph.ControlGraphBuilder;
import oqube.patchwork.report.CoverageListener;
import com.uwyn.jhighlight.renderer.Renderer;
import com.uwyn.jhighlight.renderer.XhtmlRendererFactory;

/**
 * A utility class that constructs a map from class names to source URLs using a
 * sourcepath list and that can highlight source code for executed blocks. This
 * class can be used if source code rendering is needed.
 * 
 * @author nono
 * 
 */
public class SourceMapper implements CoverageListener {

    private Logger log = Logger.getLogger(SourceMapper.class.getName());

    private static final String EOL = System.getProperty("line.separator");

    private final Renderer defaultRenderer = XhtmlRendererFactory.getRenderer("java");

    private File basedir = new File(".");

    private ControlGraphBuilder graphBuilder;

    private String urlPrefix = "";

    private Map<String, BitSet> hltLines = new HashMap<String, BitSet>();

    private SourceToURL sourceToURL;

    private Map<String, SourceLines> sourceLines = new HashMap<String, SourceLines>();

    private Map<String, LineAndBlocks> methodLines = new HashMap<String, LineAndBlocks>();

    private int hit;

    public int hit() {
        return hit;
    }

    /**
   * Produce xhtml file for given name. Assume name is a dot or slash separated
   * list of components tha are transformed into directories.
   * 
   * @param name
   *          the base name of file to highlight.
   * @return a link to the newly created file. Maybe null in case of error.
   */
    public String highlight(String name) {
        try {
            URL url = sourceToURL.get(name);
            SourceLines lines = sourceLines.get(name);
            if (url == null || lines == null) return null;
            InputStream is = url.openStream();
            String fname = basedir.getPath() + File.separator + name + ".html";
            File dir = new File(fname.substring(0, fname.lastIndexOf(File.separatorChar)));
            if (!dir.exists()) dir.mkdirs();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            Renderer renderer = makeRenderer(name);
            log.fine("using renderer " + renderer.getClass().getName());
            renderer.highlight(name, is, bos, "UTF-8", false);
            addCoveredLinesFormat(name, fname, bos.toByteArray(), lines);
            log.fine("done highlighting, returning " + urlPrefix + name + ".html");
            return urlPrefix + name + ".html";
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Renderer makeRenderer(String name) {
        int dot = name.lastIndexOf('.');
        String suf = dot == -1 ? "java" : name.substring(dot + 1);
        Renderer r = XhtmlRendererFactory.getRenderer(suf);
        if (r == null) return defaultRenderer; else return r;
    }

    void addCoveredLinesFormat(String name, String fname, byte[] bs, SourceLines lines) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(bs)));
        PrintStream pos = new PrintStream(new FileOutputStream(fname));
        String line;
        int ln = 2;
        Pattern pat = Pattern.compile("^<span class=\"java_.*");
        while ((line = br.readLine()) != null) {
            if (pat.matcher(line).matches()) {
                int cov = lines.covered(ln);
                int max = lines.blocks(ln);
                String color = makeColor(cov, max);
                pos.append("<div style=\"background-color:" + color + ";\">").append(line).append(EOL).append("</div>");
                ln++;
            } else pos.append(line).append(EOL);
        }
        pos.flush();
        pos.close();
    }

    private String makeColor(int cov, int max) {
        if (max == 0) return "#fff";
        int r = 15 - ((cov * 15) / max);
        int g = ((cov * 15) / max);
        assert r + g == 15;
        return "#" + Integer.toHexString(r) + Integer.toHexString(g) + "0";
    }

    /**
   * @return Returns the log.
   */
    public Logger getLog() {
        return log;
    }

    /**
   * @param log
   *          The log to set.
   */
    public void setLog(Logger log) {
        this.log = log;
    }

    /**
   * Update coverage information for this source mapper. The given path lists is
   * used to highlight source code by translating executed blocks to their line
   * equivalent.
   * 
   * @param cp
   *          a Map<String, List<int[]>> instance from full method names to
   *          list of blocks.
   */
    public void update(Map cp) {
        for (Iterator i = cp.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry me = (Map.Entry) i.next();
            String method = (String) me.getKey();
            List blocks = (List) me.getValue();
            if (blocks.isEmpty()) continue;
            int dot = method.lastIndexOf('.');
            if (dot == -1) throw new IllegalArgumentException("Invalid method name " + method + ": Must be <class>.<method><signature>");
            int paren = method.lastIndexOf('(');
            if (paren == -1) throw new IllegalArgumentException("Invalid signature in " + method + ": Must be <class>.<method><signature>");
            String cln = method.substring(0, dot);
            String mn = method.substring(dot + 1, paren);
            String signature = method.substring(paren);
            try {
                ControlGraph cg = graphBuilder.createGraphForMethod(cln, mn, signature);
                Comparator<BasicBlock> comp = new Comparator<BasicBlock>() {

                    public int compare(BasicBlock bb1, BasicBlock bb2) {
                        int s1, s2, e1, e2;
                        s1 = bb1.getStartLine();
                        s2 = bb2.getStartLine();
                        e1 = bb1.getEndLine();
                        e2 = bb2.getEndLine();
                        return s1 < s2 ? -1 : s1 > s2 ? 1 : (e1 < e2 ? -1 : (e1 > e2 ? 1 : 0));
                    }
                };
                List<BasicBlock> l = cg.getBlocks();
                if (l.isEmpty()) continue;
                Collections.sort(l, comp);
                BitSet cov = (BitSet) hltLines.get(cln);
                if (cov == null) {
                    cov = new BitSet();
                    hltLines.put(cln, cov);
                }
                for (Iterator j = blocks.iterator(); j.hasNext(); ) {
                    int[] path = (int[]) j.next();
                    for (int k = 0; k < path.length; k++) {
                        BasicBlock bb = (BasicBlock) l.get(path[k] - 1);
                        for (int m = bb.getStartLine(); m < bb.getEndLine(); m++) {
                            cov.set(m - 1);
                        }
                    }
                }
            } catch (IOException e) {
                log.severe("Cannot construct graph for method " + method);
                e.printStackTrace();
            }
        }
    }

    /**
   * Online update of covered lines information. This method is inefficient and
   * need to be refactored.
   */
    public void update(int tid, String method, int block) {
        LineAndBlocks lines = methodLines.get(method);
        if (lines == null) {
            ControlGraph cg;
            try {
                cg = graphBuilder.createGraphForMethod(method);
                String sname = cg.getSourceFile();
                if (sname != null) {
                    SourceLines sl = sourceLines.get(sname);
                    if (sl == null) {
                        sl = new SourceLines();
                        sourceLines.put(sname, sl);
                    }
                    assert sl != null;
                    sl.put(method, lines = new LineAndBlocks(cg));
                    methodLines.put(method, lines);
                } else return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
        assert lines != null;
        hit++;
        lines.update(block);
    }

    /**
   * @return the graphBuilder
   */
    public ControlGraphBuilder getGraphBuilder() {
        return graphBuilder;
    }

    /**
   * @param graphBuilder
   *          the graphBuilder to set
   */
    public void setGraphBuilder(ControlGraphBuilder graphBuilder) {
        this.graphBuilder = graphBuilder;
    }

    /**
   * @return the sourceToURL
   */
    public SourceToURL getSourceToURL() {
        return sourceToURL;
    }

    /**
   * @param sourceToURL
   *          the sourceToURL to set
   */
    public void setSourceToURL(SourceToURL sourceToURL) {
        this.sourceToURL = sourceToURL;
    }

    /**
   * @return the basedir
   */
    public File getBasedir() {
        return basedir;
    }

    /**
   * @param basedir
   *          the basedir to set
   */
    public void setBasedir(File basedir) {
        this.basedir = basedir;
    }

    /**
   * @return the urlPrefix
   */
    public String getUrlPrefix() {
        return urlPrefix;
    }

    /**
   * @param urlPrefix
   *          the urlPrefix to set
   */
    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }
}
