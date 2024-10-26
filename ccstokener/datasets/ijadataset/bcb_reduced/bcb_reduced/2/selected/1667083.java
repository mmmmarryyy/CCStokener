package com.jguild.devportal.infrastructure.continuousintegration.hudson;

import com.jguild.devportal.infrastructure.InfrastructureConfiguration;
import com.jguild.devportal.infrastructure.InfrastructureManager;
import com.jguild.devportal.infrastructure.versioncontrol.VCRepository;
import com.jguild.devportal.infrastructure.versioncontrol.subversion.SVNRepositoryModuleCfg;
import com.jguild.devportal.project.Module;
import com.jguild.devportal.web.DisplayableException;
import com.jguild.devportal.workflow.SetupInfrastructureSimpleTask;
import org.apache.commons.io.IOUtils;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Simple tasks that creates a job in hudson for the specified module.
 */
public class HudsonSetupTask extends SetupInfrastructureSimpleTask<HudsonConfiguration> {

    private static final boolean svnSupported;

    private HudsonApp hudsonApp;

    static {
        boolean supported = true;
        try {
            Class.forName("com.jguild.devportal.infrastructure.versioncontrol.subversion.SVNRepository");
        } catch (ClassNotFoundException e) {
            supported = false;
        }
        svnSupported = supported;
    }

    public HudsonSetupTask(final InfrastructureManager infrastructureManager, final boolean activate, final HudsonApp hudsonApp, final HudsonConfiguration hudsonConfiguration) {
        super(hudsonConfiguration, activate);
        this.hudsonApp = hudsonApp;
    }

    /**
     * Return a short name that described what the task does.
     *
     * @return
     */
    public String getName() {
        return "Setup Hudson infrastructure";
    }

    protected void doSetup() throws Exception {
        final URL url = new URL(hudsonApp.getUrl() + (hudsonApp.getUrl().endsWith("/") ? "" : "/") + "createItem?name=" + hudsonApp.getJobName(infrastructureCfg.getModule()));
        final URLConnection con = url.openConnection();
        con.setDoOutput(true);
        con.setRequestProperty("Content-Type", "application/xml");
        final OutputStream os = con.getOutputStream();
        final String configXml = "<project>\n" + "  <builders/>\n" + "  <publishers class=\"vector\"/>\n" + "  <buildWrappers class=\"vector\"/>\n" + getVCXml() + "  <canRoam>true</canRoam>\n" + "  <disabled>false</disabled>\n" + "  <triggers class=\"vector\"/>\n" + "  <keepDependencies>false</keepDependencies>\n" + "  <properties/>\n" + "  <description></description>\n" + "  <actions class=\"java.util.concurrent.CopyOnWriteArrayList\"/>\n" + "</project>";
        IOUtils.write(configXml, os);
        os.close();
        final InputStream in = con.getInputStream();
        logInfo(IOUtils.toString(in));
        in.close();
    }

    private String getVCXml() {
        final Module module = infrastructureCfg.getModule();
        final InfrastructureConfiguration vcConfig = module.getInfrastructureConfig(VCRepository.CATEGORY);
        if (vcConfig == null) {
            logWarning("No version control repository found in the module.");
            return "";
        } else if (svnSupported && vcConfig instanceof SVNRepositoryModuleCfg) {
            final SVNRepositoryModuleCfg svn = (SVNRepositoryModuleCfg) vcConfig;
            String url = svn.getRepository().getReposDir(module).replace('\\', '/');
            if (!url.startsWith("/")) {
                url = "/" + url;
            }
            if (url.endsWith("/")) {
                url = url + "/";
            }
            url = "file://" + url + svn.getTrunkPath();
            return "<scm class=\"hudson.scm.SubversionSCM\">\n" + "    <locations>\n" + "      <hudson.scm.SubversionSCM_-ModuleLocation>\n" + "        <remote>" + url + "</remote>\n" + "        <local/>\n" + "      </hudson.scm.SubversionSCM_-ModuleLocation>\n" + "    </locations>\n" + "    <useUpdate>true</useUpdate>\n" + "  </scm>\n";
        } else {
            throw new DisplayableException("Unsupported version control system: " + vcConfig.getApplication().getType().getName());
        }
    }
}
