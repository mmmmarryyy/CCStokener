package com.ldap.jedi;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Properties;
import javax.naming.NamingEnumeration;

/**
 * Classe contenant l'ensemble des m�thodes n�cessaires pour le traitement des caract�res sp�ciaux ainsi que des m�thodes attach�s � aucun objet du package.
 * 
 * @author HUMEAU Xavier
 * @version Version 1.0
 */
public class JediUtil {

    /**
	 * Constante de s�paration pour les chemins LDAP.
	 */
    public static final String SEPARATOR = ",";

    /**
	 * Constante fixant le temps limite de recherche (en ms).
	 */
    protected static int SEARCH_TIME_LIMIT = 300000;

    /**
	 * Constante specifiant l'affichage ou non de la trace.
	 */
    protected static boolean CST_PRINT_TRACE = false;

    /**
	 * Constante specifiant le d�bugage ou non.
	 */
    protected static boolean CST_PRINT_DEBUG = false;

    /**
	 * M�thode qui retourne le temps limite fix�.
	 * 
	 * @return Le temps fix�.
	 */
    public static int getTimeLimit() {
        return (SEARCH_TIME_LIMIT);
    }

    /**
	 * M�thode qui sp�cifie combien de temps la recherche doit durer au maximum. Si le temps est �gal � 0 alors le temps est infini.
	 * 
	 * @param time
	 *            Le temps que la recherche ne doit pas d�passer.
	 * @throws JediException
	 *             Si le temps pass� en param�tre est n�gatif.
	 */
    public static void setTimeLimit(int time) throws JediException {
        if (time < 0) {
            JediLog.log(JediLog.LOG_TECHNICAL, JediLog.ERROR, "jedi_msg_parameter_error", Integer.toString(time), null);
            throw new JediException("JediUtil : setTimeLimit(int) : param�tre d'initialisation incorrect");
        }
        SEARCH_TIME_LIMIT = time;
    }

    /**
	 * M�thode qui indique si on affiche la trace.
	 * 
	 * @return Si il y a ou non affichage de la trace.
	 */
    public static boolean getWithTrace() {
        return (CST_PRINT_TRACE);
    }

    /**
	 * M�thode qui sp�cifie si on veut ou non afficher la trace.
	 * 
	 * @param trace
	 *            Sp�cifie si on affiche la trace.
	 */
    public static void setWithTrace(boolean trace) {
        CST_PRINT_TRACE = trace;
    }

    /**
	 * M�thode qui indique si on debug.
	 * 
	 * @return Si il y a ou non un debugage.
	 */
    public static boolean getDebug() {
        return (CST_PRINT_DEBUG);
    }

    /**
	 * M�thode qui sp�cifie si on veut ou non d�buger.
	 * 
	 * @param debug
	 *            Sp�cifie si on debug.
	 */
    public static void setDebug(boolean debug) {
        CST_PRINT_DEBUG = debug;
        if (debug == true) {
            JediUtil.setWithTrace(true);
        }
    }

    /**
	 * M�thode permettant de charger un properties
	 * 
	 * @param name
	 *            L'URL du properties
	 * @return Un properties
	 */
    public static Properties loadProperties(String name) throws JediException {
        URL url = null;
        InputStream conf = null;
        Properties prop = new Properties();
        BufferedInputStream bconf = null;
        if (name == null) {
            JediLog.log(JediLog.LOG_TECHNICAL, JediLog.ERROR, "jedi_msg_parameter_error", "", null);
            throw new JediException("URI nulle");
        }
        try {
            url = new URL(name);
            conf = url.openStream();
        } catch (MalformedURLException e) {
            try {
                conf = new FileInputStream(name);
            } catch (FileNotFoundException ex) {
                JediLog.log(JediLog.LOG_TECHNICAL, JediLog.ERROR, "jedi_msg_internal_error", "", null);
                throw new JediException("Le fichier de configuration est introuvable");
            }
        } catch (IOException ex) {
            JediLog.log(JediLog.LOG_TECHNICAL, JediLog.ERROR, "jedi_msg_internal_error", "", null);
            throw new JediException("Le fichier de configuration est inaccessible");
        }
        if (conf == null) {
            return null;
        }
        try {
            bconf = new BufferedInputStream(conf);
            prop.load(bconf);
            conf.close();
        } catch (IOException ex) {
            JediLog.log(JediLog.LOG_TECHNICAL, JediLog.ERROR, "jedi_msg_internal_error", "", null);
            throw new JediException("Erreur en cours de lecture du fichier de configuration");
        }
        return prop;
    }

    /**
	 * @param fichierProperties
	 * @return Properties
	 * @throws Exception
	 */
    public static Properties getPropsFromFile(String fichierProperties) throws Exception {
        Properties props = null;
        InputStream is = JediUtil.class.getResourceAsStream(fichierProperties);
        if (is == null) {
            throw new Exception();
        }
        props = new Properties();
        props.load(is);
        return props;
    }

    /**
	 * Tableau r�f�ren�ant les caract�res n�cessitant un caract�re d'�chappement au sein de la m�thode otherToLdap.
	 */
    protected static char[] SPECIALS_CARACTERES = { '/', '\\', '<', '>', '+', '#', ';', ',', '\"' };

    /**
	 * M�thode ajoutant un caract�re d'�chappement devant tous les caract�res de stringToModify que l'on retrouve dans le tableau arrayWithoutRead.
	 * 
	 * @param stringToModify
	 *            String � modifier.
	 * @return Le String pass� en param�tre avec des caract�res d'�chappement.
	 * @throws JediException
	 *             Si le String est null ou vide.
	 */
    public static String formatForLdap(String stringToModify) throws JediException {
        boolean charIsSpecial = false;
        boolean charIsVerySpecial = false;
        int indexChar = 0;
        int stringToModifyLength = stringToModify.length();
        if (stringToModify == null || stringToModify.length() == 0) {
            throw new JediException("JediUtil : otherToLdap(String) : Param�tre d'initialisation incorrect");
        }
        while (indexChar < stringToModifyLength) {
            charIsSpecial = false;
            charIsVerySpecial = false;
            for (int i = 0; i < SPECIALS_CARACTERES.length; i++) {
                charIsSpecial = charIsSpecial || (stringToModify.charAt(indexChar) == SPECIALS_CARACTERES[i]);
                charIsVerySpecial = charIsVerySpecial || (stringToModify.charAt(indexChar) == '\"');
            }
            if (charIsSpecial == true && charIsVerySpecial == false) {
                stringToModify = stringToModify.substring(0, indexChar) + "\\" + stringToModify.substring(indexChar, stringToModifyLength);
                indexChar++;
                stringToModifyLength++;
            }
            if (charIsSpecial == true && charIsVerySpecial == true && indexChar != stringToModifyLength) {
                stringToModify = stringToModify.substring(0, indexChar) + "\'" + stringToModify.substring(indexChar + 1, stringToModifyLength);
            }
            if (charIsSpecial == true && charIsVerySpecial == true && indexChar == stringToModifyLength) {
                stringToModify = stringToModify.substring(0, indexChar) + "\'";
            }
            indexChar++;
        }
        return (stringToModify);
    }

    /**
	 * M�thode qui d�tecte si le String pass� en param�tre contient � la fois des caract�res du tableau arraySpecial et du tableau arrayIncompatible car ces
	 * deux tableaux sont incompatibles au sein d'un m�me String.
	 * 
	 * @param stringToModify
	 *            String � analyser.
	 * @return Si le String contient des caract�res incompatibles ou non.
	 * @throws JediException
	 *             Si le String est null ou vide.
	 */
    public static boolean searchIncompatibility(String stringToModify) throws JediException {
        boolean charIsSpecial = false;
        boolean charIncompatible = false;
        char charAtIndex = 0;
        int indexChar = 0;
        if (stringToModify == null) {
            throw new JediException("JediUtil : searchIncompatibility(String) : Param�tre d'initialisation incorrect");
        }
        while (indexChar < stringToModify.length()) {
            charIsSpecial = charIsSpecial || (stringToModify.charAt(indexChar) == '/');
            if (indexChar == stringToModify.length() - 1) {
                charIncompatible = charIncompatible || (stringToModify.charAt(indexChar) == '\\');
            } else {
                charAtIndex = (stringToModify.charAt(indexChar + 1));
                charIncompatible = charIncompatible || ((stringToModify.charAt(indexChar) == '\\') && (Arrays.binarySearch(SPECIALS_CARACTERES, charAtIndex) < 0));
            }
            indexChar++;
        }
        return (charIsSpecial && charIncompatible);
    }

    /**
	 * M�thode qui supprime de la liste tous les attributs ayant une valeur nulle.
	 * 
	 * @param attributeList
	 *            La liste dans laquelle on veut supprimer les elements nuls.
	 * @throws JediException
	 *             Si il y a une erreur de suppression
	 */
    public static void removeNullValueFromList(JediAttributeList attributeList) throws JediException {
        NamingEnumeration<String> ne = attributeList.getIDs();
        String nameAttribute = null;
        try {
            while (ne.hasMoreElements()) {
                nameAttribute = ne.nextElement();
                try {
                    if (attributeList.get(nameAttribute).get() == null) {
                        attributeList.remove(nameAttribute);
                    }
                } catch (Exception e) {
                    attributeList.remove(nameAttribute);
                }
            }
        } catch (Exception ex) {
            JediLog.log(JediLog.LOG_TECHNICAL, JediLog.ERROR, "jedi_msg_internal_error", "", null);
            throw new JediException("JediUtil : removeNullValueFromList(JediAttributeList) : Erreur de suppression dans la liste des attributs");
        }
    }

    public static boolean endWithPath(String completeString, String endString) {
        if (completeString != null && endString != null) {
            return trimPath(completeString).toLowerCase().endsWith((trimPath(endString)).toLowerCase());
        } else {
            return false;
        }
    }

    private static String trimPath(String stringToConvert) {
        return stringToConvert.replaceAll(" ", "");
    }
}
