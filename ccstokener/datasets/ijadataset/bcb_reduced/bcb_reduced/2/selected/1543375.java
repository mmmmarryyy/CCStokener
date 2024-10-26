package webelements.templatemaker;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import webelements.datastructure.ArrayPositioner;
import webelements.datastructure.DefaultDataFieldResolver;
import webelements.datastructure.DefaultParserObjectWrapper;
import webelements.datastructure.FieldDoesNotHaveDimensionException;
import webelements.datastructure.FieldIndexOutOfBoundsException;
import webelements.datastructure.InvalidElementException;

/**
 * This DataFieldResolver differs from  parser.DefaultDatafieldResover  in the implementation of
 * the .serialized property.
 * And in the implemntation of TemplateModelRoot
 */
public class WebDataFieldResolver extends DefaultDataFieldResolver {

    HttpSession httpSession;

    HttpServletRequest servReq;

    public WebDataFieldResolver(Dictionary fields, HttpServletRequest servReq) {
        super(fields);
        this.httpSession = servReq.getSession(true);
        this.servReq = servReq;
    }

    private String getUrlContent(String address) throws MalformedURLException, IOException {
        URL url;
        Reader urlIn;
        if (address.startsWith("http:")) {
            url = new URL(address);
            urlIn = new InputStreamReader(url.openStream());
        } else {
            url = new URL(new URL(servReq.getScheme(), servReq.getServerName(), servReq.getServerPort(), servReq.getPathInfo()), address);
            System.out.println("Including: " + url.toString());
            urlIn = new InputStreamReader(url.openStream());
        }
        StringWriter out = new StringWriter();
        int ch = urlIn.read();
        while (ch != -1) {
            out.write(ch);
            ch = urlIn.read();
        }
        out.flush();
        return out.toString();
    }

    public Object resolve(String fieldName, int[] arrayPos) throws FieldDoesNotHaveDimensionException, FieldIndexOutOfBoundsException, InvalidElementException {
        if ((fieldName != null) && (fieldName.startsWith("include"))) {
            String argument = fieldName.substring("include".length() + 1);
            try {
                return getUrlContent(argument);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return super.resolve(fieldName, arrayPos);
    }

    protected Object getSubObject(Object parentObject, String subObjectName, ArrayPositioner arrayPositioner) throws FieldIndexOutOfBoundsException {
        if (subObjectName.equals("serialized")) {
            if (!(parentObject instanceof Class)) {
                parentObject = DefaultParserObjectWrapper.wrapObject(parentObject);
            } else {
                parentObject = DefaultParserObjectWrapper.getWrapperClass((Class) parentObject);
            }
            int counter;
            try {
                Integer counterInteger = (Integer) httpSession.getValue("SERCOUNTER");
                counter = counterInteger.intValue();
            } catch (Exception ex) {
                counter = 0;
            }
            String sessionName = "OBJ" + counter++;
            httpSession.putValue(sessionName, parentObject);
            httpSession.putValue("SERCOUNTER", new Integer(counter));
            return sessionName;
        } else {
            return super.getSubObject(parentObject, subObjectName, arrayPositioner);
        }
    }
}
