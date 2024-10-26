package ch.ethz.mxquery.functions.fn;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Vector;
import ch.ethz.mxquery.android.MXQuery;
import ch.ethz.mxquery.contextConfig.Context;
import ch.ethz.mxquery.datamodel.types.Type;
import ch.ethz.mxquery.datamodel.types.TypeInfo;
import ch.ethz.mxquery.datamodel.types.TypeLexicalConstraints;
import ch.ethz.mxquery.datamodel.xdm.BooleanToken;
import ch.ethz.mxquery.exceptions.DynamicException;
import ch.ethz.mxquery.exceptions.ErrorCodes;
import ch.ethz.mxquery.exceptions.MXQueryException;
import ch.ethz.mxquery.model.TokenBasedIterator;
import ch.ethz.mxquery.model.XDMIterator;
import ch.ethz.mxquery.util.IOLib;

public class DocAvailable extends TokenBasedIterator {

    protected void init() throws MXQueryException {
        String add = getStringValueOrEmpty(subIters[0]);
        if (add == null) {
            currentToken = BooleanToken.FALSE_TOKEN;
            return;
        }
        URI uri;
        if (!TypeLexicalConstraints.isValidURI(add)) throw new DynamicException(ErrorCodes.F0017_INVALID_ARGUMENT_TO_FN_DOC, "Invalid URI given to fn:doc-available", loc);
        try {
            if (TypeLexicalConstraints.isAbsoluteURI(add)) {
                uri = new URI(add);
            } else {
                uri = new URI(IOLib.convertToAndroid(add));
            }
        } catch (URISyntaxException se) {
            throw new DynamicException(ErrorCodes.F0017_INVALID_ARGUMENT_TO_FN_DOC, "Invalid URI given to fn:doc-available", loc);
        }
        if (add.startsWith("http://")) {
            URL url;
            try {
                url = uri.toURL();
            } catch (MalformedURLException e) {
                throw new DynamicException(ErrorCodes.F0017_INVALID_ARGUMENT_TO_FN_DOC, "Invalid URI given to fn:doc-available", loc);
            }
            try {
                InputStream in = url.openStream();
                in.close();
            } catch (IOException e) {
                currentToken = BooleanToken.FALSE_TOKEN;
                return;
            }
            currentToken = BooleanToken.TRUE_TOKEN;
        } else {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(MXQuery.getContext().openFileInput(uri.toString())));
                currentToken = BooleanToken.TRUE_TOKEN;
            } catch (FileNotFoundException e) {
                currentToken = BooleanToken.FALSE_TOKEN;
            } catch (IOException e) {
                currentToken = BooleanToken.FALSE_TOKEN;
            }
        }
    }

    public TypeInfo getStaticType() {
        return new TypeInfo(Type.BOOLEAN, Type.OCCURRENCE_IND_EXACTLY_ONE);
    }

    protected XDMIterator copy(Context context, XDMIterator[] subIters, Vector nestedPredCtxStack) throws MXQueryException {
        XDMIterator copy = new DocAvailable();
        copy.setContext(context, true);
        copy.setSubIters(subIters);
        return copy;
    }
}
