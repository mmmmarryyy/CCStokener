package org.merak.example.business;

import javax.servlet.http.HttpSession;
import org.merak.core.web.HttpRequest;
import org.merak.core.web.HttpResponse;
import org.merak.core.web.mvc.IBusinessLogic;

public class ShowCurrency implements IBusinessLogic {

    @Override
    public boolean authorize(HttpSession session) {
        return true;
    }

    @Override
    public String execute(HttpRequest request, HttpResponse response) throws Exception {
        request.setAttribute("msg", "Hello World !!");
        return "/upload.jsp";
    }
}
