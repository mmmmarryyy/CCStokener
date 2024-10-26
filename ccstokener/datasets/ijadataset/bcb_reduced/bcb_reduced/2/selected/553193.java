package reports.utility;

/**
 *
 * @author Administrator
 */
public class SystemFilesLoader {

    private static SystemFilesLoader thisInstance;

    private java.util.Properties newgenlibDesktopProperties;

    /** Creates a new instance of SystemFilesLoader */
    private SystemFilesLoader() {
        java.util.Properties props = new java.util.Properties();
        try {
            java.net.URL url = new java.net.URL(NewGenLibDesktopRoot.getInstance().getURLRoot() + "/SystemFiles/Env_Var.txt");
            props.load(url.openStream());
        } catch (Exception exp) {
            exp.printStackTrace();
        }
        newgenlibDesktopProperties = props;
    }

    public static SystemFilesLoader getInstance() {
        if (thisInstance == null) thisInstance = new SystemFilesLoader();
        return thisInstance;
    }

    /** Getter for property newgenlibProperties.
     * @return Value of property newgenlibProperties.
     *
     */
    public java.util.Properties getNewgenlibDesktopProperties() {
        return newgenlibDesktopProperties;
    }

    /** Setter for property newgenlibProperties.
     * @param newgenlibProperties New value of property newgenlibProperties.
     *
     */
    public void setNewgenlibDesktopProperties(java.util.Properties newgenlibDesktopProperties) {
        this.newgenlibDesktopProperties = newgenlibDesktopProperties;
    }
}
