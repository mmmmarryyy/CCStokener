package org.dspace.license;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.sql.SQLException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;

public class CreativeCommons {

    /**
     * The Bundle Name
     */
    public static final String CC_BUNDLE_NAME = "CC-LICENSE";

    private static final String CC_BS_SOURCE = "org.dspace.license.CreativeCommons";

    /**
     * Some BitStream Names (BSN)
     */
    private static final String BSN_LICENSE_URL = "license_url";

    private static final String BSN_LICENSE_TEXT = "license_text";

    private static final String BSN_LICENSE_RDF = "license_rdf";

    private static boolean enabled_p;

    static {
        enabled_p = ConfigurationManager.getBooleanProperty("webui.submit.enable-cc");
        if (enabled_p) {
            String proxyHost = ConfigurationManager.getProperty("http.proxy.host");
            String proxyPort = ConfigurationManager.getProperty("http.proxy.port");
            if ((proxyHost != null) && (proxyPort != null)) {
                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", proxyPort);
            }
        }
    }

    /**
     * Simple accessor for enabling of CC
     */
    public static boolean isEnabled() {
        return enabled_p;
    }

    private static Bundle getCcBundle(Item item) throws SQLException, AuthorizeException, IOException {
        Bundle[] bundles = item.getBundles(CC_BUNDLE_NAME);
        if ((bundles.length > 0) && (bundles[0] != null)) {
            item.removeBundle(bundles[0]);
        }
        return item.createBundle(CC_BUNDLE_NAME);
    }

    /**
     * This is a bit of the "do-the-right-thing" method for CC stuff in an item
     */
    public static void setLicense(Context context, Item item, String cc_license_url) throws SQLException, IOException, AuthorizeException {
        Bundle bundle = getCcBundle(item);
        String license_text = fetchLicenseText(cc_license_url);
        String license_rdf = fetchLicenseRDF(cc_license_url);
        int license_start = license_rdf.indexOf("<License");
        int license_end = license_rdf.indexOf("</License>") + 10;
        String document_rdf = "<rdf:RDF xmlns=\"http://web.resource.org/cc/\"\n" + "   xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" + "   xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" + "<Work rdf:about=\"\">\n" + "<license rdf:resource=\"" + cc_license_url + "\">\n" + "</Work>\n\n" + license_rdf.substring(license_start, license_end) + "\n\n</rdf:RDF>";
        BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(context, "License");
        setBitstreamFromBytes(item, bundle, BSN_LICENSE_URL, bs_format, cc_license_url.getBytes());
        setBitstreamFromBytes(item, bundle, BSN_LICENSE_TEXT, bs_format, license_text.getBytes());
        setBitstreamFromBytes(item, bundle, BSN_LICENSE_RDF, bs_format, document_rdf.getBytes());
    }

    public static void setLicense(Context context, Item item, InputStream licenseStm, String mimeType) throws SQLException, IOException, AuthorizeException {
        Bundle bundle = getCcBundle(item);
        BitstreamFormat bs_format = BitstreamFormat.findByShortDescription(context, "License");
        Bitstream bs = bundle.createBitstream(licenseStm);
        bs.setSource(CC_BS_SOURCE);
        bs.setName((mimeType != null && (mimeType.equalsIgnoreCase("text/xml") || mimeType.equalsIgnoreCase("text/rdf"))) ? BSN_LICENSE_RDF : BSN_LICENSE_TEXT);
        bs.setFormat(bs_format);
        bs.update();
    }

    public static void removeLicense(Context context, Item item) throws SQLException, IOException, AuthorizeException {
        Bundle[] bundles = item.getBundles(CC_BUNDLE_NAME);
        if ((bundles.length > 0) && (bundles[0] != null)) {
            item.removeBundle(bundles[0]);
        }
    }

    public static boolean hasLicense(Context context, Item item) throws SQLException, IOException {
        Bundle[] bundles = item.getBundles(CC_BUNDLE_NAME);
        if (bundles.length == 0) {
            return false;
        }
        try {
            if ((getLicenseURL(item) == null) || (getLicenseText(item) == null) || (getLicenseRDF(item) == null)) {
                return false;
            }
        } catch (AuthorizeException ae) {
            return false;
        }
        return true;
    }

    public static String getLicenseURL(Item item) throws SQLException, IOException, AuthorizeException {
        return getStringFromBitstream(item, BSN_LICENSE_URL);
    }

    public static String getLicenseText(Item item) throws SQLException, IOException, AuthorizeException {
        return getStringFromBitstream(item, BSN_LICENSE_TEXT);
    }

    public static String getLicenseRDF(Item item) throws SQLException, IOException, AuthorizeException {
        return getStringFromBitstream(item, BSN_LICENSE_RDF);
    }

    /**
     * Get Creative Commons license RDF, returning Bitstream object.
     * @return bitstream or null.
     */
    public static Bitstream getLicenseRdfBitstream(Item item) throws SQLException, IOException, AuthorizeException {
        return getBitstream(item, BSN_LICENSE_RDF);
    }

    /**
     * Get Creative Commons license Text, returning Bitstream object.
     * @return bitstream or null.
     */
    public static Bitstream getLicenseTextBitstream(Item item) throws SQLException, IOException, AuthorizeException {
        return getBitstream(item, BSN_LICENSE_TEXT);
    }

    /**
     * Get a few license-specific properties. We expect these to be cached at
     * least per server run.
     */
    public static String fetchLicenseText(String license_url) {
        String text_url = license_url;
        byte[] urlBytes = fetchURL(text_url);
        return (urlBytes != null) ? new String(urlBytes) : "";
    }

    public static String fetchLicenseRDF(String license_url) {
        String rdf_url = license_url + "rdf";
        byte[] urlBytes = fetchURL(rdf_url);
        return (urlBytes != null) ? new String(urlBytes) : "";
    }

    /**
     * This helper method takes some bytes and stores them as a bitstream for an
     * item, under the CC bundle, with the given bitstream name
     */
    private static void setBitstreamFromBytes(Item item, Bundle bundle, String bitstream_name, BitstreamFormat format, byte[] bytes) throws SQLException, IOException, AuthorizeException {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        Bitstream bs = bundle.createBitstream(bais);
        bs.setName(bitstream_name);
        bs.setSource(CC_BS_SOURCE);
        bs.setFormat(format);
        bs.update();
    }

    /**
     * This helper method wraps a String around a byte array returned from the
     * bitstream method further down
     */
    private static String getStringFromBitstream(Item item, String bitstream_name) throws SQLException, IOException, AuthorizeException {
        byte[] bytes = getBytesFromBitstream(item, bitstream_name);
        if (bytes == null) {
            return null;
        }
        return new String(bytes);
    }

    /**
     * This helper method retrieves the bytes of a bitstream for an item under
     * the CC bundle, with the given bitstream name
     */
    private static Bitstream getBitstream(Item item, String bitstream_name) throws SQLException, IOException, AuthorizeException {
        Bundle cc_bundle = null;
        try {
            Bundle[] bundles = item.getBundles(CC_BUNDLE_NAME);
            if ((bundles != null) && (bundles.length > 0)) {
                cc_bundle = bundles[0];
            } else {
                return null;
            }
        } catch (Exception exc) {
            return null;
        }
        return cc_bundle.getBitstreamByName(bitstream_name);
    }

    private static byte[] getBytesFromBitstream(Item item, String bitstream_name) throws SQLException, IOException, AuthorizeException {
        Bitstream bs = getBitstream(item, bitstream_name);
        if (bs == null) {
            return null;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Utils.copy(bs.retrieve(), baos);
        return baos.toByteArray();
    }

    /**
     * Fetch the contents of a URL
     */
    private static byte[] fetchURL(String url_string) {
        try {
            URL url = new URL(url_string);
            URLConnection connection = url.openConnection();
            byte[] bytes = new byte[connection.getContentLength()];
            int offset = 0;
            while (true) {
                int len = connection.getInputStream().read(bytes, offset, bytes.length - offset);
                if (len == -1) {
                    break;
                }
                offset += len;
            }
            return bytes;
        } catch (Exception exc) {
            return null;
        }
    }
}
