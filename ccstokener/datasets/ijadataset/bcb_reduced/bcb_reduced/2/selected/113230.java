package com.skruk.elvis.admin.manage.descriptions.gui;

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.skruk.elvis.admin.gui.ElvisBookResource;
import com.skruk.elvis.admin.gui.ElvisListModel;
import com.skruk.elvis.admin.ontology.OntologyStorage;
import com.skruk.elvis.admin.registry.ElvisRegistry;
import java.awt.BorderLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.border.EmptyBorder;
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
public class BookInstancePanel extends PropertyPanel {

    /**  Description of the Field */
    private JLabel jlLabel;

    /**  Description of the Field */
    private JComboBox jcbInstances;

    /**Constructor for the BookInstancePanel object */
    public BookInstancePanel() {
        initGUI();
    }

    /**
	 * Auto-generated event handler method
	 *
	 * @param  evt  Description of the Parameter
	 */
    protected void jcbInstancesActionPerformed(ActionEvent evt) {
    }

    /**
	 * Initializes the GUI.
	 * Auto-generated code - any changes you make will disappear.
	 */
    public void initGUI() {
        try {
            preInitGUI();
            jcbInstances = new JComboBox();
            jlLabel = new JLabel();
            BorderLayout thisLayout = new BorderLayout();
            this.setLayout(thisLayout);
            thisLayout.setHgap(10);
            thisLayout.setVgap(10);
            this.setPreferredSize(new java.awt.Dimension(518, 33));
            this.setMinimumSize(new java.awt.Dimension(200, 33));
            this.setBorder(new EmptyBorder(new Insets(5, 5, 5, 5)));
            this.setMaximumSize(new java.awt.Dimension(32767, 33));
            jcbInstances.setMinimumSize(new java.awt.Dimension(150, 24));
            this.add(jcbInstances, BorderLayout.CENTER);
            jcbInstances.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent evt) {
                    jcbInstancesActionPerformed(evt);
                }
            });
            jlLabel.setPreferredSize(new java.awt.Dimension(100, 0));
            jlLabel.setMinimumSize(new java.awt.Dimension(100, 0));
            this.add(jlLabel, BorderLayout.WEST);
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
	 *  Description of the Method
	 *
	 * @param  uri  Description of the Parameter
	 */
    public void init(String uri) {
        ElvisListModel model = new ElvisListModel();
        this.jcbInstances.setModel(model);
        try {
            URL urlListResources = new URL(ElvisRegistry.getInstance().getProperty("elvis.server") + "/servlet/listResources?xpath=document()//Book");
            InputStream streamResources = urlListResources.openStream();
            XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();
            xpp.setInput(new InputStreamReader(streamResources));
            int type = xpp.getEventType();
            ElvisBookResource selectEr = null;
            while (type != XmlPullParser.END_DOCUMENT) {
                if (type == XmlPullParser.START_TAG && "Resource".equals(xpp.getName())) {
                    ElvisBookResource er = model.add(xpp.getAttributeValue("", "resId"), xpp.getAttributeValue("", "author"), xpp.getAttributeValue("", "title"));
                    if (uri != null && uri.equals(er.getUri())) {
                        selectEr = er;
                    }
                }
                type = xpp.next();
            }
            if (selectEr != null) {
                this.jcbInstances.setSelectedItem(selectEr);
                this.jcbInstances.setEnabled(false);
            }
            streamResources.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (XmlPullParserException xppe) {
            xppe.printStackTrace();
        }
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
            BookInstancePanel inst = new BookInstancePanel();
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
	 *  Sets the label attribute of the BookInstancePanel object
	 *
	 * @param  _label  The new label value
	 */
    public void setLabel(String _label) {
    }

    /**
	 *  Gets the uri attribute of the BookInstancePanel object
	 *
	 * @return    The uri value
	 */
    public String getUri() {
        String uri = null;
        if (this.jcbInstances.getSelectedItem() != null) {
            uri = ((ElvisBookResource) this.jcbInstances.getSelectedItem()).getUri();
        }
        return uri;
    }

    /**
	 *  Gets the value attribute of the BookInstancePanel object
	 *
	 * @return    The value value
	 */
    public RDFNode getValue() {
        return OntologyStorage.getInstance().getModel().getIndividual(this.getUri());
    }
}
