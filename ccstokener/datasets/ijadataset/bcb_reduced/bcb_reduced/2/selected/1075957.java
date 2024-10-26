package criticker.lib;

import java.net.URL;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import criticker.lib.parser.AbstractParser;
import android.os.AsyncTask;
import android.util.Log;

public class ParseTask extends AsyncTask<String, Void, Void> {

    public static final int RETURN_TEXT = 0;

    public static final int RETURN_PROCESS_LIST = 1;

    public static final int RETURN_LIST = 2;

    protected AbstractParser mParser = null;

    protected ApiResponse mCallback = null;

    protected int mReturnType = RETURN_TEXT;

    public ParseTask(ApiResponse callback, AbstractParser parser, int returnType) {
        mCallback = callback;
        mParser = parser;
        mReturnType = returnType;
    }

    @Override
    protected Void doInBackground(String... urls) {
        Log.d("ParseTask", "Getting URL " + urls[0]);
        try {
            XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
            reader.setContentHandler(mParser);
            reader.parse(new InputSource(new URL(urls[0]).openStream()));
        } catch (Exception e) {
            if (mCallback != null) mCallback.OnFailure(new ApiResponseObject(ApiResponse.RESPONSE_CRITICAL_FAILURE, e.getLocalizedMessage()));
        }
        return null;
    }

    protected void onPostExecute(Void unused) {
        if (mParser.DataValid()) {
            switch(mReturnType) {
                case RETURN_PROCESS_LIST:
                    FilmData.GetInstance().SetFilmList(mParser.GetData());
                case RETURN_LIST:
                    if (mCallback != null) mCallback.OnSuccess(new ApiResponseObjectList(ApiResponse.RESPONSE_SUCCESS, mParser.GetData()));
                    break;
                case RETURN_TEXT:
                default:
                    if (mCallback != null) mCallback.OnSuccess(new ApiResponseObject(ApiResponse.RESPONSE_SUCCESS, null));
                    break;
            }
        } else {
            if (mCallback != null) mCallback.OnFailure(new ApiResponseObject(ApiResponse.RESPONSE_FAILURE, mParser.GetErrorMessage()));
        }
    }
}
