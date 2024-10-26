package org.swingerproject.script;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.io.InputStream;
import java.net.URL;
import org.swingerproject.Swinger;

/**
 * 
 * @author hadrien.fortin
 * @deprecated
 */
public class GroovyScript {

    private static GroovyShell shell = new GroovyShell();

    private String src;

    public void load() {
        new Thread() {

            @Override
            public void run() {
                try {
                    URL url = null;
                    if (src.startsWith("http://") || src.startsWith("file:")) url = new URL(src); else url = new URL(Swinger.getInstance().getContext().getBaseUrl() + '/' + src);
                    InputStream in = url.openStream();
                    Script script = shell.parse(in);
                    in.close();
                    Binding binding = new Binding();
                    binding.setVariable("swinger", Swinger.getInstance());
                    script.setBinding(binding);
                } catch (Exception e) {
                } finally {
                }
            }
        }.start();
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }
}
