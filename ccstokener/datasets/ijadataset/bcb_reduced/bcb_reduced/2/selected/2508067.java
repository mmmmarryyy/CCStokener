package org.translationcomponent.google;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Translate a text or detect a language.
 * 
 * <p>
 * Calls Google Translation API.
 * </p>
 * 
 * <p>
 * Sends a RESTful HTTP request and Google returns a JSON object.
 * </p>
 * 
 * <p>
 * The translation text is limited to 5000 characters. (in May 2009 this might
 * change in the future)
 * </p>
 * 
 * <p>
 * Configure an instance of this class once and use it as a Singleton. Calls to
 * getLanguage() and getTranslation() are thread safe.
 * </p>
 * 
 * <p>
 * Known issues:
 * 
 * <ol>
 * <li>When passing in ",&amp;" it is translated to ", &amp;". So Google inserts
 * an extra space. Inserting whitespace is observed and is acceptable for now.</li>
 * 
 * <li>Google resolves Entity references. So &amp;nbsp; becomes a space.
 * &amp;euro; becomes the euro character. Actually if &amp;nbsp; is in between
 * tags (for example &lt;td>&amp;nbsp;&lt;/td>) it stays an &amp;nbsp; As this
 * is correct for HTML it is acceptable.</li>
 * 
 * <li>It sometimes changes the HTML: it moves tags. Also if the text starts
 * with a close tag it can remove the close tag.</li>
 * 
 * <li>It inserts spaces, line breaks.</li>
 * 
 * <li>In a &lt;![CDATA[]]> the last ">" can become &amp;gt;.</li>
 * 
 * <li>It translates characters into HTML entities. So "&amp;" can be returned
 * as "&amp;amp;" If you do not want this use a Helper Class to change them back
 * to Unicode characters. For example use HTMLEntities at Tecnick.com:</li>
 * 
 * <a href=
 * "http://www.tecnick.com/public/code/cp_dpage.php?aiocp_dp=htmlentities">
 * http://www.tecnick.com/public/code/cp_dpage.php?aiocp_dp=htmlentities </a>
 * </li>
 * </ol>
 * </p>
 * 
 * <p>
 * The Translator Component has a HTML parser that takes out the texts and only
 * sends the text to Google Translate. See the "translation-service" module for
 * this HTML parser.
 * </p>
 * 
 * 
 * <p>
 * For the ones who want to change this class. Possible Google JSON responses:
 * 
 * <ul>
 * <li>
 * {"responseData":{"translatedText":"hola"},"responseDetails":null,
 * "responseStatus":200}</li>
 * 
 * <li>
 * {"responseData":{"detectedSourceLanguage":"en","translatedText":"hola"},
 * "responseDetails":null,"responseStatus":200}</li>
 * 
 * <li>
 * {"responseData":null,"responseDetails":"invalid translation language pair",
 * "responseStatus":400}</li>
 * 
 * <li>
 * {"responseData":null,"responseDetails":"invalid translation language pair",
 * "responseStatus":400}</li>
 * 
 * <li>
 * {"responseData":null,"responseDetails":
 * "the string to be translated exceeds the maximum length allowed."
 * ,"responseStatus":400}</li>
 * 
 * <li>
 * {"responseData":null,"responseDetails":"invalid key","responseStatus":400}</li>
 * 
 * <li>
 * {"responseData":null,"responseDetails":"invalid result data","responseStatus"
 * :404}</li>
 * </ul>
 * 
 * </p>
 * 
 * @author ROB
 * 
 */
public class GoogleTranslator {

    /**
	 * Configuration to make calls.
	 */
    private GoogleConfiguration configuration = new GoogleConfiguration();

    public GoogleTranslator() {
        super();
    }

    /**
	 * Translate a text.
	 * 
	 * Uses the HTTP Referer and Google API key configured in the
	 * GoogleConfiguration class.
	 * 
	 * 
	 * @param sourceLanguage
	 *            The language code of the source. For example "en" or "zh-CN".
	 *            See <a href=
	 *            "http://code.google.com/apis/ajaxlanguage/documentation/#SupportedLanguages"
	 *            >http://code.google.com/apis/ajaxlanguage/documentation/#
	 *            SupportedLanguages</a> Pass null or a blank string to
	 *            auto-detect the language.
	 * @param sourceText
	 *            The text to translate. Currently limited to 5000 characters.
	 * @param translationLanguage
	 *            The target language.
	 * @return Translation result.
	 * @see org.translationcomponent.google.GoogleLanguage List of supported
	 *      languages.
	 * @see org.translationcomponent.google.GoogleConfiguration Configuration.
	 */
    public Translation getTranslation(final String sourceLanguage, final CharSequence sourceText, final String translationLanguage) {
        return this.getTranslation(sourceLanguage, sourceText, translationLanguage, null, null);
    }

    /**
	 * Translate a text.
	 * 
	 * @param sourceLanguage
	 *            The language code for the original text. Pass null or an empty
	 *            string to have Google auto-detect the language. Google will
	 *            default always to english if language cannot be auto-detected
	 *            by Google. For supported languages see <a href=
	 *            "http://code.google.com/apis/ajaxlanguage/documentation/#SupportedLanguages"
	 *            >http://code.google.com/apis/ajaxlanguage/documentation/#
	 *            SupportedLanguages</a>
	 * @param sourceText
	 *            The text to translate. Can be null or an empty string in which
	 *            case an empty string is returned.
	 * @param targetLanguage
	 *            The language to translate to.
	 * @param urlReferer
	 *            The URL of the HTML page the text is coming from. Use your
	 *            companies website if you do not have the URL of the page. This
	 *            is a requirment of Google. Though the translation works
	 *            without it.
	 * @param googleKey
	 *            The unique Google key. You have to register with Google to get
	 *            this key. See
	 *            http://code.google.com/apis/ajaxfeeds/signup.html
	 * @return The translation result.
	 * @see org.translationcomponent.google.GoogleLanguage List of supported
	 *      languages.
	 * @see org.translationcomponent.google.GoogleConfiguration Configuration.
	 */
    public Translation getTranslation(String sourceLanguage, final CharSequence sourceText, final String targetLanguage, final String urlReferer, final String googleKey) {
        if (sourceText == null || sourceText.length() == 0) {
            return new Translation(sourceLanguage, "", targetLanguage, "", GoogleStatus.OK, null);
        }
        if (configuration.getTranslationMaximumLength() != -1 && sourceText.length() > configuration.getTranslationMaximumLength()) {
            return new Translation(sourceLanguage, sourceText, targetLanguage, "", GoogleStatus.MAXIMUM_LENGTH_EXCEEDED, "Text exceeds maximum length. Text size: " + sourceText.length() + ", maximum size: " + configuration.getTranslationMaximumLength());
        }
        if (targetLanguage == null || targetLanguage.length() == 0) {
            return new Translation(sourceLanguage, sourceText, targetLanguage, "", GoogleStatus.TARGET_LANGUAGE_CODE_INVALID, "Target language is not selected.");
        }
        Translation out = null;
        for (int counter = 1; out == null && counter <= configuration.getRetryCount(); counter++) {
            try {
                HttpURLConnection conn = (HttpURLConnection) configuration.getGoogleTranslateURL().openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(configuration.getConnectionTimeout());
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=" + configuration.getCharacterEncoding());
                if (urlReferer != null && urlReferer.length() != 0) {
                    conn.setRequestProperty("Referer", urlReferer);
                } else if (configuration.getDefaultReferer() != null) {
                    conn.setRequestProperty("Referer", configuration.getDefaultReferer());
                }
                conn.setDoOutput(true);
                conn.setDoInput(true);
                if (configuration.isThrowSocketConnection()) {
                    throw new SocketTimeoutException();
                }
                OutputStreamWriter w = new OutputStreamWriter(conn.getOutputStream(), configuration.getPostBodyCharacterSet());
                try {
                    if (googleKey != null && googleKey.length() != 0) {
                        w.write("key=");
                        w.write(URLEncoder.encode(googleKey, configuration.getCharacterEncoding()));
                    } else if (configuration.getGoogleKey() != null) {
                        w.write("key=");
                        w.write(URLEncoder.encode(configuration.getGoogleKey(), configuration.getCharacterEncoding()));
                    }
                    w.write("&v=");
                    w.write(this.configuration.getGoogleApiVersion());
                    w.write("&langpair=");
                    if (sourceLanguage != null) {
                        w.write(sourceLanguage);
                    }
                    w.write(URLEncoder.encode("|", configuration.getCharacterEncoding()));
                    w.write(targetLanguage);
                    w.write("&q=");
                    w.write(URLEncoder.encode(sourceText.toString(), configuration.getCharacterEncoding()));
                } finally {
                    w.flush();
                    w.close();
                }
                int httpStatus = conn.getResponseCode();
                if (httpStatus == 200) {
                    try {
                        JSONObject json = null;
                        InputStreamReader reader = new InputStreamReader(conn.getInputStream(), configuration.getCharacterEncoding());
                        try {
                            json = new JSONObject(new JSONTokener(reader));
                        } finally {
                            reader.close();
                        }
                        int googleStatus = json.getInt("responseStatus");
                        if (googleStatus == 200) {
                            final JSONObject responseData = json.getJSONObject("responseData");
                            String translation = responseData.getString("translatedText");
                            if (configuration.isConvertToHTMLEntities()) {
                                translation = HTMLEntities.convertToEntities(translation);
                            }
                            if (sourceLanguage == null && responseData.has("detectedSourceLanguage")) {
                                sourceLanguage = responseData.getString("detectedSourceLanguage");
                            }
                            out = new Translation(sourceLanguage, sourceText, targetLanguage, translation, GoogleStatus.OK, null);
                        } else {
                            if (googleStatus == 404 && counter < configuration.getRetryCount()) {
                            } else {
                                String errorMessage = json.getString("responseDetails");
                                GoogleStatus status = GoogleStatus.ERROR;
                                if (googleStatus == 400) {
                                    if ("invalid translation language pair".equals(errorMessage)) {
                                        if (sourceLanguage == null) {
                                            status = GoogleStatus.TARGET_LANGUAGE_CODE_INVALID;
                                        } else {
                                            status = GoogleStatus.LANGUAGE_PAIR_NOT_SUPPORTED;
                                        }
                                    } else if ("the string to be translated exceeds the maximum length allowed.".equals(errorMessage)) {
                                        status = GoogleStatus.MAXIMUM_LENGTH_EXCEEDED;
                                    } else if ("invalid key".equals(errorMessage)) {
                                        if (googleKey == null && configuration.getGoogleKey() == null) {
                                            errorMessage = "Google key is not configured.";
                                        }
                                    }
                                }
                                String error = "Google responseStatus=" + googleStatus + ". " + errorMessage;
                                out = new Translation(sourceLanguage, sourceText, targetLanguage, "", status, error);
                            }
                        }
                    } catch (JSONException e) {
                        out = new Translation(sourceLanguage, sourceText, targetLanguage, "", GoogleStatus.ERROR, e.getClass().getName() + ": " + e.getMessage());
                    }
                } else {
                    StringWriter writer = new StringWriter();
                    try {
                        writer.write("Http Status: ");
                        writer.write(String.valueOf(httpStatus));
                        writer.write(" \n");
                        InputStream errorStream = conn.getErrorStream();
                        if (errorStream != null) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), configuration.getCharacterEncoding()));
                            try {
                                char[] buffer = new char[1024];
                                int count = 0;
                                while (-1 != (count = reader.read(buffer))) {
                                    writer.write(buffer, 0, count);
                                }
                            } finally {
                                reader.close();
                            }
                        }
                    } finally {
                        writer.close();
                    }
                    if (counter == configuration.getRetryCount()) {
                        out = new Translation(sourceLanguage, sourceText, targetLanguage, "", GoogleStatus.ERROR, writer.toString());
                    }
                }
            } catch (IOException e) {
                if (counter == configuration.getRetryCount()) {
                    out = new Translation(sourceLanguage, sourceText, targetLanguage, "", GoogleStatus.CONNECTION_FAILED, e.getClass().getName() + ": " + e.getMessage());
                }
            }
        }
        return out;
    }

    /**
	 * Detects the language of a text.
	 * 
	 * Uses the HTTP Referer and Google API key configured in the
	 * GoogleConfiguration class.
	 * 
	 * @param sourceText
	 *            The text.
	 * @return The language detected. If you pass in nonsense it will always
	 *         return "en" (English). English is the default language.
	 * @see org.translationcomponent.google.GoogleConfiguration Configuration.
	 */
    public LanguageDetected getLanguage(CharSequence sourceText) {
        return this.getLanguage(sourceText, null, null);
    }

    /**
	 * Detect the language.
	 * 
	 * @param sourceText
	 *            Source.
	 * @param urlReferer
	 *            HTTP Referer header. The URL of your website or the HTML page.
	 * @param googleKey
	 *            Unique Google key. It will work without but Google might block
	 *            you after a while.
	 * @return Detected language.
	 * @see org.translationcomponent.google.GoogleConfiguration Configuration.
	 */
    public LanguageDetected getLanguage(CharSequence sourceText, final String urlReferer, final String googleKey) {
        if (sourceText == null || sourceText.length() == 0) {
            return new LanguageDetected(null, false, 0D, sourceText, GoogleStatus.ERROR, "Text is empty");
        }
        if (configuration.getDetectMaximumLength() != -1 && sourceText.length() > configuration.getDetectMaximumLength()) {
            sourceText = sourceText.subSequence(0, configuration.getDetectMaximumLength());
            int pos = sourceText.toString().lastIndexOf(' ');
            if (pos != -1) {
                sourceText = sourceText.subSequence(0, pos);
            }
        }
        LanguageDetected out = null;
        for (int counter = 1; out == null && counter <= configuration.getRetryCount(); counter++) {
            try {
                final String textEncoded = URLEncoder.encode(sourceText.toString(), configuration.getCharacterEncoding());
                final StringBuilder url = new StringBuilder(configuration.getGoogleDetectURL().length() + 25 + textEncoded.length());
                url.append(configuration.getGoogleDetectURL());
                url.append("?v=");
                url.append(this.configuration.getGoogleApiVersion());
                if (googleKey != null && googleKey.length() != 0) {
                    url.append("&key=");
                    url.append(URLEncoder.encode(googleKey, configuration.getCharacterEncoding()));
                } else if (configuration.getGoogleKey() != null) {
                    url.append("&key=");
                    url.append(URLEncoder.encode(configuration.getGoogleKey(), configuration.getCharacterEncoding()));
                }
                url.append("&q=");
                url.append(textEncoded);
                HttpURLConnection conn = (HttpURLConnection) new URL(url.toString()).openConnection();
                conn.setConnectTimeout(configuration.getConnectionTimeout());
                if (urlReferer != null && urlReferer.length() != 0) {
                    conn.setRequestProperty("Referer", urlReferer);
                } else if (configuration.getDefaultReferer() != null) {
                    conn.setRequestProperty("Referer", configuration.getDefaultReferer());
                }
                conn.setDoOutput(true);
                if (configuration.isThrowSocketConnection()) {
                    throw new SocketTimeoutException();
                }
                int httpStatus = conn.getResponseCode();
                if (httpStatus == 200) {
                    try {
                        JSONObject json = null;
                        InputStreamReader reader = new InputStreamReader(conn.getInputStream(), configuration.getCharacterEncoding());
                        try {
                            json = new JSONObject(new JSONTokener(reader));
                        } finally {
                            reader.close();
                        }
                        int googleStatus = json.getInt("responseStatus");
                        if (googleStatus == 200) {
                            final JSONObject responseData = json.getJSONObject("responseData");
                            final String language = responseData.getString("language");
                            final boolean isReliable = responseData.getBoolean("isReliable");
                            final double confidence = responseData.getDouble("confidence");
                            out = new LanguageDetected(language, isReliable, confidence, sourceText, GoogleStatus.OK, null);
                        } else {
                            if (googleStatus == 404 && counter < configuration.getRetryCount()) {
                            } else {
                                String errorMessage = json.getString("responseDetails");
                                GoogleStatus status = GoogleStatus.ERROR;
                                if (googleStatus == 400) {
                                    if ("the string to be translated exceeds the maximum length allowed.".equals(errorMessage)) {
                                        status = GoogleStatus.MAXIMUM_LENGTH_EXCEEDED;
                                    } else if ("invalid key".equals(errorMessage)) {
                                        if (googleKey == null && configuration.getGoogleKey() == null) {
                                            errorMessage = "Google key is not configured.";
                                        }
                                    }
                                }
                                String error = "Google responseStatus=" + googleStatus + ". " + errorMessage;
                                out = new LanguageDetected(null, false, 0.0d, sourceText, status, error);
                            }
                        }
                    } catch (JSONException e) {
                        out = new LanguageDetected(null, false, 0.0d, sourceText, GoogleStatus.ERROR, e.getClass().getName() + ": " + e.getMessage());
                    }
                } else {
                    StringWriter writer = new StringWriter();
                    try {
                        writer.write("Http Status: ");
                        writer.write(String.valueOf(httpStatus));
                        writer.write(" \n");
                        InputStream errorStream = conn.getErrorStream();
                        if (errorStream != null) {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getErrorStream(), configuration.getCharacterEncoding()));
                            try {
                                char[] buffer = new char[1024];
                                int count = 0;
                                while (-1 != (count = reader.read(buffer))) {
                                    writer.write(buffer, 0, count);
                                }
                            } finally {
                                reader.close();
                            }
                        }
                    } finally {
                        writer.close();
                    }
                    if (counter == configuration.getRetryCount()) {
                        out = new LanguageDetected(null, false, 0.0d, sourceText, GoogleStatus.ERROR, writer.toString());
                    }
                }
            } catch (IOException e) {
                if (counter == configuration.getRetryCount()) {
                    out = new LanguageDetected(null, false, 0.0d, sourceText, GoogleStatus.CONNECTION_FAILED, e.getClass().getName() + ": " + e.getMessage());
                }
            }
        }
        return out;
    }

    /**
	 * Get the configuration.
	 * 
	 * @return configuration.
	 */
    public GoogleConfiguration getConfiguration() {
        return configuration;
    }

    /**
	 * Set the configuration.
	 * 
	 * Calls to getLanguage() and getTranslation() will use the new
	 * configuration immediately after it is updated.
	 * 
	 * @param configuration
	 *            New configuration.
	 */
    public void setConfiguration(GoogleConfiguration configuration) {
        this.configuration = configuration;
    }
}
