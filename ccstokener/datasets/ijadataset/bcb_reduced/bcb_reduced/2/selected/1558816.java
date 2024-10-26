package org.andrewberman.ui.unsorted;

import java.applet.Applet;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;

public class JSCaller {

    private Applet app;

    public boolean reflectionWorking = true;

    Method getWindow = null;

    Method eval = null;

    Method call = null;

    Method setMember = null;

    Method getMember = null;

    Object jsObject = null;

    public JSCaller(Applet app) {
        this.app = app;
        try {
            initialize();
        } catch (Exception e) {
            return;
        }
    }

    private void initialize() {
        if (!reflectionWorking) {
        }
        if (jsObject != null) return;
        try {
            ClassLoader cl = app.getClass().getClassLoader();
            Class c = cl.loadClass("netscape.javascript.JSObject");
            Method methods[] = c.getMethods();
            for (Method m : methods) {
                if (m.getName().compareTo("getWindow") == 0) getWindow = m; else if (m.getName().compareTo("eval") == 0) eval = m; else if (m.getName().compareTo("call") == 0) call = m; else if (m.getName().compareTo("setMember") == 0) setMember = m; else if (m.getName().compareTo("getMember") == 0) getMember = m;
            }
            jsObject = getWindow.invoke(c, app);
            reflectionWorking = true;
        } catch (Exception e) {
            reflectionWorking = false;
            throw new RuntimeException("JS reflection failed -- maybe we're not inside a browser?");
        }
    }

    public synchronized void injectJavaScript(String file) {
        initialize();
        try {
            InputStream in = openStreamRaw(file);
            BufferedReader read = new BufferedReader(new InputStreamReader(in));
            StringBuffer buff = new StringBuffer();
            String s;
            while ((s = read.readLine()) != null) {
                buff.append(s.trim());
            }
            eval(buff.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public synchronized Object eval(String expression) throws Exception {
        initialize();
        Object result = eval.invoke(jsObject, expression);
        return null;
    }

    public synchronized Object call(String methodName, Object... args) throws Exception {
        return callWithObject(jsObject, methodName, args);
    }

    public synchronized Object call(String methodName) throws Exception {
        return callWithObject(jsObject, methodName);
    }

    public synchronized Object callWithObject(Object object, String methodName, Object... args) throws Exception {
        initialize();
        Object result = call.invoke(object, methodName, args);
        return result;
    }

    public synchronized void setMember(String memberName, Object value) throws Exception {
        initialize();
        setMember.invoke(jsObject, memberName, value);
    }

    public synchronized Object getMember(String memberName) throws Exception {
        initialize();
        Object result = getMember.invoke(jsObject, memberName);
        return result;
    }

    public synchronized Object getWindow() {
        initialize();
        return jsObject;
    }

    private InputStream openStreamRaw(String filename) {
        InputStream stream = null;
        if (filename == null) return null;
        if (filename.length() == 0) {
            return null;
        }
        try {
            URL url = new URL(filename);
            stream = url.openStream();
            return stream;
        } catch (MalformedURLException mfue) {
        } catch (FileNotFoundException fnfe) {
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        ClassLoader cl = getClass().getClassLoader();
        stream = cl.getResourceAsStream("data/" + filename);
        if (stream != null) {
            String cn = stream.getClass().getName();
            if (!cn.equals("sun.plugin.cache.EmptyInputStream")) {
                return stream;
            }
        }
        stream = cl.getResourceAsStream(filename);
        if (stream != null) {
            String cn = stream.getClass().getName();
            if (!cn.equals("sun.plugin.cache.EmptyInputStream")) {
                return stream;
            }
        }
        return stream;
    }
}
