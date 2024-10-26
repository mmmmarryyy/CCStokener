import ptolemy.plot.*;
import java.applet.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;

public class Scatter extends Applet {

    private transient SPlot hPlot;

    private transient SPlot vPlot;

    private int MAXPANELS = 1;

    private int MAXSITES = 1;

    private String plotDir = "./plotfiles/";

    private String siteFile = plotDir + "sites.lst";

    /** Return a string describing this applet.
     */
    public String getAppletInfo() {
        return "Scatter 0.1: USGS GPS data plotter.\n" + "By: Russell Moffitt rampage@gps.caltech.edu\n";
    }

    /** Initialize the applet.  Read the applet parameters.
     */
    public void init() {
        super.init();
        java.awt.List _mySiteList = new java.awt.List(6);
        Panel leftSide = new Panel();
        Panel rightSide = new Panel();
        Panel topSide = new Panel();
        this.setBackground(new Color(153, 204, 255));
        _mySiteList.setBackground(new Color(204, 204, 240));
        Vector _mySites = _getSiteNames();
        for (int i = 0; i < _mySites.size(); i++) {
            _mySiteList.add((String) _mySites.elementAt(i));
        }
        setLayout(new GridBagLayout());
        GridBagConstraints b = new GridBagConstraints();
        b.fill = GridBagConstraints.BOTH;
        b.insets = new Insets(5, 5, 5, 5);
        b.gridx = 0;
        b.gridy = 0;
        b.gridwidth = 1;
        b.gridheight = 20;
        b.weightx = b.weighty = 0;
        add(leftSide, b);
        b.gridx = 1;
        b.gridy = 0;
        b.gridwidth = 3;
        b.gridheight = 20;
        b.weightx = b.weighty = 1;
        add(rightSide, b);
        leftSide.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(5, 5, 5, 5);
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = c.weighty = 0;
        leftSide.add(new Label("Select Site"), c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = c.weighty = 1;
        leftSide.add(_mySiteList, c);
        rightSide.setLayout(new FlowLayout());
        hPlot = new SPlot(300, 300);
        hPlot.plotDir((URL) getDocumentBase(), plotDir);
        hPlot.setPlotType("north-east-30");
        vPlot = new SPlot(237, 300);
        vPlot.plotDir((URL) getDocumentBase(), plotDir);
        vPlot.setPlotType("up-30");
        _detrend(false);
        rightSide.add(hPlot);
        rightSide.add(vPlot);
        _mySiteList.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                Integer t = new Integer(e.getItem().toString());
                int numselected = ((java.awt.List) e.getItemSelectable()).getSelectedIndexes().length;
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    _clearPlots();
                    _addSite(((java.awt.List) e.getItemSelectable()).getItem(t.intValue()));
                }
            }
        });
    }

    private void _detrend(boolean tmp) {
        hPlot.detrend(tmp);
        hPlot.updatePlot();
        vPlot.detrend(tmp);
        vPlot.updatePlot();
    }

    private void _addSite(String sitename) {
        hPlot.addSite(sitename);
        vPlot.addSite(sitename);
    }

    private void _removeSite(String sitename) {
        hPlot.removeSite(sitename);
        vPlot.removeSite(sitename);
    }

    private void _clearPlots() {
        hPlot.clearPlot();
        vPlot.clearPlot();
    }

    public Vector _getSiteNames() {
        Vector _mySites = new Vector();
        boolean gotSites = false;
        while (!gotSites) {
            try {
                URL dataurl = new URL(getDocumentBase(), siteFile);
                BufferedReader readme = new BufferedReader(new InputStreamReader(new GZIPInputStream(dataurl.openStream())));
                while (true) {
                    String S = readme.readLine();
                    if (S == null) break;
                    StringTokenizer st = new StringTokenizer(S);
                    _mySites.addElement(st.nextToken());
                }
                gotSites = true;
            } catch (IOException e) {
                _mySites.removeAllElements();
                gotSites = false;
            }
        }
        return (_mySites);
    }
}
