package org.danann.cernunnos.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.IOUtils;
import org.danann.cernunnos.AbstractContainerTask;
import org.danann.cernunnos.Attributes;
import org.danann.cernunnos.EntityConfig;
import org.danann.cernunnos.Formula;
import org.danann.cernunnos.Reagent;
import org.danann.cernunnos.ResourceHelper;
import org.danann.cernunnos.SimpleFormula;
import org.danann.cernunnos.TaskRequest;
import org.danann.cernunnos.TaskResponse;

public final class ArchiveIteratorTask extends AbstractContainerTask {

    private final ResourceHelper resource = new ResourceHelper();

    public Formula getFormula() {
        Reagent[] reagents = new Reagent[] { ResourceHelper.CONTEXT_TARGET, ResourceHelper.LOCATION_TASK, AbstractContainerTask.SUBTASKS };
        final Formula rslt = new SimpleFormula(ArchiveIteratorTask.class, reagents);
        return rslt;
    }

    public void init(EntityConfig config) {
        super.init(config);
        resource.init(config);
    }

    public void perform(TaskRequest req, TaskResponse res) {
        URL url = resource.evaluate(req, res);
        ZipInputStream zip = null;
        try {
            final String urlExternalForm = url.toExternalForm();
            try {
                zip = new ZipInputStream(new BufferedInputStream(url.openStream()));
            } catch (IOException ioe) {
                throw new RuntimeException("Failed to open input stream for URL: " + urlExternalForm, ioe);
            }
            res.setAttribute(Attributes.CONTEXT, "jar:" + urlExternalForm + "!/");
            try {
                for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    res.setAttribute(Attributes.LOCATION, entry.getName());
                    super.performSubtasks(req, res);
                    zip.closeEntry();
                }
            } catch (IOException ioe) {
                throw new RuntimeException("Failed to read specified archive:  " + urlExternalForm, ioe);
            }
        } finally {
            IOUtils.closeQuietly(zip);
        }
    }
}
