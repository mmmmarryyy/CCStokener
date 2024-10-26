package ru.javawebcrowler.spider;

import org.apache.commons.jxpath.ri.JXPathContextReferenceImpl;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import ru.javawebcrowler.utils.HtmlHelper;
import ru.javawebcrowler.utils.MySocketFactory;
import ru.javawebcrowler.spider.statistics.SpiderStatistics;
import ru.javawebcrowler.spider.statistics.LogMessage;
import ru.javawebcrowler.xmlmodel.AbstractPage;
import ru.javawebcrowler.xmlmodel.FilePage;
import ru.javawebcrowler.xmlmodel.HtmlPage;
import ru.javawebcrowler.xmlmodel.Pages;
import ru.javawebcrowler.xpath.TagNodePointerFactory;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Паук, который ползает по сайту, используя конфигурацию, содержащуюся в Pages
 *
 * @author Evgeny
 * @since 24.09.2009
 */
public class HtmlSpider {

    private final Pages config;

    private final HashSet<RealAbstractItem> foundItems = new HashSet<RealAbstractItem>();

    private final HttpClient httpClient = new DefaultHttpClient();

    public HttpClient getHttpClient() {
        return httpClient;
    }

    static {
        JXPathContextReferenceImpl.addNodePointerFactory(new TagNodePointerFactory());
    }

    private List<LogMessage> log = new ArrayList<LogMessage>();

    public void log(LogMessage message) {
        log.add(message);
    }

    public List<LogMessage> getLog() {
        return log;
    }

    public HtmlSpider(Pages config, Map<String, String> loginParameters) throws IOException {
        this.config = config;
        httpClient.getParams().setParameter("http.useragent", "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 5.1)");
        MySocketFactory.patchHttpClient(httpClient);
        if (config.getAuthentication() != null) {
            if (config.getAuthentication().getFormPage() != null) {
                HtmlHelper.justVisitPage(new URL(config.getAuthentication().getFormPage()), httpClient);
            }
            List<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            List<Pages.Authentication.Param> paramList = config.getAuthentication().getParam();
            for (Pages.Authentication.Param param : paramList) {
                params.add(new BasicNameValuePair(param.getName(), getValue(loginParameters.get(param.getName()), param.getValue())));
            }
            UrlEncodedFormEntity entity = new UrlEncodedFormEntity(params);
            HttpPost post = new HttpPost(config.getAuthentication().getPage());
            post.setEntity(entity);
            HttpResponse response = httpClient.execute(post);
            response.getEntity().consumeContent();
        }
    }

    private static String getValue(String value, String defaultValue) {
        return value == null ? defaultValue : value;
    }

    public SpiderStatistics crawl() throws Exception {
        List<AbstractPage> pages = config.getPage();
        SpiderStatistics stats = new SpiderStatistics();
        addListener(stats);
        for (AbstractPage page : pages) {
            if (page.getStart() != null) {
                foundItem(page, new URL(page.getStart()), null, null);
            }
        }
        stats.lodSizeStats();
        return stats;
    }

    protected void foundItem(AbstractPage page, URL url, RealHtmlPage cameFrom, String linkName) throws Exception {
        if (page instanceof FilePage) {
            foundItem(new RealFilePage(url, linkName, this, (FilePage) page, cameFrom));
        } else if (page instanceof HtmlPage) {
            foundItem(new RealHtmlPage(url, linkName, this, (HtmlPage) page, cameFrom));
        }
    }

    protected void foundItem(RealAbstractItem foundItem) throws Exception {
        foundItem.evalParameters();
        if (foundItems.contains(foundItem)) {
            foundItem.info("[УДАЛЕНИЕ] такая страница уже просмотрена");
            return;
        }
        foundItem.info("[ДОБАВЛЕНИЕ] такой страницы еще нет");
        foundItems.add(foundItem);
        notifyListeners(foundItem);
        foundItem.process();
    }

    private void notifyListeners(RealAbstractItem foundItem) {
        for (SpiderListener listener : listeners) {
            listener.onItemFound(foundItem);
        }
    }

    protected void foundItem(URL url, String pageId, RealHtmlPage cameFrom, String linkName) throws Exception {
        List<AbstractPage> pages = config.getPage();
        for (AbstractPage page : pages) {
            if (page.getId().equals(pageId)) {
                foundItem(page, url, cameFrom, linkName);
            }
        }
    }

    private List<SpiderListener> listeners = new ArrayList<SpiderListener>();

    public void addListener(SpiderListener listener) {
        listeners.add(listener);
    }

    public void removeListener(SpiderListener listener) {
        listeners.remove(listener);
    }

    public static interface SpiderListener {

        public void onItemFound(RealAbstractItem item);
    }
}
