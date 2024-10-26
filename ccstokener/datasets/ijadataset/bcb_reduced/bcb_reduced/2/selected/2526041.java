package com.skruk.elvis.admin.manage.marc21.gui;

import com.skruk.elvis.admin.gui.ElvisListModel;
import com.skruk.elvis.admin.gui.WizardPanel;
import com.skruk.elvis.admin.i18n.ResourceFormatter;
import com.skruk.elvis.admin.plugin.Marc21Plugin;
import com.skruk.elvis.admin.registry.ElvisRegistry;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

/**
 * This code was generated using CloudGarden's Jigloo
 * SWT/Swing GUI Builder, which is free for non-commercial
 * use. If Jigloo is being used commercially (ie, by a
 * for-profit company or business) then you should purchase
 * a license - please visit www.cloudgarden.com for details.
 *
 * @author     skruk
 * @created    20 lipiec 2004
 */
public class SelectResourcePanel extends WizardPanel {

    /**  Description of the Field */
    private JList jlResourceList;

    /**  Description of the Field */
    private JTextField jtfResId;

    /**  Description of the Field */
    private JLabel jlResId;

    /**  Description of the Field */
    private JPanel jPanel1;

    /**  Description of the Field */
    private JLabel jlHead;

    /**  Description of the Field */
    private JScrollPane jScrollPane2;

    /**  Description of the Field */
    private JPanel jPanel10;

    /**  Description of the Field */
    private JPanel jPanel9;

    /**  Description of the Field */
    static ResourceFormatter formater = null;

    /**Constructor for the SelectResourcePanel object */
    public SelectResourcePanel() {
        synchronized (SelectResourcePanel.class) {
            if (formater == null) {
                formater = Marc21Plugin.getInstance().getFormater();
            }
        }
        initGUI();
        xInitGUI();
    }

    /**  Description of the Method */
    protected void xInitGUI() {
        this.jlHead.setText(formater.getText("select_marc21_title"));
        this.jlResId.setText(formater.getText("select_marc21_label_text"));
        this.jlResId.setToolTipText(formater.getText("select_marc21_label_description"));
        ElvisListModel model = new ElvisListModel();
        this.jlResourceList.setModel(model);
        try {
            URL urlListResources = new URL(ElvisRegistry.getInstance().getProperty("elvis.server") + "/servlet/listResources?xpath=document()//Book");
            InputStream streamResources = urlListResources.openStream();
            XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
            xpp.setInput(new InputStreamReader(streamResources));
            int type = xpp.getEventType();
            while (type != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG && "Resource".equals(xpp.getName())) {
                    model.add(xpp.getAttributeValue("", "resId"), xpp.getAttributeValue("", "author"), xpp.getAttributeValue("", "title"));
                }
                type = xpp.next();
            }
            streamResources.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (XmlPullParserException xppe) {
            xppe.printStackTrace();
        }
        ListSelectionModel selectionModel = this.jlResourceList.getSelectionModel();
        selectionModel.addListSelectionListener(new ListSelectionListener() {

            /**
				 * @param  e  Description of the Parameter
				 * @see       javax.swing.event.ListSelectionListener#valueChanged(javax.swing.event.ListSelectionEvent)
				 */
            public void valueChanged(ListSelectionEvent e) {
                int index = e.getFirstIndex();
                boolean isAdjusting = e.getValueIsAdjusting();
                if (!isAdjusting) {
                    ElvisListModel _model = (ElvisListModel) jlResourceList.getModel();
                    jtfResId.setText(_model.get(index).getId());
                }
            }
        });
    }

    /**
	 * Initializes the GUI.
	 * Auto-generated code - any changes you make will disappear.
	 */
    public void initGUI() {
        try {
            preInitGUI();
            jPanel9 = new JPanel();
            jPanel10 = new JPanel();
            jScrollPane2 = new JScrollPane();
            jlResourceList = new JList();
            jPanel1 = new JPanel();
            jlResId = new JLabel();
            jtfResId = new JTextField();
            jlHead = new JLabel();
            BorderLayout thisLayout = new BorderLayout();
            this.setLayout(thisLayout);
            thisLayout.setHgap(0);
            thisLayout.setVgap(0);
            this.setPreferredSize(new java.awt.Dimension(341, 236));
            BorderLayout jPanel9Layout = new BorderLayout();
            jPanel9.setLayout(jPanel9Layout);
            jPanel9Layout.setHgap(0);
            jPanel9Layout.setVgap(0);
            jPanel9.setPreferredSize(new java.awt.Dimension(60, 20));
            this.add(jPanel9, BorderLayout.CENTER);
            BorderLayout jPanel10Layout = new BorderLayout();
            jPanel10.setLayout(jPanel10Layout);
            jPanel10Layout.setHgap(0);
            jPanel10Layout.setVgap(0);
            jPanel10.setBorder(new EmptyBorder(new Insets(10, 10, 10, 10)));
            jPanel9.add(jPanel10, BorderLayout.CENTER);
            jPanel10.add(jScrollPane2, BorderLayout.CENTER);
            jlResourceList.setLayout(null);
            jlResourceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            jScrollPane2.add(jlResourceList);
            jScrollPane2.setViewportView(jlResourceList);
            BorderLayout jPanel1Layout = new BorderLayout();
            jPanel1.setLayout(jPanel1Layout);
            jPanel1Layout.setHgap(0);
            jPanel1Layout.setVgap(0);
            jPanel1.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
            jPanel10.add(jPanel1, BorderLayout.NORTH);
            jlResId.setLayout(null);
            jlResId.setText("id.xml");
            jlResId.setHorizontalAlignment(SwingConstants.TRAILING);
            jlResId.setPreferredSize(new java.awt.Dimension(103, 19));
            jlResId.setBorder(new EmptyBorder(new Insets(0, 0, 0, 5)));
            jPanel1.add(jlResId, BorderLayout.WEST);
            jPanel1.add(jtfResId, BorderLayout.CENTER);
            jlHead.setLayout(null);
            jlHead.setText("Head");
            jlHead.setHorizontalAlignment(SwingConstants.CENTER);
            jlHead.setHorizontalTextPosition(SwingConstants.CENTER);
            jlHead.setFont(new java.awt.Font("Dialog", 1, 18));
            jlHead.setPreferredSize(new java.awt.Dimension(584, 30));
            jlHead.setMinimumSize(new java.awt.Dimension(0, 30));
            jlHead.setMaximumSize(new java.awt.Dimension(1000, 30));
            this.add(jlHead, BorderLayout.NORTH);
            postInitGUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Add your pre-init code in here */
    public void preInitGUI() {
    }

    /** Add your post-init code in here */
    public void postInitGUI() {
    }

    /**
	 * Auto-generated main method
	 *
	 * @param  args  The command line arguments
	 */
    public static void main(String[] args) {
        showGUI();
    }

    /**
	 * This static method creates a new instance of this class and shows
	 * it inside a new JFrame, (unless it is already a JFrame).
	 *
	 * It is a convenience method for showing the GUI, but it can be
	 * copied and used as a basis for your own code.	*
	 * It is auto-generated code - the body of this method will be
	 * re-generated after any changes are made to the GUI.
	 * However, if you delete this method it will not be re-created.
	 */
    public static void showGUI() {
        try {
            javax.swing.JFrame frame = new javax.swing.JFrame();
            SelectResourcePanel inst = new SelectResourcePanel();
            frame.setContentPane(inst);
            frame.getContentPane().setSize(inst.getSize());
            frame.setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
            frame.pack();
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
	 *  Description of the Method
	 *
	 * @param  sb  Description of the Parameter
	 * @return     Description of the Return Value
	 */
    public boolean canFinish(StringBuffer sb) {
        return false;
    }

    /**
	 *  Description of the Method
	 *
	 * @param  sb  Description of the Parameter
	 * @return     Description of the Return Value
	 */
    public boolean canProceed(StringBuffer sb) {
        boolean result = this.jtfResId.getText().trim().length() > 0;
        if (result) {
            updateContext();
        } else {
            sb.append(formater.getText("select_marc21_proceed_error"));
        }
        return result;
    }

    /**
	 *  Description of the Method
	 *
	 * @param  sb  Description of the Parameter
	 * @return     Description of the Return Value
	 */
    public boolean canRetreat(StringBuffer sb) {
        return false;
    }

    /**  Description of the Method */
    public void notifyEnter() {
    }

    /**  Description of the Method */
    protected void updateContext() {
        this.context.addString("resource_id", this.jtfResId.getText());
    }
}
