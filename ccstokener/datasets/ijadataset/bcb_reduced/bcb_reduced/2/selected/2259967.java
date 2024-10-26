package newgen.presentation.cataloguing;

/**
 *
 * @author  Administrator
 */
public class DataDownloader implements Runnable {

    /** Creates a new instance of DataDownloader */
    public DataDownloader(String urltext) {
        this.setUrl(urltext);
    }

    public void run() {
        try {
            java.net.URL url = new java.net.URL(this.getUrl());
            java.net.URLConnection urlconn = url.openConnection();
            urlconn.setDoInput(true);
            java.io.InputStream is = urlconn.getInputStream();
            org.jdom.input.SAXBuilder sb = new org.jdom.input.SAXBuilder();
            org.jdom.Document doc = sb.build(is);
            this.setText((new org.jdom.output.XMLOutputter()).outputString(doc));
        } catch (Exception exp) {
        }
    }

    /** Getter for property url.
     * @return Value of property url.
     *
     */
    public java.lang.String getUrl() {
        return url;
    }

    /** Setter for property url.
     * @param url New value of property url.
     *
     */
    public void setUrl(java.lang.String url) {
        this.url = url;
    }

    /** Getter for property text.
     * @return Value of property text.
     *
     */
    public java.lang.String getText() {
        return text;
    }

    /** Setter for property text.
     * @param text New value of property text.
     *
     */
    public void setText(java.lang.String text) {
        this.text = text;
    }

    private String url;

    private String text;
}
