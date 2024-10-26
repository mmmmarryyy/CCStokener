package com.abiquo.sdk;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Vector;
import com.abiquo.sdk.output.AbstractOutput;

/**
 * Parses config from console arguments.
 */
public class Config {

    public static enum InputStreamType {

        STANDARD_INPUT, FILE_PATH, URL
    }

    private Vector<AbstractOutput> outputs;

    private InputStream inputStream;

    private File outputDirectory;

    private boolean overwrite;

    public Config() {
        outputs = new Vector<AbstractOutput>();
        outputDirectory = new File(".");
        overwrite = false;
    }

    public Vector<AbstractOutput> getOutputs() {
        return outputs;
    }

    public void setOutputs(Vector<AbstractOutput> outputs) {
        this.outputs = outputs;
    }

    public void addOutput(AbstractOutput abstractOutput) {
        abstractOutput.setConfig(this);
        outputs.add(abstractOutput);
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void setInputStream(InputStreamType type) throws IOException {
        setInputStream(type, null);
    }

    public void setInputStream(InputStreamType type, String value) throws IOException {
        switch(type) {
            case STANDARD_INPUT:
                inputStream = System.in;
                break;
            case FILE_PATH:
                inputStream = new FileInputStream(value);
                break;
            case URL:
                URL url = new URL(value);
                URLConnection urlConn = url.openConnection();
                inputStream = urlConn.getInputStream();
                break;
            default:
                throw new IllegalArgumentException("Invalid input stream type");
        }
    }

    public InputStream getResourceClassInputStream() {
        return inputStream;
    }

    public void setOutputDirectory(String path) {
        outputDirectory = new File(path);
        if (!outputDirectory.exists()) {
            outputDirectory.mkdir();
        }
    }

    public File getOutputDirectory() throws FileNotFoundException {
        return outputDirectory;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }

    public boolean getOverwrite() {
        return overwrite;
    }
}
