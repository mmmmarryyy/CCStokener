package cn.edu.nju.software.xyz.pim.finance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurrencyAdp {

    public static double[][] getCurrency() throws IOException {
        URL url = new URL("http://hk.finance.yahoo.com/currency");
        BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream(), "big5"));
        double currency[][] = new double[11][11];
        while (true) {
            String line = in.readLine();
            String reg = "<td\\s((align=\"right\"\\sclass=\"yfnc_tabledata1\")" + "|(class=\"yfnc_tabledata1\"\\salign=\"right\"))>" + "([\\d|\\.]+)</td>";
            Matcher m = Pattern.compile(reg).matcher(line);
            int i = 0, j = 0;
            boolean isfound = false;
            while (m.find()) {
                isfound = true;
                currency[i][j] = Double.parseDouble(m.group(4));
                if (j == 10) {
                    j = 0;
                    i++;
                } else j++;
            }
            if (isfound) break;
        }
        return currency;
    }
}
