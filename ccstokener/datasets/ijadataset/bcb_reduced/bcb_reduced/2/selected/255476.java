package com.projetoptymo;

import java.net.*;
import java.io.*;
import java.util.regex.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import android.content.Context;

/**
 * Permet de r�soudre un itin�raire via internet
 * @version 0.1
 */
public class CalculItineraire {

    private int Depart;

    private int Arrivee;

    private Context context;

    private int Hour;

    private int Minute;

    private int Day;

    private int Month;

    private int Year;

    private Element itineraire;

    private NodeList etapes;

    /**
	 * Constructeur
	 * @param context context d'execution
	 * @param Depart num�ro de l'arret de d�part
	 * @param Arrivee num�ro de l'arret d'arriv�e
	 */
    public CalculItineraire(Context context, int Depart, int Arrivee, int Minute, int Hour, int Day, int Month, int Year) {
        this.context = context;
        this.Depart = Depart;
        this.Arrivee = Arrivee;
        this.Minute = Minute;
        this.Hour = Hour;
        this.Day = Day;
        this.Month = Month;
        this.Year = Year;
    }

    /**
	 * @param Depart d�finit le num�ro de l'arret de d�part
	 */
    public void setDepart(int Depart) {
        this.Depart = Depart;
    }

    /**
	 * @param Arrivee d�finit le num�ro de l'arret d'arriv�e
	 */
    public void setArrivee(int Arrivee) {
        this.Arrivee = Arrivee;
    }

    /**
	 * Lance le t�l�chargement des donn�es de r�sultat
	 * depuis le site optymo.fr
	 */
    public void Download() {
        try {
            URL url = new URL("http://www.optymo.fr/ws/calcul.aspx");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            String data = "arret_dep=";
            data += Depart;
            data += "&type_dep=arret";
            data += "&arret_arr=";
            data += Arrivee;
            data += "&type_arr=arret";
            data += "&heureDep=";
            data += Hour;
            data += "%3A";
            data += Minute;
            data += "&dateDep=";
            data += Day;
            data += "-";
            data += Month;
            data += "-";
            data += Year;
            data += "&hidden_arret_dep=";
            data += Depart;
            data += "&hidden_type_dep=arret";
            data += "&hidden_arret_arr=";
            data += Arrivee;
            data += "&hidden_type_arr=arret";
            data += "&envois_id_dep=aid_";
            data += Depart;
            data += "&envois_id_arr=aid_";
            data += Arrivee;
            data += "&envois_heure_dep=";
            data += (Hour * 60) + Minute;
            data += "&envois_date_dep=";
            data += Year;
            data += "-";
            data += Month;
            data += "-";
            data += Day;
            data += "&Submit=+";
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data + "\r\n\r\n");
            wr.flush();
            wr.close();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            Pattern p = Pattern.compile("DataXml'\\)\\.value='(.*)';");
            boolean done = false;
            while ((line = rd.readLine()) != null && done != true) {
                Matcher m = p.matcher(line);
                while (m.find()) {
                    FileOutputStream fichier = context.openFileOutput("tempiti.xml", 0);
                    OutputStreamWriter osw = new OutputStreamWriter(fichier);
                    osw.write("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
                    osw.write(m.group(1));
                    osw.close();
                    done = true;
                }
            }
            rd.close();
        } catch (Exception e) {
            System.out.println("Erreur : Impossible de t�l�charger les r�sultats de l'itin�raire.");
        }
    }

    /**
	 * Parse le r�sultat du t�l�chargement
	 */
    boolean ParseResult() {
        try {
            DocumentBuilderFactory fabrique = DocumentBuilderFactory.newInstance();
            DocumentBuilder constructeur = fabrique.newDocumentBuilder();
            FileInputStream fichier = context.openFileInput("tempiti.xml");
            Document document = constructeur.parse(fichier);
            Element racine = document.getDocumentElement();
            NodeList nl = racine.getElementsByTagName("iti");
            int max = nl.getLength();
            if (max == 0) {
                return false;
            }
            itineraire = (Element) nl.item(0);
            etapes = itineraire.getElementsByTagName("etape");
        } catch (ParserConfigurationException pce) {
            System.out.println("Erreur de configuration du parser DOM.");
        } catch (SAXException se) {
            System.out.println("Erreur lors du parsing du document");
        } catch (IOException ioe) {
            System.out.println("Erreur d'entr�e/sortie");
        }
        return true;
    }

    /**
	 * @return dur�e de l'itin�raire
	 */
    public String getItineraireDuree() {
        return itineraire.getAttribute("duree");
    }

    /**
	 * @return nombre d'�tapes
	 */
    public int getEtapeCount() {
        return etapes.getLength();
    }

    /**
	 * R�cuperer l'action d'une �tape (Monter/D�scendre)
	 * @param index num�ro de l'�tape
	 * @return action de l'�tape
	 */
    public String getEtapeActionAt(int index) {
        Element etape = (Element) etapes.item(index);
        return etape.getAttribute("action");
    }

    /**
	 * R�cuperer l'heure � laquelle une �tape se d�roule
	 * @param index num�ro de l'�tape
	 * @return heure de l'�tape
	 */
    public String getEtapeHeureAt(int index) {
        Element etape = (Element) etapes.item(index);
        return etape.getAttribute("heure");
    }

    /**
	 * R�cuperer le nom de l'arret d'une �tape
	 * @param index num�ro de l'�tape
	 * @return nom de l'arret
	 */
    public String getEtapeArretAt(int index) {
        Element etape = (Element) etapes.item(index);
        NodeList arret = etape.getElementsByTagName("arret");
        Node arretnode = arret.item(0);
        NodeList value = arretnode.getChildNodes();
        Node n = value.item(0);
        return n.getNodeValue();
    }

    /**
	 * R�cuperer le num�ro de la ligne d'une �tape
	 * @param index num�ro de l'�tape
	 * @return num�ro de ligne de l'�tape
	 */
    public int getEtapeLigneAt(int index) {
        Element etape = (Element) etapes.item(index);
        NodeList arret = etape.getElementsByTagName("ligne");
        Node arretnode = arret.item(0);
        NodeList value = arretnode.getChildNodes();
        Node n = value.item(0);
        String ligne = n.getNodeValue();
        return Integer.parseInt(Character.toString(ligne.charAt(1)));
    }
}
