package ru.ksu.niimm.cll.mocassin.crawl.analyzer.lsa.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import ru.ksu.niimm.cll.mocassin.crawl.analyzer.lsa.LSIPropertiesLoader;

public class LSIPropertiesLoaderImpl implements LSIPropertiesLoader {

    private static final String REGULARIZATION_PARAMETER_NAME = "regularization";

    private static final String MIN_IMPROVEMENT_PARAMETER_NAME = "min.improvement";

    private static final String MIN_EPOCH_PARAMETER_NAME = "min.epochs";

    private static final String MAX_FACTORS_PARAMETER_NAME = "max.factors";

    private static final String MAX_EPOCHS_PARAMETER_NAME = "max.epochs";

    private static final String INITIAL_LEARNING_RATE_PARAMETER_NAME = "initial.learning.rate";

    private static final String FEATURE_INIT_PARAMETER_NAME = "feature.init";

    private static final String ANNILING_RATE_PARAMETER_NAME = "anniling.rate";

    private static final String USE_STOP_WORD_LIST_PARAMETER_NAME = "useStopWordList";

    private static final String PROPERTIES_FILENAME = "lsa.properties";

    private Properties properties;

    public LSIPropertiesLoaderImpl() throws IOException {
        this.properties = loadProperties();
    }

    @Override
    public boolean useStopWords() {
        return Boolean.parseBoolean(get(USE_STOP_WORD_LIST_PARAMETER_NAME));
    }

    @Override
    public int getAnilingRate() {
        return Integer.parseInt(get(ANNILING_RATE_PARAMETER_NAME));
    }

    @Override
    public double getFeatureInit() {
        return Double.parseDouble(get(FEATURE_INIT_PARAMETER_NAME));
    }

    @Override
    public double getInitialLearningRate() {
        return Double.parseDouble(get(INITIAL_LEARNING_RATE_PARAMETER_NAME));
    }

    @Override
    public int getMaxEpochs() {
        return Integer.parseInt(get(MAX_EPOCHS_PARAMETER_NAME));
    }

    @Override
    public int getMaxFactors() {
        return Integer.parseInt(get(MAX_FACTORS_PARAMETER_NAME));
    }

    @Override
    public int getMinEpochs() {
        return Integer.parseInt(get(MIN_EPOCH_PARAMETER_NAME));
    }

    @Override
    public double getMinImprovement() {
        return Double.parseDouble(get(MIN_IMPROVEMENT_PARAMETER_NAME));
    }

    @Override
    public double getRegularization() {
        return Double.parseDouble(get(REGULARIZATION_PARAMETER_NAME));
    }

    public String get(String key) {
        return getProperties().getProperty(key);
    }

    public Properties getProperties() {
        return properties;
    }

    private Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        ClassLoader loader = LSIPropertiesLoaderImpl.class.getClassLoader();
        URL url = loader.getResource(PROPERTIES_FILENAME);
        InputStream stream = url.openStream();
        try {
            properties.load(stream);
        } finally {
            stream.close();
        }
        return properties;
    }
}
