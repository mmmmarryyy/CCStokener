package org.openeye.web;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import com.sun.facelets.FaceletContext;
import com.sun.facelets.FaceletException;
import com.sun.facelets.el.VariableMapperWrapper;
import com.sun.facelets.tag.*;
import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import org.jboss.seam.bpm.ManagedJbpmContext;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class IncludeProcessFileHandler extends TagHandler {

    private final TagAttribute processAttribute = getRequiredAttribute("process");

    public IncludeProcessFileHandler(TagConfig config) {
        super(config);
    }

    public void apply(FaceletContext context, UIComponent parent) throws IOException, FacesException, FaceletException, ELException {
        ValueExpression processExpression = processAttribute.getValueExpression(context, ProcessInstance.class);
        ProcessInstance processIns = (ProcessInstance) processExpression.getValue(context);
        if (processIns != null) {
            processIns = ManagedJbpmContext.instance().getGraphSession().getProcessInstance(processIns.getId());
            if (processIns == null) {
                throw new TagException(tag, "Value for process attribute is null");
            }
            FileDefinition fileDefinition = processIns.getProcessDefinition().getFileDefinition();
            if (fileDefinition == null) {
                throw new TagException(tag, "Process has a null fileDefinition property");
            }
            String file = getFormXhtml(fileDefinition, processIns.getRootToken().getNode().getName());
            if (file == null || file.isEmpty()) {
                return;
            }
            if (!fileDefinition.hasFile(file)) {
                throw new TagException(tag, "Process does not contain file '" + file + "'");
            }
            String filePath = processIns.getProcessDefinition().getId() + "/" + file;
            javax.el.VariableMapper orig = context.getVariableMapper();
            VariableMapperWrapper newVarMapper = new VariableMapperWrapper(orig);
            context.setVariableMapper(newVarMapper);
            try {
                nextHandler.apply(context, parent);
                URL url = new URL("par", "", 0, filePath, new FileDefinitionURLStreamHandler(fileDefinition, file));
                context.includeFacelet(parent, url);
            } finally {
                context.setVariableMapper(orig);
            }
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

    private static final class FileDefinitionURLStreamHandler extends URLStreamHandler {

        private final FileDefinition fileDefinition;

        private final String src;

        public FileDefinitionURLStreamHandler(FileDefinition fileDefinition, String src) {
            this.fileDefinition = fileDefinition;
            this.src = src;
        }

        protected URLConnection openConnection(URL url) {
            return new FileDefinitionURLConnection(url, fileDefinition, src);
        }
    }

    private static final class FileDefinitionURLConnection extends URLConnection {

        private final FileDefinition fileDefinition;

        private final String src;

        protected FileDefinitionURLConnection(URL url, FileDefinition fileDefinition, String src) {
            super(url);
            this.fileDefinition = fileDefinition;
            this.src = src;
        }

        public void connect() {
        }

        public InputStream getInputStream() throws FileNotFoundException {
            InputStream inputStream = fileDefinition.getInputStream(src);
            if (inputStream == null) {
                throw new FileNotFoundException("File '" + src + "' not found in process file definition");
            } else {
                return inputStream;
            }
        }
    }
}
