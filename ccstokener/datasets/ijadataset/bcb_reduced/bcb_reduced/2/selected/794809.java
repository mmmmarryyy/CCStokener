package xbrowser.renderer.custom;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;

public class XFormView extends FormView {

    public XFormView(Element elem) {
        super(elem);
    }

    protected void submitData(String data) {
        (new XSubmitThread(getElement(), data)).start();
    }

    /**
	 * The SubmitThread is responsible for submitting the form.
	 * It performs a POST or GET based on the value of method
	 * attribute associated with  HTML.Tag.FORM.  In addition to
	 * submitting, it is also responsible for display the
	 * results of the form submission.
	 */
    class XSubmitThread extends Thread {

        public XSubmitThread(Element elem, String data) {
            this.data = data;
            hdoc = (HTMLDocument) elem.getDocument();
            formAttr = getFormAttributes(elem.getAttributes());
        }

        private AttributeSet getFormAttributes(AttributeSet attr) {
            Enumeration names = attr.getAttributeNames();
            while (names.hasMoreElements()) {
                Object name = names.nextElement();
                if (name instanceof HTML.Tag) {
                    HTML.Tag tag = (HTML.Tag) name;
                    if (tag == HTML.Tag.FORM) {
                        Object o = attr.getAttribute(tag);
                        if (o != null && o instanceof AttributeSet) return (AttributeSet) o;
                    }
                }
            }
            return null;
        }

        /**
		 * This method is responsible for extracting the
		 * method and action attributes associated with the
		 * &lt;FORM&gt; and using those to determine how (POST or GET)
		 * and where (URL) to submit the form.  If action is
		 * not specified, the base url of the existing document is
		 * used.  Also, if method is not specified, the default is
		 * GET.  Once form submission is done, run uses the
		 * SwingUtilities.invokeLater() method, to load the results
		 * of the form submission into the current JEditorPane.
		 */
        public void run() {
            if (data.length() > 0) {
                String method = getMethod();
                String action = getAction();
                URL url;
                try {
                    URL actionURL;
                    URL baseURL = hdoc.getBase();
                    if (action == null) {
                        String file = baseURL.getFile();
                        actionURL = new URL(baseURL.getProtocol(), baseURL.getHost(), baseURL.getPort(), file);
                    } else actionURL = new URL(baseURL, action);
                    URLConnection connection;
                    if ("post".equalsIgnoreCase(method)) {
                        url = actionURL;
                        connection = url.openConnection();
                        ((HttpURLConnection) connection).setFollowRedirects(false);
                        XRendererSupport.setCookies(url, connection);
                        connection.setRequestProperty("Accept-Language", "en-us");
                        connection.setRequestProperty("User-Agent", XRendererSupport.getContext().getUserAgent());
                        postData(connection, data);
                        XRendererSupport.getContext().getLogger().message(this, "Posted data: {" + data + "}");
                    } else {
                        url = new URL(actionURL + "?" + data);
                        connection = url.openConnection();
                        XRendererSupport.setCookies(url, connection);
                    }
                    connection.connect();
                    in = connection.getInputStream();
                    URL base = connection.getURL();
                    XRendererSupport.getCookies(base, connection);
                    XRendererSupport.getContext().getLogger().message(this, "Stream got ok.");
                    JEditorPane c = (JEditorPane) getContainer();
                    HTMLEditorKit kit = (HTMLEditorKit) c.getEditorKit();
                    newDoc = (HTMLDocument) kit.createDefaultDocument();
                    newDoc.putProperty(Document.StreamDescriptionProperty, base);
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            XRendererSupport.getContext().getLogger().message(this, "loading...");
                            loadDocument();
                            XRendererSupport.getContext().getLogger().message(this, "document loaded...");
                        }
                    });
                } catch (MalformedURLException m) {
                } catch (IOException e) {
                }
            }
        }

        /**
		 * This method is responsible for loading the
		 * document into the FormView's container,
		 * which is a JEditorPane.
		 */
        public void loadDocument() {
            JEditorPane c = (JEditorPane) getContainer();
            try {
                c.read(in, newDoc);
            } catch (IOException e) {
            }
        }

        /**
		 * Get the value of the action attribute.
		 */
        public String getAction() {
            if (formAttr == null) return null;
            return (String) formAttr.getAttribute(HTML.Attribute.ACTION);
        }

        /**
		 * Get the form's method parameter.
		 */
        String getMethod() {
            if (formAttr != null) {
                String method = (String) formAttr.getAttribute(HTML.Attribute.METHOD);
                if (method != null) return method.toLowerCase();
            }
            return null;
        }

        /**
		 * This method is responsible for writing out the form submission
		 * data when the method is POST.
		 *
		 * @param connection to use.
		 * @param data to write.
		 */
        public void postData(URLConnection connection, String data) {
            connection.setDoOutput(true);
            PrintWriter out = null;
            try {
                out = new PrintWriter(new OutputStreamWriter(connection.getOutputStream()));
                out.print(data);
                out.flush();
            } catch (IOException e) {
            } finally {
                if (out != null) out.close();
            }
        }

        private String data;

        private HTMLDocument hdoc;

        private HTMLDocument newDoc;

        private AttributeSet formAttr;

        private InputStream in;
    }
}
