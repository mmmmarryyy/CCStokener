package cn.webwheel.utils;

import cn.webwheel.*;
import cn.webwheel.di.Desire;
import cn.webwheel.di.Key;
import cn.webwheel.di.NativeProvider;
import cn.webwheel.di.Provider;
import cn.webwheel.di.utils.RichContainer;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.servlet.ServletContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 默认应用程序主类的抽象实现。
 */
public abstract class DefaultMain extends Main {

    /**
     * WebWheel DI容器。
     */
    protected RichContainer container;

    /**
     * WebWheel DI过滤器。
     */
    protected DIFilter diFilter;

    /**
     * 处理异常的过滤器
     */
    protected LogicExceptionFilter exceptionFilter;

    /**
     * {@link #bindAction(String, Class)}与{@link #bindPage(String, Class)}所使用的过滤器组。<br>
     * 其中的过滤器队列默认为：{@link #diFilter}
     */
    protected FilterGroup filterGroup;

    /**
     * 统一的编码。
     */
    protected String charset;

    /**
     * 动作http请求映射后缀
     */
    protected String actExtension = ".do";

    /**
     * 模板文件名后缀
     */
    protected String tempExtension = ".html";

    /**
     * 页面http请求映射后缀
     */
    protected String pageExtension = ".html";

    /**
     * 构造方法。
     *
     * @param charset 编码
     */
    protected DefaultMain(String charset) {
        this.charset = charset;
        container = new RichContainer();
        container.bind(RichContainer.class, container);
        container.bind(DefaultMain.class, this);
        container.bind(ObjectFactory.class, this);
        container.bindProvider(ServletContext.class, new Provider<ServletContext, Desire>() {

            public ServletContext get(Key<ServletContext, Desire> key, Desire an, String data, NativeProvider<?> nativeProvider) {
                return DefaultMain.this.servletContext;
            }
        }, true);
        TemplateResultInterpreter templateResultInterpreter = new TemplateResultInterpreter(this, charset);
        container.bind(TemplateResultInterpreter.class, templateResultInterpreter);
        registerResultType(TemplateResult.class, templateResultInterpreter);
        SimpleResultInterpreter simpleResultInterpreter = new SimpleResultInterpreter();
        container.bind(SimpleResultInterpreter.class, simpleResultInterpreter);
        registerResultType(SimpleResult.class, simpleResultInterpreter);
        filterGroup = root.append(diFilter = new DIFilter(container, charset)).append(exceptionFilter = new LogicExceptionFilter());
    }

    /**
     * 构造方法，使用utf-8编码构造。
     */
    public DefaultMain() {
        this("utf-8");
    }

    /**
     * 通过{@link #container}的{@link cn.webwheel.di.utils.RichContainer#getInstance(Class)}方法返回类型的实例。
     *
     * @param cls 类型
     * @return 实例
     */
    public <T> T getInstanceOf(Class<T> cls) {
        return container.getInstance(cls);
    }

    @Override
    protected void destroy() {
        container = null;
        diFilter = null;
        filterGroup = null;
    }

    /**
     * 映射一个http请求地址到一个动作类，使用{@link #filterGroup}过滤器组。
     *
     * @param path        http请求地址
     * @param actionClass 动作类的类型，将由{@link #getInstanceOf(Class)}创建实例处理相应http请求
     */
    public void bindAction(String path, Class<? extends Action> actionClass) {
        bindAction(path, actionClass, filterGroup);
    }

    /**
     * 使用给定的过滤器组映射一个http请求地址到一个动作类。{@link cn.webwheel.utils.DefaultMain#autoBind(String)} 方法使用此方法。
     *
     * @param path        http请求地址
     * @param actionClass 动作类的类型，将由{@link #getInstanceOf(Class)}创建实例处理相应http请求
     * @param filterGroup 过滤器组
     */
    public void bindAction(String path, Class<? extends Action> actionClass, FilterGroup filterGroup) {
        filterGroup.bindAction(path, actionClass);
    }

    /**
     * 绑定一个http请求地址到一个页面动作类，页面动作类的参考实现为{@link cn.webwheel.utils.DefaultPage}，
     * 也就是一个可以返回页面模板结果的动作类。<br>
     * 并通过{@link cn.webwheel.utils.TemplateResultInterpreter#bindTemplate(Class, String, String)}方法
     * 将页面动作类的类型映射到该页面模板文件位置。
     *
     * @param path      http请求地址，同时也是页面模板文件的位置
     * @param pageClass 页面动作类的类型
     * @throws IllegalArgumentException 该页面模板文件不存在
     */
    public void bindPage(String path, Class<? extends Action> pageClass) {
        bindPage(path, pageClass, filterGroup);
    }

    /**
     * 使用给定的过滤器组绑定一个http请求道一个页面动作类，{@link cn.webwheel.utils.DefaultMain#autoBind(String)} 方法使用此方法。
     * @param path http请求地址
     * @param pageClass 页面动作类
     * @param filterGroup 过滤器组
     */
    public void bindPage(String path, Class<? extends Action> pageClass, FilterGroup filterGroup) {
        bindComponent(pageClass, path);
        bindAction(path, pageClass, filterGroup);
    }

    /**
     * 绑定一个模板组件类到一个模板文件中，{@link cn.webwheel.utils.DefaultMain#autoBind(String)} 方法使用此方法。
     *
     * @param compClass 组件类型
     * @param template  模板文件位置
     * @throws IllegalArgumentException 模板文件不存在
     */
    public void bindComponent(Class compClass, String template) {
        getInstanceOf(TemplateResultInterpreter.class).bindTemplate(compClass, template, charset);
    }

    /**
     * 扫描包下的所有类，按照约定映射Page/Comp/Act类。<br>
     * 以Page/Comp/Act开头为名字的类将分别使用{@link #bindPage(String, Class)} {@link #bindComponent(Class, String)} {@link #bindAction(String, Class)}进行绑定。<br>
     * 映射路径或模板文件路径同包路径一致，并以参数pkg为根路径。<br>
     * 例如传入参数"net.my.web"，已知存在类net.my.web.some.pkg.ActPost和net.my.web.PageIndex，
     * 并且这两个类都实现了{@link cn.webwheel.Action}接口。<br>
     * 则调用autoBind方法后，将在内部执行<br>
     * <code>
     * bindAction("/some/pkg/post.do", ActPost.class);<br>
     * 与<br>
     * bindPage("/index.html", PageIndex.class);
     * </code><br>
     * Act/Comp/Page后的类名字的第一个字母变为小写，Act类的路径追加后缀.do，Comp/Page类的对应路径追加后缀.html
     *
     * @param pkg 包路径
     * @throws IOException io异常
     */
    @SuppressWarnings({ "deprecation" })
    public void autoBind(String pkg) throws IOException {
        Map<String, FilterGroup> filterGroupMap = new HashMap<String, FilterGroup>();
        Enumeration<URL> enm = getClass().getClassLoader().getResources(pkg.replace('.', '/'));
        while (enm.hasMoreElements()) {
            URL url = enm.nextElement();
            if (url.getProtocol().equals("file")) {
                autoBind(pkg.replace('.', '/'), pkg, new File(URLDecoder.decode(url.getFile())), filterGroupMap);
            } else if (url.getProtocol().equals("jar")) {
                String file = URLDecoder.decode(url.getFile());
                String root = file.substring(file.lastIndexOf('!') + 2);
                file = file.substring(0, file.length() - root.length() - 2);
                URL jarurl = new URL(file);
                if (jarurl.getProtocol().equals("file")) {
                    JarFile jarFile = new JarFile(URLDecoder.decode(jarurl.getFile()));
                    try {
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (!name.endsWith(".class")) continue;
                            if (!name.startsWith(root + '/')) continue;
                            name = name.substring(0, name.length() - 6);
                            name = name.replace('/', '.');
                            int i = name.lastIndexOf('.');
                            autoBind(root, name.substring(0, i), name.substring(i + 1), filterGroupMap);
                        }
                    } finally {
                        jarFile.close();
                    }
                }
            }
        }
    }

    private void autoBind(String root, String pkg, File dir, Map<String, FilterGroup> filterGroupMap) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            String fn = file.getName();
            if (file.isDirectory()) {
                autoBind(root, pkg + "." + fn, file, filterGroupMap);
            } else if (fn.endsWith(".class")) {
                autoBind(root, pkg, fn.substring(0, fn.length() - 6), filterGroupMap);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void autoBind(String root, String pkg, String name, Map<String, FilterGroup> filterGroupMap) {
        if (name.startsWith("Act")) {
            if (name.length() > 3) {
                try {
                    Class cls = Class.forName(pkg + "." + name);
                    if (Action.class.isAssignableFrom(cls)) {
                        String path = pkg.substring(root.length()).replace('.', '/') + '/' + name.substring(3, 4).toLowerCase() + name.substring(4) + actExtension;
                        bindAction(path, cls, getFilterGroup(cls, filterGroupMap));
                    }
                } catch (ClassNotFoundException e) {
                }
            }
        } else if (name.startsWith("Comp")) {
            if (name.length() > 4) {
                String path = pkg.substring(root.length()).replace('.', '/') + '/' + name.substring(4, 5).toLowerCase() + name.substring(5) + tempExtension;
                Class cls;
                try {
                    cls = Class.forName(pkg + "." + name);
                } catch (ClassNotFoundException e) {
                    return;
                }
                bindComponent(cls, path);
            }
        } else if (name.startsWith("Page")) {
            if (name.length() > 4) {
                Class cls;
                try {
                    cls = Class.forName(pkg + "." + name);
                } catch (ClassNotFoundException e) {
                    return;
                }
                if (Action.class.isAssignableFrom(cls)) {
                    String path = pkg.substring(root.length()).replace('.', '/') + '/' + name.substring(4, 5).toLowerCase() + name.substring(5);
                    bindPage(path + pageExtension, cls, getFilterGroup(cls, filterGroupMap));
                }
            }
        }
    }

    private FilterGroup getFilterGroup(String pkg, FilterGroup parent) {
        Package pg = Package.getPackage(pkg);
        if (pg == null) return parent;
        Filters fs = pg.getAnnotation(Filters.class);
        if (fs == null) return parent;
        if (fs.fromRoot()) {
            parent = filterGroup;
        }
        for (Class<? extends Filter> fc : fs.value()) {
            parent = parent.append(getInstanceOf(fc));
        }
        return parent;
    }

    private FilterGroup getFilterGroup(String pkg, Map<String, FilterGroup> filterGroupMap) {
        FilterGroup fg = filterGroupMap.get(pkg);
        if (fg != null) return fg;
        int i = pkg.lastIndexOf('.');
        if (i != -1) {
            fg = getFilterGroup(pkg.substring(0, i), filterGroupMap);
        } else {
            fg = filterGroup;
        }
        fg = getFilterGroup(pkg, fg);
        filterGroupMap.put(pkg, fg);
        return fg;
    }

    private FilterGroup getFilterGroup(Class<?> cls, Map<String, FilterGroup> filterGroupMap) {
        FilterGroup fg = getFilterGroup(cls.getPackage().getName(), filterGroupMap);
        Filters f = cls.getAnnotation(Filters.class);
        if (f == null) {
            return fg;
        }
        if (f.fromRoot()) fg = filterGroup;
        for (Class<? extends Filter> fc : f.value()) {
            fg = fg.append(getInstanceOf(fc));
        }
        return fg;
    }

    /**
     * 扫描插件
     *
     * @throws Exception 异常
     */
    @SuppressWarnings("unchecked")
    public void scanPlugins() throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        builder.setEntityResolver(new EntityResolver() {

            public InputSource resolveEntity(String publicId, String systemId) throws SAXException, IOException {
                return new InputSource(new StringReader(""));
            }
        });
        Enumeration<URL> enm = getClass().getClassLoader().getResources("webwheel-plugin.xml");
        while (enm.hasMoreElements()) {
            URL url = enm.nextElement();
            InputStream is = url.openStream();
            Document doc;
            try {
                doc = builder.parse(is);
            } finally {
                is.close();
            }
            String pkg = null;
            Node node = doc.getDocumentElement().getAttributeNode("package");
            if (node != null) {
                pkg = node.getNodeValue();
            }
            NodeList list = doc.getDocumentElement().getElementsByTagName("result-type");
            for (int i = 0; i < list.getLength(); i++) {
                NamedNodeMap as = list.item(i).getAttributes();
                Class type = null;
                node = as.getNamedItem("type");
                if (node == null) {
                    throw new Exception("result-type element need attribute: type at " + url);
                }
                try {
                    type = Class.forName(node.getNodeValue());
                } catch (ClassNotFoundException e) {
                    if (pkg != null) {
                        try {
                            type = Class.forName(pkg + "." + node.getNodeValue());
                        } catch (ClassNotFoundException e2) {
                        }
                    }
                    if (type == null) throw new Exception("can not find class: " + node.getNodeValue() + " at " + url);
                }
                node = as.getNamedItem("interpreter");
                if (node == null) {
                    throw new Exception("result-type element need attribute: interpreter at " + url);
                }
                Class<? extends ResultInterpreter> interpreter = null;
                try {
                    interpreter = (Class<? extends ResultInterpreter>) Class.forName(node.getNodeValue());
                } catch (ClassNotFoundException e) {
                    if (pkg != null) {
                        try {
                            interpreter = (Class<? extends ResultInterpreter>) Class.forName(pkg + "." + node.getNodeValue());
                        } catch (ClassNotFoundException e2) {
                        }
                    }
                    if (interpreter == null) throw new Exception("can not find class: " + node.getNodeValue() + " at " + url);
                }
                if (!ResultInterpreter.class.isAssignableFrom(interpreter)) {
                    throw new Exception(interpreter + " is not result interpreter at " + url);
                }
                registerResultType(type, getInstanceOf(interpreter));
            }
            list = doc.getDocumentElement().getElementsByTagName("component");
            for (int i = 0; i < list.getLength(); i++) {
                NamedNodeMap as = list.item(i).getAttributes();
                Class type = null;
                node = as.getNamedItem("class");
                if (node == null) {
                    throw new Exception("component element need attribute: class at " + url);
                }
                try {
                    type = Class.forName(node.getNodeValue());
                } catch (ClassNotFoundException e) {
                    if (pkg != null) {
                        try {
                            type = Class.forName(pkg + "." + node.getNodeValue());
                        } catch (ClassNotFoundException e2) {
                        }
                    }
                    if (type == null) throw new Exception("can not find class: " + node.getNodeValue() + " at " + url);
                }
                String charset = "utf-8";
                node = as.getNamedItem("charset");
                if (node != null) {
                    charset = node.getNodeValue();
                }
                node = as.getNamedItem("template");
                if (node == null) {
                    throw new Exception("component element need attribute: template at " + url);
                }
                getInstanceOf(TemplateResultInterpreter.class).bindTemplate(type, TemplateResultInterpreter.ClassPath + node.getNodeValue(), charset);
            }
        }
    }
}
