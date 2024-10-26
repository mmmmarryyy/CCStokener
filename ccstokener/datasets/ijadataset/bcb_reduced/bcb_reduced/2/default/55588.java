import java.net.*;
import java.io.*;
import jacol.*;

public class stock {

    public static void main(String[] args) {
        String ticker = "MSFT";
        if (args.length == 1) {
            ticker = args[0];
        }
        new stock(ticker);
    }

    public stock(String ticker) {
        try {
            URL url = new URL("http://finance.yahoo.com/q?s=" + ticker + "&d=v1");
            BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;
            StringBuffer page = new StringBuffer(8192);
            while ((line = reader.readLine()) != null) {
                page.append(line);
            }
            LispInterpreter lisp = InterpreterFactory.getInterpreter();
            lisp.eval("(load \"nregex\")");
            String quote = lisp.eval("(second (regex \"<b>([0-9][0-9]\\.[0-9][0-9])</b>\" \"" + cleanse(page) + "\"))");
            System.out.println("Current quote: " + quote);
            lisp.exit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String cleanse(StringBuffer buff) {
        String str = buff.toString();
        StringBuffer ret = new StringBuffer(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c != '"') ret.append(c);
        }
        return ret.toString();
    }
}
