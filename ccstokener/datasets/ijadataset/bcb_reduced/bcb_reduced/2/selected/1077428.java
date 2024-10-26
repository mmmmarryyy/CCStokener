package com.memomics.cytoscape_plugin.tasks;

import giny.model.Edge;
import giny.model.GraphObject;
import giny.model.Node;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import com.memomics.cytoscape_plugin.conf.Configurator;
import com.memomics.cytoscape_plugin.model.Application;
import com.memomics.cytoscape_plugin.model.Attributes;
import com.memomics.cytoscape_plugin.panels.MemomicsJSPBrowser;
import cytoscape.Cytoscape;
import cytoscape.task.Task;
import cytoscape.task.TaskMonitor;

public class ShowInteractionsTask implements Task {

    private static final long serialVersionUID = -5769255813455829766L;

    private GraphObject graphObj = null;

    TaskMonitor taskMonitor;

    private boolean isNormalized;

    private boolean isCancelled = false;

    public String getTitle() {
        return "Show interactions ...";
    }

    public ShowInteractionsTask(GraphObject graphObj, boolean isNormalized) {
        super();
        this.graphObj = graphObj;
        this.isNormalized = isNormalized;
    }

    public void halt() {
        isCancelled = true;
    }

    public void run() {
        taskMonitor.setPercentCompleted(-1);
        taskMonitor.setStatus("Preparing request...");
        if (graphObj == null) {
            System.out.println("No graph object!");
            return;
        }
        MemomicsJSPBrowser browser;
        String broswerTitle = null;
        String url = null;
        String interactionsURLTemplate = Configurator.getInteractionsURLTemplate();
        if (graphObj instanceof Node) {
            Node node = (Node) graphObj;
            String umis = Cytoscape.getNodeAttributes().getStringAttribute(node.getIdentifier(), Attributes.umis);
            if (umis == null) {
                System.out.println("No umis for node " + node.getIdentifier() + "!");
            } else {
                System.out.println("Umis: " + umis);
            }
            String use_umis = "false";
            if (isNormalized) {
                use_umis = "true";
            }
            url = interactionsURLTemplate.replaceFirst("@umis@", umis);
            url = url.replaceFirst("@request_type@", "node_interactions");
            url = url.replaceFirst("@use_umis@", use_umis);
            String sessionID = Application.get().getSessionID();
            if (sessionID != null) {
                url = url + "&session_id=" + sessionID;
            }
            System.out.println("Interactions URL: " + url);
            String canonicalName = Cytoscape.getNodeAttributes().getStringAttribute(node.getIdentifier(), Attributes.canonicalName);
            broswerTitle = "Interactions of " + canonicalName + " (" + umis + ")";
        }
        if (graphObj instanceof Edge) {
            Edge edge = (Edge) graphObj;
            String umis = Cytoscape.getEdgeAttributes().getStringAttribute(edge.getIdentifier(), Attributes.umis);
            if (umis == null) {
                System.out.println("No umis for edge " + edge.getIdentifier() + "!");
                return;
            } else {
                System.out.println("Umis: " + umis);
            }
            String use_umis = "true";
            if (!isNormalized) {
                System.out.println("Keyword interactions search for edges not supported.");
                return;
            }
            url = interactionsURLTemplate.replaceFirst("@umis@", umis);
            url = url.replaceFirst("@request_type@", "edge_interactions");
            url = url.replaceFirst("@use_umis@", use_umis);
            String sessionID = Application.get().getSessionID();
            if (sessionID != null) {
                url = url + "&session_id=" + sessionID;
            }
            System.out.println("Interactions URL: " + url);
            String canonicalName = Cytoscape.getEdgeAttributes().getStringAttribute(edge.getIdentifier(), Attributes.canonicalName);
            broswerTitle = "Interactions of " + canonicalName + " (" + umis + ")";
        }
        taskMonitor.setStatus("Connecting to the web server...");
        URL url2;
        String html = "";
        try {
            url2 = new URL(url);
            URLConnection urlconnection;
            urlconnection = url2.openConnection();
            taskMonitor.setStatus("Downloading content...");
            taskMonitor.setPercentCompleted(0);
            urlconnection.setUseCaches(false);
            InputStream is = urlconnection.getInputStream();
            int available = is.available();
            int read = 0;
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader bufRead = new BufferedReader(isr);
            String aLine = null;
            do {
                aLine = bufRead.readLine();
                if (aLine != null) {
                    read = read + aLine.length();
                    html = html + aLine;
                }
            } while (aLine != null && !isCancelled);
            bufRead.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (isCancelled) {
            return;
        }
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("c:\\temp\\output.html"));
            writer.write(html);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        html = "<html>" + html.substring(html.indexOf("<body>") + 6, html.indexOf("</body>") - 1) + "/<html>";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("c:\\temp\\converted.html"));
            writer.write(html);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        MemomicsJSPBrowser b = new MemomicsJSPBrowser();
        try {
            b.setURL(new URL(url), broswerTitle + " (ORIGINAL)");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        browser = new MemomicsJSPBrowser(broswerTitle, html);
        System.out.println("HTML:" + html);
    }

    public void setTaskMonitor(TaskMonitor arg0) throws IllegalThreadStateException {
        this.taskMonitor = arg0;
    }
}
