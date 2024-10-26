package org.autoplot.dshop;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Map;
import org.das2.datum.TimeUtil;
import org.das2.datum.Units;
import org.das2.util.Base64;
import org.das2.util.monitor.ProgressMonitor;
import org.virbo.dataset.DDataSet;
import org.virbo.dataset.DataSetOps;
import org.virbo.dataset.MutablePropertyDataSet;
import org.virbo.dataset.QDataSet;
import org.virbo.dataset.WritableDataSet;
import org.virbo.datasource.AbstractDataSource;
import org.virbo.datasource.URLSplit;
import org.virbo.dsutil.AsciiParser;

/**
 * 
 * @author jbf
 */
public class DShopDataSource extends AbstractDataSource {

    public static final String TYPE_MAG = "MAG";

    public static final String TYPE_PLASMA = "PLASMA";

    Map<String, Object> properties;

    DShopDataSource(URL url) {
        super(url);
    }

    QDataSet getTimes(QDataSet rank2Times) {
        DDataSet ttag = DDataSet.createRank1(rank2Times.length());
        for (int i = 0; i < ttag.length(); i++) {
            double us2000 = TimeUtil.convert((int) rank2Times.value(i, 0), (int) rank2Times.value(i, 1), (int) rank2Times.value(i, 2), (int) rank2Times.value(i, 3), (int) rank2Times.value(i, 4), rank2Times.value(i, 5), Units.us2000);
            ttag.putValue(i, us2000);
        }
        ttag.putProperty(QDataSet.UNITS, Units.us2000);
        ttag.putProperty(QDataSet.MONOTONIC, Boolean.TRUE);
        return ttag;
    }

    private boolean columnNameEqual(String a, String b) {
        a = a.replaceAll("-", "_").toLowerCase();
        b = b.replaceAll("-", "_").toLowerCase();
        return a.equals(b);
    }

    @Override
    public QDataSet getDataSet(ProgressMonitor mon) throws Exception {
        String colname = params.get("arg_0");
        params.remove("arg_0");
        String surl = this.resourceURL.toString() + "?" + URLSplit.formatParams(params);
        System.err.println(surl);
        URLConnection urlc;
        urlc = new URL(surl).openConnection();
        String userInfo = urlc.getURL().getUserInfo();
        if (userInfo != null) {
            String encode = new String(Base64.encodeBytes(urlc.getURL().getUserInfo().getBytes()));
            urlc.setRequestProperty("Authorization", "Basic " + encode);
        }
        mon.started();
        String[] colNames = null;
        int[] colOffsets = null;
        int[] colLengths = null;
        InputStream in = urlc.getInputStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        try {
            String s = reader.readLine();
            if (!s.equals("UNIFIED_OUTPUT_FOLLOWS")) {
                throw new IllegalArgumentException("response didn't start with UNIFIED_OUTPUT_FOLLOWS");
            }
            while (!s.startsWith("colnames")) {
                s = reader.readLine();
            }
            String scolnames = s.substring(10);
            colNames = scolnames.split(",");
            colLengths = new int[colNames.length];
            colOffsets = new int[colNames.length];
            for (int i = 0; i < colNames.length; i++) {
                colOffsets[i] = i;
                colLengths[i] = 1;
            }
            s = reader.readLine();
            if (s == null) {
                throw new IllegalArgumentException("no records returned");
            }
            AsciiParser parser = new AsciiParser();
            parser.guessDelimParser(s);
            parser.setValidMin(-1.9e37);
            WritableDataSet wds = parser.readStream(reader, s, mon);
            QDataSet ttag = getTimes(wds);
            int i;
            for (i = colNames.length - 1; i >= 0; i--) {
                if (columnNameEqual(colNames[i], colname)) break;
            }
            if (i == -1) throw new IllegalArgumentException("bad column parameter: expected one of " + scolnames);
            MutablePropertyDataSet data;
            if (colLengths[i] == 1) {
                data = DataSetOps.slice1(wds, colOffsets[i]);
            } else {
                data = DataSetOps.trim(wds, colOffsets[i], colLengths[i]);
            }
            data.putProperty(QDataSet.DEPEND_0, ttag);
            return data;
        } catch (Exception e) {
            throw e;
        } finally {
            reader.close();
            mon.finished();
        }
    }
}
