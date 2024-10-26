package conga;

import conga.io.FormattedSource;
import conga.io.Source;
import conga.io.UrlRepo;
import conga.io.format.ConfigFormats;
import conga.io.format.DefaultXmlFormat;
import conga.param.Parameter;
import conga.param.ParameterImpl;
import conga.param.tree.ParamList;
import conga.param.tree.ParamListImpl;
import conga.param.tree.ReloadableTreeImpl;
import conga.param.tree.RootNode;
import conga.param.tree.TreeImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/** @author Justin Caballero */
public abstract class CongaTestCase extends Assertions {

    private static final String[] TEST_DATA = new String[] { "test0.xml", "test1.xml", "testConversion.xml", "test0-alt.xml" };

    protected void setUp() throws Exception {
        super.setUp();
        ConfigFormats.setDefaultFormat(ConfigFormats.XML);
    }

    protected void tearDown() throws Exception {
        ConfigFormats.setDefaultFormat(null);
    }

    protected static FormattedSource urlSource(int sourceNum) {
        return urlSource(resource(sourceNum));
    }

    protected FormattedSource urlSource(String resource) {
        return urlSource(resource(resource));
    }

    protected static FormattedSource urlSource(File file) {
        try {
            return new UrlRepo(file.toURL());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    protected static FormattedSource urlSource(URL url) {
        return new UrlRepo(url);
    }

    protected static URL resource(int sourceNum) {
        return CongaTestCase.class.getResource(TEST_DATA[sourceNum]);
    }

    protected URL resource(String path) {
        return getClass().getResource(path);
    }

    protected static TreeImpl newTree() {
        return new TreeImpl();
    }

    protected File copyToFile(URL url) {
        try {
            File temp = tempFile("xml");
            return copyToFile(url, temp);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected File tempFile(String suffix) throws IOException {
        File temp = File.createTempFile(getClass().getName(), suffix);
        temp.deleteOnExit();
        return temp;
    }

    protected static File copyToFile(URL url, File dest) {
        try {
            FileUtils.copyURLToFile(url, dest);
            return dest;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected static Parameter param(String name, String value) {
        return new ParameterImpl(name, value);
    }

    protected static Parameter param(Source source) {
        return new ParameterImpl("key", "value", source, null, 0);
    }

    protected static Parameter param() {
        return param("key", "value");
    }

    public static RootNode loadRootTree(int... treeNumbers) {
        RootNode root = new RootNode();
        for (int i : treeNumbers) {
            root.add(new ReloadableTreeImpl(urlSource(i)));
        }
        return root;
    }

    public static FormattedSource readTestTree(TreeImpl tree, int treeNumber) {
        FormattedSource source = urlSource(treeNumber);
        new DefaultXmlFormat().read(source, tree);
        return source;
    }

    public static RootConfiguration loadTestConfig(int... numbers) {
        ConfigurationBuilder b = ConfigurationBuilder.create();
        for (int number : numbers) {
            b.loadUrl(resource(number), ConfigFormats.XML);
        }
        return b.newConfig();
    }

    protected String slurpUrl(URL url) {
        try {
            return slurpStream(url.openStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected String slurpResource(String s) {
        return slurpStream(getClass().getResourceAsStream(s));
    }

    protected String slurpFile(File f) {
        try {
            return slurpStream(new FileInputStream(f));
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    protected String slurpStream(InputStream is) {
        BufferedReader r = new BufferedReader(new InputStreamReader(is));
        StringBuffer sb = new StringBuffer();
        String line;
        try {
            while ((line = r.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    protected static void writeToResource(String resource, String str, Class cls) throws IOException {
        FileWriter fw = new FileWriter(cls.getResource(resource).getFile());
        fw.write(str);
        fw.close();
    }

    protected void assertSimpleTree(TreeImpl tree, boolean ordered, String... kvPairs) {
        Validate.isTrue(kvPairs.length % 2 == 0);
        assertNotNull(tree);
        assertEquals(1, tree.children().size());
        List<Parameter> list = new ArrayList<Parameter>(((ParamList) tree.children().get(0)).getParams());
        for (int i = 0; i < kvPairs.length; i++) {
            String key = kvPairs[i];
            String value = kvPairs[++i];
            ParameterImpl p = new ParameterImpl(key, value);
            if (ordered) {
                assertEquals(p, list.get(0));
                list.remove(0);
            } else {
                assertTrue(list.remove(p));
            }
        }
        assertEquals(0, list.size());
    }

    protected TreeImpl newSimpleTree(String... kvPairs) {
        Validate.isTrue(kvPairs.length % 2 == 0);
        TreeImpl tree = newTree();
        ParamListImpl list = new ParamListImpl();
        for (int i = 0; i < kvPairs.length; i++) {
            String key = kvPairs[i];
            String value = kvPairs[++i];
            list.getBackingList().add(new ParameterImpl(key, value));
        }
        tree.add(list);
        return tree;
    }
}
