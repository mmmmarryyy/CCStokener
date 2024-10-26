package mkk.princess.util;

import mkk.princess.core.domain.ParseData;
import mkk.princess.core.shared.LogHelper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Shengzhao Li
 */
public abstract class HtmlParseUtils {

    private static LogHelper log = LogHelper.create(HtmlParseUtils.class);

    public static final String HTTP = "http://";

    public static final String HTTPS = "https://";

    public static final String HTTP_WWW = "http://www.";

    public static byte[] getResponse(String url) throws IOException {
        log.info("Get Response from [" + url + "]");
        HttpEntity entity = getHttpEntity(url);
        return EntityUtils.toByteArray(entity);
    }

    private static HttpEntity getHttpEntity(String url) throws IOException {
        HttpClient client = new DefaultHttpClient();
        HttpParams params = client.getParams();
        HttpConnectionParams.setConnectionTimeout(params, 60000);
        HttpConnectionParams.setSoTimeout(params, 120000);
        HttpGet httpGet = new HttpGet(url);
        httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; rv:9.0.1) Gecko/20100101 Firefox/9.0.1");
        HttpResponse response = client.execute(httpGet);
        return response.getEntity();
    }

    public static String getHtmlText(String url) {
        log.info("Get Html from [" + url + "]");
        try {
            HttpEntity httpEntity = getHttpEntity(url);
            return EntityUtils.toString(httpEntity, "UTF-8");
        } catch (Exception e) {
            log.info("Get Html from [" + url + "] error", e);
            return null;
        }
    }

    /**
     * HTML中的每一行是一条数据.
     *
     * @param url url
     * @return list
     */
    public static List<String> getHtmlLineAsList(String url) {
        log.info("Get Html as list from [" + url + "]");
        try {
            List<String> list = new ArrayList<String>();
            byte[] response = getResponse(url);
            BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(response)));
            try {
                String text;
                while ((text = br.readLine()) != null) {
                    list.add(text);
                }
                return list;
            } finally {
                br.close();
            }
        } catch (Exception e) {
            log.info("Get Html as list from [" + url + "] error", e);
            return Collections.emptyList();
        }
    }

    /**
     * 校正URL.
     *
     * @param url url
     * @return Regulated url
     */
    public static String regulateUrl(String url) {
        if (url == null || url.length() == 0) {
            return url;
        }
        String lowerCaseUrl = url.toLowerCase();
        if (lowerCaseUrl.startsWith(HTTP) || lowerCaseUrl.startsWith(HTTPS)) {
            return url;
        }
        if (!lowerCaseUrl.startsWith("www.")) {
            url = HTTP_WWW + url;
        } else {
            url = HTTP + url;
        }
        return url;
    }

    public static void writeExcelData(String fullPath, List<ParseData> parseDataList) throws IOException {
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet("email");
        sheet.setColumnWidth(0, 10000);
        sheet.setColumnWidth(1, 10000);
        sheet.setColumnWidth(2, 10000);
        int i = 0;
        HSSFRow titleRow = sheet.createRow(i++);
        titleRow.createCell(0).setCellValue("Email");
        titleRow.createCell(1).setCellValue("Host Url");
        titleRow.createCell(2).setCellValue("Original Url");
        for (ParseData parseData : parseDataList) {
            HSSFRow row = sheet.createRow(i++);
            HSSFCell cell = row.createCell(0);
            cell.setCellValue(parseData.getEmail());
            cell = row.createCell(1);
            cell.setCellValue(parseData.getHost());
            cell = row.createCell(2);
            cell.setCellValue(parseData.getOriginalUrl());
        }
        log.info("Write excel data to file: " + fullPath);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fullPath));
        try {
            workbook.write(bos);
        } finally {
            bos.close();
        }
    }

    public static String cleanHtml(String html) {
        if (html == null) {
            return null;
        }
        HtmlCleaner htmlCleaner = new HtmlCleaner();
        TagNode tagNode = htmlCleaner.clean(html);
        return htmlCleaner.getInnerHtml(tagNode);
    }

    /**
     * Clean and return <i>body</i> content
     *
     * @param html html
     * @return body html
     */
    public static String cleanBody(String html) {
        if (html == null) {
            return null;
        }
        HtmlCleaner htmlCleaner = new HtmlCleaner();
        TagNode tagNode = htmlCleaner.clean(html);
        TagNode bodyNode = tagNode.findElementByName("body", false);
        if (bodyNode == null) {
            bodyNode = tagNode.findElementByName("BODY", false);
        }
        return htmlCleaner.getInnerHtml(bodyNode);
    }
}
