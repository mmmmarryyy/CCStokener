package org.jcvi.vics.server.access.hibernate;

import org.apache.log4j.Logger;
import org.jcvi.vics.model.common.SystemConfigurationProperties;
import org.jcvi.vics.model.download.MooreOrganism;
import org.jcvi.vics.server.access.MooreDAO;
import java.io.IOException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;

/**
 * This class is responsible for retrieving data from the MF150 database
 *
 * @author Tareq Nabeel
 */
public class MooreDAOImpl extends DaoBaseImpl implements MooreDAO {

    private static Logger logger = Logger.getLogger(MooreDAOImpl.class);

    private String HTTP_REQUEST = SystemConfigurationProperties.getString("mf150.url");

    private String FIELD_DELIMITER = SystemConfigurationProperties.getString("mf150.fieldDelim");

    private String ORGANISM_DELIMITER = SystemConfigurationProperties.getString("mf150.organismDelim");

    private String EQUALS_STR = SystemConfigurationProperties.getString("mf150.equalsOper");

    private MooreDAOImpl() {
    }

    /**
     * This method retrieves data from the MF-150 site
     *
     * @return
     * @throws DaoException
     */
    public List<MooreOrganism> findAllOrganisms() throws DaoException {
        int totalOrganisms = 0;
        try {
            URL url = new URL(HTTP_REQUEST);
            logger.info("Retrieving data from " + HTTP_REQUEST);
            List<MooreOrganism> allOrganisms = new ArrayList<MooreOrganism>();
            Scanner organismsScanner = new Scanner(url.openStream());
            organismsScanner.useDelimiter(ORGANISM_DELIMITER);
            Map<String, String> organismDataMap = new HashMap<String, String>();
            while (organismsScanner.hasNext()) {
                totalOrganisms++;
                organismDataMap.clear();
                String[] organismFieldData = getOrganismFieldData(organismsScanner.next(), totalOrganisms);
                for (int i = 0; i < organismFieldData.length; i++) {
                    extractFieldData(organismDataMap, organismFieldData[i], totalOrganisms, i + 1);
                }
                allOrganisms.add(createMooreOrganism(organismDataMap));
            }
            organismsScanner.close();
            logger.info("findAllOrganisms() returning " + allOrganisms.size() + " organisms");
            return allOrganisms;
        } catch (UnknownHostException e) {
            throw new DaoException(e, "findAllOrganisms() failed at organism=" + totalOrganisms);
        } catch (IOException e) {
            throw new DaoException(e, "findAllOrganisms() failed at organism=" + totalOrganisms);
        } catch (Exception e) {
            throw new DaoException(e, "findAllOrganisms() failed at organism=" + totalOrganisms);
        }
    }

    /**
     * Adds the field name and value to organismDataMap using EQUALS_STR as the delimiter
     *
     * @param organismDataMap
     * @param fieldNameValue
     * @param totalOrganisms
     * @param fieldNumber
     */
    private void extractFieldData(Map<String, String> organismDataMap, String fieldNameValue, int totalOrganisms, int fieldNumber) {
        if (fieldNameValue == null) {
            throw new IllegalArgumentException("Invalid field data=" + fieldNameValue + " for organism number=" + totalOrganisms + " fieldNumber=" + fieldNumber);
        }
        if (fieldNameValue.trim().length() == 0) {
            return;
        }
        String[] fieldNameValueArr = fieldNameValue.split(EQUALS_STR);
        if (fieldNameValueArr != null && fieldNameValueArr.length == 2) {
            organismDataMap.put(fieldNameValueArr[0], fieldNameValueArr[1]);
        } else {
            throw new IllegalArgumentException("Invalid equals operator used for field data=" + fieldNameValue + " for organism number=" + totalOrganisms + " fieldNumber=" + fieldNumber + ".  Expected operator=" + EQUALS_STR);
        }
    }

    /**
     * Returns the fields for an organism using FIELD_DELIMITER as the delimiter
     *
     * @param organismData
     * @param totalOrganisms
     * @return
     */
    private String[] getOrganismFieldData(String organismData, int totalOrganisms) {
        if (organismData == null || organismData.length() == 0) {
            throw new IllegalArgumentException("Invalid organism data:" + organismData + " for organism number=" + totalOrganisms);
        }
        String[] fields = organismData.split(FIELD_DELIMITER);
        if (fields == null || fields.length < 2) {
            throw new IllegalArgumentException("Invalid field delimiter used for organism data=" + organismData + " for organism number=" + totalOrganisms + ". Expected delimiter=" + FIELD_DELIMITER);
        }
        return fields;
    }

    /**
     * Initialize the organism based on values read into organismData
     *
     * @param organismData
     * @return
     */
    private MooreOrganism createMooreOrganism(Map<String, String> organismData) {
        MooreOrganism organism = new MooreOrganism();
        organism.setAccession(getStringFieldValue(organismData, "Accession"));
        organism.setAvgContigSize(getDoubleFieldValue(organismData, "AvgContigSize"));
        organism.setCitations(getStringFieldValue(organismData, "Citations"));
        organism.setCollectionMethod(getStringFieldValue(organismData, "CollectionMethod"));
        organism.setDeliveryDate(getDateFieldValue(organismData, "DeliveryDate"));
        organism.setDepth(getStringFieldValue(organismData, "Depth"));
        organism.setDescription(getStringFieldValue(organismData, "Description"));
        organism.setGenomeSize(getDoubleFieldValue(organismData, "GenomeSize"));
        organism.setGcContentPerc(getDoubleFieldValue(organismData, "GcContentPerc"));
        organism.setGenbankContribDate(getDateFieldValue(organismData, "GenbankContribDate"));
        organism.setGeneCodingPerc(getDoubleFieldValue(organismData, "GeneCodingPerc"));
        organism.setGeneCount(getLongFieldValue(organismData, "GeneCount"));
        organism.setGeneRrnaCount(getLongFieldValue(organismData, "GeneRrnaCount"));
        organism.setGeneTrnaCount(getLongFieldValue(organismData, "GeneTrnaCount"));
        organism.setGenomeLength(getDoubleFieldValue(organismData, "GenomeLength"));
        organism.setInvestigatorEmail(getStringFieldValue(organismData, "InvestigatorEmail"));
        organism.setInvestigatorName(getStringFieldValue(organismData, "InvestigatorName"));
        organism.setInvestigatorWebsite(getStringFieldValue(organismData, "InvestigatorWebsite"));
        organism.setLocation(getStringFieldValue(organismData, "Location"));
        organism.setNcbiUrl(getStringFieldValue(organismData, "NcbiUrl"));
        organism.setOrganismName(getStringFieldValue(organismData, "OrganismName"));
        organism.setProposer(getStringFieldValue(organismData, "Proposer"));
        organism.setReceivedDate(getDateFieldValue(organismData, "ReceivedDate"));
        organism.setReleaseDate(getDateFieldValue(organismData, "ReleaseDate"));
        organism.setRelevance(getStringFieldValue(organismData, "Relevance"));
        organism.setScaffoldCount(getLongFieldValue(organismData, "ScaffoldCount"));
        organism.setSpecies(getStringFieldValue(organismData, "Species"));
        organism.setSpeciesTag(getStringFieldValue(organismData, "SpeciesTag"));
        organism.setStatus(getStringFieldValue(organismData, "Status"));
        organism.setStatusTag(getStringFieldValue(organismData, "StatusTag"));
        organism.setStrain(getStringFieldValue(organismData, "Strain"));
        return organism;
    }

    /**
     * Noticed that null field values are being sent as "null"
     *
     * @param organismData
     * @param fieldName
     * @return
     */
    private String getStringFieldValue(Map<String, String> organismData, String fieldName) {
        String value = organismData.get(fieldName);
        return (value == null || value.trim().equals("null") ? null : value);
    }

    /**
     * @param organismData
     * @param fieldName
     * @return
     */
    private Double getDoubleFieldValue(Map<String, String> organismData, String fieldName) {
        String value = organismData.get(fieldName);
        return (value == null || value.trim().equals("null") || value.trim().equals("") ? null : new Double(value));
    }

    /**
     * @param organismData
     * @param fieldName
     * @return
     */
    private Long getLongFieldValue(Map<String, String> organismData, String fieldName) {
        String value = organismData.get(fieldName);
        return (value == null || value.trim().equals("null") || value.trim().equals("") ? null : new Long(value));
    }

    /**
     * Noticed that null field values are being sent as "0"
     *
     * @param organismData
     * @param fieldName
     * @return
     */
    private Date getDateFieldValue(Map<String, String> organismData, String fieldName) {
        String releaseDateStr = organismData.get(fieldName);
        Date releaseDate = null;
        if (releaseDateStr != null && !releaseDateStr.trim().equals("0") && !releaseDateStr.trim().equals("") && !releaseDateStr.trim().equals("null")) {
            releaseDate = new Date(new Long(releaseDateStr));
        }
        return releaseDate;
    }
}
