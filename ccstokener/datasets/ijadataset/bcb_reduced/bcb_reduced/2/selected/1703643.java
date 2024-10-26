package it.uniromadue.portaleuni.manager;

import it.uniromadue.portaleuni.dto.Profilo;
import it.uniromadue.portaleuni.utils.Conf;
import java.net.URL;
import java.util.ArrayList;
import org.apache.log4j.Logger;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class ProfiliManager {

    /**
	 * Gestisce il mapping tra tipi di utenti e funzioni
	 * a cui questi possono accedere
	 */
    private static final Logger logger = Logger.getLogger(ProfiliManager.class);

    private static ArrayList profili = new ArrayList();

    static {
        initProfili();
    }

    private static void initProfili() {
        try {
            URL url = new URL(Conf.XML_FILE_URL);
            XStream xstream = new XStream(new DomDriver());
            xstream.alias("profili", ArrayList.class);
            xstream.alias("profilo", Profilo.class);
            xstream.alias("userType", String.class);
            xstream.alias("functions", ArrayList.class);
            xstream.alias("function", String.class);
            profili = (ArrayList) xstream.fromXML(url.openStream());
            logger.info("Loading profili from " + Conf.XML_FILE_URL);
        } catch (Exception e) {
            logger.error("Unable to load profili from " + Conf.XML_FILE_URL + ". " + e.getMessage());
        }
    }

    /**
	 * @param userType
	 * @param function
	 * @return true se la funzione � abilitata per il tipo di utente, false altrimenti
	 */
    public static boolean isEnabled(String userType, String function) {
        for (int i = 0; i < profili.size(); i++) {
            Profilo p = (Profilo) profili.get(i);
            if (p.getUserType().equalsIgnoreCase(userType)) {
                ArrayList functions = (ArrayList) p.getFunctions();
                if (functions != null) {
                    for (int j = 0; j < functions.size(); j++) {
                        String f = (String) functions.get(j);
                        if (f != null && f.equalsIgnoreCase(function)) return true;
                    }
                }
            }
        }
        return false;
    }
}
