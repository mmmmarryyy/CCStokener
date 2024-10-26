package de.shandschuh.jaolt.core.auctionplatform.ebay;

import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import de.shandschuh.jaolt.core.ConfigurationValue;
import de.shandschuh.jaolt.core.auction.Picture;
import de.shandschuh.jaolt.core.auctionplatform.EBayAuctionPlatform;
import de.shandschuh.jaolt.core.pictureuploadservice.BasicPictureUploadService;
import de.shandschuh.jaolt.tools.url.URLHelper;

public class EPSPictureUploadService extends BasicPictureUploadService {

    private URL url;

    private int auctionPlatformSiteCode;

    private String token;

    public EPSPictureUploadService(URL url, int auctionPlatformSiteCode, String token) {
        this.url = url;
        this.auctionPlatformSiteCode = auctionPlatformSiteCode;
        this.token = token;
    }

    @Override
    public ConfigurationValue[] getConfigurationValues() {
        return null;
    }

    @Override
    public String getDescription() {
        return "EPS";
    }

    @Override
    public int getMaximumFileSize() {
        return 1024 * 1024;
    }

    @Override
    public String getName() {
        return "eBay picture sevice";
    }

    @Override
    public URL uploadPicture(Picture picture, ConfigurationValue[] vonfigurationValues) throws Exception {
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setRequestMethod("POST");
        connection.addRequestProperty("Content-Type", "multipart/form-data; boundary=MIME_boundery");
        connection.addRequestProperty("X-EBAY-API-COMPATIBILITY-LEVEL", EBayAuctionPlatform.COMPATIBILITY_LEVEL);
        connection.addRequestProperty("X-EBAY-API-CALL-NAME", "UploadSiteHostedPictures");
        connection.addRequestProperty("X-EBAY-API-SITEID", "" + auctionPlatformSiteCode);
        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.writeBytes("--");
        outputStream.writeBytes("MIME_boundery");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"XML Payload\"");
        outputStream.writeBytes("Content-Type: text/xml;charset=utf-8");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("\r\n");
        StringBuilder requestStringBuilder = new StringBuilder("<?xml version=\"1.0\" encoding=\"utf-8\"?><UploadSiteHostedPicturesRequest xmlns=\"urn:ebay:apis:eBLBaseComponents\"><RequesterCredentials><eBayAuthToken>");
        requestStringBuilder.append(token);
        requestStringBuilder.append("</eBayAuthToken></RequesterCredentials></UploadSiteHostedPicturesRequest>");
        outputStream.writeBytes(requestStringBuilder.toString());
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("--");
        outputStream.writeBytes("MIME_boundery");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("Content-Disposition: form-data; name=\"dummy\"; filename=\"dummy\"");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("Content-Transfer-Encoding: binary");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("Content-Type: application/octet-stream");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("\r\n");
        FileInputStream fileInputStream = new FileInputStream(picture.getFile());
        int maxBufferSize = 1024;
        int bytesAvailable = fileInputStream.available();
        int bufferSize = Math.min(bytesAvailable, maxBufferSize);
        byte[] buffer = new byte[bufferSize];
        int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        while (bytesRead > 0) {
            outputStream.write(buffer, 0, bufferSize);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
        }
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("--");
        outputStream.writeBytes("MIME_boundery");
        outputStream.writeBytes("--");
        outputStream.writeBytes("\r\n");
        outputStream.writeBytes("\r\n");
        outputStream.flush();
        outputStream.close();
        Element response = new SAXBuilder().build(connection.getInputStream()).getRootElement();
        EBayAuctionPlatform.checkResponseForExceptions(response);
        return URLHelper.createURL(response.getChild("SiteHostedPictureDetails").getChildText("FullURL"));
    }

    @Override
    public boolean isUsable() {
        return true;
    }
}
