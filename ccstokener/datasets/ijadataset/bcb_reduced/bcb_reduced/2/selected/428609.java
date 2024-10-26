package org.mindswap.markup.utils;

import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import java.awt.GridLayout;
import java.io.PrintWriter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.net.URLEncoder;
import java.net.URLConnection;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Vector;
import java.util.Iterator;
import java.util.Date;
import java.text.SimpleDateFormat;
import org.mindswap.markup.MarkupModel;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import org.mindswap.utils.*;

/**
 *
 * <p>Title: SubmitRDF - a utility class for submitting rdf data to the mindswap portal </p>
 * <p>Description: </p>
 * <p>Copyright: Copyright (c) 2003</p>
 * <p>Company: </p>
 * @author Michael Grove
 * @author Ron Alford
 * @version 1.0
 */
public class SubmitRDF {

    private static final boolean DEBUG = false;

    private String rdfpayload;

    private String name;

    private String description = "Program Data";

    private Date date = new Date();

    private Vector emails;

    private Vector categories;

    private JTextField mUser;

    private JTextField mDescription;

    private MarkupModel model;

    private SimpleDateFormat sdf;

    private static String lastUser = null;

    private class VisualAuthenticator extends Authenticator {

        protected PasswordAuthentication getPasswordAuthentication() {
            JTextField user = new JTextField();
            JTextField password = new JPasswordField();
            JPanel panel = new JPanel(new GridLayout(2, 2));
            panel.add(new JLabel("User"));
            panel.add(user);
            panel.add(new JLabel("Password"));
            panel.add(password);
            int option = JOptionPane.showConfirmDialog(null, new Object[] { "Host: " + getRequestingHost(), "Realm: " + getRequestingPrompt(), panel }, "Authorization Required", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (option == JOptionPane.OK_OPTION) {
                return new PasswordAuthentication(user.getText(), password.getText().toCharArray());
            } else return null;
        }
    }

    private class StaticAuthenticator extends Authenticator {

        private String user;

        private String pass;

        public StaticAuthenticator(String username, String password) {
            user = username;
            pass = password;
        }

        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, pass.toCharArray());
        }
    }

    public SubmitRDF(String rdf, MarkupModel theModel) {
        rdfpayload = rdf;
        Authenticator.setDefault(new VisualAuthenticator());
        name = null;
        categories = new Vector();
        emails = new Vector();
        sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd");
        model = theModel;
    }

    public SubmitRDF(String rdf, String username, String password) {
        rdfpayload = rdf;
        Authenticator.setDefault(new StaticAuthenticator(username, password));
        name = null;
        categories = new Vector();
        emails = new Vector();
        sdf = new SimpleDateFormat();
        sdf.applyPattern("yyyy-MM-dd");
    }

    public void addCreator(String email) {
        emails.add(email);
    }

    public void setCreators(java.util.List email) {
        emails = new Vector(email);
    }

    public void addCategory(String category) {
        categories.add(category);
    }

    public void setCategories(java.util.List category) {
        categories = new Vector(category);
    }

    public void setName(String fname) {
        name = fname;
    }

    public void setDescription(String desc) {
        description = desc;
    }

    public void setDate(Date d) {
        date = d;
    }

    public void setDate(String d) {
        try {
            date = sdf.parse(d);
        } catch (Exception e) {
        }
        if (date == null) {
            date = new Date();
        }
    }

    private void writeCreators(PrintWriter out) throws java.io.IOException {
        Iterator iter = emails.iterator();
        while (iter.hasNext()) {
            String encodedEmail = URLEncoder.encode((String) iter.next(), "UTF-8");
            out.write("&creator=" + encodedEmail);
        }
    }

    private void writeCategories(PrintWriter out) throws java.io.IOException {
        Iterator iter = categories.iterator();
        while (iter.hasNext()) {
            String encodedCategory = URLEncoder.encode((String) iter.next(), "UTF-8");
            out.write("&category=" + encodedCategory);
        }
    }

    private void writeName(PrintWriter out) {
        if (name != null) {
            out.write("&num=" + name);
        }
    }

    private void writeDescription(PrintWriter out) {
        if (description != null) {
            out.write("&desc=" + description);
        }
    }

    private void writeDate(PrintWriter out) {
        if (date == null) {
            date = new Date();
        }
        String datestring = sdf.format(date);
        out.write("&date=" + datestring);
    }

    private boolean getUserInfo() {
        mUser = new JTextField();
        if (lastUser != null) mUser.setText(lastUser);
        mDescription = new JTextField();
        JPanel panel = new JPanel(new GridLayout(2, 2));
        panel.add(new JLabel("Email"));
        panel.add(mUser);
        panel.add(new JLabel("Description of RDF submission"));
        panel.add(mDescription);
        int option = JOptionPane.showConfirmDialog(null, panel, "RDF Submit Information", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (option == JOptionPane.OK_OPTION) return true; else return false;
    }

    public boolean submit() throws java.io.IOException, Exception {
        return submit("http://owl.mindswap.org/manage/accept-rdf");
    }

    public boolean submit(String uri) throws java.io.IOException, Exception {
        if (getUserInfo()) {
            String encodedrdf = URLEncoder.encode(rdfpayload, "UTF-8");
            URL url = new URL(uri);
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            setDescription(mDescription.getText());
            addCreator(mUser.getText());
            lastUser = mUser.getText();
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.println("rdfblob=" + encodedrdf);
            writeCreators(out);
            writeCategories(out);
            writeName(out);
            writeDescription(out);
            writeDate(out);
            out.println("&inputtype=1");
            out.println("&op=Submit");
            out.close();
            return doSubmit(connection, rdfpayload);
        } else {
            JOptionPane.showMessageDialog(null, "Submit cannot be completed without user information, please try again.", "User Info Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    private boolean doSubmit(URLConnection theConn, String theRDF) {
        boolean success = false;
        try {
            Model aModel = ModelFactory.createDefaultModel();
            aModel.read(new java.io.StringReader(theRDF), model.getBaseURL());
            theConn.connect();
            InputStream is = theConn.getInputStream();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            String output = "";
            String line;
            while ((line = in.readLine()) != null) {
                output += line;
                if (DEBUG) System.err.println(line);
            }
            if (output.indexOf("Success") != -1) {
                theConn.connect();
                String loc = theConn.getHeaderField("Location");
                absolutizeInstances(aModel, loc, model);
                success = true;
            } else {
                success = false;
            }
            is.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            success = false;
        }
        return success;
    }

    private void absolutizeInstances(Model theModel, String location, MarkupModel m) {
        ResIterator rIter = theModel.listSubjectsWithProperty(RDF.type);
        while (rIter.hasNext()) {
            Resource res = rIter.nextResource();
            Resource theInst = m.getInstance(res.toString());
            if (theInst != null && theInst.getURI().startsWith(m.getBaseURL())) {
                m.getInstances().remove(theInst.getURI());
                String temp = theInst.getURI().replaceFirst(m.getBaseURL(), location + "#");
                theInst = com.hp.hpl.jena.util.ResourceUtils.renameResource(theInst, temp);
                m.getInstances().put(theInst.getURI(), theInst);
            }
        }
    }

    public static void usage() {
        System.out.println("Usage:");
        System.out.println("java SubmitRDF [-u username] [-p password] [-n name] [-D date] [-d description] {-e email} {-c category} filename");
        System.out.println("Examples:");
        System.out.println("$ java SubmitRDF results.rdf");
        System.out.println("$ java SubmitRDF -u foo -p bar -D 2003-09-07 -d \"Pellet results\" -n sightings -e john1@example.com -e john2@example.com -c http://owl.mindswap.org/2003/ont/owlweb.rdf#ProgramData results.rdf");
    }

    private static String readFile(String filename) {
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(filename));
        } catch (Exception e) {
            System.err.println("Could not open file: " + filename);
            System.exit(0);
        }
        StringBuffer rdfbuffer = new StringBuffer();
        try {
            String line = in.readLine();
            while (line != null) {
                rdfbuffer.append(line);
                line = in.readLine();
            }
        } catch (Exception e) {
            System.err.println("Could not read from file " + filename);
            System.exit(0);
        }
        return rdfbuffer.toString();
    }

    public static void main(String args[]) {
        String filename = null;
        String name = null;
        String username = null;
        String password = null;
        String date = null;
        String description = null;
        Vector emails = new Vector();
        Vector categories = new Vector();
        for (int argi = 0; argi < args.length; argi++) {
            String arg = args[argi];
            if (arg.startsWith("-")) {
                if (argi + 1 >= args.length) {
                    System.err.println("Arguments don't match!");
                    usage();
                    System.exit(0);
                } else if (arg.equals("-n")) {
                    argi++;
                    name = args[argi];
                } else if (arg.equals("-u")) {
                    argi++;
                    username = args[argi];
                } else if (arg.equals("-p")) {
                    argi++;
                    password = args[argi];
                } else if (arg.equals("-e")) {
                    argi++;
                    emails.add(args[argi]);
                } else if (arg.equals("-c")) {
                    argi++;
                    categories.add(args[argi]);
                } else if (arg.equals("-d")) {
                    argi++;
                    description = args[argi];
                } else if (arg.equals("-D")) {
                    argi++;
                    date = args[argi];
                } else {
                    System.err.println("Unkown argument '" + arg + "'");
                    usage();
                    System.exit(0);
                }
            } else {
                if (filename != null) {
                    System.err.println("Multiple File names!");
                    usage();
                    System.exit(0);
                }
                filename = args[argi];
            }
        }
        SubmitRDF srdf = null;
        if (filename == null) {
            System.err.println("No filename!");
            usage();
            System.exit(0);
        } else {
            if ((username == null) || (password == null)) {
            } else {
                srdf = new SubmitRDF(readFile(filename), username, password);
            }
        }
        srdf.setCreators(emails);
        srdf.setCategories(categories);
        if (name != null) {
            srdf.setName(name);
        }
        if (description != null) {
            srdf.setDescription(description);
        }
        if (date != null) {
            srdf.setDate(date);
        }
        try {
            srdf.submit();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Submission failed!");
        }
    }
}
