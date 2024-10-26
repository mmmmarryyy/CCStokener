package net.viens.numenor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import net.viens.numenor.Queue;

/**
 * @author Steve Viens
 */
public class SerializerTest {

    static {
        System.setProperty("log4j.configuration", "test-log4j.properties");
    }

    public static void main(String[] args) throws Exception {
        readFileWriteFileTest("data/jobs.xml", "data/jobs_" + System.currentTimeMillis() + ".xml");
    }

    public static void readFileTest(String fileName) throws Exception {
        System.out.println("Initiated reading local queue file: " + fileName);
        InputStream instream = new FileInputStream(fileName);
        Serializer serializer = new Serializer();
        Queue queue = (Queue) serializer.parse(instream);
        instream.close();
        System.out.println("Completed reading local queue file (jobs=" + queue.size() + ")");
    }

    public static void readUrlTest(String url) throws Exception {
        System.out.println("Initiated reading remote queue URL: " + url);
        InputStream instream = new URL(url).openStream();
        Serializer serializer = new Serializer();
        Queue queue = (Queue) serializer.parse(instream);
        instream.close();
        System.out.println("Completed reading remote queue URL (jobs=" + queue.size() + ")");
    }

    public static void readFileWriteFileTest(String fromFile, String toFile) throws Exception {
        System.out.println("Initiated reading source queue File: " + fromFile);
        InputStream instream = new FileInputStream(fromFile);
        Serializer serializer = new Serializer();
        Response response = (Response) serializer.parse(instream);
        Queue queue = response.getQueue();
        instream.close();
        System.out.println("Completed reading source queue URL (jobs=" + queue.size() + ")");
        System.out.println("Initiated writing target queue File: " + toFile);
        OutputStream outstream = new FileOutputStream(toFile);
        serializer.write(response, outstream);
        outstream.close();
        System.out.println("Completed writing target queue file.");
    }

    public static void readUrlWriteFileTest(String url, String fileName) throws Exception {
        System.out.println("Initiated reading source queue URL: " + url);
        InputStream instream = new URL(url).openStream();
        Serializer serializer = new Serializer();
        Response response = (Response) serializer.parse(instream);
        Queue queue = response.getQueue();
        instream.close();
        System.out.println("Completed reading source queue URL (jobs=" + queue.size() + ")");
        System.out.println("Initiated writing target queue File: " + fileName);
        OutputStream outstream = new FileOutputStream(fileName);
        serializer.write(response, outstream);
        outstream.close();
        System.out.println("Completed writing target queue file.");
    }
}
