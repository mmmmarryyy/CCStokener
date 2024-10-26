package org.htmlparser.tests.lexerTests;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.htmlparser.lexer.Page;
import org.htmlparser.tests.ParserTestCase;
import org.htmlparser.util.ParserException;

public class PageTests extends ParserTestCase {

    static {
        System.setProperty("org.htmlparser.tests.lexerTests.PageTests", "PageTests");
    }

    /**
     * The default charset.
     * This should be <code>ISO-8859-1</code>,
     * see RFC 2616 (http://www.ietf.org/rfc/rfc2616.txt?number=2616) section 3.7.1
     * Another alias is "8859_1".
     */
    public static final String DEFAULT_CHARSET = "ISO-8859-1";

    /**
     * Base URI for absolute URL tests.
     */
    static final String BASEURI = "http://a/b/c/d;p?q";

    /**
     * Page for absolute URL tests.
     */
    public static Page mPage;

    static {
        mPage = new Page();
        mPage.setBaseUrl(BASEURI);
    }

    /**
     * Test the third level page class.
     */
    public PageTests(String name) {
        super(name);
    }

    /**
     * Test initialization with a null value.
     */
    public void testNull() throws ParserException {
        try {
            new Page((URLConnection) null);
            assertTrue("null value in constructor", false);
        } catch (IllegalArgumentException iae) {
        }
        try {
            new Page((String) null);
            assertTrue("null value in constructor", false);
        } catch (IllegalArgumentException iae) {
        }
    }

    /**
     * Test initialization with a real value.
     */
    public void testURLConnection() throws ParserException, IOException {
        String link;
        URL url;
        link = "http://www.ibm.com/jp/";
        url = new URL(link);
        new Page(url.openConnection());
    }

    /**
     * Test initialization with non-existant URL.
     */
    public void testBadURLConnection() throws IOException {
        String link;
        URL url;
        link = "http://www.bigbogosity.org/";
        url = new URL(link);
        try {
            new Page(url.openConnection());
        } catch (ParserException pe) {
        }
    }

    public void test1() throws ParserException {
        assertEquals("test1 failed", "https:h", mPage.getAbsoluteURL("https:h"));
    }

    public void test2() throws ParserException {
        assertEquals("test2 failed", "http://a/b/c/g", mPage.getAbsoluteURL("g"));
    }

    public void test3() throws ParserException {
        assertEquals("test3 failed", "http://a/b/c/g", mPage.getAbsoluteURL("./g"));
    }

    public void test4() throws ParserException {
        assertEquals("test4 failed", "http://a/b/c/g/", mPage.getAbsoluteURL("g/"));
    }

    public void test5() throws ParserException {
        assertEquals("test5 failed", "http://a/g", mPage.getAbsoluteURL("/g"));
    }

    public void test6() throws ParserException {
        assertEquals("test6 failed", "http://g", mPage.getAbsoluteURL("//g"));
    }

    public void test7() throws ParserException {
        assertEquals("test7 strict failed", "http://a/b/c/?y", mPage.getAbsoluteURL("?y", true));
        assertEquals("test7 non-strict failed", "http://a/b/c/d;p?y", mPage.getAbsoluteURL("?y"));
    }

    public void test8() throws ParserException {
        assertEquals("test8 failed", "http://a/b/c/g?y", mPage.getAbsoluteURL("g?y"));
    }

    public void test9() throws ParserException {
        assertEquals("test9 failed", "https:h", mPage.getAbsoluteURL("https:h"));
    }

    public void test10() throws ParserException {
        assertEquals("test10 failed", "https:h", mPage.getAbsoluteURL("https:h"));
    }

    public void test11() throws ParserException {
        assertEquals("test11 failed", "http://a/b/c/g#s", mPage.getAbsoluteURL("g#s"));
    }

    public void test12() throws ParserException {
        assertEquals("test12 failed", "http://a/b/c/g?y#s", mPage.getAbsoluteURL("g?y#s"));
    }

    public void test13() throws ParserException {
        assertEquals("test13 failed", "http://a/b/c/;x", mPage.getAbsoluteURL(";x"));
    }

    public void test14() throws ParserException {
        assertEquals("test14 failed", "http://a/b/c/g;x", mPage.getAbsoluteURL("g;x"));
    }

    public void test15() throws ParserException {
        assertEquals("test15 failed", "http://a/b/c/g;x?y#s", mPage.getAbsoluteURL("g;x?y#s"));
    }

    public void test16() throws ParserException {
        assertEquals("test16 failed", "http://a/b/c/", mPage.getAbsoluteURL("."));
    }

    public void test17() throws ParserException {
        assertEquals("test17 failed", "http://a/b/c/", mPage.getAbsoluteURL("./"));
    }

    public void test18() throws ParserException {
        assertEquals("test18 failed", "http://a/b/", mPage.getAbsoluteURL(".."));
    }

    public void test19() throws ParserException {
        assertEquals("test19 failed", "http://a/b/", mPage.getAbsoluteURL("../"));
    }

    public void test20() throws ParserException {
        assertEquals("test20 failed", "http://a/b/g", mPage.getAbsoluteURL("../g"));
    }

    public void test21() throws ParserException {
        assertEquals("test21 failed", "http://a/", mPage.getAbsoluteURL("../.."));
    }

    public void test22() throws ParserException {
        assertEquals("test22 failed", "http://a/g", mPage.getAbsoluteURL("../../g"));
    }

    public void test23() throws ParserException {
        assertEquals("test23 failed", "http://a/g", mPage.getAbsoluteURL("../../../g"));
    }

    public void test24() throws ParserException {
        assertEquals("test24 failed", "http://a/g", mPage.getAbsoluteURL("../../../../g"));
    }

    public void test25() throws ParserException {
        assertEquals("test25 failed", "http://a/./g", mPage.getAbsoluteURL("/./g"));
    }

    public void test26() throws ParserException {
        assertEquals("test26 failed", "http://a/../g", mPage.getAbsoluteURL("/../g"));
    }

    public void test27() throws ParserException {
        assertEquals("test27 failed", "http://a/b/c/g.", mPage.getAbsoluteURL("g."));
    }

    public void test28() throws ParserException {
        assertEquals("test28 failed", "http://a/b/c/.g", mPage.getAbsoluteURL(".g"));
    }

    public void test29() throws ParserException {
        assertEquals("test29 failed", "http://a/b/c/g..", mPage.getAbsoluteURL("g.."));
    }

    public void test30() throws ParserException {
        assertEquals("test30 failed", "http://a/b/c/..g", mPage.getAbsoluteURL("..g"));
    }

    public void test31() throws ParserException {
        assertEquals("test31 failed", "http://a/b/g", mPage.getAbsoluteURL("./../g"));
    }

    public void test32() throws ParserException {
        assertEquals("test32 failed", "http://a/b/c/g/", mPage.getAbsoluteURL("./g/."));
    }

    public void test33() throws ParserException {
        assertEquals("test33 failed", "http://a/b/c/g/h", mPage.getAbsoluteURL("g/./h"));
    }

    public void test34() throws ParserException {
        assertEquals("test34 failed", "http://a/b/c/h", mPage.getAbsoluteURL("g/../h"));
    }

    public void test35() throws ParserException {
        assertEquals("test35 failed", "http://a/b/c/g;x=1/y", mPage.getAbsoluteURL("g;x=1/./y"));
    }

    public void test36() throws ParserException {
        assertEquals("test36 failed", "http://a/b/c/y", mPage.getAbsoluteURL("g;x=1/../y"));
    }

    public void test37() throws ParserException {
        assertEquals("test37 failed", "http://a/b/c/g?y/./x", mPage.getAbsoluteURL("g?y/./x"));
    }

    public void test38() throws ParserException {
        assertEquals("test38 failed", "http://a/b/c/g?y/../x", mPage.getAbsoluteURL("g?y/../x"));
    }

    public void test39() throws ParserException {
        assertEquals("test39 failed", "http://a/b/c/g#s/./x", mPage.getAbsoluteURL("g#s/./x"));
    }

    public void test40() throws ParserException {
        assertEquals("test40 failed", "http://a/b/c/g#s/../x", mPage.getAbsoluteURL("g#s/../x"));
    }

    public void test41() throws ParserException {
        assertEquals("test41 failed", "http://a/b/c/g", mPage.getAbsoluteURL("http:g"));
    }
}
