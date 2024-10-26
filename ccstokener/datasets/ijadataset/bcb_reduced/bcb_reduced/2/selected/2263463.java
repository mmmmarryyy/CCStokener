package com.nullfish.lib.tablelayout;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * XML�m�[�h���珉�����p�l���̒��ێ���
 * 
 * @author shunji
 */
public abstract class NodePanel extends JPanel {

    /**
	 * ���̃p�l����ێ����郌�C�A�E�g
	 */
    HtmlTableLayout owner;

    /**
	 * �w�i�摜
	 */
    private Image bgImage;

    /**
	 * �w�i�摜�`��N���X
	 */
    private BgImagePainter imagePainter = BgImagePainter.DEFAULT;

    /**
	 * �����������XML�m�[�h
	 */
    protected Element node;

    /**
	 * �w�i�F����
	 */
    public static final String ATTR_BGCOLOR = "bgcolor";

    /**
	 * �w�i�C���[�W�w��
	 */
    public static final String ATTR_BG_IMAGE = "bgimage";

    /**
	 * �w�i�C���[�W�\���p�^�[���w��
	 */
    public static final String ATTR_BG_IMAGE_ALIGN = "bgimage_align";

    /**
	 * ��������
	 */
    public static final String ATTR_WIDTH = "width";

    /**
	 * �c������
	 */
    public static final String ATTR_HEIGHT = "height";

    /**
	 * ��������
	 */
    public static final String ATTR_PREF_WIDTH = "prefwidth";

    /**
	 * �c������
	 */
    public static final String ATTR_PREF_HEIGHT = "prefheight";

    /**
	 * �w�蕝
	 */
    private int preferredWidth = -1;

    /**
	 * �w�荂
	 */
    private int preferredHeight = -1;

    /**
	 * �R���X�g���N�^
	 * 
	 * @param owner	���̃p�l����ێ����郌�C�A�E�g
	 */
    public NodePanel(HtmlTableLayout owner) {
        super(new GridBagLayout());
        this.owner = owner;
    }

    /**
	 * ������s��
	 * 
	 * @param node	�����������XML�m�[�h
	 */
    protected void init(Element node) {
        this.node = node;
        setOpaque(false);
        Border border = HtmlTableBorderFactory.createBorder(node);
        if (border != null) {
            setBorder(border);
        } else {
            if (HtmlTablePanel.isDebug()) {
            }
        }
        Color backGroundColor = ColorUtility.stringToColor(node.getAttribute(ATTR_BGCOLOR));
        if (backGroundColor != null) {
            setOpaque(true);
            setBackground(backGroundColor);
        }
        String widthStr = node.getAttribute(ATTR_PREF_WIDTH);
        if (widthStr != null && widthStr.length() > 0) {
            preferredWidth = Integer.parseInt(widthStr);
        }
        widthStr = node.getAttribute(ATTR_WIDTH);
        if (widthStr != null && widthStr.length() > 0) {
            preferredWidth = Integer.parseInt(widthStr);
        }
        String heightStr = node.getAttribute(ATTR_PREF_HEIGHT);
        if (heightStr != null && heightStr.length() > 0) {
            preferredHeight = Integer.parseInt(heightStr);
        }
        heightStr = node.getAttribute(ATTR_HEIGHT);
        if (heightStr != null && heightStr.length() > 0) {
            preferredHeight = Integer.parseInt(heightStr);
        }
        String bgImageAlignStr = node.getAttribute(ATTR_BG_IMAGE_ALIGN);
        String bgImageStr = node.getAttribute(ATTR_BG_IMAGE);
        if (bgImageStr != null && bgImageStr.length() > 0) {
            setBgImage(createImage(bgImageStr), bgImageAlignStr);
        }
    }

    /**
	 * XML�t�@�C���̓�̓X�g���[�����珉����B
	 * 
	 * @param is	��̓X�g���[��
	 * @throws SAXException
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws FactoryConfigurationError
	 */
    protected void initFromStream(InputStream is) throws SAXException, IOException, ParserConfigurationException, FactoryConfigurationError {
        DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = docBuilder.parse(is);
        init((Element) doc.getDocumentElement());
    }

    /**
	 * JComponent#paintComponent(Graphics)�̃I�[�o�[���C�h�B
	 * �w�i�摜��`�悷��B
	 * 
	 * @param g Graphics
	 */
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bgImage != null) {
            imagePainter.paint(this, g, bgImage);
        }
    }

    /**
	 * �p�X����C���[�W�𐶐�����B<br>
	 * �܂��N���X�p�X���̃p�X�Ƃ��ĉ��߂��A���Ƀ��[�J���t�@�C���̃p�X�Ƃ��ĉ��߂���B
	 * 
	 * @param name	�p�X
	 * @return	�C���[�W
	 */
    protected Image createImage(String name) {
        try {
            URL url = getClass().getResource(name);
            return stream2Image(url.openStream());
        } catch (Exception e) {
        }
        try {
            File file = new File(name);
            if (file.exists()) {
                return stream2Image(new FileInputStream(file));
            }
        } catch (IOException e) {
        }
        return null;
    }

    /**
	 * ��̓X�g���[������C���[�W�𐶐�����B
	 * 
	 * @param is	��̓X�g���[��
	 * @return	�C���[�W
	 */
    private Image stream2Image(InputStream is) {
        ByteArrayOutputStream bos = null;
        BufferedInputStream bis = null;
        try {
            bos = new ByteArrayOutputStream();
            bis = new BufferedInputStream(is);
            byte[] buffer = new byte[4096];
            while (bis.read(buffer) > -1) {
                bos.write(buffer);
            }
            return Toolkit.getDefaultToolkit().createImage(bos.toByteArray());
        } catch (Exception e) {
            return null;
        } finally {
            try {
                bos.close();
            } catch (Exception e) {
            }
            try {
                bis.close();
            } catch (Exception e) {
            }
        }
    }

    /**
	 * �ʒu���w�肵�ăR���|�[�l���g��ǉ�����B
	 * 
	 * @param comp	�R���|�[�l���g
	 * @param position	�ʒu����
	 */
    public abstract void addComponent(Component comp, String position);

    /**
	 * �w�i�摜���Z�b�g����B
	 * 
	 * @param bgImage	�w�i�摜
	 * @param alignName	�z�u����
	 */
    public void setBgImage(Image bgImage, String alignName) {
        if (this.bgImage != null) {
            this.bgImage.flush();
        }
        imagePainter = BgImagePainter.getInstance(alignName);
        this.bgImage = bgImage;
        repaint();
    }

    /**
	 * Component#getPreferredSize()�̃I�[�o�[���C�h�B
	 * �T�C�Y���w�肳��Ă�ꍇ�ɂ��̃T�C�Y��Ԃ��B
	 */
    public Dimension getPreferredSize() {
        Dimension rtn = super.getPreferredSize();
        if (preferredHeight != -1) {
            rtn.height = preferredHeight;
        }
        if (preferredWidth != -1) {
            rtn.width = preferredWidth;
        }
        return rtn;
    }

    /**
	 * Object#finalize()�̃I�[�o�[���C�h�B
	 * �w�i�摜��p���B
	 */
    public void finalize() throws Throwable {
        if (bgImage != null) {
            bgImage.flush();
        }
        super.finalize();
    }

    protected HtmlTableLayout getOwner() {
        return owner;
    }
}
