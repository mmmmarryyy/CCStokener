package com.inetmon.jn.nwd;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.SocketChannel;
import java.nio.charset.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

/**
 * This class provides registration process
 * 
 * @author NieMouse
 *
 */
public class RegProc {

    public static String jDtr;

    public static String MAC;

    public static String InstallName;

    public static String InstallCompany;

    public static String WinName;

    public static String WinCompany;

    public static String jAge;

    public static String jSal;

    public static String jSta;

    public static String jSex;

    public static String jCon;

    public static String jOcc;

    public static String jInt;

    public static String jVer;

    public static String jTyp;

    public static String Serial;

    private static URL url = null;

    private String link;

    /**
	 * Constructor
	 * @throws IOException 
	 *
	 */
    public RegProc() throws IOException {
        link = getFromFile("[SERVER]", "url", Activator.getAppPath() + "/server.ini");
        try {
            url = new URL("http://" + link + "/");
        } catch (Exception e) {
            System.out.println(e);
        }
        MAC = null;
        Serial = null;
        checkReg();
    }

    /**
	 * Start Registration
	 * @return
	 * @throws IOException 
	 */
    public static void StartReg() throws IOException {
        if (MAC.equals("not_found")) {
            try {
                MAC = getMAC();
                BufferedWriter bw;
                bw = new BufferedWriter(new FileWriter(Activator.getAppPath() + "/register.ini", true));
                bw.newLine();
                bw.append("[SPECIFICINFO]");
                bw.newLine();
                bw.append("MAC=" + MAC);
                bw.close();
            } catch (IOException e) {
            }
        }
        accessRegistry();
        InstallName = WinName;
        InstallCompany = WinCompany;
        jAge = getFromFile("[INFO]", "Age", Activator.getAppPath() + "/register.ini");
        jSal = getFromFile("[INFO]", "Salary", Activator.getAppPath() + "/register.ini");
        jSta = getFromFile("[INFO]", "Status", Activator.getAppPath() + "/register.ini");
        jSex = getFromFile("[INFO]", "Sex", Activator.getAppPath() + "/register.ini");
        jCon = getFromFile("[INFO]", "Country", Activator.getAppPath() + "/register.ini");
        jOcc = getFromFile("[INFO]", "Occupation", Activator.getAppPath() + "/register.ini");
        jInt = getFromFile("[INFO]", "Interest", Activator.getAppPath() + "/register.ini");
        jVer = getFromFile("[APPLICATION]", "jver", Activator.getAppPath() + "/settings.ini");
        if (downloadRegPage()) {
            if (IsServerRegSuccess()) {
                BufferedWriter bw;
                bw = new BufferedWriter(new FileWriter(Activator.getAppPath() + "/register.ini", true));
                bw.newLine();
                bw.append("serial=" + Serial);
                bw.close();
                MessageBox mBox = new MessageBox(new Shell(), SWT.ICON_INFORMATION | SWT.OK);
                mBox.setText("Registered");
                mBox.setMessage("Registration Successful");
                if (mBox.open() == SWT.YES) {
                    mBox.getParent().close();
                }
            } else {
                MessageBox mBox = new MessageBox(new Shell(), SWT.ICON_ERROR | SWT.OK);
                mBox.setText("Unregistered");
                mBox.setMessage("Registration Failed: Insufficient information");
                if (mBox.open() == SWT.YES) {
                    mBox.getParent().close();
                }
            }
        } else {
            MessageBox mBox = new MessageBox(new Shell(), SWT.ICON_ERROR | SWT.OK);
            mBox.setText("Unregistered");
            mBox.setMessage("Registration Failed: Server response error");
            if (mBox.open() == SWT.YES) {
                mBox.getParent().close();
            }
        }
    }

    /**
	 * Get MAC address
	 * @return mac address
	 * @throws IOException 
	 */
    public static String getMAC() throws IOException {
        if (getFromFile("[SPECIFICINFO]", "MAC", Activator.getAppPath() + "/register.ini").equals("not_found")) {
            String strReturn = "not_detected";
            String command = "ipconfig /all";
            Process pid = Runtime.getRuntime().exec(command);
            BufferedReader in = new BufferedReader(new InputStreamReader(pid.getInputStream()));
            while (true) {
                String line = in.readLine();
                if (line == null) break;
                Pattern p = Pattern.compile(".*Physical Address.*: (.*)");
                Matcher m = p.matcher(line);
                if (m.matches()) {
                    if (!m.group(1).equals("00-00-00-00-00-00")) {
                        strReturn = m.group(1);
                        break;
                    }
                }
            }
            in.close();
            return strReturn.replaceAll("-", "");
        } else {
            return getFromFile("[SPECIFICINFO]", "MAC", Activator.getAppPath() + "/register.ini");
        }
    }

    /**
	 * Check registration status
	 * @return
	 */
    public static void checkReg() {
        jDtr = getFromFile("[SERVER]", "csz", Activator.getAppPath() + "/server.ini");
        jTyp = getFromFile("[SERVER]", "mor", Activator.getAppPath() + "/server.ini");
        MAC = getFromFile("[SPECIFICINFO]", "MAC", Activator.getAppPath() + "/register.ini");
        Serial = getFromFile("[SPECIFICINFO]", "serial", Activator.getAppPath() + "/register.ini");
        if (MAC.equals("not_found") || Serial.equals("not_found") || !Serial.startsWith(jDtr)) {
            final Display d = PlatformUI.getWorkbench().getDisplay();
            MAC = "not_found";
            d.syncExec(new Runnable() {

                public void run() {
                    final Shell dlg = new Shell(SWT.APPLICATION_MODAL);
                    dlg.setText("Registration");
                    dlg.setLayout(new TableWrapLayout());
                    dlg.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    dlg.setSize(400, 450);
                    FormToolkit toolkit = new FormToolkit(dlg.getDisplay());
                    Section sec1 = toolkit.createSection(dlg, Section.TITLE_BAR);
                    sec1.setText("Online Registration");
                    Section sec = toolkit.createSection(dlg, Section.DESCRIPTION);
                    sec.setText("Please take a moment to register with us");
                    Canvas c1 = new Canvas(dlg, SWT.NONE);
                    c1.setLayout(new GridLayout(2, false));
                    c1.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    Label l1 = new Label(c1, SWT.NONE);
                    l1.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    l1.setText("Age: ");
                    final Combo age = new Combo(c1, SWT.BORDER | SWT.DROP_DOWN);
                    age.setItems(new String[] { "Below 20", "21-25", "26-30", "31-35", "36-40", "41-45", "46-50", "51-55", "56-60", "Above 60" });
                    age.setToolTipText("Select one from the list");
                    l1 = new Label(c1, SWT.NONE);
                    l1.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    l1.setText("Sex: ");
                    Canvas c2 = new Canvas(c1, SWT.NONE);
                    c2.setLayout(new GridLayout(2, false));
                    c2.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    String[] s1 = { "Male", "Female" };
                    final Button[] s2 = new Button[s1.length];
                    for (int i = 0; i < s1.length; i++) {
                        s2[i] = new Button(c2, SWT.RADIO);
                        s2[i].setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                        s2[i].setText(s1[i]);
                    }
                    l1 = new Label(c1, SWT.NONE);
                    l1.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    l1.setText("Country: ");
                    final Combo con = new Combo(c1, SWT.BORDER | SWT.DROP_DOWN);
                    con.setItems(new String[] { "Afghanistan", "Albania", "Algeria", "American Samoa", "Andorra", "Angola", "Anguilla", "Antarctica", "Antigua and Barbuda", "Argentina", "Armenia", "Aruba", "Ascension", "Australia", "Austria", "Azebaijan", "Bahamas", "Bahrain", "Bangladesh", "Barbados", "Belarus", "Belgium", "Belize", "Benin", "Bermuda", "Bhutan", "Bolivia", "Bosnia and Herzegovina", "Botswana", "Bouvet Island", "Brazil", "Brunei Darussalam", "Bulgaria", "Burkina Faso", "British Indian Ocean Territory", "Burundi", "Cambodia", "Cameroon", "Canada", "Cape Verda", "Cayman Islands", "Central African Republic", "Chad", "Chile", "China", "christmas Island", "Cocos Islands", "Colombia", "Comoros", "Congo", "Congo (DRC)", "Cook Island", "Costa Rica", "Cote d'Ivoire", "Croatia", "Cuba", "Cyprus", "Czech Republic", "Denmark", "Djibouti", "Dominica", "Dominican Republic", "Ecuador", "Egypt", "El Salvador", "Equatorial Guinea", "Eritrea", "Estonia", "Ethiopia", "Falkland Island (Islas Malvinas)", "Faroe Islands", "Fiji Islands", "Finland", "France", "French Guiana", "French Polynesia", "French Southern and Antarctic Lands", "Gabon", "Gambia, The", "Georgia", "Germany", "Ghana", "Gibraltar", "Greece", "Greenland", "Grenada", "Guadeloupe", "Guam", "Guatemala", "Guernsey", "Guinea", "Guinea-Bissau", "Guyana", "Haiti", "Heard Island and McDonald Islands", "Honduras", "Hong Kong SAR", "Hungary", "Iceland", "India", "Indonesia", "Iran", "Iraq", "Ireland", "Isle of Man", "Isreal", "Italy", "Jamaica", "Japan", "Jersey", "Jordan", "Kazakhstan", "Kenya", "Kiribati", "Korea", "Kuwait", "Kyrgyzstan", "Laos", "Latvia", "Lebanon", "Lesotho", "Liberia", "Libya", "Liechtenstein", "Lithuania", "Luxembourg", "Macao SAR", "Macedonia, Former Yogoslav Republic of", "Madagascar", "Melawi", "Malaysia", "Maldives", "Mali", "Malta", "Marshall Islands", "Martinique", "Mauritania", "Mauritius", "Mayotte", "Mexico", "Micronesia", "Moldova", "Monaco", "Mongolia", "Montserrat", "Morocco", "Mozambique", "Myanmar", "Namibia", "Nauru", "Nepal", "Netherlands", "Netherlands Antilles", "New Caledonia", "New Zealand", "Nicaragua", "Niger", "Nigeria", "Niue", "Norfolk Island", "North Korea", "Nothern Mariana Islands", "Norway", "Oman", "Pakistan", "Palau", "Palestinian Authority", "Panama", "Papua New Guinea", "Paraguay", "Peru", "Philippines", "Pitcairn Islands", "Poland", "Portugal", "Puerto Rico", "Qatar", "Reunion", "Romania", "Russia", "Rwanda", "Samoa", "San Marino", "Sao Tome and Principe", "Saudi Arabia", "Senegal", "Serbia and Montenegro", "Seychelles", "Sierra Leone", "Singapore", "Slovakia", "Slovenia", "Solomon Islands", "Somalia", "South Africa", "South Georgia and the South Sandwich Islands", "Spain", "Sri Lanka", "St. Helena", "St. Kitts and Nevis", "St. Lucia", "St. Pierre and Miquelon", "St. Vincent and the Grenadines", "Sudan", "Suriname", "Svalbard and Jan Mayen", "Swaziland", "Switzerland", "Syria", "Taiwan", "Tajikistan", "Tanzania", "Thailand", "Timor-Leste", "Togo", "Tokelau", "Tonga", "Trinidad and Tobado", "Tristan da Cunha", "Tunisia", "Turkey", "Turkmenistan", "Turks and Caicos Islands", "Tuvalu", "Uganda", "Ukraine", "United Arab Emirates", "United Kingdom", "United States", "United States Minor Outlyinng Islands", "Uruguay", "Uzbekistan", "Vanuatu", "Vatican City", "Venezuela", "Vietnam", "Virgin ISlands", "Virgin Islands, British", "Wallis and Futuna", "Yemen", "Zambia", "Zimbabwe" });
                    con.setToolTipText("Select one from the list");
                    l1 = new Label(c1, SWT.NONE);
                    l1.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    l1.setText("Occupation: ");
                    final Combo occ = new Combo(c1, SWT.BORDER | SWT.DROP_DOWN);
                    occ.setItems(new String[] { "Accounting/finance", "Computer related (Internet)", "Computer related (other)", "Consulting", "Customer service/support", "Education/training", "Engineering", "Executive/senior management", "General administrative/supervisory", "Government/military", "HR/Personnel", "IS/IT management", "Manufacturing/production/operations", "Professional (medical, legal, etc.)", "Research and development", "Retired", "Sales/marketing/advertising", "Self-employed/owner", "Student", "Unemployed/between jobs", "Homemaker", "Tradesman/craftsman", "Other" });
                    occ.setToolTipText("Select one from the list");
                    l1 = new Label(c1, SWT.NONE);
                    l1.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    l1.setText("Marital Status: ");
                    final Combo sta = new Combo(c1, SWT.BORDER);
                    sta.setItems(new String[] { "Single", "Married", "Not specified" });
                    sta.setToolTipText("Select one from the list");
                    l1 = new Label(c1, SWT.NONE);
                    l1.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    l1.setText("Annual Salary: ");
                    final Combo sal = new Combo(c1, SWT.BORDER);
                    sal.setItems(new String[] { "Above 100,000", "75,001-100,000", "50,001-75,000", "25,001-50,000", "10,001-25,000", "Below 10,000" });
                    sal.setToolTipText("Select one from the list");
                    Canvas c3 = new Canvas(dlg, SWT.NONE);
                    c3.setLayout(new GridLayout());
                    c3.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    Group g2 = new Group(c3, SWT.NONE);
                    g2.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    g2.setLayout(new GridLayout(3, false));
                    g2.setText("Interest");
                    String[] interest = { "Art", "Autos", "Business", "Computers & Internet", "Education", "Entertainment", "Finance & Investments", "Food & Dining", "Games", "Health/Medicine", "Hobbies", "Home & Family", "Music", "Outdoor/Recreation", "Pets", "Politics", "Religion", "Shopping", "Sports", "Travel" };
                    final Button[] b = new Button[interest.length];
                    for (int i = 0; i < b.length; i++) {
                        b[i] = new Button(g2, SWT.CHECK);
                        b[i].setText(interest[i]);
                        b[i].setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    }
                    Canvas c4 = new Canvas(dlg, SWT.NONE);
                    c4.setLayout(new GridLayout(2, true));
                    c4.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    Button submit = toolkit.createButton(c4, "Submit", SWT.PUSH);
                    submit.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    submit.addListener(SWT.Selection, new Listener() {

                        public void handleEvent(Event event) {
                            boolean tempstate = true;
                            for (int i = 0; i < b.length; i++) {
                                if (b[i].getSelection()) tempstate = false;
                            }
                            if (age.getSelectionIndex() == -1 || (!s2[0].getSelection() && !s2[1].getSelection()) || con.getSelectionIndex() == -1 || occ.getSelectionIndex() == -1 || sta.getSelectionIndex() == -1 || sal.getSelectionIndex() == -1 || tempstate) {
                                MessageBox mbox = new MessageBox(new Shell(), SWT.ICON_WARNING | SWT.OK);
                                mbox.setText("Oops!");
                                mbox.setMessage("All fields are required");
                                if (mbox.open() == SWT.YES) {
                                }
                            } else {
                                String inter = "";
                                for (int i = 0; i < b.length; i++) {
                                    if (b[i].getSelection()) {
                                        inter += "1";
                                    } else {
                                        inter += "0";
                                    }
                                }
                                PrintWriter reg = null;
                                try {
                                    reg = new PrintWriter(Activator.getAppPath() + "/register.ini");
                                    reg.println("[INFO]");
                                    reg.println("Age=" + age.getSelectionIndex());
                                    for (int i = 0; i < s2.length; i++) {
                                        if (s2[i].getSelection()) {
                                            reg.println("Sex=" + i);
                                        }
                                    }
                                    reg.println("Country=" + con.getSelectionIndex());
                                    reg.println("Occupation=" + occ.getSelectionIndex());
                                    reg.println("Salary=" + sal.getSelectionIndex());
                                    reg.println("Status=" + sta.getSelectionIndex());
                                    reg.println("Interest=" + inter);
                                } catch (FileNotFoundException e1) {
                                    e1.printStackTrace();
                                }
                                reg.close();
                                dlg.close();
                                try {
                                    StartReg();
                                } catch (Exception e) {
                                    MessageBox mBox = new MessageBox(new Shell(), SWT.ICON_INFORMATION | SWT.OK);
                                    mBox.setText("Unregistered");
                                    mBox.setMessage("Registration Failed: " + e);
                                    if (mBox.open() == SWT.YES) {
                                        mBox.getParent().close();
                                    }
                                }
                            }
                        }
                    });
                    Button cancel = toolkit.createButton(c4, "Cancel", SWT.PUSH);
                    cancel.setBackground(dlg.getDisplay().getSystemColor(SWT.COLOR_WHITE));
                    cancel.addListener(SWT.Selection, new Listener() {

                        public void handleEvent(Event event) {
                            PlatformUI.getWorkbench().close();
                        }
                    });
                    dlg.setLocation(GeneralFunc.centerScreen(dlg, dlg.getBounds().width, dlg.getBounds().height));
                    dlg.open();
                }
            });
        } else {
            MessageBox mBox = new MessageBox(new Shell(), SWT.ICON_INFORMATION | SWT.OK);
            mBox.setText("Registered");
            mBox.setMessage("Registration Successful");
            if (mBox.open() == SWT.YES) {
                mBox.getParent().close();
                Activator.runDBUpdate();
            }
        }
    }

    /**
	 * Is server registration success
	 * @return true if success
	 */
    public static boolean IsServerRegSuccess() {
        SocketChannel channel = null;
        String jStatus = null;
        String temp = null;
        try {
            Charset charset = Charset.forName("ISO-8859-1");
            CharsetDecoder decoder = charset.newDecoder();
            CharsetEncoder encoder = charset.newEncoder();
            ByteBuffer buffer = ByteBuffer.allocateDirect(1024);
            CharBuffer charBuffer = CharBuffer.allocate(1024);
            InetSocketAddress socketAddress = new InetSocketAddress(url.getHost(), 80);
            channel = SocketChannel.open();
            channel.connect(socketAddress);
            String request = "GET " + url + "register.php?csz=" + checkEmptyString(jDtr) + "&&mac=" + MAC + "&&uname=" + checkEmptyString(InstallName) + "&&cname=" + checkEmptyString(InstallCompany) + "&&winuname=" + checkEmptyString(WinName) + "&&wincname=" + checkEmptyString(WinCompany) + "&&age=" + checkEmptyString(jAge) + "&&sal=" + checkEmptyString(jSal) + "&&sta=" + checkEmptyString(jSta) + "&&sex=" + checkEmptyString(jSex) + "&&con=" + checkEmptyString(jCon) + "&&occ=" + checkEmptyString(jOcc) + "&&int=" + checkEmptyString(jInt) + "&&ver=" + checkEmptyString(jVer) + "&&mor=" + checkEmptyString(jTyp) + " \r\n\r\n";
            channel.write(encoder.encode(CharBuffer.wrap(request)));
            while ((channel.read(buffer)) != -1) {
                buffer.flip();
                decoder.decode(buffer, charBuffer, false);
                charBuffer.flip();
                temp = charBuffer.toString();
                buffer.clear();
                charBuffer.clear();
            }
        } catch (UnknownHostException e) {
            System.err.println(e);
        } catch (IOException e) {
            System.err.println(e);
        } finally {
            if (channel != null) {
                try {
                    channel.close();
                } catch (IOException ignored) {
                }
            }
        }
        int index1 = temp.indexOf("=");
        int index2 = temp.indexOf(";");
        jStatus = temp.substring(index1 + 1, index2);
        if (jStatus.equals("0")) {
            index1 = temp.indexOf("=", index2);
            index2 = temp.indexOf(";", index1);
            Serial = temp.substring(index1 + 1, index2);
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Download registration page
	 * @return true if server response OK
	 */
    public static boolean downloadRegPage() {
        String filename = "register.php?csz=" + checkEmptyString(jDtr) + "&&mac=" + MAC + "&&uname=" + checkEmptyString(InstallName) + "&&cname=" + checkEmptyString(InstallCompany) + "&&winuname=" + checkEmptyString(WinName) + "&&wincname=" + checkEmptyString(WinCompany) + "&&age=" + checkEmptyString(jAge) + "&&sal=" + checkEmptyString(jSal) + "&&sta=" + checkEmptyString(jSta) + "&&sex=" + checkEmptyString(jSex) + "&&con=" + checkEmptyString(jCon) + "&&occ=" + checkEmptyString(jOcc) + "&&int=" + checkEmptyString(jInt) + "&&ver=" + checkEmptyString(jVer) + "&&mor=" + checkEmptyString(jTyp);
        URL url1 = null;
        try {
            url1 = new URL(url + filename);
        } catch (MalformedURLException e1) {
        }
        int status = 0;
        try {
            status = ((HttpURLConnection) url1.openConnection()).getResponseCode();
        } catch (IOException e1) {
            System.out.println(e1);
        }
        if (status == 200) {
            return true;
        } else {
            return false;
        }
    }

    /**
	 * Check if the string is empty
	 * @param StringToCheck
	 * @return StringToCheck
	 */
    public static String checkEmptyString(String StringToCheck) {
        if (StringToCheck.length() == 0) {
            StringToCheck = "not_found";
        }
        return StringToCheck;
    }

    /**
	 * Read from profile
	 * @param title
	 * @param item
	 * @param filename
	 * @return value
	 */
    public static String getFromFile(String title, String item, String filename) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(filename));
            String temp = null;
            while ((temp = in.readLine()) != null) {
                if (temp.equals(title)) {
                    while ((temp = in.readLine()) != null) {
                        if (temp.contains(item)) {
                            int index = temp.indexOf("=");
                            in.close();
                            return temp.substring(index + 1, temp.length());
                        }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            return "not_found";
        } catch (IOException e) {
        }
        return "not_found";
    }

    /**
	 * get the Windows registered name and company
	 * @throws IOException
	 */
    public static void accessRegistry() throws IOException {
        String strReturn = "not_detected";
        String command = "systeminfo";
        Process pid = Runtime.getRuntime().exec(command);
        BufferedReader in = new BufferedReader(new InputStreamReader(pid.getInputStream()));
        while (true) {
            String line = in.readLine();
            if (line == null) break;
            Pattern p = Pattern.compile(".*Registered Owner.*: (.*)");
            Matcher m = p.matcher(line);
            if (m.matches()) {
                strReturn = m.group(1);
                break;
            }
        }
        in.close();
        WinName = strReturn.replace(" ", "");
        command = "systeminfo";
        pid = Runtime.getRuntime().exec(command);
        in = new BufferedReader(new InputStreamReader(pid.getInputStream()));
        while (true) {
            String line = in.readLine();
            if (line == null) break;
            Pattern p = Pattern.compile(".*Registered Organization.*: (.*)");
            Matcher m = p.matcher(line);
            if (m.matches()) {
                strReturn = m.group(1);
                break;
            }
        }
        in.close();
        WinCompany = strReturn.replace(" ", "");
    }
}
