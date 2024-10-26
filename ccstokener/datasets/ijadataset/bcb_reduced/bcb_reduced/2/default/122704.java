import ptolemy.plot.*;
import java.applet.Applet;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.util.*;
import java.util.zip.*;

public class DataSet extends Applet {

    private String sitename;

    private String plottype;

    private Vector[] data;

    private Double[] fit;

    private Double decYear;

    private double average;

    private boolean detrend = false;

    private boolean haveFit = false;

    private boolean haveDate = false;

    private boolean allowDemean = true;

    public DataSet(String name, String type, URL docBase, String plotDir) {
        sitename = name.toUpperCase();
        data = new Vector[3];
        data[0] = new Vector();
        data[1] = new Vector();
        data[2] = new Vector();
        if (type == null) return;
        plottype = type.toLowerCase();
        String filename;
        filename = plotDir + sitename + "_" + plottype + ".plt.gz";
        try {
            double total = 0;
            URL dataurl = new URL(docBase, filename);
            BufferedReader readme = new BufferedReader(new InputStreamReader(new GZIPInputStream(dataurl.openStream())));
            while (true) {
                String myline = readme.readLine();
                if (myline == null) break;
                myline = myline.toLowerCase();
                if (myline.startsWith("fit:")) {
                    if (haveFit) {
                        continue;
                    }
                    StringTokenizer st = new StringTokenizer(myline.replace('\n', ' '));
                    fit = new Double[5];
                    String bye = (String) st.nextToken();
                    fit[0] = new Double((String) st.nextToken());
                    fit[1] = new Double((String) st.nextToken());
                    fit[2] = new Double((String) st.nextToken());
                    fit[3] = new Double((String) st.nextToken());
                    fit[4] = new Double((String) st.nextToken());
                    haveFit = true;
                    continue;
                }
                if (myline.startsWith("decyear:")) {
                    StringTokenizer st = new StringTokenizer(myline.replace('\n', ' '));
                    String bye = (String) st.nextToken();
                    decYear = new Double((String) st.nextToken());
                    haveDate = true;
                    continue;
                }
                StringTokenizer st = new StringTokenizer(myline.replace('\n', ' '));
                boolean ok = true;
                String tmp;
                Double[] mydbl = new Double[3];
                for (int i = 0; i < 3 && ok; i++) {
                    if (st.hasMoreTokens()) {
                        tmp = (String) st.nextToken();
                        if (tmp.startsWith("X") || tmp.startsWith("x")) {
                            ok = false;
                            break;
                        } else {
                            mydbl[i] = new Double(tmp);
                        }
                    } else {
                        mydbl[i] = new Double(0.0);
                    }
                }
                if (ok) {
                    if (mydbl[2].doubleValue() > 100) continue;
                    total = mydbl[1].doubleValue() + total;
                    for (int i = 0; i < 3; i++) {
                        data[i].addElement(mydbl[i]);
                    }
                }
            }
            average = total / length();
        } catch (FileNotFoundException e) {
            System.err.println("PlotApplet: file not found: " + e);
        } catch (IOException e) {
            System.err.println("PlotApplet: error reading input file: " + e);
        }
    }

    public Object clone() {
        if (!(this instanceof DataSet)) {
            throw new IllegalArgumentException("A DataSet object can't be copied to itself!");
        }
        return new DataSet((DataSet) this);
    }

    private DataSet(DataSet orig) {
        sitename = orig.sitename;
        plottype = orig.plottype;
        data = new Vector[orig.data.length];
        for (int i = 0; i < orig.data.length; i++) {
            data[i] = (Vector) orig.data[i].clone();
        }
        fit = new Double[orig.fit.length];
        for (int i = 0; i < orig.fit.length; i++) {
            fit[i] = new Double(orig.fit[i].doubleValue());
        }
        decYear = new Double(orig.decYear.doubleValue());
        average = orig.average;
        detrend = orig.detrend;
        haveFit = orig.haveFit;
        haveDate = orig.haveDate;
        allowDemean = orig.allowDemean;
    }

    public int length() {
        return data[0].size();
    }

    public String siteName() {
        return sitename;
    }

    public double getX(int j) {
        return ((Double) data[0].elementAt(j)).doubleValue();
    }

    public double getY(int j) {
        double val = ((Double) data[1].elementAt(j)).doubleValue();
        if (allowDemean && (!detrend || !haveFit)) {
            val -= average;
        }
        if (haveFit) {
            double startfit = fit[1].doubleValue();
            if (detrend) {
                val = val - (startfit + ((getX(j) - fit[0].doubleValue()) * fit[4].doubleValue()));
            }
        }
        return val;
    }

    public Vector getAllX() {
        return (Vector) data[0].clone();
    }

    public Vector getAllY() {
        return (Vector) data[1].clone();
    }

    public Vector getAllError() {
        return (Vector) data[2].clone();
    }

    public void addPoint(double x, double y) {
        Double x1 = new Double(x);
        Double y1 = new Double(y);
        data[0].addElement(x1);
        data[1].addElement(y1);
        data[2].addElement(new Double(0.0));
    }

    public void addPoint(double x, double y, double error) {
        Double x1 = new Double(x);
        Double y1 = new Double(y);
        Double error1 = new Double(error);
        data[0].addElement(x1);
        data[1].addElement(y1);
        data[2].addElement(error1);
    }

    public void clearPoints() {
        for (int i = 0; i < 3; i++) {
            data[i].removeAllElements();
        }
    }

    public double getDecYear() {
        if (haveDate) {
            return decYear.doubleValue();
        } else {
            return -1.0;
        }
    }

    public double getAverage() {
        return average;
    }

    public double[] getFit() {
        if (haveFit) {
            double[] fits = new double[5];
            for (int i = 0; i < 5; i++) {
                fits[i] = fit[i].doubleValue();
            }
            return fits;
        } else {
            double[] nothing = new double[5];
            nothing[0] = 0.0;
            return nothing;
        }
    }

    public void allowDemean(boolean tmp) {
        allowDemean = tmp;
    }

    public double getErrorHigh(int j) {
        return getY(j) + ((Double) data[2].elementAt(j)).doubleValue();
    }

    public double getErrorLow(int j) {
        return getY(j) - ((Double) data[2].elementAt(j)).doubleValue();
    }

    public void detrend(boolean tmp) {
        detrend = tmp;
    }
}
