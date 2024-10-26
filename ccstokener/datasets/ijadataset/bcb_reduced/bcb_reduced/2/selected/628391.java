package album;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PchomeAlbumDownload {

    String pchome = "http://photo.pchome.com.tw";

    String id = "emily_ling";

    Vector<AlbumNode> albumLinks;

    Pattern pattern;

    String saveLink = "";

    Matcher matcher;

    public PchomeAlbumDownload(String id, String savedLink) {
        this.id = id;
        this.saveLink = savedLink;
        System.out.println("�}�l��o" + id + "���ۥ����");
        getAlbumLink(id);
        System.out.println("------��o---�ۥ���ơA�i��U�@�B�J�A�i����ۥ��Ӥ�s��---");
        getAllPhotoPages();
        System.out.println("�}�l����ɮצ�m�G");
        getAllPhotoLinks();
        System.out.println("�}�l�N�ɮ׳s���g�J�ɮץH�Ѭd��");
        saveAllLinks();
        System.out.println("����");
    }

    public PchomeAlbumDownload() {
        System.out.println("�}�l��o�ۥ����");
        getAlbumLink(id);
        System.out.println("------��o---�ۥ���ơA�i��U�@�B�J�A�i����ۥ��Ӥ�s��---");
        getAllPhotoPages();
        System.out.println("�}�l����ɮצ�m�G");
        getAllPhotoLinks();
        System.out.println("�}�l�N�ɮ׳s���g�J�ɮץH�Ѭd��");
        saveAllLinks();
        System.out.println("����");
    }

    /**
	 * �̫᪺�B�J�G�|�N�w�g��U�Ӫ������s������
	 * �x�s��{id}.txt�̭�
	 */
    private void saveAllLinks() {
        PhotoNode pn;
        String temp;
        if (saveLink == null || saveLink.equals("")) saveLink = "E:/";
        try {
            File file = new File(saveLink + id + ".txt");
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            for (AlbumNode an : albumLinks) {
                temp = "  " + an.coverName + "\r\n";
                bw.write(temp);
                for (int i = 0; i < an.photos.size(); i++) {
                    pn = an.photos.get(i);
                    bw.write(pn.photoLink + "\r\n");
                }
                bw.write("\r\n\r\n");
            }
            bw.close();
        } catch (IOException e) {
            System.out.println("error,somethingwrong in saveLinks()");
            e.printStackTrace();
        }
    }

    ;

    /**
     * �|�i�J�C�@�i�Ӥ��m�A�åB�N�Ӥ�s����X�ӡA�i�H�N�Ӥ�s��
     * ��ɦ]��pchome���÷Ӥ�B�J�G�ҥH�Ӥ�s���L�k�����ϥΡA�ٻݭn�A�g�L���ɤ~�i�H
     * 
     */
    public void getAllPhotoLinks() {
        String content;
        for (AlbumNode an : albumLinks) {
            System.out.println("  �}�l�i��M��" + an.coverName + "���Ҧ��Ӥ��m");
            for (String s : an.photoPages) {
                content = getContent(s);
                String reg = "><a href.{1,}?\\.jpg";
                pattern = Pattern.compile(reg);
                matcher = pattern.matcher(content);
                String url;
                while (matcher.find()) {
                    url = matcher.group();
                    String photoLink = pchome + url.substring(url.indexOf("img=") + 4);
                    if (photoLink.indexOf("<") != -1) continue;
                    String photoTitle;
                    int titleIndex = content.indexOf("<span class=\"");
                    if (titleIndex == -1) {
                        System.out.println("  " + photoLink);
                        an.addPhotoLink(photoLink);
                        continue;
                    } else {
                        System.out.println("  " + photoLink);
                        an.addPhotoLink(photoLink);
                    }
                }
            }
            ;
        }
    }

    public void getAllPhotoPages() {
        for (AlbumNode an : albumLinks) {
            String content = getContent(an.link);
            String reg = "<a href=\"/" + id + "/\\d{1,}\">";
            pattern = Pattern.compile(reg);
            matcher = pattern.matcher(content);
            getAllPhotoPagesData(an);
            for (int i = 2; ; i++) {
                if (content.indexOf("�U�@��") == -1) break; else {
                    content = an.link + "*" + i;
                    content = getContent(content);
                    pattern = Pattern.compile(reg);
                    matcher = pattern.matcher(content);
                    getAllPhotoPagesData(an);
                }
            }
        }
    }

    ;

    /**
     * �ݩ�getAllPhotoPages�A�u�O�]���L�ݭn�����A���F�{���M�n
     * �ҥH��L��X�ӦӤw�C
     * @param an
     */
    private void getAllPhotoPagesData(AlbumNode an) {
        String url;
        int i;
        while (matcher.find()) {
            url = matcher.group();
            i = url.indexOf("/");
            i = url.indexOf('/', i + 1);
            an.photoPages.add(pchome + "/" + id + url.substring(i, url.length() - 2) + "/");
        }
    }

    /**
     * ��X�ۥ��C��(�n�]�A�ƭ����{��)
     * @param id
     */
    public void getAlbumLink(String id) {
        albumLinks = new Vector<AlbumNode>();
        String link = pchome + "/" + id;
        String content = getContent(link);
        if (content == null || content.equals("")) {
            System.out.println("�L�k��o" + id + "���ۥ��C��");
            return;
        }
        String reg1 = "<a href=\"/" + id + "/\\d{1,}+/\"";
        String reg2 = " .{1,}?jpg";
        String reg3 = ".{1,}?class=\"dis\">";
        pattern = Pattern.compile(reg1 + reg2 + reg3);
        matcher = pattern.matcher(content);
        getAlbumLinks();
        for (int i = 2; ; i++) {
            if (content.indexOf(id + "*" + i) != -1) {
                link = pchome + "/" + id + "*" + i;
                System.out.println("�i�J��" + i + "���G" + link);
                String temp = getContent(link);
                matcher = pattern.matcher(temp);
                getAlbumLinks();
            } else break;
        }
    }

    /**
     * �ݩ�getAlbumLink��method�]���ȤF�{���X�ӹL����A�ӱN���W�ߥX��
     */
    private void getAlbumLinks() {
        String url = "";
        String temp;
        while (matcher.find()) {
            url = matcher.group();
            AlbumNode an = new AlbumNode();
            if (url.indexOf("�K�X�O�@") != -1) {
                System.out.println("need password");
                continue;
            }
            try {
                int i = url.indexOf('/');
                i = url.indexOf("/", i + 1);
                i = url.indexOf('/', i + 1);
                temp = "http://photo.pchome.com.tw" + url.substring(url.indexOf('/'), i) + "/";
                an.link = temp;
                temp = "http://photo.pchome.com.tw" + url.substring(url.indexOf("Img[") + 4, url.indexOf(".jpg") + 4);
                an.coverPhotoLink = temp;
                temp = url.substring(url.indexOf("</span></span><div class=\"name\">") + 32, url.indexOf("</div></a><div class=\"dis\">"));
                while ((temp.indexOf('<') != -1 && temp.indexOf('>') != -1)) {
                    temp = temp.substring(temp.indexOf('>') + 1);
                }
                if (temp.lastIndexOf("...") != -1) an.coverName = temp.substring(0, temp.indexOf("...")); else an.coverName = temp;
                System.out.println("��o�ۥ��G" + an.coverName + " �����");
                System.out.println("��ï�s��:" + an.link);
                albumLinks.add(an);
                System.out.println();
            } catch (StringIndexOutOfBoundsException e) {
                System.out.println("error,�L�k��o�G" + an.link + "�����");
            }
        }
    }

    /**
     * �ΨӨ�o��html��󪺤覡�G
     * @param httpUrl
     * @return
     */
    private String getContent(String httpUrl) {
        String htmlCode = "";
        try {
            InputStream in;
            URL url = new java.net.URL(httpUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("User-Agent", "Mozilla/4.0");
            connection.connect();
            in = connection.getInputStream();
            byte[] buffer = new byte[512];
            int length = -1;
            while ((length = in.read(buffer, 0, 512)) != -1) {
                htmlCode += new String(buffer, 0, length);
            }
        } catch (Exception e) {
        }
        if (htmlCode == null) {
            return null;
        }
        return htmlCode;
    }

    class AlbumNode {

        public String link;

        public String coverPhotoLink;

        public String coverName;

        public Vector<PhotoNode> photos;

        public Vector<String> photoPages;

        public AlbumNode() {
            photos = new Vector<PhotoNode>();
            photoPages = new Vector<String>();
        }

        ;

        public AlbumNode(String link, String coverPhotoLink, String coverName) {
            this.link = link;
            this.coverPhotoLink = coverPhotoLink;
            this.coverName = coverName;
            if (coverName.indexOf("...") != -1) {
                this.coverName = this.coverName.substring(0, coverName.length() - 3);
            }
            photos = new Vector<PhotoNode>();
            photoPages = new Vector<String>();
        }

        public void addPhotoLink(String photoLink) {
            PhotoNode pn = new PhotoNode(photoLink);
            photos.add(pn);
        }

        public void addPhotoLink(String photoLink, String photoName) {
            PhotoNode pn = new PhotoNode(photoLink, photoName);
            photos.add(pn);
        }
    }

    class PhotoNode {

        String photoLink;

        String photoName;

        public PhotoNode(String photoLink) {
            this.photoLink = photoLink;
            photoName = "";
        }

        ;

        public PhotoNode(String photoLink, String photoName) {
            this.photoLink = photoLink;
            this.photoName = photoName;
        }
    }

    public static void main(String args[]) {
        new PchomeAlbumDownload();
    }
}
