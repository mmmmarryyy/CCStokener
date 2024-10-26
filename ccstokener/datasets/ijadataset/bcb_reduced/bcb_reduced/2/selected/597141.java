package ru.spb.leonidv.opensearch;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.AbstractWriter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import net.sf.Base64;
import nl.ikarus.nxt.priv.imageio.icoreader.lib.ICOReader;
import nl.ikarus.nxt.priv.imageio.icoreader.lib.ICOReaderSpi;
import org.jvnet.substance.skin.SubstanceBusinessLookAndFeel;
import ru.spb.leonidv.lvcontrols.SwingUtils;
import ru.spb.leonidv.lvcontrols.checklistpanel.ChecklistPanel;
import ru.spb.leonidv.lvcontrols.fileedit.FileEdit;
import ru.spb.leonidv.lvcontrols.fileedit.FileSelectedEvent;
import ru.spb.leonidv.lvcontrols.fileedit.FileSelectedListener;
import ru.spb.leonidv.opensearch.xml.ImageInfo;
import ru.spb.leonidv.opensearch.xml.ObjectFactory;
import ru.spb.leonidv.opensearch.xml.SearchPlugin;
import ru.spb.leonidv.opensearch.xml.UrlInfo;

/**
 * 
 * @author leonidv
 */
public final class MainFrame extends javax.swing.JFrame {

    private static final long serialVersionUID = -2372032328078027306L;

    private static final int BASE64_LINE_LENGTH = 76;

    private static final Color IMAGE_LABEL_COLOR = new Color(255, 153, 0);

    /**
	 * @param args
	 *            the command line arguments
	 */
    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(new SubstanceBusinessLookAndFeel());
        } catch (UnsupportedLookAndFeelException ex) {
            ex.printStackTrace();
        }
        ICOReaderSpi.registerIcoReader();
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }

    private javax.swing.JButton buttonLoad;

    private javax.swing.JButton buttonSave;

    private javax.swing.JButton buttonSearchTerm;

    private javax.swing.JComboBox comboEncoding;

    private javax.swing.JComboBox comboURLType;

    private FileEdit fileEditImage;

    private BufferedImage image;

    private JLabel labelFirefoxWarning;

    private JLabel labelImage;

    private JLabel labelImageBase64;

    private javax.swing.JPanel panelActions;

    private ChecklistPanel panelAuthor;

    private ChecklistPanel panelFirefox;

    private Box panelImage;

    private ChecklistPanel panelMainInfo;

    private ChecklistPanel panelURL;

    private JTextArea textareaImageBase64;

    private javax.swing.JTextField textAttribution;

    private javax.swing.JTextField textContact;

    private javax.swing.JTextField textDescription;

    private javax.swing.JTextField textDeveloper;

    private javax.swing.JTextField textIconUpdateUrl;

    private javax.swing.JTextField textSearchForm;

    private javax.swing.JTextField textShortName;

    private javax.swing.JTextField textUpdateInterval;

    private javax.swing.JTextField textUpdateURL;

    private javax.swing.JTextField textURLTemplate;

    private ObjectFactory factory;

    private Preferences prefs = new Preferences();

    private JAXBContext context;

    private class WindowSaver extends WindowAdapter {

        @Override
        public void windowClosing(WindowEvent e) {
            prefs.saveFrame(MainFrame.this);
            savePluginPreferences();
        }
    }

    /**
	 * Creates new form MainFrame
	 */
    public MainFrame() {
        this.setTitle("OpenSearch editor");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        factory = new ObjectFactory();
        try {
            context = JAXBContext.newInstance("ru.spb.leonidv.opensearch.xml");
        } catch (JAXBException ex) {
            ex.printStackTrace();
        }
        initComponents();
        prefs.loadFrame(this);
        loadPluginPreferences();
        addWindowListener(new WindowSaver());
    }

    /**
	 * Загружает информацию о плагине, установленную по умолчанию
	 */
    private void loadPluginPreferences() {
        textDeveloper.setText(prefs.getDeveloper());
        textContact.setText(prefs.getContact());
        textUpdateInterval.setText(prefs.getUpdateInterval());
        textUpdateURL.setText(prefs.getUpdateInterval());
        comboEncoding.getModel().setSelectedItem(prefs.getEncoding());
        comboURLType.getModel().setSelectedItem(prefs.getUrlType());
        fileEditImage.getFileChooser().setFileFilter(new IconFileFilter());
        JTextField textField = fileEditImage.getTextField();
        String text = Messages.getString("MainFrame.IconURL.text");
        final int cursorPosition = text.indexOf("|");
        text = text.replace("|", "");
        textField.setText(text);
        textField.getCaret().setDot(cursorPosition);
        fileEditImage.setButtonText(Messages.getString("MainFrame.IconURL.button"));
    }

    /**
	 * Сохраняет настройки общей информации о плагине
	 */
    private void savePluginPreferences() {
        prefs.setDeveloper(textDeveloper.getText());
        prefs.setContact(textContact.getText());
        prefs.setUpdateInterval(textUpdateInterval.getText());
        prefs.setUrlType(comboURLType.getSelectedItem().toString());
        prefs.setEncoding(comboEncoding.getSelectedItem().toString());
        File file = fileEditImage.getFile();
        if (file.exists() && (file.getParent() != null)) {
            file = new File(file.getParent());
            prefs.setImageDir(file.toString());
        }
    }

    private void buttonSaveActionPerformed(java.awt.event.ActionEvent evt) {
        savePluginAction();
    }

    private void buttonSearchTermActionPerformed(java.awt.event.ActionEvent evt) {
        StringBuilder result = new StringBuilder(textURLTemplate.getText());
        int selectionStart, selectionEnd;
        selectionStart = textURLTemplate.getSelectionStart();
        selectionEnd = textURLTemplate.getSelectionEnd();
        result.delete(selectionStart, selectionEnd);
        result.insert(textURLTemplate.getSelectionStart(), "{searchTerms}");
        textURLTemplate.setText(result.toString());
    }

    private void buttontLoadActionPerformed(java.awt.event.ActionEvent evt) {
        JFileChooser chooser = new JFileChooser(getLastPluginDir());
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File pluginFile = chooser.getSelectedFile();
        setLastPluginDir(pluginFile.getParentFile());
        Unmarshaller unmarshaller;
        try {
            unmarshaller = context.createUnmarshaller();
            SearchPlugin plugin = (SearchPlugin) unmarshaller.unmarshal(pluginFile);
            loadPlugin(plugin);
        } catch (JAXBException e) {
            System.out.println(Messages.getString("MainFrame.ErrorUnmarshallerCreator"));
            e.printStackTrace();
        }
    }

    private void loadPlugin(SearchPlugin plugin) {
        textShortName.setText(plugin.getShortName());
        textDescription.setText(plugin.getDescription());
        comboEncoding.setSelectedItem(plugin.getInputEncoding());
        UrlInfo urlInfo = plugin.getURL();
        textURLTemplate.setText(urlInfo.getTemplate());
        comboURLType.setSelectedItem(urlInfo.getType());
        try {
            setImage(plugin.getImage().getValue().replace(ImageInfo.IMAGE_ICON_PREFIX, ""));
        } catch (NullPointerException ex) {
            textareaImageBase64.setText(ex.getLocalizedMessage());
        }
        textDeveloper.setText(plugin.getDeveloper());
        textAttribution.setText(plugin.getAttribution());
        textContact.setText(plugin.getContact());
        textUpdateInterval.setText(String.valueOf(plugin.getUpdateInterval()));
        textUpdateURL.setText(plugin.getUpdateUrl());
        textIconUpdateUrl.setText(plugin.getIconUpdateUrl());
        textSearchForm.setText(plugin.getSearchForm());
    }

    /**
	 * Создает панель с кнопками загрузки и сохранения поискового плагина
	 * 
	 * @return - панель действий
	 */
    private JPanel createPanelActions() {
        if (panelActions == null) {
            panelActions = new JPanel();
            panelActions.setLayout(new GridLayout(1, 2));
            buttonLoad = new JButton(Messages.getString("MainFrame.LoadButton"));
            buttonLoad.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    buttontLoadActionPerformed(evt);
                }
            });
            buttonSave = new JButton(Messages.getString("MainFrame.SaveButton"));
            buttonSave.addActionListener(new java.awt.event.ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    buttonSaveActionPerformed(evt);
                }
            });
            panelActions.add(buttonLoad);
            panelActions.add(buttonSave);
        }
        return panelActions;
    }

    /**
	 * Создает панель с информацией об авторе поиского плагина.
	 * 
	 * @return - панель с информацией об авторе поиского плагина.
	 */
    private JPanel createPanelAuthor() {
        if (panelAuthor == null) {
            textDeveloper = new JTextField();
            textAttribution = new JTextField();
            textContact = new JTextField();
            panelAuthor = new ChecklistPanel();
            panelAuthor.addRow(Messages.getString("MainFrame.Developer"), textDeveloper);
            panelAuthor.addRow(Messages.getString("MainFrame.Attribution"), textAttribution);
            panelAuthor.addRow(Messages.getString("MainFrame.Contact"), textContact);
        }
        return panelAuthor;
    }

    /**
	 * Создает панель с настройками обновления поиского плагина. Эти настройки
	 * специфичны для браузера Firefox.
	 * 
	 * @return - панель настроек обновления плагина
	 */
    private JPanel createPanelFirefox() {
        if (panelFirefox == null) {
            panelFirefox = new ChecklistPanel();
            textUpdateURL = new JTextField();
            textUpdateInterval = new JTextField("7");
            textSearchForm = new JTextField();
            textIconUpdateUrl = new JTextField();
            labelFirefoxWarning = createHintLabel(Messages.getString("MainFrame.FirefoxWarning"));
            panelFirefox.addRow(Messages.getString("MainFrame.UpdateURL"), textUpdateURL);
            panelFirefox.addRow(Messages.getString("MainFrame.UpdateInterval"), textUpdateInterval);
            panelFirefox.addRow(Messages.getString("MainFrame.SearchForm"), textSearchForm);
            panelFirefox.addRow(Messages.getString("MainFrame.IconUpdateURL"), textIconUpdateUrl);
            panelFirefox.addTitle(labelFirefoxWarning);
        }
        return panelFirefox;
    }

    private JLabel createHintLabel(String text) {
        JLabel result = new JLabel(text);
        result.setFont(new Font("Dialog", 2, 12));
        return result;
    }

    /**
	 * Создает панель загрузки изображения.
	 * 
	 * @return - панель загрузки изображения
	 */
    private Box createPanelImage() {
        if (panelImage == null) {
            fileEditImage = new FileEdit();
            fileEditImage.addFileSelectedListener(new FileSelectedListener() {

                @Override
                public void fileSelected(FileSelectedEvent evt) {
                    fileEditImageFileSelected(evt);
                }
            });
            fileEditImage.setPreferredSize(new Dimension(100, 24));
            fileEditImage.setMaximumSize(new Dimension(2000, 24));
            labelImage = new JLabel() {

                private static final long serialVersionUID = -9020448597116313103L;

                @Override
                public void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    if (image != null) {
                        g.drawImage(image, 0, 0, labelImage.getWidth(), labelImage.getHeight(), null);
                    }
                }
            };
            labelImage.setBackground(new Color(204, 255, 204));
            labelImage.setPreferredSize(new Dimension(16, 16));
            labelImage.setMaximumSize(new Dimension(16, 16));
            labelImage.setMinimumSize(new Dimension(16, 16));
            JLabel labelHint = createHintLabel(Messages.getString("MainFrame.IconURL.hint"));
            labelImageBase64 = new JLabel(Messages.getString("MainFrame.Base64"));
            textareaImageBase64 = new JTextArea();
            textareaImageBase64.setColumns(10);
            textareaImageBase64.setRows(5);
            final JScrollPane scrollPane = new JScrollPane(textareaImageBase64);
            Font font = new Font("Monospaced", 0, 10);
            textareaImageBase64.setFont(font);
            textareaImageBase64.setEditable(false);
            panelImage = Box.createVerticalBox();
            Box boxImagePath = Box.createHorizontalBox();
            boxImagePath.add(labelImage);
            boxImagePath.add(Box.createHorizontalStrut(5));
            boxImagePath.add(fileEditImage);
            Box boxHint = Box.createHorizontalBox();
            boxHint.add(labelHint);
            boxHint.add(Box.createHorizontalGlue());
            Box panelText = Box.createHorizontalBox();
            panelText.add(labelImageBase64);
            panelText.add(Box.createGlue());
            panelImage.add(boxImagePath);
            panelImage.add(boxHint);
            panelImage.add(Box.createVerticalGlue());
            panelImage.add(panelText);
            panelImage.add(scrollPane);
        }
        return panelImage;
    }

    /**
	 * Создает панель с описанием плагина.
	 * 
	 * @return - панель описания плагина
	 */
    private ChecklistPanel createPanelMainInfo() {
        if (panelMainInfo == null) {
            panelMainInfo = new ChecklistPanel();
            panelMainInfo.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
            textShortName = new JTextField();
            textDescription = new JTextField();
            panelMainInfo.addRow(Messages.getString("MainFrame.ShortName"), textShortName);
            panelMainInfo.addRow(Messages.getString("MainFrame.Description"), textDescription);
        }
        return panelMainInfo;
    }

    /**
	 * Создает панель с описанием строки поиска и возвращаемого результата.
	 * 
	 * @return - панель описания строки поиска
	 */
    private JPanel createPanelURL() {
        if (panelURL == null) {
            panelURL = new ChecklistPanel();
            comboURLType = new JComboBox();
            comboURLType.setEditable(true);
            comboURLType.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "text/html", "application/rss+xml" }));
            panelURL.addRow(Messages.getString("MainFrame.Type"), comboURLType);
            textURLTemplate = new JTextField();
            textURLTemplate.setText("http://");
            buttonSearchTerm = new JButton("ST");
            buttonSearchTerm.setMaximumSize(new Dimension(15, 25));
            buttonSearchTerm.addActionListener(new ActionListener() {

                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    buttonSearchTermActionPerformed(evt);
                }
            });
            JPanel panelTemplateEditor = new JPanel(new BorderLayout());
            panelTemplateEditor.add(textURLTemplate, BorderLayout.CENTER);
            panelTemplateEditor.add(buttonSearchTerm, BorderLayout.EAST);
            panelURL.addRow(Messages.getString("MainFrame.Template"), panelTemplateEditor);
            comboEncoding = new JComboBox();
            comboEncoding.setEditable(true);
            comboEncoding.setModel(new DefaultComboBoxModel(new String[] { "windows-1251", "windows-1252", "UTF-8" }));
            panelURL.addRow(Messages.getString("MainFrame.Encoding"), comboEncoding);
        }
        return panelURL;
    }

    private void fileEditImageFileSelected(ru.spb.leonidv.lvcontrols.fileedit.FileSelectedEvent evt) {
        try {
            String fileName = evt.getSelectedFileName();
            byte[] data;
            if (fileName.startsWith("http://")) {
                data = loadIconFromURL(fileName);
                if (textIconUpdateUrl.getText().trim().isEmpty()) {
                    textIconUpdateUrl.setText(fileName);
                }
            } else {
                data = loadIconFromFile(fileName);
            }
            if (data.length > 0) {
                String result = Base64.encodeBytes(data, Base64.DONT_BREAK_LINES);
                setImage(result);
            }
        } catch (FileNotFoundException ex) {
            System.out.println(Messages.getString("MainFrame.ErrorFileNotFound"));
        } catch (MalformedURLException e) {
            System.out.println(Messages.getString("MainFrame.ErrorBadURLFormat"));
        } catch (IOException e) {
            System.out.println(Messages.getString("MainFrame.ConnectionError"));
        }
    }

    private byte[] loadIconFromFile(String fileName) throws FileNotFoundException, IOException {
        byte[] data;
        InputStream input = new BufferedInputStream(new FileInputStream(fileName));
        data = new byte[input.available()];
        input.read(data);
        input.close();
        return data;
    }

    private byte[] loadIconFromURL(String urlString) throws MalformedURLException, IOException {
        byte[] data;
        URL url = new URL(urlString);
        URLConnection connection = url.openConnection();
        InputStream input = new BufferedInputStream(connection.getInputStream());
        data = new byte[connection.getContentLength()];
        input.read(data);
        input.close();
        return data;
    }

    /**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
    private void initComponents() {
        getContentPane().add(createPanelMainInfo(), java.awt.BorderLayout.NORTH);
        getContentPane().add(createPanelActions(), java.awt.BorderLayout.SOUTH);
        JTabbedPane tabbedPaneOptions = new javax.swing.JTabbedPane();
        tabbedPaneOptions.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        tabbedPaneOptions.addTab(Messages.getString("MainFrame.URLTab"), createPanelURL());
        tabbedPaneOptions.addTab(Messages.getString("MainFrame.ImageTab"), createPanelImage());
        tabbedPaneOptions.addTab(Messages.getString("MainFrame.FirefoxTab"), createPanelFirefox());
        tabbedPaneOptions.addTab(Messages.getString("MainFrame.AuthorTab"), createPanelAuthor());
        getContentPane().add(tabbedPaneOptions, BorderLayout.CENTER);
        pack();
    }

    private void savePluginAction() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(getLastPluginDir());
        if (fileChooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        setLastPluginDir(fileChooser.getSelectedFile().getParentFile());
        OutputStream output;
        try {
            output = new FileOutputStream(fileChooser.getSelectedFile());
            final SearchPlugin plugin = savePlugin();
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.marshal(plugin, System.out);
            marshaller.marshal(plugin, output);
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (PropertyException ex) {
            ex.printStackTrace();
        } catch (JAXBException ex) {
            ex.printStackTrace();
        }
    }

    private ImageInfo saveImage() {
        ImageInfo imageInfo = factory.createImage();
        if (image != null) {
            imageInfo.setHeight(BigInteger.valueOf(image.getHeight()));
            imageInfo.setWidth(BigInteger.valueOf(image.getWidth()));
            imageInfo.setValue(ImageInfo.IMAGE_ICON_PREFIX + textareaImageBase64.getText().replaceAll("\n", ""));
        }
        return imageInfo;
    }

    private SearchPlugin savePlugin() {
        SearchPlugin plugin = factory.createSearchPlugin();
        plugin.setShortName(textShortName.getText());
        plugin.setDescription(textDescription.getText());
        plugin.setInputEncoding(comboEncoding.getSelectedItem().toString());
        plugin.setURL(saveURL());
        plugin.setImage(saveImage());
        plugin.setDeveloper(textDeveloper.getText());
        plugin.setAttribution(textAttribution.getText());
        plugin.setContact(textContact.getText());
        plugin.setUpdateInterval(Integer.parseInt(textUpdateInterval.getText()));
        plugin.setUpdateUrl(textUpdateURL.getText());
        plugin.setIconUpdateUrl(textIconUpdateUrl.getText());
        plugin.setSearchForm(textSearchForm.getText());
        return plugin;
    }

    private UrlInfo saveURL() {
        UrlInfo url = factory.createUrlInfo();
        url.setTemplate(textURLTemplate.getText());
        url.setType(comboURLType.getSelectedItem().toString());
        return url;
    }

    private void setImage(final String imageBase64) {
        InputStream input = new ByteArrayInputStream(Base64.decode(imageBase64));
        try {
            image = ImageIO.read(input);
            labelImage.repaint();
            StringBuilder formattedString = new StringBuilder();
            int i;
            for (i = 0; i < imageBase64.length() / BASE64_LINE_LENGTH; i++) {
                int start = i * BASE64_LINE_LENGTH;
                int end = (i + 1) * BASE64_LINE_LENGTH;
                formattedString.append(imageBase64.substring(start, end));
                formattedString.append('\n');
            }
            formattedString.append(imageBase64.substring(i * BASE64_LINE_LENGTH));
            textareaImageBase64.setText(formattedString.toString());
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
	 * Возвращает последнуюю директорию, из(в) которую последний раз сохранялся
	 * (считывался) поисковый плагин.
	 * 
	 * @return - директория хранения плагинов у пользователя
	 */
    private File getLastPluginDir() {
        return new File(prefs.getLastPluginDir());
    }

    /**
	 * Устанавливает последнуюю директорию, с которой работал пользователь.
	 * 
	 * @param dir -
	 *            директория хранения поисковых плагинов пользователя.
	 */
    private void setLastPluginDir(File dir) {
        prefs.setLastPluginDir(dir.toString());
    }
}
