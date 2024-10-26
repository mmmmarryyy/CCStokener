package com.hp.hpl.jena.n3;

import com.hp.hpl.jena.graph.GraphEvents;
import com.hp.hpl.jena.rdf.model.*;
import java.net.*;
import java.io.*;
import com.hp.hpl.jena.shared.*;

/**
 * @author		Andy Seaborne
 * @version 	$Id: N3JenaReader.java,v 1.16 2006/03/22 13:53:26 andy_seaborne Exp $
 */
public class N3JenaReader implements RDFReader {

    RDFErrorHandler errorHandler = null;

    N3toRDF converter = null;

    public N3JenaReader() {
        converter = new N3toRDF();
    }

    public void read(Model model, Reader r, String base) {
        read(model, r, base, null);
    }

    public void read(Model model, java.lang.String url) {
        try {
            URLConnection conn = new URL(url).openConnection();
            String encoding = conn.getContentEncoding();
            if (encoding == null) read(model, new InputStreamReader(conn.getInputStream()), url, url); else read(model, new InputStreamReader(conn.getInputStream(), encoding), url, url);
        } catch (JenaException e) {
            if (errorHandler == null) throw e;
            errorHandler.error(e);
        } catch (Exception ex) {
            if (errorHandler == null) throw new JenaException(ex);
            errorHandler.error(ex);
        }
    }

    public void read(Model model, Reader r, String base, String sourceName) {
        try {
            model.notifyEvent(GraphEvents.startRead);
            converter.setBase(base);
            converter.setModel(model);
            N3Parser p = new N3Parser(r, converter);
            p.parse();
        } catch (JenaException e) {
            if (errorHandler == null) throw e;
            errorHandler.error(e);
        } catch (Exception ex) {
            if (errorHandler == null) throw new JenaException(ex);
            errorHandler.error(ex);
        } finally {
            model.notifyEvent(GraphEvents.finishRead);
        }
    }

    public void read(Model model, InputStream in, String base) {
        read(model, in, base, null);
    }

    public void read(Model model, InputStream in, String base, String sourceName) {
        try {
            model.notifyEvent(GraphEvents.startRead);
            converter.setBase(base);
            converter.setModel(model);
            N3Parser p = new N3Parser(in, converter);
            p.parse();
        } catch (JenaException e) {
            if (errorHandler == null) throw e;
            errorHandler.error(e);
        } catch (Exception ex) {
            if (errorHandler == null) throw new JenaException(ex);
            errorHandler.error(ex);
        } finally {
            model.notifyEvent(GraphEvents.finishRead);
        }
    }

    public RDFErrorHandler setErrorHandler(RDFErrorHandler errHandler) {
        RDFErrorHandler old = errorHandler;
        errorHandler = errHandler;
        return old;
    }

    public Object setProperty(String propName, Object propValue) {
        return null;
    }
}
