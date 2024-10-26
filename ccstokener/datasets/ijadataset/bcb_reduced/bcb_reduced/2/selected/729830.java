package de.gmd.first.hospitalgenerator;

import java.io.IOException;
import java.io.FileReader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import java.util.Hashtable;
import org.apache.xerces.parsers.DOMParser;
import org.xml.sax.InputSource;
import java.util.Properties;
import java.util.GregorianCalendar;
import java.util.Date;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Vector;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import java.io.FileWriter;
import org.xml.sax.SAXException;

/**
 *
 * Title:        Hospital Scenario Generator 1.1 <p/>
 * Description:  The HSG generates for a given specification a timetabling-problem,
 *               subject to hospital-resources and patients. <p/>
 * Copyright:    (c) 2001 <a target="_top" href="http://www.first.gmd.de">GMD FIRST</a> <p/>
 *
 * This class is needed to create an hospital-scenario.
 * The GMD Report 133. German National Research Center for Information Technology, 2001 explains the modell.<p/>
 *
 * <p/>Requirements:<p/>
 * The  XML-Parser <a target="_top" href="http://www.apache.org">Xerces</a> must be found in the classpath.
 *
 * @author       Frank Rehberger
 * @author       Markus Hannebauer
 * @version      1.1
 *
 */
public class HospitalScenarioGenerator {

    /** creating a factory object
    */
    public HospitalScenarioGenerator() {
    }

    /**  creates a new hospital-scenario.
    *
    *   The structure of the generated hospital can be specified by an xml-file.
    *
    *   @param urlString points to a xml-config-file
    *   @return a HospitalScenario representing the generated data.
    */
    public HospitalScenario getHospitalScenario(String urlString) throws GeneratorException {
        return createHospitalScenario(urlString);
    }

    /** factory call
    *  @param urlString points to a xml-config-file
    *  @return a HospitalScenario representing the generated data.
    */
    public static HospitalScenario createHospitalScenario(String urlString) throws GeneratorException {
        Document doc = null;
        java.io.InputStream stream = null;
        try {
            URL url = new URL(urlString);
            stream = url.openStream();
        } catch (MalformedURLException e) {
            throw new GeneratorException("MalformedURLException: " + e.getMessage());
        } catch (IOException e) {
            throw new GeneratorException("IOException while opening: " + e.getMessage());
        }
        try {
            InputSource source = new InputSource(stream);
            DOMParser parser = new DOMParser();
            parser.parse(source);
            doc = parser.getDocument();
        } catch (SAXException e) {
            throw new GeneratorException("SAXException: " + e.getMessage());
        } catch (IOException e) {
            throw new GeneratorException("IOException: while instanciating DOM " + e.getMessage());
        }
        HospitalScenarioType hospitalType = new HospitalScenarioType(doc, doc.getDocumentElement());
        return (HospitalScenario) hospitalType.getInstance();
    }

    /**
    * 
    */
    private static void write() {
        HospitalScenarioGenerator gen = new HospitalScenarioGenerator();
        try {
            FileWriter writer = new FileWriter("c:/Temp/scenario.xml");
            HospitalScenario scenario = gen.getHospitalScenario("file:C:/Temp/HSG/data/config.xml");
            writer.write(scenario.toString());
            writer.flush();
            writer.close();
            writer = new FileWriter("c:/temp/patient_stream.xml");
            writer.write("<patient_stream>");
            PatientStream stream = scenario.getPatientStream();
            while (stream.hasMorePatients()) {
                Patient p = stream.nextPatient();
                writer.write(p.toString());
            }
            writer.write("</patient_stream>");
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
            int i = 0;
        }
    }

    public static void main(String[] args) {
        write();
    }
}
