package juploader.upload.plugin.glowfoto;

import juploader.httpclient.FileUploadRequest;
import juploader.httpclient.GetPageRequest;
import juploader.httpclient.HttpResponse;
import juploader.httpclient.NVPair;
import juploader.httpclient.exceptions.RequestCancelledException;
import juploader.parsing.Parser;
import juploader.parsing.exceptions.ParsingException;
import juploader.plugin.Plugin;
import juploader.upload.UploadResult;
import juploader.upload.exceptions.ChangesOnServerException;
import juploader.upload.exceptions.ConnectionException;
import juploader.upload.plugin.AbstractUploadProvider;
import juploader.upload.plugin.UploadOptionsPanel;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.ResourceBundle;

/** @author Adam Pawelec */
@Plugin(author = "Adam Pawelec", authorEmail = "proktor86@gmail.com", name = "GlowFoto", version = 1, iconPath = "/juploader/upload/plugin/glowfoto/icon.png", maxFileSize = 1572864, supportedExtensions = { "png", "jpg", "jpeg", "gif" })
public class GlowFoto extends AbstractUploadProvider {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("juploader/upload/plugin/glowfoto/Bundle");

    public UploadResult upload(File file, List<NVPair> parameters) throws ConnectionException, ChangesOnServerException, FileNotFoundException, RequestCancelledException {
        String action = readAction();
        FileUploadRequest request = createFileUploadRequest(parameters);
        request.setUrl(action);
        request.setFile("image", file);
        return executeRequestAndParseResponse(request, file);
    }

    protected String readAction() throws RequestCancelledException, ChangesOnServerException, ConnectionException {
        GetPageRequest request = getHttpClient().createGetPageRequest();
        request.setUrl("http://www.glowfoto.com");
        HttpResponse response = executeRequest(request);
        try {
            Parser parser = new Parser(response.getResponseBody());
            String action = parser.parseOne("enctype=\"multipart/form-data\" method=\"post\" action=\"(.*)\" style");
            closeReponse(response);
            return action;
        } catch (ParsingException ex) {
            throw new ChangesOnServerException();
        } catch (IOException ex) {
            throw new ChangesOnServerException();
        }
    }

    @Override
    protected UploadResult parseResponseBody(InputStream responseBody, File file) throws ChangesOnServerException {
        try {
            UploadResult uploadResult = new UploadResult(file, 5);
            Parser parser = new Parser(responseBody);
            List<String> links = parser.parseMany("onFocus='this.select\\(\\);' value=\"(.*)\"", 5);
            if (links.size() != 5) {
                throw new ChangesOnServerException();
            }
            uploadResult.addLink(bundle.getString("result.direct"), links.get(3));
            uploadResult.addLink(bundle.getString("result.static"), links.get(4));
            uploadResult.addLink(bundle.getString("result.bbcode"), links.get(0));
            uploadResult.addLink(bundle.getString("result.html"), links.get(1));
            uploadResult.addLink(bundle.getString("result.wiki"), links.get(2));
            return uploadResult;
        } catch (IOException ex) {
            throw new ChangesOnServerException();
        }
    }

    public UploadOptionsPanel getUploadOptionsPanel() {
        return new GlowFotoOptionsPanel();
    }
}
