package org.merak.example.business;

import org.merak.core.web.HttpRequest;
import org.merak.core.web.HttpResponse;
import org.merak.core.web.mvc.PublicBusinessLogic;

public class HelloWorld extends PublicBusinessLogic {

    private static final String view = "/WEB-INF/pages/hello_world.jsp";

    @Override
    public String execute(HttpRequest request, HttpResponse response) throws Exception {
        return view;
    }
}
