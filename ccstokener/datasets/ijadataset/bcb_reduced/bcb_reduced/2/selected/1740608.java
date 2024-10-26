package jp.co.withone.osgi.gadget.flickrconnector;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import jp.co.withone.osgi.gadget.pictureviewer.PictureController;
import com.aetrion.flickr.Flickr;
import com.aetrion.flickr.people.User;
import com.aetrion.flickr.photos.Photo;
import com.aetrion.flickr.photos.PhotoList;
import com.aetrion.flickr.photos.PhotosInterface;
import com.aetrion.flickr.photos.SearchParameters;

public class PictureControllerImpl implements PictureController {

    private int index;

    @Override
    public byte[] getPicture() {
        final String apikey = FlickrConnectorSettingView.apikey;
        final String yahooID = FlickrConnectorSettingView.yahooID;
        if (apikey == null || apikey.equals("") || yahooID == null || yahooID.equals("")) {
            return null;
        }
        Flickr flickr = new Flickr(apikey);
        User user = null;
        try {
            user = flickr.getPeopleInterface().findByEmail(yahooID);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
        String id = user.getId();
        PhotosInterface photos = flickr.getPhotosInterface();
        SearchParameters param = new SearchParameters();
        param.setUserId(id);
        PhotoList photoList = null;
        try {
            photoList = photos.search(param, 100, 0);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
        final int size = photoList.size();
        if (size == 0) {
            return null;
        }
        this.index++;
        if (this.index >= size) {
            this.index = 0;
        }
        Photo photo = (Photo) photoList.get(this.index);
        String url = photo.getSmallUrl();
        try {
            InputStream is = new URL(url).openStream();
            return getImageByte(is);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private byte[] getImageByte(InputStream is) {
        try {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final BufferedInputStream bis = new BufferedInputStream(is);
            final byte[] b = new byte[1024];
            while (true) {
                final int count = bis.read(b);
                if (count == -1) {
                    break;
                }
                baos.write(b, 0, count);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
