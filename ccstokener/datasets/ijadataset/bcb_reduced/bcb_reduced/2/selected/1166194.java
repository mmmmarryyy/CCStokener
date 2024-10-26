package org.xito.launcher.youtube;

import java.net.*;
import java.io.*;
import org.w3c.dom.*;
import org.w3c.tidy.*;
import org.xito.launcher.*;

/**
 *
 * @author DRICHAN
 */
public class YouTubeDesc extends BaseLaunchDesc {

    public static final String YOUTUBE_URL = "http://youtube.com/watch?v=";

    private String videoId;

    private boolean new_browser_flag;

    public YouTubeDesc() {
        super();
    }

    public String getVideoId() {
        return videoId;
    }

    public void setVideioId(String id) {
        videoId = id;
    }

    public void initializeWebInfo() throws MalformedURLException, IOException, DOMException {
        Tidy tidy = new Tidy();
        URL url = new URL(YOUTUBE_URL + videoId);
        InputStream in = url.openConnection().getInputStream();
        Document doc = tidy.parseDOM(in, null);
        Element e = doc.getDocumentElement();
        String title = null;
        if (e != null && e.hasChildNodes()) {
            NodeList children = e.getElementsByTagName("title");
            if (children != null) {
                for (int i = 0; i < children.getLength(); i++) {
                    try {
                        Element childE = (Element) children.item(i);
                        if (childE.getTagName().equals("title")) {
                            NodeList titleChildren = childE.getChildNodes();
                            for (int n = 0; n < titleChildren.getLength(); n++) {
                                if (titleChildren.item(n).getNodeType() == childE.TEXT_NODE) {
                                    title = titleChildren.item(n).getNodeValue();
                                }
                            }
                        }
                    } catch (Exception exp) {
                        exp.printStackTrace();
                    }
                }
            }
        }
        if (title == null || title.equals("")) {
            throw new DOMException(DOMException.NOT_FOUND_ERR, "no title found");
        } else {
            setTitle(title);
        }
    }
}
