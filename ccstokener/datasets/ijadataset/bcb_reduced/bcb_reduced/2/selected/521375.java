package org.merak.example.business;

import java.io.File;
import org.merak.core.web.HttpRequest;
import org.merak.core.web.HttpResponse;
import org.merak.core.web.WebParameter;
import org.merak.core.web.mvc.PublicBusinessLogic;

public class RequestFile extends PublicBusinessLogic {

    private static final String view = null;

    @Override
    public String execute(HttpRequest request, HttpResponse response) throws Exception {
        WebParameter p = request.getOctetStream();
        System.out.println(p.getName());
        System.out.println(p.getValue());
        File output = new File("c://octet.txt");
        p.getFile(output);
        return view;
    }
}
