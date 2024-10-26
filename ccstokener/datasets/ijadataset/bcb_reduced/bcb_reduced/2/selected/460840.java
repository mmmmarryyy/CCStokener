package model.ipreader;

import java.io.*;
import java.net.*;
import java.util.regex.*;
import model.ipreader.exceptions.*;

public class IPReader {

    private String regularExpression;

    private String url;

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) throws MalformedURLException {
        new URL(url);
        this.url = url;
    }

    public String getRegularExpression() {
        return regularExpression;
    }

    public void setRegularExpression(String regularExpression) throws PatternSyntaxException {
        Pattern.compile(regularExpression);
        this.regularExpression = regularExpression;
    }

    private String readWebPage() throws IOException {
        String readString;
        String result = "";
        BufferedReader HTMLpage = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
        while ((readString = HTMLpage.readLine()) != null) result += readString;
        return result;
    }

    public String getIP() throws IOException, IPNotFoundException {
        String text = readWebPage();
        String result = "";
        Pattern pattern = Pattern.compile(regularExpression);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) result = matcher.group(); else throw new IPNotFoundException();
        return result;
    }
}
