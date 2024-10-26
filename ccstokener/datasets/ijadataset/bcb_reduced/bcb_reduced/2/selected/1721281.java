package org.merak.core.web.mvc;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.merak.core.web.HttpRequest;
import org.merak.core.web.HttpResponse;
import org.merak.core.web.mvc.annotation.NotNull;
import org.merak.core.web.mvc.annotation.NotOutOfBound;
import org.merak.core.web.mvc.validation.BatchValidator;
import org.merak.core.web.mvc.validation.NotNullValidator;
import org.merak.core.web.mvc.validation.NotOutOfBoundValidator;

public class PageController {

    private String businessName = null;

    private Class<?> businessClass = null;

    private List<BatchValidator> validatorList = new LinkedList<BatchValidator>();

    public PageController(String businessName, Class<?> businessClass) throws Exception {
        this.businessName = businessName;
        this.businessClass = businessClass;
        this.compileAnnotations();
    }

    private void compileAnnotations() throws SecurityException, NoSuchMethodException {
        Method method = this.businessClass.getMethod("execute", HttpRequest.class, HttpResponse.class);
        NotNull notNull = method.getAnnotation(NotNull.class);
        if (notNull != null) this.validatorList.add(new NotNullValidator(notNull));
        NotOutOfBound notOutOfBound = method.getAnnotation(NotOutOfBound.class);
        if (notOutOfBound != null) this.validatorList.add(new NotOutOfBoundValidator(notOutOfBound));
    }

    /** Find the object responsible for handling the specific requested business.
     *  @param businessName the key-name of the requested business object.
     *  @return one business object from the specified businessPackage
     *  @throws ServletException */
    private IBusinessLogic getBusinessLogic() throws ServletException {
        try {
            return (IBusinessLogic) this.businessClass.newInstance();
        } catch (Exception e) {
            throw new ServletException("The Bussiness Logic cannot be started: " + this.businessName, e);
        }
    }

    public String runBusinessLogic(HttpServletRequest req, HttpServletResponse res) throws ServletException, ExceptionView, SecurityException {
        IBusinessLogic business = this.getBusinessLogic();
        if (!business.authorize(req.getSession())) throw new SecurityException("The Business Logic Execution is not authorized for this user");
        try {
            HttpRequest request = new HttpRequest(req);
            HttpResponse response = new HttpResponse(res);
            for (BatchValidator v : this.validatorList) {
                if (!v.validate(request, response)) return v.getView();
            }
            return business.execute(request, response);
        } catch (ExceptionView view) {
            throw view;
        } catch (Throwable e) {
            throw new ServletException("The Business Logic Execution caused an Error: " + this.businessName, e);
        }
    }

    public String runExceptionLogic(HttpServletRequest req, HttpServletResponse res) throws ServletException {
        try {
            HttpRequest request = new HttpRequest(req);
            HttpResponse response = new HttpResponse(res);
            for (BatchValidator v : this.validatorList) {
                if (!v.validate(request, response)) return v.getView();
            }
            return this.getBusinessLogic().execute(request, response);
        } catch (Throwable e) {
            throw new ServletException("The Exception Logic Execution caused an Error: " + this.businessName, e);
        }
    }
}
