package net.sf.buildbox.worker.maven;

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

public class MavenWorkerPluginTest {

    private File workdir;

    private final Map<String, String> jobParams = new HashMap<String, String>();

    @Before
    public void setUp() throws IOException {
        final URL url = getClass().getResource("maventest.properties.xml");
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
    public void runMvnAsAcceptedJob() throws Exception {
        final long executionId = 1;
        final JobId jobId = new JobId("test:/SvnFetchWorkerPluginTest/runAsAcceptedJob", "build");
        final WorkerJob workerJob = WorkerJob.create(jobId, "clazz:" + MavenWorkerPlugin.class.getName(), executionId);
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
