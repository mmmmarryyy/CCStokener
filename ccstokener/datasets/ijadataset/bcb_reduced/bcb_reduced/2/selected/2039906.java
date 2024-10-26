package org.jmantis.web;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import org.jmantis.server.Config;
import org.jmantis.server.Context;
import org.jmantis.server.Server;
import org.jmantis.web.config.ActionInfo;
import org.jmantis.web.config.ViewInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.collect.Maps;
import com.googlecode.httl.Engine;

public class HttlViewHandler implements ViewHandler {

    private static final Logger log = LoggerFactory.getLogger(Server.class);

    private static final String EXT_NAME = ".htm";

    private Engine engine;

    @Override
    public void init(Config config) throws Exception {
        Properties configure = new Properties();
        URL url = HttlViewHandler.class.getResource("/httl.properties");
        log.info("配置路径:{}", url);
        InputStream is = url.openStream();
        try {
            configure.load(is);
            File dir = new File(config.getTemplateFilePath());
            log.info("启动httl模板引擎:[template.directory:{}]", dir.getAbsolutePath());
            configure.put("template.directory", dir.getAbsolutePath());
            engine = Engine.getEngine(configure);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    @Override
    public void doView(ActionInfo action, Object result, Request request, Response response) throws Exception {
        Map<String, Object> context = Maps.newHashMap();
        context.put("context", Context.getInstance().getAttributesMap());
        ViewInfo view = null;
        String viewValue = null;
        if (null == result) {
            view = action.getDefaultView();
        } else {
            if (result instanceof View) {
                View v = (View) result;
                view = action.getView(v.getName());
                context.put("result", v.getValue());
            } else if (result instanceof CharSequence) {
                view = action.getView((CharSequence) result);
            } else {
                context.put("result", result);
                view = action.getDefaultView();
            }
        }
        if (null != view) {
            viewValue = view.getValue();
        }
        if (viewValue != null) {
            engine.getTemplate(viewValue + EXT_NAME).render(context, response.getWriter());
        }
    }
}
