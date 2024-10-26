package gnu.fishingcat.reports;

import java.awt.Color;
import java.awt.Paint;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import gnu.fishingcat.Kit;
import gnu.trove.THashMap;

public class BlackHoleReplyTimeData implements ReplyTimeData {

    private static String NAME = "Black Hole";

    private static String BLACK_HOLE = "Black_Hole";

    private static String regexp = "<tr>.*?<b>.*?:(.*?)</b>.*?(\\d+).*?(\\d+).*?(\\d+)";

    public Map getData(final MarketFilter filter) {
        Map allData = null;
        try {
            allData = getBlackHoleData();
        } catch (Exception e) {
            Kit.handleException(e);
            return null;
        }
        Map results = new THashMap();
        for (Iterator i = allData.keySet().iterator(); i.hasNext(); ) {
            String market = (String) i.next();
            if (filter.accept(market)) {
                results.put(market, allData.get(market));
            } else {
                int paren = market.indexOf("(");
                if (paren != -1) {
                    String stripMarket = market.substring(0, paren).trim();
                    if (filter.accept(stripMarket)) {
                        results.put(stripMarket, allData.get(market));
                    }
                }
            }
        }
        return results;
    }

    public String getName() {
        return NAME;
    }

    public String getAbout() {
        return "Black Hole";
    }

    public Paint getPaint() {
        return Color.black;
    }

    private float daysOld(File file) {
        long time = file.lastModified();
        long now = (new Date()).getTime();
        long diff = now - time;
        float daysOut = (float) diff / 86400000.0f;
        return daysOut;
    }

    private Map getStoredData(File dataFile) throws Exception {
        if (dataFile.exists()) {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(dataFile));
            Map map = (Map) ois.readObject();
            ois.close();
            return map;
        }
        return null;
    }

    private Map getBlackHoleData() throws Exception {
        File dataFile = new File(Kit.getDataDir() + BLACK_HOLE);
        if (dataFile.exists() && daysOld(dataFile) < 1) {
            return getStoredData(dataFile);
        }
        InputStream stream = null;
        try {
            String bh_url = "http://www.critique.org/users/critters/blackholes/sightdata.html";
            URL url = new URL(bh_url);
            stream = url.openStream();
        } catch (IOException e) {
            return getStoredData(dataFile);
        }
        BufferedReader br = new BufferedReader(new InputStreamReader(stream));
        StringBuffer data = new StringBuffer();
        String line;
        while ((line = br.readLine()) != null) {
            data.append(line);
        }
        br.close();
        Pattern p = Pattern.compile(regexp);
        Matcher m = p.matcher(data);
        Map map = new THashMap();
        while (m.find()) {
            map.put(m.group(1).trim(), new ReplyTimeDatum(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(4)), 0, Integer.parseInt(m.group(2))));
        }
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(dataFile));
        oos.writeObject(map);
        oos.close();
        return map;
    }

    public static void main(String[] args) throws Exception {
        System.out.println(new HashMap(new BlackHoleReplyTimeData().getData(new PersonalMarketFilter())));
    }
}
