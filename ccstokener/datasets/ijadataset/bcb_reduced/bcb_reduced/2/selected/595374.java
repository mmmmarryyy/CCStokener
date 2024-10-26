package net.sf.buildbox.worker.subversion;

import net.sf.buildbox.reactor.model.JobId;
import net.sf.buildbox.reactor.model.WorkerJob;
import net.sf.buildbox.reactor.model.WorkerJobParams;
import net.sf.buildbox.worker.StdoutFeedback;
import net.sf.buildbox.worker.api.ExecutionContext;
import net.sf.buildbox.worker.impl.AcceptedJobRunner;
import net.sf.buildbox.worker.impl.WorkerPluginFactory;
import org.codehaus.plexus.util.FileUtils;
import org.junit.Before;
import org.junit.Test;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class SvnFetchWorkerPluginTest {

    private File workdir;

    private final Map<String, String> jobParams = new HashMap<String, String>();

    @Before
    public void setUp() throws IOException {
        final URL url = getClass().getResource("svntest.properties");
        workdir = new File(new File(url.getPath()).getParentFile(), "workdir");
        final Properties properties = new Properties();
        properties.load(url.openStream());
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            jobParams.put(entry.getKey() + "", entry.getValue() + "");
        }
        FileUtils.deleteDirectory(workdir);
        workdir.mkdirs();
    }

    @Test
    public void runDirectly() throws Exception {
        final SvnFetchWorkerPlugin plugin = new SvnFetchWorkerPlugin();
        plugin.ecCache = new File(workdir, "ecCache");
        plugin.ecCode = new File(workdir, "ecCode");
        plugin.svnRootUrl = jobParams.get("pp.svnRootUrl");
        plugin.revision = jobParams.get("pp.revision");
        plugin.locationPath = jobParams.get("pp.locationPath");
        plugin.execute(new StdoutFeedback());
    }

    /**
     * This is an attempt to define simplest possible api to any plugin execution
     * @param workerJob  meta (executionId + project + symbolic command) name + what
     * @param properties how - params of the functionality
     * @param workdir  where - dedicated area for sandboxes etc
     * @return result code
     */
    private AcceptedJobRunner createPlugin(WorkerJob workerJob, Map<String, String> properties, File workdir) {
        return null;
    }

    @Test
    public void runAsAcceptedJob() throws Exception {
        final long executionId = 1;
        final JobId jobId = new JobId("test:/SvnFetchWorkerPluginTest/runAsAcceptedJob", "fetch");
        final WorkerJob workerJob = WorkerJob.create(jobId, "clazz:" + SvnFetchWorkerPlugin.class.getName(), executionId);
        final WorkerJobParams workerJobParams = new WorkerJobParams();
        workerJobParams.setSimpleParams(jobParams);
        final ExecutionContext ec = new ExecutionContext(workdir, jobId, executionId);
        final AcceptedJobRunner acceptedJobRunner = new AcceptedJobRunner();
        acceptedJobRunner.setWorkerPluginFactory(new WorkerPluginFactory());
        acceptedJobRunner.setWorkerFeedback(new StdoutFeedback());
        acceptedJobRunner.setupJob(workerJob, workerJobParams, ec, Collections.<String, String>emptyMap());
        acceptedJobRunner.call();
    }
}
