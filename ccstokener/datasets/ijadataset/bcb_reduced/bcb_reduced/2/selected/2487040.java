package org.apache.jsp;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.jsp.*;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.JarURLConnection;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

public final class happyjuddi_jsp extends org.apache.jasper.runtime.HttpJspBase implements org.apache.jasper.runtime.JspSourceDependent {

    /**
     * Look for the named class in the classpath
     *
     * @param name of the class to lookup
     * @return the location of the named class
     * @throws IOException
     */
    String lookupClass(String className) throws IOException {
        Class clazz = null;
        try {
            clazz = Class.forName(className);
            if (clazz == null) return null;
        } catch (ClassNotFoundException e) {
            return null;
        }
        URL url = null;
        try {
            url = clazz.getProtectionDomain().getCodeSource().getLocation();
            if (url == null) return "";
        } catch (Throwable t) {
            return "";
        }
        String location = getLocation(url);
        if (location == null) return ""; else return location;
    }

    /**
     * Look for the named resource or properties file.
     *
     * @param resourceName
     * @return true if the file was found
     */
    String lookupResource(String resourceName) {
        URL url = null;
        ClassLoader classLoader = null;
        classLoader = this.getClass().getClassLoader();
        if (classLoader != null) {
            url = classLoader.getResource(resourceName);
            if (url != null) {
                return getLocation(url);
            }
        } else {
            classLoader = System.class.getClassLoader();
            if (classLoader != null) {
                url = classLoader.getResource(resourceName);
                if (url != null) {
                    return getLocation(url);
                }
            }
        }
        return null;
    }

    /**
     * Determine the location of the Java class.
     *
     * @param clazz
     * @return the file path to the jar file or class 
     *  file where the class was located.
     */
    String getLocation(URL url) {
        try {
            String location = url.toString();
            if (location.startsWith("jar:file:/")) {
                File file = new File(url.getFile());
                return file.getPath().substring(6);
            } else if (location.startsWith("jar")) {
                url = ((JarURLConnection) url.openConnection()).getJarFileURL();
                return url.toString();
            } else if (location.startsWith("file")) {
                File file = new File(url.getFile());
                return file.getAbsolutePath();
            } else {
                return url.toString();
            }
        } catch (Throwable t) {
            return null;
        }
    }

    private static java.util.List _jspx_dependants;

    public Object getDependants() {
        return _jspx_dependants;
    }

    public void _jspService(HttpServletRequest request, HttpServletResponse response) throws java.io.IOException, ServletException {
        JspFactory _jspxFactory = null;
        PageContext pageContext = null;
        ServletContext application = null;
        ServletConfig config = null;
        JspWriter out = null;
        Object page = this;
        JspWriter _jspx_out = null;
        PageContext _jspx_page_context = null;
        try {
            _jspxFactory = JspFactory.getDefaultFactory();
            response.setContentType("text/html");
            pageContext = _jspxFactory.getPageContext(this, request, response, null, false, 8192, true);
            _jspx_page_context = pageContext;
            application = pageContext.getServletContext();
            config = pageContext.getServletConfig();
            out = pageContext.getOut();
            _jspx_out = out;
            out.write('\n');
            out.write('\n');
            out.write("\n");
            out.write("<html>\n");
            out.write("<head>\n");
            out.write("<title>jUDDI Happiness Page</title>\n");
            out.write("<link rel=\"stylesheet\" href=\"juddi.css\">\n");
            out.write("</head>\n");
            out.write("<body>\n");
            out.write("\n");
            out.write("<div class=\"nav\" align=\"right\"><font size=\"-2\"><a href=\"http://ws.apache.org/juddi/\">jUDDI@Apache</a></font></div>\n");
            out.write("<h1>jUDDI</h1>\n");
            out.write("\n");
            out.write("<div class=\"announcement\">\n");
            out.write("<p>\n");
            out.write("<h3>Happy jUDDI!</h3>\n");
            out.write("\n");
            out.write("<h4>jUDDI Version Information</h4>\n");
            out.write("<pre>\n");
            out.write("<b>jUDDI Version:</b> ");
            out.print(org.apache.juddi.util.Release.getRegistryVersion());
            out.write("\n");
            out.write("<b>Build Date:</b>    ");
            out.print(org.apache.juddi.util.Release.getLastModified());
            out.write("\n");
            out.write("<b>UDDI Version:</b>  ");
            out.print(org.apache.juddi.util.Release.getUDDIVersion());
            out.write("\n");
            out.write("</pre>\n");
            out.write("        \n");
            out.write("<h4>jUDDI Dependencies: Class Files &amp; Libraries</h4>\n");
            out.write("<pre>\n");
            String[] classArray = { "org.apache.juddi.IRegistry", "org.apache.axis.transport.http.AxisServlet", "org.apache.commons.discovery.Resource", "org.apache.commons.logging.Log", "org.apache.log4j.Layout", "javax.xml.soap.SOAPMessage", "javax.xml.rpc.Service", "com.ibm.wsdl.factory.WSDLFactoryImpl", "javax.xml.parsers.SAXParserFactory" };
            for (int i = 0; i < classArray.length; i++) {
                out.write("<b>Looking for</b>: " + classArray[i] + "<br>");
                String result = lookupClass(classArray[i]);
                if (result == null) {
                    out.write("<font color=\"red\">-Not Found</font><br>");
                } else if (result.length() == 0) {
                    out.write("<font color=\"blue\">+Found in an unknown location</font><br>");
                } else {
                    out.write("<font color=\"green\">+Found in: " + result + "</font><br>");
                }
            }
            out.write("\n");
            out.write("</pre>\n");
            out.write("        \n");
            out.write("<h4>jUDDI Dependencies: Resource &amp; Properties Files</h4>\n");
            out.write("<pre>\n");
            String[] resourceArray = { "log4j.xml" };
            for (int i = 0; i < resourceArray.length; i++) {
                out.write("<b>Looking for</b>: " + resourceArray[i] + "<br>");
                String result = lookupResource(resourceArray[i]);
                if (result == null) {
                    out.write("<font color=\"red\">-Not Found</font><br>");
                } else if (result.length() == 0) {
                    out.write("<font color=\"blue\">+Found in an unknown location</font><br>");
                } else {
                    out.write("<font color=\"green\">+Found in: " + result + "</font><br>");
                }
            }
            out.write("\n");
            out.write("</pre>\n");
            out.write("\n");
            out.write("<h4>jUDDI DataSource Validation</h4>\n");
            out.write("<pre>\n");
            String dsname = null;
            Context ctx = null;
            DataSource ds = null;
            Connection conn = null;
            String sql = "SELECT COUNT(*) FROM PUBLISHER";
            try {
                dsname = request.getParameter("dsname");
                if ((dsname == null) || (dsname.trim().length() == 0)) dsname = "java:comp/env/jdbc/juddiDB";
                ctx = new InitialContext();
                if (ctx == null) throw new Exception("No Context");
                out.print("<font color=\"green\">");
                out.print("+ Got a JNDI Context!");
                out.println("</font>");
            } catch (Exception ex) {
                out.print("<font color=\"red\">");
                out.print("- No JNDI Context (" + ex.getMessage() + ")");
                out.println("</font>");
            }
            try {
                ds = (DataSource) ctx.lookup(dsname);
                if (ds == null) throw new Exception("No Context");
                out.print("<font color=\"green\">");
                out.print("+ Got a JDBC DataSource (dsname=" + dsname + ")");
                out.println("</font>");
            } catch (Exception ex) {
                out.print("<font color=\"red\">");
                out.print("- No '" + dsname + "' DataSource Located(" + ex.getMessage() + ")");
                out.println("</font>");
            }
            try {
                conn = ds.getConnection();
                if (conn == null) throw new Exception("No Connection (conn=null)");
                out.print("<font color=\"green\">");
                out.print("+ Got a JDBC Connection!");
                out.println("</font>");
            } catch (Exception ex) {
                out.print("<font color=\"red\">");
                out.print("- DB connection was not aquired. (" + ex.getMessage() + ")");
                out.println("</font>");
            }
            try {
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                out.print("<font color=\"green\">");
                out.print("+ " + sql + " = ");
                if (rs.next()) out.print(rs.getString(1));
                out.println("</font>");
                conn.close();
            } catch (Exception ex) {
                out.print("<font color=\"red\">");
                out.print("- " + sql + " failed (" + ex.getMessage() + ")");
                out.println("</font>");
            }
            out.write("\n");
            out.write("</pre>\n");
            out.write("\n");
            out.write("\n");
            out.write("<h4>System Properties</h4>\n");
            out.write("<pre>\n");
            try {
                Properties sysProps = System.getProperties();
                SortedSet sortedProperties = new TreeSet(sysProps.keySet());
                for (Iterator keys = sortedProperties.iterator(); keys.hasNext(); ) {
                    String key = (String) keys.next();
                    out.println("<b>" + key + "</b>: " + sysProps.getProperty(key));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            out.write("\n");
            out.write("</pre>\n");
            out.write("\n");
            out.write("<hr>\n");
            out.write("Platform: ");
            out.print(getServletConfig().getServletContext().getServerInfo());
            out.write("\n");
            out.write("\n");
            out.write("<table width=\"100%\" border=\"0\">\n");
            out.write("<tr><td height=\"50\" align=\"center\" valign=\"bottom\" nowrap><div class=\"footer\">&nbsp;</div></td></tr>\n");
            out.write("</table>\n");
            out.write("\n");
            out.write("</body>\n");
            out.write("</div>\n");
            out.write("</html>");
        } catch (Throwable t) {
            if (!(t instanceof SkipPageException)) {
                out = _jspx_out;
                if (out != null && out.getBufferSize() != 0) out.clearBuffer();
                if (_jspx_page_context != null) _jspx_page_context.handlePageException(t);
            }
        } finally {
            if (_jspxFactory != null) _jspxFactory.releasePageContext(_jspx_page_context);
        }
    }
}
