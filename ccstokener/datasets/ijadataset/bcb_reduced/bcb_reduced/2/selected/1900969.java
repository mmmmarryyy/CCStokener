package net.beasainwireless.guregipuzkoa.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.xml.bind.JAXB;
import net.beasainwireless.guregipuzkoa.api.xml.GGError;
import net.beasainwireless.guregipuzkoa.api.xml.GGLicenses;
import net.beasainwireless.guregipuzkoa.api.xml.GGMunicipalities;
import net.beasainwireless.guregipuzkoa.api.xml.GGPhotoInfo;
import net.beasainwireless.guregipuzkoa.api.xml.GGPhotoSearch;
import net.beasainwireless.guregipuzkoa.api.xml.GGProvinces;
import net.beasainwireless.guregipuzkoa.api.xml.GGToken;
import net.beasainwireless.guregipuzkoa.api.xml.GGUser;
import net.beasainwireless.guregipuzkoa.api.xml.GGUserInfo;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

public class GGClient {

    public static final int HTTP_STATUS_OK = 200;

    private static final String REST_URL = "http://api.guregipuzkoa.net/services/rest/";

    private static final String UPLOAD_URL = "http://api.guregipuzkoa.net/services/upload/";

    public static final SimpleDateFormat isoDate = new SimpleDateFormat("yyyy-MM-dd");

    public static final SimpleDateFormat isoDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final SimpleDateFormat isoTime = new SimpleDateFormat("HH:mm:ss");

    private HttpClient httpClient = null;

    private String key = null;

    private String token = null;

    @SuppressWarnings("unused")
    private String user = null;

    public GGClient(String key) {
        this.key = key;
    }

    public void close() {
    }

    /**
	 * Checks if a REST operations has returned a succesful result.
	 * 
	 * If the error is due to some misuse or failure of the API a GGException
	 * will be thrown showing the info given by the API.
	 * 
	 * If the error is an HTTP related error or not controlled by the API other
	 * exceptions are thrown.
	 * 
	 * If everything goes right the method just finishes returning boolean true.
	 * 
	 * @param response
	 * @param status
	 * @return boolean
	 * @throws IOException
	 * @throws IllegalStateException
	 * @throws GGException
	 * @throws Exception
	 */
    protected boolean errorCheck(HttpResponse response, int status) throws IOException, IllegalStateException, GGException, Exception {
        if (status != HttpStatus.SC_OK) {
            InputStream content = response.getEntity().getContent();
            try {
                GGError error = JAXB.unmarshal(content, GGError.class);
                throw new GGException(error);
            } catch (GGException gge) {
                throw gge;
            } catch (Exception e) {
                e.printStackTrace();
                throw new Exception("Falló la petición HTTP, status code devuelto: " + status);
            }
        }
        return true;
    }

    public GGUser findByUsername(String userName) throws IllegalStateException, GGException, Exception {
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("method", "gg.people.findByUsername"));
        qparams.add(new BasicNameValuePair("key", this.key));
        qparams.add(new BasicNameValuePair("username", userName));
        String url = REST_URL + "?" + URLEncodedUtils.format(qparams, "UTF-8");
        URI uri = new URI(url);
        HttpGet httpget = new HttpGet(uri);
        HttpResponse response = httpClient.execute(httpget);
        int status = response.getStatusLine().getStatusCode();
        errorCheck(response, status);
        InputStream content = response.getEntity().getContent();
        GGUser user = JAXB.unmarshal(content, GGUser.class);
        return user;
    }

    public GGLicenses getLicensesInfo() throws IllegalStateException, GGException, Exception {
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("method", "gg.photos.licenses.getInfo"));
        qparams.add(new BasicNameValuePair("key", this.key));
        String url = REST_URL + "?" + URLEncodedUtils.format(qparams, "UTF-8");
        URI uri = new URI(url);
        HttpGet httpget = new HttpGet(uri);
        HttpResponse response = httpClient.execute(httpget);
        int status = response.getStatusLine().getStatusCode();
        errorCheck(response, status);
        InputStream content = response.getEntity().getContent();
        GGLicenses licenses = JAXB.unmarshal(content, GGLicenses.class);
        return licenses;
    }

    public GGMunicipalities getListMunicipalities() throws IllegalStateException, GGException, Exception {
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("method", "gg.photos.geo.getListMunicipality"));
        qparams.add(new BasicNameValuePair("key", this.key));
        String url = REST_URL + "?" + URLEncodedUtils.format(qparams, "UTF-8");
        URI uri = new URI(url);
        HttpGet httpget = new HttpGet(uri);
        HttpResponse response = httpClient.execute(httpget);
        int status = response.getStatusLine().getStatusCode();
        errorCheck(response, status);
        InputStream content = response.getEntity().getContent();
        GGMunicipalities municipalities = JAXB.unmarshal(content, GGMunicipalities.class);
        return municipalities;
    }

    public GGProvinces getListProvinces() throws IllegalStateException, GGException, Exception {
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("method", "gg.photos.geo.getListProvinces"));
        qparams.add(new BasicNameValuePair("key", this.key));
        String url = REST_URL + "?" + URLEncodedUtils.format(qparams, "UTF-8");
        URI uri = new URI(url);
        HttpGet httpget = new HttpGet(uri);
        HttpResponse response = httpClient.execute(httpget);
        int status = response.getStatusLine().getStatusCode();
        errorCheck(response, status);
        InputStream content = response.getEntity().getContent();
        GGProvinces provinces = JAXB.unmarshal(content, GGProvinces.class);
        return provinces;
    }

    public GGPhotoInfo getPhotoInfo(String photoId, String language) throws IllegalStateException, GGException, Exception {
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("method", "gg.photos.getInfo"));
        qparams.add(new BasicNameValuePair("key", this.key));
        qparams.add(new BasicNameValuePair("photo_id", photoId));
        if (null != language) {
            qparams.add(new BasicNameValuePair("language", language));
        }
        String url = REST_URL + "?" + URLEncodedUtils.format(qparams, "UTF-8");
        URI uri = new URI(url);
        HttpGet httpget = new HttpGet(uri);
        HttpResponse response = httpClient.execute(httpget);
        int status = response.getStatusLine().getStatusCode();
        errorCheck(response, status);
        InputStream content = response.getEntity().getContent();
        GGPhotoInfo photo = JAXB.unmarshal(content, GGPhotoInfo.class);
        return photo;
    }

    public GGToken getToken(String username, String password) throws IllegalStateException, GGException, Exception {
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("method", "gg.auth.getToken"));
        qparams.add(new BasicNameValuePair("key", this.key));
        qparams.add(new BasicNameValuePair("username", username));
        qparams.add(new BasicNameValuePair("password", password));
        String url = REST_URL + "?" + URLEncodedUtils.format(qparams, "UTF-8");
        URI uri = new URI(url);
        HttpGet httpget = new HttpGet(uri);
        HttpResponse response = httpClient.execute(httpget);
        int status = response.getStatusLine().getStatusCode();
        errorCheck(response, status);
        InputStream content = response.getEntity().getContent();
        GGToken token = JAXB.unmarshal(content, GGToken.class);
        this.token = token.auth.token;
        this.user = token.auth.user.name;
        return token;
    }

    public GGUserInfo getUserInfo(String userId) throws IllegalStateException, GGException, Exception {
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("method", "gg.people.getInfo"));
        qparams.add(new BasicNameValuePair("key", this.key));
        qparams.add(new BasicNameValuePair("user_id", userId));
        String url = REST_URL + "?" + URLEncodedUtils.format(qparams, "UTF-8");
        URI uri = new URI(url);
        HttpGet httpget = new HttpGet(uri);
        HttpResponse response = httpClient.execute(httpget);
        int status = response.getStatusLine().getStatusCode();
        errorCheck(response, status);
        InputStream content = response.getEntity().getContent();
        GGUserInfo userInfo = JAXB.unmarshal(content, GGUserInfo.class);
        return userInfo;
    }

    public void open() {
        this.httpClient = new DefaultHttpClient();
    }

    public GGPhotoSearch photosSearch(String language, String userId, String tags, String tagsMode, String text, String license, Date minUploadedDate, Date maxUploadedDate, Date minTakenDate, Date maxTakenDate, Date minStartDate, Date maxStartDate, Date minEndDate, Date maxEndDate, String page, String perPage) throws IllegalStateException, GGException, Exception {
        List<NameValuePair> qparams = new ArrayList<NameValuePair>();
        qparams.add(new BasicNameValuePair("method", "gg.photos.search"));
        qparams.add(new BasicNameValuePair("key", this.key));
        if (null != language) {
            qparams.add(new BasicNameValuePair("language", language));
        }
        if (null != userId) {
            qparams.add(new BasicNameValuePair("user_id", userId));
        }
        if (null != tags) {
            qparams.add(new BasicNameValuePair("tags", tags));
        }
        if (null != tagsMode) {
            qparams.add(new BasicNameValuePair("tags_mode", tagsMode));
        }
        if (null != text) {
            qparams.add(new BasicNameValuePair("text", text));
        }
        if (null != license) {
            qparams.add(new BasicNameValuePair("license", license));
        }
        if (null != minUploadedDate) {
            String minUploadedDateString = isoDate.format(minUploadedDate);
            qparams.add(new BasicNameValuePair("min_uploaded_date", minUploadedDateString));
        }
        if (null != maxUploadedDate) {
            String maxUploadedDateString = isoDate.format(maxUploadedDate);
            qparams.add(new BasicNameValuePair("max_uploaded_date", maxUploadedDateString));
        }
        if (null != minTakenDate) {
            String minTakenDateString = isoDate.format(minTakenDate);
            qparams.add(new BasicNameValuePair("min_taken_date", minTakenDateString));
        }
        if (null != maxTakenDate) {
            String maxTakenDateString = isoDate.format(maxTakenDate);
            qparams.add(new BasicNameValuePair("max_taken_date", maxTakenDateString));
        }
        if (null != minStartDate) {
            String minStartDateString = isoDate.format(minStartDate);
            qparams.add(new BasicNameValuePair("min_start_date", minStartDateString));
        }
        if (null != maxStartDate) {
            String maxStartDateString = isoDate.format(maxStartDate);
            qparams.add(new BasicNameValuePair("max_start_date", maxStartDateString));
        }
        if (null != minEndDate) {
            String minEndDateString = isoDate.format(minEndDate);
            qparams.add(new BasicNameValuePair("min_end_date", minEndDateString));
        }
        if (null != maxEndDate) {
            String maxEndDateString = isoDate.format(maxEndDate);
            qparams.add(new BasicNameValuePair("max_end_date", maxEndDateString));
        }
        if (null != page) {
            qparams.add(new BasicNameValuePair("page", page));
        }
        if (null != perPage) {
            qparams.add(new BasicNameValuePair("per_page", perPage));
        }
        String url = REST_URL + "?" + URLEncodedUtils.format(qparams, "UTF-8");
        URI uri = new URI(url);
        HttpGet httpget = new HttpGet(uri);
        HttpResponse response = httpClient.execute(httpget);
        int status = response.getStatusLine().getStatusCode();
        errorCheck(response, status);
        InputStream content = response.getEntity().getContent();
        GGPhotoSearch photoSearch = JAXB.unmarshal(content, GGPhotoSearch.class);
        return photoSearch;
    }

    public GGPhotoInfo upload(File file, String provinceId, String municipalityId, String folderId, String licenseId, String sourceId, String title, String description, String titleEs, String descriptionEs, String titleEu, String descriptionEu, Date date, Boolean isDateUncertain, String photographer, String signature, Time time, Date startDate, Date endDate, String latitude, String longitude, String language, String tags) throws IllegalStateException, GGException, Exception {
        HttpPost httpPost = new HttpPost(UPLOAD_URL);
        FileBody photoFile = new FileBody(file);
        MultipartEntity reqEntity = new MultipartEntity();
        reqEntity.addPart("photo", photoFile);
        reqEntity.addPart("license_id", new StringBody(licenseId));
        if (null != provinceId) {
            reqEntity.addPart("province_id", new StringBody(provinceId));
        }
        if (null != municipalityId) {
            reqEntity.addPart("municipality_id", new StringBody(municipalityId));
        }
        if (null != folderId) {
            reqEntity.addPart("folder_id", new StringBody(folderId));
        }
        if (null != sourceId) {
            reqEntity.addPart("source_id", new StringBody(sourceId));
        }
        if (null != title) {
            reqEntity.addPart("title", new StringBody(title));
        }
        if (null != description) {
            reqEntity.addPart("description", new StringBody(description));
        }
        if (null != titleEs) {
            reqEntity.addPart("title_es", new StringBody(titleEs));
        }
        if (null != descriptionEs) {
            reqEntity.addPart("description_es", new StringBody(descriptionEs));
        }
        if (null != titleEu) {
            reqEntity.addPart("title_eu", new StringBody(titleEu));
        }
        if (null != descriptionEu) {
            reqEntity.addPart("description_eu", new StringBody(descriptionEu));
        }
        if (null != date) {
            String dateString = isoDate.format(date);
            reqEntity.addPart("date", new StringBody(dateString));
        }
        if (null != isDateUncertain) {
            String booleanString = isDateUncertain ? "true" : "false";
            reqEntity.addPart("is_date_uncertain", new StringBody(booleanString));
        }
        if (null != photographer) {
            reqEntity.addPart("photographer", new StringBody(photographer));
        }
        if (null != signature) {
            reqEntity.addPart("signature", new StringBody(signature));
        }
        if (null != time) {
            String timeString = isoTime.format(time);
            reqEntity.addPart("time", new StringBody(timeString));
        }
        if (null != startDate) {
            String dateString = isoDate.format(startDate);
            reqEntity.addPart("start_date", new StringBody(dateString));
        }
        if (null != endDate) {
            String dateString = isoDate.format(endDate);
            reqEntity.addPart("end_date", new StringBody(dateString));
        }
        if (null != latitude) {
            reqEntity.addPart("latitude", new StringBody(latitude));
        }
        if (null != longitude) {
            reqEntity.addPart("longitude", new StringBody(longitude));
        }
        if (null != language) {
            reqEntity.addPart("language", new StringBody(language));
        }
        if (null != tags) {
            reqEntity.addPart("tags", new StringBody(tags));
        }
        reqEntity.addPart("token", new StringBody(this.token));
        reqEntity.addPart("key", new StringBody(this.key));
        httpPost.setEntity(reqEntity);
        HttpResponse response = httpClient.execute(httpPost);
        int status = response.getStatusLine().getStatusCode();
        errorCheck(response, status);
        InputStream content = response.getEntity().getContent();
        GGPhotoInfo photo = JAXB.unmarshal(content, GGPhotoInfo.class);
        return photo;
    }
}
