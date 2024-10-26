package edu.mta.ok.nworkshop;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * The class holds a map with all the predictors properties loaded from an external property file 
 */
public class PredictorProperties {

    public enum PropertyKeys {

        PREDICTIONS_FILE("predictionsFile"), RESIDUALS_MODEL("globalEffectResiduals"), NEIGHBORS_NUM("neighborsNum"), FEATURES_NUM("featuresNum"), MAX_EPHOCS_NUM("maxEphocsNum"), INTERPOLATION_FILE_NAME("interpolationFile"), MOVIE_MODEL_FILE_NAME("movieModelFile"), USER_MODEL_FILE_NAME("userModelFile"), USER_MAPPING_FILE("userMapFile"), PROBE_FILE("probeFile"), GLOBAL_EFFECTS_FILE("effectFile"), GLOBAL_EFFECT_PROBE_FILE("effectProbeFile");

        private String propertyName;

        private PropertyKeys(String name) {
            propertyName = name;
        }

        @Override
        public String toString() {
            return propertyName;
        }
    }

    public enum Predictors {

        GENERAL("general"), KNN("knn"), IMPROVED_KNN("improvedKNN"), SVD("svd"), IMPROVED_SVD("improvedSVD"), KNN_SVD("knnSVD");

        private String name;

        private Predictors(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private final Map<Predictors, Map<String, String>> predictorsProperties = new HashMap<Predictors, Map<String, String>>();

    private static PredictorProperties instance = null;

    private PredictorProperties() {
        initPredictorsProperties();
    }

    /**
	 * Load the predictors properties from a file 
	 */
    private void initPredictorsProperties() {
        Properties prop = new Properties();
        try {
            URL url = ClassLoader.getSystemResource("predictors.properties");
            prop.load(url.openStream());
        } catch (IOException e) {
            System.err.println("Error loading properties file. error: " + e.getMessage());
        }
        for (Predictors currPred : Predictors.values()) {
            Map<String, String> currPredictorsMap = new HashMap<String, String>();
            for (PropertyKeys currPKey : PropertyKeys.values()) {
                String key = currPred.toString() + "." + currPKey.toString();
                String value = prop.getProperty(key);
                if (value != null) {
                    StringTokenizer st = new StringTokenizer(key, ".");
                    st.nextToken();
                    key = st.nextToken();
                    currPredictorsMap.put(key, value);
                }
            }
            predictorsProperties.put(currPred, currPredictorsMap);
        }
    }

    /**
	 * Return a map with all the properties a certain predictor has
	 * 
	 * @param predictor a predictor we want to get his properties
	 * @return Map with all the given predictors properties loaded from the file
	 */
    public Map<String, String> getPredictorProperties(Predictors predictor) {
        return predictorsProperties.get(predictor);
    }

    public int getPredictorIntProperty(Predictors predictor, PropertyKeys key) {
        return getPredictorIntProperty(predictor, key, -1);
    }

    public int getPredictorIntProperty(Predictors predictor, PropertyKeys key, int defaultValue) {
        int retVal = (this.predictorsProperties.containsKey(predictor) && this.predictorsProperties.get(predictor).containsKey(key.toString())) ? Integer.valueOf(this.predictorsProperties.get(predictor).get(key.toString())) : defaultValue;
        return retVal;
    }

    public String getPredictorStringProperty(Predictors predictor, PropertyKeys key) {
        return this.getPredictorStringProperty(predictor, key, null);
    }

    public String getPredictorStringProperty(Predictors predictor, PropertyKeys key, String defaultValue) {
        String retVal = (this.predictorsProperties.containsKey(predictor) && this.predictorsProperties.get(predictor).containsKey(key.toString())) ? this.predictorsProperties.get(predictor).get(key.toString()) : defaultValue;
        return retVal;
    }

    /**
	 * 
	 * @return the configured movie indexed model file name or the constant value if non is configured in the properties file
	 */
    public String getMovieIndexedModelFile() {
        return Constants.NETFLIX_OUTPUT_DIR + getPredictorStringProperty(Predictors.GENERAL, PropertyKeys.MOVIE_MODEL_FILE_NAME, Constants.DEFAULT_MOVIE_INDEXED_MODEL_FILE_NAME);
    }

    /**
	 * 
	 * @return the configured user indexed model file name or the constant value if non is configured in the properties file
	 */
    public String getUserIndexedModelFile() {
        return Constants.NETFLIX_OUTPUT_DIR + getPredictorStringProperty(Predictors.GENERAL, PropertyKeys.USER_MODEL_FILE_NAME, Constants.DEFAULT_USER_INDEXED_MODEL_FILE_NAME);
    }

    /**
	 * 
	 * @return the configured user indices map file name or the constant value if non is configured in the properties file
	 */
    public String getUserIndicesMappingFile() {
        return Constants.NETFLIX_OUTPUT_DIR + getPredictorStringProperty(Predictors.GENERAL, PropertyKeys.USER_MAPPING_FILE, Constants.DEFAULT_USER_INDEX_MAPPING_FILE_NAME);
    }

    /**
	 * 
	 * @return the configured probe model file name or the constant value if non is configured in the properties file
	 */
    public String getProbeFile() {
        return Constants.NETFLIX_OUTPUT_DIR + getPredictorStringProperty(Predictors.GENERAL, PropertyKeys.PROBE_FILE, Constants.DEFAULT_PROBE_FILE_NAME);
    }

    /**
	 * 
	 * @return the single instance of the class
	 */
    public static PredictorProperties getInstance() {
        if (instance == null) {
            instance = new PredictorProperties();
        }
        return instance;
    }
}
