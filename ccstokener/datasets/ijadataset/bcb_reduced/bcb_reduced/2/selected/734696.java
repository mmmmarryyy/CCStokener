package spidr.applets;

import java.awt.Color;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import java.net.URL;
import java.net.MalformedURLException;
import spidr.applets.ptolemy.plot.*;
import spidr.datamodel.*;
import wdc.utils.WDCDay;

/**
The class extends PlotApplet to plot time series resulting from DailyData objects.
*/
public class PlotDSS extends PlotApplet {

    /** Returns a string describing this applet.
  */
    public String getAppletInfo() {
        return "PlotDSS 2.0: Spidr visualization applet.\n" + "By: Mikhail Zhizhin, jjn@wdcb.ru; Alexei Burtsev, bur@wdcb.ru\n ";
    }

    /** Returns parameter information
  */
    public String[][] getParameterInfo() {
        final String paramInfo[][] = { { "background", "hexcolor value", "background color" }, { "foreground", "hexcolor value", "foreground color" }, { "dataseturl", "url", "the URL of the STP data set to plot" }, { "pxgraphargs", "args", "pxgraph style command line arguments" } };
        return paramInfo;
    }

    /** Initializes the applet.
  */
    public void init() {
        super.newPlot();
        super.init();
        DataSequenceSet dss = null;
        String dataurlspec = getParameter("dataseturl");
        if (dataurlspec != null) {
            try {
                showStatus("Reading data");
                URL dataurl = new URL(getDocumentBase(), dataurlspec);
                dss = readDataSet(dataurl.openStream());
                showStatus("Ploting data");
                plotOnPanel(plot(), dss);
                showStatus("Done");
            } catch (MalformedURLException e) {
                System.err.println(e.toString());
            } catch (FileNotFoundException e) {
                System.err.println("PlotDSS: file not found: " + e);
            } catch (ClassNotFoundException e) {
                System.err.println("PlotDSS: class not found: " + e);
            } catch (StreamCorruptedException e) {
                System.err.println("PlotDSS: error reading GZIP data: " + e);
            } catch (IOException e) {
                System.err.println("PlotDSS: error reading input file: " + e);
            }
        }
    }

    /** Plots data on the given  panel.
  * @param plotPanel The panel to plot on
  * @param dss DataSequenceSet object to be ploted
  */
    public static void plotOnPanel(Plot plotPanel, DataSequenceSet dss) {
        plotOnPanel(plotPanel, dss, null, null);
    }

    /** Plots data on the given  panel.
  * @param plotPanel The panel to plot on
  * @param dss DataSequenceSet object to be ploted
  */
    public static void plotOnPanel(Plot plotPanel, DataSequenceSet dss, WDCDay dayFrom, WDCDay dayTo) {
        plotPanel.setTitle(dss.getTitle());
        boolean isLogScale = dss.getScale().equals("log");
        plotPanel.setYLog(isLogScale);
        plotPanel.setMarksStyle("none");
        plotPanel.setXLabel("UTC");
        if (dss.size() > 0) {
            DataDescription descr = ((DataSequence) dss.elementAt(0)).getDescription();
            if (descr != null) plotPanel.setYLabel(descr.getUnits()); else plotPanel.setYLabel("ERROR");
        } else {
            plotPanel.setYLabel("NO DATA");
            return;
        }
        if (dss.getMinRangeValue() != null && dss.getMaxRangeValue() != null) plotPanel.setYRange(dss.getMinRangeValue().doubleValue(), dss.getMaxRangeValue().doubleValue());
        plotPanel.setXTime(true);
        if (dayFrom != null && dayTo != null) {
            if (dayFrom.isMore(dayTo)) plotPanel.setXRange(dayTo.epochTime(), dayFrom.epochTime() + 24 * 3600000); else plotPanel.setXRange(dayFrom.epochTime(), dayTo.epochTime() + 24 * 3600000);
        }
        String marks = (String) dss.attributes.get("marks");
        if (marks != null) {
            plotPanel.setMarksStyle(marks);
        }
        boolean isBars = dss.getRepresentation().equals("bars");
        if (isBars) {
            long width = ((DataSequence) dss.elementAt(0)).getSampling();
            if (width <= 0) width = 1;
            plotPanel.setBars(width * 60000, (width * 60000) / 2);
        }
        plotPanel.clearCurrentColors();
        for (int nElem = 0; nElem < dss.size(); nElem++) {
            DataSequence ds = (DataSequence) dss.elementAt(nElem);
            String color = (String) ds.attributes.get("color");
            if (color != null) plotPanel.setCurrentColor(nElem, Color.decode(color));
            DataDescription descr = ds.getDescription();
            if (descr != null) plotPanel.addLegend(nElem, descr.getLabel()); else {
                plotPanel.addLegend(nElem, "ERROR");
                continue;
            }
            if (ds.size() == 0) continue;
            ds.sort();
            float missingValue = descr.getMissingValue();
            boolean isConnected = false;
            for (int num = 0; num < ds.size(); num++) {
                plotPanel.setXTime(true);
                int dayId = ((DailyData) ds.elementAt(num)).getDayId();
                if (num > 0) {
                    int numDays = (new WDCDay(((DailyData) ds.elementAt(num)).getDayId())).daysSinceETime() - (new WDCDay(((DailyData) ds.elementAt(num - 1)).getDayId())).daysSinceETime();
                    long sampInDays = ((DailyData) ds.elementAt(num - 1)).getSampling() / 1440;
                    if (sampInDays != 0 && numDays > sampInDays || sampInDays == 0 && numDays > 1) isConnected = false;
                }
                float[] data = ((DailyData) ds.elementAt(num)).getData();
                if (data == null) {
                    isConnected = false;
                    continue;
                }
                int[] times = ((DailyData) ds.elementAt(num)).getTimes();
                int numData = (times != null) ? Math.min(data.length, times.length) : data.length;
                long msSampling = ((DailyData) ds.elementAt(num)).getSampling() * 60000L;
                long time = (new WDCDay(dayId)).epochTime();
                for (int k = 0; k < numData; k++) {
                    if (data[k] != missingValue && !(isLogScale && data[k] <= 0)) {
                        if (times != null) {
                            double tRef = time + times[k] * 60000L;
                            plotPanel.addPoint(nElem, tRef, data[k], !isBars && isConnected);
                        } else {
                            double tRef = time + k * msSampling;
                            plotPanel.addPoint(nElem, tRef, data[k], !isBars && isConnected);
                        }
                        isConnected = true;
                    } else isConnected = false;
                }
            }
        }
    }

    /** Reads DataSequenceSet object from the stream.
  * @param strm The input stream object
  * @return DataSequenceSet object
  */
    public DataSequenceSet readDataSet(InputStream is) throws IOException, StreamCorruptedException, ClassNotFoundException {
        GZIPInputStream gzis = new GZIPInputStream(is);
        ObjectInputStream ois = new ObjectInputStream(gzis);
        DataSequenceSet dss = (DataSequenceSet) ois.readObject();
        ois.close();
        gzis.close();
        return dss;
    }
}
