import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import javax.xml.transform.*;
import javax.xml.transform.stream.*;
import java.io.StringWriter;
import java.io.StringReader;
import javax.swing.event.*;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.Desktop;

public class NewsScrollPanel extends JScrollPane implements HyperlinkListener {

    private JEditorPane $jEditor;

    private Gui $parent = null;

    private boolean $feedIsHTML = true;

    private String $NewsFeedUrl = null;

    private String $NewsFeedFile = null;

    private int $MaxItemCountShown = 5;

    private String $currentNewsFeedContent = null;

    private String $currentHTMLFeedContent = null;

    public NewsScrollPanel(Gui parent, String newsFeedUrl, String newsFeedFile) {
        $NewsFeedUrl = newsFeedUrl;
        $NewsFeedFile = newsFeedFile;
        $parent = parent;
        $currentNewsFeedContent = readURLContent($NewsFeedFile);
        if ($currentNewsFeedContent == null) {
            $currentNewsFeedContent = "No news yet";
        }
        $currentHTMLFeedContent = generateHTMLCode($currentNewsFeedContent);
        $jEditor = new JEditorPane();
        $jEditor.setContentType("text/html");
        $jEditor.setEditable(false);
        $jEditor.setText($currentHTMLFeedContent);
        $jEditor.addHyperlinkListener(this);
        setViewportView($jEditor);
        (new Timer()).schedule(new TimerTask() {

            public void run() {
                update();
            }
        }, 0, 300000);
    }

    public void update() {
        String newData = null;
        try {
            newData = readURLContent(new URL($NewsFeedUrl));
        } catch (Exception e) {
            e.printStackTrace();
        }
        if ($currentNewsFeedContent == null || (newData != null && !$currentNewsFeedContent.equals(newData))) {
            $currentNewsFeedContent = newData;
            $currentHTMLFeedContent = generateHTMLCode($currentNewsFeedContent);
            $jEditor.setText($currentHTMLFeedContent);
            if ($parent != null) {
                $parent.displayTrayMessage("Palantir News", "The news section was updated");
            }
            writeToFile(newData, $NewsFeedFile);
        }
    }

    private void writeToFile(String data, String filename) {
        try {
            FileWriter fw = new FileWriter(filename);
            fw.write(data, 0, data.length());
            fw.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String readURLContent(String location) {
        try {
            return readURLContent(new URL("file://localhost/" + location));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String readURLContent(URL url) {
        String output = "";
        try {
            InputStream in = url.openStream();
            byte[] buffer = new byte[102400];
            int numRead;
            int fileSize = 0;
            while ((numRead = in.read(buffer)) != -1) {
                output += new String(buffer, 0, numRead);
                fileSize += numRead;
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return output;
    }

    private String generateHTMLCode(String data) {
        if (data == null || data.equals("")) {
            return "<h1>There was a problem reading the News feed</h1>";
        }
        if ($feedIsHTML) {
            return data;
        }
        String out = null;
        try {
            StreamSource inputData = new StreamSource(new StringReader(data));
            StreamSource inputDataXSL = new StreamSource(new StringReader(data.replace('\n', ' ')));
            TransformerFactory factory = TransformerFactory.newInstance();
            Source s = factory.getAssociatedStylesheet(inputDataXSL, null, null, null);
            Transformer transformer = factory.newTransformer(s);
            StringWriter outputString = new StringWriter();
            StreamResult output = new StreamResult(outputString);
            transformer.transform(inputData, output);
            out = outputString.toString();
        } catch (Exception e) {
            out = null;
        }
        System.out.println(out);
        return out;
    }

    public String getNewsData() {
        return $currentNewsFeedContent;
    }

    public String getHTMLData() {
        return $currentHTMLFeedContent;
    }

    public void hyperlinkUpdate(HyperlinkEvent evt) {
        if (evt.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            try {
                Debug.log("NewsScrollPanel", "Link clicked: " + evt.getURL());
                Desktop.getDesktop().browse(evt.getURL().toURI());
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        }
    }

    public static void main(String args[]) {
        NewsScrollPanel a = new NewsScrollPanel(null, "http://palantir.kulnet.kuleuven.be/test.html", "file://localhost/tmp/pom.xml");
        String s = a.readURLContent(args[0]);
        String out = a.generateHTMLCode(s);
        System.out.println(out);
    }
}
