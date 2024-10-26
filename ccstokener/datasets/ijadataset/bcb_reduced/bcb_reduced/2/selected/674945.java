package org.manaty.jbpmconsole;

import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.*;
import java.io.*;
import java.net.*;
import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import org.jboss.seam.bpm.ManagedJbpmContext;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * @author cuijie
 *
 */
public class IncludeProcessFileHandler extends TagHandler {

    private static final class FileDefinitionURLConnection extends URLConnection {

        private final FileDefinition fileDefinition;

        private final String src;

        public void connect() {
        }

        public InputStream getInputStream() throws FileNotFoundException {
            InputStream inputStream = fileDefinition.getInputStream(src);
            if (inputStream == null) {
                throw new FileNotFoundException((new StringBuilder("File '")).append(src).append("' not found in process file definition").toString());
            } else {
                return inputStream;
            }
        }

        protected FileDefinitionURLConnection(URL url, FileDefinition fileDefinition, String src) {
            super(url);
            this.fileDefinition = fileDefinition;
            this.src = src;
        }
    }

    private static final class FileDefinitionURLStreamHandler extends URLStreamHandler {

        private final FileDefinition fileDefinition;

        private final String src;

        protected URLConnection openConnection(URL url) {
            return new FileDefinitionURLConnection(url, fileDefinition, src);
        }

        public FileDefinitionURLStreamHandler(FileDefinition fileDefinition, String src) {
            this.fileDefinition = fileDefinition;
            this.src = src;
        }
    }

    private final TagAttribute processAttribute = getRequiredAttribute("process");

    public IncludeProcessFileHandler(TagConfig config) {
        super(config);
        System.out.println("--IncludeProcessFileHandler---");
        System.out.println("---nextHandler:" + nextHandler);
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException, ELException {
        String file;
        ProcessInstance processIns;
        FileDefinition fileDefinition;
        javax.el.VariableMapper orig;
        ValueExpression processExpression = processAttribute.getValueExpression(ctx, ProcessInstance.class);
        processIns = (ProcessInstance) processExpression.getValue(ctx);
        processIns = ManagedJbpmContext.instance().getGraphSession().getProcessInstance(processIns.getId());
        if (processIns == null) {
            throw new TagException(tag, "Value for process attribute is null");
        }
        fileDefinition = processIns.getProcessDefinition().getFileDefinition();
        if (fileDefinition == null) {
            throw new TagException(tag, "Process has a null fileDefinition property");
        }
        file = getFormXhtml(fileDefinition, processIns.getRootToken().getNode().getName());
        if (file == null || file.length() == 0) {
            return;
        }
        if (!fileDefinition.hasFile(file)) {
            throw new TagException(tag, (new StringBuilder("Process does not contain file '")).append(file).append("'").toString());
        }
        orig = ctx.getVariableMapper();
        VariableMapperWrapper newVarMapper = new VariableMapperWrapper(orig);
        ctx.setVariableMapper(newVarMapper);
        StringBuffer buffer = new StringBuffer();
        buffer.append(processIns.getProcessDefinition().getId());
        buffer.append("/");
        buffer.append(file);
        try {
            nextHandler.apply(ctx, parent);
            URL url = new URL("par", "", 0, buffer.toString(), new FileDefinitionURLStreamHandler(fileDefinition, file));
            ctx.includeFacelet(parent, url);
        } finally {
            ctx.setVariableMapper(orig);
        }
    }

    private String getFormXhtml(FileDefinition fileDefinition, String taskname) {
        if (!fileDefinition.hasFile("forms.xml")) {
            throw new TagException(tag, "forms.xml dont exist");
        }
        java.io.InputStream inputStream = fileDefinition.getInputStream("forms.xml");
        Document document = XmlUtil.parseXmlInputStream(inputStream);
        Element documentElement = document.getDocumentElement();
        NodeList nodeList = documentElement.getElementsByTagName("form");
        int length = nodeList.getLength();
        for (int i = 0; i < length; i++) {
            Element element = (Element) nodeList.item(i);
            String itemTaskName = element.getAttribute("task");
            String itemFormName = element.getAttribute("form");
            if (itemTaskName != null && itemFormName != null && itemTaskName.equals(taskname)) {
                return itemFormName;
            }
        }
        return null;
    }
}
