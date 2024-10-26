package fortunata.fswapps.vpoet;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntClass;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.util.FileManager;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;
import java.io.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.util.ArrayList;
import fortunata.fswapps.vpoet.Vpoet;
import fortunata.fswapps.vpoet.VPOETRequestData;
import fortunata.fswapps.omemo.Omemo;
import fortunata.servlets.VPoetRequestServlet;
import fortunata.fswapps.vpoet.PropertyValueGetter;
import org.apache.log4j.Logger;

/**
 * Renders visualizations using VPOET. This class is used by VPOET servlet
 */
public class VPOETVisualizationRenderer {

    private static Logger log = Logger.getLogger(VPOETVisualizationRenderer.class);

    private static int uniqueID = 0;

    private String ontofilesDir = null;

    private OntModel modelVPOET = null;

    private OntModel modelOMEMO = null;

    private OntModel modelFromSource = null;

    private Individual defaultClassIndividual = null;

    private VPOETRequestData vpoetReqData = null;

    private Vpoet vpoet;

    private Omemo omemo;

    public VPOETVisualizationRenderer(String ontofilesDir) {
        this.ontofilesDir = ontofilesDir;
        modelVPOET = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF, null);
        vpoet = new Vpoet();
        vpoet.readSWAppModel(modelVPOET, this.ontofilesDir + vpoet.getOntoFileName(), this.ontofilesDir + vpoet.getIndividualsFileName());
        modelOMEMO = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM_RDFS_INF, null);
        omemo = new Omemo();
        omemo.readSWAppModel(modelOMEMO, this.ontofilesDir + omemo.getOntoFileName(), this.ontofilesDir + omemo.getIndividualsFileName());
    }

    /**
     * Looks for a visualization design for a given VPOETRequestData (i.e.: ontoElem, outFormat, and provider...)
     * Source is required only por valDesignType=output in GET requests.  If many designs are available returns any of them
     * If valDesignID != null assumes that a concrete visualization (designID+designerID) is required.
     * @param out  the servlet outputstream
     * @param rpv the values of the request.
     */
    public void render(PrintWriter out, final VPOETRequestData rpv) {
        vpoetReqData = rpv;
        if (rpv.valOutFormat != null && !rpv.valOutFormat.equals("HTML")) {
            out.print("Sorry, parameter " + rpv.paramNameOutFormat + " has a value (" + rpv.valOutFormat + ") not supported yet.");
            return;
        }
        if (rpv.valSource != null && !canReadSource(rpv.valSource)) {
            out.print("Sorry, parameter " + rpv.paramNameSource + " with value '" + rpv.valSource + "' is not available. Check the URL.");
            return;
        }
        String indivDesignURI = null;
        if (rpv.valDesignID != null) {
            indivDesignURI = vpoet.queryModelForConcreteDesign(modelVPOET, rpv.valProvider, rpv.valDesignID);
            if (indivDesignURI == null) {
                out.print("Sorry, there is no design for those designID+designerID ('" + rpv.valDesignID + "' / '" + rpv.valProvider + "')");
                return;
            }
        } else {
            String designType = (rpv.valAction.equals(rpv.paramValueRenderInput)) ? "input" : "output";
            indivDesignURI = vpoet.queryModel(modelVPOET, rpv.valOntoElemID, rpv.valProvider, designType);
            if (indivDesignURI == null) {
                out.print("Sorry, there is no design for those designType+ontoElem+provider ('" + designType + "' / '" + rpv.valOntoElemID + "' / ' " + rpv.valProvider + "')");
                return;
            }
        }
        out.print(renderDesign(indivDesignURI, rpv.valSource, rpv.valIndividualID, rpv));
    }

    /**
     *   Renders a given design using a given data source. A concrete individual can be specified. In that
     * case, only renders that individual. If no individual is specified, renders all the individuals that
     * match the design's ontoElem. In this case, each individual will have a link to the right request.
     * @param designURI The design knows which ontoElem can render
     * @param source  The URL of the data source (typically a rdf file)
     * @param classIndividualID  The identifier of a concrete instance in the data source. Null if no instance is specified
     * @param rpv The request data. Used only when rendering individuals, that is, when classIndividualID is null.
     * @return the html code
     */
    private String renderDesign(String designURI, String source, String classIndividualID, VPOETRequestData rpv) {
        Individual indivDesign = modelVPOET.getIndividual(designURI);
        String renderizingOntoElemID = indivDesign.getPropertyValue(modelVPOET.getProperty(vpoet.getURI() + Vpoet.propNameOntoElemID)).toString();
        String omemoURI = omemo.getURI();
        String vpoetURI = vpoet.getURI();
        if (modelFromSource == null) {
            String urlInstancesFile = source;
            String defaultOntologyID = renderizingOntoElemID.substring(0, renderizingOntoElemID.lastIndexOf("."));
            String alias = defaultOntologyID.substring(0, defaultOntologyID.indexOf("."));
            String version = defaultOntologyID.substring(1 + defaultOntologyID.indexOf("."));
            Individual externalOntologyIndv = modelOMEMO.getIndividual(omemo.getInstanceUniqueURI(alias, version));
            String urlDefsFile = externalOntologyIndv.getPropertyValue(modelOMEMO.getProperty(omemoURI + Omemo.propNameUrl)).toString();
            String ontoLang = externalOntologyIndv.getPropertyValue(modelOMEMO.getProperty(omemoURI + Omemo.propNameOntoLang)).toString();
            OntModelSpec modSpec = null;
            if (ontoLang.equals("OWL")) {
                modSpec = OntModelSpec.OWL_MEM_RDFS_INF;
            } else {
                if (ontoLang.equals("RDFS")) {
                    modSpec = OntModelSpec.RDFS_MEM_RDFS_INF;
                } else {
                }
            }
            modelFromSource = ModelFactory.createOntologyModel(modSpec, null);
            Model desfsModel = FileManager.get().loadModel(urlDefsFile);
            modelFromSource.add(desfsModel);
            Model individualsModel = FileManager.get().loadModel(urlInstancesFile);
            modelFromSource.add(individualsModel);
        }
        String defaultOntologyID = renderizingOntoElemID.substring(0, renderizingOntoElemID.lastIndexOf("."));
        String alias = defaultOntologyID.substring(0, defaultOntologyID.indexOf("."));
        String version = defaultOntologyID.substring(1 + defaultOntologyID.indexOf("."));
        Individual externalOntologyIndv = modelOMEMO.getIndividual(omemo.getInstanceUniqueURI(alias, version));
        String term = renderizingOntoElemID.substring(1 + renderizingOntoElemID.lastIndexOf("."));
        StringBuffer sb = new StringBuffer();
        if (Character.isLowerCase(term.charAt(0)) == false) {
            String ontoNS = externalOntologyIndv.getPropertyValue(modelOMEMO.getProperty(omemoURI + Omemo.propNameNS)).toString();
            OntClass oc = modelFromSource.getOntClass(ontoNS + term);
            ExtendedIterator iter = modelFromSource.listIndividuals(oc);
            sb.append("<HTML>\n");
            sb.append("<HEAD>\n");
            sb.append(indivDesign.getPropertyValue(modelVPOET.getProperty(vpoetURI + Vpoet.propNameCSSCode)).toString());
            sb.append("\n");
            sb.append(indivDesign.getPropertyValue(modelVPOET.getProperty(vpoetURI + Vpoet.propNameJSCode)).toString());
            sb.append("\n");
            sb.append("</HEAD>\n");
            sb.append("<BODY>\n");
            if (classIndividualID != null) {
                Individual indv = modelFromSource.getIndividual(classIndividualID);
                if (indv == null) {
                    return "Individual with ID " + classIndividualID + " does not exist in source " + source;
                } else {
                    defaultClassIndividual = indv;
                }
                sb.append(renderInstance(indivDesign, source, renderizingOntoElemID));
            } else {
                sb.append("<B>Number of individuals found: " + getSizeOfIterator(modelFromSource.listIndividuals(oc)) + "</B><BR>\n");
                String encodedID = null;
                String encodedSource = null;
                while (iter.hasNext()) {
                    defaultClassIndividual = (Individual) iter.next();
                    if (defaultClassIndividual.isAnon() == false) {
                        try {
                            encodedID = URLEncoder.encode(defaultClassIndividual.toString(), "UTF-8");
                            sb.append("ID = " + defaultClassIndividual.toString() + " (<A href=\"" + rpv.getRequestForIndividual(encodedID) + "\">" + encodedID + "</A>)<BR>\n");
                            encodedSource = rpv.getRequestForSource(encodedID);
                            sb.append("Render <A href=\"" + encodedSource + "\">source</A><BR>\n");
                        } catch (Exception e) {
                            sb.append("Error in URI of individual " + defaultClassIndividual.toString() + "<BR>");
                        }
                    } else {
                        sb.append("ID = (Anonymous) <BR>\n");
                    }
                    sb.append(renderInstance(indivDesign, source, renderizingOntoElemID));
                    sb.append("<BR>\n");
                }
            }
            sb.append("</BODY>\n");
            sb.append("</HTML>\n");
        }
        return sb.toString();
    }

    private int getSizeOfIterator(ExtendedIterator iter) {
        int num = 0;
        while (iter.hasNext()) {
            iter.next();
            num++;
        }
        return num;
    }

    private String renderInstance(Individual indivDesign, String source, String renderizingOntoElemID) {
        StringBuffer sb = new StringBuffer();
        String htmlCode = null;
        try {
            htmlCode = translateCode(indivDesign.getPropertyValue(modelVPOET.getProperty(vpoet.getURI() + Vpoet.propNameHTMLCode)).toString(), source, renderizingOntoElemID);
            sb.append(htmlCode);
        } catch (Exception e) {
            sb.append(e.getMessage());
        }
        return sb.toString();
    }

    /**
     * Checks if the specified file is accesible through Internet
     * @param fileURL
     * @return
     */
    private boolean canReadSource(String fileURL) {
        URL url;
        try {
            url = new URL(fileURL);
        } catch (MalformedURLException e) {
            log.error("Error accessing URL " + fileURL + ".");
            return false;
        }
        InputStream is;
        try {
            is = url.openStream();
        } catch (IOException e) {
            log.error("Error creating Input Stream from URL '" + fileURL + "'.");
            return false;
        }
        return true;
    }

    private String renderizeValues(ArrayList values, String pre, String post, String sep) {
        StringBuffer sb = new StringBuffer();
        int len = values.size();
        for (int i = 0; i < len; i++) {
            sb.append(pre);
            sb.append(values.get(i));
            sb.append(post);
            if (i < len - 1) {
                sb.append(sep);
            }
        }
        return sb.toString();
    }

    /**
     * Translate the string. Translation table:
     * if ontoElem is a property (starts with NO capital letter) -> Use OmemoGetP() => propName = ontoElem
     * if ontoElem is a Class (starts with capital letter) -> Use OmemoGetP(propName) -> propName
     * @param strIn   initial string
     * @param source  the url of the instances file
     * @param ontoElemID  This is the ontoElem that is been renderized (class or prop)
     * Ej: foaf.20050403.firstName (property) or foaf.20050403.Person (Class)
     * @return the initial string translating macros
     */
    private String translateCode(final String strIn, final String source, final String ontoElemID) {
        StringBuffer sb = new StringBuffer();
        String stopWord = "OmemoGetP(";
        int i = strIn.indexOf(stopWord);
        int afterOpen;
        int upToClose;
        int commaPos;
        int processedIdx = 0;
        String rest = null;
        if (i != -1) {
            sb.append(strIn.substring(0, i));
            afterOpen = i + stopWord.length();
            upToClose = strIn.indexOf(")", afterOpen);
            String inner = strIn.substring(afterOpen, upToClose);
            String[] parts = inner.split(",");
            ArrayList values = null;
            try {
                switch(parts.length) {
                    case 3:
                        values = getPropertyValues(source, ontoElemID, null);
                        sb.append(renderizeValues(values, parts[0].trim(), parts[1].trim(), parts[2].trim()));
                        break;
                    case 5:
                        values = getAbrevPropertyValue(source, parts[0], parts[1], ontoElemID);
                        sb.append(renderizeValues(values, parts[2].trim(), parts[3].trim(), parts[4].trim()));
                        break;
                    case 4:
                        values = getAbrevPropertyValue(source, parts[0].trim(), null, ontoElemID);
                        sb.append(renderizeValues(values, parts[1].trim(), parts[2].trim(), parts[3].trim()));
                        break;
                    default:
                        sb.append("Error in OmemoGetP.... Invalid number of arguments:" + parts.length);
                }
            } catch (Exception e) {
                sb.append("Error getting values in OmemoGetP.... Processing " + ontoElemID + " in " + inner);
            }
            processedIdx = upToClose;
            rest = translateCode(new String(strIn.substring(processedIdx + 1)), source, ontoElemID);
            sb.append(rest);
        } else {
            sb.append(strIn);
        }
        return translateCodePhase2(sb.toString(), source, ontoElemID);
    }

    /**
     * Parse macros like these: OmemoGetLink(knows)
     * @param strIn
     * @param source
     * @param ontoElemID  This is the ontoElem that is been renderized (class or prop)
     * Ej: foaf.20050403.firstName or foaf.20050403.Person
     * @return  The visualization for the ontoElem+designed specified
     */
    private String translateCodePhase4(String strIn, String source, String ontoElemID) {
        StringBuffer sb = new StringBuffer();
        String stopWord = "OmemoGetLink(";
        int i = strIn.indexOf(stopWord);
        int afterOpen;
        int upToClose;
        int processedIdx = 0;
        String rest = null;
        if (i != -1) {
            sb.append(strIn.substring(0, i));
            afterOpen = i + stopWord.length();
            upToClose = strIn.indexOf(")", afterOpen);
            String propName = strIn.substring(afterOpen, upToClose);
            String relationDestinationIndStr = getAbrevPropertyIndvStr(source, propName, ontoElemID);
            sb.append(vpoetReqData.getRequestForRelation(source, ontoElemID, relationDestinationIndStr));
            processedIdx = upToClose;
            rest = translateCodePhase4(new String(strIn.substring(processedIdx + 1)), source, ontoElemID);
            sb.append(rest);
        } else {
            sb.append(strIn);
        }
        return translateCodePhase5(sb.toString(), source, ontoElemID);
    }

    /**
     * Parse macros like these: OmemoGetUniqueID
     * @param strIn
     * @param source
     * @param ontoElemID  This is the ontoElem that is been renderized (class or prop)
     * Ej: foaf.20050403.firstName or foaf.20050403.Person
     * @return  The visualization for the ontoElem+designed specified
     */
    private String translateCodePhase5(String strIn, String source, String ontoElemID) {
        StringBuffer sb = new StringBuffer();
        String stopWord = "OmemoGetUniqueID";
        int i = strIn.indexOf(stopWord);
        String rest = null;
        if (i != -1) {
            sb.append(strIn.substring(0, i));
            sb.append(uniqueID++);
            rest = translateCodePhase5(new String(strIn.substring(i + stopWord.length())), source, ontoElemID);
            sb.append(rest);
        } else {
            sb.append(strIn);
        }
        return sb.toString();
    }

    /**
     * Parse macros like these: OmemoBaseURL
     * @param strIn
     * @param source
     * @param ontoElemID  This is the ontoElem that is been renderized (class or prop)
     * Ej: foaf.20050403.firstName or foaf.20050403.Person
     * @return  The visualization for the ontoElem+designed specified
     */
    private String translateCodePhase3(String strIn, String source, String ontoElemID) {
        StringBuffer sb = new StringBuffer();
        String stopWord = "OmemoBaseURL";
        int i = strIn.indexOf(stopWord);
        String rest = null;
        if (i != -1) {
            sb.append(strIn.substring(0, i));
            sb.append(VPoetRequestServlet.getFortunataBaseURL());
            rest = translateCodePhase3(new String(strIn.substring(i + stopWord.length())), source, ontoElemID);
            sb.append(rest);
        } else {
            sb.append(strIn);
        }
        return translateCodePhase4(sb.toString(), source, ontoElemID);
    }

    /**
     * Parse macros like these: OmemoConditionalVizFor(firstName, mra68)
     *                          OmemoConditionalVizFor(firstName, mra68, designName)
     * @param strIn
     * @param source
     * @param ontoElemID  This is the ontoElem that is been renderized (class or prop)
     * Ej: foaf.20050403.firstName or foaf.20050403.Person
     * @return  The visualization for the ontoElem+designed specified
     */
    private String translateCodePhase2(String strIn, String source, String ontoElemID) {
        StringBuffer sb = new StringBuffer();
        String stopWord = "OmemoConditionalVizFor(";
        int i = strIn.indexOf(stopWord);
        int afterOpen;
        int upToClose;
        int processedIdx = 0;
        String rest = null;
        if (i != -1) {
            sb.append(strIn.substring(0, i));
            afterOpen = i + stopWord.length();
            upToClose = strIn.indexOf(")", afterOpen);
            String inner = strIn.substring(afterOpen, upToClose);
            String[] parts = inner.split(",");
            try {
                switch(parts.length) {
                    case 2:
                        sb.append(getConditionalVizForProperty(parts[0].trim(), parts[1].trim(), null, source, ontoElemID));
                        break;
                    case 3:
                        sb.append(getConditionalVizForProperty(parts[0].trim(), parts[1].trim(), parts[2].trim(), source, ontoElemID));
                        break;
                    default:
                        sb.append("Error in OmemoConditionalVizFor.... Invalid number of arguments: " + parts.length);
                        break;
                }
            } catch (Exception e) {
                sb.append("Error getting values in OmemoConditionalVizFor... Processing " + ontoElemID + " in " + inner);
            }
            processedIdx = upToClose;
            rest = translateCodePhase2(new String(strIn.substring(processedIdx + 1)), source, ontoElemID);
            sb.append(rest);
        } else {
            sb.append(strIn);
        }
        return translateCodePhase3(sb.toString(), source, ontoElemID);
    }

    /**
     * If exists a design for the specified ontoElem+provider, and ontoElem has a value in source, then render
     * the value using that design. 
     * @param propName Abreviated form of property. Ej: firsName
     * @param providerID
     * @param designID This can be null. In that case, any valid design is used. If designID is not null, this design is used
     * @param source
     * @parem ontoElemID The ontoElem that is been rendered (class or prop). Ej: foaf.20050603.Person, or foaf.20050603.name
     * @return the HTML code
     * @throws Exception exception with the right error message
     */
    private String getConditionalVizForProperty(String propName, String providerID, String designID, String source, String ontoElemID) throws Exception {
        PropertyValueGetter pvg = new PropertyValueGetter(propName);
        String[] value = null;
        if (!pvg.hasPropertyValue(modelFromSource, defaultClassIndividual)) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        String fullyQualifiedPropName = abrev2FullPropName(propName, ontoElemID);
        Individual indivDesign = null;
        String indivDesignURI = null;
        if (designID == null) {
            indivDesignURI = vpoet.queryModel(modelVPOET, fullyQualifiedPropName, providerID, "output");
        } else {
            indivDesignURI = vpoet.queryModelForConcreteDesign(modelVPOET, providerID, designID);
        }
        if (indivDesignURI != null) {
            indivDesign = modelVPOET.getIndividual(indivDesignURI);
            String htmlCode = indivDesign.getPropertyValue(modelVPOET.getProperty(vpoet.getURI() + Vpoet.propNameHTMLCode)).toString();
            sb.append(translateCode(htmlCode, source, fullyQualifiedPropName));
        } else {
        }
        return sb.toString();
    }

    /**
     * Converts from shor property name to full name
     * @param propName this is a abreviated form of the full ontoElemID. E.g.: firstName
     * @param ontoElemID  The ontoElem that is been renderized (class or property). Fully qualified, e.g. foaf.20050403.Person
     * @return  In this example returns foaf.20050403.firstName
     */
    private static String abrev2FullPropName(String propName, String ontoElemID) {
        String ontoID = ontoElemID.substring(0, ontoElemID.lastIndexOf("."));
        return ontoID + "." + propName;
    }

    /**
     * Get the property value when the ontElemID is a property abreviated
     * @param source
     * @param propName this is a abreviated form of the full ontoElemID. E.g.: firstName
     * @param prefs
     * @param ontoElemID  The ontoElem that is been renderized (class or property). Fully qualified, e.g. foaf.20050403.Person
     * @return
     */
    private ArrayList getAbrevPropertyValue(String source, String propName, String prefs, String ontoElemID) throws Exception {
        String propFullName = abrev2FullPropName(propName, ontoElemID);
        return getPropertyValues(source, propFullName, prefs);
    }

    /**
     * When property is a relation, this method return the individual destination of the relation
     * @param source
     * @param propName this is a abreviated form of the full ontoElemID. E.g.: firstName
     * @param ontoElemID  The ontoElem that is been renderized (class or property). Fully qualified, e.g. foaf.20050403.Person
     * @return
     */
    private String getAbrevPropertyIndvStr(String source, String propName, String ontoElemID) {
        String propFullName = abrev2FullPropName(propName, ontoElemID);
        return "http://www.w3.org/People/Berners-Lee/card#i";
    }

    /**
     *  Gets the values of the given ontoElemID. Checks if the ontoElemID has the right format to be a property.
     * @param source
     * @param ontoElemID  Example: foaf.20050403.firstName. No abbreviated forms are allowed (ej: first)
     * @param prefs Only when propName is a relation. In this case, pref is a list on properties sorted
     * by preference. In any other case is null.
     * @return  The property values
     * @throws Exception with the right message in case of error.
     */
    private ArrayList getPropertyValues(String source, String ontoElemID, String prefs) throws Exception {
        String propName = null;
        String lastTerm = ontoElemID.substring(1 + ontoElemID.lastIndexOf("."));
        if (Character.isLowerCase(lastTerm.charAt(0))) {
            propName = lastTerm;
        } else {
            throw new Exception("properties names begin lower case. Error in " + ontoElemID);
        }
        ArrayList val = null;
        if (propName != null) {
            try {
                PropertyValueGetter pvg = null;
                if (prefs == null) {
                    pvg = new PropertyValueGetter(propName);
                } else {
                    String[] prefsList = new String[1];
                    prefsList[0] = prefs;
                    pvg = new PropertyValueGetter(propName, prefsList);
                }
                val = pvg.getPropertyValues(modelFromSource, defaultClassIndividual);
            } catch (Exception e) {
                throw new Exception(e);
            }
        }
        return val;
    }

    public static void main(String[] args) throws Exception {
        testOmemoBasic();
    }

    private static void testOmemoRender() throws Exception {
        String ontoFilesDir = "C:\\Archivos de programa\\Apache Software Foundation\\Tomcat 5.5\\webapps\\fortunata\\";
        VPOETVisualizationRenderer srv = new VPOETVisualizationRenderer(ontoFilesDir);
        String urlStr1 = "http://ishtar.ii.uam.es/fortunata/servlet/VPoetRequestServlet?" + "action=renderOutput" + "&designID=FOAFOutputConditionalGraphics" + "&provider=mra68" + "&source=http://www.w3.org/People/Berners-Lee/card";
        VPOETRequestData vrd = new VPOETRequestData(urlStr1);
        vrd.valAction = vrd.paramValueRenderOutput;
        vrd.valOutFormat = "HTML";
        vrd.valProvider = "mra68";
        vrd.valDesignID = "SimpleFOAFOutputConditional";
        vrd.valSource = "http://www.w3.org/People/Berners-Lee/card";
        PrintWriter pwso = new PrintWriter(System.out);
        srv.render(pwso, vrd);
    }

    private static void testOmemoRenderDERI() throws Exception {
        String ontoFilesDir = "C:\\Archivos de programa\\Apache Software Foundation\\Tomcat 5.5\\webapps\\fortunata\\";
        FileOutputStream fos = new FileOutputStream("DERI.tmp");
        PrintWriter pwso = new PrintWriter(fos);
        for (int i = 120; i < 123; i++) {
            VPOETVisualizationRenderer srv = new VPOETVisualizationRenderer(ontoFilesDir);
            String idvEnc = URLEncoder.encode("http://www.deri.ie/fileadmin/scripts/foaf.php?id=" + i + "#me", "UTF-8");
            String urlStr1 = "http://ishtar.ii.uam.es/fortunata/servlet/VPoetRequestServlet?" + "action=renderOutput" + "&designID=FOAFOutputConditionalGraphics" + "&provider=mra68" + "&source=http://www.deri.ie/fileadmin/scripts/foaf.php?id=" + i + "&indvID=" + idvEnc;
            VPOETRequestData vrd = new VPOETRequestData(urlStr1);
            vrd.valAction = vrd.paramValueRenderOutput;
            vrd.valOutFormat = "HTML";
            vrd.valProvider = "mra68";
            vrd.valDesignID = "FOAFOutputConditionalGraphics";
            vrd.valSource = "http://www.deri.ie/fileadmin/scripts/foaf.php?id=" + i;
            vrd.valIndividualID = URLDecoder.decode(idvEnc, "UTF-8");
            System.out.println("Processing URL=" + urlStr1);
            try {
                srv.render(pwso, vrd);
            } catch (Exception e) {
                pwso.print("ERROR rendering URL = " + urlStr1 + "\n. The process continues.");
            }
            pwso.flush();
        }
    }

    private static void testOmemoBasic() throws Exception {
        String ontoFilesDir = "C:\\Archivos de programa\\Apache Software Foundation\\Tomcat 5.5\\webapps\\fortunata\\";
        VPOETVisualizationRenderer srv = new VPOETVisualizationRenderer(ontoFilesDir);
        String source = "file:///C:/Documents%20and%20Settings/Mariano%20Rico/Escritorio/mifoaf.rdf";
        String testConditional = srv.translateCode(" lirili OmemoGetP() otra OmemoGetP(givenname) y una OmemoGetP(knows, name) OmemoGetUniqueID y otro OmemoGetUniqueID", source, "foaf.20050403.Person");
        System.out.println(testConditional);
    }
}
