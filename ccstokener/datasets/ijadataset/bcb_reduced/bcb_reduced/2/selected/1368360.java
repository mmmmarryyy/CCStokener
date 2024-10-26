package org.weasis.dicom.explorer.wado;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.transform.stax.StAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.dcm4che2.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.weasis.core.api.explorer.model.TreeModel;
import org.weasis.core.api.gui.util.AbstractProperties;
import org.weasis.core.api.gui.util.GuiExecutor;
import org.weasis.core.api.media.MimeInspector;
import org.weasis.core.api.media.data.MediaSeriesGroup;
import org.weasis.core.api.media.data.MediaSeriesGroupNode;
import org.weasis.core.api.media.data.Series;
import org.weasis.core.api.media.data.TagW;
import org.weasis.core.api.util.FileUtil;
import org.weasis.core.api.util.GzipManager;
import org.weasis.core.ui.docking.UIManager;
import org.weasis.core.ui.editor.image.ViewerPlugin;
import org.weasis.dicom.codec.DicomInstance;
import org.weasis.dicom.codec.DicomSeries;
import org.weasis.dicom.codec.DicomVideoSeries;
import org.weasis.dicom.codec.wado.WadoParameters;
import org.weasis.dicom.explorer.DicomModel;
import org.xml.sax.SAXException;

public class DownloadManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloadManager.class);

    public DownloadManager() {
    }

    public static ArrayList<LoadSeries> buildDicomSeriesFromXml(URI uri, final DicomModel model) {
        ArrayList<LoadSeries> seriesList = new ArrayList<LoadSeries>();
        XMLStreamReader xmler = null;
        InputStream stream = null;
        try {
            XMLInputFactory xmlif = XMLInputFactory.newInstance();
            String path = uri.getPath();
            URL url = uri.toURL();
            LOGGER.info("Downloading WADO references: {}", url);
            if (path.endsWith(".gz")) {
                stream = GzipManager.gzipUncompressToStream(url);
            } else if (path.endsWith(".xml")) {
                stream = url.openStream();
            } else {
                File outFile = File.createTempFile("wado_", "", AbstractProperties.APP_TEMP_DIR);
                if (FileUtil.writeFile(url, outFile) == -1) {
                    if (MimeInspector.isMatchingMimeTypeFromMagicNumber(outFile, "application/x-gzip")) {
                        stream = new BufferedInputStream((new GZIPInputStream(new FileInputStream((outFile)))));
                    } else {
                        stream = url.openStream();
                    }
                }
            }
            File tempFile = null;
            if (uri.toString().startsWith("file:") && path.endsWith(".xml")) {
                tempFile = new File(path);
            } else {
                tempFile = File.createTempFile("wado_", ".xml", AbstractProperties.APP_TEMP_DIR);
                FileUtil.writeFile(stream, new FileOutputStream(tempFile));
            }
            xmler = xmlif.createXMLStreamReader(new FileReader(tempFile));
            Source xmlFile = new StAXSource(xmler);
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            try {
                Schema schema = schemaFactory.newSchema(DownloadManager.class.getResource("/config/wado_query.xsd"));
                Validator validator = schema.newValidator();
                validator.validate(xmlFile);
                LOGGER.info("[Validate with XSD schema] wado_query is valid");
            } catch (SAXException e) {
                LOGGER.error("[Validate with XSD schema] wado_query is NOT valid");
                LOGGER.error("Reason: {}", e.getLocalizedMessage());
            } catch (Exception e) {
                LOGGER.error("Error when validate XSD schema. Try to update JRE");
                e.printStackTrace();
            }
            xmler = xmlif.createXMLStreamReader(new FileReader(tempFile));
            int eventType;
            if (xmler.hasNext()) {
                eventType = xmler.next();
                switch(eventType) {
                    case XMLStreamConstants.START_ELEMENT:
                        String key = xmler.getName().getLocalPart();
                        if (WadoParameters.TAG_DOCUMENT_ROOT.equals(key)) {
                            String wadoURL = getTagAttribute(xmler, WadoParameters.TAG_WADO_URL, null);
                            boolean onlySopUID = Boolean.valueOf(getTagAttribute(xmler, WadoParameters.TAG_WADO_ONLY_SOP_UID, "false"));
                            String additionnalParameters = getTagAttribute(xmler, WadoParameters.TAG_WADO_ADDITIONNAL_PARAMETERS, "");
                            String overrideList = getTagAttribute(xmler, WadoParameters.TAG_WADO_OVERRIDE_TAGS, null);
                            String webLogin = getTagAttribute(xmler, WadoParameters.TAG_WADO_WEB_LOGIN, null);
                            final WadoParameters wadoParameters = new WadoParameters(wadoURL, onlySopUID, additionnalParameters, overrideList, webLogin);
                            int pat = 0;
                            MediaSeriesGroup patient = null;
                            while (xmler.hasNext()) {
                                eventType = xmler.next();
                                switch(eventType) {
                                    case XMLStreamConstants.START_ELEMENT:
                                        if (TagW.DICOM_LEVEL.Patient.name().equals(xmler.getName().getLocalPart())) {
                                            patient = readPatient(model, seriesList, xmler, wadoParameters);
                                            pat++;
                                        } else if (WadoParameters.TAG_HTTP_TAG.equals(xmler.getName().getLocalPart())) {
                                            String httpkey = getTagAttribute(xmler, "key", null);
                                            String httpvalue = getTagAttribute(xmler, "value", null);
                                            wadoParameters.addHttpTag(httpkey, httpvalue);
                                        }
                                        break;
                                    default:
                                        break;
                                }
                            }
                            if (pat == 1) {
                                final MediaSeriesGroup uniquePatient = patient;
                                GuiExecutor.instance().execute(new Runnable() {

                                    @Override
                                    public void run() {
                                        synchronized (UIManager.VIEWER_PLUGINS) {
                                            for (final ViewerPlugin p : UIManager.VIEWER_PLUGINS) {
                                                if (uniquePatient.equals(p.getGroupID())) {
                                                    p.setSelectedAndGetFocus();
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                });
                            }
                            for (LoadSeries loadSeries : seriesList) {
                                String modality = (String) loadSeries.getDicomSeries().getTagValue(TagW.Modality);
                                boolean ps = modality != null && ("PR".equals(modality) || "KO".equals(modality));
                                if (!ps) {
                                    loadSeries.startDownloadImageReference(wadoParameters);
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        } finally {
            FileUtil.safeClose(xmler);
            FileUtil.safeClose(stream);
        }
        return seriesList;
    }

    private static MediaSeriesGroup readPatient(DicomModel model, ArrayList<LoadSeries> seriesList, XMLStreamReader xmler, WadoParameters wadoParameters) throws XMLStreamException {
        String unknown = org.weasis.dicom.codec.Messages.getString("DicomMediaIO.unknown");
        String patientID = getTagAttribute(xmler, TagW.PatientID.getTagName(), unknown);
        Date birthdate = DateUtils.parseDA(getTagAttribute(xmler, TagW.PatientBirthDate.getTagName(), null), false);
        String name = getTagAttribute(xmler, TagW.PatientName.getTagName(), unknown);
        if (name.trim().equals("")) {
            name = unknown;
        }
        name = name.replace("^", " ");
        String patientPseudoUID = patientID + (birthdate == null ? "" : TagW.dicomformatDate.format(birthdate).toString() + name.substring(0, name.length() < 5 ? name.length() : 5));
        MediaSeriesGroup patient = model.getHierarchyNode(TreeModel.rootNode, patientPseudoUID);
        if (patient == null) {
            patient = new MediaSeriesGroupNode(TagW.PatientPseudoUID, patientPseudoUID, TagW.PatientName);
            patient.setTag(TagW.PatientID, patientID);
            patient.setTag(TagW.PatientName, name);
            patient.setTag(TagW.PatientSex, getTagAttribute(xmler, TagW.PatientSex.getTagName(), "O"));
            patient.setTag(TagW.PatientBirthDate, birthdate);
            patient.setTagNoNull(TagW.PatientBirthTime, TagW.getDicomTime(getTagAttribute(xmler, TagW.PatientBirthTime.getTagName(), null)));
            model.addHierarchyNode(TreeModel.rootNode, patient);
            LOGGER.info("Adding new patient: " + patient);
        }
        int eventType;
        boolean state = true;
        while (xmler.hasNext() && state) {
            eventType = xmler.next();
            switch(eventType) {
                case XMLStreamConstants.START_ELEMENT:
                    if (TagW.DICOM_LEVEL.Study.name().equals(xmler.getName().getLocalPart())) {
                        readStudy(model, seriesList, xmler, patient, wadoParameters);
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (TagW.DICOM_LEVEL.Patient.name().equals(xmler.getName().getLocalPart())) {
                        state = false;
                    }
                    break;
                default:
                    break;
            }
        }
        return patient;
    }

    private static MediaSeriesGroup readStudy(DicomModel model, ArrayList<LoadSeries> seriesList, XMLStreamReader xmler, MediaSeriesGroup patient, WadoParameters wadoParameters) throws XMLStreamException {
        String studyUID = getTagAttribute(xmler, TagW.StudyInstanceUID.getTagName(), "");
        MediaSeriesGroup study = model.getHierarchyNode(patient, studyUID);
        if (study == null) {
            study = new MediaSeriesGroupNode(TagW.StudyInstanceUID, studyUID, TagW.StudyDate);
            study.setTagNoNull(TagW.StudyDate, TagW.getDicomDate(getTagAttribute(xmler, TagW.StudyDate.getTagName(), null)));
            study.setTagNoNull(TagW.StudyTime, TagW.getDicomTime(getTagAttribute(xmler, TagW.StudyTime.getTagName(), null)));
            study.setTagNoNull(TagW.StudyDescription, getTagAttribute(xmler, TagW.StudyDescription.getTagName(), null));
            study.setTagNoNull(TagW.AccessionNumber, getTagAttribute(xmler, TagW.AccessionNumber.getTagName(), null));
            study.setTagNoNull(TagW.StudyID, getTagAttribute(xmler, TagW.StudyID.getTagName(), null));
            model.addHierarchyNode(patient, study);
        }
        int eventType;
        boolean state = true;
        while (xmler.hasNext() && state) {
            eventType = xmler.next();
            switch(eventType) {
                case XMLStreamConstants.START_ELEMENT:
                    if (TagW.DICOM_LEVEL.Series.name().equals(xmler.getName().getLocalPart())) {
                        readSeries(model, seriesList, xmler, patient, study, wadoParameters);
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (TagW.DICOM_LEVEL.Study.name().equals(xmler.getName().getLocalPart())) {
                        state = false;
                    }
                    break;
                default:
                    break;
            }
        }
        return study;
    }

    private static Series readSeries(DicomModel model, ArrayList<LoadSeries> seriesList, XMLStreamReader xmler, MediaSeriesGroup patient, MediaSeriesGroup study, WadoParameters wadoParameters) throws XMLStreamException {
        String seriesUID = getTagAttribute(xmler, TagW.SeriesInstanceUID.getTagName(), "");
        Series dicomSeries = (Series) model.getHierarchyNode(study, seriesUID);
        if (dicomSeries == null) {
            dicomSeries = new DicomSeries(seriesUID);
            dicomSeries.setTag(TagW.ExplorerModel, model);
            dicomSeries.setTag(TagW.SeriesInstanceUID, seriesUID);
            dicomSeries.setTag(TagW.Modality, getTagAttribute(xmler, TagW.Modality.getTagName(), null));
            dicomSeries.setTag(TagW.WadoParameters, wadoParameters);
            dicomSeries.setTag(TagW.WadoInstanceReferenceList, new ArrayList<DicomInstance>());
            dicomSeries.setTagNoNull(TagW.SeriesNumber, getIntegerTagAttribute(xmler, TagW.SeriesNumber.getTagName(), null));
            dicomSeries.setTagNoNull(TagW.SeriesDescription, getTagAttribute(xmler, TagW.SeriesDescription.getTagName(), null));
            dicomSeries.setTagNoNull(TagW.ReferringPhysicianName, getTagAttribute(xmler, TagW.ReferringPhysicianName.getTagName(), null));
            dicomSeries.setTagNoNull(TagW.WadoTransferSyntaxUID, getTagAttribute(xmler, TagW.WadoTransferSyntaxUID.getTagName(), null));
            dicomSeries.setTagNoNull(TagW.WadoCompressionRate, getIntegerTagAttribute(xmler, TagW.WadoCompressionRate.getTagName(), null));
            dicomSeries.setTagNoNull(TagW.DirectDownloadThumbnail, getTagAttribute(xmler, TagW.DirectDownloadThumbnail.getTagName(), null));
            model.addHierarchyNode(study, dicomSeries);
        } else {
            WadoParameters wado = (WadoParameters) dicomSeries.getTagValue(TagW.WadoParameters);
            if (wado == null) {
                dicomSeries.setTag(TagW.WadoParameters, wadoParameters);
            } else if (!wado.getWadoURL().equals(wadoParameters.getWadoURL())) {
                LOGGER.error("Wado parameters must be unique for a DICOM Series: {}", dicomSeries);
                return dicomSeries;
            }
        }
        List<DicomInstance> dicomInstances = (List<DicomInstance>) dicomSeries.getTagValue(TagW.WadoInstanceReferenceList);
        boolean containsInstance = false;
        if (dicomInstances == null) {
            dicomSeries.setTag(TagW.WadoInstanceReferenceList, new ArrayList<DicomInstance>());
        } else if (dicomInstances.size() > 0) {
            containsInstance = true;
        }
        int eventType;
        boolean state = true;
        while (xmler.hasNext() && state) {
            eventType = xmler.next();
            switch(eventType) {
                case XMLStreamConstants.START_ELEMENT:
                    if (TagW.DICOM_LEVEL.Instance.name().equals(xmler.getName().getLocalPart())) {
                        String sopInstanceUID = getTagAttribute(xmler, TagW.SOPInstanceUID.getTagName(), null);
                        if (sopInstanceUID != null) {
                            String tsuid = getTagAttribute(xmler, TagW.TransferSyntaxUID.getTagName(), null);
                            DicomInstance dcmInstance = new DicomInstance(sopInstanceUID, tsuid);
                            if (containsInstance && dicomInstances.contains(dcmInstance)) {
                                LOGGER.warn("DICOM instance {} already exists, abort downloading.", sopInstanceUID);
                            } else {
                                dcmInstance.setInstanceNumber(getIntegerTagAttribute(xmler, TagW.InstanceNumber.getTagName(), -1));
                                dcmInstance.setDirectDownloadFile(getTagAttribute(xmler, TagW.DirectDownloadFile.getTagName(), null));
                                dicomInstances.add(dcmInstance);
                            }
                        }
                    }
                    break;
                case XMLStreamConstants.END_ELEMENT:
                    if (TagW.DICOM_LEVEL.Series.name().equals(xmler.getName().getLocalPart())) {
                        state = false;
                    }
                    break;
                default:
                    break;
            }
        }
        if (dicomInstances.size() > 0) {
            if (dicomInstances.size() == 1 && "1.2.840.10008.1.2.4.100".equals(dicomInstances.get(0).getTransferSyntaxUID())) {
                model.removeHierarchyNode(study, dicomSeries);
                dicomSeries = new DicomVideoSeries((DicomSeries) dicomSeries);
                model.addHierarchyNode(study, dicomSeries);
            }
            String modality = (String) dicomSeries.getTagValue(TagW.Modality);
            boolean ps = modality != null && ("PR".equals(modality) || "KO".equals(modality));
            final LoadSeries loadSeries = new LoadSeries(dicomSeries, model);
            Integer sn = (Integer) (ps ? Integer.MAX_VALUE : dicomSeries.getTagValue(TagW.SeriesNumber));
            DownloadPriority priority = new DownloadPriority((String) patient.getTagValue(TagW.PatientName), (String) study.getTagValue(TagW.StudyInstanceUID), (Date) study.getTagValue(TagW.StudyDate), sn);
            loadSeries.setPriority(priority);
            seriesList.add(loadSeries);
        }
        return dicomSeries;
    }

    private static String getTagAttribute(XMLStreamReader xmler, String attribute, String defaultValue) {
        if (attribute != null) {
            String val = xmler.getAttributeValue(null, attribute);
            if (val != null) {
                return val;
            }
        }
        return defaultValue;
    }

    private static Integer getIntegerTagAttribute(XMLStreamReader xmler, String attribute, Integer defaultValue) {
        if (attribute != null) {
            try {
                String val = xmler.getAttributeValue(null, attribute);
                if (val != null) {
                    return Integer.valueOf(val);
                }
            } catch (NumberFormatException e) {
            }
        }
        return defaultValue;
    }
}
