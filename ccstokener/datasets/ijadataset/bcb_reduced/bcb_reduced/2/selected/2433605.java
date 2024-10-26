package trading.speculation;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

public class TWSEMargin {

    /**
	 * @param args
	 */
    public static void main(String[] args) throws Exception {
        TWSEMargin margin = new TWSEMargin();
        Options options = new Options();
        options.addOption("t", false, "display current time");
        options.addOption("d", true, "margin date");
        CommandLineParser parser = new PosixParser();
        CommandLine cmd = parser.parse(options, args);
        if (cmd.hasOption("t")) {
        } else {
        }
        String str, dt = cmd.getOptionValue("d");
        if (dt == null) {
            str = margin.getMargins(-25);
        } else {
            str = margin.getMargins(Integer.parseInt(dt));
        }
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(new File("d:/margin.html")));
            bw.write(str);
            bw.close();
        } catch (IOException e) {
        }
    }

    private String getMargins(int idt) {
        String url, s, str, nstr, st;
        ArrayList<Integer> al = new ArrayList<Integer>();
        StringBuffer sb = new StringBuffer();
        sb.append("<table border=1>");
        sb.append("<tr><th>���</th><th>�ĸ�l�B</th><th>��e�@��</th>");
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, idt);
        for (int i = Math.abs(idt); i > 0; i--) {
            cal.add(Calendar.DAY_OF_YEAR, 1);
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY || cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) continue;
            url = this.makeURL(cal.getTime());
            s = this.fetchHTML(url);
            str = this.extractMarginAll(s);
            nstr = this.extractMargin(str);
            st = nstr.replaceAll(",", "");
            if (!st.equals("")) {
                al.add(Integer.parseInt(st));
            }
            sb.append("<tr>");
            sb.append("<td>").append(formatDate(cal.getTime(), "yyyy/MM/dd")).append("</td>");
            sb.append("<td>").append(nstr).append("</td>");
            sb.append("<td>").append(st).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table>");
        return sb.toString();
    }

    private int diffMrgn(int[] mrgn, int idx) {
        if (idx == 0) return 0;
        return (mrgn[idx] - mrgn[idx - 1]) / 100000;
    }

    private String formatDate(Date dt, String fmt) {
        DateFormat df = new SimpleDateFormat(fmt);
        return df.format(dt);
    }

    private String makeURL(Date dt) {
        String baseurl = "http://www.twse.com.tw/ch/trading/exchange/MI_MARGN/";
        String today = "MI_MARGN.php";
        if (dt == null) return baseurl + today; else {
            String chunk = "genpage/Report" + this.formatDate(dt, "yyyyMM") + "/A112" + this.formatDate(dt, "yyyyMMdd") + "MS.php?select2=MS&chk_date=";
            String yy = new Integer(Integer.parseInt(this.formatDate(dt, "yyyy")) - 1911).toString();
            chunk += yy + "/" + this.formatDate(dt, "MM/dd");
            return baseurl + chunk;
        }
    }

    private String fetchHTML(String s) {
        String str;
        StringBuffer sb = new StringBuffer();
        try {
            URL url = new URL(s);
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
        } catch (MalformedURLException e) {
        } catch (IOException e) {
        }
        return sb.toString();
    }

    private String extractTodayURL(String html) {
        Pattern p = Pattern.compile(".*\\(\"(.*)\"\\).*");
        Matcher m = p.matcher(html);
        boolean b = m.matches();
        if (b == true) return m.group(1); else return html;
    }

    private String extractMarginAll(String s) {
        Pattern p = Pattern.compile(".*<!--DOWNLOAD_PRINT_START_BLOCK-->(.*)<!--DOWNLOAD_PRINT_END_BLOCK-->.*");
        Matcher m = p.matcher(s);
        boolean b = m.matches();
        if (b == true) return m.group(1); else return s;
    }

    private String extractMargin(String s) {
        Pattern p = Pattern.compile(".*�ĸ���B\\(�a��\\).*>(.*)</td></tr>.*");
        Matcher m = p.matcher(s);
        boolean b = m.matches();
        if (b == true) return m.group(1); else return s;
    }
}
