package user.losacorp.html;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import javax.swing.text.BadLocationException;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.parser.ParserDelegator;
import user.losacorp.xml.Image;
import user.losacorp.xml.Photo;
import user.losacorp.xml.Video;
import user.losacorp.xml.Thumbnail;

public class HtmlSpider {

    private static boolean isArchivo;

    private HtmlSpiderParser hsp = null;

    /**
	 * Modificar este constructor para que solo inicialice urlSite local, y
	 * crear un nuevo m�todo que devuelva un HtmlPage que se pueda manipular.
	 * 
	 * @param urlSite
	 */
    public HtmlSpider(String urlSite) {
        try {
            InputStream in = null;
            if (!isArchivo) {
                URL url = new URL(urlSite);
                in = url.openStream();
            } else {
                File file = new File(urlSite);
                in = new FileInputStream(file);
            }
            InputStreamReader isr = new InputStreamReader(in);
            hsp = new HtmlSpiderParser(new HtmlPage());
            ParserDelegator pd = new ParserDelegator();
            pd.parse(isr, hsp, true);
            isr.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
	 * @return the hsp
	 */
    public HtmlSpiderParser getHtmlSiperParser() {
        return hsp;
    }

    public HtmlPage getHtmlParsed() {
        return this.getHtmlSiperParser().getHtmlPage();
    }

    public class HtmlPage {

        private StringBuilder accumulator;

        private ArrayList<Photo> photoList;

        private ArrayList<Video> videoList;

        private ArrayList<Thumbnail> thumbnailList;

        public HtmlPage() {
            accumulator = new StringBuilder();
            photoList = new ArrayList<Photo>();
            videoList = new ArrayList<Video>();
            thumbnailList = new ArrayList<Thumbnail>();
        }

        /**
		 * @return the accumulator
		 */
        public StringBuilder getAccumulator() {
            return accumulator;
        }

        /**
		 * @return the imageList
		 */
        public ArrayList<Photo> getPhotoList() {
            return photoList;
        }

        /**
		 * @return the videoList
		 */
        public ArrayList<Video> getVideoList() {
            return videoList;
        }

        /**
		 * @return the thumbnailList
		 */
        public ArrayList<Thumbnail> getThumbnailList() {
            return thumbnailList;
        }

        public void addPhoto(Photo photo) {
            this.photoList.add(photo);
        }

        public void addVideo(Video video) {
            this.videoList.add(video);
        }

        public void addThumbnail(Thumbnail thumbnail) {
            this.thumbnailList.add(thumbnail);
        }

        public void setAccumulator(String accumulator) {
            this.accumulator.append(accumulator);
        }

        public String getPageText() {
            return this.getAccumulator().toString();
        }

        @Override
        public String toString() {
            return new String("Texto: " + getAccumulator().toString() + "\nLista Phto: " + getPhotoList().toString() + "\nLista Video: " + getVideoList().toString() + "\nLista Thumbnails: " + getThumbnailList().toString());
        }
    }

    /**
	 * 
	 * Clase interna utilizada para parser el html
	 * 
	 * @author Javier Lo
	 * 
	 */
    public class HtmlSpiderParser extends HTMLEditorKit.ParserCallback {

        private HtmlPage htmlPage;

        private StringBuilder accumulator;

        private boolean isClassEntry = false;

        private boolean isPhotosAlbum;

        private boolean isSinglePhoto;

        private boolean isObject;

        private final HTML.UnknownTag HTML_TAG_EMBED = new HTML.UnknownTag("embed");

        public HtmlSpiderParser(HtmlPage htmlPage) {
            this.htmlPage = htmlPage;
            this.accumulator = new StringBuilder();
        }

        @Override
        public void handleSimpleTag(Tag tag, MutableAttributeSet mas, int pos) {
            if (isClassEntry) {
                if (tag.equals(HTML.Tag.IMG)) {
                    String src = (mas.getAttribute(HTML.Attribute.SRC)).toString();
                    if (src != null) {
                        if ((isPhotosAlbum || isSinglePhoto)) {
                            htmlPage.addThumbnail(new Thumbnail(src));
                        } else {
                            htmlPage.addPhoto(new Photo(src));
                        }
                    }
                } else if ((tag.equals(HTML.Tag.PARAM)) && (mas.getAttribute(HTML.Attribute.NAME).equals("movie"))) {
                } else if (tag.equals(HTML_TAG_EMBED)) {
                    if ((mas.getAttribute(HTML.Attribute.SRC) != null)) {
                        htmlPage.addVideo(new Video(((mas.getAttribute(HTML.Attribute.SRC) != null) ? mas.getAttribute(HTML.Attribute.SRC).toString() : ""), ((mas.getAttribute(HTML.Attribute.TYPE) != null) ? mas.getAttribute(HTML.Attribute.TYPE).toString() : ""), ((mas.getAttribute(Video.ALLOW_SCRIPT_ACCESS) != null) ? mas.getAttribute(Video.ALLOW_SCRIPT_ACCESS).toString() : ""), (mas.getAttribute(Video.ALLOW_FULL_SCREEN) != null) ? mas.getAttribute(Video.ALLOW_FULL_SCREEN).toString() : "", (mas.getAttribute(HTML.Attribute.WIDTH) != null) ? Integer.parseInt(mas.getAttribute(HTML.Attribute.WIDTH).toString()) : 0, (mas.getAttribute(HTML.Attribute.HEIGHT) != null) ? Integer.parseInt(mas.getAttribute(HTML.Attribute.HEIGHT).toString()) : 0));
                    }
                }
            }
        }

        @Override
        public void handleStartTag(Tag tag, MutableAttributeSet mas, int pos) {
            if (tag.equals(HTML.Tag.P)) {
                accumulator.setLength(0);
                if ((mas.getAttribute(HTML.Attribute.CLASS) != null) && (mas.getAttribute(HTML.Attribute.CLASS).equals("post-photos"))) {
                    isPhotosAlbum = true;
                } else if ((mas.getAttribute(HTML.Attribute.ID) != null) && (mas.getAttribute(HTML.Attribute.ID).equals("photo"))) {
                    isSinglePhoto = true;
                } else {
                    isPhotosAlbum = isSinglePhoto = false;
                }
            } else if (tag.equals(HTML.Tag.DIV) && (mas.getAttribute(HTML.Attribute.CLASS) != null)) {
                if ((mas.getAttribute(HTML.Attribute.CLASS).equals("entry"))) {
                    isClassEntry = true;
                }
            } else if (tag.equals(HTML.Tag.OBJECT)) {
                isObject = true;
            }
        }

        @Override
        public void handleText(char[] data, int pos) {
            if (isClassEntry) {
                accumulator.append(data);
            }
        }

        @Override
        public void handleEndTag(Tag tag, int pos) {
            if ((isClassEntry) && (tag.equals(HTML.Tag.P)) && accumulator.length() != 0) {
                htmlPage.setAccumulator(accumulator.toString());
            } else if (isObject) {
                isObject = false;
            }
        }

        @Override
        public void handleComment(char[] data, int pos) {
            if ((new String(data)).equalsIgnoreCase(" /.entry ")) {
                isClassEntry = false;
            }
        }

        /**
		 * @return the htmlPage
		 */
        public HtmlPage getHtmlPage() {
            return htmlPage;
        }

        @Override
        public void flush() throws BadLocationException {
            super.flush();
            if (!htmlPage.getThumbnailList().isEmpty()) {
                for (Image thumbnail : htmlPage.getThumbnailList()) {
                    thumbnail.getSrc().replace("-60x-", "-450x-");
                }
            }
        }

        @Override
        public String toString() {
            return htmlPage.toString();
        }
    }

    public static void main(String[] args) {
        setIsArchivo(false);
        String urlSite = "http://www.techeblog.com/elephant/photo.phtml?post_key=156609&photo_key=29603";
        HtmlSpider hs = new HtmlSpider(urlSite);
        System.out.println(hs.getHtmlSiperParser().getHtmlPage().toString());
    }

    private static void setIsArchivo(boolean b) {
        isArchivo = b;
    }
}
