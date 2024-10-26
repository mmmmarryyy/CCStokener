package com.packagetracker.carriers.handlers;

import java.io.OutputStreamWriter;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.SAXParserFactory;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import com.google.android.maps.MapActivity;
import com.packagetracker.R;
import com.packagetracker.carriers.PackageDetails;
import com.packagetracker.carriers.PackageLocation;

public class UpsHandler extends PackageHandler {

    private static final long serialVersionUID = -5505849714398253834L;

    public void parsePackageActivity(final MapActivity activity, final String number) {
        super.setMapActivity(activity);
        try {
            final HttpsURLConnection conn = (HttpsURLConnection) new URL(activity.getString(R.string.ups_url)).openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setHostnameVerifier(new AllowAllHostnameVerifier());
            conn.setRequestMethod("POST");
            final OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(this.getValidationData(number));
            writer.flush();
            writer.close();
            SAXParserFactory.newInstance().newSAXParser().parse(new InputSource(conn.getInputStream()), this);
        } catch (Exception e) {
        }
    }

    @Override
    public void startElement(final String uri, final String local, final String name, final Attributes atts) {
        if ("Activity".equals(local)) {
            this.setPackageDetails(new PackageDetails());
            this.setPackageLocation(new PackageLocation());
        }
        this.setTag(local);
    }

    @Override
    public void endElement(final String uri, final String local, final String name) {
        if ("Activity".equals(local)) {
            if (this.getPackageActivity().locationExists(this.getPackageLocation().getCity(), this.getPackageLocation().getStateProvinceCode())) {
                this.getPackageActivity().getLocation(this.getPackageLocation().getCity(), this.getPackageLocation().getStateProvinceCode()).addDetails(this.getPackageDetails());
            } else {
                this.getPackageLocation().addDetails(this.getPackageDetails());
                this.getPackageActivity().addLocation(this.getPackageLocation());
            }
            this.setPackageLocation(null);
            this.setPackageDetails(null);
        }
        if (this.getPackageLocation() != null) {
            if ("City".equals(local)) {
                this.getPackageLocation().setCity(this.getDataSet().getString());
            } else if ("StateProvinceCode".equals(local)) {
                this.getPackageLocation().setStateProvinceCode(this.getDataSet().getString());
            }
        }
        if (this.getPackageDetails() != null) {
            if ("Description".equals(local)) {
                this.getPackageDetails().setDescription(this.getDataSet().getString());
            } else if ("Date".equals(local)) {
                this.getPackageDetails().setDate(this.getDataSet().getString());
            } else if ("Time".equals(local)) {
                this.getPackageDetails().setTime(this.getDataSet().getString());
            }
        }
    }

    @Override
    public void characters(final char chars[], final int start, final int length) {
        if ("City".equals(this.getTag()) || "StateProvinceCode".equals(this.getTag()) || "Description".equals(this.getTag()) || "Date".equals(this.getTag()) || "Time".equals(this.getTag())) {
            this.getDataSet().setString(new String(chars, start, length));
        }
    }

    private String getValidationData(final String number) {
        final StringBuffer xml = new StringBuffer();
        xml.append("<?xml version=\"1.0\"?>");
        xml.append("<AccessRequest xml:lang=\"en-US\">");
        xml.append("<AccessLicenseNumber>");
        xml.append(this.getMapActivity().getString(R.string.ups_license_number));
        xml.append("</AccessLicenseNumber>");
        xml.append("<UserId>");
        xml.append(this.getMapActivity().getString(R.string.ups_userid));
        xml.append("</UserId>");
        xml.append("<Password>");
        xml.append(this.getMapActivity().getString(R.string.ups_password));
        xml.append("</Password>");
        xml.append("</AccessRequest>");
        xml.append("<?xml version=\"1.0\"?>");
        xml.append("<TrackRequest xml:lang=\"en-US\">");
        xml.append("<Request>");
        xml.append("<TransactionReference>");
        xml.append("<CustomerContext>Android Shipment Tracking</CustomerContext>");
        xml.append("<XpciVersion>");
        xml.append(this.getMapActivity().getString(R.string.ups_xpci_version));
        xml.append("</XpciVersion>");
        xml.append("</TransactionReference>");
        xml.append("<RequestAction>Track</RequestAction>");
        xml.append("<RequestOption>1</RequestOption>");
        xml.append("</Request>");
        xml.append("<TrackingNumber>");
        xml.append(number);
        xml.append("</TrackingNumber>");
        xml.append("</TrackRequest>");
        return xml.toString();
    }
}
