package com.griddynamics.convergence.demo.dar;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.handler.ResourceHandler;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;

public class EmbebedServer {

    private int port = 80;

    private String welcomePage = "DataAwareDemo.html";

    private String servletPath = "DemoService";

    private Servlet gwtServlet;

    private Server server;

    public void setPort(int port) {
        this.port = port;
    }

    public void setWelcomePage(String welcomePage) {
        this.welcomePage = welcomePage;
    }

    public void setServletPath(String servletPath) {
        this.servletPath = servletPath;
    }

    public void setGwtServlet(Servlet gwtServlet) {
        this.gwtServlet = gwtServlet;
    }

    public void start() throws Exception {
        server = new Server(port);
        final ServletHandler servletHandler = new ServletHandler() {

            @Override
            public void handle(String target, HttpServletRequest request, HttpServletResponse response, int type) throws IOException, ServletException {
                if (!response.isCommitted()) {
                    super.handle(target, request, response, type);
                }
            }
        };
        servletHandler.addServletWithMapping(new ServletHolder(gwtServlet), "/" + servletPath);
        final ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setBaseResource(new ClassPathResource(""));
        resourceHandler.setWelcomeFiles(new String[] { welcomePage });
        ContextHandler context = new ContextHandler();
        context.setContextPath("/");
        context.setBaseResource(new ClassPathResource(""));
        HandlerCollection handlers = new HandlerCollection();
        handlers.addHandler(resourceHandler);
        handlers.addHandler(servletHandler);
        context.addHandler(handlers);
        server.addHandler(context);
        server.start();
    }

    public void join() throws Exception {
        server.join();
    }

    @SuppressWarnings("serial")
    private static class ClassPathResource extends Resource {

        private String path;

        public ClassPathResource(String path) {
            this.path = path;
        }

        @Override
        public Resource addPath(String path) throws IOException, MalformedURLException {
            return new ClassPathResource(this.path + path);
        }

        @Override
        public boolean delete() throws SecurityException {
            throw new SecurityException("Rename not supported");
        }

        @Override
        public boolean exists() {
            URL url = getClass().getResource(path);
            if (url == null) {
                System.out.println("Missing resource [" + path + "]");
            }
            return url != null;
        }

        @Override
        public File getFile() throws IOException {
            return null;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            if (!isDirectory()) {
                URL url = getClass().getResource(path);
                return url.openStream();
            } else {
                throw new IOException("Cannot list");
            }
        }

        @Override
        public String getName() {
            return path;
        }

        @Override
        public OutputStream getOutputStream() throws IOException, SecurityException {
            throw new IOException("Output not supported");
        }

        @Override
        public URL getURL() {
            return getClass().getResource(path);
        }

        @Override
        public boolean isDirectory() {
            return path.endsWith("/");
        }

        @Override
        public long lastModified() {
            return -1;
        }

        @Override
        public long length() {
            return -1;
        }

        @Override
        public String[] list() {
            return null;
        }

        @Override
        public void release() {
        }

        @Override
        public boolean renameTo(Resource dest) throws SecurityException {
            throw new SecurityException("Rename not supported");
        }
    }
}
