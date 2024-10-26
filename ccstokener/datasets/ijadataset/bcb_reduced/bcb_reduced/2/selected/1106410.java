package googlecode.maven;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.maven.artifact.manager.WagonManager;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.codehaus.plexus.PlexusConstants;
import org.codehaus.plexus.PlexusContainer;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.codehaus.plexus.context.Context;
import org.codehaus.plexus.context.ContextException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Contextualizable;

/**
 * This maven plugin will upload the current artifact into its googlecode
 * download area location.
 * <p>
 * It is intended to run after the install goal in the maven lifecycle and
 * publish the artifact archive to the googlecode project area.
 * </p>
 * <p>
 * The uploaded file name will be the same as the actual artifact-version
 * standard name.
 * </p>
 * 
 * @goal deploy
 * @phase install
 * @requiresProject true
 * @author amarion
 */
public class GooglecodeDeployMojo extends AbstractMojo implements Contextualizable {

    /**
	 * Pattern of the <code>googlecode</code> upload <code>URL</code>.
	 */
    private static final MessageFormat GOOGLECODE_URL_PATTERN = new MessageFormat("http://uploads.code.google.com/upload/{0}");

    /**
	 * The Maven Project.
	 * 
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 */
    protected MavenProject project;

    /**
	 * Contextualized.
	 */
    private PlexusContainer container;

    /**
	 * Google project name.
	 * 
	 * @parameter expression="${googlecode-name}"
	 */
    private String projectName;

    /**
	 * Google project upload summary.
	 * 
	 * @parameter
	 */
    private String summary;

    /**
	 * Labels tags for the uploaded file.
	 * 
	 * @parameter
	 */
    private List<String> labels;

    /**
	 * Site to deploy to.
	 * 
	 * @parameter
	 */
    private Site site;

    /**
	 * True for testing purposes (do not actually make the upload).
	 * 
	 * @parameter default-value="false"
	 */
    private boolean test;

    /**
	 * {@inheritDoc}
	 */
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            URL url = getUploadUrl();
            HttpEntity body = getRequestBody();
            WagonManager manager = (WagonManager) container.lookup(WagonManager.ROLE);
            DefaultHttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(url.toExternalForm());
            post.setEntity(body);
            getLog().info(post.getRequestLine().toString());
            if (site != null && site.getId() != null) {
                AuthenticationInfo info = manager.getAuthenticationInfo(site.getId());
                if (info != null) {
                    getLog().info("Using user " + info.getUserName());
                    client.getCredentialsProvider().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(info.getUserName(), info.getPassword()));
                }
            }
            if (!test) {
                HttpResponse response = client.execute(post);
                if (response.getStatusLine().getStatusCode() != 200) {
                    throw new MojoExecutionException("Http Transfer failed." + response.getStatusLine());
                }
                client.getConnectionManager().shutdown();
            }
        } catch (UnsupportedEncodingException e) {
            throw new MojoExecutionException("Cannot build http request.", e);
        } catch (ComponentLookupException e) {
            throw new MojoExecutionException("Cannot find the wagon manager.", e);
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Cannot build http request.", e);
        } catch (ClientProtocolException e) {
            throw new MojoExecutionException("Http Transfer failed.", e);
        } catch (IOException e) {
            throw new MojoExecutionException("Http Transfer failed.", e);
        }
    }

    /**
	 * Build the request body.
	 * 
	 * @return the HttpEntry corresponding the request.
	 * @throws UnsupportedEncodingException
	 *             when an unsupported encoding has been used.
	 * @throws MojoExecutionException
	 *             when the mojo is used with a pom that do not have file
	 *             generation.
	 */
    public HttpEntity getRequestBody() throws UnsupportedEncodingException, MojoExecutionException {
        MultipartEntity root = new MultipartEntity();
        if (summary != null) {
            root.addPart("summary", new StringBody(summary));
        }
        root.addPart("file", new FileBody(getFileToUpload()));
        if (labels != null) {
            for (String label : labels) {
                root.addPart("label", new StringBody(label));
            }
        }
        return root;
    }

    /**
	 * Gets the file to upload.
	 * 
	 * @return the file that will be uploaded.
	 * @throws MojoExecutionException
	 *             when the mojo is used with a pom that do not have file
	 *             generation.
	 */
    public File getFileToUpload() throws MojoExecutionException {
        File file = project.getArtifact().getFile();
        if (file == null || file.isDirectory()) {
            throw new MojoExecutionException("The packaging for this project did not assign a file to the build artifact");
        }
        getLog().info("File to upload: " + file.getPath());
        return file;
    }

    /**
	 * Compute the project upload url. This is either the alternate url, or the
	 * computed url using default google upload url with project name.
	 * 
	 * @return the url to upload the project.
	 * @throws MalformedURLException
	 */
    public URL getUploadUrl() throws MalformedURLException {
        URL url = null;
        if (site != null && site.getUrl() != null) {
            url = site.getUrl();
            getLog().info("Using alternate url for upload: " + url);
        } else {
            url = new URL(GOOGLECODE_URL_PATTERN.format(new String[] { getProjectName() }));
            getLog().info("Using google url for upload: " + url);
        }
        return url;
    }

    /**
	 * Looking up for project name. Will first look up for googlecode-name
	 * parameter. If the google name is not set, will then look up for
	 * artifactId or root artifactId (multi-module projects).
	 * 
	 * @return the name of the project.
	 */
    public String getProjectName() {
        if (projectName == null) {
            return getRootProject().getArtifactId();
        }
        return projectName;
    }

    /**
	 * Gets the root project of the provided artifact.
	 * 
	 * @return the root project.
	 */
    private MavenProject getRootProject() {
        return getRootProject(project);
    }

    /**
	 * Gets the root project for the provided project.
	 * 
	 * @param project
	 *            the project to get the root from.
	 * @return the root project of the provided project.
	 */
    private MavenProject getRootProject(MavenProject project) {
        if (project.hasParent()) {
            return getRootProject(project.getParent());
        } else {
            return project;
        }
    }

    /**
	 * {@inheritDoc}
	 */
    public void contextualize(Context context) throws ContextException {
        this.container = (PlexusContainer) context.get(PlexusConstants.PLEXUS_KEY);
    }
}
