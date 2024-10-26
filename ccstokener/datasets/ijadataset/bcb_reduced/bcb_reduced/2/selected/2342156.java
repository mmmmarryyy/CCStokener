package net.sourceforge.websnaptool.hr.snap;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import net.sourceforge.websnaptool.hr.exception.RunException;
import net.sourceforge.websnaptool.hr.exception.SnapParserErrorHandler;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lobobrowser.html.UserAgentContext;
import org.lobobrowser.html.parser.HtmlParser;
import org.lobobrowser.html.test.SimpleUserAgentContext;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 *  Snap:网页快照
 *  
 * User: Muwang Zheng
 * Date: 2010-9-13
 * Time: 下午03:49:03
 * 
 */
public class Snap {

    /**
	 * log
	 */
    public static final Log LOG = LogFactory.getLog(Snap.class);

    /**
	 * XPATH
	 */
    public static final XPath XPATH = XPathFactory.newInstance().newXPath();

    /**
	 * 文档
	 */
    private Document document;

    /**
	 * @return the document
	 */
    public Document getDocument() {
        if (null == this.document) {
            this.document = buildDocument();
        }
        return document;
    }

    /**
	 * 根据指定的uri生成文档信息
	 * @param uri 
	 * @return 文档信息
	 */
    public Document buildDocument() {
        String uri = this.getUri();
        if (uri == null) {
            throw new RunException("URL为空，不能构建document");
        }
        Document document = null;
        Logger.getLogger("org.lobobrowser").setLevel(Level.WARNING);
        InputStream in = null;
        try {
            UserAgentContext uacontext = new SimpleUserAgentContext();
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Snap.LOG.info("URL:" + uri);
            URL url = new URL(uri);
            URLConnection connection = url.openConnection();
            connection.setRequestProperty("User-Agent", "Baiduspider+(+http://www.baidu.com/search/spider.htm)");
            in = connection.getInputStream();
            document = builder.newDocument();
            String charset = org.lobobrowser.util.Urls.getCharset(connection);
            if (null != charset) {
                if (!charset.toUpperCase().startsWith("UTF")) {
                    charset = "GB2312";
                }
            }
            Reader reader = new InputStreamReader(in, charset);
            HtmlParser parser = new HtmlParser(uacontext, document, new SnapParserErrorHandler(), uri, uri);
            parser.parse(reader);
            return document;
        } catch (Exception e) {
            if (LOG.isInfoEnabled()) {
                LOG.info(e.getMessage());
            } else {
                LOG.error(e.getMessage(), e);
            }
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
        return document;
    }

    /**
	 * 地址入口
	 */
    private String uri = null;

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
        this.buildBaseUrl(this.getUri());
    }

    /**
	 * 获取url全路径
	 * @param href
	 * @return url
	 */
    protected String getFullPath(String href) {
        if (href != null && !"".equals(href) && !this.isFullPath(href)) {
            if (!href.startsWith("/")) {
                return this.getBaseUrl() + "/" + href;
            }
            return this.getBaseUrl() + href;
        }
        return href;
    }

    /**
	 * 是否是全路径
	 * @param href　连接
	 * @return 　是否是全路径
	 */
    protected boolean isFullPath(String href) {
        if (null == href) return false;
        return href.toLowerCase().startsWith("http://") || href.toLowerCase().startsWith("https://");
    }

    /**
	 * 生成基本URL
	 * @param uri
	 */
    public void buildBaseUrl(String uri) {
        if (null == uri || "".equals(uri.trim())) {
            return;
        }
        this.setBaseUrl(getWeb(uri));
    }

    /**
	 * 获取该网址所在网页
	 * @param uri
	 * @return 该网址所在网页
	 */
    public static String getWeb(String uri) {
        try {
            URL url = new URL(uri);
            return url.getProtocol() + "://" + url.getHost() + (url.getPort() > 0 ? ":" + url.getPort() : "") + "";
        } catch (MalformedURLException e) {
            LOG.error(null != uri ? uri + e.getMessage() : e.getMessage(), e);
        }
        return null;
    }

    /**
	 * 生成基本路径
	 * @param uri　原uri
	 * @return 基本路径
	 */
    public String genBaseUrl(String uri) {
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
	 * 根据指定的path获取当前document中的字符串值
	 * @param path xpath
	 * @return 结点列表
	 */
    public String getStrVal(String path) {
        return getStrVal(path, this.getDocument());
    }

    /**
	 * 根据指定的path获取当前上下文中的字符串值
	 * @param path xpath
	 * @param context 上下文
	 * @return 结点列表
	 */
    public String getStrVal(String path, Object context) {
        return (String) getEvaluateObj(path, context, XPathConstants.STRING);
    }

    /**
	 * 根据指定的path获取当前document中的结点列表
	 * @param path xpath
	 * @return 结点列表
	 */
    public NodeList getNodeList(String path) {
        return getNodeList(path, this.getDocument());
    }

    /**
	 * 根据指定的path获取当前上下文的结点列表
	 * @param path xpath
	 * @param context 上下文
	 * @return 结点列表
	 */
    public NodeList getNodeList(String path, Object context) {
        return (NodeList) getEvaluateObj(path, context, XPathConstants.NODESET);
    }

    /**
	 * 根据指定的path获取当前对象
	 * @param path xpath
	 * @param context 上下文
	 * @param qname QName
	 * @return 结点列表
	 */
    public Object getEvaluateObj(String path, Object context, QName qname) {
        try {
            return XPATH.evaluate(path, context, qname);
        } catch (XPathExpressionException e) {
            throw new RunException(e.getMessage(), e);
        }
    }

    /**
	 * baseUrl
	 */
    private String baseUrl;

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
}
