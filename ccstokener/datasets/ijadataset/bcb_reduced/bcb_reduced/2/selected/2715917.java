package hatenaSwing;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * URISyntaxException このクラスはHttpURLConnectionを使ってサーバに GETとPOSTを送信します。 詳細については、<a
 * href="http://www.w3.org/pub/WWW/Protocols/">仕様</a>を参照してください
 * 
 * @author pyridoxin
 * @see java.net.URLConnection
 */
public class HTTPMethod {

    /**
     * URLの文字列表現
     */
    private String urlString;

    /**
     * POST用エンティティボディ
     */
    private String postString;

    private String charsetName;

    /**
     * POST後にサーバが返す文字列
     */
    private String returnPOSTString;

    /**
     * GET後にサーバが返す文字列
     */
    private String returnGETString;

    /**
     * charsetNameのデフォルト
     */
    public static final String DEFAULT_CHARSET_NAME = "UTF-8";

    /**
     * GET後にサーバが返す文字列を返します。GETしていなければNULLを、 GETが失敗した場合は空文字を返します
     * 
     * @return　returnGETString
     */
    public String getReturnGETString() {
        return this.returnGETString;
    }

    /**
     * URLのString表現です。ここで設定された文字列は一旦URIを 通してからURLに渡されます。
     * 
     * @param uri
     * @see java.net.URI
     * @see java.net.URL
     */
    public void setURIString(final String uri) {
        this.urlString = uri;
    }

    /**
     * POST用エンティティボディを設定します。
     * 
     * @param s
     */
    public void setPostString(final String s) {
        this.postString = s;
    }

    /**
     * encodeする文字を設定します
     * 
     * @param s
     */
    public void setCharsetName(final String s) {
        this.charsetName = s;
    }

    /**
     * POST後にサーバが返す文字列を返します。POSTしていなければNULLを、 POSTが失敗した場合は空文字を返します
     * 
     * @return　returnGETString
     */
    public String getReturnPOSTString() {
        return this.returnPOSTString;
    }

    /**
     * 何も設定されていないHTTPMethodのコンストラクタです。
     */
    public HTTPMethod() {
        this.urlString = null;
        this.postString = null;
        this.charsetName = DEFAULT_CHARSET_NAME;
        this.returnPOSTString = null;
        this.returnGETString = null;
    }

    /**
     * 
     * POST用のコンストラクタです。呼び出すと同時にPOSTします。
     * 
     * @param urlString
     * @param postString
     *            Post用エンティティボディ
     * @param charsetName
     * @throws IOException
     * @throws URISyntaxException
     */
    public HTTPMethod(final String urlString, final String postString, final String charsetName) throws IOException, URISyntaxException {
        if (urlString != null) {
            this.urlString = urlString;
        }
        if (postString != null) {
            this.postString = postString;
        } else {
            this.postString = "";
        }
        if (charsetName != null) {
            this.charsetName = charsetName;
        } else {
            this.charsetName = DEFAULT_CHARSET_NAME;
        }
        this.returnPOSTString = null;
        this.POST();
    }

    /**
     * GET用のコンストラクタです。呼び出すと同時にGETします。
     * 
     * @param urlString
     * @param charsetName
     * @throws IOException
     * @throws URISyntaxException
     */
    public HTTPMethod(final String urlString, final String charsetName) throws IOException, URISyntaxException {
        if (urlString != null) {
            this.urlString = urlString;
        } else {
            this.postString = "";
        }
        if (charsetName != null) {
            this.charsetName = charsetName;
        } else {
            this.charsetName = DEFAULT_CHARSET_NAME;
        }
        this.returnGETString = null;
        this.GET();
    }

    /**
     * POST
     * 
     * @throws IOException
     * @throws URISyntaxException
     */
    public void POST() throws IOException, URISyntaxException {
        InputStream is;
        InputStreamReader isr;
        HttpURLConnection urlcon;
        this.returnPOSTString = "";
        urlcon = (HttpURLConnection) this.makeURLcon(this.urlString);
        urlcon.setDoOutput(true);
        urlcon.setRequestMethod("POST");
        this.makeOutputStream(urlcon, this.postString);
        is = urlcon.getInputStream();
        is = new BufferedInputStream(is);
        isr = new InputStreamReader(is, this.charsetName);
        System.out.println(this.charsetName);
        StringBuilder s = new StringBuilder();
        int c;
        while ((c = isr.read()) != -1) s.append((char) c);
        this.returnPOSTString = s.toString();
        isr.close();
        is.close();
    }

    /**
     * GET
     * 
     * @throws IOException
     * @throws URISyntaxException
     */
    public void GET() throws IOException, URISyntaxException {
        InputStream is;
        InputStreamReader isr;
        this.returnPOSTString = "";
        HttpURLConnection urlcon = null;
        urlcon = (HttpURLConnection) this.makeURLcon(this.urlString);
        urlcon.setDoOutput(true);
        urlcon.setRequestMethod("GET");
        is = urlcon.getInputStream();
        is = new BufferedInputStream(is);
        isr = new InputStreamReader(is, this.charsetName);
        System.out.println(this.charsetName);
        int c;
        StringBuilder s = new StringBuilder();
        while ((c = isr.read()) != -1) s.append((char) c);
        this.returnGETString = s.toString();
        isr.close();
    }

    private OutputStream makeOutputStream(final HttpURLConnection urlcon, final String s) throws IOException {
        OutputStream os = null;
        os = urlcon.getOutputStream();
        os = new BufferedOutputStream(os);
        final OutputStreamWriter out = new OutputStreamWriter(os, "8859_1");
        out.write(s);
        out.flush();
        out.close();
        return os;
    }

    private URLConnection makeURLcon(final String s) throws URISyntaxException, IOException {
        URI uri;
        URL url;
        uri = new URI(s);
        url = uri.toURL();
        URLConnection urlcon = url.openConnection();
        return urlcon;
    }

    protected String makeNameAndValue(String name, String value) {
        String nameAndValue = name + "=" + value;
        return nameAndValue;
    }

    protected LinkedBlockingDeque<String> getinputString(final String s) {
        final LinkedBlockingDeque<String> inputDeque = new LinkedBlockingDeque<String>();
        final String inputRegex = "<input[^>]+>";
        final Matcher inputMatcher = Pattern.compile(inputRegex).matcher(s);
        while (inputMatcher.find()) inputDeque.add(inputMatcher.group());
        return inputDeque;
    }
}
