package de.shandschuh.jaolt.core;

import java.net.URL;
import java.util.Date;
import java.util.Properties;
import de.shandschuh.jaolt.core.updatechannel.StandardUpdateChannel;
import de.shandschuh.jaolt.gui.Lister;
import de.shandschuh.jaolt.tools.url.URLHelper;

public abstract class UpdateChannel {

    private String version;

    private String revision;

    private Date date;

    /**
	 * Returns the priority of this update-channel.
	 * 
	 * -currently not used-
	 * 
	 * @return Priority
	 */
    public abstract int getPriority();

    private static UpdateChannel currentChannel;

    public boolean isNewVersionAvailable() {
        return !Lister.version.equals(version) || !Lister.revision.equals(revision);
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getRevision() {
        return revision;
    }

    public void setRevision(String revision) {
        this.revision = revision;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    /**
	 * Updates the current version information (version and revision)
	 * 
	 * @throws Exception On any occurred error
	 */
    public abstract void updateVersionInformation() throws Exception;

    /**
	 * Returns the name of this update-channel.
	 * 
	 * @return name
	 */
    public abstract String getName();

    /**
	 * Updates the version information by a simple properties-file.
	 * 
	 * @param  url       Location of the properties-file
	 * @throws Exception On any occurred error
	 */
    protected void updateVersionInformation(URL url) throws Exception {
        Properties properties = new Properties();
        properties.load(url.openStream());
        setVersion("" + properties.get("version"));
        setRevision("" + properties.getProperty("revision"));
        setDate(new Date(Long.parseLong(properties.getProperty("date"))));
    }

    /**
	 * Returns the URL (page) of the download-links.
	 * 
	 * @return URL of the download-links
	 */
    public abstract URL getDownloadURL();

    /**
	 * Returns true if the update-channel supports update-information.
	 * 
	 * @return true if update-information is supported
	 */
    public boolean hasUpdateInformation() {
        return true;
    }

    /**
	 * Returns the URL where to post the error-information to.
	 * 
	 * @return URL for error-information
	 */
    public URL getErrorReportURL() {
        return URLHelper.createURL("https://jaoltwsn.appspot.com/errorreport");
    }

    /**
	 * Returns the URL that is the main URL of this distribution.
	 * 
	 * @return main URL
	 */
    public URL getMainURL() {
        return URLHelper.createURL("http://code.google.com/p/jaolt");
    }

    /**
	 * Returns the name of the auction-platform that should be selected if an account
	 * is beeing created.
	 * 
	 * @return name of the auction-platform 
	 */
    public String getStandardAuctionPlatformName() {
        return "eBay";
    }

    public boolean hasUpdateInformationFetched() {
        return hasUpdateInformation() && version != null && date != null;
    }

    public static UpdateChannel getCurrentChannel() {
        if (currentChannel == null) {
            try {
                Properties properties = new Properties();
                properties.load(UpdateChannel.class.getClassLoader().getResourceAsStream("update.channel"));
                currentChannel = (UpdateChannel) Class.forName(properties.getProperty("class")).newInstance();
            } catch (Exception exceptin) {
                currentChannel = new StandardUpdateChannel();
            }
        }
        return currentChannel;
    }

    public String getApplicationTitle() {
        return "jAOLT";
    }
}
