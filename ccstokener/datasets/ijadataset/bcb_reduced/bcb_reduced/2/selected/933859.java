package org.magnos.asset.source;

import java.io.InputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.regex.Pattern;
import org.magnos.asset.base.BaseAssetSource;

/**
 * A source that reads assets from an FTP server.
 * 
 * @author Philip Diffenderfer
 *
 */
public class FtpSource extends BaseAssetSource {

    public static final Pattern REGEX_VALID = Pattern.compile("^ftp\\://[^:]+\\:[^@]+@/?([a-zA-Z0-9\\-\\._\\?\\,\\'/\\\\\\+&amp;%\\$#\\=~])*$");

    private final String url;

    /**
	 * Instantiates a new FtpSource.
	 * 
	 * @param host
	 * 		The host name (or IP) of the FTP server, as well as the directory 
	 * 		path where the assets are contained. This host often should end in 
	 * 		a slash (/) unless one has already been specified in the host and 
	 * 		the remainder of the host name is a prefix to a set of assets.
	 * @param username
	 * 		The user name to use sign into the server.
	 * @param password
	 * 		The password to use to sign into the server.
	 */
    public FtpSource(String host, String username, String password) {
        super(REGEX_VALID, null, "");
        url = String.format("ftp://%s:%s@%s{0};type=i", username, password, host);
    }

    /**
	 * The URL of the FTP server where there exists an {0} in the place where 
	 * the request will be inserted.
	 * 
	 * @return
	 * 		The reference to the URL String.
	 */
    public String getUrl() {
        return url;
    }

    @Override
    public String getAbsolute(String request) {
        return MessageFormat.format(url, request);
    }

    @Override
    public InputStream getStream(String request) throws Exception {
        URL url = new URL(getAbsolute(request));
        return url.openStream();
    }
}
