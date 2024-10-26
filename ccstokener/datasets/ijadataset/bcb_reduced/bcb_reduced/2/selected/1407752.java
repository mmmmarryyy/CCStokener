package ajaxservlet.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import saadadb.database.Database;
import saadadb.exceptions.QueryException;
import saadadb.query.parser.PositionParser;
import ajaxservlet.SaadaServlet;

/** * @version $Id: SimbadTooltip.java 367 2012-04-20 15:09:19Z laurent.mistahl $

 * Servlet implementation class SimbadTooltip
 */
public class SimbadTooltip extends SaadaServlet {

    private static final long serialVersionUID = 1L;

    /**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    /**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        process(request, response);
    }

    @SuppressWarnings("unchecked")
    private void process(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        printAccess(request, false);
        try {
            response.setContentType("application/json");
            String position = request.getParameter("pos");
            if (position == null || position.length() == 0) {
                reportJsonError(request, response, "Missing position parameter");
            } else {
                PositionParser pp;
                pp = new PositionParser(position);
                String url = "http://simbad.u-strasbg.fr/simbad/sim-script?submit=submit+script&script=";
                url += URLEncoder.encode("format object \"%IDLIST[%-30*]|-%COO(A)|%COO(D)|%OTYPELIST(S)\"\n" + pp.getPosition() + " radius=1m", "ISO-8859-1");
                System.out.println(url);
                URL simurl = new URL(url);
                BufferedReader in = new BufferedReader(new InputStreamReader(simurl.openStream()));
                String boeuf;
                boolean data_found = false;
                JSONObject retour = new JSONObject();
                JSONArray dataarray = new JSONArray();
                JSONArray colarray = new JSONArray();
                JSONObject jsloc = new JSONObject();
                jsloc.put("sTitle", "ID");
                colarray.add(jsloc);
                jsloc = new JSONObject();
                jsloc.put("sTitle", "Position");
                colarray.add(jsloc);
                jsloc = new JSONObject();
                jsloc.put("sTitle", "Type");
                colarray.add(jsloc);
                retour.put("aoColumns", colarray);
                int datasize = 0;
                while ((boeuf = in.readLine()) != null) {
                    if (data_found) {
                        String[] fields = boeuf.trim().split("\\|", -1);
                        int pos = fields.length - 1;
                        if (pos >= 3) {
                            String type = fields[pos];
                            pos--;
                            String dec = fields[pos];
                            pos--;
                            String ra = fields[pos];
                            String id = fields[0].split("\\s{2,}")[0].trim();
                            JSONArray darray = new JSONArray();
                            darray.add(id.trim());
                            darray.add(ra + " " + dec);
                            darray.add(type.trim());
                            dataarray.add(darray);
                            datasize++;
                            if (datasize >= 15) {
                                darray = new JSONArray();
                                darray.add("truncated to 15");
                                darray.add("");
                                darray.add("");
                                dataarray.add(darray);
                                datasize++;
                            }
                        }
                    } else if (boeuf.startsWith("::data")) {
                        data_found = true;
                    }
                }
                in.close();
                retour.put("aaData", dataarray);
                retour.put("iTotalRecords", datasize);
                retour.put("iTotalDisplayRecords", datasize);
                JsonUtils.teePrint(response.getOutputStream(), retour.toJSONString());
            }
        } catch (QueryException e) {
            reportJsonError(request, response, e);
        }
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        PositionParser pp;
        Database.init("XIDResult");
        pp = new PositionParser("01:33:50.904+30:39:35.79");
        String url = "http://simbad.u-strasbg.fr/simbad/sim-script?submit=submit+script&script=";
        String script = "format object \"%IDLIST[%-30*]|-%COO(A)|%COO(D)|%OTYPELIST(S)\"\n";
        String tmp = "";
        script += pp.getPosition() + " radius=1m";
        url += URLEncoder.encode(script, "ISO-8859-1");
        URL simurl = new URL(url);
        BufferedReader in = new BufferedReader(new InputStreamReader(simurl.openStream()));
        String boeuf;
        boolean data_found = false;
        JSONObject retour = new JSONObject();
        JSONArray dataarray = new JSONArray();
        JSONArray colarray = new JSONArray();
        JSONObject jsloc = new JSONObject();
        jsloc.put("sTitle", "ID");
        colarray.add(jsloc);
        jsloc = new JSONObject();
        jsloc.put("sTitle", "Position");
        colarray.add(jsloc);
        jsloc = new JSONObject();
        jsloc.put("sTitle", "Type");
        colarray.add(jsloc);
        retour.put("aoColumns", colarray);
        int datasize = 0;
        while ((boeuf = in.readLine()) != null) {
            if (data_found) {
                String[] fields = boeuf.trim().split("\\|", -1);
                int pos = fields.length - 1;
                if (pos >= 3) {
                    String type = fields[pos];
                    pos--;
                    String dec = fields[pos];
                    pos--;
                    String ra = fields[pos];
                    String id = "";
                    for (int i = 0; i < pos; i++) {
                        id += fields[i];
                        if (i < (pos - 1)) {
                            id += "|";
                        }
                    }
                    if (id.length() <= 30) {
                        JSONArray darray = new JSONArray();
                        darray.add(id.trim());
                        darray.add(ra + " " + dec);
                        darray.add(type.trim());
                        dataarray.add(darray);
                        datasize++;
                    }
                }
            } else if (boeuf.startsWith("::data")) {
                data_found = true;
            }
        }
        retour.put("aaData", dataarray);
        retour.put("iTotalRecords", datasize);
        retour.put("iTotalDisplayRecords", datasize);
        System.out.println(retour.toJSONString());
        in.close();
    }
}
