package org.hlj.web.ui.filter;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.hlj.commons.classes.ClassUtil;
import org.hlj.commons.io.stream.StreamUtil;
import org.hlj.log.log4j.common.SysLog;
import org.hlj.web.util.HttpUtil;
import org.hlj.web.util.ResponseUtil;
import org.hlj.web.util.UrlUtil;

/**
 * 过滤hljui使用的js和css请求,过滤已/hljui开头的js和css请求
 * @author 韩连健
 * @since JDK5
 * @version 1.0 2010-01-20
 */
public final class UIFilter implements Filter {

    private static String urlPath;

    private static String filePath;

    /**
	 * 初始化过滤器
	 */
    public void init(FilterConfig filterConfig) throws ServletException {
        urlPath = "/hljui";
        filePath = "org/hlj/web/ui/static";
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
            SysLog.warn(e);
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
