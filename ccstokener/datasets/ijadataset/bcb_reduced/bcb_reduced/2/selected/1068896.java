package org.argouml.persistence;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URL;
import org.argouml.kernel.Project;
import org.argouml.kernel.ProjectMember;
import org.argouml.uml.diagram.ProjectMemberDiagram;
import org.tigris.gef.base.Diagram;
import org.tigris.gef.ocl.ExpansionException;
import org.tigris.gef.ocl.OCLExpander;
import org.tigris.gef.ocl.TemplateReader;

/**
 * The file persister for the diagram members.
 * @author Bob Tarling
 */
class DiagramMemberFilePersister extends MemberFilePersister {

    /**
     * The tee file for persistence.
     */
    private static final String PGML_TEE = "/org/argouml/persistence/PGML.tee";

    @Override
    public void load(Project project, InputStream inputStream) throws OpenException {
        try {
            PGMLStackParser parser = new PGMLStackParser(project.getUUIDRefs());
            Diagram d = parser.readDiagram(inputStream, false);
            inputStream.close();
            project.addMember(d);
        } catch (Exception e) {
            if (e instanceof OpenException) {
                throw (OpenException) e;
            }
            throw new OpenException(e);
        }
    }

    @Override
    public void load(Project project, URL url) throws OpenException {
        try {
            load(project, url.openStream());
        } catch (IOException e) {
            throw new OpenException(e);
        }
    }

    @Override
    public String getMainTag() {
        return "pgml";
    }

    @Override
    @Deprecated
    public void save(ProjectMember member, Writer writer, boolean xmlFragment) throws SaveException {
        ProjectMemberDiagram diagramMember = (ProjectMemberDiagram) member;
        OCLExpander expander;
        try {
            expander = new OCLExpander(TemplateReader.getInstance().read(PGML_TEE));
        } catch (ExpansionException e) {
            throw new SaveException(e);
        }
        if (!xmlFragment) {
            try {
                expander.expand(writer, diagramMember.getDiagram());
            } catch (ExpansionException e) {
                throw new SaveException(e);
            }
        } else {
            try {
                File tempFile = File.createTempFile("pgml", null);
                tempFile.deleteOnExit();
                FileWriter w = new FileWriter(tempFile);
                expander.expand(w, diagramMember.getDiagram());
                w.close();
                addXmlFileToWriter((PrintWriter) writer, tempFile);
            } catch (ExpansionException e) {
                throw new SaveException(e);
            } catch (IOException e) {
                throw new SaveException(e);
            }
        }
    }

    @Override
    public void save(ProjectMember member, OutputStream outStream) throws SaveException {
        ProjectMemberDiagram diagramMember = (ProjectMemberDiagram) member;
        OCLExpander expander;
        try {
            expander = new OCLExpander(TemplateReader.getInstance().read(PGML_TEE));
        } catch (ExpansionException e) {
            throw new SaveException(e);
        }
        PrintWriter pw = new PrintWriter(outStream);
        try {
            expander.expand(pw, diagramMember.getDiagram());
        } catch (ExpansionException e) {
            throw new SaveException(e);
        } finally {
            pw.flush();
        }
    }
}
