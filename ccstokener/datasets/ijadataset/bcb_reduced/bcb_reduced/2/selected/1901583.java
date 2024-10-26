package com.patientis.business.eprescribing.maintenance;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipInputStream;
import org.patientos.surescripts.BodyType;
import org.patientos.surescripts.DirectoryDownload;
import org.patientos.surescripts.Message;
import org.patientos.surescripts.TaxonomyType;
import com.patientis.business.eprescribing.exception.SurescriptsPharmacyException;
import com.patientis.business.eprescribing.model.SurescriptsResponseDTO;
import com.patientis.business.eprescribing.send.SurescriptsSender;
import com.patientis.client.service.clinical.ClinicalService;
import com.patientis.client.service.common.BaseService;
import com.patientis.client.service.reference.ReferenceService;
import com.patientis.data.common.BaseData;
import com.patientis.data.common.ISParameter;
import com.patientis.model.clinical.FormModel;
import com.patientis.model.clinical.FormRecordModel;
import com.patientis.model.clinical.RecordItemModel;
import com.patientis.model.common.ByteWrapper;
import com.patientis.model.common.Converter;
import com.patientis.model.reference.ContentTypeReference;

/**
 * This class manages the process of calling the Surescipts services which
 * provide a URL to a zip file containing information about pharmacies available
 * on the Surescripts network. Download files can be either in the form of a
 * nightly delta update or a full download; in either case we have to handle
 * either updating a pharmacy that is already in the database or creating an
 * entirely new record.
 * 
 * @author ilink
 * 
 */
public class MaintainPharmacies extends SurescriptsSender implements IPharmacyFields {

    private static final String PHARMACY_TAXONOMY = "183500000X";

    private static final String SURESCRIPTS_VERSION = "4";

    /**
	 * This method is used for the nighlty pharmac update. The nightly pharmacy
	 * update just has the changes from the prevous day
	 * 
	 * @param downloadDate
	 */
    public void downloadPharmacies(String downloadDate) {
        processDownloadRequest(downloadDate);
    }

    /**
	 * This method calls the Surescripts server to get a complete download of
	 * pharmacy reference data. The file is then processed and a pharmacy record
	 * is either created or updated as appropriate. This should be done at least
	 * once a week incase any nightly updates were missed
	 */
    public void downloadPharmacies() {
        processDownloadRequest(null);
    }

    private void processDownloadRequest(String date) {
        Message ssMessage = new Message();
        ssMessage.setHeader(createSSHeader("", null));
        ssMessage.setBody(createSSBody(date));
        SurescriptsResponseDTO responseDTO = null;
        String fileId = "-1";
        try {
            try {
                responseDTO = sendMessage(ssMessage);
            } catch (Exception e1) {
                throw new SurescriptsPharmacyException("An exception when sending the message to the Surescripts network: " + e1.getMessage());
            }
            if (responseDTO.getStatus().getCode().equals("000") || responseDTO.getStatus().getCode().equals("010")) {
                File f;
                try {
                    f = downloadFile(new URL(responseDTO.getResponse().getBody().getDirectoryDownloadResponse().getURL()));
                } catch (MalformedURLException e) {
                    throw new SurescriptsPharmacyException("An exception occured when accesing the download URL: " + e.getMessage());
                }
                byte[] fileBArray = new byte[(int) f.length()];
                FileInputStream fis;
                try {
                    fis = new FileInputStream(f);
                    fis.read(fileBArray);
                    fileId = String.valueOf(BaseData.getSystem().storeFile(new ByteWrapper(fileBArray), null, ContentTypeReference.APPLICATIONPDF.getRefId(), BaseService.createServiceCall()));
                } catch (Exception e) {
                    throw new SurescriptsPharmacyException("An exception occured accesing and storing the download file: " + e.getMessage());
                }
                processDownloadFile(unzipExtractFile(f));
            }
        } catch (SurescriptsPharmacyException e) {
            e.printStackTrace();
        } finally {
            try {
                logTransaction(responseDTO, fileId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void logTransaction(SurescriptsResponseDTO dto, String fileId) throws SurescriptsPharmacyException {
        FormModel form;
        try {
            form = ClinicalService.prepareNewForm(50007650);
            List<Long> recordItemRefIds = ClinicalService.getRecordItemRefIdsByFormType(form.getFormTypeRefId());
            Set<FormRecordModel> records = new HashSet<FormRecordModel>();
            for (int i = 0; i < recordItemRefIds.size(); i++) {
                RecordItemModel recordItem = ClinicalService.getRecordItemForRecordItemRefId(recordItemRefIds.get(i));
                FormRecordModel record = recordItem.createFormRecord();
                if (record.getRecordItemRef().getDisplay().equals("Surescripts Pharamcy Download RequestXML (System Surescripts)")) record.setValueString(dto.getRequestXML()); else if (record.getRecordItemRef().getDisplay().equals("Surescripts Pharamcy Download ResponseXML (System Surescripts)")) record.setValueString(dto.getResponseXML()); else if (record.getRecordItemRef().getDisplay().equals("Surescripts Pharamcy Download Error Code (System Surescripts)")) record.setValueString(dto.getError().getCode()); else if (record.getRecordItemRef().getDisplay().equals("Surescripts Pharamcy Download File (System Surescripts)")) record.setValueString(fileId); else if (record.getRecordItemRef().getDisplay().equals("Surescripts Pharamcy Download Status (System Surescripts)")) record.setValueString(dto.getStatus().getCode()); else if (record.getRecordItemRef().getDisplay().equals("Surescripts Pharamcy Download Error Message (System Surescripts)")) record.setValueString(dto.getError().getDescription());
                records.add(record);
            }
            form.setRecords(records);
            ClinicalService.store(form);
        } catch (Exception e) {
            throw new SurescriptsPharmacyException("Error logging the download session: " + e.getMessage());
        }
    }

    /**
	 * 
	 * Download the zip file containing the pharmacy data
	 * 
	 * @param url -
	 *            The location of the zip file to download
	 * @return The zip file from the Surescripts server
	 * @throws IOException
	 * @throws SurescriptsPharmacyException
	 */
    private File downloadFile(URL url) throws SurescriptsPharmacyException {
        File zipFile = null;
        BufferedInputStream in = null;
        FileOutputStream fout = null;
        try {
            in = new BufferedInputStream(url.openStream());
            fout = new FileOutputStream(zipFile);
            byte data[] = new byte[1024];
            int count;
            while ((count = in.read(data, 0, 1024)) != -1) {
                fout.write(data, 0, count);
            }
        } catch (IOException e) {
            throw new SurescriptsPharmacyException("An IO Exception occured while downloading the file from the server");
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException e) {
            }
            if (fout != null) try {
                fout.close();
            } catch (IOException e) {
            }
        }
        return zipFile;
    }

    /**
	 * Read each line of the download file (each line represents a different
	 * pharmacy). For each pharmacy check to see if it is already in the system
	 * and then process accordingly
	 * 
	 * @param file
	 *            The unzipped download file
	 * @throws IOException
	 */
    private void processDownloadFile(File file) throws SurescriptsPharmacyException {
        HashMap<String, String> parsedValues = new HashMap<String, String>();
        FileInputStream fstream;
        try {
            fstream = new FileInputStream(file);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String strLine;
            while ((strLine = br.readLine()) != null) {
                for (PharmacyDownloadFields field : PharmacyDownloadFields.values()) {
                    parsedValues.put(field.name(), strLine.substring(field.getstart(), field.getstart() + field.getLength()));
                }
                long formId = -1;
                try {
                    formId = checkForExistingPharmacy(parsedValues.get("NCPDPID"));
                } catch (Exception e) {
                }
                if (formId == -1) {
                    try {
                        createNewPharmacy(parsedValues);
                    } catch (Exception e) {
                    }
                } else {
                    try {
                        updateExistingPharmacy(parsedValues, formId);
                    } catch (Exception e) {
                    }
                }
            }
            in.close();
        } catch (Exception e1) {
            throw new SurescriptsPharmacyException("A critical exception when processing the download file: " + e1.getMessage());
        }
    }

    /**
	 * Check to see if the Pharmacy already exists. If it does then return its
	 * form id
	 * 
	 * @param id
	 *            The Surescripts unique pharmacy ud
	 * @return The form id for an existing pharmacy or -1 if the pharmacy doesnt
	 *         exist
	 * @throws Exception
	 *             Thrown when there is an error looking up the pharmacy
	 */
    @SuppressWarnings("unchecked")
    private long checkForExistingPharmacy(String id) throws Exception {
        long formId = -1;
        String sql = "select form_id from form_records where record_item_ref_id = (select ref_id from refs where reference_group = 'RecordItem' and ref_key = 'SURESCRIPTSPHARMACYNCPDPID(SYSTEMSURESCRIPTS)')";
        List list = ReferenceService.getSqlParameter(sql, ISParameter.createList(), 1);
        for (Object o : list) {
            formId = Converter.convertLong(o);
        }
        return formId;
    }

    /**
	 * 
	 * Update an existing pharmacy with the new information in the download file
	 * 
	 * @param values
	 *            The values for this pharmacy
	 * @param formId
	 *            The existing form id for this pharmacy
	 * @throws Exception
	 *             When the form could not be creaed or saved
	 */
    private void updateExistingPharmacy(HashMap<String, String> values, long formId) throws Exception {
        FormModel form = null;
        form = ClinicalService.getForm(formId);
        savePharmacyValues(form, values);
    }

    /**
	 * Create a new pharmacy form and save it
	 * 
	 * @param values
	 *            The values from the download file to populate into the form
	 * @throws Exception
	 *             When the form could not be created or saved
	 */
    private void createNewPharmacy(HashMap<String, String> values) throws Exception {
        FormModel newForm = ClinicalService.prepareNewForm(50007539);
        savePharmacyValues(newForm, values);
    }

    /**
	 * Unzip a file
	 * 
	 * @param zipped
	 *            The zip file to extract
	 * @return The unzipped file
	 */
    private File unzipExtractFile(File zipped) {
        File unzipped = null;
        try {
            ZipInputStream inStream = new ZipInputStream(new FileInputStream(zipped));
            OutputStream outStream = new FileOutputStream(unzipped);
            byte[] buffer = new byte[1024];
            int nrBytesRead;
            if (inStream.getNextEntry() != null) {
                while ((nrBytesRead = inStream.read(buffer)) > 0) {
                    outStream.write(buffer, 0, nrBytesRead);
                }
            }
            outStream.close();
            inStream.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return unzipped;
    }

    /**
	 * Populate values in a pharmacy form and save it
	 * 
	 * @param form
	 * @param values
	 * @throws Exception
	 */
    private void savePharmacyValues(FormModel form, HashMap<String, String> values) throws Exception {
        List<Long> recordItemRefIds = ClinicalService.getRecordItemRefIdsByFormType(form.getFormTypeRefId());
        Set<FormRecordModel> records = new HashSet<FormRecordModel>();
        for (int i = 0; i < recordItemRefIds.size(); i++) {
            RecordItemModel recordItem = ClinicalService.getRecordItemForRecordItemRefId(recordItemRefIds.get(i));
            FormRecordModel record = recordItem.createFormRecord();
            String key = PharmacyFormFieldMappings.valueOf(record.getRecordItemRef().getDisplay().replace(" ", "").replace("(", "").replace(")", "")).getFileField();
            record.setValueString(values.get(key));
            records.add(record);
        }
        form.setRecords(records);
        ClinicalService.store(form);
    }

    /**
	 * Create the Surescripts message body
	 * 
	 * @return
	 */
    protected BodyType createSSBody(String downloadDate) {
        BodyType body = new BodyType();
        DirectoryDownload download = new DirectoryDownload();
        download.setAccountID("");
        if (null != downloadDate) download.setDirectoryDate(downloadDate);
        TaxonomyType taxonomy = new TaxonomyType();
        taxonomy.setTaxonomyCode(PHARMACY_TAXONOMY);
        download.setTaxonomy(taxonomy);
        download.setVersionID(SURESCRIPTS_VERSION);
        body.setDirectoryDownload(download);
        return body;
    }
}
