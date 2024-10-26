import java.io.*;
import java.net.*;

/** <p>This class accept lot of stream input sources and can return split outputs.</p>
 * <p>So it have lot of constructors, like:
 * <ul>
 * <li><code>public InputSplitter()</code><br />
 *&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;empty: you'll need to set a stream later with <code>newStream</code> methods;</li>
 * <li><code>public InputSplitter(BufferedReader fBuffFile)</code><br />
 *&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;direct access to given <code>BufferedReader</code> object, not a new or copy;</li>
 * <li><code>public InputSplitter(FileReader fReadFile)</code><br /></li>
 * <li><code>public InputSplitter(String fileName, boolean blnCreate)</code><br />
 *&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;reads <code>fileName</code> and create it, if not found, when <code>blnCreate</code> is <code>true</code>;</li>
 * <li><code>public InputSplitter(String fileName)</code><br />
 *&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;reads <code>fileName</code> and never create it;</li>
 * <li><code>public InputSplitter(InputStreamReader inputStream)</code><br /></li>
 * <li><code>public InputSplitter(HttpURLConnection hucRead)</code><br /></li>
 * <li><code>public InputSplitter(URL urlRead)</code><br /></li>
 * <li><code>public InputSplitter(String strURL, String strArgs)</code><br />
 *&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;needs two String argument (first is clean URL, and seconds are arguments or empty) and concatenate them as a sole address to try to connect online and get HTML output.</li>
 *</ul></p>
 * <p>Independently  of what's your constructor, it will obtain a BufferedReader object at the end.</p>
 * <p>There is a <code>newStream</code> method constructor-like for each constructor, except empty arguments type.</p> **/
public class InputNavigator implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    protected boolean blnVerbose = false;

    protected BufferedReader buffRead = null;

    /** Initialize an empty object, you'll need to make it have a stream later with <code>newStream</code> methods. **/
    public InputNavigator() {
    }

    /** Initialize a new object streaming from an initialized BufferedReader object. **/
    public InputNavigator(BufferedReader fBuffFile) {
        newStream(fBuffFile);
    }

    /** Initialize a new object streaming from an initialized FileReader object. **/
    public InputNavigator(FileReader fReadFile) {
        newStream(fReadFile);
    }

    /** <p>Initialize a new object streaming from a file named <code>fileName</code>.<br />
	 * <li><code>blnCreate</code>: if <code>true</code>, it will try to create an empty file, if doesn't exists.</p><br/>
	 * @throws IOException **/
    public InputNavigator(String fileName, boolean blnCreate) throws IOException {
        newStream(fileName, blnCreate);
    }

    /** <p>Initialize a new object streaming from a file named <code>fileName</code>.</p>
	 * <p>It will throw error if <code>fileName</code> doesn't exists.</p>
	 * @throws IOException **/
    public InputNavigator(String fileName) throws IOException {
        newStream(fileName, false);
    }

    /** Initialize a new object streaming from an Input Streamer object. **/
    public InputNavigator(InputStreamReader inputStream) {
        newStream(inputStream);
    }

    /** Initialize a new object streaming from an URL object. 
	 * @throws IOException **/
    public InputNavigator(HttpURLConnection hucRead) throws IOException {
        newStream(hucRead);
    }

    /** Initialize a new object streaming from an URL object. 
	 * @throws IOException **/
    public InputNavigator(URL urlRead) throws IOException {
        newStream(urlRead);
    }

    /** <p>Initialize a new object streaming from a given textual <code>URL</code>.<br />
	 * It needs a second string argument for additional text to be added to clean URL, or at least an empty string.</p>
	 * @throws IOException **/
    public InputNavigator(String strURL, String strArgs) throws IOException {
        newStream(strURL, strArgs);
    }

    /** <p>Initialize a new object streaming from a given <code>socket</code> connection.<br />
	 * @throws IOException **/
    public InputNavigator(Socket socketInput) throws IOException {
        newStream(new InputStreamReader(socketInput.getInputStream()));
    }

    /** <p>Initialize a new object streaming from a given <code>host</code> and <code>port</code> connection.<br />
	 * The port is NOT optional.</p>
	 * @throws IOException **/
    public InputNavigator(String strHost, int intPort) throws IOException {
        newStream(new Socket(strHost, intPort));
    }

    /** Re-set stream from an initialized BufferedReader object. **/
    public void newStream(BufferedReader fBuffFile) {
        this.buffRead = fBuffFile;
    }

    /** Re-set stream from an initialized FileReader object. **/
    public void newStream(FileReader fReadFile) {
        newStream(new BufferedReader(fReadFile));
    }

    /** Re-set stream from an Input Streamer object. **/
    public void newStream(InputStreamReader inputStream) {
        newStream(new BufferedReader(inputStream));
    }

    /** <p>Re-set stream from a file named <code>fileName</code>.<br />
	 * <li><code>blnCreate</code>: if <code>true</code>, will try to create an empty file, if doesn't exists.</p><br/>
	 * @throws IOException **/
    public void newStream(String fileName, boolean blnCreate) throws IOException {
        if (blnCreate) if (!fileExists(fileName)) fileNew(fileName);
        newStream(new FileReader(fileName));
    }

    /** <p>Re-set stream from a file named <code>fileName</code>.</p>
	 * <p>It will throw error if <code>fileName</code> doesn't exists.</p>
	 * @throws IOException **/
    public void newStream(String fileName) throws IOException {
        newStream(fileName, false);
    }

    /** Re-set stream from an URL object. 
	 * @throws IOException **/
    public void newStream(HttpURLConnection hucRead) throws IOException {
        if (hucRead.getResponseCode() == HttpURLConnection.HTTP_OK) {
            if (blnVerbose) System.out.println("Connection succeeded!!!");
            newStream(new InputStreamReader(hucRead.getInputStream()));
        } else if (blnVerbose) System.out.println("ERROR: connection failed.");
    }

    /** Re-set stream from an URL object. 
	 * @throws IOException **/
    public void newStream(URL urlRead) throws IOException {
        newStream((HttpURLConnection) urlRead.openConnection());
    }

    /** <p>Re-set stream from a given textual <code>URL</code>.<br />
	 * It needs a second string argument for additional text to add to clean URL, or at least empty String.</p>
	 * @throws IOException **/
    public void newStream(String strURL, String strArgs) throws IOException {
        newStream(new URL(strURL + strArgs));
    }

    /** <p>Re-set stream from a given <code>socket</code> connection.<br />
	 * @throws IOException **/
    public void newStream(Socket socketInput) throws IOException {
        newStream(new InputStreamReader(socketInput.getInputStream()));
    }

    /** <p>Re-set stream from a given <code>host</code> and <code>port</code> connection.<br />
	 * The port is NOT optional.</p>
	 * @throws IOException **/
    public void newStream(String strHost, int intPort) throws IOException {
        newStream(new Socket(strHost, intPort));
    }

    public String readLine() throws IOException {
        return buffRead.readLine();
    }

    public String[] readSplitLine(String strRegex, int intLimit) throws IOException {
        String[] strLines = null;
        String strLine = buffRead.readLine();
        if (strLine != null) strLines = strLine.split(strRegex, intLimit);
        return strLines;
    }

    public String[] readSplitLine(String strRegex) throws IOException {
        return readSplitLine(strRegex, 0);
    }

    public String fastForward(String strSearch, boolean blnRegexMatch) throws IOException {
        boolean blnContinue = false;
        String strLine = null;
        do {
            strLine = buffRead.readLine();
            if (strLine != null) if (blnRegexMatch) blnContinue = !strLine.matches(strSearch); else blnContinue = !strLine.contains(strSearch);
        } while (blnContinue && strLine != null);
        return strLine;
    }

    public String fastForward(String strSearch) throws IOException {
        return fastForward(strSearch, false);
    }

    public String[] fastSplitForward(String strSearch, String strSplitRegex, int intLimit, boolean blnRegexMatch) throws IOException {
        String[] strLines = null;
        String strLine = fastForward(strSearch, blnRegexMatch);
        if (strLine != null) strLines = strLine.split(strSplitRegex, intLimit);
        return strLines;
    }

    public String[] fastSplitForward(String strSearch, String strSplitRegex, boolean blnRegexMatch) throws IOException {
        return fastSplitForward(strSearch, strSplitRegex, 0, blnRegexMatch);
    }

    public String[] fastSplitForward(String strSearch, String strSplitRegex, int intLimit) throws IOException {
        return fastSplitForward(strSearch, strSplitRegex, intLimit, false);
    }

    public String[] fastSplitForward(String strSearch, String strSplitRegex) throws IOException {
        return fastSplitForward(strSearch, strSplitRegex, 0, false);
    }

    public void close() throws IOException {
        if (buffRead != null) buffRead.close();
    }

    /** <h1>fileExists</b></h1>
	 * <pre>public static boolean <b>fileExist</b>(String fileName)</pre>
	 * <p>Returns <code>true</code> if <code>fileName</code> exists,
	 *  <code>false</code> otherwise.</p> **/
    public static boolean fileExists(String fileName) {
        boolean blnExists = false;
        File file = new File(fileName);
        blnExists = file.exists();
        return blnExists;
    }

    /** <h1>fileNew</b></h1>
	 * <pre>public static boolean <b>fileNew</b>(String fileName)</pre>
	 * <p>Create a new empty file named <code>fileName</code>.</p> 
	 * @throws IOException **/
    public static boolean fileNew(String fileName) throws IOException {
        boolean blnCreated = false;
        File file = new File(fileName);
        blnCreated = file.createNewFile();
        return blnCreated;
    }

    public boolean isBlnVerbose() {
        return blnVerbose;
    }

    public BufferedReader getBufferedReader() {
        return buffRead;
    }

    public void setBlnVerbose(boolean blnVerbose) {
        this.blnVerbose = blnVerbose;
    }
}
