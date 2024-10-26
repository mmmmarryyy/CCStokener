package org.wdcode.ui.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.wdcode.common.io.StreamUtil;
import org.wdcode.common.log.WdLogs;
import org.wdcode.common.util.ClassUtil;
import org.wdcode.web.util.HttpUtil;
import org.wdcode.web.util.ResponseUtil;
import org.wdcode.web.util.UrlUtil;

/**
 * 过滤wdui使用的js和css请求,过滤已/wdui开头的js和css请求
 * @author WD
 * @since JDK6
 * @version 1.0 2010-01-20
 */
public final class WdUIFilter implements Filter {

    private static String urlPath;

    private static String filePath;

    /**
	 * 初始化过滤器
	 */
    public void init(FilterConfig filterConfig) throws ServletException {
        urlPath = "/wdui";
        filePath = "org/wdcode/ui/static";
    }

    /**
	 * 执行过滤器
	 */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        try {
            String servletPath = httpRequest.getServletPath();
            int pos = servletPath.indexOf(urlPath);
            if (pos > -1) {
                if (pos > 0) {
                    servletPath = servletPath.substring(pos);
                }
                ResponseUtil.setContentType(response, HttpUtil.getContentType(servletPath));
                StreamUtil.write(response.getOutputStream(), UrlUtil.openStream(ClassUtil.getResource(servletPath.replaceAll(urlPath, filePath))));
            }
        } catch (RuntimeException e) {
            WdLogs.warn(e);
        }
    }

    /**
	 * 销毁过滤器
	 */
    public void destroy() {
        urlPath = null;
        filePath = null;
    }
}
