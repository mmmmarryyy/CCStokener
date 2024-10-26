package org.xul.samples.groovy;

import groovy.lang.GroovyClassLoader;
import groovy.lang.GroovyObject;
import groovy.lang.MetaMethod;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.xul.script.scripts.AbstractScript;

/** A Groovy Script.
 *
 * @version 0.4.4
 */
public class GroovyScript extends AbstractScript {

    public static final String MIME = "text/groovy";

    private GroovyObject groovyObject = null;

    private Map<String, MetaMethod> declaredMethods = new HashMap(10);

    public GroovyScript(URL url) {
        super(MIME, url);
    }

    public GroovyObject getGroovyObject() {
        return groovyObject;
    }

    @Override
    public Set<String> getDeclaredMethods() {
        return declaredMethods.keySet();
    }

    @Override
    public Object evalMethod(String name, Object[] args) throws Exception {
        if (declaredMethods.containsKey(name)) {
            MetaMethod method = declaredMethods.get(name);
            return method.doMethodInvoke(groovyObject, args);
        } else {
            throw new Exception("Impossible to find Groovy method of name " + name);
        }
    }

    @Override
    public Object init() throws Exception {
        if (url != null) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            ClassLoader parent = getClass().getClassLoader();
            GroovyClassLoader loader = new GroovyClassLoader(parent);
            Class groovyClass = loader.parseClass(new File(url.getFile()));
            groovyObject = (GroovyObject) groovyClass.newInstance();
            reader.close();
            initDeclaredMethods();
            return groovyObject;
        } else return null;
    }

    private void initDeclaredMethods() {
        List<MetaMethod> methods = groovyObject.getMetaClass().getMethods();
        Iterator<MetaMethod> it = methods.iterator();
        while (it.hasNext()) {
            MetaMethod m = it.next();
            declaredMethods.put(m.getName(), m);
        }
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
