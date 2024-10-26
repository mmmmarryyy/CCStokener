package iCua.Data;

import java.net.*;
import java.io.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class LastFM {

    public static void getCoverArt(String album, String artist, String downloadFilename) {
        System.out.println("http://ws.audioscrobbler.com/1.0/album/" + java.net.URLEncoder.encode(artist) + "/" + java.net.URLEncoder.encode(album) + "/info.xml");
        String art = null;
        download("http://ws.audioscrobbler.com/1.0/album/" + java.net.URLEncoder.encode(artist) + "/" + java.net.URLEncoder.encode(album) + "/info.xml", "//tmp//info.xml");
        try {
            File file = new File("//tmp//info.xml");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            NodeList nodeLst = doc.getElementsByTagName("coverart");
            for (int s = 0; s < nodeLst.getLength(); s++) {
                Node fstNode = nodeLst.item(s);
                if (fstNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element fstElmnt = (Element) fstNode;
                    NodeList fstNmElmntLst = fstElmnt.getElementsByTagName("large");
                    Element fstNmElmnt = (Element) fstNmElmntLst.item(0);
                    NodeList fstNm = fstNmElmnt.getChildNodes();
                    art = ((Node) fstNm.item(0)).getNodeValue();
                }
            }
            download(art, downloadFilename);
        } catch (Exception E) {
            System.out.println("no puedo descargar");
        }
    }

    public static void getArtistArt(String artist, String downloadFilename) {
        String art = null;
        download("http://ws.audioscrobbler.com/1.0/artist/" + java.net.URLEncoder.encode(artist) + "/similar.xml", "//tmp//similar.xml");
        try {
            File file = new File("//tmp//similar.xml");
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);
            doc.getDocumentElement().normalize();
            art = doc.getDocumentElement().getAttribute("picture").toString();
            download(art, downloadFilename);
        } catch (Exception E) {
            System.out.println("no puedo descargar");
        }
    }

    public static void download(String address, String localFileName) {
        OutputStream out = null;
        URLConnection conn = null;
        InputStream in = null;
        try {
            URL url = new URL(address);
            out = new BufferedOutputStream(new FileOutputStream(localFileName));
            conn = url.openConnection();
            in = conn.getInputStream();
            byte[] buffer = new byte[1024];
            int numRead;
            long numWritten = 0;
            while ((numRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, numRead);
                numWritten += numRead;
            }
            System.out.println(localFileName + "\t" + numWritten);
        } catch (Exception exception) {
            exception.printStackTrace();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            } catch (IOException ioe) {
            }
        }
    }
}
