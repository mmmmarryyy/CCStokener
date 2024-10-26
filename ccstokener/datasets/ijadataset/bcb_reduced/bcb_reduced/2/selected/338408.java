package org.google.translate.api.v2.core;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.google.translate.api.v2.core.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Provides access to the <a href="http://code.google.com/apis/language/translate/v2/getting_started.html">Google Translate API v2</a>.
 * <ul>
 * <li><a href="http://code.google.com/apis/language/translate/v2/getting_started.html#translate">translate</a> - Translates source text from source language to target language</li>
 * <li><a href="http://code.google.com/apis/language/translate/v2/getting_started.html#list_languages">languages</a> - List the source and target languages supported by the translate methods</li>
 * <li><a href="http://code.google.com/apis/language/translate/v2/getting_started.html#language_detect">detect</a> - Detect language of source text</li>
 * </ul>
 */
public class Translator implements Closeable {

    /**
     * Used to automatically avoid the Google API "Too many text segments" error - the {@link #detect(String[])} and
     * {@link #translate(String[], String, String)} methods, internally split the sourceTexts array if needed.
     */
    public static final int MAX_SOURCE_TEXTS = 128;

    private static final Logger LOGGER = LoggerFactory.getLogger(Translator.class);

    private static final String SCHEMA = "https";

    private static final String HOST = "www.googleapis.com";

    private static final String PARAMETERS_ENCODING = "UTF-8";

    private String apiKey;

    private HttpClient httpClient;

    /**
     * Create a Translator to allow access to the API using the apiKey and a {@link DefaultHttpClient}.\
     * After use, the translator should be closed using the {@link #close()} method.
     *
     * @param apiKey The Google account API key to use in order to access Google APIs, see
     *               <a href=http://code.google.com/apis/language/translate/v2/using_rest.html#auth>Using REST, authentication</a>
     */
    public Translator(String apiKey) {
        this(apiKey, new DefaultHttpClient());
    }

    /**
     * Create a Translator to allow access to the API using the apiKey and a {@link HttpClient}.
     * After use, the translator should be closed using the {@link #close()} method.
     *
     * @param apiKey     The Google account API key to use in order to access Google APIs, see
     *                   <a href=http://code.google.com/apis/language/translate/v2/using_rest.html#auth>Using REST, authentication</a>
     * @param httpClient The HTTP client to use when accessing the Google Translate API
     */
    public Translator(String apiKey, HttpClient httpClient) {
        this.apiKey = apiKey;
        this.httpClient = httpClient;
    }

    /**
     * Translating sourceText from sourceLanguage to targetLanguage.
     *
     * @param sourceText     The text to translate (Must be 5K or less due to Google Translate API limitations)
     * @param sourceLanguage The language code of the source text or null for auto detection.
     * @param targetLanguage The language code to translate to.
     * @return The translation result.
     * @throws URISyntaxException  In case of a malformed URI
     * @throws IOException         In case of an HTTP exception
     * @throws TranslatorException In case Google Translate API returned an error.
     */
    public Translation translate(String sourceText, String sourceLanguage, String targetLanguage) throws IOException, URISyntaxException, TranslatorException {
        return translate(new String[] { sourceText }, sourceLanguage, targetLanguage)[0];
    }

    /**
     * Translating sourceText from sourceLanguage to targetLanguage.
     *
     * @param sourceTexts    Texts to translate - each text can be in a different language (The total size of the texts
     *                       must be 5K or less due to Google Translate API limitations). Not limited by number of texts.
     * @param sourceLanguage The language code of the source text or null for auto detection.
     * @param targetLanguage The language code to translate to.
     * @return The translation results.
     * @throws URISyntaxException  In case of a malformed URI
     * @throws IOException         In case of an HTTP exception
     * @throws TranslatorException In case Google Translate API returned an error.
     */
    public Translation[] translate(String[] sourceTexts, String sourceLanguage, String targetLanguage) throws URISyntaxException, IOException, TranslatorException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("sourceTexts = " + Arrays.toString(sourceTexts) + ", sourceLanguage = " + sourceLanguage + ", targetLanguage = " + targetLanguage);
        }
        List<BasicNameValuePair> getParams = new ArrayList<BasicNameValuePair>(3);
        getParams.add(new BasicNameValuePair("key", apiKey));
        if (sourceLanguage != null) {
            getParams.add(new BasicNameValuePair("source", sourceLanguage));
        }
        getParams.add(new BasicNameValuePair("target", targetLanguage));
        URI uri = createURI("/language/translate/v2", getParams);
        int fromIndex = 0;
        int toIndex = sourceTexts.length;
        Translation[] allTranslations = null;
        if (toIndex > MAX_SOURCE_TEXTS) {
            allTranslations = new Translation[sourceTexts.length];
            toIndex = MAX_SOURCE_TEXTS;
        }
        do {
            HttpPost httpPost = createHttpPost(uri, sourceTexts, fromIndex, toIndex);
            TranslateResponse translateResponse = readResponse(httpPost, TranslateResponse.class);
            Translation[] segmentTranslations = translateResponse.getData().getTranslations();
            if (allTranslations != null) {
                System.arraycopy(segmentTranslations, 0, allTranslations, fromIndex, segmentTranslations.length);
            } else {
                allTranslations = segmentTranslations;
            }
            fromIndex = toIndex;
            toIndex = Math.min(sourceTexts.length, fromIndex + MAX_SOURCE_TEXTS);
        } while (fromIndex < sourceTexts.length);
        for (Translation translation : allTranslations) {
            translation.setTranslatedText(StringEscapeUtils.unescapeHtml4(translation.getTranslatedText()));
        }
        return allTranslations;
    }

    /**
     * Lists the supported languages. These languages can be used as the values of the sourceLanguage and targetLanguage
     * for the different API methods.
     *
     * @param targetLanguage Language code - If not null, the list of supported languages will contain the name of the language in the targetLanguage
     * @return Array of supported languages
     * @throws URISyntaxException  In case of a malformed URI
     * @throws IOException         In case of an HTTP exception
     * @throws TranslatorException In case Google Translate API returned an error.
     */
    public Language[] languages(String targetLanguage) throws URISyntaxException, IOException, TranslatorException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("targetLanguage = " + targetLanguage);
        }
        List<BasicNameValuePair> getParams = new ArrayList<BasicNameValuePair>(2);
        getParams.add(new BasicNameValuePair("key", apiKey));
        if (targetLanguage != null) {
            getParams.add(new BasicNameValuePair("target", targetLanguage));
        }
        URI uri = createURI("/language/translate/v2/languages", getParams);
        HttpGet httpGet = new HttpGet(uri);
        LanguagesResponse languagesResponse = readResponse(httpGet, LanguagesResponse.class);
        return languagesResponse.getData().getLanguages();
    }

    /**
     * Detects the language of a text.
     *
     * @param sourceTexts Texts to detect - each text can be in a different language (The total size of the texts
     *                    must be 5K or less due to Google Translate API limitations). Not limited by number of texts.
     * @return Matrix of detections - each detections[i] corresponds to sourceTexts[i] - if sourceTexts[i] can be
     *         associated with more than one language, detections[i] can contain multiple Detection objects.
     * @throws URISyntaxException  In case of a malformed URI
     * @throws IOException         In case of an HTTP exception
     * @throws TranslatorException In case Google Translate API returned an error.
     */
    public Detection[][] detect(String[] sourceTexts) throws URISyntaxException, IOException, TranslatorException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("sourceTexts = " + Arrays.toString(sourceTexts));
        }
        List<BasicNameValuePair> getParams = Arrays.asList(new BasicNameValuePair("key", apiKey));
        URI uri = createURI("/language/translate/v2/detect", getParams);
        int fromIndex = 0;
        int toIndex = sourceTexts.length;
        Detection[][] allDetections = null;
        if (toIndex > MAX_SOURCE_TEXTS) {
            allDetections = new Detection[sourceTexts.length][];
            toIndex = MAX_SOURCE_TEXTS;
        }
        do {
            HttpPost httpPost = createHttpPost(uri, sourceTexts, fromIndex, toIndex);
            DetectResponse detectionResponse = readResponse(httpPost, DetectResponse.class);
            Detection[][] segmentDetections = detectionResponse.getData().getDetections();
            if (allDetections != null) {
                System.arraycopy(segmentDetections, 0, allDetections, fromIndex, segmentDetections.length);
            } else {
                allDetections = segmentDetections;
            }
            fromIndex = toIndex;
            toIndex = Math.min(sourceTexts.length, fromIndex + MAX_SOURCE_TEXTS);
        } while (fromIndex < sourceTexts.length);
        return allDetections;
    }

    /**
     * Creates an HttpPost with a q parameter for every source text.
     * </br>
     * Using POST so the limit of the q parameter will be 5K instead of a 2K limit for the URL when using GET.
     *
     * @param uri         The URI to pass to the {@link HttpPost#HttpPost(java.net.URI)} constructor.
     * @param sourceTexts The texts to add as multiple q parameters, only texts from fromIndex to toIndex will be sent
     * @param fromIndex Starting index for the sourceTexts array (inclusive)
     * @param toIndex Ending index for the sourceTexts array (exclusive)
     * @return The created HttpPost
     * @throws UnsupportedEncodingException if the {@link #PARAMETERS_ENCODING} isn't supported
     */
    protected HttpPost createHttpPost(URI uri, String[] sourceTexts, int fromIndex, int toIndex) throws UnsupportedEncodingException {
        HttpPost httpPost = new HttpPost(uri);
        httpPost.addHeader("X-HTTP-Method-Override", "GET");
        List<BasicNameValuePair> postParams = new ArrayList<BasicNameValuePair>(sourceTexts.length);
        for (int index = fromIndex; index < toIndex; index++) {
            String sourceText = sourceTexts[index];
            postParams.add(new BasicNameValuePair("q", sourceText));
        }
        UrlEncodedFormEntity entity = new UrlEncodedFormEntity(postParams, PARAMETERS_ENCODING);
        httpPost.setEntity(entity);
        return httpPost;
    }

    /**
     * Creates a URI from the {@link #SCHEMA}, {@link #HOST}, default port, path and parameters.
     *
     * @param path      The path section of the URI
     * @param getParams the parameters for the GET method
     * @return The created URI
     * @throws URISyntaxException If both a scheme and a path are given but the path is relative,
     *                            if the URI string constructed from the given components violates RFC 2396,
     *                            or if the authority component of the string is present but cannot be parsed as a server-based authority
     */
    protected URI createURI(String path, List<BasicNameValuePair> getParams) throws URISyntaxException {
        return URIUtils.createURI(SCHEMA, HOST, -1, path, URLEncodedUtils.format(getParams, PARAMETERS_ENCODING), null);
    }

    /**
     * Executes the request and creates a clazz instance from the response.
     *
     * @param httpUriRequest The request object
     * @param clazz          The class that the request represents
     * @param <T>            Any class that extends AbstractResponse.
     * @return An object of type clazz that was created from the content of the response.
     * @throws IOException         If an exception occurred while executing the request, reading the response or converting the
     *                             response to an instance of clazz.
     * @throws TranslatorException In case Google Translate API returned an error.
     */
    protected <T extends AbstractResponse> T readResponse(HttpUriRequest httpUriRequest, Class<T> clazz) throws IOException, TranslatorException {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Executing request " + httpUriRequest.getURI());
        }
        HttpResponse httpResponse = httpClient.execute(httpUriRequest);
        String response = EntityUtils.toString(httpResponse.getEntity());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Reading '" + response + "' into " + clazz.getName());
        }
        T abstractResponse = TranslatorObjectMapper.instance().readValue(response, clazz);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Response object " + abstractResponse);
        }
        if (abstractResponse.getError() != null) {
            throw new TranslatorException(abstractResponse.getError());
        }
        return abstractResponse;
    }

    /**
     * Shuts down the connection manager and invalidates this Translator instance.
     */
    @Override
    public void close() {
        httpClient.getConnectionManager().shutdown();
    }
}
