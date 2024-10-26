import ptolemy.plot.*;
import java.applet.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;

class TSPlot extends Panel {

    public transient Vector _myData;

    public transient Plot _myPlot;

    public boolean detrend = false;

    public boolean errorbars = false;

    public String marksStyle = "points";

    public String plotType = null;

    public String title = "";

    public String xtitle = "";

    public String ytitle = "";

    public boolean allowDemean = true;

    public boolean connected = false;

    public boolean plotfit = false;

    public URL docBase = null;

    public String plotDir = "./plotfiles/";

    public TSPlot() {
        _myPlot = newPlot();
        _myData = new Vector();
        _myPlot.setSize(400, 200);
        setLayout(new FlowLayout());
        add(_myPlot);
        _myPlot.setButtons(true);
    }

    public TSPlot(int width, int height) {
        _myPlot = newPlot();
        _myData = new Vector();
        _myPlot.setSize(width, height);
        setLayout(new FlowLayout());
        add(_myPlot);
        _myPlot.setButtons(true);
    }

    public void detrend(boolean tmp) {
        detrend = tmp;
    }

    public void errorbars(boolean tmp) {
        errorbars = tmp;
    }

    public void plotfit(boolean tmp) {
        plotfit = tmp;
    }

    public String getPlotType() {
        return (plotType);
    }

    public void setPlotType(String mytmp) {
        plotType = mytmp;
        if (plotType == null) {
            title = "";
            xtitle = "";
            ytitle = "";
            allowDemean = true;
        } else {
            String tmp = new String();
            String filename = new String(plotDir + plotType + ".dft.gz");
            boolean gotInfo = false;
            while (!gotInfo) {
                try {
                    URL dataurl = new URL(docBase, filename);
                    BufferedReader readme = new BufferedReader(new InputStreamReader(new GZIPInputStream(dataurl.openStream())));
                    while (true) {
                        String myline = readme.readLine();
                        if (myline == null) break;
                        if (myline.toLowerCase().startsWith("demean=")) {
                            tmp = myline.substring(7).trim().toLowerCase();
                            if (tmp.compareTo("yes") == 0) {
                                allowDemean = true;
                            } else {
                                allowDemean = false;
                            }
                        } else if (myline.toLowerCase().startsWith("connectpoints=")) {
                            tmp = myline.substring(14).trim().toLowerCase();
                            if (tmp.compareTo("yes") == 0) {
                                connected = true;
                            } else {
                                connected = false;
                            }
                        } else if (myline.toLowerCase().startsWith("title=")) {
                            title = myline.substring(6).trim();
                        } else if (myline.toLowerCase().startsWith("xtitle=")) {
                            xtitle = myline.substring(7).trim();
                        } else if (myline.toLowerCase().startsWith("ytitle=")) {
                            ytitle = myline.substring(7).trim();
                        }
                    }
                    gotInfo = true;
                } catch (FileNotFoundException e) {
                    System.err.println("PlotApplet: file not found: " + e);
                } catch (IOException e) {
                    System.err.println("PlotApplet: error reading input file: " + e);
                }
            }
        }
    }

    public Vector siteList() {
        Vector siteList = new Vector();
        if (_myData.size() == 0) return (null);
        for (int j = 0; j < _myData.size(); j++) {
            siteList.addElement(new String(((DataSet) _myData.elementAt(j)).siteName()));
        }
        return (siteList);
    }

    public void updatePlot() {
        _myPlot.clear(true);
        _myPlot.repaint();
        if (plotfit && _myData.size() > 0) {
            double[] fit = new double[5];
            fit = ((DataSet) _myData.elementAt(0)).getFit();
            if (fit[0] != 0.0) {
                double average = 0.0;
                if (allowDemean) {
                    average = ((DataSet) _myData.elementAt(0)).getAverage();
                }
                _myPlot.setMarksStyle("none");
                _myPlot.setConnected(false);
                _myPlot.addPoint(0, fit[0], fit[1] - average, false);
                _myPlot.setConnected(true);
                _myPlot.addPoint(0, fit[2], fit[3] - average, true);
            }
        }
        for (int j = 0; j < _myData.size(); j++) {
            drawPlot(j);
        }
    }

    public void changePlot() {
        _myPlot.clear(true);
        String[] sitename = new String[_myData.size()];
        for (int j = 0; j < _myData.size(); j++) {
            sitename[j] = new String(((DataSet) _myData.elementAt(j)).siteName());
        }
        _myData.removeAllElements();
        for (int j = 0; j < sitename.length; j++) {
            addSite(sitename[j]);
        }
    }

    public void clearPlot() {
        _myData.removeAllElements();
        _myPlot.clear(true);
        _myPlot.repaint();
    }

    public int drawPlot(int dataset) {
        ((DataSet) _myData.elementAt(dataset)).detrend(detrend);
        _myPlot.setMarksStyle(marksStyle);
        _myPlot.setConnected(connected);
        _myPlot.setTitle(title);
        _myPlot.setXLabel(xtitle);
        _myPlot.setYLabel(ytitle);
        DataSet tmp = (DataSet) _myData.elementAt(dataset);
        for (int j = 0; j < tmp.length(); j++) {
            if (errorbars) {
                _myPlot.addPointWithErrorBars(dataset + 1, tmp.getX(j), tmp.getY(j), tmp.getErrorHigh(j), tmp.getErrorLow(j), true);
            } else {
                _myPlot.addPoint(dataset + 1, tmp.getX(j), tmp.getY(j), true);
            }
        }
        _myPlot.fillPlot();
        return (1);
    }

    public int addSite(String sitename) {
        boolean found = false;
        for (int j = 0; j < _myData.size(); j++) {
            if (((DataSet) _myData.elementAt(j)).siteName().compareTo(sitename) == 0) {
                updatePlot();
                return (1);
            }
        }
        _myData.addElement(new DataSet(sitename, plotType, docBase, plotDir));
        ((DataSet) _myData.lastElement()).allowDemean(allowDemean);
        ((DataSet) _myData.lastElement()).detrend(detrend);
        updatePlot();
        return (1);
    }

    public void removeSite(String sitename) {
        for (int dataset = 0; dataset < _myData.size(); dataset++) {
            if (((DataSet) _myData.elementAt(dataset)).siteName().compareTo(sitename) == 0) {
                _myData.removeElementAt(dataset);
            }
        }
        updatePlot();
    }

    public void setMaxRange(double xMax, double xMin, double yMax, double yMin) {
        _myPlot.setXRange(xMax, xMin);
        _myPlot.setYRange(yMax, yMin);
        repaint();
    }

    public void plotDir(URL ploturl, String dir) {
        docBase = ploturl;
        plotDir = dir;
    }

    public Plot newPlot() {
        return new MyPlot();
    }

    public Plot plot() {
        return _myPlot;
    }
}
