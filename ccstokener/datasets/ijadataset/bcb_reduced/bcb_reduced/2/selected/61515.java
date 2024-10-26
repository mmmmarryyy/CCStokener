package org.apache.myfaces.custom.skin;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.el.ValueExpression;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.xml.parsers.SAXParserFactory;
import org.apache.myfaces.custom.skin.skins.MinimalDesktopSkinExtension;
import org.apache.myfaces.custom.skin.skins.MinimalPdaSkinExtension;
import org.apache.myfaces.custom.skin.skins.MinimalPortletSkinExtension;
import org.apache.myfaces.custom.skin.skins.SimpleDesktopSkin;
import org.apache.myfaces.custom.skin.skins.SimplePdaSkin;
import org.apache.myfaces.custom.skin.skins.SimplePortletSkin;
import org.apache.myfaces.trinidad.logging.SkinLogger;
import org.apache.myfaces.trinidad.skin.Skin;
import org.apache.myfaces.trinidad.skin.SkinAddition;
import org.apache.myfaces.trinidad.skin.SkinFactory;
import org.apache.myfaces.trinidadinternal.config.LazyValueExpression;
import org.apache.myfaces.trinidadinternal.share.io.NameResolver;
import org.apache.myfaces.trinidadinternal.share.xml.ClassParserFactory;
import org.apache.myfaces.trinidadinternal.share.xml.ParseContextImpl;
import org.apache.myfaces.trinidadinternal.share.xml.ParserFactory;
import org.apache.myfaces.trinidadinternal.share.xml.ParserManager;
import org.apache.myfaces.trinidadinternal.share.xml.TreeBuilder;
import org.apache.myfaces.trinidadinternal.share.xml.XMLProvider;
import org.apache.myfaces.trinidadinternal.share.xml.XMLUtils;
import org.apache.myfaces.trinidadinternal.skin.BaseSkinUtils;
import org.apache.myfaces.trinidadinternal.skin.SkinExtension;
import org.apache.myfaces.trinidadinternal.skin.SkinFactoryImpl;
import org.apache.myfaces.trinidadinternal.skin.parse.SkinAdditionNode;
import org.apache.myfaces.trinidadinternal.skin.parse.SkinNode;
import org.apache.myfaces.trinidadinternal.skin.parse.SkinsNode;
import org.apache.myfaces.trinidadinternal.skin.parse.XMLConstants;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utility functions for creating Skin objects and SkinExtension objects 
 * from the trinidad-skins.xml file and
 * adding them to the SkinFactory. It also adds SkinAdditions to the Skin objects.
 * @version $Name:  $ ($Revision: 267 $) $Date: 2008-11-27 18:55:01 -0500 (Thu, 27 Nov 2008) $
 */
public class SkinUtils extends BaseSkinUtils {

    /**
   * Register the base skins with the SkinFactory. (simple/minimal)
   * Make sure the SkinFactory.getFactory() does not return null before
   * calling this method.
   */
    public static void registerBaseSkins() {
        SkinFactory skinFactory = SkinFactory.getFactory();
        if (skinFactory == null) {
            SkinFactory.setFactory(new SkinFactoryImpl());
            skinFactory = SkinFactory.getFactory();
        }
        _registerTrinidadSkins(skinFactory);
    }

    /**
   * Register any custom skin extensions (and skin-additions) found in the
   * trinidad-skins.xml file with the SkinFactory.
   * 
   * Make sure the SkinFactory.getFactory() does not return null before
   * calling this method.
   * You should call registerBaseSkins() before calling this method.
   * @param context ServletContext, used to get the trinidad-skins.xml file.
   */
    public static void registerSkinExtensions(ExternalContext context) {
        SkinFactory skinFactory = SkinFactory.getFactory();
        if (skinFactory == null) {
            SkinFactory.setFactory(new SkinFactoryImpl());
            skinFactory = SkinFactory.getFactory();
        }
        _registerSkinExtensionsAndAdditions(context, skinFactory);
    }

    /**
   * 
   * @param provider an XMLProvider implementation.
   * @param resolver A NameResolver that can be used to locate
   *                 resources, such as source images for colorized
   *                 icons.
   * @param inputStream the inputStream. Must be non-null
   * @param parserManager the ParserManager to use for parsing
   *                Must  be non-null.
   * @param configFile The name of the config file we are parsing.
   * @return A SkinsNode object (contains a List of SkinNode and a List of SkinAdditionNode)
   */
    private static SkinsNode _getSkinsNodeFromInputStream(XMLProvider provider, NameResolver resolver, InputStream inputStream, ParserManager parserManager, String configFile) {
        if (inputStream == null) throw new NullPointerException(_LOG.getMessage("NO_INPUTSTREAM"));
        if (parserManager == null) throw new NullPointerException(_LOG.getMessage("NULL_PARSEMANAGER"));
        SkinsNode skinsNode = null;
        try {
            InputSource input = new InputSource();
            input.setByteStream(inputStream);
            input.setPublicId(configFile);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            ParseContextImpl context = new ParseContextImpl();
            if (resolver != null) XMLUtils.setResolver(context, resolver);
            TreeBuilder builder = new TreeBuilder(parserManager, SkinsNode.class);
            skinsNode = ((SkinsNode) builder.parse(provider, input, context));
        } catch (IOException ioe) {
            _LOG.warning(ioe);
        } catch (SAXException saxe) {
            _LOG.warning(saxe);
        } finally {
            try {
                inputStream.close();
            } catch (IOException ioe) {
                ;
            }
        }
        return skinsNode;
    }

    /**
   * Creates a ParserManager pre-registered witih all
   * the default ParserFactories needed to create SkinExtensions.
   */
    public static ParserManager createDefaultManager() {
        ParserManager manager = new ParserManager();
        _registerFactory(manager, SkinsNode.class, "SkinsNode");
        _registerFactory(manager, SkinNode.class, "SkinNode");
        _registerFactory(manager, SkinAdditionNode.class, "SkinAdditionNode");
        return manager;
    }

    private static ParserManager _getDefaultManager() {
        if (_sManager == null) _sManager = createDefaultManager();
        return _sManager;
    }

    private static void _registerFactory(ParserManager manager, Class<?> expectedType, String baseName) {
        String className = _LAF_PARSE_PACKAGE + baseName + "Parser";
        ParserFactory factory = new ClassParserFactory(className);
        manager.registerFactory(expectedType, XMLConstants.SKIN_NAMESPACE, factory);
    }

    /**
   * register the Trinidad skins: simpleDesktopSkin, simplePdaSkin,
   * and minimalDesktopSkin, minimalPdaSkin, and portlet skins.
   * @param skinFactory
   */
    private static void _registerTrinidadSkins(SkinFactory skinFactory) {
        SimpleDesktopSkin simpleDesktopSkin = new SimpleDesktopSkin();
        skinFactory.addSkin(simpleDesktopSkin.getId(), simpleDesktopSkin);
        SimplePdaSkin simplePdaSkin = new SimplePdaSkin();
        skinFactory.addSkin(simplePdaSkin.getId(), simplePdaSkin);
        SimplePortletSkin simplePortletSkin = new SimplePortletSkin();
        skinFactory.addSkin(simplePortletSkin.getId(), simplePortletSkin);
        MinimalDesktopSkinExtension minimalDesktopSkin = new MinimalDesktopSkinExtension(simpleDesktopSkin);
        skinFactory.addSkin(minimalDesktopSkin.getId(), minimalDesktopSkin);
        MinimalPdaSkinExtension minimalPdaSkin = new MinimalPdaSkinExtension(simplePdaSkin);
        skinFactory.addSkin(minimalPdaSkin.getId(), minimalPdaSkin);
        MinimalPortletSkinExtension minimalPortletSkin = new MinimalPortletSkinExtension(simplePortletSkin);
        skinFactory.addSkin(minimalPortletSkin.getId(), minimalPortletSkin);
    }

    /**
   * Parse the trinidad-skins.xml file for SkinExtensions and SkinAdditionNodes and add each
   * SkinExtension to the skinFactory and each SkinAddition to its skin.
   * First find all the trinidad-skins.xml files that are in META-INF directory, and 
   * add those skins and skin additions.
   * Then find the WEB-INF/trinidad-skins.xml file and add those skins and skin additions.
   * The skins are ordered so that the 'extended' skins are registered before the skins that extend
   * them.
   * @param context
   * @param skinFactory
   */
    private static void _registerSkinExtensionsAndAdditions(ExternalContext context, SkinFactory skinFactory) {
        if (context == null) return;
        List<SkinsNode> metaInfSkinsNodeList = _getMetaInfSkinsNodeList();
        List<SkinNode> metaInfSkinNodes = new ArrayList<SkinNode>();
        for (SkinsNode skinsNode : metaInfSkinsNodeList) {
            metaInfSkinNodes.addAll(skinsNode.getSkinNodes());
        }
        List<SkinNode> sortedMetaInfSkinNodes = _sortSkinNodes(skinFactory, metaInfSkinNodes);
        for (SkinNode skinNode : sortedMetaInfSkinNodes) {
            _addSkinToFactory(skinFactory, skinNode, true);
        }
        SkinsNode webInfSkinsNode = _getWebInfSkinsNode(context);
        if (webInfSkinsNode != null) {
            List<SkinNode> webInfSkinNodes = webInfSkinsNode.getSkinNodes();
            List<SkinNode> sortedWebInfSkinNodes = _sortSkinNodes(skinFactory, webInfSkinNodes);
            for (SkinNode skinNode : sortedWebInfSkinNodes) {
                _addSkinToFactory(skinFactory, skinNode, false);
            }
        }
        FacesContext fContext = FacesContext.getCurrentInstance();
        _registerMetaInfSkinAdditions(fContext, skinFactory, metaInfSkinsNodeList);
        if (webInfSkinsNode != null) {
            List<SkinAdditionNode> skinAdditionNodeList = webInfSkinsNode.getSkinAdditionNodes();
            _registerSkinAdditions(fContext, skinFactory, skinAdditionNodeList, false);
        }
    }

    /**
   * Given the a List of SkinNodes, sort them so that the SkinNodes in such a way so that
   * when we register the skins we make sure that the 'base' skins are registered before
   * skins that extend them.
   * @param skinFactory
   * @param skinNodes
   * @return sorted List of SkinNodes
   */
    private static List<SkinNode> _sortSkinNodes(SkinFactory skinFactory, List<SkinNode> skinNodes) {
        List<SkinNode> sortedSkinNodes = new ArrayList<SkinNode>();
        List<String> skinNodesAdded = new ArrayList<String>();
        List<String> baseSkinIds = new ArrayList<String>();
        for (Iterator<String> i = skinFactory.getSkinIds(); i.hasNext(); ) {
            baseSkinIds.add(i.next());
        }
        for (SkinNode skinNode : skinNodes) {
            String skinExtends = skinNode.getSkinExtends();
            if (skinExtends == null) {
                sortedSkinNodes.add(skinNode);
                skinNodesAdded.add(skinNode.getId());
            }
        }
        _sortSkinNodesWithExtensions(skinNodes, sortedSkinNodes, skinNodesAdded, baseSkinIds, 0);
        return sortedSkinNodes;
    }

    /**
   * This sorts SkinNodes that have their 'extends' value set, which means the skin
   * extends another skin. The order of our skin nodes matters in this case. We want the 
   * base skin to be registered first.
   * @param skinNodes
   * @param sortedSkinNodes
   * @param skinNodesAdded
   * @param baseSkinIds
   */
    private static void _sortSkinNodesWithExtensions(List<SkinNode> skinNodes, List<SkinNode> sortedSkinNodes, List<String> skinNodesAdded, List<String> baseSkinIds, int originalLeftOverListSize) {
        List<SkinNode> leftOverList = new ArrayList<SkinNode>();
        for (SkinNode skinNode : skinNodes) {
            String skinExtends = skinNode.getSkinExtends();
            if (skinExtends != null) {
                if (skinNodesAdded.contains(skinExtends) || baseSkinIds.contains(skinExtends)) {
                    sortedSkinNodes.add(skinNode);
                    skinNodesAdded.add(skinNode.getId());
                } else {
                    leftOverList.add(skinNode);
                }
            }
        }
        if ((originalLeftOverListSize > 0) && (leftOverList.size() == originalLeftOverListSize)) {
            StringBuffer buffer = new StringBuffer();
            for (SkinNode leftOverNode : leftOverList) {
                buffer.append("Skin with id: " + leftOverNode.getId() + " extends skin with id: " + leftOverNode.getSkinExtends() + "\n");
                sortedSkinNodes.add(leftOverNode);
                skinNodesAdded.add(leftOverNode.getId());
            }
            _LOG.warning("The following skins extend each other in a circular " + "fashion or the skin they extend does not exist.\n" + buffer.toString());
        } else if (leftOverList.size() > 0) {
            _sortSkinNodesWithExtensions(leftOverList, sortedSkinNodes, skinNodesAdded, baseSkinIds, leftOverList.size());
        }
    }

    /**
   * Given a skinNode, create a Skin object and 
   * register the SkinExtension object with the skinFactory
   * @param skinFactory
   * @param skinNode
   */
    private static void _addSkinToFactory(SkinFactory skinFactory, SkinNode skinNode, boolean isMetaInfFile) {
        String renderKitId = skinNode.getRenderKitId();
        String id = skinNode.getId();
        String family = skinNode.getFamily();
        String styleSheetName = skinNode.getStyleSheetName();
        String bundleName = skinNode.getBundleName();
        String translationSourceExpression = skinNode.getTranslationSourceExpression();
        if (renderKitId == null) renderKitId = _RENDER_KIT_ID_DESKTOP;
        Skin baseSkin = null;
        String skinExtends = skinNode.getSkinExtends();
        if (skinExtends != null) baseSkin = skinFactory.getSkin(null, skinExtends);
        if (baseSkin == null) {
            baseSkin = _getDefaultBaseSkin(skinFactory, renderKitId);
            if (skinExtends != null) {
                _LOG.severe("UNABLE_LOCATE_BASE_SKIN", new String[] { skinExtends, id, family, renderKitId, baseSkin.getId() });
            }
        }
        if (styleSheetName != null) {
            if (isMetaInfFile) styleSheetName = _prependMetaInf(styleSheetName);
        }
        Skin skin = null;
        if (bundleName != null) {
            skin = new SkinExtension(baseSkin, id, family, renderKitId, styleSheetName, bundleName);
        } else {
            ValueExpression translationSourceVE = null;
            if (translationSourceExpression != null) {
                translationSourceVE = _createTranslationSourceValueExpression(translationSourceExpression);
            }
            if (translationSourceVE != null) {
                skin = new SkinExtension(baseSkin, id, family, renderKitId, styleSheetName, translationSourceVE);
            } else {
                skin = new SkinExtension(baseSkin, id, family, renderKitId, styleSheetName);
            }
        }
        skinFactory.addSkin(id, skin);
    }

    private static ValueExpression _createTranslationSourceValueExpression(String translationSourceExpression) {
        if (translationSourceExpression != null) {
            translationSourceExpression = translationSourceExpression.trim();
            return LazyValueExpression.createValueExpression(translationSourceExpression, Object.class);
        } else return null;
    }

    /**
   * Get the WEB-INF/trinidad-skins.xml file, parse it, and return a List of SkinsNode objects. 
   * @param context ServletContext used to getResourceAsStream
   * @return List of SkinNodes (skin elements) found in trinidad-skins.xml
   */
    private static SkinsNode _getWebInfSkinsNode(ExternalContext context) {
        InputStream in = context.getResourceAsStream(_CONFIG_FILE);
        if (in != null) {
            SkinsNode webInfSkinsNode = _getSkinsNodeFromInputStream(null, null, in, _getDefaultManager(), _CONFIG_FILE);
            return webInfSkinsNode;
        } else {
            return null;
        }
    }

    /**
   * Get all the META-INF/trinidad-skins.xml files, parse them, and from each file we get
   * a SkinsNode object -- the information inside the &lt;skins&gt; element -- each skin
   * and each skin-addition.
   * @return Each SkinsNode object we get from each META-INF/trinidad-skins.xml file, 
   * in a List<SkinsNode>.
   */
    private static List<SkinsNode> _getMetaInfSkinsNodeList() {
        List<SkinsNode> allSkinsNodes = new ArrayList<SkinsNode>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> urls = loader.getResources(_META_INF_CONFIG_FILE);
            Set<String> urlPaths = new HashSet<String>(16);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                boolean successfullyAdded = urlPaths.add(url.getPath());
                if (!successfullyAdded) {
                    if (_LOG.isFinest()) {
                        _LOG.finest("Skipping skin URL:{0} because it was already processed. " + "It was on the classpath more than once.", url);
                    }
                } else {
                    _LOG.finest("Processing skin URL:{0}", url);
                    InputStream in = url.openStream();
                    try {
                        if (in != null) {
                            SkinsNode metaInfSkinsNode = _getSkinsNodeFromInputStream(null, null, in, _getDefaultManager(), _META_INF_CONFIG_FILE);
                            allSkinsNodes.add(metaInfSkinsNode);
                        }
                    } catch (Exception e) {
                        _LOG.warning("ERR_PARSING", url);
                        _LOG.warning(e);
                    } finally {
                        in.close();
                    }
                }
            }
        } catch (IOException e) {
            _LOG.severe("ERR_LOADING_FILE", _META_INF_CONFIG_FILE);
            _LOG.severe(e);
        }
        return allSkinsNodes;
    }

    private static Skin _getDefaultBaseSkin(SkinFactory factory, String renderKitId) {
        String baseSkinId = (_RENDER_KIT_ID_PDA.equals(renderKitId)) ? _SIMPLE_PDA_SKIN_ID : _SIMPLE_DESKTOP_SKIN_ID;
        Skin baseSkin = factory.getSkin(null, baseSkinId);
        if (baseSkin == null) _LOG.severe(_UNKNOWN_BASE_SKIN_ERROR + baseSkinId);
        return baseSkin;
    }

    private static void _registerMetaInfSkinAdditions(FacesContext fContext, SkinFactory skinFactory, List<SkinsNode> metaInfSkinsNodeList) {
        List<SkinAdditionNode> skinAdditionNodeList = new ArrayList<SkinAdditionNode>();
        for (SkinsNode skinsNode : metaInfSkinsNodeList) {
            skinAdditionNodeList.addAll(skinsNode.getSkinAdditionNodes());
        }
        Collections.sort(skinAdditionNodeList);
        _registerSkinAdditions(fContext, skinFactory, skinAdditionNodeList, true);
    }

    /**
   * Get the skin id and other information from each SkinAdditionNode and
   * get the skin and register the SkinAddition with the skin
   * @param fContext
   * @param skinFactory
   * @param skinAdditionNodeList
   * @param isMetaInfFile true if the trinidad-skins.xml file is in the META-INF
   * directory.
   */
    private static void _registerSkinAdditions(FacesContext fContext, SkinFactory skinFactory, List<SkinAdditionNode> skinAdditionNodeList, boolean isMetaInfFile) {
        for (SkinAdditionNode skinAdditionNode : skinAdditionNodeList) {
            String skinId = skinAdditionNode.getSkinId();
            String styleSheetName = skinAdditionNode.getStyleSheetName();
            String resourceBundleName = skinAdditionNode.getResourceBundleName();
            String translationSourceExpression = skinAdditionNode.getTranslationSourceExpression();
            Skin skin = skinFactory.getSkin(fContext, skinId);
            if (skin != null && ((styleSheetName != null) || (resourceBundleName != null) || (translationSourceExpression != null))) {
                if (isMetaInfFile && (styleSheetName != null)) styleSheetName = _prependMetaInf(styleSheetName);
                SkinAddition addition = null;
                if (resourceBundleName != null) {
                    addition = new SkinAddition(styleSheetName, resourceBundleName);
                } else {
                    ValueExpression translationSourceVE = null;
                    if (translationSourceExpression != null) {
                        translationSourceVE = _createTranslationSourceValueExpression(translationSourceExpression);
                    }
                    if (translationSourceVE != null) {
                        addition = new SkinAddition(styleSheetName, translationSourceVE);
                    } else {
                        addition = new SkinAddition(styleSheetName);
                    }
                }
                skin.addSkinAddition(addition);
            }
        }
    }

    /**
   * Prepend META-INF to the styleSheetName if it doesn't begin with '/'.
   * @param styleSheetName
   * @return String styleSheetName or the styleSheetName prepended with META-INF/
   */
    private static String _prependMetaInf(String styleSheetName) {
        if (!(styleSheetName.startsWith("/"))) return _META_INF_DIR.concat(styleSheetName); else return styleSheetName;
    }

    private SkinUtils() {
    }

    private static ParserManager _sManager;

    private static final String _LAF_PARSE_PACKAGE = "org.apache.myfaces.trinidadinternal.skin.parse.";

    private static final String _CONFIG_FILE = "/WEB-INF/trinidad-skins.xml";

    private static final String _META_INF_CONFIG_FILE = "META-INF/trinidad-skins.xml";

    private static final String _META_INF_DIR = "META-INF/";

    private static final SkinLogger _LOG = SkinLogger.createSkinLogger(SkinUtils.class);

    private static final String _UNKNOWN_BASE_SKIN_ERROR = "Unable to locate base skin: ";

    private static final String _RENDER_KIT_ID_DESKTOP = "org.apache.myfaces.trinidad.desktop";

    private static final String _RENDER_KIT_ID_PDA = "org.apache.myfaces.trinidad.pda";

    private static final String _SIMPLE_PDA_SKIN_ID = "simple.pda";

    private static final String _SIMPLE_DESKTOP_SKIN_ID = "simple.desktop";
}
