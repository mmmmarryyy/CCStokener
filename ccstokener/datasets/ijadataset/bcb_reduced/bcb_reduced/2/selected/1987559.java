package malgnsoft.util;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.xml.parsers.*;
import javax.xml.transform.dom.*;
import org.w3c.dom.*;
import malgnsoft.db.DataSet;
import javax.servlet.jsp.JspWriter;
import malgnsoft.util.Malgn;

public class OpenApi {

    public String[] apiTypes = { "naverbook=>네이버책", "naverimage=>네이버이미지", "navernews=>네이버뉴스", "naverkin=>네이버지식", "tomorrow=>내일검색", "archive=>국가기록원", "youtube=>You Tube" };

    URL url = null;

    InputStream is = null;

    Document xmlDocument = null;

    DocumentBuilderFactory factory = null;

    DocumentBuilder builder = null;

    Element root = null;

    NodeList items = null;

    String apiUrl = null;

    String apiName = null;

    String keyword = null;

    Hashtable<String, String> parameters = new Hashtable<String, String>();

    String dataField = "item";

    String[] dataElements = null;

    String[] reportElements = null;

    String dateFormat = null;

    String dateConvFormat = null;

    Vector<String> errors = new Vector<String>();

    public OpenApi() {
    }

    public OpenApi(String apiName, String keyword) throws Exception {
        this.apiName = apiName.toLowerCase();
        this.keyword = keyword;
        try {
            factory = DocumentBuilderFactory.newInstance();
            builder = factory.newDocumentBuilder();
        } catch (Exception e) {
            errors.add("API검색 초기화 실패. error - create builder from factory.");
            Malgn.errorLog("{OpenApi.OpenApi} " + e.getMessage());
        }
    }

    public void addParameter(String key, String value) {
        parameters.put(key, value);
    }

    private String getParameters() {
        String str = "";
        Enumeration e = parameters.keys();
        while (e.hasMoreElements()) {
            String key = (String) e.nextElement();
            String value = parameters.containsKey(key) ? parameters.get(key).toString() : "";
            str += "&" + key + "=" + value;
        }
        return str.length() > 1 ? str.substring(1) : "";
    }

    private void parse() throws Exception {
        url = new URL(apiUrl);
        is = url.openStream();
        try {
            xmlDocument = builder.parse(is);
            root = xmlDocument.getDocumentElement();
            items = root.getElementsByTagName(dataField);
        } catch (Exception e) {
            errors.add("API검색을 할수 없습니다. error - parseData from inputstream.");
            Malgn.errorLog("{Generator.parse} " + e.getMessage());
        } finally {
            if (is != null) is.close();
        }
    }

    public DataSet getDataSet() throws Exception {
        DataSet result = new DataSet();
        if (initialize()) {
            Locale loc = new Locale("ENGLISH");
            loc.setDefault(loc.US);
            parse();
            if (null != items) {
                for (int i = 0; i < items.getLength(); i++) {
                    result.addRow();
                    NodeList subItems = items.item(i).getChildNodes();
                    for (int j = 0; j < subItems.getLength(); j++) {
                        String[] dataElementKeys = Malgn.getItemKeys(dataElements);
                        String nodeName = subItems.item(j).getNodeName();
                        if (Malgn.inArray(nodeName, dataElementKeys)) {
                            String key = Malgn.getItem(subItems.item(j).getNodeName().trim(), dataElements).toLowerCase();
                            String value = null != subItems.item(j).getFirstChild() ? subItems.item(j).getFirstChild().getNodeValue() : "";
                            result.put(key, value);
                            if ("media:thumbnail".equals(nodeName) || "media:player".equals(nodeName)) {
                                result.put(key, ((Element) subItems.item(j)).getAttribute("url"));
                            }
                            if ("pubdate".equals(key) && null != dateFormat) {
                                result.put(key + "_conv", Malgn.getTimeString(dateConvFormat, Malgn.strToDate(dateFormat, value, loc)));
                            }
                        }
                    }
                    result.put("__i", i);
                    result.put("__asc", i + 1);
                }
            }
        } else {
            errors.add("지정되지 않은 API. error - unknown api.");
        }
        return result;
    }

    public void error(JspWriter out) throws Exception {
        out.print(!errors.isEmpty() ? Malgn.join("<hr><br>", errors.toArray()) : "");
        errors.clear();
    }

    private boolean initialize() throws Exception {
        if ("navernews".equals(apiName)) {
            parameters.put("key", "02f21a3b0cbb431be65ecc1557a059d7");
            parameters.put("query", URLEncoder.encode(keyword, "utf-8"));
            parameters.put("target", "news");
            parameters.put("start", "1");
            parameters.put("display", "100");
            apiUrl = "http://openapi.naver.com/search?" + getParameters();
            errors.add(apiUrl);
            dataField = "item";
            dataElements = new String[] { "title", "originallink", "link", "description", "pubDate" };
            reportElements = new String[] { "rss", "channel", "lastBuildDate", "total", "start", "display" };
            dateFormat = "EEE, dd MMM yyyy HH:mm:ss Z";
            dateConvFormat = "yyyy.MM.dd HH:mm";
            return true;
        }
        if ("naverbook".equals(apiName)) {
            parameters.put("key", "02f21a3b0cbb431be65ecc1557a059d7");
            parameters.put("query", URLEncoder.encode(keyword, "utf-8"));
            parameters.put("target", "book");
            parameters.put("start", "1");
            parameters.put("display", "100");
            apiUrl = "http://openapi.naver.com/search?" + getParameters();
            errors.add(apiUrl);
            dataField = "item";
            dataElements = new String[] { "title", "originallink", "link", "image", "author", "price", "discount", "publisher", "pubdate", "isbn", "description" };
            reportElements = new String[] { "rss", "channel", "lastBuildDate", "total", "start", "display" };
            dateFormat = "yyyyMMdd";
            dateConvFormat = "yyyy.MM.dd";
            return true;
        }
        if ("naverkin".equals(apiName)) {
            parameters.put("key", "02f21a3b0cbb431be65ecc1557a059d7");
            parameters.put("query", URLEncoder.encode(keyword, "utf-8"));
            parameters.put("target", "kin");
            parameters.put("start", "1");
            parameters.put("display", "100");
            apiUrl = "http://openapi.naver.com/search?" + getParameters();
            errors.add(apiUrl);
            dataField = "item";
            dataElements = new String[] { "title", "link", "description" };
            reportElements = new String[] { "rss", "channel", "lastBuildDate", "total", "start", "display" };
            return true;
        }
        if ("naverimage".equals(apiName)) {
            parameters.put("key", "02f21a3b0cbb431be65ecc1557a059d7");
            parameters.put("query", URLEncoder.encode(keyword, "utf-8"));
            parameters.put("target", "image");
            parameters.put("start", "1");
            parameters.put("display", "100");
            apiUrl = "http://openapi.naver.com/search?" + getParameters();
            errors.add(apiUrl);
            dataField = "item";
            dataElements = new String[] { "title", "link", "thumbnail", "sizeheight", "sizewidth" };
            reportElements = new String[] { "rss", "channel", "lastBuildDate", "total", "start", "display" };
            return true;
        }
        if ("tomorrow".equals(apiName)) {
            parameters.put("apikey", "58A29064D80F4E055D88E39C719AB9445D0F160C");
            parameters.put("q", URLEncoder.encode(keyword, "utf-8"));
            parameters.put("sort", "1");
            parameters.put("count", "100");
            apiUrl = "http://naeil.incruit.com/rss/search/?" + getParameters();
            errors.add(apiUrl);
            dataField = "item";
            dataElements = new String[] { "title", "link", "description" };
            reportElements = new String[] { "" };
            return true;
        }
        if ("archive".equals(apiName)) {
            parameters.put("key", "J0J9H2X6C4U7H2M9H2X1Z3X5W3X0Z5T0");
            parameters.put("query", URLEncoder.encode(keyword, "utf-8"));
            parameters.put("sort", "1");
            parameters.put("online_reading", "Y");
            parameters.put("display", "100");
            apiUrl = "http://search.archives.go.kr/openapi/search.arc?" + getParameters();
            errors.add(apiUrl);
            dataField = "item";
            dataElements = new String[] { "title", "link", "prod_name=>description" };
            reportElements = new String[] { "" };
            return true;
        }
        if ("youtube".equals(apiName)) {
            parameters.put("q", URLEncoder.encode(keyword, "utf-8"));
            apiUrl = "http://gdata.youtube.com/feeds/api/videos?" + getParameters();
            errors.add(apiUrl);
            dataField = "media:group";
            dataElements = new String[] { "media:title=>title", "media:player=>link", "media:description=>description", "media:thumbnail=>image" };
            reportElements = new String[] { "" };
            return true;
        }
        return false;
    }
}
