package gov.lanl.TestObsData;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.text.*;
import gov.lanl.ObservationManager.*;
import gov.lanl.Utility.*;
import gov.lanl.ObsDataTools.*;
import gov.lanl.CoasViewers.*;
import gov.lanl.GUITools.GridBagHelper;
import gov.lanl.TMCryptography.*;
import iaik.security.provider.IAIK;
import java.security.*;
import org.omg.DsObservationAccess.*;
import org.omg.NamingAuthority.*;
import org.omg.TerminologyServices.*;
import org.omg.PersonIdService.*;
import org.omg.DsObservationValue.*;
import org.omg.CosNaming.*;

/**
 * Demonstrates the ObsDataTools
 * @author Jim George
 * @version $Revision: 2164 $ $Date: 2002-11-21 13:22:25 -0500 (Thu, 21 Nov 2002) $
 */
public class Client extends Frame implements ActionListener, ObsDataListenerInterface {

    /**
	 * flag for debugging output
	 */
    boolean debug = false;

    /**
	 * the config properties
	 */
    Properties the_props;

    /**
	 * The orb
	 */
    org.omg.CORBA.ORB the_orb = null;

    /**
	 * The name service
	 */
    static NamingContext nc = null;

    /**
	* The container for the name service
	*/
    gov.lanl.Utility.NameService ns = null;

    /**
	 * the gui widgets for the Frame
	 */
    Label coas_server_label = new Label("Coas Server Name:");

    TextField coas_server_name;

    Label patient_label = new Label("Patient ID:");

    TextField patient_name = new TextField("10006", 10);

    Label which_label = new Label("Which Item");

    TextField which_val = new TextField("0", 10);

    Label display_label = new Label("Display Type");

    Choice display_choice = new Choice();

    Button[] actions = new Button[] { new Button("Connect.."), new Button("Info"), new Button("Histories"), new Button("Summaries"), new Button("Treatments"), new Button("Reports"), new Button("ImageStudies"), new Button("Immunologies"), new Button("Contraindications"), new Button("Attachments"), new Button("Display Item"), new Button("Display as XML") };

    TextArea result = new TextArea("First click <Connect>,\n" + "then the <Info> and trait buttons will work!!\n\n" + "Select a trait, like <Histories> and it will search\n" + "for all histories for the PatientID\n\n" + "Once you have selected some traits, <Display Item>\n" + "will display the <Which Item> element of the selected\n" + "traits, using the selected <Display Type>", 20, 80);

    Font taFont = new Font("Courier", Font.PLAIN, 12);

    ObservationComponent obsComp = null;

    QueryAccess queryAcc = null;

    ObservationMgr obsMgr = null;

    ObsDataDisplayFactory displayFactory = new ObsDataDisplayFactory();

    AuthorityId authorityId = new AuthorityId(RegistrationAuthority.DNS, "telemed.lanl.gov");

    QualifiedPersonId who;

    String[] what;

    TimeSpan when = new TimeSpan("19711223T154113", "20001226T035112");

    ObservationDataIteratorHolder rest;

    ObservationDataStruct[] obsDataSeq = null;

    org.omg.CORBA.Any[] anyObsDataSeq = null;

    /**
	 * Testing interface for coas server and obsdatatools
	 */
    public Client(String[] args, Properties inProps) {
        super("Demo ObsDataTools & Coas Server");
        the_props = inProps;
        String pval = the_props.getProperty("Debug");
        if (pval != null) {
            if (pval.equals("true")) {
                debug = true;
            }
        }
        ;
        getOrb(args);
        ns = new gov.lanl.Utility.NameService(the_orb, the_props.getProperty("NameService"));
        pval = the_props.getProperty("serverName", "COASServer");
        coas_server_name = new TextField(pval, 15);
        setLayout(new GridBagLayout());
        int yrow = 0, xcol = 0;
        xcol = GridBagHelper.addComponentInRow(this, coas_server_label, xcol, yrow, 2, 2);
        xcol = GridBagHelper.addComponentInRow(this, coas_server_name, xcol, yrow, 2, 2);
        xcol = GridBagHelper.addComponentInRow(this, patient_label, xcol, yrow, 2, 2);
        xcol = GridBagHelper.addComponentInRow(this, patient_name, xcol, yrow, 2, 2);
        yrow++;
        String[] choices = displayFactory.getDisplayTypes();
        for (int i = 0; i < choices.length; i++) {
            display_choice.add(choices[i]);
        }
        xcol = 0;
        xcol = GridBagHelper.addComponentInRow(this, display_label, xcol, yrow, 2, 2);
        xcol = GridBagHelper.addComponentInRow(this, display_choice, xcol, yrow, 2, 2);
        xcol = GridBagHelper.addComponentInRow(this, which_label, xcol, yrow, 2, 2);
        xcol = GridBagHelper.addComponentInRow(this, which_val, xcol, yrow, 2, 2);
        yrow++;
        for (int i = 0; i < actions.length; i = i + 4) {
            xcol = 0;
            for (int j = 0; j < 4; j++) {
                xcol = GridBagHelper.addComponentInRow(this, actions[i + j], xcol, yrow, 2, 2);
                actions[i + j].addActionListener(this);
            }
            yrow++;
        }
        result.setFont(taFont);
        GridBagHelper.constrain(this, result, 0, yrow, 4, 4, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST, 1.0, 1.0, 20, 5, 5, 5);
        addWindowListener(new ClientCloser());
        setBackground(java.awt.Color.lightGray);
        pack();
        setSize(600, 400);
        show();
    }

    /**
	 * Get the orb
	 */
    void getOrb(String[] args) {
        if (the_orb == null) {
            try {
                the_orb = org.omg.CORBA.ORB.init(args, the_props);
                result.append("Found orb\n");
                System.out.print("Found orb\n");
            } catch (org.omg.CORBA.SystemException ecxp) {
                System.out.println("Cant find ORB");
                return;
            }
        }
    }

    /**
	 * Get the orb
	 */
    void getNameService() {
        org.omg.CORBA.Object obj = null;
        String iorURL = the_props.getProperty("NameService", "");
        if (iorURL != "") {
            try {
                URL url = new URL(iorURL);
                BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
                String ref = in.readLine();
                in.close();
                obj = the_orb.string_to_object(ref);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        } else {
            try {
                obj = the_orb.resolve_initial_references("NameService");
            } catch (org.omg.CORBA.ORBPackage.InvalidName ex) {
                System.out.println("Can't resolve `NameService'");
                System.exit(1);
            }
        }
        if (obj == null) {
            System.err.println("'NameService' is a nil object reference");
            System.exit(1);
        }
        try {
            nc = org.omg.CosNaming.NamingContextHelper.narrow(obj);
            if (nc == null) {
                System.err.println("'NameService' is not " + "a NamingContext object reference");
                System.exit(1);
            }
        } catch (Exception ex) {
            System.err.println(ex);
            System.exit(1);
        }
        result.append("Found NameService\n");
    }

    /**
	 * Handles the action events
	 * @param ae is the incoming action event
	 */
    public void actionPerformed(ActionEvent ae) {
        if (ae.getSource() instanceof Button) {
            Button theButton = (Button) ae.getSource();
            result.setText(theButton.getLabel() + " pressed\n");
            if (theButton == actions[0]) {
                doLoginToAuthenticator();
                doConnect();
                doComponent();
            } else if (theButton == actions[1]) {
                try {
                    doCodes();
                } catch (Exception e) {
                    System.out.println(e);
                    result.append("    " + e + "\n");
                }
                try {
                    doPolicies();
                } catch (Exception e) {
                    System.out.println(e);
                    result.append("    " + e + "\n");
                }
                try {
                    doVersion();
                } catch (Exception e) {
                    System.out.println(e);
                    result.append("    " + e + "\n");
                }
            } else if (theButton == actions[2]) {
                doQueryObs(XML.History);
            } else if (theButton == actions[3]) {
                doQueryObs(XML.Summary);
            } else if (theButton == actions[4]) {
                doQueryObs(XML.Treatment);
            } else if (theButton == actions[5]) {
                doQueryObs(XML.Report);
            } else if (theButton == actions[6]) {
                doQueryObs("DNS:telemed.lanl.gov/TraitCode/ImageStudy");
            } else if (theButton == actions[7]) {
                doQueryObs(XML.Immunology);
            } else if (theButton == actions[8]) {
                doQueryObs(XML.ContraIndication);
            } else if (theButton == actions[9]) {
                doQueryObs(XML.Attachment);
            } else if (theButton == actions[10]) {
                doDisplayOfObsData(null);
            } else if (theButton == actions[11]) {
                doDisplayOfObsData(ObsDataDisplayFactory.DISPLAY_TYPE_TREE);
            }
        }
    }

    /**
	 * Display the current obsdata element as a display type
	 */
    void doDisplayOfObsData(String inType) {
        String which, type;
        which = which_val.getText();
        int whichObs = Integer.parseInt(which);
        if (inType == null) {
            type = display_choice.getSelectedItem();
        } else {
            type = inType;
        }
        result.append(whichObs + " " + type + "\n");
        if (obsDataSeq == null) {
            result.append("obsDataSeq null\n");
            return;
        } else if (obsDataSeq.length <= whichObs) {
            result.append("obsDataSeq not enought elements\n");
            return;
        } else {
            ObsDataAccessInterface odai = new CoasDataAccess(obsDataSeq[whichObs]);
            odai.setObsIdCodeString(MXML.OBSIDCODE);
            ObsDataServiceInterface textDisplay = displayFactory.createDisplay(type, null, odai, obsComp, this);
            textDisplay.performService();
        }
    }

    /**
	 * Login to authenticator, may replace this with authwin later.
	 * Uses Sascha as default.
	 */
    void doLoginToAuthenticator() {
        String security = the_props.getProperty("Security", "off");
        if (security.equalsIgnoreCase("on")) {
            result.append("\nSetting security...\n");
            IAIK.addAsProvider(true);
            TMKeyCrypto clientCrypto = null;
            try {
                String telemedHome = the_props.getProperty("telemed.home");
                String privPath = the_props.getProperty("privKeyInfoPath", "dist/servers/keys/TMAuthen/users");
                if (telemedHome != null) {
                    privPath = telemedHome + "/" + privPath;
                }
                System.out.println(telemedHome + ":" + privPath);
                String privPwd = the_props.getProperty("privPwd");
                String authenServerName = the_props.getProperty("authenServerName", "rsna.org");
                String loginName = the_props.getProperty("loginName", "Koenig");
                String loginPwd = the_props.getProperty("loginPwd", "Sascha");
                result.append("Login in as " + loginName + " with private key " + privPath + "\n");
                clientCrypto = new IAIKRSACrypto(privPath + "/" + loginName + "/private", privPwd, null);
            } catch (Exception e) {
                result.append(e + "\n");
            }
        }
    }

    /**
	 * Locate the server and connect to it
	 */
    void doConnect() {
        try {
            String serverName = coas_server_name.getText();
            result.append("Binding to server " + serverName + "\n");
            org.omg.CORBA.Object corbaObj = ns.connect(serverName);
            obsComp = ObservationComponentHelper.narrow(corbaObj);
            if (obsComp == null) result.append("Can't resolve '" + serverName + "'\n"); else result.append("Resolved '" + serverName + "'\n");
        } catch (Exception e) {
            result.append("Can't bind to server " + coas_server_name.getText() + "\n" + e + "\n");
            return;
        }
        result.append("Bind succeeded\n");
    }

    /**
	 * List access component data and set queryAccess
	 */
    void doComponent() {
        queryAcc = null;
        result.append("Getting components\n");
        try {
            String test = obsComp.coas_version();
            String[] test2 = obsComp.get_supported_policies();
            String[] test3 = obsComp.get_supported_qualifiers("xxx");
            String test4 = obsComp.get_current_time();
            ObservationMgr obMgr = obsComp.get_observation_mgr();
            AccessComponentData accCompData = obsComp.get_components();
            result.append("QueryAccess ");
            if (accCompData.query_access == null) {
                result.append(" null\n");
            } else {
                result.append("ok QueryAccess set\n");
            }
            queryAcc = accCompData.query_access;
            result.append("BrowseAccess ");
            if (accCompData.browse_access == null) {
                result.append(" null\n");
            } else {
                result.append("ok\n");
            }
            result.append("AsynchAccess ");
            if (accCompData.asynch_access == null) {
                result.append(" null\n");
            } else {
                result.append("ok\n");
            }
            result.append("ConstraintLanguageAccess ");
            if (accCompData.constraint_access == null) {
                result.append(" null\n");
            } else {
                result.append("ok\n");
            }
            result.append("ObservationLoader ");
            if (accCompData.observation_loader == null) {
                result.append(" null\n");
            } else {
                result.append("ok\n");
            }
            result.append("ConsumerAccess ");
            if (accCompData.consumer_access == null) {
                result.append(" null\n");
            } else {
                result.append("ok\n");
            }
            result.append("SupplierAccess ");
            if (accCompData.supplier_access == null) {
                result.append(" null\n");
            } else {
                result.append("ok\n");
            }
            result.append("ObservationMgr ");
            obsMgr = obsComp.get_observation_mgr();
            if (obsMgr == null) {
                result.append(" null\n");
            } else {
                result.append("ok\n");
            }
        } catch (Exception e) {
            result.append("Error: can't get QueryAccess component!\n" + e);
        }
    }

    /**
	 * List the codes
	 */
    void doCodes() {
        result.append("Listing Codes\n");
        if (queryAcc == null) {
            result.append("QueryAccess null, click Components, exiting\n");
            return;
        }
        String[] codes = queryAcc.get_supported_codes(100, new QualifiedCodeIteratorHolder());
        result.append(codes.length + " returned\n");
        for (int i = 0; i < codes.length; i++) {
            result.append(codes[i] + "\n");
        }
    }

    /**
	 * List the policies
	 */
    void doPolicies() {
        result.append("Listing Policies\n");
        if (queryAcc == null) {
            result.append("QueryAccess null, click Components, exiting\n");
            return;
        }
        String[] policies = queryAcc.get_supported_policies();
        result.append(policies.length + " returned\n");
        for (int i = 0; i < policies.length; i++) {
            result.append(policies[i] + "\n");
        }
    }

    /**
	 * List the version
	 */
    void doVersion() {
        result.append("Listing Version\n");
        if (queryAcc == null) {
            result.append("QueryAccess null, click Components, exiting\n");
            return;
        }
        String version = queryAcc.coas_version();
        result.append(version + "\n");
    }

    /**
	 * Set the values for who, what, when, rest
	 */
    void setWhoWhatWhenRest(String inWhat) {
        what = new String[] { inWhat };
        who = new QualifiedPersonId(authorityId, patient_name.getText());
        rest = new ObservationDataIteratorHolder();
    }

    /**
	 * Query for the Observation data and display it
	 */
    void doQueryObs(String inObsName) {
        obsDataSeq = null;
        if (queryAcc == null) {
            result.append("QueryAccess null, click Components, exiting\n");
            return;
        }
        setWhoWhatWhenRest(inObsName);
        try {
            result.append("calling QueryAccess.get_observations_by_time(...) for\n" + "    " + who + "\n" + "    " + what[0] + "\n" + "    " + when + "\n");
            anyObsDataSeq = queryAcc.get_observations_by_time(who, what, when, 100, rest);
            obsDataSeq = new ObservationDataStruct[anyObsDataSeq.length];
            for (int i = 0; i < anyObsDataSeq.length; i++) {
                obsDataSeq[i] = ObservationDataStructHelper.extract(anyObsDataSeq[i]);
            }
        } catch (Exception e) {
            result.append("Error in get observations\n" + e);
            return;
        }
        if (obsDataSeq != null) {
            result.append(obsDataSeq.length + " " + inObsName + " returned\n");
        }
    }

    /**
	 * Main startup method for the server
	 * @param args[] used as input ConfigProperties
	 * like -file <fname> for config properties
	 */
    public static void main(String[] args) {
        String propFile = "./Client.cfg";
        PrintableConfigProperties my_props = new PrintableConfigProperties();
        my_props.setProperties(propFile, args);
        my_props.print(true);
        Client theClient = new Client(args, my_props);
    }

    /**
	 * The WindowAdapter closer class
	 */
    class ClientCloser extends WindowAdapter {

        /**
		 * Handle the window closing event
		 *
		 * @param e is the incoming event
		 *
		 */
        public void windowClosing(WindowEvent e) {
            System.exit(0);
        }
    }

    /**
	 * The method which is called with the observation data and the change code.
	 *
	 *
	 * @param inObsDataAccess is the ObsDataAccess object containing the
	 * ObservationData which has changed.
	 * @param inChange indicates what kind of change; is one of the included
	 * static ints.
	 */
    public void notifyChange(ObsDataAccessInterface inObsDataAccess, int inChange) {
        System.out.println("notifyChange with ObsDataAccess called = " + inChange);
    }

    /**
	 * The method which is called with the observation data and the change code.
	 *
	 *
	 * @param inObservationData is the ObservationData which has changed.
	 * @param inChange indicates what kind of change; is one of the included
	 * static ints.
	 */
    public void notifyChange(ObservationDataStruct inObservationData, int inChange) {
        System.out.println("notifyChange with ObservationData called = " + inChange);
    }
}
