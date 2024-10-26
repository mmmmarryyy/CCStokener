import ptolemy.plot.*;
import java.applet.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;

public class TimeSeries extends Applet {

    private transient TSPlot[] _myTSPlots;

    private transient Legend legend;

    private transient Vector[] _myPlotTypes;

    private int MAXPANELS = 3;

    private int MAXSITES = 5;

    private String plotDir = "./plotfiles/";

    private String plotTypeFile = plotDir + "plottypes.lst.gz";

    private String siteFile = plotDir + "sites.lst.gz";

    /** Return a string describing this applet.
     */
    public String getAppletInfo() {
        return "TimeSeries 0.1: USGS GPS data plotter.\n" + "By: Keith F. Stark stark@dukester.com\n";
    }

    /** Initialize the applet.  Read the applet parameters.
     */
    public void init() {
        super.init();
        java.awt.List _mySiteList = new java.awt.List(6, true);
        java.awt.List _myPlotList = new java.awt.List(9, true);
        Panel leftSide = new Panel();
        Panel rightSide = new Panel();
        Panel topSide = new Panel();
        this.setBackground(new Color(153, 204, 255));
        _mySiteList.setBackground(new Color(204, 204, 240));
        _myPlotList.setBackground(new Color(204, 204, 240));
        Vector _mySites = _getSiteNames();
        for (int i = 0; i < _mySites.size(); i++) {
            _mySiteList.add((String) _mySites.elementAt(i));
        }
        _getPlotTypes();
        for (int i = 0; i < _myPlotTypes[0].size(); i++) {
            _myPlotList.add((String) _myPlotTypes[0].elementAt(i));
        }
        Checkbox detrendbox = new Checkbox("Detrend", false);
        Checkbox errorbarsbox = new Checkbox("Error Bars", false);
        Checkbox fitlinebox = new Checkbox("Draw Trend Line", false);
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
        leftSide.add(new Label("Select Sites (" + MAXSITES + " max)"), c);
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = c.weighty = 1;
        leftSide.add(_mySiteList, c);
        c.gridx = 0;
        c.gridy = 2;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = c.weighty = 0;
        leftSide.add(new Label("Select Plots (" + MAXPANELS + " max)"), c);
        c.gridx = 0;
        c.gridy = 3;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = c.weighty = 0;
        leftSide.add(_myPlotList, c);
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = c.weighty = 0;
        leftSide.add(detrendbox, c);
        c.gridx = 0;
        c.gridy = 5;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = c.weighty = 0;
        leftSide.add(errorbarsbox, c);
        c.gridx = 0;
        c.gridy = 6;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = c.weighty = 0;
        leftSide.add(fitlinebox, c);
        rightSide.setLayout(new FlowLayout());
        legend = new Legend();
        legend.setSize(400, 20);
        Vector tmp = new Vector();
        rightSide.add(legend);
        _myTSPlots = new TSPlot[MAXPANELS];
        for (int i = 0; i < MAXPANELS; i++) {
            _myTSPlots[i] = new TSPlot();
            _myTSPlots[i].plotDir((URL) getDocumentBase(), (String) plotDir);
            rightSide.add(_myTSPlots[i]);
        }
        _mySiteList.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                Integer t = new Integer(e.getItem().toString());
                int numselected = ((java.awt.List) e.getItemSelectable()).getSelectedIndexes().length;
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (numselected > MAXSITES) {
                        ((java.awt.List) e.getItemSelectable()).deselect(t.intValue());
                    } else {
                        _addSite(((java.awt.List) e.getItemSelectable()).getItem(t.intValue()));
                    }
                } else {
                    if (numselected < MAXSITES) {
                        _removeSite(((java.awt.List) e.getItemSelectable()).getItem(t.intValue()));
                    }
                }
            }
        });
        _myPlotList.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                Integer t = new Integer(e.getItem().toString());
                int numselected = ((java.awt.List) e.getItemSelectable()).getSelectedIndexes().length;
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    if (numselected > MAXPANELS) {
                        ((java.awt.List) e.getItemSelectable()).deselect(t.intValue());
                    } else {
                        _addPlot(t.intValue());
                    }
                } else {
                    _removePlot(t.intValue());
                }
            }
        });
        detrendbox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                _detrend(((Checkbox) e.getItemSelectable()).getState());
            }
        });
        errorbarsbox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                _errorbars(((Checkbox) e.getItemSelectable()).getState());
            }
        });
        fitlinebox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                _fitline(((Checkbox) e.getItemSelectable()).getState());
            }
        });
        String startSite = getParameter("site");
        if (startSite == null) {
            startSite = "";
        }
        String startPlot1 = getParameter("plot1");
        if (startPlot1 == null) {
            startPlot1 = "";
        }
        String startPlot2 = getParameter("plot2");
        if (startPlot2 == null) {
            startPlot2 = "";
        }
        String startPlot3 = getParameter("plot3");
        if (startPlot3 == null) {
            startPlot3 = "";
        }
        int siteCount = _mySiteList.getItemCount();
        for (int i = 0; i < siteCount; i++) {
            if (startSite.equalsIgnoreCase(_mySiteList.getItem(i))) {
                _mySiteList.select(i);
                _mySiteList.makeVisible(i);
                _addSite(startSite);
            }
        }
        int plotCount = _myPlotList.getItemCount();
        for (int i = 0; i < plotCount; i++) {
            if (startPlot1.equalsIgnoreCase(_myPlotList.getItem(i))) {
                _myPlotList.select(i);
                _addPlot(i);
            }
        }
        for (int i = 0; i < plotCount; i++) {
            if (startPlot2.equalsIgnoreCase(_myPlotList.getItem(i))) {
                _myPlotList.select(i);
                _addPlot(i);
            }
        }
        for (int i = 0; i < plotCount; i++) {
            if (startPlot3.equalsIgnoreCase(_myPlotList.getItem(i))) {
                _myPlotList.select(i);
                _addPlot(i);
            }
        }
    }

    private void _detrend(boolean tmp) {
        for (int i = 0; i < MAXPANELS; i++) {
            _myTSPlots[i].detrend(tmp);
            _myTSPlots[i].updatePlot();
        }
    }

    private void _errorbars(boolean tmp) {
        for (int i = 0; i < MAXPANELS; i++) {
            _myTSPlots[i].errorbars(tmp);
            _myTSPlots[i].updatePlot();
        }
    }

    private void _fitline(boolean tmp) {
        for (int i = 0; i < MAXPANELS; i++) {
            _myTSPlots[i].plotfit(tmp);
            _myTSPlots[i].updatePlot();
        }
    }

    private void _addSite(String sitename) {
        for (int i = 0; i < MAXPANELS; i++) {
            _myTSPlots[i].addSite(sitename);
        }
        _setLegend();
    }

    private void _removeSite(String sitename) {
        for (int i = 0; i < MAXPANELS; i++) {
            _myTSPlots[i].removeSite(sitename);
        }
        _setLegend();
    }

    private void _setLegend() {
        Vector siteList = new Vector();
        for (int i = 0; i < MAXPANELS; i++) {
            siteList = _myTSPlots[i].siteList();
            if (siteList != null) break;
        }
        if (siteList == null) {
            legend.setSiteList(null);
        } else {
            legend.setSiteList(siteList);
        }
        legend.repaint();
    }

    private void _removePlot(int plottype) {
        for (int i = 0; i < MAXPANELS; i++) {
            if (_myTSPlots[i].getPlotType() == null) continue;
            if (_myTSPlots[i].getPlotType().compareTo((String) _myPlotTypes[1].elementAt(plottype)) == 0) {
                _myTSPlots[i].setPlotType(null);
                _myTSPlots[i].changePlot();
            }
        }
    }

    private void _addPlot(int plottype) {
        boolean done = false;
        for (int i = 0; i < MAXPANELS; i++) {
            if (_myTSPlots[i].getPlotType() != null) {
                if (_myTSPlots[i].getPlotType().compareTo((String) _myPlotTypes[1].elementAt(plottype)) == 0) {
                    return;
                }
            }
        }
        for (int i = 0; i < MAXPANELS; i++) {
            if (_myTSPlots[i].getPlotType() == null) {
                _myTSPlots[i].setPlotType((String) _myPlotTypes[1].elementAt(plottype));
                _myTSPlots[i].changePlot();
                break;
            }
        }
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

    public void _getPlotTypes() {
        boolean gotPlots = false;
        while (!gotPlots) {
            try {
                _myPlotTypes = new Vector[2];
                _myPlotTypes[0] = new Vector();
                _myPlotTypes[1] = new Vector();
                URL dataurl = new URL(getDocumentBase(), plotTypeFile);
                BufferedReader readme = new BufferedReader(new InputStreamReader(new GZIPInputStream(dataurl.openStream())));
                while (true) {
                    String S = readme.readLine();
                    if (S == null) break;
                    StringTokenizer st = new StringTokenizer(S);
                    _myPlotTypes[0].addElement(st.nextToken());
                    if (st.hasMoreTokens()) {
                        _myPlotTypes[1].addElement(st.nextToken());
                    } else {
                        _myPlotTypes[1].addElement((String) _myPlotTypes[0].lastElement());
                    }
                }
                gotPlots = true;
            } catch (IOException e) {
                _myPlotTypes[0].removeAllElements();
                _myPlotTypes[1].removeAllElements();
                gotPlots = false;
            }
        }
    }
}
