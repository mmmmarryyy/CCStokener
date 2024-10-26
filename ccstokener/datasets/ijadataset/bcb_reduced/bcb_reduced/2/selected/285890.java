package de.offis.example_applications.transformation4u;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.StringBufferInputStream;
import java.net.URL;
import java.rmi.RemoteException;
import component_interfaces.semanticmm4u.realization.compositor.provided.IVariable;
import component_interfaces.semanticmm4u.realization.generator.provided.IGenerator;
import component_interfaces.semanticmm4u.realization.generator.provided.IMultimediaPresentation;
import component_interfaces.semanticmm4u.realization.generator.realization.ITargetMediaList;
import component_interfaces.semanticmm4u.realization.media_elements_connector.realization.IMediaElementsConnectorLocator;
import de.offis.semanticmm4u.compositors.variables.operators.complex.MM4UDeserializer;
import de.offis.semanticmm4u.failures.MM4UGeneratorException;
import de.offis.semanticmm4u.failures.compositors.MM4UComplexCompositorsException;
import de.offis.semanticmm4u.generators.BinaryMedium;
import de.offis.semanticmm4u.generators.GeneratorToolkit;
import de.offis.semanticmm4u.global.Constants;
import de.offis.semanticmm4u.global.Debug;
import de.offis.semanticmm4u.media_elements_connector.MediaElementsAccessorToolkit;
import de.offis.semanticmm4u.media_elements_connector.uri.URIMediaElementsConnectorLocator;
import de.offis.semanticmm4u.user_profiles_connector.SimpleUserProfile;

public class Transformation4UService {

    private static final String DTD_PATH = Constants.getValue("derivation_url");

    private IMediaElementsConnectorLocator mediaConnectorLocator;

    public Transformation4UService() {
        mediaConnectorLocator = new URIMediaElementsConnectorLocator(Constants.getDefaultURIMediaConnectorBasePath(), "offis-mediaserver.index");
    }

    public byte[] applyTransformationOnURL(String url, int format) throws RemoteException {
        byte[] result = null;
        try {
            result = applyTransformation(new URL(url).openStream(), format);
        } catch (Exception e) {
            throwServiceException(e);
        }
        return result;
    }

    public byte[] applyTransformationOnDocument(String document, int format) throws RemoteException {
        byte[] result = null;
        try {
            result = applyTransformation(new StringBufferInputStream(document), format);
        } catch (Exception e) {
            throwServiceException(e);
        }
        return result;
    }

    public byte[] applyTransformationOnURL(String url, String userProfile) throws RemoteException {
        int format = this.getFormatFromUserProfile(userProfile);
        return applyTransformationOnURL(url, format);
    }

    public byte[] applyTransformationOnDocument(String document, String userProfile) throws RemoteException {
        int format = this.getFormatFromUserProfile(userProfile);
        return applyTransformationOnDocument(document, format);
    }

    private byte[] applyTransformation(InputStream input, int format) throws IOException, MM4UGeneratorException {
        byte[] presentation = null;
        Debug.println("applyTransformation - initDeserializer");
        MM4UDeserializer deserializer = initDeserializer(input, false);
        Debug.println("applyTransformation - generateDocument");
        IMultimediaPresentation transformedDocument = generateDocument(deserializer, format);
        Debug.println("applyTransformation - getPresentationContent");
        presentation = getPresentationContent(transformedDocument);
        Debug.println("Finished.");
        return presentation;
    }

    private MM4UDeserializer initDeserializer(InputStream myDocument, boolean validate) throws MM4UComplexCompositorsException {
        MM4UDeserializer mm4uDeserializer = new MM4UDeserializer(MediaElementsAccessorToolkit.getFactory(mediaConnectorLocator));
        mm4uDeserializer.doDeSerialize(myDocument, DTD_PATH, validate);
        return mm4uDeserializer;
    }

    private IMultimediaPresentation generateDocument(MM4UDeserializer myDeserializer, int outputFormat) throws MM4UGeneratorException {
        Debug.println("generatreDocument - getFactory");
        IGenerator generatorToolkit = GeneratorToolkit.getFactory(outputFormat);
        Debug.println("Generator:" + generatorToolkit);
        Debug.println("Generator - doDeserialize");
        IVariable rootOperator = myDeserializer.getRootOperator();
        Debug.println("Deserialized document: " + rootOperator.toString());
        Debug.println("Generator - doTransform");
        IMultimediaPresentation document = generatorToolkit.doTransform(rootOperator, Constants.COPYRIGHT_STATEMENT, new SimpleUserProfile());
        Debug.println("Generated document:" + document);
        return document;
    }

    private byte[] getPresentationContent(IMultimediaPresentation myMultimediaPresentation) throws IOException {
        ITargetMediaList tempContent = myMultimediaPresentation.getContent();
        if (myMultimediaPresentation.isBinaryPresentation()) {
            ByteArrayOutputStream content = ((BinaryMedium) tempContent.elementAt(0)).getByteContent();
            return content.toByteArray();
        } else {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            PrintStream printWriter = new PrintStream(o);
            printWriter.println(tempContent.toString());
            printWriter.close();
            return o.toByteArray();
        }
    }

    private void throwServiceException(Exception e) throws RemoteException {
        OutputStream o = new ByteArrayOutputStream();
        PrintStream w = new PrintStream(o);
        e.printStackTrace(w);
        throw new RemoteException("" + o);
    }

    private int getFormatFromUserProfile(String userProfile) {
        int format = 2;
        if (userProfile.equals("maltech")) {
            format = 10;
        }
        return format;
    }
}
