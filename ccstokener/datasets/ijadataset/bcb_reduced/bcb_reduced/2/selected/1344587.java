package com.io_software.utils.sms;

import com.io_software.utils.web.Form;
import com.io_software.utils.web.HTMLPage;
import com.io_software.utils.web.TextInputElement;
import com.io_software.utils.web.SelectInputElement;
import com.io_software.utils.web.ActualFormParameter;
import com.io_software.utils.web.ActualFormParameters;
import inspection.ObjectInspector;
import com.abb.util.TeeReader;
import java.net.URL;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.BufferedReader;

/** wraps <tt>http://www.sms-kostenlos.de</tt>, the free SMS sending
    web service

    @author Axel Uhl
    @version $Id: SMSSender.java,v 1.15 2001/10/25 08:20:33 uhl Exp $
*/
public class SMSSender {

    /** (re-)initializes the form. Abacho's web site requires a new session for
	every SMS being sent.
    */
    private void init() throws IOException {
        URL url1 = new URL("http://mail.abacho.de/freesms/");
        HTMLPage p1 = new HTMLPage(url1);
        HTMLPage p2 = null;
        HTMLPage p3 = null;
        for (Enumeration e = p1.getLinks(); p2 == null && e.hasMoreElements(); ) {
            URL l = (URL) e.nextElement();
            if (l.toString().indexOf("schritt_02") >= 0) {
                p2 = new HTMLPage(l);
                for (Enumeration f = p2.getLinks(); f.hasMoreElements(); ) {
                    URL l2 = (URL) f.nextElement();
                    if (l2.toString().indexOf("schritt_03") >= 0) {
                        p3 = new HTMLPage(l2, l);
                        form = p3.getFormByName("Main");
                        if (form == null) throw new IOException("Error: SMS sending form not found in page");
                    }
                }
                if (p3 == null) throw new IOException("Error: AGB page (schritt_03) not found");
            }
        }
        if (p2 == null) throw new IOException("Error: Entry page (schritt_02) not found");
        messageInputElement = (TextInputElement) form.getInputElementByName("message");
        number1InputElement = (SelectInputElement) form.getInputElementByName("vorwahl");
        number2InputElement = (TextInputElement) form.getInputElementByName("rufnummer");
        letterCountInputElement = (TextInputElement) form.getInputElementByName("Anzahl");
    }

    /** Sends a message that can be longer than MAX_PAYLOAD characters by
	splitting it into several parts.

	@param number the phone number string. May contain non-numeric
	characters, which will be ignored. If the
	number starts with "00", then the next two digits will be
	matched against the available country codes (currently 49, 43,
	and 41).<p>
	If the number starts with only one "0", then the next three
	digits are matched against the network provider code. The
	remainder of the number goes into the <tt>number</tt> field.
	@param message the message string to be sent. The string
	length is cut down to MAX_PAYLOAD characters if the string is longer.
    */
    public void sendLong(String number, String message) throws IOException {
        do {
            String part = message.substring(0, Math.min(MAX_PAYLOAD, message.length()));
            message = message.substring(part.length());
            boolean success = false;
            for (int i = 0; !success && i < RETRIES; i++) {
                success = send(number, part);
                if (!success) System.out.println("Failed on attempt #" + i);
            }
            if (!success) throw new IOException("Error: sending message failed " + RETRIES + " times. Giving up.");
        } while (message.length() > 0);
    }

    /** sends an SMS message to the specified number. Any non-numeric
	characters contained in the number string are ignored.

	@param number the phone number string. May contain non-numeric
	characters, which will be ignored. If the
	number starts with "00", then the next two digits will be
	matched against the available country codes (currently 49, 43,
	and 41).<p>
	If the number starts with only one "0", then the next three
	digits are matched against the network provider code. The
	remainder of the number goes into the <tt>number</tt> field.
	@param message the message string to be sent. The string length is cut
	down to MAX_PAYLOAD characters if the string is longer.  All
	whitespaces other than blanks are replaced by blanks.
	@return <tt>true</tt> in case sending was considered successful,
	<tt>false</tt> if there is reason to believe that sending the SMS
	failed.
    */
    public boolean send(String number, String message) throws IOException {
        init();
        message = message.substring(0, Math.min(MAX_PAYLOAD, message.length()));
        message = message.replace('\r', ' ');
        message = message.replace('\n', ' ');
        ActualFormParameters params = new ActualFormParameters();
        String strippedNumber = strip(number);
        ActualFormParameter number1Param;
        ActualFormParameter number2Param;
        if (strippedNumber.startsWith("00")) strippedNumber = "+" + strippedNumber.substring(2); else if (strippedNumber.startsWith("0")) strippedNumber = "+49" + strippedNumber.substring(1);
        number1Param = new ActualFormParameter(number1InputElement.getName(), strippedNumber.substring(0, 6));
        number2Param = new ActualFormParameter(number2InputElement.getName(), strippedNumber.substring(6));
        params.add(number1Param);
        params.add(number2Param);
        ActualFormParameter messageParam = new ActualFormParameter(messageInputElement.getName(), message);
        params.add(messageParam);
        ActualFormParameter letterCountParam = new ActualFormParameter(letterCountInputElement.getName(), "" + (MAX_PAYLOAD - message.length()));
        params.add(letterCountParam);
        form.addDefaultParametersTo(params);
        Reader r = form.submitForm(params, form.getNetscapeRequestProperties());
        String result = getStringFromReader(r);
        String pattern = "<meta http-equiv = \"refresh\" content=\"1; url=";
        int patternIndex = result.indexOf(pattern);
        if (patternIndex < 0) return false;
        int end = result.lastIndexOf("\">");
        if (end < 0) return false;
        String url = result.substring(patternIndex + pattern.length(), end);
        result = getStringFromReader(new InputStreamReader(new URL(url).openStream()));
        return result.indexOf("wurde erfolgreich verschickt") >= 0;
    }

    /** The passed page is expected to contain a form which has a submit button
	text containing the substring "send". If such a form is found, it is
	returned. Otherwise <tt>null</tt> is returned.

	@param p the page whose forms to analyze
	@return a form containing the substring "send" in its submit button
	text or <tt?null</tt> if no such form is found in the passed page.
    */
    private Form getConfirmForm(HTMLPage p) {
        for (Enumeration e = p.getForms(); e.hasMoreElements(); ) {
            Form f = (Form) e.nextElement();
            String sbt = f.getSubmitButtonText();
            if (sbt.indexOf("send") >= 0) return f;
        }
        return null;
    }

    /** removes all non-numerical characters from the passed number
	string

	@param s the string to remove the non-numerical characters
	from. Must not be <tt>null</tt>.
	@return a copy of <tt>s</tt> with the non-numerical characters
	removed
    */
    private String strip(String s) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') result.append(c);
        }
        return result.toString();
    }

    /** opens an {@link SMSDialog} and asks for the number and the
	text, and when committed, sends it
    */
    public void sendWithInputDialog() throws IOException {
        SMSDialog d = new SMSDialog();
        d.addNotify();
        d.setSize(d.getPreferredSize());
        d.setVisible(true);
        if (d.wasCommitted()) sendLong(d.getNumber(), d.getMessage());
    }

    /** This simple <tt>main</tt> method sends the message in
	<tt>args[1]</tt> to the number specified in <tt>args[0]</tt>.

	@param args the message is expected in <tt>args[1]</tt>, the
	number in <tt>args[0]</tt>
    */
    public static void main(String[] args) {
        try {
            SMSSender sender = new SMSSender();
            if (args.length == 2) sender.sendLong(args[0], args[1]); else {
                sender.sendWithInputDialog();
                System.exit(0);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /** reads the complete reader contents and returns them as a string.
	Note, that this is not a very scalable solution since it doesn't
	allow "streaming" of very long readers. The original problem is
	the implementation bug in the OROINC Perl5StreamInput
	implementation.
	
	@param r the reader from which to read the string. Will be closed
	    by this method after reading the contents.
	@return the contents of <tt>r</tt> as string
      */
    private String getStringFromReader(Reader r) throws IOException {
        StringBuffer result = new StringBuffer();
        char[] buffer = new char[8192];
        int c = 0;
        while (c != -1) {
            c = r.read(buffer);
            if (c > 0) result.append(buffer, 0, c);
        }
        r.close();
        return result.toString();
    }

    /** Number of retries in case sending seems to have failed */
    private static final int RETRIES = 5;

    /** the form that can be used to send an SMS */
    private Form form;

    /** message input element */
    private TextInputElement messageInputElement;

    /** prefix input element */
    private SelectInputElement number1InputElement;

    /** number input element */
    private TextInputElement number2InputElement;

    /** number input element */
    private TextInputElement letterCountInputElement;

    /** maximum length of SMS payload */
    private static final int MAX_PAYLOAD = 133;
}
