package simtools.ui;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

public class HTMLWriter {

    public static BasicMessageWriter messageWriter = ResourceFinder.getMessages(HTMLWriter.class);

    protected BufferedWriter writer;

    public HTMLWriter(BufferedWriter writer) {
        this.writer = writer;
    }

    public void writeHtmlBegin() throws IOException {
        writer.write(messageWriter.print0args("html"));
    }

    public void writeHtmlEnd() throws IOException {
        writer.write(messageWriter.print0args("/html"));
    }

    public void writeBodyBegin() throws IOException {
        writer.write(messageWriter.print0args("body"));
    }

    public void writeBodyEnd() throws IOException {
        writer.write(messageWriter.print0args("/body"));
    }

    public void writeHeadBegin() throws IOException {
        writer.write(messageWriter.print0args("head"));
    }

    public void writeHeadEnd() throws IOException {
        writer.write(messageWriter.print0args("/head"));
    }

    public void writeTitle(String title) throws IOException {
        writer.write(messageWriter.print1args("title", title));
    }

    public void writeTitleEnd() throws IOException {
        writer.write(messageWriter.print0args("/title"));
    }

    public void writeH1(String title, String link) throws IOException {
        if (link != null) writer.write(messageWriter.print2args("h1Linked", link, title)); else writer.write(messageWriter.print1args("h1", title));
    }

    public void writeFrameSetBegin(String colValue) throws IOException {
        writer.write(messageWriter.print1args("frameSet", colValue));
    }

    public void writeFrameSetEnd() throws IOException {
        writer.write(messageWriter.print0args("/frameSet"));
    }

    public void writeFrame(String name, String src) throws IOException {
        writer.write(messageWriter.print2args("frame", name, src));
    }

    public void writeSeparator() throws IOException {
        writer.write(messageWriter.print0args("separator"));
        writer.write(messageWriter.print0args("space"));
    }

    public void writeH2(String title, String link) throws IOException {
        if (link != null) writer.write(messageWriter.print2args("h2Linked", link, title)); else writer.write(messageWriter.print1args("h2", title));
    }

    /**
     * @param imagePath
     * @param title
     * @throws IOException
     */
    public void writeImage(String imagePath, String title) throws IOException {
        writer.write(messageWriter.print2args("image", imagePath, title));
    }

    /**
     * Write HTML code for an image which contains zones linked to targets
     * @param imagePath
     * @param title
     * @param imageMap
     * @param targets. List of targets. A target is an array of 5 components : left-x, top-y, right-x, bottom-y and target name
     * @throws IOException
     */
    public void writeImage(String imagePath, String title, String imageMap, String[][] targets) throws IOException {
        writer.write(messageWriter.print1args("map", imageMap));
        for (int i = 0; i < targets.length; i++) {
            writer.write(messageWriter.printNargs("area", targets[i]));
        }
        writer.write(messageWriter.print0args("/map"));
        Object[] args = { imagePath, imageMap, title };
        writer.write(messageWriter.printNargs("imageMaped", args));
    }

    /**
     * Write HTML code for a paragraph.
     * @param par
     * @param target
     * @throws IOException
     */
    public void writeP(String par, String link) throws IOException {
        if (link != null) writer.write(messageWriter.print2args("pLinked", link, par)); else writer.write(messageWriter.print1args("p", par));
    }

    public void writeP(String par) throws IOException {
        writer.write(messageWriter.print1args("p", par));
    }

    public void writeULBegin() throws IOException {
        writer.write(messageWriter.print0args("ul"));
    }

    public void writeULEnd() throws IOException {
        writer.write(messageWriter.print0args("/ul"));
    }

    public void writeNoframesBegin() throws IOException {
        writer.write(messageWriter.print0args("noframes"));
    }

    public void writeNoframesEnd() throws IOException {
        writer.write(messageWriter.print0args("/noframes"));
    }

    public void writeLiBegin() throws IOException {
        writer.write(messageWriter.print0args("li"));
    }

    public void writeLiEnd() throws IOException {
        writer.write(messageWriter.print0args("/li"));
    }

    public void writeTargetName(String target) throws IOException {
        writer.write(messageWriter.print1args("targetName", target));
    }

    public void writeTargetReference(String reference, String targetedText) throws IOException {
        writer.write(messageWriter.print2args("targetReference", reference, targetedText));
    }

    public void writeTargetReference(String reference, String target, String targetedText) throws IOException {
        Object[] args = new Object[3];
        args[0] = reference;
        args[1] = target;
        args[2] = targetedText;
        writer.write(messageWriter.printNargs("targetReference", args));
    }

    public void writeH3(String title, String link) throws IOException {
        if (link != null) writer.write(messageWriter.print2args("h3Linked", link, title)); else writer.write(messageWriter.print1args("h3", title));
    }

    /**
     * Write HTML code for an array.
     * @param titles, list of titles
     * @param contents, array contents
     * @throws IOException
     */
    public void writeTab(String[] titles, ArrayList contents) throws IOException {
        writer.write(messageWriter.print0args("table"));
        writer.write(messageWriter.print0args("thread"));
        writer.write(messageWriter.print0args("trColored"));
        for (int i = 0; i < titles.length; i++) {
            writer.write(messageWriter.print1args("td", titles[i]));
        }
        writer.write(messageWriter.print0args("/tr"));
        writer.write(messageWriter.print0args("/thread"));
        writer.write(messageWriter.print0args("body"));
        for (int i = 0; i < contents.size(); i++) {
            writer.write(messageWriter.print0args("tr"));
            String property[] = (String[]) contents.get(i);
            for (int j = 0; j < property.length; j++) {
                writer.write(messageWriter.print1args("td", property[j]));
            }
            writer.write(messageWriter.print0args("/tr"));
        }
        writer.write(messageWriter.print0args("/body"));
        writer.write(messageWriter.print0args("/table"));
    }

    /**
     * Write file contents
     * @param file
     */
    public void writeFile(URL url) throws IOException, FileNotFoundException {
        BufferedReader readBuffer = new BufferedReader(new InputStreamReader(url.openStream()));
        char buffer[] = new char[512 * 1024];
        int readNumber;
        while ((readNumber = readBuffer.read(buffer)) != -1) {
            writer.write(buffer, 0, readNumber);
        }
        readBuffer.close();
    }
}
