package net.sourceforge.websnaptool.i.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.parser.HtmlParser;
import org.lobobrowser.html.test.SimpleUserAgentContext;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 *  WebSnap: 缃戦〉蹇収
 *  
 * User: Muwang Zheng
 * Date: 2010-6-12
 * Time: 涓嬪崍02:25:59
 * 
 */
public class WebSnap {

    /**
	 * log
	 */
    private static final Log LOG = LogFactory.getLog(WebSnap.class);

    /**
	 * 分页数
	 */
    private static int MAXPAGE_DEFAULT = 40;

    /**
	 * 分页数
	 */
    private static int MAXPAGE = MAXPAGE_DEFAULT;

    /**
	 * XPATH
	 */
    public static final XPath XPATH = XPathFactory.newInstance().newXPath();

    /**
	 * 
	 */
    private final Map<String, String> URLMAP = new HashMap<String, String>();

    /**
	 * 
	 */
    private String uri = "http://esf.xmhouse.com/rent/t1/l2/b/u/p800-1000/a-/d/o/k/3502030000/1.html";

    /**
	 * 
	 */
    private String pathElement = "/html/body/div[2]/div/div/div[3]/dl[2]/dd";

    /**
	 * 
	 */
    private String pathNextPage = "/html/body/div[2]/div/div/div[4]/ul/li//a[@class='disOn']";

    /**
	 * baseUrl
	 */
    private String baseUrl;

    /**
	 * 妫�祴鍏冪礌鐨刄RL
	 */
    private String checkXPath = "li/a[starts-with(@href,'/view/2010/06/12/')]";

    /**
	 * 鐩綍urlXPath
	 */
    private String targetUrlXpath = "li/a";

    /**
	 * 鍚嶇О鍦板潃
	 */
    private String namePath;

    /**
	 * 椤甸潰绱㈠紩
	 */
    private int pageIndex = 0;

    /**
	 * 鏋勯�
	 */
    public WebSnap() {
        super();
    }

    /**
	 * @param uri
	 * @param pathNextPage
	 * @param pathElement
	 * @param checkXPath
	 * @param targetUrlXpath
	 */
    public WebSnap(String uri, String pathNextPage, String pathElement, String checkXPath, String targetUrlXpath) {
        super();
        this.uri = uri;
        this.pathElement = pathElement;
        this.pathNextPage = pathNextPage;
        this.checkXPath = checkXPath;
        this.targetUrlXpath = targetUrlXpath;
        WebSnap.MAXPAGE = WebSnap.MAXPAGE_DEFAULT;
        this.buildBaseUrl(this.getUri());
    }

    /**
	 * @param uri
	 * @param pathNextPage
	 * @param pathElement
	 * @param checkXPath
	 * @param targetUrlXpath
	 * @param page
	 */
    public WebSnap(String uri, String pathNextPage, String pathElement, String checkXPath, String targetUrlXpath, Integer page) {
        super();
        this.uri = uri;
        this.pathElement = pathElement;
        this.pathNextPage = pathNextPage;
        this.checkXPath = checkXPath;
        this.targetUrlXpath = targetUrlXpath;
        WebSnap.MAXPAGE = null == page ? WebSnap.MAXPAGE_DEFAULT : page;
        this.buildBaseUrl(this.getUri());
    }

    /**
	 * @param uri
	 * @param pathNextPage
	 * @param pathElement
	 * @param checkXPath
	 * @param targetUrlXpath
	 */
    public WebSnap(String uri, String pathNextPage, String pathElement, String checkXPath, String targetUrlXpath, String namePath) {
        super();
        this.uri = uri;
        this.pathElement = pathElement;
        this.pathNextPage = pathNextPage;
        this.checkXPath = checkXPath;
        this.targetUrlXpath = targetUrlXpath;
        this.namePath = namePath;
        WebSnap.MAXPAGE = WebSnap.MAXPAGE_DEFAULT;
        this.buildBaseUrl(this.getUri());
    }

    /**
	 * 鑾峰彇url
	 * @param href
	 * @return url
	 */
    private String getFullPath(String href) {
        if (href != null && !"".equals(href) && !this.isFullPath(href)) {
            return this.getBaseUrl() + href;
        }
        return href;
    }

    /**
	 * @param href
	 * @return
	 */
    private boolean isFullPath(String href) {
        if (null == href) return false;
        return href.indexOf("http") == 0;
    }

    public void buildBaseUrl(String uri) {
        URL url;
        try {
            url = new URL(uri);
            this.setBaseUrl(url.getProtocol() + "://" + url.getHost() + (url.getPort() > 0 ? ":" + url.getPort() : "") + "");
        } catch (MalformedURLException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static String genBaseUrl(String uri) {
        URL url;
        try {
            url = new URL(uri);
            return (url.getProtocol() + "://" + url.getHost() + (url.getPort() > 0 ? ":" + url.getPort() : "") + "");
        } catch (MalformedURLException e) {
            LOG.error(e.getMessage(), e);
        }
        return uri;
    }

    /**
	 * @param uri
	 * @return
	 */
    public static Document buildDocument(String uri) {
        Logger.getLogger("org.lobobrowser").setLevel(Level.WARNING);
        InputStream in = null;
        try {
            UserAgentContext uacontext = new SimpleUserAgentContext();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            URL url = new URL(uri);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0;) ");
            in = connection.getInputStream();
            Document document = builder.newDocument();
            String charset = org.lobobrowser.util.Urls.getCharset(connection);
            if (null != charset) {
                if (charset.toUpperCase().indexOf("ISO") >= 0) {
                    charset = "UTF-8";
                }
            }
            Reader reader = new InputStreamReader(in, charset);
            HtmlParser parser = new HtmlParser(uacontext, document);
            parser.parse(reader);
            return document;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        } finally {
            if (null != in) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.error(e.getMessage(), e);
                }
                in = null;
            }
        }
        return null;
    }

    /**
	 */
    public void reviewPage() {
        String uri = this.getUri();
        if (null == uri || "".equals(uri) || URLMAP.get(this.getUri()) != null || this.getPageIndex() > WebSnap.MAXPAGE) {
            return;
        }
        this.setPageIndex(this.getPageIndex() + 1);
        LOG.info("review:" + uri);
        try {
            Document document = this.buildDocument(uri);
            if (null != document) {
                boolean f = findPages(document);
                if (f) {
                    reviewNextPage(document);
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
	 * @param document
	 * @return
	 * @throws Exception
	 */
    private boolean findPages(Document document) throws Exception {
        NodeList nodeList = (NodeList) XPATH.evaluate(this.getPathElement(), document, XPathConstants.NODESET);
        if (nodeList == null || nodeList.getLength() == 0) {
            return false;
        }
        for (int i = 0; null != nodeList && i < nodeList.getLength(); i++) {
            Element element = (Element) nodeList.item(i);
            NodeList cNodeList = (NodeList) XPATH.evaluate(this.getCheckXPath(), element, XPathConstants.NODESET);
            if (null != cNodeList && cNodeList.getLength() > 0) {
                String href = (String) XPATH.evaluate(this.getTargetUrlXpath(), element, XPathConstants.STRING);
                String name = null;
                if (this.getNamePath() != null) {
                    name = (String) XPATH.evaluate(this.getNamePath(), element, XPathConstants.STRING);
                }
                if (null != href && !"".equals(href)) {
                    String url = this.getFullPath(href);
                    try {
                        InputHelper.insetNewHouseData(url);
                    } catch (Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
        }
        return true;
    }

    /**
	 * 鏌ョ湅鎸囧畾鍦板潃鐨勯〉闈㈠唴瀹�
	 * 
	 * @param url
	 *            鎸囧畾鐨勫湴鍧�
	 */
    private void viewPage(String url) {
        try {
            Document document = buildDocument(url);
            Node n = (Node) XPATH.evaluate("/html/body/div[2]/div/div/table//tr[5]/td[2]/text()", document, XPathConstants.NODE);
            String s = (String) XPATH.evaluate("substring-before(substring-after(/html/body/div[2]/div/div/table//tr[5]/td[2]/text(),'('),')')", document, XPathConstants.STRING);
            LOG.info(s);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
	 * @param document
	 * @throws XPathExpressionException 
	 */
    private void reviewNextPage(Document document) throws Exception {
        String path = (String) XPATH.evaluate(this.getPathNextPage(), document, XPathConstants.STRING);
        if (null != path && !"".equals(path)) {
            this.setUri(this.getFullPath(path));
            this.reviewPage();
        }
    }

    /**
	 * @return the uri
	 */
    public String getUri() {
        return uri;
    }

    /**
	 * @param uri the uri to set
	 */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
	 * @return the pathElement
	 */
    public String getPathElement() {
        return pathElement;
    }

    /**
	 * @param pathElement the pathElement to set
	 */
    public void setPathElement(String pathElement) {
        this.pathElement = pathElement;
    }

    /**
	 * @return the pathNextPage
	 */
    public String getPathNextPage() {
        return pathNextPage;
    }

    /**
	 * @param pathNextPage the pathNextPage to set
	 */
    public void setPathNextPage(String pathNextPage) {
        this.pathNextPage = pathNextPage;
    }

    /**
	 * @return the baseUrl
	 */
    public String getBaseUrl() {
        return baseUrl;
    }

    /**
	 * @param baseUrl the baseUrl to set
	 */
    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
	 * @return the pageIndex
	 */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
	 * @param pageIndex the pageIndex to set
	 */
    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    /**
	 * @return the checkXPath
	 */
    public String getCheckXPath() {
        return checkXPath;
    }

    /**
	 * @param checkXPath the checkXPath to set
	 */
    public void setCheckXPath(String checkXPath) {
        this.checkXPath = checkXPath;
    }

    /**
	 * @return the targetUrlXpath
	 */
    public String getTargetUrlXpath() {
        return targetUrlXpath;
    }

    /**
	 * @param targetUrlXpath the targetUrlXpath to set
	 */
    public void setTargetUrlXpath(String targetUrlXpath) {
        this.targetUrlXpath = targetUrlXpath;
    }

    /**
	 * @return the xpath
	 */
    public static XPath getXpath() {
        return XPATH;
    }

    /**
	 * @return the namePath
	 */
    public String getNamePath() {
        return namePath;
    }

    /**
	 * @param namePath the namePath to set
	 */
    public void setNamePath(String namePath) {
        this.namePath = namePath;
    }

    public static void main(String[] args) {
        WebSnap webSnap = new WebSnap();
        String url = "http://xm.58.com/zufang/2518177841921x.shtml";
        webSnap.buildBaseUrl(url);
        webSnap.viewPage(url);
    }
}
