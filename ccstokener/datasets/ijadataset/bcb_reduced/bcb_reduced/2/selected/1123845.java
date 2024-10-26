package org.javalid.test.web.db.view.servlet;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import javax.naming.InitialContext;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.sql.DataSource;
import org.javalid.core.AnnotationValidator;
import org.javalid.core.AnnotationValidatorImpl;
import org.javalid.core.ValidationMessage;
import org.javalid.test.web.db.model.Employee;

/**
 * Simple test servlet which calls validation depending on input parameter
 * and forwards to jsp which will render the validation result. Note this
 * is for testing purposes only and demonstrates the usage of the integration
 * with the database through a datasource.
 * @author  M.Reuvers
 * @version 1.0
 * @since   1.1
 */
public class TestServlet extends HttpServlet {

    private AnnotationValidator validator;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        Connection conn = null;
        URL url = null;
        try {
            conn = ((DataSource) new InitialContext().lookup("java:comp/env/jdbc/JavalidTestDS")).getConnection();
            if (System.getProperty("install.oracle") == null) {
                url = this.getClass().getResource("org/javalid/test/web/db/config/hsqldb-install.sql");
                if (url == null) {
                    url = this.getClass().getResource("/org/javalid/test/web/db/config/hsqldb-install.sql");
                }
            } else {
                url = this.getClass().getResource("org/javalid/test/web/db/config/oracle-install.sql");
                if (url == null) {
                    url = this.getClass().getResource("/org/javalid/test/web/db/config/oracle-install.sql");
                }
            }
            BufferedInputStream in = new BufferedInputStream(url.openConnection().getInputStream());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            out.close();
            in.close();
            PreparedStatement st = conn.prepareStatement(out.toString());
            st.execute();
            st.close();
            conn.commit();
            conn.close();
        } catch (Exception e) {
            throw new RuntimeException("FAILED", e);
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
        validator = new AnnotationValidatorImpl("org/javalid/test/web/db/config/javalid-config.xml");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String what = request.getParameter("what");
        Employee emp = null;
        List<ValidationMessage> messages = null;
        if (what != null) {
            if (what.equals("1")) {
                emp = getValidEmployee();
                messages = validator.validateObject(emp, "emp");
            } else if (what.equals("2")) {
                emp = getValidEmployee();
                emp.setBoss(new Employee());
                messages = validator.validateObject(emp, "emp");
            } else if (what.equals("3")) {
                emp = getValidEmployee();
                emp.setBoss(new Employee());
                emp.getBoss().setId(3L);
                messages = validator.validateObject(emp, "emp");
            } else if (what.equals("4")) {
                emp = getValidEmployee();
                emp.setBoss(new Employee());
                emp.getBoss().setId(2L);
                messages = validator.validateObject(emp, "emp");
            } else if (what.equals("5")) {
                emp = getValidEmployee();
                emp.setBoss(new Employee());
                emp.getBoss().setId(1L);
                messages = validator.validateObject(emp, "emp");
            }
        }
        request.setAttribute("employee", emp);
        request.setAttribute("messages", messages);
        request.getRequestDispatcher("result.jsp").forward(request, response);
    }

    private Employee getValidEmployee() {
        Employee emp = new Employee();
        emp.setId(10L);
        emp.setFirstName("New");
        emp.setLastName("Employee");
        return emp;
    }
}
