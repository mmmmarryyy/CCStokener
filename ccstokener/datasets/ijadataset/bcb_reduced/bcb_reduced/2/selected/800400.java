package oxygen.tool.classcompatibilityinspector;

import java.io.FileWriter;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

public class CCITool {

    public static final String FILENAME_PREFIX = "classcompatibilityinspector-file-";

    private static CCITool singleton = null;

    private static String[] defaultMatchNoneOf = new String[] { "java.*", "javax.*" };

    private String[] matchNoneOf = null;

    private CCIHandler[] classInspectorHdlrs;

    /**
   * Look on the system classpath, for all the 
   * numerically named files starting with a specified prefix
   * When the numbers stop increasing sequentially from 0, then 
   * we are done. 
   * So it should find say for example
   * classcompatibilityinspector-file-0.xml, classcompatibilityinspector-file-1.xml
   */
    private CCITool() throws Exception {
        List cihList = new ArrayList();
        ClassLoader cl = getClass().getClassLoader();
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            InputStream is = cl.getResourceAsStream(FILENAME_PREFIX + i + ".xml");
            if (is == null) break;
            CCIUtils.say("loading a CCIHandler ...");
            CCIHandler cih = new CCIHandler(is);
            cihList.add(cih);
            CCIUtils.say("done loading a CCIHandler: " + cih);
        }
        classInspectorHdlrs = (CCIHandler[]) cihList.toArray(new CCIHandler[0]);
        CCIUtils.say("loaded CCIHandlers: " + classInspectorHdlrs.length);
    }

    public void setIgnoreMatches(String[] regexes) {
        matchNoneOf = regexes;
    }

    /** 
   * does the work of inspectClassPath, returning a 
   * map of URL to CCIResults.
   * Each URL points to a Class location on the classpath, 
   * and the mapping points to a CCIResults
   * which has all the issues found while inspecting.
   * This map also contains all the empty results.
   */
    public Map inspectPath(String classpath, String indexes) throws Exception {
        Map map = new HashMap();
        List list = new ArrayList();
        CCIHandler[] hdlrs = classInspectorHdlrs;
        if (indexes != null) {
            hdlrs = getSubsetForIndexes(indexes);
        }
        for (int i = 0; i < hdlrs.length; i++) {
            CCIHandler cih = hdlrs[i];
            list.add(cih.getClassPrefixes());
        }
        String[] prefixes = CCIUtils.mergeStringArrs(list);
        CCIUtils.say("CI inspectPath using prefixes: " + Arrays.asList(prefixes));
        URL[] urls = CCIUtils.extractAllURLsInClasspath(classpath);
        String[] matchOneOf = new String[prefixes.length];
        for (int i = 0; i < matchOneOf.length; i++) {
            matchOneOf[i] = prefixes[i] + ".*";
        }
        CCIIntrospector ccii = new CCIIntrospector(matchOneOf, matchNoneOf);
        for (int i = 0; i < urls.length; i++) {
            InputStream is = urls[i].openStream();
            String[] refApis = ccii.introspectClassAndGetReferencedAPIs(is);
            is.close();
            CCIResults cir = new CCIResults();
            for (int i2 = 0; i2 < hdlrs.length; i2++) {
                CCIHandler cih = hdlrs[i2];
                CCIResults cir1 = cih.getRefs(refApis);
                cir = CCIResults.merge(cir, cir1);
            }
            map.put(urls[i], cir);
        }
        return map;
    }

    private CCIHandler[] getSubsetForIndexes(String indexList) {
        ArrayList list = new ArrayList();
        StringTokenizer stz = new StringTokenizer(indexList, " ,.-");
        while (stz.hasMoreTokens()) {
            list.add(stz.nextToken().trim());
        }
        CCIHandler[] hdlrs = new CCIHandler[list.size()];
        for (int i = 0; i < hdlrs.length; i++) {
            hdlrs[i] = classInspectorHdlrs[Integer.parseInt((String) list.get(i))];
        }
        return hdlrs;
    }

    /**
   * returns an instance of a class inspector to use.
   * We always return a single instance.
   */
    public static CCITool getInspector() throws Exception {
        if (singleton == null) {
            singleton = new CCITool();
        }
        return singleton;
    }

    public static void main(String[] args) throws Exception {
        ArrayList matchNoneOfList = new ArrayList();
        String indexes = null;
        String classpath = null;
        String resultfile = "results.html";
        boolean doHtml = true;
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-indexes")) {
                indexes = args[++i];
            } else if (args[i].equals("-classpath")) {
                classpath = args[++i];
            } else if (args[i].equals("-resultfile")) {
                resultfile = args[++i];
            } else if (args[i].equals("-ignorematch")) {
                matchNoneOfList.add(args[++i]);
            } else if (args[i].equals("-ignorematch")) {
                matchNoneOfList.add(args[++i]);
            } else if (args[i].equals("-defaultignorematches")) {
                matchNoneOfList.addAll(Arrays.asList(defaultMatchNoneOf));
            } else if (args[i].equals("-text")) {
                resultfile = "results.txt";
                doHtml = false;
            }
        }
        CCITool tool = new CCITool();
        String[] matchNoneOf22 = (String[]) matchNoneOfList.toArray(new String[0]);
        tool.setIgnoreMatches(matchNoneOf22);
        Map m = tool.inspectPath(classpath, indexes);
        System.out.println("Map results size: " + m.size());
        PrintWriter pw = new PrintWriter(new FileWriter(resultfile));
        if (doHtml) {
            new CCIResultHTMLWriter(m, pw);
        } else {
            new CCIResultTextWriter(m, pw);
        }
        pw.close();
    }
}
