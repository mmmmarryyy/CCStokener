package com.valueteam.transfer;

import java.io.File;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.amazonaws.auth.PropertiesCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;

/**
 * @author maurizio
 *
 */
public class AWSManager {

    private static Log log = LogFactory.getLog(AWSManager.class);

    PropertiesCredentials props = null;

    private AWSManager() {
        try {
            ClassLoader loader = AWSDataTransfer.class.getClassLoader();
            if (loader == null) loader = ClassLoader.getSystemClassLoader();
            String propFile = "AwsCredentials.properties";
            java.net.URL url = loader.getResource(propFile);
            props = new PropertiesCredentials(url.openStream());
        } catch (Exception e) {
            log.error("ERROR in loading AWS credentials.");
            e.printStackTrace();
        }
    }

    private static AWSManager instance = null;

    public static AWSManager getInstance() {
        if (instance == null) {
            instance = new AWSManager();
        }
        return instance;
    }

    /**
	 * 
	 * @param bucketName, is mapped into database. Each customer has an individual bucketName(view database customers table)
	 * @param fileName
	 * @return
	 */
    public boolean deleteContentFromAWS(String bucketName, String fileName) {
        log.debug("deleteContentFromAWS START");
        boolean response = false;
        try {
            AmazonS3 s3 = new AmazonS3Client(props);
            s3.deleteObject(bucketName, fileName);
            response = true;
        } catch (Exception e) {
            log.error("ERROR in deleting content on AWS.");
            e.printStackTrace();
        }
        log.debug("deleteContentFromAWS END with response: " + response);
        return response;
    }
}
