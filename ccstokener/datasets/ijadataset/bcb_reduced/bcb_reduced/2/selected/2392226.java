package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.SwingWorker;
import java.net.URL;
import javax.swing.JOptionPane;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * ImageSearcher searches images with titles or descriptions that contain
 * the search text. It publishes progress as it retrieves thumbnails, and it
 * updates the image list with intermediate results.
 *
 * @author John O'Conner
 */
public class ImageSearcher extends SwingWorker<List<ImageInfo>, ImageInfo> {

    private ImageSearcher() {
    }

    public ImageSearcher(DefaultListModel model, String key, String search) {
        this(model, key, search, 1);
    }

    public ImageSearcher(DefaultListModel model, String key, String search, int page) {
        this.model = model;
        this.key = key;
        this.search = search;
        this.page = page;
    }

    /**
     * The image service returns an XML document that contains information
     * about the matched images. Parse that document to extract that information
     * and create a list of ImageInfo objects.
     */
    private List<ImageInfo> parseImageInfo(InputStream is) {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser;
        List<ImageInfo> imageList = new ArrayList<ImageInfo>();
        SearchHandler handler = new SearchHandler(imageList);
        try {
            saxParser = factory.newSAXParser();
            saxParser.parse(is, handler);
        } catch (ParserConfigurationException ex) {
            ex.printStackTrace();
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return imageList;
    }

    @Override
    protected List<ImageInfo> doInBackground() {
        String searchURL = String.format("%s&api_key=%s" + "&per_page=%s" + "&text=%s", SEARCH_URL, key, MAX_IMAGES, search);
        Object strResults = null;
        InputStream is = null;
        URL url = null;
        List<ImageInfo> infoList = null;
        try {
            url = new URL(searchURL);
            is = url.openStream();
            infoList = parseImageInfo(is);
            retrieveAndProcessThumbnails(infoList);
        } catch (MalformedURLException mfe) {
            JOptionPane.showMessageDialog(null, "No Working Internet Connection/Or low bandwidth", "Error while Connecting", JOptionPane.ERROR_MESSAGE);
            mfe.printStackTrace();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "No Working Internet Connection/Or low bandwidth", "Error while Connecting", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    JOptionPane.showMessageDialog(null, "No Working Internet Connection/Or low bandwidth", "Error while Connecting", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return infoList;
    }

    private Icon retrieveThumbNail(String strImageUrl) {
        ImageIcon thumbNail = null;
        URL imgUrl = null;
        InputStream is = null;
        try {
            imgUrl = new URL(strImageUrl);
            thumbNail = new ImageIcon(imgUrl);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            thumbNail = null;
        }
        return thumbNail;
    }

    private void retrieveAndProcessThumbnails(List<ImageInfo> infoList) {
        for (int x = 0; x < infoList.size() && !isCancelled(); ++x) {
            ImageInfo info = infoList.get(x);
            String strImageUrl = String.format("%s/%s/%s_%s_s.jpg", IMAGE_URL, info.getServer(), info.getId(), info.getSecret());
            Icon thumbNail = retrieveThumbNail(strImageUrl);
            info.setThumbnail(thumbNail);
            publish(info);
            setProgress(100 * (x + 1) / infoList.size());
        }
    }

    /**
     * process is called as a result of this worker thread's calling the
     * publish method.
     *
     * As image thumbnails are retrieved, the worker publishes them to the
     * list model.
     *
     */
    protected void process(List<ImageInfo> infoList) {
        for (ImageInfo info : infoList) {
            if (isCancelled()) {
                break;
            }
            model.addElement(info);
        }
    }

    protected void process(ImageInfo... infoList) {
        for (ImageInfo info : infoList) {
            if (isCancelled()) {
                break;
            }
            model.addElement(info);
        }
    }

    @Override
    protected void done() {
        setProgress(100);
        if (isCancelled()) {
            return;
        }
        List<ImageInfo> infoList = null;
        try {
            infoList = (List<ImageInfo>) get();
        } catch (ExecutionException ex) {
            JOptionPane.showMessageDialog(null, "No Working Internet Connection/Or low bandwidth", "Error while Connecting", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        } catch (InterruptedException ex) {
            JOptionPane.showMessageDialog(null, "No Working Internet Connection/Or low bandwidth", "Error while Connecting", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
        if (infoList.size() == 0) {
            ImageInfo info = new ImageInfo(null, null, null, null, "No images available", false, false, false);
            model.addElement(info);
        }
    }

    private String key;

    private String search;

    private DefaultListModel model;

    private int page;

    private int pages;

    private int perPage;

    private int total;

    private final String SEARCH_URL = "http://api.flickr.com/services/rest/?" + "method=flickr.photos.search";

    private final String IMAGE_URL = "http://static.flickr.com";

    private final int MAX_IMAGES = 100;

    class SearchHandler extends DefaultHandler {

        List<ImageInfo> imageList;

        public SearchHandler(List<ImageInfo> imageList) {
            this.imageList = imageList;
        }

        public void startDocument() throws SAXException {
        }

        public void endDocument() throws SAXException {
        }

        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            if (qName.equals("rsp")) {
                doRsp(attributes);
            } else if (qName.equals("photos")) {
                doPhotos(attributes);
            } else if (qName.equals("photo")) {
                doOnePhoto(attributes);
            }
        }

        private void doRsp(Attributes attributes) throws SAXException {
            String status = attributes.getValue("stat");
            if (!status.equalsIgnoreCase("ok")) {
                throw new SAXException("Response Error");
            }
        }

        private void doPhotos(Attributes attributes) {
            String strPage = attributes.getValue("page");
            String strPages = attributes.getValue("pages");
            String strPerPage = attributes.getValue("perpage");
            String strTotal = attributes.getValue("total");
            page = Integer.parseInt(strPage);
            pages = Integer.parseInt(strPages);
            perPage = Integer.parseInt(strPerPage);
            total = Integer.parseInt(strTotal);
        }

        private void doOnePhoto(Attributes attributes) {
            String id = attributes.getValue("id");
            String owner = attributes.getValue("owner");
            String secret = attributes.getValue("secret");
            String server = attributes.getValue("server");
            String farm = attributes.getValue("farm");
            String title = attributes.getValue("title");
            boolean bPublic = attributes.getValue("ispublic").equals("1");
            boolean bFriend = attributes.getValue("isfriend").equals("1");
            boolean bFamily = attributes.getValue("isfamily").equals("1");
            ImageInfo info = new ImageInfo(id, owner, secret, server, title, bPublic, bFriend, bFamily);
            imageList.add(info);
        }

        public void endElement(String uri, String localName, String qName) throws SAXException {
        }
    }
}
