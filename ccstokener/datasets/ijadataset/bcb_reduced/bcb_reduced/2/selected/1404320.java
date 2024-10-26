package anima.info.rdf;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.URL;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import anima.dcc.LangString;
import anima.info.InfoDataNode;
import anima.info.InfoIOException;
import anima.info.InfoNode;
import anima.info.InfoNodeBranch;
import anima.info.InfoNodeExtended;
import anima.info.InfoResource;
import anima.info.InfoUnitFormated;
import anima.info.InfoUnitIOException;
import anima.info.InvalidChildInfoNode;
import anima.info.Namespaces;
import anima.info.xml.InfoUnitXMLData;
import com.hp.hpl.jena.mem.ModelMem;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.NodeIterator;
import com.hp.hpl.jena.rdf.model.RDFException;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

public class InfoUnitRDF extends InfoUnitFormated {

    public static final String STRUCTURE_RDF = InfoUnitRDF.class.getResource("info-rdf.xml").toString(), NAMESPACES_RDF = STRUCTURE_RDF, MODEL_RDF = InfoUnitRDFAnima.class.getResource("model-rdf-anima.xml").toString();

    protected static String spaces = "                                                  ";

    protected static InfoDataNode rdf;

    protected static String rdfResource, rdfParseType;

    protected static String standardRDFMain, standardRDFID;

    public InfoUnitRDF() {
        super();
    }

    public InfoUnitRDF(InfoNodeBranch infoRoot, Namespaces infoNamespaces) {
        super(infoRoot, infoNamespaces);
    }

    public InfoUnitRDF(URL rdfSource, boolean extended, boolean simplified) throws InfoIOException {
        super();
        parse(rdfSource, extended, simplified);
    }

    protected void init() {
        simplifyNamespacesSource = NAMESPACES_RDF;
    }

    public void parse(URL source, boolean extended, boolean simplified) throws InfoIOException {
        InfoUnitXMLData iur = new InfoUnitXMLData(STRUCTURE_RDF);
        rdf = iur.load("rdf");
        standardRDFMain = rdf.ft("main expanded");
        standardRDFID = rdf.ft("id expanded");
        infoNamespaces = new Namespaces(source.toString(), false);
        try {
            Reader rdfReader = new InputStreamReader(source.openStream(), "UTF-8");
            ModelMem rdfModel = new ModelMem();
            rdfModel.read(rdfReader, source.toString());
            infoRoot = buildTree(rdfModel, extended);
        } catch (RDFException error1) {
            throw new InfoUnitIOException(error1.getMessage());
        } catch (IOException error2) {
            throw new InfoUnitIOException(error2.getMessage());
        }
        if (simplified) simplify();
    }

    protected InfoNodeBranch buildTree(Model rdfModel, boolean extended) throws RDFException {
        Hashtable ho = new Hashtable();
        NodeIterator ni = rdfModel.listObjects();
        while (ni.hasNext()) {
            RDFNode node = ni.nextNode();
            ho.put(node.toString(), node);
        }
        Hashtable hr = new Hashtable();
        ResIterator ri = rdfModel.listSubjects();
        Vector resRootList = new Vector();
        Vector rootIdList = new Vector();
        while (ri.hasNext()) {
            Resource res = ri.nextResource();
            String resId = (res.isAnon()) ? res.getId().toString() : res.getURI();
            hr.put(resId, res);
            if (!ho.containsKey(resId)) {
                resRootList.addElement(res);
                rootIdList.addElement(resId);
            }
        }
        Resource resRoot = null;
        String rootId = null;
        InfoNodeBranch infoRoot = null;
        if (resRootList.size() > 0) {
            InfoNodeBranch infoList[] = new InfoNodeBranch[resRootList.size()];
            infoRoot = (!extended) ? new InfoNodeBranch(standardRDFMain, infoList) : new InfoNodeExtended(standardRDFMain, infoList, null);
            for (int i = 0; i < infoList.length; i++) {
                resRoot = (Resource) resRootList.get(i);
                rootId = (String) rootIdList.get(i);
                if (resRoot != null) {
                    Vector ilChildren = new Vector();
                    infoList[i] = createInfoNode(rootId, ilChildren, infoRoot, extended);
                    ilChildren.addElement(createInfoNode(standardRDFID, new InfoResource(rootId), infoList[i], extended));
                    parseResource(resRoot, infoList[i], extended, hr, new Hashtable());
                }
            }
        }
        return infoRoot;
    }

    protected void parseResource(Resource resMain, InfoNode infoMain, boolean extended, Hashtable hashResource, Hashtable tracedPath) throws RDFException {
        String resKey = (resMain.isAnon()) ? resMain.getId().toString() : resMain.getURI();
        tracedPath.put(resKey, resMain);
        Vector infov = (Vector) infoMain.getValue();
        StmtIterator si = resMain.listProperties();
        while (si.hasNext()) {
            Statement stm = si.nextStatement();
            Object obj = stm.getObject();
            String sobj = obj.toString();
            String pred = stm.getPredicate().getNameSpace() + stm.getPredicate().getLocalName();
            Resource res = (Resource) hashResource.get(sobj);
            InfoNodeBranch in;
            if (res != null && !tracedPath.containsKey(sobj)) {
                Vector inChildren = new Vector();
                in = createInfoNode(pred, inChildren, infoMain, extended);
                inChildren.addElement(createInfoNode(standardRDFID, new InfoResource(sobj), in, extended));
                parseResource(res, in, extended, hashResource, tracedPath);
            } else {
                Object content;
                if (obj instanceof Literal) {
                    Literal lit = (Literal) obj;
                    if (lit.getLanguage() != null && lit.getLanguage().length() > 0) content = new LangString(lit.getValue().toString(), lit.getLanguage()); else if (lit.getDatatypeURI() != null && lit.getDatatypeURI().endsWith("#anyURI")) content = new InfoResource(lit.getValue().toString()); else content = lit.getValue();
                } else content = new InfoResource(sobj);
                in = createInfoNode(pred, content, infoMain, extended);
            }
            infov.addElement(in);
        }
        tracedPath.remove(resKey);
        InfoNodeBranch infoArray[] = null;
        if (!extended) infoArray = InfoNodeBranch.objectInfoConvert(infov); else infoArray = InfoNodeExtended.objectInfoConvert(infov);
        infoMain.setValue(infoArray);
    }

    public void write(URL output) throws InfoUnitIOException {
        InfoUnitXMLData si = new InfoUnitXMLData(STRUCTURE_INFO);
        write(output, MODEL_RDF, null);
    }

    public void write(URL output, String model, String mainResourceClass) throws InfoUnitIOException {
        InfoUnitXMLData iur = new InfoUnitXMLData(STRUCTURE_RDF);
        rdf = iur.load("rdf");
        rdfResource = rdf.ft("resource");
        rdfParseType = rdf.ft("parse type");
        try {
            PrintWriter outw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(output.getFile()), "UTF-8"));
            URL urlModel = new URL(model);
            BufferedReader inr = new BufferedReader(new InputStreamReader(urlModel.openStream()));
            String finalTag = "</" + rdf.ft("main") + ">";
            String line = inr.readLine();
            while (line != null && !line.equalsIgnoreCase(finalTag)) {
                outw.println(line);
                line = inr.readLine();
            }
            inr.close();
            InfoNode nodeType = infoRoot.path(rdf.ft("constraint"));
            String type = null;
            if (nodeType != null) {
                type = nodeType.getValue().toString();
                try {
                    infoRoot.removeChildNode(nodeType);
                } catch (InvalidChildInfoNode error) {
                }
            } else if (mainResourceClass != null) type = mainResourceClass; else type = rdf.ft("description");
            outw.println("   <" + type + " " + rdf.ft("about") + "=\"" + ((infoNamespaces == null) ? infoRoot.getLabel() : infoNamespaces.convertEntity(infoRoot.getLabel().toString())) + "\">");
            Set<InfoNode> nl = infoRoot.getChildren();
            writeNodeList(nl, outw, 5);
            outw.println("   </" + type + ">");
            if (line != null) outw.println(finalTag);
            outw.close();
        } catch (IOException error) {
            throw new InfoUnitIOException(error.getMessage());
        }
    }

    protected void writeNodeList(Set<InfoNode> nl, PrintWriter outw, int nSpaces) {
        int i = 0;
        for (InfoNode info : nl) {
            if (info.isLiteral()) outw.println(spaces.substring(0, nSpaces) + "<" + info.getLabel() + ">" + info.getValue() + "</" + info.getLabel() + ">"); else if (info.isResourceReference()) outw.println(spaces.substring(0, nSpaces) + "<" + info.getLabel() + " " + rdfResource + "=\"" + ((infoNamespaces == null) ? info.getValue() : infoNamespaces.convertEntity(info.getValue().toString())) + "\"/>"); else {
                Set<InfoNode> li = info.getChildren();
                outw.println(spaces.substring(0, nSpaces) + "<" + info.getLabel() + " " + rdfParseType + "=\"Resource\">");
                writeNodeList(li, outw, nSpaces + 2);
                outw.println(spaces.substring(0, nSpaces) + "</" + info.getLabel() + ">");
            }
            i++;
        }
    }
}
