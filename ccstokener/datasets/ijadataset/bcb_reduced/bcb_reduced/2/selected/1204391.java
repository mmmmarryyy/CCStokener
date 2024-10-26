package net.sf.fkk.gui.avatar;

import net.sf.fkk.gui.MediaManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Iterator;
import java.net.URL;
import java.net.MalformedURLException;
import java.awt.image.BufferedImage;

public class AvatarManager {

    private MediaManager mediaManager;

    public AvatarManager(MediaManager mema) {
        mediaManager = mema;
        AvatarListe = new LinkedList();
    }

    public Iterator getIterator(boolean ki) {
        return (new AvatarIterator(ki));
    }

    public BufferedImage getAvatar(String name, boolean win, boolean lose, boolean left) {
        Iterator it = AvatarListe.listIterator(0);
        while (it.hasNext()) {
            Avatar ava = (Avatar) it.next();
            if (ava.Name.equals(name)) {
                return (ava.getAvatar(win, lose, left));
            }
        }
        return (null);
    }

    private void ladeAvatarBild(Node avapicnode, Avatar newava, URL baseurl) {
        int type = 0;
        int ori;
        NamedNodeMap picattlist = avapicnode.getAttributes();
        if (picattlist == null) {
            throw new AvatarException("Fehler im Avatarpack : Avatarbild hat keine Attribute!");
        }
        Node pictype = picattlist.getNamedItem("type");
        Node picori = picattlist.getNamedItem("orientation");
        Node picpath = picattlist.getNamedItem("path");
        if ((pictype == null) || ((pictype.getNodeType() != pictype.ATTRIBUTE_NODE))) {
            throw new AvatarException("Fehler im Avatarpack: Avatarbild hat kein Typ");
        }
        if (pictype.getNodeValue().equals("base")) {
            type = 1;
        } else if (pictype.getNodeValue().equals("lose")) {
            type = 2;
        } else if (pictype.getNodeValue().equals("win")) {
            type = 3;
        }
        if (type == 0) {
            throw new AvatarException("Fehler im Avatarpack: Avatarbild has ungueltigen Typ!");
        }
        if ((picori == null) || (picori.getNodeType() != picori.ATTRIBUTE_NODE)) {
            throw new AvatarException("Fehler im Avatarpack : Avatarbild hat keine Orientierung");
        }
        if ((picpath == null) || (picpath.getNodeType() != picpath.ATTRIBUTE_NODE)) {
            throw new AvatarException("Fehler im Avatarpack : Avatarbild hat keinen Pfad");
        }
        ori = picori.getNodeValue().equals("left") ? 0 : picori.getNodeValue().equals("right") ? 1 : 2;
        switch(type) {
            case 1:
                if (ori != 2) {
                    newava.BaseAvatar[ori] = picpath.getNodeValue();
                } else {
                    newava.BaseAvatar[0] = picpath.getNodeValue();
                    newava.BaseAvatar[1] = picpath.getNodeValue();
                }
                break;
            case 2:
                if (ori != 2) {
                    newava.LoseAvatar[ori] = picpath.getNodeValue();
                } else {
                    newava.LoseAvatar[0] = picpath.getNodeValue();
                    newava.LoseAvatar[1] = picpath.getNodeValue();
                }
                break;
            case 3:
                if (ori != 2) {
                    newava.WinAvatar[ori] = picpath.getNodeValue();
                } else {
                    newava.WinAvatar[0] = picpath.getNodeValue();
                    newava.WinAvatar[1] = picpath.getNodeValue();
                }
                break;
        }
    }

    private void ladeAvatar(Node avatar, URL baseurl) {
        Avatar newava;
        boolean havepic = false;
        if (avatar.getNodeType() != avatar.ELEMENT_NODE) return;
        if (!avatar.getNodeName().equals("avatar")) return;
        NamedNodeMap avatarattlist = avatar.getAttributes();
        if (avatarattlist == null) {
            throw new AvatarException("Fehler im Avatarpack : Avatar hat keine Attribute!");
        }
        Node avatartype = avatarattlist.getNamedItem("type");
        Node avatarname = avatarattlist.getNamedItem("name");
        if ((avatartype == null) || (avatartype.getNodeType() != avatartype.ATTRIBUTE_NODE)) {
            throw new AvatarException("Fehler im Avatarpack : Avatar hat keinen Typ");
        }
        if ((avatarname == null) || (avatarname.getNodeType() != avatarname.ATTRIBUTE_NODE)) {
            throw new AvatarException("Fehler im Avatarpack : Avatar hat keinen Namen");
        }
        newava = new Avatar(mediaManager, baseurl);
        newava.Name = avatarname.getNodeValue();
        newava.KI = avatartype.getNodeValue().equals("ki");
        NodeList avapiclist = avatar.getChildNodes();
        for (int k = 0; k < avapiclist.getLength(); ++k) {
            if ((avapiclist.item(k) == null) || (avapiclist.item(k).getNodeType() != Node.ELEMENT_NODE)) continue;
            if (avapiclist.item(k).getNodeName().equals("picture")) {
                ladeAvatarBild(avapiclist.item(k), newava, baseurl);
                havepic = true;
            }
        }
        if (havepic == false) {
            throw new AvatarException("Fehler im Avatarpack : Avatar hat kein Bild");
        }
        AvatarListe.add(newava);
    }

    public void ladeAvatarJARFile(String filename) throws AvatarException {
        URL baseurl;
        try {
            URL resurl = this.getClass().getResource("/" + filename);
            if (resurl == null) {
                throw new AvatarException("Kann Avatarpack nicht finden!");
            }
            baseurl = new URL("jar:" + resurl.toExternalForm() + "!/");
        } catch (MalformedURLException ex) {
            throw new AvatarException("Ungueltige URL des Avatars!", ex);
        }
        ladeAvatarPack("avatarpack.xml", baseurl);
    }

    public void ladeAvatarSubdir(String filename) throws AvatarException {
        URL resurl = this.getClass().getResource("/" + filename);
        if (resurl == null) {
            throw new AvatarException("Kann Avatarpack nicht finden!");
        }
        ;
        try {
            ladeAvatarPack("avatarpack.xml", new URL(resurl.toExternalForm() + "/"));
        } catch (MalformedURLException ex) {
            throw new AvatarException("Ungueltige URL des Avatars!", ex);
        }
    }

    public void ladeAvatarPack(String filename, URL baseurl) throws AvatarException {
        DocumentBuilderFactory fac = DocumentBuilderFactory.newInstance();
        DocumentBuilder parser;
        Document doc;
        URL url, dtdurl;
        fac.setValidating(true);
        try {
            parser = fac.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new AvatarException("Kann Parser nicht erstellen!", ex);
        }
        if (baseurl == null) {
            url = this.getClass().getResource(filename);
        } else {
            try {
                url = new URL(baseurl.toExternalForm() + filename);
            } catch (MalformedURLException ex) {
                throw new AvatarException("Kann URL des Avatarfiles nicht erstellen!", ex);
            }
        }
        dtdurl = this.getClass().getResource("/fkkavatarpack.dtd");
        try {
            doc = parser.parse(url.openStream(), dtdurl.toExternalForm());
        } catch (SAXException ex) {
            throw new AvatarException("Fehler im Avatarpack : fehlerhaftes XML-Dokument!", ex);
        } catch (IOException ex) {
            throw new AvatarException("Kann Avatarpack " + url + " nicht laden!");
        }
        Element docele = doc.getDocumentElement();
        if (!docele.getNodeName().equals("avatarpack")) {
            throw new AvatarException("Fehler im Avatarpack : falsche Document Node!");
        }
        NodeList avalist = docele.getChildNodes();
        for (int i = 0; i < avalist.getLength(); ++i) {
            ladeAvatar(avalist.item(i), baseurl);
        }
    }

    public void ladeAvatare() {
        Iterator it = AvatarListe.listIterator(0);
        while (it.hasNext()) {
            Avatar ava = (Avatar) it.next();
            ava.ladeAvatare();
        }
    }

    private LinkedList AvatarListe;

    public class AvatarIterator implements Iterator {

        /**
	 * Ein neuer Spielfelditerator wird erstellt
	 */
        public AvatarIterator(boolean ki) {
            avatarIter = AvatarListe.listIterator(0);
            iterKI = ki;
            getNext();
        }

        public boolean hasNext() {
            if (nextAvatar != null) return (true);
            return (false);
        }

        public Object next() {
            Avatar av = nextAvatar;
            getNext();
            return (av.Name);
        }

        public void getNext() {
            while (avatarIter.hasNext()) {
                nextAvatar = (Avatar) avatarIter.next();
                if (iterKI == nextAvatar.KI) return;
            }
            nextAvatar = null;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        private Iterator avatarIter;

        boolean iterKI;

        Avatar nextAvatar;
    }
}
