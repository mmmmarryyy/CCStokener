package phex.tools;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import phex.common.address.AddressUtils;
import phex.security.IpCidrPair;
import phex.utils.IOUtil;

/**
 * 
 */
public class Ip2CountryDBBuilder {

    private static final String APNIC = "ftp://ftp.apnic.net/pub/stats/apnic/delegated-apnic-latest";

    private static final String RIPE = "ftp://ftp.apnic.net/pub/stats/ripe-ncc/delegated-ripencc-latest";

    private static final String ARIN = "ftp://ftp.apnic.net/pub/stats/arin/delegated-arin-latest";

    private static final String LACNIC = "ftp://ftp.apnic.net/pub/stats/lacnic/delegated-lacnic-latest";

    private static final String AFRINIC = "ftp://ftp.apnic.net/pub/stats/afrinic/delegated-afrinic-latest";

    private static List<IpCountryRange> dataList;

    public static void main(String args[]) throws Exception {
        dataList = new ArrayList<IpCountryRange>();
        String[] rirs = { LACNIC, APNIC, RIPE, ARIN, AFRINIC };
        for (int i = 0; i < rirs.length; i++) {
            System.out.println("Loading " + rirs[i]);
            URL url = new URL(rirs[i]);
            URLConnection connection = url.openConnection();
            InputStream inputStream = connection.getInputStream();
            readData(inputStream);
            System.out.println("Total data read: " + dataList.size());
            inputStream.close();
        }
        Collections.sort(dataList);
        System.out.println("before size: " + dataList.size());
        consolidateList();
        System.out.println("after size: " + dataList.size());
        writeToOutputFile("ip2country.csv");
    }

    private static void writeToOutputFile(String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/phex/resources/" + fileName));
        Iterator<IpCountryRange> iterator = dataList.iterator();
        while (iterator.hasNext()) {
            IpCountryRange range = iterator.next();
            writer.write("" + range.from + "," + range.to + "," + range.countryCode + "\n");
        }
        writer.close();
    }

    private static void writeIpCidrListToOutputFile(String fileName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("src/main/resources/phex/resources/" + fileName));
        for (IpCountryRange range : dataList) {
            List<IpCidrPair> range2cidr = AddressUtils.range2cidr(range.from, range.to);
            for (IpCidrPair pair : range2cidr) {
                writer.write(pair.getMinIp() + "," + pair.cidr + "," + range.countryCode + "\n");
            }
        }
        writer.close();
    }

    private static void consolidateList() {
        List<IpCountryRange> consolidatedList = new ArrayList<IpCountryRange>();
        int size = dataList.size();
        for (int i = 0; i < size; i++) {
            IpCountryRange range = dataList.get(i);
            for (int j = i + 1; j < size; j++) {
                IpCountryRange nextRange = dataList.get(j);
                if (!range.countryCode.equals(nextRange.countryCode)) {
                    break;
                }
                if (range.to + 1 != nextRange.from) {
                    break;
                }
                range.to = range.to + nextRange.to - nextRange.from + 1;
                i++;
            }
            consolidatedList.add(range);
        }
        dataList = consolidatedList;
    }

    private static void readData(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("2")) {
                continue;
            }
            if (line.endsWith("summary")) {
                continue;
            }
            StringTokenizer tokenizer = new StringTokenizer(line, "|");
            tokenizer.nextToken();
            String countryCode = tokenizer.nextToken();
            String type = tokenizer.nextToken();
            if (!type.equals("ipv4")) {
                continue;
            }
            String start = tokenizer.nextToken();
            String value = tokenizer.nextToken();
            int rangeValue = Integer.parseInt(value);
            int fromIp = AddressUtils.parseDottedIpToInt(start);
            int toIp = fromIp + rangeValue - 1;
            IpCountryRange range = new IpCountryRange(fromIp, toIp, countryCode);
            dataList.add(range);
        }
    }

    private static class IpCountryRange implements Comparable<IpCountryRange> {

        int from;

        int to;

        String countryCode;

        public IpCountryRange(int from, int to, String cc) {
            this.from = from;
            this.to = to;
            countryCode = cc;
        }

        @Override
        public String toString() {
            return String.valueOf(from) + " - " + String.valueOf(to) + " " + countryCode;
        }

        public int compareTo(IpCountryRange range) {
            if (IOUtil.unsignedInt2Long(from) > IOUtil.unsignedInt2Long(range.from)) {
                return 1;
            } else if (IOUtil.unsignedInt2Long(from) < IOUtil.unsignedInt2Long(range.from)) {
                return -1;
            }
            return 0;
        }
    }
}
