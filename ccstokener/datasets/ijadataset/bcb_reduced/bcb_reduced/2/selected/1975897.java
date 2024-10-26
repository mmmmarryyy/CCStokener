package org.gguth.input;

import org.antlr.runtime.CharStream;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.tree.TreeAdaptor;
import java.net.URL;
import java.io.IOException;
import org.gguth.InstrumentConstructor;
import org.gguth.InputTypeNotSupportedException;

/**
 * Created by IntelliJ IDEA.
 * User: jbunting
 * Date: Nov 16, 2008
 * Time: 1:13:56 PM
 * To change this template use File | Settings | File Templates.
 */
public class UrlInput extends AbstractCharStreamInput {

    private URL url;

    public UrlInput(URL url, InstrumentConstructor instrumentConstructor) {
        super(instrumentConstructor);
        this.url = url;
    }

    public UrlInput(URL url, InstrumentConstructor instrumentConstructor, TreeAdaptor treeAdaptor, String walksRule, Object... walksRuleParameters) {
        super(instrumentConstructor, treeAdaptor, walksRule, walksRuleParameters);
        this.url = url;
    }

    public CharStream createLexerInput() {
        try {
            return new ANTLRInputStream(url.openStream());
        } catch (IOException e) {
            throw new InputTypeNotSupportedException("Could not load url %s.", e, url);
        }
    }
}
