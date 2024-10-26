import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

/**
 *
 * @author Juan D. Aldana
 */
@WebServlet(name = "principal", urlPatterns = { "/principal" })
public class principal extends HttpServlet {

    /**
     * Processes requests for both HTTP
     * <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        PrintWriter out = response.getWriter();
        try {
            int op = Integer.parseInt(request.getParameter("op"));
            String id = "";
            switch(op) {
                case (1):
                    String user, passwd;
                    user = request.getParameter("user");
                    passwd = request.getParameter("passwd");
                    String session = "", userid = "";
                    session = operation(user, passwd);
                    if (session.equals("Usuario No Registrado")) {
                        out.println("<h2>" + session + "</h2>");
                    } else {
                        if (session.equals("Password incorrecto")) {
                            out.println("<h2>" + session + "</h2>");
                        } else {
                            out.println("<h2>Has sido logueado correctamente </br> " + session + "</h2>" + "<br ><a href='datos_personales.jsp'>Actualizar Datos Personales</a>");
                            userid = hello(user);
                            HttpSession sesion = request.getSession(true);
                            sesion.setAttribute("id", userid);
                            sesion.setAttribute("nombrec", session);
                        }
                    }
                    break;
                case (2):
                    String nombres, cedula, apellidos, userr, passwdr, emailr;
                    nombres = request.getParameter("nombres");
                    apellidos = request.getParameter("apellidos");
                    cedula = request.getParameter("cedula");
                    userr = request.getParameter("userr");
                    passwdr = request.getParameter("passwdr");
                    emailr = request.getParameter("emailr");
                    out.println(registro(cedula, nombres, apellidos, userr, passwdr, emailr));
                    HttpSession sesion = request.getSession(true);
                    sesion.setAttribute("id", cedula);
                    sesion.setAttribute("nombrec", nombres + " " + apellidos);
                    out.println("<h2>Has sido logueado correctamente </br>" + nombres + " " + apellidos + "</h2>" + "<br ><a href='datos_personales.jsp'>Actualizar Datos Personales</a>");
                    break;
                case (3):
                    String busqueda = request.getParameter("busqueda");
                    System.out.print(busqueda);
                    try {
                        URL url = new URL("http://localhost//WSPHPDepartamento//cliente.php?busqueda=" + busqueda);
                        HttpURLConnection con = (HttpURLConnection) url.openConnection();
                        con.getContent();
                        InputStream in = con.getInputStream();
                        StringBuffer data = new StringBuffer();
                        int i;
                        while ((i = in.read()) != -1) {
                            data.append((char) i);
                        }
                        out.println(data);
                    } catch (Exception e) {
                        System.out.println("Fallo al recibir información del script PHP");
                    }
                    break;
                case (4):
                    String usu = "", apel = "", apel2 = "", tid = "", dep1 = "";
                    String mun1 = "", dep2 = "", mun2 = "", fechan = "", dir = "", dep3 = "", mun3 = "", cel = "", tel1 = "", tel2 = "";
                    sesion = request.getSession();
                    id = (String) sesion.getAttribute("id");
                    usu = request.getParameter("usu");
                    apel = request.getParameter("apel");
                    apel2 = request.getParameter("apel2");
                    tid = request.getParameter("tid");
                    dep1 = request.getParameter("dep1");
                    mun1 = request.getParameter("mun1");
                    dep2 = request.getParameter("dep2");
                    mun2 = request.getParameter("mun2");
                    fechan = request.getParameter("fechan");
                    dir = request.getParameter("dir");
                    dep3 = request.getParameter("dep3");
                    mun3 = request.getParameter("mun3");
                    cel = request.getParameter("cel");
                    tel1 = request.getParameter("tel1");
                    tel2 = request.getParameter("tel2");
                    out.println(datosPersonales(id, usu, apel, apel2, tid, dep1, mun1, dep2, mun2, fechan, dir, dep3, mun3, cel, tel1, tel2));
                    break;
                case (5):
                    String programa, facultad, year, nivel, titulo, fecha;
                    sesion = request.getSession();
                    id = (String) sesion.getAttribute("id");
                    programa = request.getParameter("programa");
                    facultad = request.getParameter("facultad");
                    year = request.getParameter("year");
                    nivel = request.getParameter("nivel");
                    titulo = request.getParameter("titulo");
                    fecha = request.getParameter("fecha");
                    out.println(historiaAcademica(id, programa, facultad, year, nivel, titulo, fecha));
                    break;
                case (6):
                    String nom_empr = "", cargo = "", experiencia = "", emp_a_cargo = "", emp_uni = "", opinion = "";
                    String dir_lab = "", dep = "", mun = "", tel_em = "", emaillab = "", emailper = "";
                    sesion = request.getSession();
                    id = (String) sesion.getAttribute("id");
                    nom_empr = request.getParameter("nom_empr");
                    cargo = request.getParameter("cargo");
                    experiencia = request.getParameter("experiencia");
                    emp_a_cargo = request.getParameter("emp_a_cargo");
                    emp_uni = request.getParameter("emp_uni");
                    opinion = request.getParameter("opinion");
                    dir_lab = request.getParameter("dir_lab");
                    dep = request.getParameter("dep");
                    mun = request.getParameter("mun");
                    tel_em = request.getParameter("tel_em");
                    emaillab = request.getParameter("emaillab");
                    emailper = request.getParameter("emailper");
                    out.println(informacionLaboral(id, nom_empr, cargo, experiencia, emp_a_cargo, emp_uni, opinion, dir_lab, dep, mun, tel_em, emaillab, emailper));
                    break;
                default:
                    System.out.println("Opcion No valida");
            }
        } finally {
            out.close();
        }
    }

    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP
     * <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }

    private static String hello(java.lang.String name) {
        ws.Login_Service service = new ws.Login_Service();
        ws.Login port = service.getLoginPort();
        return port.hello(name);
    }

    private static String operation(java.lang.String user, java.lang.String passwd) {
        ws.Login_Service service = new ws.Login_Service();
        ws.Login port = service.getLoginPort();
        return port.operation(user, passwd);
    }

    private static String registro(java.lang.String id, java.lang.String nombres, java.lang.String apellidos, java.lang.String usuario, java.lang.String passwd, java.lang.String email) {
        ws.Login_Service service = new ws.Login_Service();
        ws.Login port = service.getLoginPort();
        return port.registro(id, nombres, apellidos, usuario, passwd, email);
    }

    private static String historiaAcademica(java.lang.String id, java.lang.String programa, java.lang.String facultad, java.lang.String year, java.lang.String nivel, java.lang.String titulo, java.lang.String fecha) {
        ws.BDEgresados_Service service = new ws.BDEgresados_Service();
        ws.BDEgresados port = service.getBDEgresadosPort();
        return port.historiaAcademica(id, programa, facultad, year, nivel, titulo, fecha);
    }

    private static String informacionLaboral(java.lang.String id, java.lang.String nomEmpr, java.lang.String cargo, java.lang.String experiencia, java.lang.String empACargo, java.lang.String empUni, java.lang.String opinion, java.lang.String dirLab, java.lang.String dep, java.lang.String mun, java.lang.String telEm, java.lang.String emaillab, java.lang.String emailper) {
        ws.BDEgresados_Service service = new ws.BDEgresados_Service();
        ws.BDEgresados port = service.getBDEgresadosPort();
        return port.informacionLaboral(id, nomEmpr, cargo, experiencia, empACargo, empUni, opinion, dirLab, dep, mun, telEm, emaillab, emailper);
    }

    private static String datosPersonales(java.lang.String id, java.lang.String usu, java.lang.String apel, java.lang.String apel2, java.lang.String tid, java.lang.String dep1, java.lang.String mun1, java.lang.String dep2, java.lang.String mun2, java.lang.String fechan, java.lang.String dir, java.lang.String dep3, java.lang.String mun3, java.lang.String cel, java.lang.String tel1, java.lang.String tel2) {
        ws.BDEgresados_Service service = new ws.BDEgresados_Service();
        ws.BDEgresados port = service.getBDEgresadosPort();
        return port.datosPersonales(id, usu, apel, apel2, tid, dep1, mun1, dep2, mun2, fechan, dir, dep3, mun3, cel, tel1, tel2);
    }
}
