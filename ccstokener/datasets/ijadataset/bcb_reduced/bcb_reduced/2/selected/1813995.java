package jezuch.utils.ini;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test(groups = { "jezuch.utils" })
public final class IniTest {

    private INIFile testIni;

    @Test(groups = { "jezuch.utils.init" })
    public void initIni() {
        testIni = new INIFile();
        testIni.add(new INISection("blah"));
        testIni.add(new INISection("ple"));
        testIni.add(new INISection("bul"));
        testIni.add(new INISection("sru"));
        testIni.add(new INISection("tutu"));
        assert testIni.size() == 5;
        assert testIni.contains(new INISection("blah"));
        assert testIni.contains(new INISection("ple"));
        assert testIni.contains(new INISection("bul"));
        assert testIni.contains(new INISection("sru"));
        assert testIni.contains(new INISection("tutu"));
        INIFile iniCopy = new INIFile(testIni);
        assert iniCopy.removeAll(Arrays.asList(new INISection("ple"), new INISection("sru"), new INISection("blah")));
        assert iniCopy.size() == 2;
        assert iniCopy.containsAll(Arrays.asList(new INISection("bul"), new INISection("tutu")));
        assert new LinkedList<INISection>(iniCopy).equals(Arrays.asList(new INISection("bul"), new INISection("tutu")));
        assert iniCopy.addAll(Arrays.asList(new INISection("ple"), new INISection("sru"), new INISection("blah"), new INISection("bul")));
        assert iniCopy.equals(testIni);
        assert iniCopy.retainAll(Arrays.asList(new INISection("ple"), new INISection("sru"), new INISection("blah")));
        assert iniCopy.size() == 3;
        assert iniCopy.containsAll(Arrays.asList(new INISection("ple"), new INISection("sru"), new INISection("blah")));
        assert new LinkedList<INISection>(iniCopy).equals(Arrays.asList(new INISection("ple"), new INISection("sru"), new INISection("blah")));
        testIni.getSectionNamesMap().get("blah").put("key1", "value1");
        testIni.getSectionNamesMap().get("blah").put("key2", "value2");
        testIni.getSectionNamesMap().get("bul").put("asdf", "ghjk");
        testIni.getSectionNamesMap().get("sru").put("foo", "bar");
    }

    public void iniRead() throws IOException, ParseException {
        Reader reader = new InputStreamReader(getClass().getResource("valid-1.ini").openStream());
        try {
            new INIFile(reader);
        } finally {
            reader.close();
        }
        reader = new InputStreamReader(getClass().getResource("valid-2.ini").openStream());
        try {
            assert testIni.equals(new INIFile(reader));
        } finally {
            reader.close();
        }
    }

    @DataProvider(name = "invalid-inis")
    public Object[][] invalidInisFromFile() {
        ArrayList<Object[]> list = new ArrayList<Object[]>();
        int n = 1;
        URL url;
        while ((url = getClass().getResource("invalid-" + (n++) + ".ini")) != null) list.add(new Object[] { url });
        return list.toArray(new Object[list.size()][]);
    }

    @Test(groups = { "jezuch.utils" }, dependsOnGroups = { "jezuch.utils.init" }, expectedExceptions = ParseException.class, dataProvider = "invalid-inis")
    public void invalidIni(URL url) throws IOException, ParseException {
        Reader reader = new InputStreamReader(url.openStream());
        try {
            new INIFile(reader);
        } finally {
            reader.close();
        }
    }
}
