package de.mpiwg.vspace.oaw.validation;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import de.mpiwg.vspace.metamodel.ExhibitionPackage;
import de.mpiwg.vspace.metamodel.ExternalLink;
import de.mpiwg.vspace.metamodel.Link;
import de.mpiwg.vspace.metamodel.Text;
import de.mpiwg.vspace.util.ExtendedPropertyHandler;
import de.mpiwg.vspace.util.PropertyHandlerRegistry;

public class LinkValidationHelper extends ValidatorExtendingValidationHelper {

    public static boolean isExternalLinkWorking(ExternalLink link) {
        if (link.getUrl() == null) return false;
        try {
            URL url = new URL(link.getUrl());
            url.openStream().close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
	 * Run checks on Link title. Get all contributed extensions and run their checks.
	 * @param link The Link to be checked.
	 * @return true if all checks succeed, false otherwise.
	 */
    public static boolean checkLinkTitle(Link link) {
        if (!validatorExtensionsRegistered()) {
            if ((link.getTitle() == null) || link.getTitle().trim().equals("")) return false;
            return true;
        }
        return runChecks(link, ExhibitionPackage.Literals.LINK__TITLE);
    }

    /**
	 * Return error messages from previously performed checks. Get error messages from contributed plugins.
	 * @param link The Link that has been checked.
	 * @return The error messages as one String.
	 */
    public static String errorMsgLinkTitle(Link link) {
        if (!validatorExtensionsRegistered() || (!link.getShowIcon() && !link.getShowTitle())) {
            ExtendedPropertyHandler handler = PropertyHandlerRegistry.REGISTRY.getPropertyHandler(Constants.PLUGIN_ID, Constants.PROPERTIES_FILE);
            return handler.getProperty("_link_title_missing");
        }
        return getCheckMessages(link, ExhibitionPackage.Literals.LINK__TITLE);
    }
}
