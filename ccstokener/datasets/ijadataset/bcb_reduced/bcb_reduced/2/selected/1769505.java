package edu.ucdavis.genomics.metabolomics.binbase.server.ejb.compounds;

import gov.nih.nlm.ncbi.pubchem.CompressType;
import gov.nih.nlm.ncbi.pubchem.FormatType;
import gov.nih.nlm.ncbi.pubchem.MatrixFormatType;
import gov.nih.nlm.ncbi.pubchem.PCIDType;
import gov.nih.nlm.ncbi.pubchem.PUGLocator;
import gov.nih.nlm.ncbi.pubchem.PUGSoap;
import gov.nih.nlm.ncbi.pubchem.ScoreTypeType;
import gov.nih.nlm.ncbi.pubchem.StatusType;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Scanner;
import org.apache.log4j.Logger;
import com.chemspider.www.InChILocator;
import com.chemspider.www.InChISoap;

/**
 * simple helper class for easier testing
 * which basically is a bruteforce approach since it tries to execute the service several times before it fails
 * @author wohlgemuth
 * 
 */
public class WebserviceHelper {

    /**
	 * pubchem fetch timeout, 5 seconds should be cool
	 */
    private static final int TIMEOUT = 1000;

    private static final int MAX_TRY = 10;

    private static final long TIME = 500;

    private Logger logger = org.apache.log4j.Logger.getLogger(getClass());

    public double calculate2DSimilarityBetweenPubchemIds(String first, String second) throws Exception {
        Exception ex = null;
        for (int i = 0; i < MAX_TRY; i++) {
            try {
                PUGLocator locator = new PUGLocator();
                PUGSoap soap = locator.getPUGSoap();
                String key1 = soap.inputList(new int[] { Integer.parseInt(first) }, PCIDType.eID_CID);
                String key2 = soap.inputList(new int[] { Integer.parseInt(second) }, PCIDType.eID_CID);
                String downloadKey = soap.scoreMatrix(key1, key2, ScoreTypeType.eScoreType_Sim2DSubs, MatrixFormatType.eMatrixFormat_IdIdScore, CompressType.eCompress_None);
                StatusType status = waitingForCalculation(soap, downloadKey);
                if (status == StatusType.eStatus_Success) {
                    URL url = new URL(soap.getDownloadUrl(downloadKey));
                    Scanner s = new Scanner(url.openStream());
                    Double result = Double.parseDouble(s.nextLine().split("\t")[2]);
                    return result;
                } else {
                    throw new CompoundNotFoundException(status.getValue() + soap.getStatusMessage(downloadKey));
                }
            } catch (CompoundNotFoundException e) {
                throw e;
            } catch (Exception e) {
                logger.warn(e.getMessage());
                ex = e;
                Thread.sleep(TIME);
            }
        }
        throw ex;
    }

    /**
	 * waits for the calculation on the pubchem side
	 * 
	 * @param soap
	 * @param downloadKey
	 * @return
	 * @throws RemoteException
	 * @throws InterruptedException
	 */
    private StatusType waitingForCalculation(PUGSoap soap, String downloadKey) throws RemoteException, InterruptedException {
        StatusType status = null;
        boolean finished = false;
        int trys = 0;
        int maxTries = 50;
        while (finished == false) {
            trys++;
            status = soap.getOperationStatus(downloadKey);
            if (status == StatusType.eStatus_Success) {
                finished = true;
            } else if (status == StatusType.eStatus_ServerError) {
                throw new RemoteException("status is in state server error: " + soap.getStatusMessage(downloadKey));
            } else if (status == StatusType.eStatus_DataError) {
                throw new RemoteException("status is in state data error: " + soap.getStatusMessage(downloadKey));
            } else if (status == StatusType.eStatus_Stopped) {
                finished = true;
            } else if (status == StatusType.eStatus_InputError) {
                throw new RemoteException("status is in state input error: " + soap.getStatusMessage(downloadKey));
            } else {
                if (trys >= maxTries) {
                    throw new RemoteException("sorry the pubchem service failed to respond with the result in: " + (trys * TIMEOUT) + " milli seconds");
                }
                logger.debug("sleeping, status message is: " + soap.getStatusMessage(downloadKey) + " status is: " + status.getValue());
                Thread.sleep(TIMEOUT + trys);
            }
        }
        return status;
    }

    public String getInchiCodeForPubchemId(String cid) throws Exception {
        Exception ex = null;
        for (int i = 0; i < MAX_TRY; i++) {
            try {
                PUGLocator locator = new PUGLocator();
                PUGSoap soap = locator.getPUGSoap();
                String key = soap.inputList(new int[] { Integer.parseInt(cid) }, PCIDType.eID_CID);
                String downloadKey = soap.download(key, FormatType.eFormat_InChI, CompressType.eCompress_None, false);
                StatusType status;
                status = waitingForCalculation(soap, downloadKey);
                if (status == StatusType.eStatus_Success) {
                    URL url = new URL(soap.getDownloadUrl(downloadKey));
                    Scanner s = new Scanner(url.openStream());
                    String result = s.nextLine().split("\t")[1];
                    return result;
                } else {
                    throw new CompoundNotFoundException(status.getValue() + soap.getStatusMessage(downloadKey));
                }
            } catch (CompoundNotFoundException e) {
                throw e;
            } catch (Exception e) {
                logger.warn(e.getMessage());
                ex = e;
                Thread.sleep(TIME);
            }
        }
        throw ex;
    }

    public String getSmileCodeForPubchemId(String cid) throws Exception {
        Exception ex = null;
        for (int i = 0; i < MAX_TRY; i++) {
            try {
                PUGLocator locator = new PUGLocator();
                PUGSoap soap = locator.getPUGSoap();
                String key = soap.inputList(new int[] { Integer.parseInt(cid) }, PCIDType.eID_CID);
                String downloadKey = soap.download(key, FormatType.eFormat_SMILES, CompressType.eCompress_None, false);
                StatusType status = waitingForCalculation(soap, downloadKey);
                if (status == StatusType.eStatus_Success) {
                    URL url = new URL(soap.getDownloadUrl(downloadKey));
                    Scanner s = new Scanner(url.openStream());
                    String result = s.nextLine().split("\t")[1];
                    return result;
                } else {
                    throw new CompoundNotFoundException(status.getValue() + soap.getStatusMessage(downloadKey));
                }
            } catch (CompoundNotFoundException e) {
                throw e;
            } catch (Exception e) {
                logger.warn(e.getMessage());
                ex = e;
                Thread.sleep(TIME);
            }
        }
        throw ex;
    }

    public String getMolFileForPubchemId(String cid) throws Exception {
        Exception ex = null;
        for (int i = 0; i < MAX_TRY; i++) {
            try {
                String smile = getSmileCodeForPubchemId(cid).trim();
                logger.debug("generated smile is: " + smile);
                InChILocator locator = new InChILocator();
                InChISoap inch = locator.getInChISoap();
                String inchi = inch.SMILESToInChI(smile).trim();
                logger.debug("generated inchi is: " + inchi);
                String result = inch.inChIToMol(inchi);
                return result;
            } catch (CompoundNotFoundException e) {
                throw e;
            } catch (Exception e) {
                logger.warn(e.getMessage());
                ex = e;
                Thread.sleep(TIME);
            }
        }
        throw ex;
    }
}
