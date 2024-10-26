package org.nexopenframework.ide.eclipse.continuum;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Vector;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.maven.continuum.model.project.BuildDefinition;
import org.apache.maven.continuum.model.project.Project;
import org.apache.maven.continuum.model.project.ProjectDependency;
import org.apache.maven.continuum.model.project.ProjectGroup;
import org.apache.maven.continuum.model.project.ProjectNotifier;
import org.apache.maven.continuum.model.project.Schedule;
import org.nexopenframework.ide.eclipse.commons.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 * <p>NexOpen Framework</p>
 * 
 * <p>Class which holds the complexities of building a Maven2 project</p>
 * 
 * @author Francesc Xavier Magdaleno
 * @version 1.0
 * @since 1.0
 */
public final class ProjectBuilder {

    /**
	 * <p>IMPORTANT NOTE : we assume that it is a Maven2 project and the stream corresponds to
	 * a root <code>pom.xml</code></p>
	 * 
	 * @see Executor#MAVEN2_ID
	 * @param is
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
    @SuppressWarnings("unchecked")
    public static Project createProject(final InputStream is) throws IOException, ParserConfigurationException, SAXException {
        final Document doc = XMLUtils.getDocument(is);
        final Element root = doc.getDocumentElement();
        Project prj = new Project();
        prj.setExecutorId(Executor.MAVEN2_ID);
        final Element groupId = XMLUtils.getChildElementByTagName(root, "groupId");
        prj.setGroupId(groupId.getTextContent().trim());
        Element artifactId = XMLUtils.getChildElementByTagName(root, "artifactId");
        prj.setArtifactId(artifactId.getTextContent().trim());
        Element name = XMLUtils.getChildElementByTagName(root, "name");
        prj.setName(name.getTextContent().trim());
        Element version = XMLUtils.getChildElementByTagName(root, "version");
        prj.setVersion(version.getTextContent().trim());
        final ProjectGroup group = new ProjectGroup();
        group.setName(name.getTextContent().trim());
        group.setGroupId(groupId.getTextContent().trim());
        prj.setProjectGroup(group);
        Element scm = XMLUtils.getChildElementByTagName(root, "scm");
        Element scmUrl = XMLUtils.getChildElementByTagName(scm, "connection");
        prj.setScmUrl(scmUrl.getTextContent().trim());
        Element scmTag = XMLUtils.getChildElementByTagName(scm, "tag");
        if (scmTag != null) {
            prj.setScmTag(scmTag.getTextContent().trim());
        }
        {
            final BuildDefinition bd = new BuildDefinition();
            bd.setGoals("clean install");
            bd.setBuildFile("pom.xml");
            bd.setDefaultForProject(true);
            bd.setArguments("--batch-mode");
            final Schedule s = new Schedule();
            s.setName("Night builds for " + prj.getArtifactId());
            s.setDescription("Runs every night at 22:15");
            s.setCronExpression("0 15 22 * * ?");
            s.setActive(true);
            bd.setSchedule(s);
            final Vector<BuildDefinition> builds = new Vector<BuildDefinition>();
            builds.add(bd);
            prj.setBuildDefinitions(builds);
        }
        final Element elemDependencies = XMLUtils.getChildElementByTagName(root, "dependencies");
        final List<Element> dependencies = XMLUtils.getChildElementsByTagName(elemDependencies, "dependency");
        if (dependencies != null && !dependencies.isEmpty()) {
            List<ProjectDependency> projectDependencies = new Vector<ProjectDependency>();
            for (final Element elem : dependencies) {
                final ProjectDependency pd = new ProjectDependency();
                pd.setArtifactId(XMLUtils.getChildElementByTagName(elem, "artifactId").getTextContent());
                pd.setGroupId(XMLUtils.getChildElementByTagName(elem, "groupId").getTextContent());
                pd.setVersion(XMLUtils.getChildElementByTagName(elem, "version").getTextContent());
                projectDependencies.add(pd);
            }
            prj.setDependencies(projectDependencies);
        }
        final Element elemNotifiers = XMLUtils.getChildElementByTagName(root, "notifiers");
        final List<Element> notifiers = XMLUtils.getChildElementsByTagName(elemNotifiers, "notifier");
        if (notifiers != null && !notifiers.isEmpty()) {
            final List<ProjectNotifier> projectNotifiers = new Vector<ProjectNotifier>();
            for (final Element elem : notifiers) {
                final ProjectNotifier pd = new ProjectNotifier();
                pd.setEnabled(true);
                final Element type = XMLUtils.getChildElementByTagName(elem, "type");
                if (type != null) {
                    pd.setType(type.getTextContent());
                }
                final Element sendOnError = XMLUtils.getChildElementByTagName(elem, "sendOnError");
                if (sendOnError != null) {
                    pd.setSendOnError(Boolean.parseBoolean(sendOnError.getTextContent()));
                }
                final Element sendOnFailure = XMLUtils.getChildElementByTagName(elem, "sendOnFailure");
                if (sendOnFailure != null) {
                    pd.setSendOnFailure(Boolean.parseBoolean(sendOnFailure.getTextContent()));
                }
                final Element sendOnSuccess = XMLUtils.getChildElementByTagName(elem, "sendOnSuccess");
                if (sendOnSuccess != null) {
                    pd.setSendOnSuccess(Boolean.parseBoolean(sendOnSuccess.getTextContent()));
                }
                final Element sendOnWarning = XMLUtils.getChildElementByTagName(elem, "sendOnWarning");
                if (sendOnWarning != null) {
                    pd.setSendOnWarning(Boolean.parseBoolean(sendOnWarning.getTextContent()));
                }
                projectNotifiers.add(pd);
            }
            prj.setNotifiers(projectNotifiers);
        }
        return prj;
    }

    /**
	 * <p></p>
	 * 
	 * <p>IMPORTANT NOTE : we assume that it is a Maven2 project and the stream corresponds to
	 * a <code>pom.xml</code></p>
	 * 
	 * @see #createProject(InputStream)
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
    public static Project createProject(final URL url) throws IOException, ParserConfigurationException, SAXException {
        InputStream is = null;
        try {
            is = url.openStream();
            Project prj = createProject(is);
            return prj;
        } finally {
            if (is != null) {
                try {
                    is.close();
                    is = null;
                } catch (IOException e) {
                }
            }
        }
    }
}
