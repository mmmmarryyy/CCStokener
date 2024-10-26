package edu.ucla.mbi.curator.actions.curator.ajax;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionForm;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import edu.ucla.mbi.curator.Constants;

/**
 * Created by IntelliJ IDEA.
 * User: jason
 * Date: Jun 7, 2006
 * Time: 11:30:16 AM
 */
public class ProxyEBIVocabHelp extends Action {

    public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        String termId = request.getParameter("termId");
        String[] ebiUrlParts = Constants.EBIVocabURLParts;
        String url = new StringBuilder(ebiUrlParts[0]).append(termId).append(ebiUrlParts[1]).toString();
        try {
            request.setAttribute("responseString", new URLReader().read(url));
            return new SendResponse().execute(mapping, form, request, response);
        } catch (IOException ioe) {
            return new SendErrorResponse().execute(mapping, form, request, response);
        }
    }
}

class URLReader {

    public String read(String url) throws IOException {
        URL myurl = new URL(url);
        BufferedReader in = new BufferedReader(new InputStreamReader(myurl.openStream()));
        StringBuffer sb = new StringBuffer();
        String inputLine;
        while ((inputLine = in.readLine()) != null) sb.append(inputLine);
        in.close();
        return sb.toString();
    }
}
