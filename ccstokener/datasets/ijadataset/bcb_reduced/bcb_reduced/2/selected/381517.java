package org.openeye.web;

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
import org.jboss.seam.bpm.ManagedJbpmContext;
import org.jbpm.file.def.FileDefinition;
import org.jbpm.graph.def.ProcessDefinition;
import org.jbpm.graph.exe.ProcessInstance;
import org.jbpm.util.XmlUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class IncludeDefinitionFileHandler extends TagHandler {

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

    public IncludeDefinitionFileHandler(TagConfig config) {
        super(config);
    }

    public void apply(FaceletContext ctx, UIComponent parent) throws IOException, FacesException, FaceletException, ELException {
        String file;
        ProcessDefinition processDef = null;
        FileDefinition fileDefinition;
        javax.el.VariableMapper orig;
        ValueExpression processExpression = processAttribute.getValueExpression(ctx, String.class);
        if (processExpression != null) {
            String definition = (String) processExpression.getValue(ctx);
            processDef = ManagedJbpmContext.instance().getGraphSession().findLatestProcessDefinition(definition);
        }
        if ((processDef != null)) {
            if (processDef == null) {
                throw new TagException(tag, "Value for process attribute is null");
            }
            fileDefinition = processDef.getFileDefinition();
            if (fileDefinition == null) {
                throw new TagException(tag, "Process has a null fileDefinition property");
            }
            file = getFormXhtml(fileDefinition);
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
            buffer.append(processDef.getId());
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
    }

    private String getFormXhtml(FileDefinition fileDefinition) {
        if (!fileDefinition.hasFile("forms.xml")) {
            throw new TagException(tag, "forms.xml dont exist");
        }
        java.io.InputStream inputStream = fileDefinition.getInputStream("forms.xml");
        Document document = XmlUtil.parseXmlInputStream(inputStream);
        Element documentElement = document.getDocumentElement();
        NodeList nodeList = documentElement.getElementsByTagName("form");
        if ((nodeList != null) && (nodeList.getLength() > 0)) {
            Element element = (Element) nodeList.item(0);
            return element.getAttribute("form");
        }
        return null;
    }
}
