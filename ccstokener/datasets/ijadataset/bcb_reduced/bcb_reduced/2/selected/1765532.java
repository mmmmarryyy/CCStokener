package com.americancoders;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;

/**
 * this uses yahoo quotes to put data into the StockData arrays.
 * 
 * @author joe mcverry
 * Change history:
 *
 *  YYMMDD BY     Description
 *  110103 JM     Google logic.
 *  110118 JM     Adjust Yahoo #'s with Adjusted Closing
 *  120103 JM     Changed Yahoo URL to ichart.finance.yahoo...
 *  120103 JM     Changed default day request to select last 360 days (orignally 1000).
 *  120225 JM     Removed prebuilt array of data passing capabilities
 *                  and cleaned up Google logic.
 * 
 */
public class GetStockData implements StockData {

    boolean goodData = false;

    double inOpen[] = null;

    double inHigh[] = null;

    double inLow[] = null;

    double inClose[] = null;

    double inVolume[] = null;

    String inDate[] = null;

    /**
	 * test getting ibm's daily numbers writes resulting data to System out
	 * 
	 * @param args
	 *            - not used
	 */
    public static void main(String args[]) {
        GetStockData gsd = new GetStockData("ibm", "d");
        int i;
        for (i = 0; i < gsd.getInClose().length; i++) System.out.println(i + " " + gsd.getInDate()[i] + " " + gsd.getInClose()[i]);
    }

    public GetStockData() {
    }

    int defaultDaysToGoBack = -360;

    public GetStockData(int inDefaultDaysToGoBack) throws Exception {
        if (inDefaultDaysToGoBack > 0) defaultDaysToGoBack = (inDefaultDaysToGoBack * -1); else if (inDefaultDaysToGoBack < 0) defaultDaysToGoBack = inDefaultDaysToGoBack; else throw new Exception("default days to go back can not be zero.");
    }

    /**
	 * 
	 * @param sym
	 *            stock symbol
	 * @param dayOrWeek
	 *            use d or w - month doesn't work on yahoo
	 */
    public GetStockData(String sym, String dayOrWeek) {
        setup(sym, dayOrWeek);
    }

    /**
	 * loads data from yahoo <br>
	 * yahoo has descending by date data so the process will resort into
	 * ascending by date <br>
	 * if dayOrWeek is d pulls 3 years worth of data <br>
	 * if dayOrWeek is w pulls about 9 years worth for weekly data <br>
	 * if dayOrWeek is $ pulls based on value in default days to go back value,
	 * daily data
	 * 
	 * @param sym
	 *            stock symbol
	 * @param dayOrWeek
	 *            use d or w - month doesn't work on yahoo
	 */
    public void setup(String sym, String dayOrWeek) {
        String useGoogle = System.getProperty("useGoogle");
        boolean bUseGoogle = (useGoogle != null && (useGoogle.equalsIgnoreCase("yes") || useGoogle.equalsIgnoreCase("true") || useGoogle.equalsIgnoreCase("1")));
        String googDailyOrWeekly = "daily";
        if (dayOrWeek.equals("d")) ; else if (dayOrWeek.equals("w")) googDailyOrWeekly = "weekly"; else if (dayOrWeek.equals("$")) ; else {
            System.out.println("day or week not d or w or $");
            goodData = false;
            return;
        }
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdfMonth = new SimpleDateFormat("MMM");
        SimpleDateFormat sdfDay = new SimpleDateFormat("dd");
        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy");
        String startDate = "&d=" + c.get(Calendar.MONTH) + "&e=" + c.get(Calendar.DAY_OF_MONTH) + "&f=" + c.get(Calendar.YEAR);
        String googEndDate = "&enddate=" + sdfMonth.format(c.getTime()) + "+" + sdfDay.format(c.getTime()) + "%2C+" + sdfYear.format(c.getTime());
        if (dayOrWeek.equals("d")) c.add(Calendar.DAY_OF_MONTH, defaultDaysToGoBack); else if (dayOrWeek.equals("w")) c.add(Calendar.DAY_OF_MONTH, defaultDaysToGoBack); else {
            c.add(Calendar.DAY_OF_MONTH, defaultDaysToGoBack);
            dayOrWeek = "d";
        }
        String stopDate = "&a=" + c.get(Calendar.MONTH) + "&b=" + c.get(Calendar.DAY_OF_MONTH) + "&c=" + c.get(Calendar.YEAR);
        String googStartDate = "&startdate=" + sdfMonth.format(c.getTime()) + "+" + sdfDay.format(c.getTime()) + "%2C+" + sdfYear.format(c.getTime());
        ArrayList<Double> op = new ArrayList<Double>();
        ArrayList<Double> hi = new ArrayList<Double>();
        ArrayList<Double> lo = new ArrayList<Double>();
        ArrayList<Double> cl = new ArrayList<Double>();
        ArrayList<String> dt = new ArrayList<String>();
        ArrayList<Double> vol = new ArrayList<Double>();
        ArrayList<Double> adjClose = new ArrayList<Double>();
        String sr;
        int cnt = 0;
        long vtot = 0;
        int vcnt = vol.size();
        try {
            URL url;
            if (bUseGoogle == false) url = new URL("http://ichart.finance.yahoo.com/table.csv?s=" + sym + "&g=" + dayOrWeek + stopDate + startDate + "&ignore=.csv"); else url = new URL("http://www.google.com/finance/historical?q=" + sym.toUpperCase() + "&histperiod=" + googDailyOrWeekly + googStartDate + googEndDate + "&output=csv");
            url.openConnection();
            InputStream inputstream = url.openStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(inputstream));
            SimpleDateFormat df2 = new SimpleDateFormat("dd-MMM-yy");
            SimpleDateFormat df3 = new SimpleDateFormat("yyyy-MM-dd");
            while ((sr = br.readLine()) != null) {
                cnt++;
                if (cnt < 2) continue;
                StringTokenizer st = new StringTokenizer(sr, ",");
                if (bUseGoogle) {
                    String idate = (String) st.nextElement();
                    df2.parse(idate);
                    String open = (String) st.nextElement();
                    String high = (String) st.nextElement();
                    String low = (String) st.nextElement();
                    String close = (String) st.nextElement();
                    String volm = (String) st.nextElement();
                    if (open.equals(close) && open.equals(high) && open.equals(low) && volm.equals("0")) ; else {
                        dt.add(df3.format(df2.parse(idate)));
                        op.add(new Double(open));
                        hi.add(new Double(high));
                        lo.add(new Double(low));
                        cl.add(new Double(close));
                        vol.add(new Double(volm));
                        vtot += Long.parseLong(volm);
                        vcnt++;
                    }
                } else {
                    dt.add((String) st.nextElement());
                    op.add(new Double((String) st.nextElement()));
                    hi.add(new Double((String) st.nextElement()));
                    lo.add(new Double((String) st.nextElement()));
                    cl.add(new Double((String) st.nextElement()));
                    vol.add(new Double((String) st.nextElement()));
                    vtot += vol.get(vcnt).longValue();
                    vcnt++;
                    adjClose.add(new Double((String) st.nextElement()));
                }
            }
            inputstream.close();
        } catch (Exception e) {
            e.printStackTrace();
            goodData = false;
            return;
        }
        if (cnt < 5) {
            goodData = false;
            return;
        }
        if (!bUseGoogle) {
            for (int i = 0; i < dt.size(); i++) {
                double adj = cl.get(i).doubleValue() / adjClose.get(i);
                op.set(i, op.get(i).doubleValue() / adj);
                cl.set(i, cl.get(i).doubleValue() / adj);
                hi.set(i, hi.get(i).doubleValue() / adj);
                lo.set(i, lo.get(i).doubleValue() / adj);
                vol.set(i, vol.get(i).doubleValue() / adj);
            }
        }
        moveD(dt);
        moveA(op, "inOpen");
        moveA(hi, "inHigh");
        moveA(lo, "inLow");
        moveA(cl, "inClose");
        moveA(vol, "inVolume");
        if (!bUseGoogle) moveA(adjClose, "inAdjustedClose");
        goodData = true;
    }

    /**
	 * sort date array
	 * 
	 * @param inl
	 *            arraylist of dates
	 */
    private void moveD(ArrayList<String> inl) {
        int l = inl.size();
        inDate = new String[l];
        for (int i = 0; i < l; i++) {
            String s = (String) inl.get(i);
            inDate[l - 1 - i] = s;
        }
    }

    /**
	 * sorts the price and volume data, converts from strings and stores in
	 * double array
	 * 
	 * @param inl
	 *            array list
	 * @param what
	 *            type of data
	 */
    private void moveA(ArrayList<Double> inl, String what) {
        double ina[] = new double[inl.size()];
        int l = inl.size();
        for (int i = 0; i < l; i++) {
            ina[l - 1 - i] = inl.get(i).doubleValue();
        }
        if (what.equals("inOpen")) {
            inOpen = new double[l];
            System.arraycopy(ina, 0, inOpen, 0, l);
        }
        if (what.equals("inClose")) {
            inClose = new double[l];
            System.arraycopy(ina, 0, inClose, 0, l);
        }
        if (what.equals("inLow")) {
            inLow = new double[l];
            System.arraycopy(ina, 0, inLow, 0, l);
        }
        if (what.equals("inHigh")) {
            inHigh = new double[l];
            System.arraycopy(ina, 0, inHigh, 0, l);
        }
        if (what.equals("inVolume")) {
            inVolume = new double[l];
            System.arraycopy(ina, 0, inVolume, 0, l);
        }
    }

    public boolean isGoodData() {
        return goodData;
    }

    public double[] getInClose() {
        return inClose;
    }

    public String[] getInDate() {
        return inDate;
    }

    public double[] getInHigh() {
        return inHigh;
    }

    public double[] getInLow() {
        return inLow;
    }

    public double[] getInOpen() {
        return inOpen;
    }

    public double[] getInVolume() {
        return inVolume;
    }
}
