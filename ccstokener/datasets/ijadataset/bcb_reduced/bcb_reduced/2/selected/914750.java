package org.opendicomviewer.controller;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.dcm4che2.data.BasicDicomObject;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.dcm4che2.media.DicomDirReader;
import org.jdesktop.application.Action;
import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;
import org.jdesktop.application.Task;
import org.opendicomviewer.ApplicationContext;
import org.opendicomviewer.model.Instance;
import org.opendicomviewer.model.Patient;
import org.opendicomviewer.model.Series;
import org.opendicomviewer.model.Study;
import org.opendicomviewer.plugin.Plugin;
import org.opendicomviewer.util.ByteArrayDataSource;
import org.opendicomviewer.util.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);

    private ApplicationContext applicationContext;

    private JFileChooser fileChooser;

    private Map<String, Patient> patientMap;

    private Map<String, Study> studyMap;

    private Map<String, Series> seriesMap;

    private Map<String, Instance> instanceMap;

    private ResourceMap resourceMap;

    public FileController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        patientMap = Collections.synchronizedMap(new HashMap<String, Patient>());
        studyMap = Collections.synchronizedMap(new HashMap<String, Study>());
        seriesMap = Collections.synchronizedMap(new HashMap<String, Series>());
        instanceMap = Collections.synchronizedMap(new HashMap<String, Instance>());
        resourceMap = Application.getInstance().getContext().getResourceMap(FileController.class);
    }

    @Action
    public void openDicomDir() {
        try {
            if (fileChooser == null) {
                fileChooser = new JFileChooser();
            }
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(false);
            fileChooser.setFileFilter(new FileFilter() {

                @Override
                public String getDescription() {
                    return "DICOMDIR";
                }

                @Override
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().equals("DICOMDIR");
                }
            });
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                openDicomDir(fileChooser.getSelectedFile());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void openDicomDir(File dicomDir) {
        applicationContext.getViewContext().getDicomThumbsPane().getDicomThumbsModel().clear();
        patientMap.clear();
        studyMap.clear();
        seriesMap.clear();
        instanceMap.clear();
        LoadDicomDirTask task = new LoadDicomDirTask(Application.getInstance(), dicomDir);
        Application.getInstance().getContext().getTaskService().execute(task);
    }

    @Action
    public void openDicomFiles() {
        try {
            if (fileChooser == null) {
                fileChooser = new JFileChooser();
            }
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(true);
            fileChooser.setFileFilter(null);
            if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                for (File dicomFile : fileChooser.getSelectedFiles()) {
                    addDicomInstance(new FileDataSource(dicomFile));
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    @Action
    public void openDicomURL() {
        try {
            String sURI = JOptionPane.showInputDialog(resourceMap.getString("openDicomURL.Action.input"));
            if (sURI != null) {
                URI uri = new URI(sURI);
                openDicomURIs(new URI[] { uri });
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void openDicomURIs(URI[] uris) throws URISyntaxException {
        clear();
        for (URI uri : uris) {
            if (uri.getScheme().equals("file")) {
                File dicomFile = new File(uri);
                addDicomInstance(new FileDataSource(dicomFile));
            } else {
                DownloadDicomInstanceTask task = new DownloadDicomInstanceTask(Application.getInstance(), uri);
                Application.getInstance().getContext().getTaskService().execute(task);
            }
        }
    }

    private void clear() {
        applicationContext.getViewContext().getDicomThumbsPane().getDicomThumbsModel().clear();
        patientMap.clear();
        studyMap.clear();
        seriesMap.clear();
        instanceMap.clear();
    }

    private void addDicomInstance(DataSource dataSource) {
        LoadDicomInstanceTask task = new LoadDicomInstanceTask(Application.getInstance(), dataSource);
        Application.getInstance().getContext().getTaskService().execute(task);
    }

    private class DownloadDicomInstanceTask extends Task<ByteArrayDataSource, Void> {

        private URI uri;

        public DownloadDicomInstanceTask(Application application, URI uri) {
            super(application);
            this.uri = uri;
        }

        @Override
        protected ByteArrayDataSource doInBackground() throws Exception {
            setMessage("Descargando " + uri.toString());
            byte[] fileBuffer = null;
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet(uri);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                fileBuffer = new byte[(int) entity.getContentLength()];
                InputStream is = entity.getContent();
                byte[] buffer = new byte[4096];
                int count = 0;
                int globalCount = 0;
                while ((count = is.read(buffer)) > 0) {
                    System.arraycopy(buffer, 0, fileBuffer, globalCount, count);
                    globalCount += count;
                    setProgress(globalCount * 100 / (int) entity.getContentLength());
                }
                is.close();
            }
            return new ByteArrayDataSource(fileBuffer);
        }

        @Override
        protected void finished() {
            try {
                ByteArrayDataSource dataSource = get();
                addDicomInstance(dataSource);
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
                JOptionPane.showMessageDialog(null, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            } catch (ExecutionException e) {
                LOGGER.error(e.getMessage(), e);
                JOptionPane.showMessageDialog(null, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class LoadDicomDirTask extends Task<File[], Void> {

        private File dicomDir;

        public LoadDicomDirTask(Application application, File dicomDir) {
            super(application);
            this.dicomDir = dicomDir;
        }

        @Override
        protected File[] doInBackground() throws Exception {
            RandomAccessFile raf = new RandomAccessFile(dicomDir, "r");
            DicomDirReader reader = new DicomDirReader(raf);
            ArrayList<File> dicomFiles = new ArrayList<File>();
            for (DicomObject dcmObj = reader.findFirstRootRecord(); dcmObj != null; dcmObj = reader.findNextSiblingRecord(dcmObj)) {
                String directoryRecordType = dcmObj.getString(Tag.DirectoryRecordType);
                if (directoryRecordType.equals("PATIENT")) {
                    dicomFiles.addAll(processPatient(reader, dcmObj, dicomDir));
                } else {
                    LOGGER.warn("Wrong directory record type " + directoryRecordType);
                }
            }
            reader.close();
            return dicomFiles.toArray(new File[dicomFiles.size()]);
        }

        private List<File> processPatient(DicomDirReader reader, DicomObject dcmObj, File dicomDir) throws IOException {
            ArrayList<File> dicomFiles = new ArrayList<File>();
            for (DicomObject child = reader.findFirstChildRecord(dcmObj); child != null; child = reader.findNextSiblingRecord(child)) {
                String directoryRecordType = child.getString(Tag.DirectoryRecordType);
                if (directoryRecordType.equals("STUDY")) {
                    dicomFiles.addAll(processStudy(reader, child, dicomDir));
                } else {
                    LOGGER.warn("Wrong directory record type " + directoryRecordType);
                }
            }
            return dicomFiles;
        }

        private List<File> processStudy(DicomDirReader reader, DicomObject dcmObj, File dicomDir) throws IOException {
            ArrayList<File> dicomFiles = new ArrayList<File>();
            for (DicomObject child = reader.findFirstChildRecord(dcmObj); child != null; child = reader.findNextSiblingRecord(child)) {
                String directoryRecordType = child.getString(Tag.DirectoryRecordType);
                if (directoryRecordType.equals("SERIES")) {
                    dicomFiles.addAll(processSeries(reader, child, dicomDir));
                } else {
                    LOGGER.warn("Wrong directory record type " + directoryRecordType);
                }
            }
            return dicomFiles;
        }

        private List<File> processSeries(DicomDirReader reader, DicomObject dcmObj, File dicomDir) throws IOException {
            ArrayList<File> dicomFiles = new ArrayList<File>();
            for (DicomObject child = reader.findFirstChildRecord(dcmObj); child != null; child = reader.findNextSiblingRecord(child)) {
                String directoryRecordType = child.getString(Tag.DirectoryRecordType);
                if (directoryRecordType.equals("IMAGE") || directoryRecordType.equals("ENCAP DOC")) {
                    dicomFiles.add(processInstance(reader, child, dicomDir));
                } else {
                    LOGGER.warn("Wrong directory record type " + directoryRecordType);
                }
            }
            return dicomFiles;
        }

        private File processInstance(DicomDirReader reader, DicomObject dcmObj, File dicomDir) throws IOException {
            String[] filePath = dcmObj.getStrings(Tag.ReferencedFileID);
            File dicomFile = FileUtils.getAbsoluteFile(dicomDir.getParentFile(), filePath);
            return dicomFile;
        }

        @Override
        protected void finished() {
            try {
                File[] dicomFiles = get();
                for (File dicomFile : dicomFiles) {
                    addDicomInstance(new FileDataSource(dicomFile));
                }
            } catch (InterruptedException e) {
                LOGGER.error(e.getMessage(), e);
                JOptionPane.showMessageDialog(null, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            } catch (ExecutionException e) {
                LOGGER.error(e.getMessage(), e);
                JOptionPane.showMessageDialog(null, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class LoadDicomInstanceTask extends Task<Void, Void> {

        private DataSource dataSource;

        public LoadDicomInstanceTask(Application application, DataSource dataSource) {
            super(application);
            this.dataSource = dataSource;
        }

        @Override
        protected Void doInBackground() throws Exception {
            DicomObject dcmObj = null;
            DicomInputStream din = new DicomInputStream(dataSource.getInputStream());
            din.setHandler(new StopTagInputHandler(Tag.PixelData - 1));
            dcmObj = new BasicDicomObject();
            din.readDicomObject(dcmObj, -1);
            din.close();
            String sopClassUID = dcmObj.getString(Tag.SOPClassUID);
            Plugin plugin = applicationContext.getPluginContext().getPlugin(sopClassUID);
            if (plugin != null) {
                String patientId = dcmObj.getString(Tag.PatientID);
                Patient patient = patientMap.get(patientId);
                if (patient == null) {
                    patient = new Patient();
                    patient.setPatientId(patientId);
                    patient.setPatientName(dcmObj.getString(Tag.PatientName));
                    patient.setPatientBirthDate(dcmObj.getDate(Tag.PatientBirthDate));
                    patient.setPatientSex(dcmObj.getString(Tag.PatientSex));
                    patientMap.put(patientId, patient);
                }
                String studyUID = dcmObj.getString(Tag.StudyInstanceUID);
                Study study = studyMap.get(studyUID);
                if (study == null) {
                    study = new Study();
                    study.setStudyInstanceUID(studyUID);
                    study.setStudyId(dcmObj.getString(Tag.StudyID));
                    study.setStudyDateTime(dcmObj.getDate(Tag.StudyDate, Tag.StudyTime));
                    study.setAccessionNumber(dcmObj.getString(Tag.AccessionNumber));
                    study.setStudyDescription(dcmObj.getString(Tag.StudyDescription));
                    study.setPatient(patient);
                    patient.addStudy(study);
                    studyMap.put(studyUID, study);
                }
                String seriesUID = dcmObj.getString(Tag.SeriesInstanceUID);
                Series series = seriesMap.get(seriesUID);
                if (series == null) {
                    series = new Series();
                    series.setSeriesInstanceUID(dcmObj.getString(Tag.SeriesInstanceUID));
                    series.setSeriesNumber(dcmObj.getString(Tag.SeriesNumber));
                    series.setModality(dcmObj.getString(Tag.Modality));
                    series.setInstitutionName(dcmObj.getString(Tag.InstitutionName));
                    series.setSeriesDescription(dcmObj.getString(Tag.SeriesDescription));
                    series.setStudy(study);
                    study.addSeries(series);
                    seriesMap.put(seriesUID, series);
                }
                String instanceUID = dcmObj.getString(Tag.SOPInstanceUID);
                Instance instance = instanceMap.get(instanceUID);
                if (instance == null) {
                    instance = plugin.createInstance(dataSource);
                    instance.setSeries(series);
                    series.addInstance(instance);
                    instanceMap.put(instanceUID, instance);
                }
                applicationContext.getViewContext().getDicomThumbsPane().getDicomThumbsModel().addInstance(instance);
                if (applicationContext.getViewContext().getDicomThumbsPane().getDicomThumbsModel().getSize() == 1) {
                    applicationContext.getViewContext().getDicomThumbsPane().setSelectedIndex(0);
                }
            }
            return null;
        }
    }

    @Action
    public void loadInstance() {
        try {
            Instance instance = applicationContext.getViewContext().getDicomThumbsPane().getSelectedInstance();
            if (instance != null) {
                Plugin plugin = applicationContext.getPluginContext().getPlugin(instance.getSopClassUID());
                if (plugin != null) {
                    plugin.viewInstance(instance);
                    applicationContext.getViewContext().getOpenDicomViewerPanel().showViewer(plugin.getId());
                    applicationContext.getViewContext().getDicomInfoPane().loadPatientInfo(instance.getSeries().getStudy().getPatient());
                }
            }
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            JOptionPane.showMessageDialog(null, e.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
        }
    }
}
