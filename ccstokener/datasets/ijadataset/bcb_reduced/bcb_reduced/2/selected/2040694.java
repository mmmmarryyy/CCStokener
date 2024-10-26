package se.kb.fedora.oreprovider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import se.kb.fedora.oreprovider.datasource.DataSource;
import se.kb.fedora.oreprovider.datasource.DataSourceFactory;
import se.kb.fedora.oreprovider.util.ResourceIndexServer;
import se.kb.oai.ore.AggregatedResource;
import se.kb.oai.ore.Aggregation;
import se.kb.oai.ore.ResourceMap;
import se.kb.oai.ore.ResourceMapFactory;
import se.kb.oai.ore.ResourceMapSerializer;
import se.kb.oai.ore.Type;
import se.kb.oai.ore.impl.AtomFactory;
import se.kb.oai.ore.impl.AtomSerializer;

public class ResourceMapServlet extends HttpServlet {

    private static final long serialVersionUID = -4948814508200162934L;

    private static final String TYPE_PREFIX = "type.";

    private static final String CREATOR_DEFAULT = "ORE Provider";

    private static final String RIGHTS_DEFAULT = "http://creativecommons.org/licenses/by-nc-sa/3.0/";

    private Properties properties;

    private String fedoraServer;

    private ResourceIndexServer resourceIndex;

    private ResourceMapFactory resourceMapFactory;

    private ResourceMapSerializer resourceMapSerializer;

    private DataSourceFactory dataSourceFactory;

    private boolean autocreation;

    private List<String> autocreationFilter;

    public void init() throws ServletException {
        try {
            this.properties = new Properties();
            properties.load(getServletContext().getResourceAsStream("/WEB-INF/classes/ore.properties"));
        } catch (IOException e) {
            throw new ServletException("The 'ore.properties' file can't be found or read properly!");
        }
        this.fedoraServer = properties.getProperty("fedora.server", "http://localhost:8080/fedora/");
        this.resourceIndex = new ResourceIndexServer(fedoraServer + "risearch");
        this.dataSourceFactory = new DataSourceFactory(fedoraServer);
        this.resourceMapFactory = new AtomFactory();
        this.resourceMapSerializer = new AtomSerializer();
        this.autocreation = Boolean.parseBoolean(properties.getProperty("rem.autocreation"));
        if (autocreation && properties.containsKey("rem.autocreation.filter")) {
            this.autocreationFilter = Arrays.asList(properties.getProperty("rem.autocreation.filter").split(" "));
        }
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PrintWriter writer = response.getWriter();
        String id = request.getPathInfo().replace("/", "");
        List<String> lines = autocreation ? executeListDatastreams(id) : executeRDFQuery(id);
        List<DataSource> sources = dataSourceFactory.createDataSources(lines);
        if (sources.size() > 0) {
            try {
                ResourceMap map = resourceMapFactory.newResourceMap("http://" + request.getServerName() + ":" + request.getServerPort() + request.getRequestURI());
                map.setModified(new Date());
                map.setCreator(properties.getProperty("rem.creator", CREATOR_DEFAULT));
                map.setRights(properties.getProperty("rem.rights", RIGHTS_DEFAULT));
                Aggregation aggregation = map.getAggregation();
                for (DataSource source : sources) {
                    if (autocreationFilter == null || autocreationFilter.contains(source.getName())) {
                        AggregatedResource resource = new AggregatedResource(source.getUrl());
                        resource.setMimeType(source.getMimeType());
                        if (properties.containsKey(TYPE_PREFIX + source.getName())) {
                            String[] types = properties.getProperty(TYPE_PREFIX + source.getName()).split(" ");
                            for (String type : types) resource.addType(new Type(type));
                        }
                        aggregation.addResource(resource);
                    }
                }
                String feed = resourceMapSerializer.serializeToString(map);
                response.setContentType("text/xml");
                writer.write(feed);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
        } else {
            response.setStatus(404);
            writer.write("<h4>No match for identifier: <i>" + id + "</i></h4>");
        }
        writer.close();
    }

    private List<String> executeRDFQuery(String id) throws IOException {
        String query = "select $id $res from <#ri> " + "where " + "$id <http://www.kb.se/dl/xml/ore#resourceMap> '" + id + "'" + "and " + "$id <http://www.kb.se/dl/xml/ore#resource> $res";
        return resourceIndex.query(query);
    }

    private List<String> executeListDatastreams(String id) throws IOException {
        List<String> list = new LinkedList<String>();
        URL url = new URL(fedoraServer + "listDatastreams/" + id + "?xml=true");
        InputStream in = url.openStream();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int read = in.read();
        while (read != -1) {
            out.write(read);
            read = in.read();
        }
        String string = out.toString();
        string = string.replaceAll(">", ">DELIMITER");
        for (String line : string.split("DELIMITER")) {
            if (!line.startsWith("<?xml")) list.add(line);
        }
        return list;
    }
}
