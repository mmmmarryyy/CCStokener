package org.jcvi.glk.elvira.flu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jcvi.glk.Assembly;
import org.jcvi.glk.Extent;
import org.jcvi.glk.elvira.Validator;

/**
 * <code>FluNCBIValdator</code> is a {@link Validator}
 * implementation that calls the NCBI Influenza Virus Resource
 * to validate and annotate a Flu segment.
 *
 * @see <a href="http://www.ncbi.nlm.nih.gov/genomes/FLU/Database/annotation.cgi">
 * NCBI Influenza Virus Resource</a>
 * @author jsitz
 * @author dkatzel
 */
public class FluNCBIValdator implements Validator<FluSegmentValidationResult, Assembly> {

    private static final Pattern RESPONSE_PATTERN = Pattern.compile("<input type=\"hidden\" name=\"tbl_h\" value=(.*?)>");

    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    private static final Pattern TYPE_PATTERN = Pattern.compile("INFO: Virus type:\\s+influenza (\\S+)");

    private static final Pattern SEGMENT_NAME_PATTERN = Pattern.compile("INFO: Segment:\\s+(\\d+)\\s+\\((.*?)\\)");

    private static final Pattern LENGTH_PATTERN = Pattern.compile("INFO: Length: (\\d+)");

    private static final Pattern SEROTYPE_PATTERN = Pattern.compile("INFO: Serotype:\\s(\\S+)");

    private static final Pattern WARNING_PATTERN = Pattern.compile("WARNING: (\\S+.*\\S)\\s*");

    private static final Pattern ERROR_PATTERN = Pattern.compile("ERROR: (\\S+.*\\S)\\s*");

    private static final String UTF_8 = "UTF-8";

    private static final String NCBI_URL = "http://www.ncbi.nlm.nih.gov/genomes/FLU/Database/annotation.cgi";

    @Override
    public FluSegmentValidationResult validate(Extent segment, Assembly toBeValidated) {
        try {
            URLConnection connection = connectToNCBIValidator();
            requestValidationFor(toBeValidated, connection);
            String response = getResponseFrom(connection);
            return parseResponse(segment, toBeValidated, response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void requestValidationFor(Assembly toBeValidated, URLConnection connection) throws IOException {
        Writer writer = new OutputStreamWriter(connection.getOutputStream());
        writer.write(generateNCBIPostData(toBeValidated));
        writer.flush();
        writer.close();
    }

    private String getResponseFrom(URLConnection connection) throws IOException {
        BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            response.append(line);
        }
        rd.close();
        return response.toString();
    }

    private String convertIntoFASTAFormat(Assembly contig) {
        return convertIntoFastaFormat(contig.getComment(), contig.getUngappedConsensus());
    }

    private static String convertIntoFastaFormat(String name, String data) {
        StringBuilder result = new StringBuilder();
        result.append("<");
        result.append(name);
        result.append("\n");
        result.append(data.replaceAll("(.{60})", "$1\n"));
        return result.toString();
    }

    private FluSegmentValidationResult parseResponse(Extent segment, Assembly toBeValidated, String response) {
        Pattern pattern = RESPONSE_PATTERN;
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            String result = matcher.group(1);
            return new FluSegmentValidationResult(segment, toBeValidated, parseFluType(result), parseSegmentName(result), parseSeroType(result), parseSegmentLength(result), parseErrors(result));
        }
        return null;
    }

    private List<String> parseErrors(String result) {
        List<String> errors = new ArrayList<String>();
        parseErrorMessages(result, errors);
        parseWarningMessages(result, errors);
        return errors;
    }

    private void parseWarningMessages(String result, List<String> errors) {
        parseMessages(result, errors, WARNING_PATTERN, "WARNING: ");
    }

    private void parseMessages(String result, List<String> messageList, Pattern pattern, String messageDelimiter) {
        Matcher matcher = pattern.matcher(result);
        while (matcher.find()) {
            for (String warningMessage : matcher.group(1).split(messageDelimiter)) {
                messageList.add(warningMessage);
            }
        }
    }

    private void parseErrorMessages(String result, List<String> errors) {
        parseMessages(result, errors, ERROR_PATTERN, "ERROR: ");
    }

    private String parseSeroType(String result) {
        String seroType = null;
        Matcher seroTypeMatcher = SEROTYPE_PATTERN.matcher(result);
        if (seroTypeMatcher.find()) {
            seroType = seroTypeMatcher.group(1);
        }
        return seroType;
    }

    private int parseSegmentLength(String result) {
        int length = 0;
        Matcher lengthMatcher = LENGTH_PATTERN.matcher(result);
        if (lengthMatcher.find()) {
            length = Integer.parseInt(lengthMatcher.group(1));
        }
        return length;
    }

    private String parseSegmentName(String result) {
        String segmentName = null;
        Matcher segMatcher = SEGMENT_NAME_PATTERN.matcher(result);
        if (segMatcher.find()) {
            segmentName = segMatcher.group(2);
        }
        return segmentName;
    }

    private Character parseFluType(String result) {
        Character type = null;
        Matcher typeMatcher = TYPE_PATTERN.matcher(result);
        if (typeMatcher.find()) {
            type = typeMatcher.group(1).toUpperCase().charAt(0);
        }
        return type;
    }

    private String generateNCBIPostData(Assembly toBeValidated) throws IOException {
        StringBuilder result = new StringBuilder();
        postPair(result, "upfile", "");
        result.append("&");
        postPair(result, "SUBMIT", "Annotate FASTA");
        result.append("&");
        postPair(result, "sequence", convertIntoFASTAFormat(toBeValidated));
        return result.toString();
    }

    private void postPair(StringBuilder result, String key, String value) throws IOException {
        result.append(key);
        result.append("=");
        result.append(encode(value));
    }

    private Object encode(String string) throws IOException {
        return URLEncoder.encode(string, UTF_8);
    }

    private URLConnection connectToNCBIValidator() throws IOException {
        final URL url = new URL(NCBI_URL);
        URLConnection connection = url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestProperty("Content-Type", CONTENT_TYPE);
        return connection;
    }
}
