package fr.macymed.modulo.module.http.admin;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import fr.macymed.commons.io.IOUtilities;

/** 
 * <p>
 * This servlet is a part of the Modulo HttpAdmin Console. It is in charge of controlling application flow.
 * </p>
 * @author <a href="mailto:alexandre.cartapanis@macymed.fr">Cartapanis Alexandre</a>
 * @version 1.0.0
 * @since Modulo HttpAdmin Module 1.0
 */
public class AdminServlet extends HttpServlet {

    /** The serial version UID. */
    private static final long serialVersionUID = 3297088010834984218L;

    /**
     * <p>
     * Handle a POST request.
     * </p>
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     * @param _request The request.
     * @param _response The response.
     * @throws ServletException If an error occurs with the Servlet.
     * @throws IOException IF an I/O error occurs.
     */
    @Override
    protected void doPost(HttpServletRequest _request, HttpServletResponse _response) throws ServletException, IOException {
        this.doGet(_request, _response);
    }

    /**
     * <p>
     * Handle a GET request.
     * </p>
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     * @param _request The request.
     * @param _response The response.
     * @throws ServletException If an error occurs with the Servlet.
     * @throws IOException IF an I/O error occurs.
     */
    @Override
    protected void doGet(HttpServletRequest _request, HttpServletResponse _response) throws ServletException, IOException {
        _response.setContentType("text/html");
        PrintWriter out = null;
        try {
            out = new PrintWriter(new PrintStream(_response.getOutputStream(), false, "UTF-8"));
            _response.setCharacterEncoding("UTF-8");
        } catch (UnsupportedEncodingException excp) {
            out = new PrintWriter(_response.getOutputStream());
        }
        if (!_request.getServletPath().equals("/")) {
            boolean found = this.handleResource(_request, _response);
            if (found) {
                out.flush();
                out.close();
                _response.flushBuffer();
                return;
            }
        }
        if (!isLoggedIn(_request.getSession(true))) {
            if (_request.getParameter("fullfil") != null) {
                String error = this.login(_request, _response);
                if (error != null) {
                    _request.getSession(true).setAttribute("login.error.desc", error);
                    this.writeLoginPage(out, _request);
                    _request.getSession(true).removeAttribute("login.error.desc");
                    out.flush();
                    out.close();
                    _response.flushBuffer();
                    return;
                }
            } else {
                boolean logged = false;
                Cookie[] cookies = _request.getCookies();
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals("logged")) {
                        logged = true;
                    }
                }
                if (!logged) {
                    this.writeLoginPage(out, _request);
                    out.flush();
                    out.close();
                    _response.flushBuffer();
                    return;
                }
            }
        }
        String pageName = _request.getParameter("page");
        if (pageName == null) {
            _response.sendRedirect("/admin/?page=index");
            return;
        }
        Page page = AdminService.pages.get(pageName);
        if (page != null) {
            String[] pageNames = new String[AdminService.pages.size()];
            Page[] pages = new Page[AdminService.pages.size()];
            pageNames[0] = "index";
            pages[0] = AdminService.pages.get("index");
            int i = 1;
            for (String str : AdminService.pages.keySet()) {
                if (str.equals("index") || str.equals("help")) {
                    continue;
                }
                pageNames[i] = str;
                pages[i] = AdminService.pages.get(str);
                i++;
            }
            pageNames[AdminService.pages.size() - 1] = "help";
            pages[AdminService.pages.size() - 1] = AdminService.pages.get("help");
            page.writePage(out, _request, _response, pageNames, pages, pageName);
        } else {
            String uri = _request.getRequestURL().toString();
            if (_request.getQueryString() != null) {
                uri += "?" + _request.getQueryString();
            }
        }
        try {
            out.close();
        } catch (Exception excp) {
        }
        _response.flushBuffer();
    }

    /**
     * <p>
     * Writes data to the output stream.
     * </p>
     * @param _request The request.
     * @param _response The response.
     * @return <code></code> - True if the resource hase been found, false otherwise.
     * @throws ServletException If an error occurs with the Servlet.
     * @throws IOException IF an I/O error occurs.
     */
    private boolean handleResource(HttpServletRequest _request, HttpServletResponse _response) throws ServletException, IOException {
        String path = _request.getRequestURI();
        path = path.substring(_request.getContextPath().length());
        URL url = this.getClass().getClassLoader().getResource(path);
        if (url == null) {
            _response.sendError(404);
            return false;
        }
        URLConnection connection = url.openConnection();
        IOUtilities.flow(connection.getInputStream(), _response.getOutputStream());
        _response.setContentType(connection.getContentType());
        _response.setContentLength(connection.getContentLength());
        _response.setCharacterEncoding(connection.getContentEncoding());
        _response.getOutputStream().flush();
        _response.getOutputStream().close();
        _response.flushBuffer();
        return true;
    }

    /**
     * <p>
     * Checks the session to see if an user is logged or not.
     * </p>
     * @param _session The session.
     * @return <code>boolean</code> - True if the user is already authentified, false otherwise.
     */
    private boolean isLoggedIn(HttpSession _session) {
        if (_session.getAttribute("user") != null) {
            return true;
        }
        return false;
    }

    /**
     * <p>
     * Writes the login page.
     * </p>
     * @param _out The writer
     * @param _request The request.
     */
    private void writeLoginPage(PrintWriter _out, HttpServletRequest _request) {
        new LoginPage().writePage(_out, _request);
    }

    /**
     * <p>
     * Checks if the user is valid or not.
     * </p>
     * @param _request The request.
     * @param _response The servlet.
     * @return <code>String</code> - An error key, or null is login in has fullfil without error.
     */
    private String login(HttpServletRequest _request, HttpServletResponse _response) {
        String login = _request.getParameter("login");
        String password = _request.getParameter("password");
        String remember = _request.getParameter("remember");
        if (login == null || !login.equals("admin")) {
            return "error.login";
        }
        if (password == null || !password.equals("admin")) {
            return "error.password";
        }
        if (remember != null && remember.equals("checked")) {
            Cookie loginCookie = new Cookie("logged", "true");
            loginCookie.setMaxAge(1000 * 60 * 60 * 24 * 365);
            _response.addCookie(loginCookie);
        }
        _request.getSession(true).setAttribute("user", new Object());
        return null;
    }
}
