package net.sf.webwarp.modules.menu.impl;

import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import net.sf.webwarp.modules.menu.Menu;
import net.sf.webwarp.modules.menu.MenuItem;
import net.sf.webwarp.modules.menu.MenuManager;
import net.sf.webwarp.modules.menu.Menubar;
import net.sf.webwarp.util.xml.Dom4jUtil;

/**
 * Reader class for reading a menu config file.
 * 
 * @version $Id: $
 * @author atr
 */
@SuppressWarnings("unchecked")
final class ConfigReader {

    private Logger log = Logger.getLogger(getClass());

    private MenuManager menuManager;

    private URL url;

    private Set<String> deps;

    private String id;

    private boolean installed;

    ConfigReader(MenuManager menuManager, URL url) {
        if (log.isDebugEnabled()) {
            log.debug("Initialized config reader for URL '" + url + "'.");
        }
        this.menuManager = menuManager;
        this.url = url;
    }

    public String getId() {
        if (id == null) {
            checkDependencies();
        }
        return id;
    }

    public Set<String> getDependencies() {
        if (deps == null) {
            checkDependencies();
        }
        return deps;
    }

    private void checkDependencies() {
        if (log.isDebugEnabled()) {
            log.debug("Reading: " + url + " (prepare)...");
        }
        SAXReader reader = new SAXReader();
        InputStream is = null;
        try {
            is = url.openStream();
            Document document = reader.read(is);
            Element root = document.getRootElement();
            id = Dom4jUtil.getChildTextTrimChecked(root, "module-id");
            String depConfig = Dom4jUtil.getChildTextTrimOptional(root, "depends-on", null);
            this.deps = new HashSet<String>();
            if (depConfig != null) {
                String[] depsArray = StringUtils.split(depConfig, ",");
                for (int i = 0; i < depsArray.length; i++) {
                    deps.add(depsArray[i]);
                }
            }
        } catch (Exception e) {
            log.error("Error reading menu config (prepare) from " + url, e);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                log.error("Error closing input stream (prepare).", e);
            }
        }
        if (log.isInfoEnabled()) {
            log.info("Menu config prepared: " + url);
        }
    }

    /**
     * Reads and initializes the menu items from the config. It is a precondition that all dependent items are already
     * configured.
     * 
     * @throws Exception
     */
    public void read() {
        if (log.isDebugEnabled()) {
            log.debug("Reading: " + url + "...");
        }
        SAXReader reader = new SAXReader();
        InputStream is = null;
        try {
            is = url.openStream();
            Document document = reader.read(is);
            Element root = document.getRootElement();
            boolean ignoreDuplicates = Dom4jUtil.getAttributeBooleanOptional(root, "ignoreDuplicates", true);
            boolean overwriteExisting = Dom4jUtil.getAttributeBooleanOptional(root, "overwriteExisting", false);
            for (Iterator<Element> iter = root.elementIterator(); iter.hasNext(); ) {
                Element itemElem = iter.next();
                String elemName = itemElem.getName();
                if ("menubar".equals(elemName)) {
                    setupMenubar(itemElem, ignoreDuplicates, overwriteExisting);
                } else if ("menu".equals(elemName)) {
                    setupMenu(itemElem, ignoreDuplicates, overwriteExisting);
                } else if ("menu-item".equals(elemName)) {
                    setupMenuItem(itemElem, ignoreDuplicates, overwriteExisting);
                } else if ("separator".equals(elemName)) {
                    setupSeparator(itemElem, ignoreDuplicates, overwriteExisting);
                } else {
                    if (!"overwriteExisting".equals(elemName) && !"ignoreDuplicates".equals(elemName) && !"module-id".equals(elemName) && !"depends-on".equals(elemName)) {
                        log.warn("Invalid XML element encountered in menu.xml: " + elemName);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.fatal("Error reading menu config from " + url, e);
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                log.error("Error closing input stream.", e);
            }
            installed = true;
        }
        if (log.isInfoEnabled()) {
            log.info("Menu initialization finished for: " + url);
        }
    }

    private void setupRoles(MenuItem item, Element parent) {
        String roles = Dom4jUtil.getAttributeOptional(parent, "roles", null);
        if (roles != null) {
            String[] roleIDs = StringUtils.split(roles, ",");
            for (int i = 0; i < roleIDs.length; i++) {
                item.addRole(roleIDs[i]);
            }
        }
    }

    private void setupMenubar(Element parent, boolean ignoreDuplicates, boolean overwriteExisting) throws Exception {
        String id = Dom4jUtil.getAttributeChecked(parent, "id");
        if (log.isDebugEnabled()) {
            log.debug("Setting up menubar with ID '" + id + "'.");
        }
        MenubarImpl item = null;
        String className = Dom4jUtil.getAttributeOptional(parent, "class", MenubarImpl.class.getName());
        if (log.isDebugEnabled()) {
            log.debug("Loading menubar class '" + className + "'...");
        }
        Class clazz = Class.forName(className, true, getClass().getClassLoader());
        if (log.isDebugEnabled()) {
            log.debug("Loading constructor (String, String) for '" + className + "'...");
        }
        Constructor constructor = clazz.getConstructor(String.class, String.class);
        String defaultName = Dom4jUtil.getAttributeChecked(parent, "name");
        item = (MenubarImpl) constructor.newInstance(id, defaultName);
        item.setResourceID(Dom4jUtil.getAttributeOptional(parent, "resourceId", null));
        if (menuManager.getMenubar(item.getID()) == null) {
            if (log.isDebugEnabled()) {
                log.debug("Adding menubar to menu tree '" + id + "'...");
            }
            menuManager.setMenubar(item);
        } else {
            if (!ignoreDuplicates) {
                if (log.isDebugEnabled()) {
                    log.warn("Menubar with id '" + id + "' is already existing, ignoring!");
                }
                throw new IllegalArgumentException("Menubar with given id already exists: " + item.getID());
            }
            if (overwriteExisting) {
                if (log.isDebugEnabled()) {
                    log.warn("Menubar with id '" + id + "' is already existing, overwriting!");
                }
                menuManager.setMenubar(item);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("Menubar setup: " + id);
        }
    }

    private void setupMenu(Element parent, boolean ignoreDuplicates, boolean overwriteExisting) throws Exception {
        String itemId = Dom4jUtil.getAttributeChecked(parent, "id");
        if (log.isDebugEnabled()) {
            log.debug("Setting up menu with ID '" + itemId + "'.");
        }
        MenuImpl item = null;
        String className = Dom4jUtil.getAttributeOptional(parent, "class", MenuImpl.class.getName());
        if (log.isDebugEnabled()) {
            log.debug("Loading menu class '" + className + "'...");
        }
        Class clazz = Class.forName(className, true, getClass().getClassLoader());
        if (log.isDebugEnabled()) {
            log.debug("Loading constructor (String, String) for '" + className + "'...");
        }
        Constructor constructor = clazz.getConstructor(String.class, String.class);
        if (log.isDebugEnabled()) {
            log.debug("Menu ID is '" + itemId + "'.");
        }
        String defaultName = Dom4jUtil.getAttributeChecked(parent, "name");
        item = (MenuImpl) constructor.newInstance(itemId, defaultName);
        item.setCommand(Dom4jUtil.getAttributeOptional(parent, "command", null));
        item.setResourceID(Dom4jUtil.getAttributeOptional(parent, "resourceId", null));
        item.setDefaultDescription(Dom4jUtil.getChildTextTrimOptional(parent, "description", null));
        item.setEnabled(Dom4jUtil.getAttributeBooleanOptional(parent, "enabled", true));
        item.setExternalLink(Dom4jUtil.getAttributeOptional(parent, "externalLink", null));
        item.setIcon(Dom4jUtil.getAttributeOptional(parent, "icon", null));
        item.setOpen(Dom4jUtil.getAttributeBooleanOptional(parent, "open", false));
        item.setTarget(Dom4jUtil.getAttributeOptional(parent, "target", null));
        item.setVisible(Dom4jUtil.getAttributeBooleanOptional(parent, "visible", true));
        setupRoles(item, parent);
        String installAt = Dom4jUtil.getAttributeOptional(parent, "installAt", null);
        String installBefore = Dom4jUtil.getAttributeOptional(parent, "installBefore", null);
        String installAfter = Dom4jUtil.getAttributeOptional(parent, "installAfter", null);
        String installParent = Dom4jUtil.getAttributeOptional(parent, "parent", null);
        if (installAt != null) {
            installItem(item, installAt, InstallType.At, ignoreDuplicates, overwriteExisting);
        } else if (installBefore != null) {
            installItem(item, installBefore, InstallType.Before, ignoreDuplicates, overwriteExisting);
        } else if (installAfter != null) {
            installItem(item, installAfter, InstallType.After, ignoreDuplicates, overwriteExisting);
        } else if (installParent != null) {
            installItem(item, installParent, InstallType.Parent, ignoreDuplicates, overwriteExisting);
        } else {
            log.error("No valid install path for menu '" + itemId + "'.");
            throw new IllegalArgumentException("No valid install path found, set one of 'installAt', 'installAfter', 'installBefore'.");
        }
    }

    private void setupMenuItem(Element parent, boolean ignoreDuplicates, boolean overwriteExisting) throws Exception {
        String id = Dom4jUtil.getAttributeChecked(parent, "id");
        if (log.isDebugEnabled()) {
            log.debug("Setting up menu item with ID '" + id + "'.");
        }
        MenuItemImpl item = null;
        String className = Dom4jUtil.getAttributeOptional(parent, "class", MenuItemImpl.class.getName());
        if (log.isDebugEnabled()) {
            log.debug("Loading menu item class '" + className + "'...");
        }
        Class clazz = Class.forName(className, true, getClass().getClassLoader());
        if (log.isDebugEnabled()) {
            log.debug("Loading constructor (String, String) for '" + className + "'...");
        }
        Constructor constructor = clazz.getConstructor(String.class, String.class);
        String defaultName = Dom4jUtil.getAttributeChecked(parent, "name");
        item = (MenuItemImpl) constructor.newInstance(id, defaultName);
        item.setCommand(Dom4jUtil.getAttributeOptional(parent, "command", null));
        item.setResourceID(Dom4jUtil.getAttributeOptional(parent, "resourceId", null));
        item.setDefaultDescription(Dom4jUtil.getChildTextTrimOptional(parent, "description", null));
        item.setEnabled(Dom4jUtil.getAttributeBooleanOptional(parent, "enabled", true));
        item.setExternalLink(Dom4jUtil.getAttributeOptional(parent, "externalLink", null));
        item.setIcon(Dom4jUtil.getAttributeOptional(parent, "icon", null));
        item.setResourceID(Dom4jUtil.getAttributeOptional(parent, "resourceId", null));
        item.setTarget(Dom4jUtil.getAttributeOptional(parent, "target", null));
        item.setVisible(Dom4jUtil.getAttributeBooleanOptional(parent, "visible", true));
        setupRoles(item, parent);
        String installAt = Dom4jUtil.getAttributeOptional(parent, "installAt", null);
        String installBefore = Dom4jUtil.getAttributeOptional(parent, "installBefore", null);
        String installAfter = Dom4jUtil.getAttributeOptional(parent, "installAfter", null);
        String installParent = Dom4jUtil.getAttributeOptional(parent, "parent", null);
        if (installAt != null) {
            installItem(item, installAt, InstallType.At, ignoreDuplicates, overwriteExisting);
        } else if (installBefore != null) {
            installItem(item, installBefore, InstallType.Before, ignoreDuplicates, overwriteExisting);
        } else if (installAfter != null) {
            installItem(item, installAfter, InstallType.After, ignoreDuplicates, overwriteExisting);
        } else if (installParent != null) {
            installItem(item, installParent, InstallType.Parent, ignoreDuplicates, overwriteExisting);
        } else {
            throw new IllegalArgumentException("No valid install path found, set one of 'installAt', 'installAfter', 'installBefore'.");
        }
    }

    private void setupSeparator(Element parent, boolean ignoreDuplicates, boolean overwriteExisting) throws Exception {
        String id = Dom4jUtil.getAttributeChecked(parent, "id");
        if (log.isDebugEnabled()) {
            log.debug("Setting up separator with ID '" + id + "'.");
        }
        SeparatorImpl item = null;
        item = new SeparatorImpl(id);
        setupRoles(item, parent);
        String installAt = Dom4jUtil.getAttributeOptional(parent, "installAt", null);
        String installBefore = Dom4jUtil.getAttributeOptional(parent, "installBefore", null);
        String installAfter = Dom4jUtil.getAttributeOptional(parent, "installAfter", null);
        String installParent = Dom4jUtil.getAttributeOptional(parent, "parent", null);
        if (installAt != null) {
            installItem(item, installAt, InstallType.At, ignoreDuplicates, overwriteExisting);
        } else if (installBefore != null) {
            installItem(item, installBefore, InstallType.Before, ignoreDuplicates, overwriteExisting);
        } else if (installAfter != null) {
            installItem(item, installAfter, InstallType.After, ignoreDuplicates, overwriteExisting);
        } else if (installParent != null) {
            installItem(item, installParent, InstallType.Parent, ignoreDuplicates, overwriteExisting);
        } else {
            throw new IllegalArgumentException("No valid install path found, set one of 'installAt', 'installAfter', 'installBefore'.");
        }
    }

    private void installItem(MenuItem item, String installPath, InstallType installType, boolean ignoreDuplicates, boolean overwriteExisting) {
        if (!installPath.contains("/") && installType == InstallType.Parent) {
            if (log.isDebugEnabled()) {
                log.debug("Installing '" + item.getID() + "' to menubar'" + installPath + "'...");
            }
            if (item instanceof Menu) {
                Menubar menubar = menuManager.getMenubar(installPath);
                if (menubar == null) {
                    log.error("Menubar not found: " + installPath);
                    throw new IllegalArgumentException("Menubar not found: " + installPath);
                }
                if (menubar.getMenu(item.getID()) == null) {
                    if (log.isDebugEnabled()) {
                        log.debug("Installing menu to menubar '" + installPath + "'...");
                    }
                    menubar.addMenu((Menu) item);
                    if (log.isInfoEnabled()) {
                        log.info("Menu '" + item.getID() + "' installed to menubar'" + installPath + "'...");
                    }
                    return;
                } else {
                    if (!ignoreDuplicates) {
                        if (log.isDebugEnabled()) {
                            log.debug("NOT installing '" + item.getID() + "' to menubar'" + installPath + "', already existing!");
                        }
                        throw new IllegalArgumentException("Menu with given id(" + item.getID() + ") already exists in menubar: " + installPath);
                    }
                    if (overwriteExisting) {
                        log.warn("Overwriting menu (" + item.getID() + ") in menubar: " + installPath);
                        menubar.removeMenu(item.getID());
                        menubar.addMenu((Menu) item);
                        if (log.isInfoEnabled()) {
                            log.info("Menu '" + item.getID() + "' installed to menubar'" + installPath + "'...");
                        }
                        return;
                    }
                }
            } else {
                throw new IllegalArgumentException("Can only add menu instances to path: " + installPath);
            }
        } else if (StringUtils.countMatches(installPath, "/") == 1 && installType != InstallType.Parent) {
            if (log.isDebugEnabled()) {
                log.debug("Installing '" + item.getID() + "' to menubar'" + installPath + "'...");
            }
            if (item instanceof Menu) {
                int index = installPath.indexOf("/");
                String menubarName = installPath.substring(0, index);
                String itemName = installPath.substring(index + 1);
                Menubar menubar = menuManager.getMenubar(menubarName);
                if (menubar == null) {
                    log.error("Menubar not found: " + installPath);
                    throw new IllegalArgumentException("Menubar not found: " + installPath);
                }
                if (menubar.getMenu(item.getID()) == null) {
                    switch(installType) {
                        case Before:
                            log.info("Installing '" + item.getID() + "' BEFORE '" + installPath + "'...");
                            menubar.addMenuBefore(itemName, (Menu) item);
                            break;
                        case After:
                            log.info("Installing '" + item.getID() + "' AFTER '" + installPath + "'...");
                            menubar.addMenuAfter(itemName, (Menu) item);
                            break;
                        case At:
                            if (StringUtils.isNumeric(itemName)) {
                                log.info("Installing '" + item.getID() + "' AT POS '" + itemName + "'...");
                                menubar.addMenu(Integer.parseInt(itemName), (Menu) item);
                            } else {
                                log.info("Installing '" + item.getID() + "' AT/BEFORE '" + itemName + "'...");
                                menubar.addMenuBefore(itemName, (Menu) item);
                            }
                            break;
                        default:
                            return;
                    }
                    return;
                } else {
                    if (!ignoreDuplicates) {
                        log.error("Menu with given id(" + item.getID() + ") already exists in menubar: " + installPath);
                        throw new IllegalArgumentException("Menu with given id(" + item.getID() + ") already exists in menubar: " + installPath);
                    }
                    if (overwriteExisting) {
                        log.warn("Overwriting menu (" + item.getID() + ") in menubar: " + installPath);
                        menubar.replaceMenu(itemName, (Menu) item);
                        if (log.isInfoEnabled()) {
                            log.info("Menu '" + item.getID() + "' installed to menubar'" + installPath + "'...");
                        }
                        return;
                    }
                }
            } else {
                log.error("Can only add menu instances to path: " + installPath);
                throw new IllegalArgumentException("Can only add menu instances to path: " + installPath);
            }
        }
        String parentPath = installPath;
        if (installType != InstallType.Parent) {
            parentPath = menuManager.getParentPath(installPath);
        }
        Menu menu = (Menu) menuManager.getItem(parentPath);
        String indexId = null;
        switch(installType) {
            case Before:
                indexId = menuManager.getIndexId(installPath);
                break;
            case Parent:
                menu = (Menu) menuManager.getItem(installPath);
                break;
            case At:
                indexId = menuManager.getIndexId(installPath);
                break;
            case After:
                indexId = menuManager.getIndexId(installPath);
                break;
            default:
                log.error("Can not install item(" + item.getID() + ") to unknown location): " + installPath);
                throw new IllegalArgumentException("Can not install item(" + item.getID() + ") to unknown location): " + installPath);
        }
        if (menu.getMenuItem(item.getID()) != null) {
            if (!ignoreDuplicates) {
                log.error("Menuitem with given id(" + item.getID() + ") already exists in menu: " + parentPath);
                throw new IllegalArgumentException("Menuitem with given id(" + item.getID() + ") already exists in menu: " + parentPath);
            }
            if (overwriteExisting) {
                log.info("Overwriting '" + item.getID() + "'...");
                menu.removeMenuItem(item.getID());
            }
        }
        switch(installType) {
            case Before:
                log.info("Installing '" + item.getID() + "' BEFORE '" + indexId + "' INTO '" + menu.getID() + "'...");
                menu.addMenuItemBefore(indexId, item);
                break;
            case Parent:
                log.info("Installing '" + item.getID() + "' INTO '" + menu.getID() + "'...");
                menu.addMenuItem(item);
                break;
            case At:
                log.info("Installing '" + item.getID() + "' AT POS '" + indexId + "' INTO '" + menu.getID() + "'...");
                menu.addMenuItem(Integer.parseInt(indexId), item);
                break;
            case After:
                log.info("Installing '" + item.getID() + "' AFTER '" + indexId + "' INTO '" + menu.getID() + "'...");
                menu.addMenuItemAfter(indexId, item);
                break;
            default:
                log.error("Can not install item(" + item.getID() + ") to unknown location): " + installPath);
                throw new IllegalArgumentException("Can not install item(" + item.getID() + ") to unknown location): " + installPath);
        }
    }

    public boolean isInstalled() {
        return installed;
    }
}
