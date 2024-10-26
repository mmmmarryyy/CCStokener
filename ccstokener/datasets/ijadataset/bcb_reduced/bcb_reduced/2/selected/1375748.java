package tudresden.ocl20.pivot.parser.internal.ocl2parser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import tudresden.ocl20.pivot.ocl2parser.parser.OCL2Parser;
import tudresden.ocl20.pivot.ocl2parser.parser.exceptions.BuildingASTException;
import tudresden.ocl20.pivot.ocl2parser.parser.exceptions.LexException;
import tudresden.ocl20.pivot.ocl2parser.parser.exceptions.ParsingException;
import tudresden.ocl20.pivot.ocl2parser.parser.exceptions.SemanticException;
import tudresden.ocl20.pivot.parser.AbstractOclParser;
import tudresden.ocl20.pivot.parser.ParseException;

/**
 * This class extends the {@link AbstractOclParser} class
 * to implement an ocl parser.
 * @author nils
 *
 */
public class OCLParser extends AbstractOclParser {

    /**
	 * Make the parsing of the given url. If an error occurred
	 * while parsing a {@link ParseException} is thrown.
	 */
    @Override
    public void doParse(URL url) throws ParseException {
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(url.openStream());
        } catch (IOException ex) {
        }
        OCL2Parser parser = new OCL2Parser(getModel(), reader);
        try {
            parser.parse();
        } catch (IOException ex) {
        } catch (ParsingException e) {
            throw new ParseException(e.getMessage());
        } catch (LexException e) {
            throw new ParseException(e.getMessage());
        } catch (BuildingASTException e) {
            throw new ParseException(e.getMessage());
        } catch (SemanticException e) {
            throw new ParseException(e.getMessage());
        }
    }
}
