package edu.ucsd.ncmir.HubAgent;

import edu.ucsd.ncmir.WIBUtils.AbstractAgent;
import edu.ucsd.ncmir.WIBUtils.QueryString;
import edu.ucsd.ncmir.spl.minixml.Attribute;
import edu.ucsd.ncmir.spl.minixml.Document;
import edu.ucsd.ncmir.spl.minixml.Element;
import edu.ucsd.ncmir.spl.minixml.JDOMException;
import edu.ucsd.ncmir.spl.minixml.SAXBuilder;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 *
 * @author spl
 */
public class HubAgent extends AbstractAgent {

    private static final String LOG_PATH = "/var/tmp/hubagent.log";

    private static final String SERVER = "incf-dev-local.crbs.ucsd.edu";

    private static final String PORT = "80";

    private String _server = this.getenv("SERVER", HubAgent.SERVER);

    private String _port = this.getenv("PORT", HubAgent.PORT);

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            new HubAgent(LOG_PATH).process();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private HubAgent(String log_path) throws IOException {
        super(log_path);
    }

    private static final String CONFIG_XML = "/usr/local/share/incf/hubs.xml";

    private QueryString _qs = new QueryString();

    private Element _success = new Element("root");

    private Element _hubs;

    @Override
    protected Element runAgent(QueryString qs) throws Throwable {
        this._qs.print(System.err);
        this._success.setAttribute(new Attribute("status", "success"));
        String config_xml = this.getenv("CONFIG_XML", HubAgent.CONFIG_XML);
        Document config = new SAXBuilder().build(new File(config_xml));
        this._hubs = config.getRootElement();
        super.launch(qs.getString("action"));
        return this._success;
    }

    public void capabilities() throws Throwable {
        for (Element e : this._hubs.getChildren("Hub")) {
            Element hub = new Element("menuitem");
            hub.setAttribute(new Attribute("has_submenu", "true"));
            hub.setAttribute(new Attribute("action", "none"));
            String name = e.getAttribute("name").getValue();
            hub.setAttribute(new Attribute("name", name));
            for (Element t : e.getChildren("Term")) {
                Element term = new Element("menuitem");
                String tname = t.getAttribute("name").getValue();
                term.setAttribute(new Attribute("name", tname));
                term.setAttribute(new Attribute("has_submenu", "false"));
                term.setAttribute(new Attribute("action", "request"));
                hub.addContent(term);
            }
            this._success.addContent(hub);
        }
    }

    private Element findAttrElement(String qs_name, String conf_elem_name, Element e) throws Error {
        String childname = this._qs.getString(qs_name).replaceAll("%20", " ");
        Element child = null;
        for (Element h : e.getChildren(conf_elem_name)) if (h.getAttribute("name").getValue().equals(childname)) {
            child = h;
            break;
        }
        if (child == null) new Error("Unable to find " + conf_elem_name);
        return child;
    }

    public void request() throws Throwable {
        Element hub = this.findAttrElement("hub", "Hub", this._hubs);
        Element term = this.findAttrElement("term", "Term", hub);
        String from = term.getAttribute("from").getValue();
        String to = term.getAttribute("to").getValue();
        double x = this._qs.getDouble("x");
        double y = this._qs.getDouble("y");
        double z = this._qs.getDouble("z");
        if (!from.equals(to)) {
            double[] xyz = this.transform(from, to, x, y, z);
            x = xyz[0];
            y = xyz[1];
            z = xyz[2];
        }
        Element response = this.getXML(String.format(term.getAttribute("uri").getValue(), this._server, this._port, x, y, z));
        Element parse = term.getChild("Parse");
        if (response.descendTo("wps:ProcessFailed") != null) throw new Error("Request failed.");
        this.elementParser(parse, response);
    }

    private void elementParser(Element parse, Element data) {
        if (data == null) throw new Error("Error parsing at " + parse.getName());
        for (Element parser : parse.getChildren()) {
            String name = parser.getName();
            String ename = parser.getAttribute("name").getValue();
            if (name.equals("DescendTo")) this.elementParser(parser, data.descendTo(ename)); else {
                Attribute occ = parser.getAttribute("occurs");
                String occurs = occ == null ? "once" : occ.getValue();
                List<Element> elist = data.getChildren(ename);
                if (occurs.equals("once")) {
                    if (elist.size() > 1) new Error("Too many " + ename + " elements.");
                } else if (!occurs.equals("multiple") && new Integer(occurs).intValue() != elist.size()) new Error("Expecting " + occurs + " elements, found " + elist.size());
                for (Element e : elist) {
                    if (name.equals("Data")) {
                        String usage = parser.getAttribute("usage").getValue();
                        String value_type = parser.getAttribute("value").getValue();
                        String dval;
                        if (value_type.equals("content")) {
                            String text = e.getText();
                            if (text == null) text = "";
                            dval = text.trim();
                        } else dval = e.getAttribute(parser.getAttribute("attrname").getValue()).getValue();
                        Element data_elem = new Element("data");
                        data_elem.setAttribute(new Attribute("usage", usage));
                        if (usage.equals("display")) {
                            String label = parser.getAttribute("label").getValue();
                            data_elem.setAttribute(new Attribute("label", label));
                        }
                        data_elem.setText(dval);
                        this._success.addContent(data_elem);
                    } else if (!name.equals("Child")) new Error(name + " is an unexpected parsing element.");
                    this.elementParser(parser, e);
                }
            }
        }
    }

    private String getenv(String name, String default_value) {
        String s = System.getenv(name);
        if (s == null) s = default_value;
        return s;
    }

    private double[] transform(String from, String to, double x, double y, double z) throws Throwable {
        Element e = this._hubs.getChild("TransformationChain");
        Element root = this.getXML(String.format(e.getAttribute("uri").getValue(), this._server, this._port, from, to));
        Element chain = root.descendTo("CoordinateTransformationChain");
        for (Element c : chain.getChildren("CoordinateTransformation")) {
            String ct = c.getText();
            ct = this.insert(ct, "x=", x);
            ct = this.insert(ct, "y=", y);
            ct = this.insert(ct, "z=", z);
            Element transform = this.getXML(ct);
            Element pos = transform.descendTo("pos");
            String[] pvals = pos.getText().trim().split(" ");
            x = new Double(pvals[0]).doubleValue();
            y = new Double(pvals[1]).doubleValue();
            z = new Double(pvals[2]).doubleValue();
        }
        return new double[] { x, y, z };
    }

    private String insert(String s, String item, double v) {
        int l = s.lastIndexOf(item);
        String s1 = s.substring(0, l);
        String s2 = s.substring(l + item.length());
        return s1 + item + String.format("%g", v) + s2;
    }

    private Element getXML(String url_string) throws MalformedURLException, IOException, JDOMException {
        URL url = new URL(url_string.trim().replaceAll("&amp;", "&"));
        System.err.println(url.toString());
        InputStream is = url.openStream();
        Element e = new SAXBuilder().build(is).getRootElement();
        is.close();
        return e;
    }
}
