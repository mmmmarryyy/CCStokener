package org.dengues.commons.ui.swt.colorstyledtext.scanner;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import org.dengues.commons.DenguesCommonsPlugin;
import org.dengues.commons.ui.swt.colorstyledtext.ColorManager;
import org.dengues.commons.ui.swt.colorstyledtext.jedit.EOLSpan;
import org.dengues.commons.ui.swt.colorstyledtext.jedit.IVisitor;
import org.dengues.commons.ui.swt.colorstyledtext.jedit.Mark;
import org.dengues.commons.ui.swt.colorstyledtext.jedit.Mode;
import org.dengues.commons.ui.swt.colorstyledtext.jedit.Rule;
import org.dengues.commons.ui.swt.colorstyledtext.jedit.Span;
import org.dengues.commons.ui.swt.colorstyledtext.jedit.TextSequence;
import org.dengues.commons.ui.swt.colorstyledtext.jedit.Type;
import org.dengues.commons.ui.swt.colorstyledtext.rules.CasedPatternRule;
import org.dengues.commons.ui.swt.colorstyledtext.rules.ColoringWhitespaceDetector;
import org.dengues.commons.ui.swt.colorstyledtext.rules.ColoringWordDetector;
import org.dengues.commons.ui.swt.colorstyledtext.rules.DelegateToken;
import org.dengues.commons.ui.swt.colorstyledtext.rules.EndOfLineRule;
import org.dengues.commons.ui.swt.colorstyledtext.rules.ITokenFactory;
import org.dengues.commons.ui.swt.colorstyledtext.rules.StarRule;
import org.dengues.commons.ui.swt.colorstyledtext.rules.TextSequenceRule;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.jface.text.rules.PatternRule;
import org.eclipse.jface.util.PropertyChangeEvent;

/**
 * Qiang.Zhang.Adolf@gmail.com class global comment. Detailled comment <br/>
 * 
 * $Id: Dengues.epf 1 2006-09-29 17:06:40Z qiang.zhang $
 * 
 */
public class ColoringEditorTools {

    public static void add(Rule rule, List<IRule> rules, ITokenFactory factory) {
        List allTypes = rule.getTypes();
        for (Iterator typeI = allTypes.iterator(); typeI.hasNext(); ) {
            Type type = (Type) typeI.next();
            add(rule, type, factory, rules);
        }
    }

    public static void add(final Rule rule, final Type type, ITokenFactory factory, final List<IRule> rules) {
        final IToken token = factory.makeToken(type);
        final Mode mode = rule.getMode();
        final boolean ignoreCase = rule.getIgnoreCase();
        type.accept(new IVisitor() {

            public void acceptSpan(Span span) {
                IToken defaultToken = token;
                if (span.hasDelegate()) {
                    Rule delegateRule = mode.getRule(span.getDelegate());
                    defaultToken = new DelegateToken(type, delegateRule, span.getEnd());
                }
                PatternRule pat = new CasedPatternRule(span.getStart(), span.getEnd(), defaultToken, mode.getDefaultRuleSet().getEscape(), span.noLineBreak(), ignoreCase);
                rules.add(pat);
            }

            public void acceptTextSequence(TextSequence text) {
                if (isWordStart(text.getText().charAt(0))) {
                    return;
                }
                rules.add(new TextSequenceRule(text.getText(), token, ignoreCase));
            }

            public void acceptEolSpan(EOLSpan eolSpan) {
                rules.add(new EndOfLineRule(eolSpan.getText(), token, ignoreCase));
            }

            public void acceptMark(Mark mark) {
                rules.add(new StarRule(mark, new ColoringWhitespaceDetector(), wordDetector, token));
            }
        });
    }

    protected static ColoringWordDetector wordDetector = new ColoringWordDetector();

    public ColoringEditorTools() {
    }

    protected static boolean isWordStart(char c) {
        return wordDetector.isWordStart(c);
    }

    public boolean affectsTextPresentation(PropertyChangeEvent event) {
        String property = event.getProperty();
        if (property == null) {
            return false;
        }
        if (property.endsWith("Bold")) {
            property = property.substring(0, property.length() - ColorManager.BOLD_SUFFIX.length());
        }
        boolean affects = property.equals(ColorManager.COMMENT1_COLOR) || property.equals(ColorManager.COMMENT2_COLOR) || property.equals(ColorManager.LITERAL1_COLOR) || property.equals(ColorManager.LITERAL2_COLOR) || property.equals(ColorManager.LABEL_COLOR) || property.equals(ColorManager.KEYWORD1_COLOR) || property.equals(ColorManager.KEYWORD2_COLOR) || property.equals(ColorManager.KEYWORD3_COLOR) || property.equals(ColorManager.FUNCTION_COLOR) || property.equals(ColorManager.MARKUP_COLOR) || property.equals(ColorManager.OPERATOR_COLOR) || property.equals(ColorManager.DIGIT_COLOR) || property.equals(ColorManager.INVALID_COLOR) || property.equals(ColorManager.NULL_COLOR);
        return affects;
    }

    /**
     * Answer the file associated with name. This handles the case of running as a plugin and running standalone which
     * happens during testing.
     * 
     * @param filename
     * @return File
     */
    public static InputStream getFile(String filename) throws IOException {
        URL url;
        url = Platform.getBundle(DenguesCommonsPlugin.PLUGIN_ID).getEntry(filename);
        return url != null ? url.openStream() : null;
    }
}
