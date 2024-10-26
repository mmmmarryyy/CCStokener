package com.nullfish.lib.tablelayout;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.swing.JComponent;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * HTML��table�^�O�`���Ń��C�A�E�g���w��ł���I���V������ȃp�l���B
 * 
 * @author shunji
 */
public class HtmlTablePanel extends NodePanel {

    HtmlTableLayout layout;

    /**
	 * �f�o�b�O���[�h
	 */
    private static boolean debug = false;

    /**
	 * �R���X�g���N�^
	 * @param node	�z�u��������XML�m�[�h�itable�^�O�j
	 */
    public HtmlTablePanel(Element node) {
        this(null, node);
    }

    /**
	 * �R���X�g���N�^
	 * @param owner	���̃p�l�����܂ރ��C�A�E�g
	 * @param node	�z�u��������XML�m�[�h�itable�^�O�j
	 */
    HtmlTablePanel(HtmlTableLayout owner, Element node) {
        super(owner);
        init(node);
        layout = new HtmlTableLayout(owner);
        layout.init(node);
        layout.layout(this);
    }

    /**
	 * �R���X�g���N�^
	 * @param file�@�z�u��������XML�t�@�C��
	 */
    public HtmlTablePanel(File file) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException, FactoryConfigurationError {
        this(null, file);
    }

    /**
	 * �R���X�g���N�^
	 * @param owner	���̃p�l�����܂ރ��C�A�E�g
	 * @param file	�z�u��������XML�t�@�C��
	 */
    HtmlTablePanel(HtmlTableLayout owner, File file) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException, FactoryConfigurationError {
        this(owner, new FileInputStream(file));
    }

    /**
	 * �R���X�g���N�^
	 * @param urlStr	�z�u��������XML�t�@�C���́A�N���X�p�X���ł̃p�X
	 */
    public HtmlTablePanel(String urlStr) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException, FactoryConfigurationError {
        this(null, urlStr);
    }

    /**
	 * �R���X�g���N�^
	 * @param owner	���̃p�l�����܂ރ��C�A�E�g
	 * @param urlStr	�z�u��������XML�t�@�C���́A�N���X�p�X���ł̃p�X
	 */
    HtmlTablePanel(HtmlTableLayout owner, String urlStr) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException, FactoryConfigurationError {
        this(owner, HtmlTablePanel.class.getResource(urlStr).openStream());
    }

    /**
	 * �R���X�g���N�^
	 * @param is	�z�u��������XML�t�@�C���́A��̓X�g���[��
	 */
    public HtmlTablePanel(InputStream is) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException, FactoryConfigurationError {
        this(null, is);
    }

    /**
	 * �R���X�g���N�^
	 * @param owner	���̃p�l�����܂ރ��C�A�E�g
	 * @param is	�z�u��������XML�t�@�C���́A��̓X�g���[��
	 */
    HtmlTablePanel(HtmlTableLayout owner, InputStream is) throws FileNotFoundException, SAXException, IOException, ParserConfigurationException, FactoryConfigurationError {
        super(owner);
        initFromStream(is);
        layout = new HtmlTableLayout(owner);
        layout.init(node);
        layout.layout(this);
    }

    /**
	 * �ʒu���w�肵�āA�p�l���ɃR���|�[�l���g��ǉ�����B
	 * 
	 * @param comp �ǉ�����R���|�[�l���g
	 * @param position �ʒu����
	 */
    public void addComponent(Component comp, String position) {
        layout.add(comp, position);
    }

    /**
	 * �ʒu���w�肵�āA�p�l���ɃR���|�[�l���g��ǉ�����B
	 * 
	 * @param map �ʒu���̂ƒǉ�����R���|�[�l���g�̃}�b�v
	 */
    public void addComponents(Map map) {
        Iterator entrySetIte = map.entrySet().iterator();
        while (entrySetIte.hasNext()) {
            Entry entry = (Entry) entrySetIte.next();
            layout.add((Component) entry.getValue(), (String) entry.getKey());
        }
    }

    /**
	 * ���̂ɑΉ������p�l�����擾����B
	 * @param position	�ꏊ����
	 * @return
	 */
    public GridPanel getGridPanel(String position) {
        return layout.getGridPanel(position);
    }

    /**
	 * ���̂ɑΉ������^�u�p�l�����擾����B
	 * @param position	�ꏊ����
	 * @return
	 */
    public TabbedPanel getTabbedPanel(String position) {
        return layout.getTabbedPanel(position);
    }

    /**
	 * TD�^�O�̃e�L�X�g�Ɠ����̃N���X�����o���e�Z���ɔz�u����B
	 * 
	 * @param  �R���|�[�l���g�������o�ϐ��Ɏ����Ă���I�u�W�F�N�g
	 * @throws NoSuchFieldException 
	 * @throws SecurityException 
	 * @throws IllegalAccessException 
	 * @throws IllegalArgumentException 
	 */
    public void layoutByMemberName(Object obj) throws SecurityException, NoSuchFieldException, IllegalArgumentException, IllegalAccessException {
        Class clazz = obj.getClass();
        List keys = getKeys();
        for (int i = 0; i < keys.size(); i++) {
            String key = (String) keys.get(i);
            Field field = clazz.getDeclaredField(key);
            field.setAccessible(true);
            Object o = field.get(obj);
            if (o != null) {
                addComponent((JComponent) o, key);
            }
        }
    }

    /**
	 * �f�o�b�O���[�h�ɐݒ肷��B
	 * �f�o�b�O���[�h�ɐݒ肳���ƁA����Ȍ�ɏ����ꂽHtmlTablePanel��
	 * �S�ẴO���b�h�ɘg��t���B
	 * 
	 * @param debug	true�Ȃ�f�o�b�O���[�h
	 */
    public static void setDebug(boolean debug) {
        HtmlTablePanel.debug = debug;
    }

    /**
	 * �f�o�b�O���[�h�Ȃ�true��Ԃ��B
	 * @return	�f�o�b�O���[�h�Ȃ�true
	 */
    public static boolean isDebug() {
        return debug;
    }

    /**
	 * �L�[�̃��X�g���擾����B 
	 * @return
	 */
    public List getKeys() {
        return layout.getNames();
    }
}
