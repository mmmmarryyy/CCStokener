package org.fudaa.dodico.crue.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang.StringUtils;
import org.fudaa.ctulu.CtuluLibFile;
import org.fudaa.ctulu.CtuluLibString;
import org.fudaa.ctulu.CtuluLog;
import org.fudaa.dodico.crue.common.BusinessMessages;
import org.fudaa.dodico.crue.common.DateDurationConverter;
import org.fudaa.dodico.crue.common.XmlVersionFinder;
import org.fudaa.dodico.crue.config.CrueConfigMetierReaderXML.DaoConfigLoi;
import org.fudaa.dodico.crue.config.CrueConfigMetierReaderXML.DaoControleLoi;
import org.fudaa.dodico.crue.config.CrueConfigMetierReaderXML.DaoExtrapolationLoi;
import org.fudaa.dodico.crue.config.CrueConfigMetierReaderXML.DaoNature;
import org.fudaa.dodico.crue.config.CrueConfigMetierReaderXML.DaoNumerique;
import org.fudaa.dodico.crue.config.CrueConfigMetierReaderXML.DaoTypeEnum;
import org.fudaa.dodico.crue.config.CrueConfigMetierReaderXML.DaoValeurStrictable;
import org.fudaa.dodico.crue.config.CrueConfigMetierReaderXML.DaoVariable;
import org.fudaa.dodico.crue.io.common.CrueIOResu;
import org.fudaa.dodico.crue.metier.emh.EnumTypeLoi;
import org.fudaa.dodico.crue.projet.coeur.CoeurConfigContrat;
import org.joda.time.LocalDateTime;

/**
 * @author deniger
 */
public class CrueConfigMetierReader {

    private static final Logger LOGGER = Logger.getLogger(CrueConfigMetierReader.class.getName());

    private final CoeurConfigContrat coeurConfig;

    public CrueConfigMetierReader(CoeurConfigContrat coeurConfig) {
        super();
        this.coeurConfig = coeurConfig;
    }

    public CrueIOResu<CrueConfigMetier> readConfigMetier(final File fichier) {
        FileInputStream in = null;
        final CtuluLog analyser = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        CrueIOResu<CrueConfigMetier> newData = new CrueIOResu<CrueConfigMetier>();
        newData.setAnalyse(analyser);
        analyser.setDesc(BusinessMessages.getString("configMetier.readFile", fichier.getAbsolutePath()));
        CrueConfigMetierReaderXML readerXML = new CrueConfigMetierReaderXML(coeurConfig);
        if (readerXML.isValide(fichier, analyser)) {
            CrueConfigMetier res = null;
            try {
                in = new FileInputStream(fichier);
                res = readConfigMetier(in, analyser);
            } catch (final FileNotFoundException e) {
                LOGGER.log(Level.FINE, "readConfigMetier", e);
                final String path = fichier == null ? "null" : fichier.getAbsolutePath();
                analyser.addError("io.FileNotFoundException.error", path);
            } finally {
                CtuluLibFile.close(in);
            }
            newData.setMetier(res);
            if (!containProfil(newData.getMetier())) {
                analyser.addError("configMetier.loiProfilRequise");
            }
        }
        return newData;
    }

    private boolean containProfil(final CrueConfigMetier l) {
        if (l == null) {
            return false;
        }
        for (final ConfigLoi configLoi : l.getConfLoi().values()) {
            if (EnumTypeLoi.LoiPtProfil.equals(configLoi.getTypeLoi())) {
                return true;
            }
        }
        return false;
    }

    /**
   * @param in
   * @return le dao
   */
    protected CrueConfigMetier readConfigMetier(final InputStream in, final CtuluLog analyser) {
        CrueConfigMetierReaderXML readerXML = new CrueConfigMetierReaderXML(coeurConfig);
        CrueConfigMetierReaderXML.DaoConfigMetier res = readerXML.readDaos(in, analyser);
        if (analyser.containsFatalError() || res == null) {
            return null;
        }
        Map<String, PropertyTypeNumerique> typesNumeriques = new HashMap<String, PropertyTypeNumerique>();
        for (DaoNumerique num : res.TypeNumeriques.Numeriques) {
            PropertyTypeNumerique propertyNum = new PropertyTypeNumerique(StringUtils.uncapitalize(num.Nom), num.Infini, Double.parseDouble(num.Infini));
            typesNumeriques.put(propertyNum.getNom(), propertyNum);
        }
        final Map<String, PropertyNature> propNature = new HashMap<String, PropertyNature>();
        for (DaoNature nature : res.Natures.Natures) {
            PropertyTypeNumerique propertyNum = typesNumeriques.get(StringUtils.uncapitalize(nature.TypeNumerique.NomRef));
            if (propertyNum != null) {
                PropertyNature propertyNat = new PropertyNature(StringUtils.uncapitalize(nature.Nom), new PropertyEpsilon(Double.parseDouble(nature.EpsilonComparaison.Valeur), Double.parseDouble(nature.EpsilonPresentation.Valeur)), nature.Unite, propertyNum);
                propNature.put(propertyNat.getNom(), propertyNat);
            }
        }
        final Map<String, PropertyNature> propEnum = new HashMap<String, PropertyNature>();
        for (DaoTypeEnum nature : res.TypeEnums.Enums) {
            List<String> enums = new ArrayList<String>();
            if (nature.ItemEnum != null) {
                for (String object : nature.ItemEnum) {
                    enums.add(object);
                }
            }
            PropertyNature propertyNat = new PropertyNature(nature.Nom, enums);
            propEnum.put(propertyNat.getNom(), propertyNat);
        }
        CrueConfigMetier propDefinition = new CrueConfigMetier(propNature, propEnum);
        final Map<String, PropertyDefinition> mapPropDefinitions = new HashMap<String, PropertyDefinition>();
        for (DaoVariable variable : res.Variables.Variables) {
            if (variable.Nature != null) {
                PropertyNature propertyNat = propNature.get(StringUtils.uncapitalize(variable.Nature.NomRef));
                PropertyValidator propertyVal = this.getValidator(variable, propertyNat.getTypeNumerique(), analyser);
                if (propertyVal == null) {
                    return null;
                }
                PropertyDefinition propertyDef = null;
                if (propertyNat.isDate()) {
                    propertyDef = new PropertyDefinition(StringUtils.uncapitalize(variable.Nom), variable.ValeurDefaut, propertyNat, propertyVal);
                } else {
                    propertyDef = new PropertyDefinition(StringUtils.uncapitalize(variable.Nom), this.getDouble(variable.ValeurDefaut, propertyNat.getTypeNumerique()), propertyNat, propertyVal);
                }
                mapPropDefinitions.put(propertyDef.getNom(), propertyDef);
            } else if (variable.TypeEnum != null) {
                PropertyNature propertyNat = propEnum.get(variable.TypeEnum.NomRef);
                PropertyValidator validator = new PropertyValidator(null, null);
                PropertyDefinition enumDef = new PropertyDefinition(StringUtils.uncapitalize(variable.Nom), StringUtils.EMPTY, propertyNat, validator);
                validator.setParamToValid(enumDef);
                mapPropDefinitions.put(enumDef.getNom(), enumDef);
            }
        }
        propDefinition.setPropDefinition(mapPropDefinitions);
        loadLois(analyser, res, propDefinition);
        return propDefinition;
    }

    private PropertyValidator getValidator(DaoVariable variable, PropertyTypeNumerique type, CtuluLog log) {
        NumberRangeValidator rangeValidate = this.getRange(variable, variable.MinValidite, variable.MaxValidite, type, log);
        if (rangeValidate == null) {
            log.addFatalError("configMetier.validityRange.error", variable.Nom);
        }
        NumberRangeValidator rangeNormalite = this.getRange(variable, variable.MinNormalite, variable.MaxNormalite, type, log);
        if (rangeNormalite == null) {
            log.addFatalError("configMetier.nomaliteRange.error", variable.Nom);
        }
        if (rangeNormalite != null && rangeValidate != null) {
            PropertyValidator validator = new PropertyValidator(rangeValidate, rangeNormalite);
            if (!isCorrectValidator(validator)) {
                log.addFatalError("configMetier.validator.error", variable.Nom);
            }
            return validator;
        }
        return null;
    }

    private NumberRangeValidator getRange(DaoVariable variable, DaoValeurStrictable min, DaoValeurStrictable max, PropertyTypeNumerique type, CtuluLog log) {
        NumberRangeValidator range = new NumberRangeValidator();
        LocalDateTime minDate = getDate(min.Valeur);
        if (minDate != null) {
            LocalDateTime maxDate = getDate(max.Valeur);
            if (maxDate == null) {
                log.addFatalError("configMetier.dateValue.badFormat", variable.Nom, min.Valeur, max.Valeur);
                return null;
            }
            range.setMax(new Double(maxDate.toDateTime().getMillis()));
            range.setMaxName(max.Valeur);
            range.setMin(new Double(minDate.toDateTime().getMillis()));
            range.setMinName(min.Valeur);
            return range;
        }
        try {
            range.setMin(this.getDouble(min.Valeur, type));
        } catch (NumberFormatException e) {
            log.addFatalError("configMetier.minValue.badFormat", variable.Nom, min.Valeur);
            return null;
        }
        try {
            range.setMax(this.getDouble(max.Valeur, type));
        } catch (NumberFormatException e) {
            log.addFatalError("configMetier.maxValue.badFormat", variable.Nom, max.Valeur);
            return null;
        }
        return this.isCorrectRange(range) ? range : null;
    }

    private LocalDateTime getDate(String value) {
        if (value == null || value.indexOf('T') < 0) {
            return null;
        }
        return DateDurationConverter.getDate(value);
    }

    private Double getDouble(String value, PropertyTypeNumerique type) {
        Double dble = null;
        if (!CtuluLibString.isEmpty(value)) {
            if (value.equals("+Infini")) {
                if (type == null) {
                    dble = Double.POSITIVE_INFINITY;
                } else {
                    dble = type.getInfini();
                }
            } else if (value.equals("-Infini")) {
                if (type == null) {
                    dble = Double.NEGATIVE_INFINITY;
                } else {
                    dble = -type.getInfini();
                }
            } else {
                dble = Double.valueOf(value);
            }
        }
        return dble;
    }

    private boolean isCorrectValidator(PropertyValidator property) {
        return (property.getRangeValidate() != null) && (property.getRangeNormalite() != null) && (property.getRangeNormalite().getMin().doubleValue() >= property.getRangeValidate().getMin().doubleValue()) && (property.getRangeNormalite().getMax().doubleValue() <= property.getRangeValidate().getMax().doubleValue());
    }

    private boolean isCorrectRange(NumberRangeValidator range) {
        if (range.isMinStrict() && range.isMaxStrict()) {
            return range.getMin().doubleValue() < range.getMax().doubleValue();
        } else {
            return range.getMin().doubleValue() <= range.getMax().doubleValue();
        }
    }

    private void loadLois(final CtuluLog analyser, CrueConfigMetierReaderXML.DaoConfigMetier res, CrueConfigMetier propDefinition) {
        HashMap<String, String> newIdByOldValue = new HashMap<String, String>();
        newIdByOldValue.put("LoiTZ", "LoiTZimp");
        newIdByOldValue.put("LoiQZ", "LoiQZimp");
        Map<String, ItemTypeExtrapolationLoi> itemTypeExtrapolation = new HashMap<String, ItemTypeExtrapolationLoi>();
        for (DaoExtrapolationLoi extraLoi : res.TypeExtrapolLois.Extrapolations) {
            ItemTypeExtrapolationLoi typeExtra = new ItemTypeExtrapolationLoi(StringUtils.uncapitalize(extraLoi.Nom));
            itemTypeExtrapolation.put(typeExtra.getNom(), typeExtra);
        }
        Map<String, ItemTypeControleLoi> itemTypeControle = new HashMap<String, ItemTypeControleLoi>();
        for (DaoControleLoi ctrlLoi : res.TypeControleLois.Controles) {
            ItemTypeControleLoi typeCtrl = new ItemTypeControleLoi(StringUtils.uncapitalize(ctrlLoi.Nom));
            itemTypeControle.put(typeCtrl.getNom(), typeCtrl);
        }
        final Set<EnumTypeLoi> typeLoiDone = new HashSet<EnumTypeLoi>();
        if (res != null && CollectionUtils.isNotEmpty(res.ConfigLois.Lois)) {
            final List<ConfigLoi> loisLoaded = new ArrayList<ConfigLoi>(res.ConfigLois.Lois.size());
            for (final DaoConfigLoi confLoi : res.ConfigLois.Lois) {
                ItemTypeExtrapolationLoi extrapolInf = null;
                ItemTypeExtrapolationLoi extrapolSup = null;
                EnumTypeLoi typeLoi = null;
                boolean error = false;
                try {
                    String typeLoiName = confLoi.Nom;
                    if (newIdByOldValue.containsKey(typeLoiName)) {
                        typeLoiName = newIdByOldValue.get(typeLoiName);
                    }
                    typeLoi = EnumTypeLoi.getTypeLoi(typeLoiName);
                    if (typeLoiDone.contains(typeLoi)) {
                        error = true;
                        analyser.addFatalError("configLoi.TypeLoi.doublon.error", confLoi.Nom);
                    } else {
                        typeLoiDone.add(typeLoi);
                    }
                } catch (final Exception e) {
                    analyser.addError(e.getMessage(), e);
                    analyser.addFatalError("configLoi.TypeLoi.error", confLoi.Nom);
                    error = true;
                }
                extrapolInf = itemTypeExtrapolation.get(StringUtils.uncapitalize(confLoi.ExtrapolInf.NomRef));
                if (extrapolInf == null) {
                    analyser.addFatalError("configLoi.extrapolInf.error", confLoi.Nom, confLoi.ExtrapolInf.NomRef);
                    error = true;
                }
                extrapolSup = itemTypeExtrapolation.get(StringUtils.uncapitalize(confLoi.ExtrapolSup.NomRef));
                if (extrapolSup == null) {
                    analyser.addFatalError("configLoi.extrapolSup.error", confLoi.Nom, confLoi.ExtrapolSup.NomRef);
                    error = true;
                }
                ItemTypeControleLoi itemTypeControleLoi = itemTypeControle.get(StringUtils.uncapitalize(confLoi.TypeControleLoi.NomRef));
                if (itemTypeControleLoi == null) {
                    analyser.addFatalError("configLoi.controle.error", confLoi.Nom, confLoi.TypeControleLoi.NomRef);
                    error = true;
                }
                final PropertyDefinition varAbs = propDefinition.getProperty(StringUtils.uncapitalize(confLoi.VarAbscisse.NomRef));
                if (varAbs == null) {
                    analyser.addFatalError("configLoi.varAbs.error", confLoi.Nom, confLoi.VarAbscisse);
                    error = true;
                }
                final PropertyDefinition varOrd = propDefinition.getProperty(StringUtils.uncapitalize(confLoi.VarOrdonnee.NomRef));
                if (varOrd == null) {
                    analyser.addFatalError("configLoi.varOrd.error", confLoi.Nom, confLoi.VarOrdonnee);
                    error = true;
                }
                if (!error) {
                    loisLoaded.add(new ConfigLoi(typeLoi, itemTypeControleLoi, extrapolInf, extrapolSup, varAbs, varOrd, confLoi.Commentaire));
                }
            }
            Collection<EnumTypeLoi> loiTypes = EnumTypeLoi.getListTypeLoi();
            Collection<?> removeAll = ListUtils.removeAll(loiTypes, typeLoiDone);
            if (removeAll.size() > 0) {
                analyser.addFatalError("configLoi.LoiNotDefinedFound.error", StringUtils.join(removeAll, "; "));
            }
            propDefinition.setConfigLoi(CrueConfigMetierReader.createMap(loisLoaded));
        } else {
            analyser.addFatalError("configLoi.noLoiFound.error");
        }
        SeveriteManager validVerbosite = SeveriteManager.validVerbosite(propDefinition, analyser);
        propDefinition.setVerbositeManager(validVerbosite);
    }

    /**
   * @param fichier
   * @return
   */
    public CrueIOResu<CrueConfigMetier> readConfigMetier(final URL url) {
        CrueIOResu<CrueConfigMetier> newData = new CrueIOResu<CrueConfigMetier>();
        final CtuluLog analyser = new CtuluLog(BusinessMessages.RESOURCE_BUNDLE);
        newData.setAnalyse(analyser);
        if (url == null) {
            analyser.addError("file.url.null.error");
            return newData;
        }
        String readVersion = new XmlVersionFinder().getVersion(url);
        if (!readVersion.equals(coeurConfig.getXsdVersion())) {
            analyser.addFatalError("configFile.versionNotCompatible", url.getFile(), readVersion, coeurConfig.getXsdVersion());
            return newData;
        }
        InputStream in = null;
        CrueConfigMetier res = null;
        try {
            in = url.openStream();
            res = readConfigMetier(in, analyser);
        } catch (final IOException e) {
            LOGGER.log(Level.FINE, e.getMessage(), e);
            analyser.addError("io.xml.error", e.getMessage());
        } finally {
            CtuluLibFile.close(in);
        }
        newData.setMetier(res);
        return newData;
    }

    /**
   * @param pathToResource l'adresse du fichier a charger commencant par /
   * @param analyser
   * @param dataLinked
   * @return
   */
    protected CrueIOResu<CrueConfigMetier> readConfigMetier(final String pathToResource) {
        return readConfigMetier(getClass().getResource(pathToResource));
    }

    public static Map<EnumTypeLoi, ConfigLoi> createMap(List<ConfigLoi> readConfigLoi) {
        if (readConfigLoi == null) {
            return null;
        }
        final Map<EnumTypeLoi, ConfigLoi> configs = new HashMap<EnumTypeLoi, ConfigLoi>(readConfigLoi.size());
        for (final ConfigLoi c : readConfigLoi) {
            configs.put(c.getTypeLoi(), c);
        }
        return configs;
    }
}
