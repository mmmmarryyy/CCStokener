package org.fudaa.dodico.crue.io.dao;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.XMLConstants;
import javax.xml.transform.sax.SAXSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.commons.lang.StringUtils;
import org.fudaa.ctulu.CtuluLibFile;
import org.fudaa.ctulu.CtuluLibString;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.dodico.crue.common.BusinessMessages;
import org.fudaa.dodico.crue.common.XmlVersionFinder;
import org.fudaa.dodico.crue.config.CrueConfigMetier;
import org.fudaa.dodico.crue.io.UnicodeInputStream;
import org.fudaa.dodico.crue.io.common.CrueData;
import org.fudaa.dodico.crue.io.common.CrueFileType;
import org.fudaa.dodico.crue.io.common.CrueIOResu;
import org.fudaa.dodico.crue.projet.coeur.CoeurConfigContrat;
import org.xml.sax.InputSource;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XmlFriendlyReplacer;
import com.thoughtworks.xstream.mapper.CannotResolveClassException;

/**
 * File format customisé pour Crue. Indique si le fichier est de type crue 9 ou 10. Contient un validator xsd pour le
 * fichier donné. Contient une méthode statique très pratique qui permet de retourner automatiquement le bon FileFormat
 * en fonction du type renvoyé. T correspond à la structure métier associée au format du fichier
 * 
 * @param <D> Represente la structure DAO
 * @param <M> Represente le modele Metier
 * @author Adrien Hadoux
 */
public class CrueXmlReaderWriterImpl<D extends AbstractCrueDao, M> implements CrueXmlReaderWriter<M> {

    protected final CrueConverter<D, M> converter;

    private final CrueDaoStructure daoConfigurer;

    /**
   * La version du fichier
   */
    private final String version;

    /**
   * le nom du fichier xsd a utiliser
   */
    private final String xsdId;

    private final CrueFileType fileType;

    /**
   * @return the fileType
   */
    public CrueFileType getFileType() {
        return fileType;
    }

    /**
   * La path complet du fichier xsd
   */
    private final String xsdPath;

    private final String xsdFile;

    /**
   * @param type le type en Upper case comme par exemple DFRT.
   */
    public CrueXmlReaderWriterImpl(final CrueFileType fileType, final CoeurConfigContrat coeurConfig, final CrueConverter<D, M> converter, final CrueDaoStructure daoConfigurer) {
        this.daoConfigurer = daoConfigurer;
        this.converter = converter;
        this.version = coeurConfig.getXsdVersion();
        this.fileType = fileType;
        this.xsdId = fileType.toString();
        this.xsdFile = xsdId.toLowerCase() + "-" + version + ".xsd";
        xsdPath = coeurConfig.getXsdUrl(xsdFile);
    }

    /**
   * Utilise par des lecteurs/ecrivain qui ne sont pas des fichiers xml de Crue10.
   * 
   * @param fileType
   * @param version
   * @param converter
   * @param daoConfigurer
   */
    protected CrueXmlReaderWriterImpl(final String fileType, final String version, final CrueConverter<D, M> converter, final CrueDaoStructure daoConfigurer) {
        this.daoConfigurer = daoConfigurer;
        this.converter = converter;
        this.version = version;
        this.fileType = null;
        this.xsdId = fileType;
        this.xsdFile = xsdId.toLowerCase() + "-" + version + ".xsd";
        xsdPath = "/xsd/" + xsdFile;
    }

    /**
   * @return the xsdId
   */
    public String getXsdId() {
        return xsdId;
    }

    protected final void configureXStream(final XStream xstream, final CtuluLog analyse, final CrueConfigMetier props) {
        daoConfigurer.configureXStream(xstream, analyse, props);
    }

    /**
   * @return the version
   */
    public String getVersion() {
        return version;
    }

    /**
   * @return le path dans jar vers le fichier xsd correspondant
   */
    public final String getXsdValidator() {
        return xsdPath;
    }

    protected XStream initXmlParser(final CtuluLog analyse, final CrueConfigMetier props) {
        final XmlFriendlyReplacer replacer = createReplacer();
        final DomDriver domDriver = new DomDriver(XmlVersionFinder.ENCODING, replacer);
        final XStream xstream = new XStream(domDriver);
        xstream.setMode(XStream.NO_REFERENCES);
        xstream.aliasAttribute("xmlns:xsi", "xmlnsxsi");
        xstream.aliasAttribute("xsi:schemaLocation", "xsischemaLocation");
        xstream.useAttributeFor(AbstractCrueDao.class, "xmlns");
        xstream.useAttributeFor(AbstractCrueDao.class, "xmlnsxsi");
        xstream.useAttributeFor(AbstractCrueDao.class, "xsischemaLocation");
        configureXStream(xstream, analyse, props);
        return xstream;
    }

    private XmlFriendlyReplacer createReplacer() {
        return new XmlFriendlyReplacer("#", "_");
    }

    public boolean isValide(final File xml, final CtuluLog res) {
        try {
            final boolean valide = isValide(xml.toURI().toURL(), res);
            res.setDesc(BusinessMessages.getString("valid.xml", xml.getName()));
            return valide;
        } catch (final MalformedURLException e) {
            res.manageException(e);
            LOGGER.log(Level.SEVERE, "isValide", e);
            return false;
        }
    }

    public boolean isValide(final String xml, final CtuluLog res) {
        return isValide(getClass().getResource(xml), res);
    }

    public boolean isValide(final URL xml, final CtuluLog res) {
        return isValide(xml, xsdPath, res);
    }

    public static boolean isValide(final URL xml, final String xsd, final CtuluLog res) {
        res.setDefaultResourceBundle(BusinessMessages.RESOURCE_BUNDLE);
        res.setDesc(BusinessMessages.getString("valid.xml", xml));
        if (xml == null) {
            res.addFatalError("io.fileNotFound");
            return false;
        }
        final ErrorHandlerDefault handler = new ErrorHandlerDefault(res);
        InputStream xmlStream = null;
        try {
            final SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            URL xsdURL = null;
            File xsdFile = new File(xsd);
            if (xsdFile.exists()) {
                xsdURL = xsdFile.toURI().toURL();
            } else {
                xsdURL = CrueXmlReaderWriterImpl.class.getResource(xsd);
            }
            if (xsdURL == null) {
                res.addFatalError("xsdNotFound.error", xsd);
                return false;
            }
            final Schema schema = schemaFactory.newSchema(xsdURL);
            final Validator validator = schema.newValidator();
            validator.setErrorHandler(handler);
            xmlStream = xml.openStream();
            UnicodeInputStream unicodeStream = new UnicodeInputStream(xmlStream, XmlVersionFinder.ENCODING);
            unicodeStream.init();
            validator.validate(new SAXSource(new InputSource(unicodeStream)));
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "isValide", e);
            return false;
        } finally {
            CtuluLibFile.close(xmlStream);
        }
        return !handler.isHasError();
    }

    /**
   * Lit les données dans le fichier f avec les données liées.
   * 
   * @param dataLinked
   * @return
   */
    public final CrueIOResu<M> read(final URL f, final CtuluLog analyzer, final CrueData dataLinked) {
        analyzer.setDefaultResourceBundle(BusinessMessages.RESOURCE_BUNDLE);
        analyzer.setDesc(BusinessMessages.getString("read.file", f));
        final D d = readDao(f, analyzer, dataLinked);
        if (d != null) {
            return createResu(d, converter.convertDaoToMetier(d, dataLinked, analyzer), analyzer);
        }
        return null;
    }

    protected CrueIOResu<M> createResu(final D d, final M m, final CtuluLog analyze) {
        final CrueIOResu<M> res = new CrueIOResu<M>();
        res.setMetier(m);
        if (d != null) {
            res.setCrueCommentaire(d.getCommentaire());
        }
        res.setAnalyse(analyze);
        return res;
    }

    private static final Logger LOGGER = Logger.getLogger(CrueXmlReaderWriterImpl.class.getName());

    /**
   * @param fichier
   * @return
   */
    public D readDao(final File fichier, final CtuluLog analyser, final CrueData dataLinked) {
        FileInputStream in = null;
        D newData = null;
        try {
            in = new FileInputStream(fichier);
            newData = readDao(in, analyser, dataLinked);
        } catch (final FileNotFoundException e) {
            LOGGER.log(Level.FINE, "readDao", e);
            final String path = fichier == null ? "null" : fichier.getAbsolutePath();
            analyser.addFatalError("io.FileNotFoundException.error", path);
        } finally {
            CtuluLibFile.close(in);
        }
        return newData;
    }

    /**
   * @param in
   * @return le dao
   */
    @SuppressWarnings("unchecked")
    protected D readDao(final InputStream in, final CtuluLog analyser, final CrueData dataLinked) {
        analyser.setDefaultResourceBundle(BusinessMessages.RESOURCE_BUNDLE);
        D newData = null;
        BufferedReader contentRead = null;
        try {
            final XStream parser = initXmlParser(analyser, dataLinked == null ? null : dataLinked.getCrueConfigMetier());
            UnicodeInputStream unicodeStream = new UnicodeInputStream(in, XmlVersionFinder.ENCODING);
            unicodeStream.init();
            contentRead = new BufferedReader(new InputStreamReader(unicodeStream, XmlVersionFinder.ENCODING));
            newData = (D) parser.fromXML(contentRead);
        } catch (ConversionException conversionException) {
            LOGGER.log(Level.FINE, "io.unknown.bsalise", conversionException);
            analyser.addFatalError("io.unknown.bsalise", StringUtils.substringBefore(conversionException.getShortMessage(), " "));
        } catch (CannotResolveClassException cannotResolveException) {
            LOGGER.log(Level.FINE, "io.unknown.bsalise", cannotResolveException);
            analyser.addFatalError("io.unknown.bsalise", cannotResolveException.getMessage());
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "io.xml.error", e);
            analyser.addFatalError("io.xml.error", e.getMessage());
        } finally {
            CtuluLibFile.close(contentRead);
        }
        return newData;
    }

    /**
   * @param pathToResource l'adresse du fichier a charger commencant par /
   * @param analyser
   * @param dataLinked
   * @return
   */
    protected D readDao(final String pathToResource, final CtuluLog analyser, final CrueData dataLinked) {
        return readDao(getClass().getResource(pathToResource), analyser, dataLinked);
    }

    /**
   * @param fichier
   * @return
   */
    public D readDao(final URL url, final CtuluLog analyser, final CrueData dataLinked) {
        if (url == null) {
            analyser.addFatalError("file.url.null.error");
            return null;
        }
        InputStream in = null;
        D newData = null;
        try {
            in = url.openStream();
            newData = readDao(in, analyser, dataLinked);
        } catch (final IOException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
            analyser.addFatalError("io.xml.error", e.getMessage());
        } finally {
            CtuluLibFile.close(in);
        }
        return newData;
    }

    /**
   * Lit les données dans le fichier f avec les données liées.
   * 
   * @param dataLinked
   * @param  validation si vrai valide les données.
   * @return
   */
    public final CrueIOResu<M> readXML(final File f, final CtuluLog analyzer) {
        return readXML(f, analyzer, null);
    }

    /**
   * Lit les données dans le fichier f avec les données liées.
   * 
   * @param dataLinked
   * @param  validation si vrai valide les données.
   * @return
   */
    public final CrueIOResu<M> readXML(final File f, final CtuluLog analyzer, final CrueData dataLinked) {
        analyzer.setDefaultResourceBundle(BusinessMessages.RESOURCE_BUNDLE);
        analyzer.setDesc(BusinessMessages.getString("read.file", f.getName()));
        final D d = readDao(f, analyzer, dataLinked);
        if (d != null) {
            return createResu(d, converter.convertDaoToMetier(d, dataLinked, analyzer), analyzer);
        }
        return createResu(null, null, analyzer);
    }

    /**
   * Lit les données dans le fichier f avec les données liées.
   * 
   * @param dataLinked
   * @return
   */
    public final CrueIOResu<M> readXML(final String pathToResource, final CtuluLog analyzer, final CrueData dataLinked) {
        analyzer.setDefaultResourceBundle(BusinessMessages.RESOURCE_BUNDLE);
        analyzer.setDesc(BusinessMessages.getString("read.file", pathToResource));
        final D d = readDao(pathToResource, analyzer, dataLinked);
        if (d != null) {
            return createResu(d, converter.convertDaoToMetier(d, dataLinked, analyzer), analyzer);
        }
        final CrueIOResu<M> res = new CrueIOResu<M>();
        res.setAnalyse(analyzer);
        return res;
    }

    protected boolean writeDAO(final File file, final D dao, final CtuluLog analyser, final CrueConfigMetier props) {
        FileOutputStream out = null;
        boolean ok = true;
        try {
            out = new FileOutputStream(file);
            out.write(getBOM(XmlVersionFinder.ENCODING));
            out.flush();
            ok = writeDAO(out, dao, analyser, props);
        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "writeDAO " + file.getName(), e);
            ok = false;
        } finally {
            CtuluLibFile.close(out);
        }
        return ok;
    }

    public static byte[] getBOM(String enc) {
        if (XmlVersionFinder.ENCODING.equals(enc)) {
            byte[] bom = new byte[3];
            bom[0] = (byte) 0xEF;
            bom[1] = (byte) 0xBB;
            bom[2] = (byte) 0xBF;
            return bom;
        } else if ("UTF-16BE".equals(enc)) {
            byte[] bom = new byte[2];
            bom[0] = (byte) 0xFE;
            bom[1] = (byte) 0xFF;
            return bom;
        } else if ("UTF-16LE".equals(enc)) {
            byte[] bom = new byte[2];
            bom[0] = (byte) 0xFF;
            bom[1] = (byte) 0xFE;
            return bom;
        } else if ("UTF-32BE".equals(enc)) {
            byte[] bom = new byte[4];
            bom[0] = (byte) 0x00;
            bom[1] = (byte) 0x00;
            bom[2] = (byte) 0xFE;
            bom[3] = (byte) 0xFF;
            return bom;
        } else if ("UTF-32LE".equals(enc)) {
            byte[] bom = new byte[4];
            bom[0] = (byte) 0x00;
            bom[1] = (byte) 0x00;
            bom[2] = (byte) 0xFF;
            bom[3] = (byte) 0xFE;
            return bom;
        } else {
            return null;
        }
    }

    /**
   * @param out le flux de sortie
   * @param dao le dao a persister
   * @param analyser le receveur d'information
   * @return
   */
    @SuppressWarnings("deprecation")
    protected boolean writeDAO(final OutputStream out, final D dao, final CtuluLog analyser, final CrueConfigMetier props) {
        boolean isOk = true;
        try {
            final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, XmlVersionFinder.ENCODING));
            writer.write(CrueHelper.ENTETE_XML + CtuluLibString.LINE_SEP);
            if (dao.getCommentaire() == null) {
                dao.setCommentaire(StringUtils.EMPTY);
            }
            final XStream parser = initXmlParser(analyser, props);
            parser.marshal(dao, new PrettyPrintWriter(writer, new char[] { ' ', ' ' }, CtuluLibString.LINE_SEP, createReplacer()));
        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "writeDAO", e);
            analyser.addFatalError("file.write.error");
            isOk = false;
        } finally {
            CtuluLibFile.close(out);
        }
        return isOk;
    }

    /**
   * MEthode qui permet d'ecrire les datas dans le fichier f specifie.
   * 
   * @param data
   * @param f
   * @return
   */
    public final boolean writeXML(final CrueIOResu<CrueData> metier, final File f, final CtuluLog analyzer) {
        analyzer.setDefaultResourceBundle(BusinessMessages.RESOURCE_BUNDLE);
        f.getParentFile().mkdirs();
        analyzer.setDesc(BusinessMessages.getString("write.file", f.getName()));
        final D d = converter.convertMetierToDao(converter.getConverterData(metier.getMetier()), analyzer);
        if (d != null) {
            d.setXsdName(xsdFile);
            d.setCommentaire(metier.getCrueCommentaire());
            return writeDAO(f, d, analyzer, metier.getMetier().getCrueConfigMetier());
        }
        return false;
    }

    /**
   * @param metier l'objet metier
   * @param out le flux de sortie qui ne sera pas ferme
   * @param analyser
   * @return true si reussite
   */
    public boolean writeXML(final CrueIOResu<CrueData> metier, final OutputStream out, final CtuluLog analyser) {
        analyser.setDefaultResourceBundle(BusinessMessages.RESOURCE_BUNDLE);
        analyser.setDesc(BusinessMessages.getString("write.file", xsdId));
        final D d = converter.convertMetierToDao(converter.getConverterData(metier.getMetier()), analyser);
        if (d != null) {
            d.setCommentaire(metier.getCrueCommentaire());
            return writeDAO(out, d, analyser, metier.getMetier().getCrueConfigMetier());
        }
        return false;
    }

    /**
   * Methode qui permet d'ecrire les datas dans le fichier f specifie.
   * 
   * @param data
   * @param f
   * @return
   */
    public final boolean writeXMLMetier(final CrueIOResu<M> metier, final File f, final CtuluLog analyzer, final CrueConfigMetier props) {
        f.getParentFile().mkdirs();
        analyzer.setDesc(BusinessMessages.getString("write.file", f.getName()));
        analyzer.setDefaultResourceBundle(BusinessMessages.RESOURCE_BUNDLE);
        final D d = converter.convertMetierToDao(metier.getMetier(), analyzer);
        if (d != null) {
            if (StringUtils.isEmpty(d.getCommentaire())) {
                d.setCommentaire(metier.getCrueCommentaire());
            }
            d.setXsdName(xsdFile);
            return writeDAO(f, d, analyzer, props);
        }
        return false;
    }

    /**
   * @param metier l'objet metier
   * @param out le flux de sortie qui ne sera pas ferme
   * @param analyser
   * @return true si reussite
   */
    public boolean writeXMLMetier(final CrueIOResu<M> metier, final OutputStream out, final CtuluLog analyser, final CrueConfigMetier props) {
        analyser.setDesc(BusinessMessages.getString("write.file", xsdId));
        analyser.setDefaultResourceBundle(BusinessMessages.RESOURCE_BUNDLE);
        final D d = converter.convertMetierToDao(metier.getMetier(), analyser);
        if (d != null) {
            d.setCommentaire(metier.getCrueCommentaire());
            return writeDAO(out, d, analyser, props);
        }
        return false;
    }
}
