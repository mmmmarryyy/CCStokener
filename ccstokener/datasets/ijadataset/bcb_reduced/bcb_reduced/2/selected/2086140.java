package org.swingerproject.script;

import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.lang.Script;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.codehaus.groovy.control.CompilationFailedException;
import org.swingerproject.Swinger;
import org.swingerproject.exceptions.SwingerException;

public class ScriptLoader implements Runnable {

    private static GroovyShell shell = new GroovyShell();

    private Script script;

    private InputStream input;

    private boolean success;

    public void run() {
        success = false;
        try {
            script = shell.parse(input);
        } catch (CompilationFailedException e) {
            throw new SwingerException(e.getMessage());
        }
        Binding binding = new Binding();
        binding.setVariable("swinger", Swinger.getInstance());
        script.setBinding(binding);
        success = true;
    }

    public static Script load(String resource) throws IOException {
        return load(ClassLoader.getSystemResourceAsStream(resource));
    }

    public static Script load(File file) throws IOException {
        return load(new FileInputStream(file));
    }

    public static Script load(URL url) throws IOException {
        return load(url.openStream());
    }

    public static Script load(InputStream input) throws IOException {
        return load(input, 3000);
    }

    public static Script load(InputStream input, long timeout) throws IOException {
        ScriptLoader loader = new ScriptLoader();
        loader.input = input;
        Thread thread = new Thread(loader);
        thread.start();
        try {
            thread.join(timeout);
        } catch (InterruptedException e) {
        }
        input.close();
        if (loader.success) {
            return loader.script;
        } else {
            throw new SwingerException("Script loading failed");
        }
    }

    public Script getScript() {
        return script;
    }

    public void setInput(InputStream input) {
        this.input = input;
    }

    public boolean isSuccess() {
        return success;
    }
}
