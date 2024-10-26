package de.frostcode.visualmon.web;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javax.annotation.concurrent.ThreadSafe;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.frostcode.visualmon.VisualMon;
import de.frostcode.visualmon.conf.ImmutableConfig;
import de.frostcode.visualmon.log.common.LogList;
import de.frostcode.visualmon.log.common.LogProvider;
import de.frostcode.visualmon.probe.ProbeData;

/**
 * VisualMon dashboard servlet displaying all probe snapshots, charts of historical data and
 * current alerts.
 */
@ThreadSafe
public final class VisualMonServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(VisualMonServlet.class);

    private static final String CACHE_NAME = "VisualMonCharts";

    private static final String ENCODING = "UTF-8";

    private static final long YEAR_IN_SECONDS = 60 * 60 * 24 * 365;

    private transient Cache chartCache;

    private transient ImmutableConfig cfg;

    private transient VisualMon filter;

    private boolean isInternalFilter = false;

    @Override
    public void init() throws ServletException {
        super.init();
        final ServletContext servletContext = getServletContext();
        filter = (VisualMon) servletContext.getAttribute(VisualMon.class.getName());
        if (null == filter) {
            isInternalFilter = true;
            filter = new VisualMon();
            filter.init(new FilterConfig() {

                @Override
                public String getFilterName() {
                    return null;
                }

                @Override
                public String getInitParameter(final String name) {
                    return servletContext.getInitParameter(name);
                }

                @Override
                @SuppressWarnings("unchecked")
                public Enumeration<String> getInitParameterNames() {
                    return servletContext.getInitParameterNames();
                }

                @Override
                public ServletContext getServletContext() {
                    return servletContext;
                }
            });
        }
        cfg = filter.getConfiguration();
        chartCache = new Cache(CACHE_NAME, 200, false, false, cfg.getChartCacheSeconds(), cfg.getChartCacheSeconds());
        CacheManager.create().addCache(chartCache);
    }

    @Override
    public void destroy() {
        if (isInternalFilter) filter.destroy();
        super.destroy();
        CacheManager.getInstance().removeCache(CACHE_NAME);
    }

    @Override
    protected void service(final HttpServletRequest req, final HttpServletResponse res) throws ServletException, IOException {
        res.setHeader("X-Generator", "VisualMon");
        String path = req.getPathInfo();
        if (null == path || "".equals(path)) res.sendRedirect(req.getServletPath() + "/"); else if ("/chart".equals(path)) {
            try {
                res.setHeader("Cache-Control", "private,no-cache,no-store,must-revalidate");
                res.addHeader("Cache-Control", "post-check=0,pre-check=0");
                res.setHeader("Expires", "Sat, 26 Jul 1997 05:00:00 GMT");
                res.setHeader("Pragma", "no-cache");
                res.setDateHeader("Expires", 0);
                renderChart(req, res);
            } catch (InterruptedException e) {
                log.info("Chart generation was interrupted", e);
                Thread.currentThread().interrupt();
            }
        } else if (path.startsWith("/log_")) {
            String name = path.substring(5);
            LogProvider provider = null;
            for (LogProvider prov : cfg.getLogProviders()) {
                if (name.equals(prov.getName())) {
                    provider = prov;
                    break;
                }
            }
            if (null == provider) {
                log.error("Log provider with name \"{}\" not found", name);
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
            } else {
                render(res, provider.getLog(filter.getLocale()));
            }
        } else if ("/".equals(path)) {
            List<LogEntry> logs = new ArrayList<LogEntry>();
            for (LogProvider provider : cfg.getLogProviders()) logs.add(new LogEntry(provider.getName(), provider.getTitle(filter.getLocale())));
            render(res, new ProbeDataList(filter.getSnapshot(), filter.getAlerts(), logs, ResourceBundle.getBundle("de.frostcode.visualmon.stats", filter.getLocale()).getString("category.empty"), cfg.getDashboardTitle().get(filter.getLocale())));
        } else {
            URL url = Thread.currentThread().getContextClassLoader().getResource(getClass().getPackage().getName().replace('.', '/') + req.getPathInfo());
            if (null == url) {
                res.sendError(HttpServletResponse.SC_NOT_FOUND);
                return;
            }
            res.setDateHeader("Last-Modified", new File(url.getFile()).lastModified());
            res.setDateHeader("Expires", new Date().getTime() + YEAR_IN_SECONDS * 1000L);
            res.setHeader("Cache-Control", "max-age=" + YEAR_IN_SECONDS);
            URLConnection conn = url.openConnection();
            String resourcePath = url.getPath();
            String contentType = conn.getContentType();
            if (resourcePath.endsWith(".xsl")) {
                contentType = "text/xml";
                res.setCharacterEncoding(ENCODING);
            }
            if (contentType == null || "content/unknown".equals(contentType)) {
                if (resourcePath.endsWith(".css")) contentType = "text/css"; else contentType = getServletContext().getMimeType(resourcePath);
            }
            res.setContentType(contentType);
            res.setContentLength(conn.getContentLength());
            OutputStream out = res.getOutputStream();
            IOUtils.copy(conn.getInputStream(), out);
            IOUtils.closeQuietly(conn.getInputStream());
            IOUtils.closeQuietly(out);
        }
    }

    @SuppressWarnings("unchecked")
    private void renderChart(final HttpServletRequest req, final HttpServletResponse res) throws InterruptedException, IOException {
        long snapShotPeriod = cfg.getSnapshotUnit().toMillis(cfg.getSnapshotResolution());
        if (!"bar".equals(req.getParameter("type"))) throw new IllegalArgumentException("Only bar charts supported yet");
        final Chart chart = new BarChart(req, snapShotPeriod, filter.getLocale());
        FutureTask<byte[]> renderResult = null;
        synchronized (chartCache) {
            Element elem = chartCache.get(chart.getKey());
            if (null == elem) {
                renderResult = new FutureTask<byte[]>(new Callable<byte[]>() {

                    @Override
                    public byte[] call() throws IOException {
                        return chart.render(filter.getProbeData(chart.getKey().getProbeDataKey()), filter.getTimeSeries(chart.getKey().getProbeDataKey()));
                    }
                });
                chartCache.put(new Element(chart.getKey(), renderResult));
                renderResult.run();
            } else {
                renderResult = (FutureTask<byte[]>) elem.getObjectValue();
            }
        }
        try {
            byte[] chartData = renderResult.get();
            res.setContentType("image/png");
            res.getOutputStream().write(chartData);
        } catch (CancellationException e) {
            chartCache.remove(chart.getKey());
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException) throw (RuntimeException) e.getCause();
            if (e.getCause() instanceof Error) throw (Error) e.getCause();
            throw new IllegalStateException("Not unchecked", e.getCause());
        }
    }

    private void render(final HttpServletResponse res, final Object jaxbBean) throws IOException {
        OutputStream out = res.getOutputStream();
        try {
            JAXBContext context = JAXBContext.newInstance(ProbeDataList.class, ProbeData.class, LogList.class);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, ENCODING);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            res.setContentType("text/xml");
            res.setCharacterEncoding(ENCODING);
            out.write(("<?xml version=\"1.0\" encoding=\"" + ENCODING + "\"?>\n").getBytes(ENCODING));
            out.write(("<?xml-stylesheet type=\"text/xsl\" href=\"" + cfg.getStylesheet() + "\"?>\n").getBytes(ENCODING));
            marshaller.marshal(jaxbBean, out);
        } catch (PropertyException e) {
            log.error("Cannot generate statistics output", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } catch (JAXBException e) {
            log.error("Cannot generate statistics output", e);
            res.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            out.close();
        }
    }
}
