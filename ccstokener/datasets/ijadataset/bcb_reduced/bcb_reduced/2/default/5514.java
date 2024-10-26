import ptolemy.plot.*;
import java.applet.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;

class SPlot extends TSPlot {

    public SPlot() {
        super();
    }

    public SPlot(int width, int height) {
        super(width, height);
    }

    public void setPlotType(String mytmp) {
        plotType = mytmp;
        if (plotType == null) {
            title = "";
            xtitle = "";
            ytitle = "";
            allowDemean = true;
        } else {
            String plotTemp = new String(plotType);
            if (plotType == "north-east-30") {
                plotTemp = "north";
                marksStyle = "dots";
            } else if (plotType == "up-30") {
                plotTemp = "up";
                marksStyle = "dots";
            }
            String tmp = new String();
            String filename = new String(plotDir + plotTemp + ".dft.gz");
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
                        if (plotType == "north-east-30") {
                            title = "Horizontal";
                            xtitle = "East (mm)";
                            ytitle = "North (mm)";
                        } else if (plotType == "up-30") {
                            title = "Vertical";
                            xtitle = "Time (decimal year)";
                            ytitle = "Up (mm)";
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

    public int addSite(String sitename) {
        boolean found = false;
        for (int j = 0; j < _myData.size(); j++) {
            if (((DataSet) _myData.elementAt(j)).siteName().compareTo(sitename) == 0) {
                updatePlot();
                return (1);
            }
        }
        double xMax = 0.0;
        double xMin = 0.0;
        double yMax = 0.0;
        double yMin = 0.0;
        if (plotType == "north-east-30") {
            DataSet north = new DataSet(sitename, "north", docBase, plotDir);
            DataSet east = new DataSet(sitename, "east", docBase, plotDir);
            DataSet[] Span = new DataSet[3];
            for (int i = 0; i < 3; i++) {
                Span[i] = (DataSet) north.clone();
                Span[i].clearPoints();
            }
            north.allowDemean(false);
            north.detrend(true);
            east.allowDemean(false);
            east.detrend(true);
            double decYear = north.getDecYear();
            int daysBack = 30;
            int subDays = 10;
            double decBack = ((double) daysBack - .2) / 365;
            for (int j = 0; j < north.length(); j++) {
                double x = north.getX(j);
                if (x > (decYear - (decBack - subDays * 2 / 365.0))) {
                    Span[0].addPoint(east.getY(j), north.getY(j));
                    if (east.getY(j) > xMax) {
                        xMax = east.getY(j);
                    } else if (east.getY(j) < xMin) {
                        xMin = east.getY(j);
                    }
                    if (north.getY(j) > yMax) {
                        yMax = north.getY(j);
                    } else if (north.getY(j) < yMin) {
                        yMin = north.getY(j);
                    }
                } else if (x > (decYear - (decBack - subDays / 365.0))) {
                    Span[1].addPoint(east.getY(j), north.getY(j));
                    if (east.getY(j) > xMax) {
                        xMax = east.getY(j);
                    } else if (east.getY(j) < xMin) {
                        xMin = east.getY(j);
                    }
                    if (north.getY(j) > yMax) {
                        yMax = north.getY(j);
                    } else if (north.getY(j) < yMin) {
                        yMin = north.getY(j);
                    }
                } else if (x > (decYear - decBack)) {
                    Span[2].addPoint(east.getY(j), north.getY(j));
                    if (east.getY(j) > xMax) {
                        xMax = east.getY(j);
                    } else if (east.getY(j) < xMin) {
                        xMin = east.getY(j);
                    }
                    if (north.getY(j) > yMax) {
                        yMax = north.getY(j);
                    } else if (north.getY(j) < yMin) {
                        yMin = north.getY(j);
                    }
                }
            }
            if (Math.abs(xMax) > Math.abs(xMin)) {
                xMin = 0 - xMax;
            } else {
                xMax = 0 - xMin;
            }
            if (Math.abs(yMax) > Math.abs(yMin)) {
                yMin = 0 - yMax;
            } else {
                yMax = 0 - yMin;
            }
            for (int i = 0; i < 3; i++) {
                _myData.addElement(Span[i]);
                ((DataSet) _myData.lastElement()).allowDemean(false);
                ((DataSet) _myData.lastElement()).detrend(false);
            }
        } else if (plotType == "up-30") {
            DataSet up = new DataSet(sitename, "up", docBase, plotDir);
            DataSet[] Up = new DataSet[4];
            for (int i = 0; i < 4; i++) {
                Up[i] = (DataSet) up.clone();
                Up[i].clearPoints();
            }
            up.allowDemean(false);
            up.detrend(true);
            double decYear = up.getDecYear();
            int daysBack = 30;
            int subDays = 10;
            double decBack = ((double) daysBack - .2) / 365;
            for (int j = 0; j < up.length(); j++) {
                double x = up.getX(j);
                if (x > (decYear - (decBack - subDays * 2 / 365.0))) {
                    Up[0].addPoint(x, up.getY(j));
                    if (up.getY(j) > yMax) {
                        yMax = up.getY(j);
                    } else if (up.getY(j) < yMin) {
                        yMin = up.getY(j);
                    }
                } else if (x > (decYear - (decBack - subDays / 365.0))) {
                    Up[1].addPoint(x, up.getY(j));
                    if (up.getY(j) > yMax) {
                        yMax = up.getY(j);
                    } else if (up.getY(j) < yMin) {
                        yMin = up.getY(j);
                    }
                } else if (x > (decYear - decBack)) {
                    Up[2].addPoint(x, up.getY(j));
                    if (up.getY(j) > yMax) {
                        yMax = up.getY(j);
                    } else if (up.getY(j) < yMin) {
                        yMin = up.getY(j);
                    }
                }
            }
            xMax = decYear;
            xMin = decYear - decBack;
            for (int i = 0; i < 3; i++) {
                _myData.addElement(Up[i]);
                ((DataSet) _myData.lastElement()).allowDemean(false);
                ((DataSet) _myData.lastElement()).detrend(false);
            }
        } else {
            _myData.addElement(new DataSet(sitename, plotType, docBase, plotDir));
            ((DataSet) _myData.lastElement()).allowDemean(allowDemean);
            ((DataSet) _myData.lastElement()).detrend(detrend);
        }
        setMaxRange(xMax, xMin, yMax, yMin);
        updatePlot();
        return (1);
    }

    public Plot newPlot() {
        return new MyPlot();
    }
}
