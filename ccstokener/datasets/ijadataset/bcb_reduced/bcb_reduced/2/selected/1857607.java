package loader;

import lxl.net.Clean;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import org.xml.sax.SAXException;

/**
 * This class is intended to be called via the 'Main-Class' attribute
 * of an application jar file manifest.  This application jar file
 * must contain the classes in this package, and a top level resource
 * named "main.jnlp" which drives the application loading and launch.
 * 
 * @author jdp
 */
public class Main extends lxl.net.ClassLoader implements Runnable {

    public static final String Resource = "main.jnlp";

    public static void main(String[] argv) {
        try {
            Main main = new Main(argv);
            main.run();
        } catch (Exception exc) {
            exc.printStackTrace();
            System.exit(1);
        }
    }

    protected final Jnlp jnlp;

    /**
     * @param argv Main function arguments.
     */
    public Main(String[] argv) throws IOException, SAXException {
        this(null, argv);
    }

    /**
     * @param dir Provide a static directory name or default to jnlp href name.
     * @param argv Main function arguments.
     */
    public Main(String dir, String[] argv) throws IOException, SAXException {
        super(dir, argv);
        InputStream in;
        if (this.hasMainArg(0)) {
            String arg = this.getMainArg(0);
            try {
                URL url = new URL(arg);
                in = url.openStream();
            } catch (MalformedURLException exc) {
                File file = new File(arg);
                if (file.exists() && file.isFile()) in = new FileInputStream(file); else {
                    in = this.getResourceAsStream(arg);
                    if (null == in) in = this.getResourceAsStream(Resource);
                }
            }
        } else {
            in = this.getResourceAsStream(Resource);
        }
        if (null != in) {
            Jnlp jnlp;
            try {
                jnlp = new Jnlp("loader:" + Main.Resource, in);
                Main.LoadTestDebug();
            } finally {
                in.close();
            }
            this.base = jnlp.getCodebaseUrl();
            if (null == dir) {
                dir = jnlp.getHrefBase();
                this.temp = new File(dir);
                this.cache = new File(this.temp, "cache");
            }
            if ((!this.temp.exists() && (!this.temp.mkdirs()))) throw new IllegalStateException("Unable to create directory '" + this.temp.getAbsolutePath() + "'."); else if ((!this.cache.exists() && (!this.cache.mkdirs()))) throw new IllegalStateException("Unable to create directory '" + this.cache.getAbsolutePath() + "'."); else {
                if (this.clean) {
                    Clean cleaner = this.cleanTemp();
                    cleaner.run();
                    if (this.cleanOnly) {
                        this.jnlp = null;
                        return;
                    } else this.jnlp = jnlp;
                } else this.jnlp = jnlp;
                jnlp.copyMain(this.getResourceAsStream(Resource));
            }
        } else throw new IllegalStateException("Resource not found, '" + Resource + "'.");
    }

    public void run() {
        Jnlp jnlp = this.jnlp;
        if (null != jnlp) {
            try {
                this.jnlp.init(this);
                this.runJnlpCheck();
                this.jnlp.main();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    protected boolean runInitJnlpAccept(Jnlp jnlp, Extension ext) {
        return true;
    }

    protected boolean runInitJnlpAccept(Jnlp jnlp, Jar ext) {
        return true;
    }

    protected boolean runInitJnlpAccept(Jnlp jnlp, Nativelib ext) {
        return true;
    }

    protected void runJnlpCheck() {
    }

    protected String findLibrary(String basename) {
        if (null != this.jnlp) {
            String filename = System.mapLibraryName(basename);
            File file = new File(this.cache, filename);
            if (this.jnlp.usingNative(basename, filename, file)) return file.getAbsolutePath();
        }
        return null;
    }

    protected Class<?> findClass(String biname) throws ClassNotFoundException {
        if (null != this.jnlp) {
            String filepath = biname.replace('.', '/') + ".class";
            File file = new File(this.cache, filepath);
            if (this.jnlp.usingShared(filepath, file)) {
                byte[] clab = ContentOf(file);
                if (null != clab) return this.defineClass(biname, clab, 0, clab.length);
            }
        }
        throw new ClassNotFoundException(biname);
    }

    protected URL findResource(String filepath) {
        if (null != this.jnlp) {
            File file = new File(this.cache, filepath);
            if (this.jnlp.usingShared(filepath, file)) {
                try {
                    return file.toURL();
                } catch (java.net.MalformedURLException exc) {
                    exc.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }
}
