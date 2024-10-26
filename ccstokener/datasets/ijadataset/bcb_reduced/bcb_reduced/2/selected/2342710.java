package zimbragh;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import au.com.bytecode.opencsv.CSVReader;

/**
 * Klasa delegata biznesowego dla pakietu kolaboracyjnego Zimbra 
 * realizującego podstawowe operacje na książce adresowej i kaledarzu
 *
 */
public class ZimbraDelegate {

    private String baseUrl;

    private String hostUrl;

    private String user;

    private String password;

    private Log log = LogFactory.getLog(ZimbraDelegate.class);

    /**
	 * Domyślny konstruktor.
	 * 
	 * @param host Nazwa komputera, na jakim znajduje się serwer Zimbry (np. testzimbra.com)
	 * @param user Nazwa konta użytkownika
	 * @param password Hasło użytkownika
	 */
    public ZimbraDelegate(String host, String user, String password) throws IOException, ParserException {
        this.hostUrl = "http://www." + host;
        this.baseUrl = hostUrl + "/zimbra/user/" + user;
        this.user = user;
        this.password = password;
    }

    /**
	 * Pobiera listę kontaktów z serwera.
	 * 
	 * @return Mapa, której kluczami są adresy e-mail osób z książki adresowej, a wartościami obiekty klasy Contact
	 * @throws IOException
	 */
    public Map<String, Contact> getContacts() throws IOException {
        Map<String, Contact> ret = new HashMap<String, Contact>();
        authenticate();
        URLConnection conn = (new URL(baseUrl + "/contacts.csv")).openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        CSVReader reader = new CSVReader(in);
        String[] nextLine;
        String[] headerLine = reader.readNext();
        while ((nextLine = reader.readNext()) != null) {
            Map<String, String> contactInfo = new HashMap<String, String>();
            for (int i = 0; i < nextLine.length; i++) {
                contactInfo.put(headerLine[i], nextLine[i]);
            }
            Contact newContact = new Contact(contactInfo);
            ret.put(newContact.getEmail(), newContact);
        }
        return ret;
    }

    private void authenticate() {
        Authenticator.setDefault(new Authenticator() {

            public PasswordAuthentication getPasswordAuthentication() {
                return (new PasswordAuthentication(user, password.toCharArray()));
            }
        });
    }

    /**
	 * Dodaje kontakt na serwer.
	 * 
	 * @param email Adres e-mail identyfikujący kontakt
	 * @param contact Obiekt reprezentujący pozycję książki adresowej
	 * @throws IOException
	 */
    public void putContact(String email, Contact contact) throws IOException {
        contact.setEmail(email);
        URL url = new URL(hostUrl + "/service/home/" + user + "/contacts?fmt=csv");
        authenticate();
        HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
        urlConn.setRequestMethod("PUT");
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream());
        printout.writeBytes(contact.toCSV());
        printout.flush();
        printout.close();
        BufferedReader input = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
        String str;
        while (null != ((str = input.readLine()))) {
            log.debug(str);
        }
        input.close();
    }

    /**
	 * Pobiera kalendarz z serwera.
	 * 
	 * @return Obiekt typu Calendar (zgodny z biblioteką iCal4j)
	 * @throws IOException
	 * @throws ParserException
	 */
    public Calendar getCalendar() throws IOException, ParserException {
        URL url = new URL(baseUrl + "/calendar.ics");
        Authenticator.setDefault(new Authenticator() {

            public PasswordAuthentication getPasswordAuthentication() {
                return (new PasswordAuthentication(user, password.toCharArray()));
            }
        });
        URLConnection conn = url.openConnection();
        InputStream is = conn.getInputStream();
        CalendarBuilder builder = new CalendarBuilder();
        return builder.build(is);
    }

    /**
	 * Zapisuje kalendarz na serwer.
	 * 
	 * @param newCalendar Obiekt kalendarza z biblioteki iCal4j do zapisania na serwerze
	 * @throws IOException
	 */
    public void setCalendar(Calendar newCalendar) throws IOException {
        URL url = new URL(hostUrl + "/service/home/" + user + "/calendar?fmt=ics");
        Authenticator.setDefault(new Authenticator() {

            public PasswordAuthentication getPasswordAuthentication() {
                return (new PasswordAuthentication(user, password.toCharArray()));
            }
        });
        URLConnection urlConn = url.openConnection();
        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        urlConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        DataOutputStream printout = new DataOutputStream(urlConn.getOutputStream());
        printout.writeBytes(newCalendar.toString());
        printout.flush();
        printout.close();
        BufferedReader input = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
        String str;
        while (null != (str = input.readLine())) {
            log.debug(str);
        }
        input.close();
    }
}
