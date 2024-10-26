package net.sf.maple.resources.testing;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import junit.framework.TestCase;
import net.sf.maple.resources.Fragment;
import net.sf.maple.resources.InLocation;
import net.sf.maple.resources.InOutLocation;
import net.sf.maple.resources.Locations;
import net.sf.maple.resources.ResourceError;

public class Test_Resources extends TestCase {

    private static class MyLocations extends Locations {
    }

    private MyLocations locs;

    @Override
    public void setUp() {
        locs = new MyLocations();
    }

    private static void checkUrl(URL expected, URL actual) {
        assertEquals(expected.toString(), actual.toString());
    }

    private static List<String> read(InputStream is) {
        List<String> result = new ArrayList<String>();
        Scanner sc = new Scanner(is);
        while (sc.hasNext()) result.add(sc.nextLine());
        sc.close();
        return result;
    }

    public void testClassPath() {
        List<String> expected = read(Test_Resources.class.getResourceAsStream("b.txt"));
        Fragment f = Fragment.EMPTY.plus("net", "sf", "maple", "resources", "testing", "b.txt");
        List<String> lst = read(locs.cp.plus(f).openInput());
        assertEquals(expected, lst);
    }

    public void testFileSystem() throws IOException {
        Fragment f = Fragment.EMPTY;
        Fragment g = f.plus(System.getProperty("java.io.tmpdir"));
        Fragment h = f.plus("april", "1971", "data.txt");
        Fragment i = f.plus(g, h);
        InOutLocation iol = locs.fs.plus(i);
        PrintStream ps = new PrintStream(iol.openOutput());
        List<String> expected = new ArrayList<String>();
        expected.add("So I am stepping out this old brown shoe");
        expected.add("Maybe I'm in love with you");
        for (String s : expected) ps.println(s);
        ps.close();
        InLocation inRoot = locs.fs;
        List<String> lst = read(inRoot.plus(i).openInput());
        assertEquals(expected, lst);
        URL url = iol.toUrl();
        lst = read(url.openStream());
        assertEquals(expected, lst);
    }

    public void testWeb() throws MalformedURLException {
        Fragment a = Fragment.EMPTY.plus("www.google.com");
        checkUrl(new URL("http://www.google.com"), locs.web.plus(a).toUrl());
        Fragment b = a.plus("webhp");
        InLocation tempb = locs.web.plus(b);
        URL ub = tempb.toUrl();
        URL expected = new URL("http://www.google.com/webhp");
        checkUrl(expected, ub);
        Fragment c = Fragment.EMPTY.plus("www.google.com", "webhp");
        InLocation tempc = locs.web.plus(c);
        URL uc = tempc.toUrl();
        checkUrl(expected, uc);
    }

    public void testUp() {
        List<String> expected = read(Test_Resources.class.getResourceAsStream("b.txt"));
        Fragment f = Fragment.EMPTY.plus("net", "sf", "maple", "data");
        Fragment g = Fragment.EMPTY.plus("sql", Fragment.UP);
        Fragment h = Fragment.EMPTY.plus("..", "resources", "testing", "b.txt");
        List<String> lst = read(locs.cp.plus(f, g, h).openInput());
        assertEquals(expected, lst);
        Fragment w = g.plus("..", "..");
        try {
            w.getActualPath();
            fail("Too many UP error should occur here");
        } catch (ResourceError e) {
        }
    }

    public void testList() throws IOException {
        String dirName = this.getClass().getName();
        String tmp = System.getProperty("java.io.tmpdir");
        InOutLocation loc = locs.fs.plus(tmp).plus(dirName);
        for (int i = 0; i < 10; ++i) loc.plus("file" + i).openOutput().close();
        File[] fs = new File(tmp, dirName).listFiles();
        Set<String> expected = new HashSet<String>();
        for (File f : fs) expected.add(f.getName());
        Set<String> actual = new HashSet<String>();
        Fragment[] frags = loc.list();
        for (Fragment frag : frags) {
            InLocation il = loc.plus(frag);
            actual.add(il.getPath().getLast().toString());
        }
        assertEquals(expected.size(), actual.size());
        expected.removeAll(actual);
        assertEquals(0, expected.size());
    }

    public void testNet() {
        String dirName = this.getClass().getName();
        String tmp = System.getProperty("java.io.tmpdir");
        Fragment f = Fragment.EMPTY.plus(tmp, dirName, "testnet.txt");
        PrintStream ps = new PrintStream(locs.fs.plus(f).openOutput());
        ps.println("I think I'm gonna be sad");
        ps.println("I think It's the day");
        ps.close();
        List<String> expected = read(locs.fs.plus(f).openInput());
        InLocation local = locs.net.plus("localhost");
        InLocation file = local.plus(f);
        List<String> actual = read(file.openInput());
        assertEquals(expected, actual);
    }

    public void testSort() {
        InLocation a = locs.fs.plus("a", "b", "c");
        InLocation b = locs.fs.plus("y", "z");
        InLocation c = locs.web.plus("www.google.com", "my");
        InLocation d = locs.web.plus("www.google.com");
        InLocation e = locs.net.plus("localhost", "y", "z");
        List<InLocation> expected = new ArrayList<InLocation>();
        expected.add(b);
        expected.add(a);
        expected.add(e);
        expected.add(d);
        expected.add(c);
        List<InLocation> actual = new ArrayList<InLocation>();
        actual.add(a);
        actual.add(b);
        actual.add(c);
        actual.add(d);
        actual.add(e);
        Collections.sort(actual);
        assertEquals(expected, actual);
        InLocation one = locs.fs.plus("a", "b", "c");
        InLocation two = locs.net.plus("a", "b", "c");
        InLocation three = locs.net.plus("a", "b", "c");
        assertTrue(one.compareTo(two) != 0);
        assertTrue(one.compareTo(three) != 0);
        assertTrue(two.compareTo(three) == 0);
    }

    public void testTmp() {
        Fragment f = Fragment.EMPTY.plus("a", "b", "c");
        InOutLocation one = locs.tmp.plus(f);
        InOutLocation two = locs.tmp.plus(f);
        List<String> expected = read(one.openInput());
        expected.add("Close your eyes and I'll kiss you");
        expected.add("tomorrow I'll miss you");
        PrintStream ps = new PrintStream(one.openOutput());
        for (String s : expected) ps.println(s);
        ps.close();
        ps = new PrintStream(two.openOutput());
        ps.println("Can't buy me love");
        ps.close();
        List<String> actual = read(one.openInput());
        assertEquals(expected, actual);
    }

    public static void main(String[] args) {
        Test_Resources inst = new Test_Resources();
        inst.setUp();
        inst.testSort();
    }
}
