package fr.macymed.modulo.module.http;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Hashtable;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import fr.macymed.commons.io.IOUtilities;

/** 
 * <p>
 * The root servlet. Can loads "static" contents from class loader, and returns a 404 error is not found.
 * </p>
 * @author <a href="mailto:alexandre.cartapanis@macymed.fr">Cartapanis Alexandre</a>
 * @version 1.5.0
 * @since Modulo Http Module 1.1
 */
public class RootServlet extends HttpServlet {

    /** The serial version UID */
    private static final long serialVersionUID = 6412441152149735418L;

    /** The list of class loader. */
    private static final Hashtable<String, ClassLoader> loaders = new Hashtable<String, ClassLoader>();

    static {
        loaders.put("", RootServlet.class.getClassLoader());
    }

    /**
     * <p>
     * Creates a new RootServlet.
     * </p>
     */
    public RootServlet() {
        super();
    }

    /**
     * <p>
     * Adds a class loader.
     * </p>
     * @param _path The path associated with the loader.
     * @param _loader The class loader to add.
     */
    public static void addClassLoader(String _path, ClassLoader _loader) {
        if (_loader != null) {
            loaders.put(_path, _loader);
        }
    }

    /**
     * <p>
     * Removes a class loader (from path).
     * </p>
     * @param _path The path to remove.
     */
    public static void removeClassLoader(String _path) {
        if (_path != null) {
            loaders.remove(_path);
        }
    }

    /**
     * <p>
     * Removes all class loaders.
     * </p>
     */
    public static void removeAllClassLoader() {
        loaders.clear();
    }

    /**
     * <p>
     * Search the request's resource along the class loader list.
     * </p>
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     * @param _request The request.
     * @param _response The response.
     * @throws ServletException If an error occurs with the Servlet.
     * @throws IOException IF an I/O error occurs.
     */
    @Override
    protected void doGet(HttpServletRequest _request, HttpServletResponse _response) throws ServletException, IOException {
        String context = _request.getContextPath();
        String path = _request.getRequestURI();
        URL url = null;
        ClassLoader loader = loaders.get(context);
        if (loader != null) {
            url = loader.getResource(path);
        }
        if (url == null) {
            _response.sendError(404);
            return;
        }
        URLConnection connection = url.openConnection();
        IOUtilities.flow(connection.getInputStream(), _response.getOutputStream());
        _response.setContentType(connection.getContentType());
        _response.setContentLength(connection.getContentLength());
        _response.setCharacterEncoding(connection.getContentEncoding());
    }
}
