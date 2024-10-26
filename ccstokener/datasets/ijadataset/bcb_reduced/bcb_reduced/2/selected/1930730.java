package net.sf.elm_ve.elm;

import java.io.*;
import java.util.*;
import java.net.*;
import org.w3c.dom.*;
import java.rmi.*;
import java.util.jar.*;

class ElmClassLoader extends ClassLoader {

    protected String elmClassPath[] = new String[0];

    protected String exportElmClassPath[] = new String[0];

    protected String jdkHome = "none";

    protected ElmClassLoader() {
        super();
    }

    protected ElmClassLoader(ClassLoader cl) {
        super(cl);
    }

    public Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] b = loadClassData(name);
        if (b == null) throw new ClassNotFoundException();
        return defineClass(name, b, 0, b.length);
    }

    byte[] loadClassData(String name) {
        ArrayList<String> al = new ArrayList<String>();
        al.add(ElmVE.elmVE.baseDir.getAbsolutePath() + File.separator + "outerClass");
        for (int i = 0; i < elmClassPath.length; i++) al.add(elmClassPath[i]);
        File f = new File(ElmVE.elmVE.baseDir, "extJars");
        File ff[] = f.listFiles();
        for (int i = 0; i < ff.length; i++) al.add(ff[i].getAbsolutePath());
        if (!jdkHome.equals("none")) al.add(jdkHome + File.separator + "lib" + File.separator + "tools.jar");
        String paths[] = al.toArray(new String[0]);
        byte[] b = null;
        for (int i = 0; i < paths.length; i++) {
            if (paths[i].endsWith(".jar")) {
                b = loadClassDataFromJarFile(new File(paths[i]), name);
                if (b != null) return b; else continue;
            } else if (paths[i].startsWith("//")) {
                if (ElmVE.elmVE.config.elmBridge.equals("none")) b = null; else b = null;
                if (b != null) return b; else continue;
            } else {
                b = loadClassDataFromFile(new File(paths[i]), name);
                if (b != null) return b; else continue;
            }
        }
        return null;
    }

    byte[] loadClassDataFromJarFile(File jarFile, String name) {
        try {
            name = name.replace('.', '/');
            JarFile jf = new JarFile(jarFile);
            JarEntry je = jf.getJarEntry(name + ".class");
            if (je == null) return null;
            InputStream is = jf.getInputStream(je);
            BufferedInputStream bis = new BufferedInputStream(is);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte b[] = new byte[1024];
            while (true) {
                int len = bis.read(b);
                if (len < 0) {
                    break;
                }
                baos.write(b, 0, len);
            }
            return baos.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    byte[] loadClassDataFromFile(File classPath, String name) {
        try {
            name = name.replace('.', File.separatorChar);
            File f = new File(classPath, name + ".class");
            if (!f.isFile()) return null;
            long l = f.length();
            byte b[] = new byte[(int) l];
            FileInputStream fis = new FileInputStream(f);
            fis.read(b);
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    byte[] elmLoadClassData(String name) {
        byte[] b = null;
        for (int i = 0; i < exportElmClassPath.length; i++) {
            if (exportElmClassPath[i].endsWith(".jar")) {
                b = loadClassDataFromJarFile(new File(exportElmClassPath[i]), name);
                if (b != null) return b; else continue;
            } else {
                b = loadClassDataFromFile(new File(exportElmClassPath[i]), name);
                if (b != null) return b; else continue;
            }
        }
        return null;
    }

    protected URL findResource(String name) {
        ArrayList<String> al = new ArrayList<String>();
        al.add(ElmVE.elmVE.baseDir.getAbsolutePath() + File.separator + "outerClass");
        for (int i = 0; i < elmClassPath.length; i++) al.add(elmClassPath[i]);
        File f = new File(ElmVE.elmVE.baseDir, "extJars");
        File ff[] = f.listFiles();
        for (int i = 0; i < ff.length; i++) al.add(ff[i].getAbsolutePath());
        if (!jdkHome.equals("none")) al.add(jdkHome + File.separator + "lib" + File.separator + "tools.jar");
        String paths[] = al.toArray(new String[0]);
        URL url = null;
        for (int i = 0; i < paths.length; i++) {
            if (paths[i].endsWith(".jar")) {
                url = findResourceFromJarFile(new File(paths[i]), name);
                if (url != null) return url; else continue;
            } else if (paths[i].startsWith("//")) {
                if (ElmVE.elmVE.config.elmBridge.equals("none")) url = null; else url = null;
                if (url != null) return url; else continue;
            } else {
                url = findResourceFromFile(new File(paths[i]), name);
                if (url != null) return url; else continue;
            }
        }
        return null;
    }

    URL findResourceFromJarFile(File jarFile, String name) {
        try {
            JarFile jf = new JarFile(jarFile);
            if (name.startsWith("/")) name = name.substring(1);
            JarEntry je = jf.getJarEntry(name);
            if (je == null) return null;
            URL url = new URL("jar:" + "file:" + jarFile.getAbsolutePath() + "!/" + name);
            return url;
        } catch (Exception e) {
            return null;
        }
    }

    URL findResourceFromFile(File classPath, String name) {
        try {
            if (name.startsWith("/")) name = name.substring(1);
            File f = new File(classPath, name);
            if (!f.isFile()) return null;
            URL ret = f.toURL();
            if (ret != null) return ret; else return null;
        } catch (Exception e) {
            return null;
        }
    }

    byte[] elmFindResource(String name) {
        try {
            URL url = null;
            for (int i = 0; i < exportElmClassPath.length; i++) {
                if (exportElmClassPath[i].endsWith(".jar")) {
                    url = findResourceFromJarFile(new File(exportElmClassPath[i]), name);
                    if (url != null) break; else continue;
                } else {
                    url = findResourceFromFile(new File(exportElmClassPath[i]), name);
                    if (url != null) break; else continue;
                }
            }
            if (url == null) return null;
            URLConnection c = url.openConnection();
            int i = c.getContentLength();
            byte b[] = new byte[i];
            InputStream is = c.getInputStream();
            is.read(b);
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    protected void setElmClassPath(ArrayList<String> cp) {
        String cps[] = new String[cp.size()];
        Iterator<String> i = cp.iterator();
        int j = 0;
        while (i.hasNext()) {
            cps[j] = i.next();
            j++;
        }
        elmClassPath = cps;
    }

    protected ArrayList<String> getElmClassPath() {
        ArrayList<String> ret = new ArrayList<String>();
        for (int i = 0; i < elmClassPath.length; i++) ret.add(elmClassPath[i]);
        return ret;
    }

    protected void setExportElmClassPath(ArrayList<String> cp) {
        String cps[] = new String[cp.size()];
        Iterator<String> i = cp.iterator();
        int j = 0;
        while (i.hasNext()) {
            cps[j] = i.next();
            j++;
        }
        exportElmClassPath = cps;
    }

    protected ArrayList<String> getExportElmClassPath() {
        ArrayList<String> ret = new ArrayList<String>();
        for (int i = 0; i < exportElmClassPath.length; i++) ret.add(exportElmClassPath[i]);
        return ret;
    }

    void setJDKHome(String s) {
        jdkHome = s;
    }

    String getJDKHome() {
        return jdkHome;
    }

    void addClassPath(String s) {
        for (int i = 0; i < elmClassPath.length; i++) {
            if (elmClassPath[i].equals(s)) return;
        }
        String newElmClassPath[] = new String[elmClassPath.length + 1];
        for (int i = 0; i < elmClassPath.length; i++) {
            newElmClassPath[i] = elmClassPath[i];
        }
        newElmClassPath[elmClassPath.length] = s;
        elmClassPath = newElmClassPath;
    }

    void delClassPath(String s) {
        boolean b = false;
        for (int i = 0; i < elmClassPath.length; i++) {
            if (elmClassPath[i].equals(s)) {
                b = true;
                break;
            }
        }
        if (b == false) return;
        String newElmClassPath[] = new String[elmClassPath.length - 1];
        int ii = 0;
        for (int i = 0; i < elmClassPath.length; i++) {
            if (elmClassPath[i].equals(s)) {
                continue;
            } else {
                newElmClassPath[ii] = elmClassPath[i];
                ii++;
            }
        }
        elmClassPath = newElmClassPath;
    }
}
