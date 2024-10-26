import java.io.IOException;
import java.net.*;
import ws300.CurrentRecord;

public class GetCurrentRecord {

    public static CurrentRecord getCurrentRecord(String url) throws MalformedURLException, IOException {
        StringBuffer urlString = new StringBuffer(url);
        if (urlString.lastIndexOf("/") != urlString.length() - 1) {
            urlString.append('/');
        }
        urlString.append("GetCurrentRecord.jsp");
        URLConnection conn = new URL(urlString.toString()).openConnection();
        return CurrentRecord.readObject(conn.getInputStream());
    }

    public static void main(String[] args) throws Exception {
        int result = 20;
        if (args.length == 1) {
            System.out.println(getCurrentRecord(args[0]));
            result = 0;
        } else {
            System.err.println("usage: GetCurrentRecord <URL>");
        }
        System.exit(result);
    }
}
