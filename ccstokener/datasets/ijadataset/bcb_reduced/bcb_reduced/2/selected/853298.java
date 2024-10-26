package org.exist.xquery.functions.util;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import org.exist.dom.QName;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;

/**
 * @author Pierrick Brihaye
 * @author Dizzzz
 *
 */
public class FileRead extends BasicFunction {

    public static final FunctionSignature signatures[] = { new FunctionSignature(new QName("file-read", UtilModule.NAMESPACE_URI, UtilModule.PREFIX), "Read content of file $a", new SequenceType[] { new SequenceType(Type.ITEM, Cardinality.EXACTLY_ONE) }, new SequenceType(Type.STRING, Cardinality.ZERO_OR_ONE)), new FunctionSignature(new QName("file-read", UtilModule.NAMESPACE_URI, UtilModule.PREFIX), "Read content of file $a with the encoding specified in $b.", new SequenceType[] { new SequenceType(Type.ITEM, Cardinality.EXACTLY_ONE), new SequenceType(Type.STRING, Cardinality.EXACTLY_ONE) }, new SequenceType(Type.STRING, Cardinality.ZERO_OR_ONE)) };

    /**
	 * @param context
	 * @param signature
	 */
    public FileRead(XQueryContext context, FunctionSignature signature) {
        super(context, signature);
    }

    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {
        String arg = args[0].itemAt(0).getStringValue();
        StringWriter sw;
        try {
            URL url = new URL(arg);
            InputStreamReader isr;
            if (args.length > 1) isr = new InputStreamReader(url.openStream(), arg = args[1].itemAt(0).getStringValue()); else isr = new InputStreamReader(url.openStream());
            sw = new StringWriter();
            char[] buf = new char[1024];
            int len;
            while ((len = isr.read(buf)) > 0) {
                sw.write(buf, 0, len);
            }
            isr.close();
            sw.close();
        } catch (MalformedURLException e) {
            throw new XPathException(getASTNode(), e.getMessage());
        } catch (IOException e) {
            throw new XPathException(getASTNode(), e.getMessage());
        }
        return new StringValue(sw.toString());
    }
}
