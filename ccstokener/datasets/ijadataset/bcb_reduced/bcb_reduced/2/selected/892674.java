package juploader.upload.plugin.zippyshare;

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
@Plugin(name = "ZippyShare", author = "Adam Pawelec", authorEmail = "proktor86@gmail.com", version = 1, iconPath = "/juploader/upload/plugin/zippyshare/icon.png", supportedExtensions = "\\w+", maxFileSize = 209715200)
public class ZippyShare extends AbstractUploadProvider {

    private static final ResourceBundle bundle = ResourceBundle.getBundle("juploader/upload/plugin/zippyshare/Bundle");

    public UploadResult upload(File file, List<NVPair> parameters) throws ConnectionException, ChangesOnServerException, FileNotFoundException, RequestCancelledException {
        String action = parseAction();
        FileUploadRequest request = createFileUploadRequest(parameters);
        request.setUrl(action);
        request.setFile("file_0", file);
        return executeRequestAndParseResponse(request, file);
    }

    @Override
    protected UploadResult parseResponseBody(InputStream responseBody, File file) throws ChangesOnServerException {
        UploadResult result = new UploadResult(file, 5);
        try {
            Parser parser = new Parser(responseBody);
            List<String> links = parser.parseMany("value=\"(.*)\"\\s+class=\"text_field\"/", 5);
            if (links.size() != 1 && links.size() != 5) {
                throw new ChangesOnServerException();
            }
            result.addLink(bundle.getString("result.link"), links.get(0));
            if (links.size() > 1) {
                result.addLink(bundle.getString("result.forum"), links.get(1));
                result.addLink(bundle.getString("result.altForum"), links.get(2));
                result.addLink(bundle.getString("result.embed"), links.get(3));
                result.addLink(bundle.getString("result.altEmbed"), links.get(4));
            }
            return result;
        } catch (IOException ex) {
            throw new ChangesOnServerException();
        }
    }

    public UploadOptionsPanel getUploadOptionsPanel() {
        return null;
    }

    protected String parseAction() throws ChangesOnServerException, ConnectionException, RequestCancelledException {
        GetPageRequest request = getHttpClient().createGetPageRequest();
        request.setUrl("http://www.zippyshare.com/index_old.jsp");
        HttpResponse response = executeRequest(request);
        try {
            Parser p = new Parser(response.getResponseBody());
            String action = p.parseOne("enctype=\"multipart/form-data\" action=\"(.*)\">");
            return action;
        } catch (ParsingException ex) {
            throw new ChangesOnServerException();
        } catch (IOException ex) {
            throw new ChangesOnServerException();
        }
    }
}
