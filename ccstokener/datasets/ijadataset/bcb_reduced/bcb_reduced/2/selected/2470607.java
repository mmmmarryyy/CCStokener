package adv.web;

import adv.tools.IOTools;
import adv.tools.TextTools;
import adv.language.ModuloMng;
import adv.language.Config;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Alberto Vilches Ratón
 * <p/>
 * Kenshira
 * <p/>
 * Fecha y hora de creación: 24-nov-2007 1:31:46
 */
public class ResourceServlet extends HttpServlet {

    Map<String, Repository> repos = new HashMap<String, Repository>();

    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        for (Enumeration e = servletConfig.getInitParameterNames(); e.hasMoreElements(); ) {
            String key = (String) e.nextElement();
            String value = servletConfig.getInitParameter(key);
            repos.put(key, new Repository(key, value));
        }
    }

    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding(Config.getMng().getEncoding());
        response.setCharacterEncoding(Config.getMng().getEncoding());
        String repository = request.getServletPath();
        if (repository.startsWith("/")) {
            repository = repository.substring(1);
        }
        Repository rep = repos.get(repository);
        String resource = request.getPathInfo();
        request.setAttribute("resource-request", resource);
        if (resource == null) {
            response.sendRedirect(repository + "/");
            return;
        }
        resource = URLDecoder.decode(resource, Config.getMng().getEncoding());
        if (resource == null || resource.equals("/") || resource.equals(".") || resource.length() == 0) {
            resource = "index.html";
        }
        resource = resource.replaceAll("-", " ");
        if (rep.template != null && (resource.endsWith(".html") || resource.endsWith(".htm"))) {
            String title = resource;
            if (title.startsWith("/")) {
                title = title.substring(1);
            }
            if (title.length() > 1) {
                title = title.substring(0, 1).toUpperCase() + title.substring(1);
            }
            if (title.endsWith(".html")) {
                title = title.substring(0, title.length() - 5);
            } else if (title.endsWith(".htm")) {
                title = title.substring(0, title.length() - 4);
            }
            request.setAttribute("repositorio", rep);
            request.setAttribute("resource", resource);
            request.setAttribute("title", title);
            request.getRequestDispatcher(rep.template).forward(request, response);
        } else {
            URL url = rep.getResource(request, resource);
            if (url != null) {
                IOTools.copy(url.openStream(), response.getOutputStream());
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, resource);
            }
        }
    }

    public static String filtraDoc(HttpServletRequest request, String resource, Repository rep, String template) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        int sec = 0;
        try {
            URL url = rep.getResource(request, resource);
            if (url == null) {
                return "Documento " + rep.dir + "/" + resource + " no encontrado";
            }
            br = new BufferedReader(new InputStreamReader(url.openStream(), rep.encoding));
            String line = br.readLine();
            while (line != null) {
                int pos = line.indexOf("KAttach(");
                if (pos > -1) {
                    sb.append(attach(request, ++sec, line, pos, template));
                } else {
                    line = line.replaceAll("%20", "-");
                    sb.append(new String(line.getBytes(rep.encoding), Config.getMng().getEncoding())).append("\n");
                }
                line = br.readLine();
            }
        } finally {
            if (br != null) br.close();
        }
        return sb.toString();
    }

    private static String attach(HttpServletRequest request, int sec, String line, int pos, String template) throws IOException {
        line = line.substring(pos + 8);
        pos = line.indexOf(")");
        String attachedFile = line.substring(0, pos);
        pos = attachedFile.lastIndexOf("/");
        String kfile = attachedFile.substring(pos);
        kfile = kfile.substring(1, kfile.length() - ModuloMng.ADV_EXTENSION.length());
        String alias = attachedFile.substring(0, pos);
        String download = alias + "/src?part=" + kfile;
        String repositorio = alias + "/edit?part=" + kfile;
        String box = ContentManager.getInstance().getContentAsText(template, request);
        File file = new File(Config.getMng().getDirCode().getCanonicalPath() + File.separatorChar + attachedFile);
        String content = textFileToString(file, Config.getMng().getEncoding());
        Map<String, Object> values = new HashMap<String, Object>();
        values.put("attach_id", String.valueOf(sec));
        values.put("attach_download_filename", request.getContextPath() + "/" + download);
        values.put("attach_browse_filename", request.getContextPath() + "/" + repositorio);
        values.put("attach_content", content);
        values.put("attach_filename", attachedFile);
        String parsed = TextTools.replaceVars(box, values);
        return parsed;
    }

    public static String textFileToString(File file, String encoding) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(new FileInputStream(file), encoding));
            String line = br.readLine();
            while (line != null) {
                sb.append(line).append("\n");
                line = br.readLine();
            }
        } finally {
            if (br != null) br.close();
        }
        return sb.toString();
    }
}
