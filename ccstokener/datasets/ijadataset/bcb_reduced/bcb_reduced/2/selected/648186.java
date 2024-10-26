package jk.spider.core.task.work;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import jk.spider.core.SpiderController;
import jk.spider.core.event.CoreEvent;
import jk.spider.core.event.impl.URLSpideredOkEvent;
import jk.spider.core.event.impl.URLSpideredProxyErrorEvent;
import jk.spider.core.event.impl.URLSpideredProxyOkEvent;
import jk.spider.core.task.WorkerTask;
import jk.spider.model.ProxyInfo;
import jk.spider.model.Resource;
import jk.spider.util.SpiderUtil;
import org.apache.log4j.Logger;

/**
 * ���Ĺ�������ͨ�����ץȡ��ҳԴ��
 * @author kqy
 *
 */
public class SpiderHttpURLProxyTask extends BaseWorkerTaskImpl {

    private static final Logger log = Logger.getLogger(SpiderHttpURLProxyTask.class);

    protected Resource resource;

    protected int httpStatus = 0;

    protected int timeMs = 0;

    protected ProxyInfo proxyInfo;

    public SpiderHttpURLProxyTask(SpiderController controller, Resource resource, ProxyInfo proxyInfo) {
        super(controller, WorkerTask.WORKERTASK_SPIDERTASK);
        this.resource = resource;
        this.proxyInfo = proxyInfo;
    }

    public String getUrlType() {
        return resource.getType();
    }

    public int getType() {
        return WorkerTask.WORKERTASK_SPIDERTASK;
    }

    public String toString() {
        return "RID [" + resource.getRId() + "] [ " + resource.getUrl() + "] httpStatus [ " + httpStatus + " ] ";
    }

    public void prepare() {
        controller.thottle();
        controller.getStorage().getResourceDAO().addRequsetCount(resource.getRId(), resource.getReqCount());
    }

    public void execute() {
        httpStatus = 0;
        timeMs = 0;
        URL url = null;
        CoreEvent event = null;
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        try {
            url = new URL(resource.getUrl());
            InetSocketAddress addr = new InetSocketAddress(proxyInfo.getPxIp(), proxyInfo.getPxPort());
            Proxy proxy = new Proxy(Proxy.Type.HTTP, addr);
            connection = (HttpURLConnection) url.openConnection(proxy);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows 2000)");
            if (!SpiderUtil.isStringNull(controller.getCookie())) {
                connection.addRequestProperty("Cookie", controller.getCookie());
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 2.0.50727)");
                connection.setFollowRedirects(true);
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setUseCaches(false);
                connection.setAllowUserInteraction(false);
                connection.setRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows 2000)");
                connection.setRequestProperty("Accept", "*/*");
                connection.setRequestProperty("accept-language", "zh-cn");
                connection.setRequestProperty("Cache-Control", "no-cache");
                connection.setRequestProperty("Pragma", "no-cache");
                connection.setRequestProperty("Host", controller.getContent().getBaseUrl());
                connection.setRequestProperty("Connection", "keep-alive");
                connection.setAllowUserInteraction(false);
            }
            long start = System.currentTimeMillis();
            connection.setConnectTimeout(60 * 1000);
            connection.setReadTimeout(60 * 1000);
            connection.connect();
            httpStatus = connection.getResponseCode();
            inputStream = new BufferedInputStream(connection.getInputStream());
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            InputStream is = new BufferedInputStream(inputStream);
            try {
                byte bytes[] = new byte[40960];
                int nRead = -1;
                while ((nRead = is.read(bytes, 0, 40960)) > 0) {
                    os.write(bytes, 0, nRead);
                }
                os.close();
                is.close();
                inputStream.close();
            } catch (SocketTimeoutException e) {
                log.error("Read timed out url [" + url + "]");
                os.close();
                is.close();
                inputStream.close();
            } catch (IOException e) {
                log.error("SpiderHttpURLProxyTask execute IOException " + proxyInfo + " url [" + url + "] - >", e);
                event = new URLSpideredProxyErrorEvent(controller, resource, proxyInfo, 14);
                os.close();
                is.close();
                inputStream.close();
            }
            String contentType = connection.getContentType();
            int timeMs = (int) (System.currentTimeMillis() - start) / 1000;
            if (httpStatus >= 200 && httpStatus < 300) {
                event = new URLSpideredProxyOkEvent(controller, resource, proxyInfo, httpStatus, contentType, os.toByteArray());
            } else {
                event = new URLSpideredProxyErrorEvent(controller, resource, proxyInfo, httpStatus);
                log.error("SpiderHttpURLProxyTask " + this.toString());
                controller.cheat();
            }
            log.info("connect by Proxy [" + proxyInfo + "]... " + this + " Time [" + timeMs + "]");
        } catch (IOException e) {
            event = new URLSpideredProxyErrorEvent(controller, resource, proxyInfo, 40);
            log.error("SpiderHttpURLProxyTask " + proxyInfo + " IOException - ��ҳ�治����  - " + this.toString());
            controller.cheat();
        } catch (Exception e) {
            event = new URLSpideredProxyErrorEvent(controller, resource, proxyInfo, 10);
            log.error("SpiderHttpURLProxyTask Exception " + proxyInfo + this.toString(), e);
            controller.cheat();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    log.error("SpiderHttpURLProxyTask IOException");
                }
            }
            this.notifyEvent(resource, event);
        }
    }
}
