package org.japano.pagenode.jstl.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import javax.servlet.ServletContext;
import org.japano.Buffer;
import org.japano.Context;
import org.japano.PageNode;
import org.japano.Request;
import org.japano.util.Util;

/**
 Retrieves an absolute or relative URL and exposes its contents to either the page, a String in 'var', or a Reader in 'varReader'.

 @author Sven Helmberger ( sven dot helmberger at gmx dot de )
 @version $Id: Import.java,v 1.3 2005/09/27 21:30:51 fforw Exp $
 #SFLOGO# 
 
 @japano.tag library="/japano/jstl/core" 
 */
public class Import extends PageNode {

    private String url;

    private String var;

    private String varReader;

    private String scope;

    private String contextURI;

    private String charEncoding;

    /**
   The URL of the resource to import.   
   
   @japano.attribute required="true"
   */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
   Name of the exported scoped variable for the resource's content. 
   The type of the scoped variable is String.

   @japano.attribute rtExprValue="false"
   */
    public void setVar(String var) {
        this.var = var;
    }

    /**
   Name of the exported scoped variable for the resource's content. 
   The type of the scoped variable is Reader.
   
   @japano.attribute rtExprValue="false"
   */
    public void setVarReader(String varReader) {
        this.varReader = varReader;
    }

    /**
   Scope for var.

   @japano.attribute rtExprValue="false"
   */
    public void setScope(String scope) {
        this.scope = scope;
    }

    /**
   Name of the context when accessing a relative URL resource that belongs to a foreign context.
   
   @japano.attribute
   */
    public void setContext(String contextURI) {
        this.contextURI = contextURI;
    }

    /**
   Character encoding of the content at the input resource.
    
   @japano.attribute
   */
    public void setCharEncoding(String charEncoding) {
        this.charEncoding = charEncoding;
    }

    public void generate(Buffer out) {
        BufferedReader br = null;
        try {
            String url = Url.createURL(this.url, contextURI, context);
            String query = Url.getQueryString(children, context);
            if (query.length() > 0) url += query;
            if (!Util.isAbsoluteURL(url)) {
                StringBuffer buf = ((Request) context.getRequest()).getServerBaseURL();
                buf.append(url);
                url = buf.toString();
            }
            if (charEncoding != null) {
                br = new BufferedReader(new InputStreamReader(new URL(url).openStream(), charEncoding));
            } else {
                br = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            }
            int scope = Context.getScopeFromName(this.scope);
            if (var != null) {
                StringBuffer buf = new StringBuffer();
                int c;
                while ((c = br.read()) > -1) {
                    buf.append((char) c);
                }
                context.setAttribute(var, buf.toString());
            } else {
                if (varReader == null) throw new IllegalStateException("Neither var nor varName attribute set.");
                context.setAttribute(var, br);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException("error reading resourse reader", e);
        } finally {
            try {
                if (br != null) br.close();
            } catch (IOException e) {
                throw new RuntimeException("Error closing resourse reader", e);
            }
        }
    }
}
