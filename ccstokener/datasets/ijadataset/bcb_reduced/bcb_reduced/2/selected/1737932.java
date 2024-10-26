package com.ipolyglot.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import javax.servlet.ServletContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.ipolyglot.commons.StringUtil;
import com.ipolyglot.service.LessonsRelativeIdsWithImages;

public class ImagesUtil {

    protected static final Log log = LogFactory.getLog(ImagesUtil.class);

    public static String getCSListOfRelativeLessonIdsWithImages() throws Exception {
        return StringUtil.getCommaSeparatedString(getListOfRelativeLessonIdsWithImages());
    }

    public static List<String> getListOfRelativeLessonIdsWithImages() {
        return LessonsRelativeIdsWithImages.LESSON_RELATIVE_IDS_WITH_IMAGES;
    }

    /** Returns true if the given lesson contains the images */
    public static boolean isLessonWithImages(String lessonId) {
        if (lessonId.indexOf("-") != 0) {
            return false;
        }
        String lessonRelativeId = null;
        try {
            lessonRelativeId = lessonId.substring(7, 11);
        } catch (java.lang.StringIndexOutOfBoundsException sioobe) {
            return false;
        }
        List<String> listOfRelativeIdsWithImages = getListOfRelativeLessonIdsWithImages();
        if (listOfRelativeIdsWithImages == null || listOfRelativeIdsWithImages.size() == 0) {
            return false;
        }
        return listOfRelativeIdsWithImages.contains(lessonRelativeId);
    }

    /** Return an image ULR of the given word/translation */
    public static String getImageUrl(ServletContext servletContext, String wordTranslationId) {
        String imageRootFolder = servletContext.getInitParameter("imgRootFolder");
        String lessonId = wordTranslationId.substring(7, 11);
        String imageUrlPath = imageRootFolder + lessonId + "/";
        String ext = ".jpg";
        String wordId = wordTranslationId.substring(11, 14);
        String imageUrl = imageUrlPath + wordId + ext;
        return imageUrl;
    }

    public static boolean isFilePresent(String imageUrl) {
        BufferedReader in = null;
        try {
            URL url = new URL(imageUrl);
            URLConnection urlConn = url.openConnection();
            int responseCode = ((HttpURLConnection) urlConn).getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.println("file not found: " + imageUrl + " HTTP Error " + responseCode + "  " + ((HttpURLConnection) urlConn).getResponseMessage());
                return false;
            }
            urlConn.getContent();
            in = new BufferedReader(new InputStreamReader(url.openStream()));
            return true;
        } catch (FileNotFoundException fnfe) {
            return false;
        } catch (IOException fnfe) {
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ex) {
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
    }
}
