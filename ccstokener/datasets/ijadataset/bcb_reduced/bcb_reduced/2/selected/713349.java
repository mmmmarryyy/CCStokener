package com.application.areca.launcher.gui.common;

import java.net.URL;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import com.myJava.util.log.Logger;

public class ArecaImages {

    private static final boolean showSmallIcons = false;

    private static final String ICON_DIR = "icons";

    private static final String B_PREFIX = showSmallIcons ? "" : "big/";

    public static Image ICO_SMALL = loadImage("ico_16.png");

    public static Image ICO_BIG = loadImage("ico_72.png");

    public static Image ICO_VOID = loadImage("void.png");

    public static Image ICO_CHANNEL = loadImage("run.png");

    public static Image ICO_REF_TARGET = loadImage("database.png");

    public static Image ICO_REF_PROCESS = loadImage("folder_blue.png");

    public static Image ICO_REF_WORKSPACE = loadImage("desktop.png");

    public static Image ICO_TARGET_NEW = loadImage("filenew.png");

    public static Image ICO_TARGET_NEW_B = loadImage(B_PREFIX + "filenew.png");

    public static Image ICO_PROCESS_NEW = ICO_TARGET_NEW;

    public static Image ICO_TARGET_EDIT = loadImage("edit.png");

    public static Image ICO_TARGET_EDIT_B = loadImage(B_PREFIX + "edit.png");

    public static Image ICO_PROCESS_EDIT = ICO_TARGET_EDIT;

    public static Image ICO_FIND = loadImage("find.png");

    public static Image ICO_HISTORY = loadImage("karm.png");

    public static Image ICO_WORKSPACE_OPEN = ICO_REF_WORKSPACE;

    public static Image ICO_WORKSPACE_OPEN_B = loadImage(B_PREFIX + "desktop.png");

    public static Image ICO_HELP = loadImage("help.png");

    public static Image ICO_HELP_B = loadImage(B_PREFIX + "help.png");

    public static Image ICO_SAVE = loadImage("filesave.png");

    public static Image ICO_CONFIGURE = loadImage("configure.png");

    public static Image ICO_CONFIGURE_B = loadImage(B_PREFIX + "configure.png");

    public static Image ICO_ACT_ARCHIVE = loadImage("db_add.png");

    public static Image ICO_ACT_ARCHIVE_B = loadImage(B_PREFIX + "db_add.png");

    public static Image ICO_ACT_MERGE = loadImage("db_update.png");

    public static Image ICO_ACT_MERGE_B = loadImage(B_PREFIX + "db_update.png");

    public static Image ICO_ACT_RESTAURE = loadImage("db_comit.png");

    public static Image ICO_ACT_RESTAURE_B = loadImage(B_PREFIX + "db_comit.png");

    public static Image ICO_ACT_DELETE = loadImage("db_remove.png");

    public static Image ICO_ACT_DELETE_B = loadImage(B_PREFIX + "db_remove.png");

    public static Image ICO_ARCHIVE_DETAIL = loadImage("db_status.png");

    public static Image ICO_ACT_CANCEL = ICO_ACT_DELETE;

    public static Image ICO_FS_FILE = loadImage("binary.png");

    public static Image ICO_FS_PIPE = loadImage("pipe.png");

    public static Image ICO_FS_FOLDER = ICO_REF_PROCESS;

    public static Image ICO_FS_FOLDER_FULL = loadImage("folder_red.png");

    public static Image ICO_FS_FOLDER_DIFFERENTIAL = loadImage("folder_green.png");

    public static Image ICO_FILTER = loadImage("run.png");

    public static Image ICO_HISTO_NEW = ICO_TARGET_NEW;

    public static Image ICO_HISTO_FOLDER_NEW = ICO_REF_PROCESS;

    public static Image ICO_HISTO_EDIT = ICO_TARGET_EDIT;

    public static Image ICO_HISTO_DELETE = loadImage("delete.png");

    public static Image ICO_TAB_LOG = loadImage("log.png");

    public static Image ICO_TAB_LOG_WARN = loadImage("log_warn.png");

    public static Image ICO_TAB_LOG_ERR = loadImage("log_err.png");

    private static Image loadImage(String strUrl) {
        try {
            URL url = ClassLoader.getSystemClassLoader().getResource(ICON_DIR + "/" + strUrl);
            return new Image(Display.getCurrent(), url.openStream());
        } catch (Exception e) {
            Logger.defaultLogger().error(e);
            return null;
        }
    }
}
