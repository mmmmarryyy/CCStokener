package com.nullfish.lib.vfs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.nullfish.lib.vfs.exception.VFSException;
import com.nullfish.lib.vfs.exception.VFSIOException;
import com.nullfish.lib.vfs.exception.WrongPathException;
import com.nullfish.lib.vfs.tag_db.TagDataBase;

/**
 * ファイルシステムを管理するシングルトンクラス。
 * このライブラリの処理の起点となる。
 */
public class VFS {

    private FileFactory[] factories;

    /**
	 * ルートファイルとアクティブなファイルシステムのマップ
	 * 
	 */
    private Map fileSystemsMap = new HashMap();

    /**
	 * 名称とインスタンスのマップ
	 */
    private static Map instanceMap = new HashMap();

    /**
	 * ユーザー情報管理クラス
	 */
    private static UserInfoManager userInfoManager;

    /**
	 * 標準インスタンス
	 */
    public static final String DEFAULT_INSTANCE = "default_file_system_manager";

    /**
	 * ファイルシステム定義ファイルのURL
	 */
    private static final String FILE_SYSTEMS = "/file_systems";

    private static int idSeed = 0;

    public int id = idSeed++;

    private Configuration config = new Configuration();

    private TagDataBase tagDataBase;

    /**
	 * Macのフラグ。
	 * Macはファイル名の大文字小文字を区別しないのに、
	 * File#equalsでは区別するのでその対策。
	 */
    public static boolean IS_MAC = System.getProperty("os.name").indexOf("Mac OS") != -1;

    /**
	 * Javaが6以降かどうかのフラグ
	 */
    public static final boolean JAVA6_OR_LATER = System.getProperty("java.version").compareTo("1.6.0") > 0;

    /**
	 * コンストラクタ。
	 * プライベート。
	 *
	 */
    private VFS() {
        List factoryList = new ArrayList();
        BufferedReader reader = null;
        try {
            URL url = getClass().getResource(FILE_SYSTEMS);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            while (true) {
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    try {
                        FileFactory factory = (FileFactory) Class.forName(line).newInstance();
                        factory.setFileSystemManager(this);
                        factoryList.add(0, factory);
                    } catch (Exception e) {
                        System.err.println("Failed to initialize class : " + line);
                        e.printStackTrace();
                    }
                }
            }
            factories = new FileFactory[factoryList.size()];
            factories = (FileFactory[]) factoryList.toArray(factories);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                }
            }
        }
    }

    /**
	 * パスからファイルを取得する。
	 * @param path
	 * @return
	 * @throws VFSException
	 */
    public VFile getFile(String path) throws VFSException {
        path = path.replace('¥', '\\');
        int pipeIndex = path.lastIndexOf(FileFactory.PIPE);
        String mainPath = path.substring(pipeIndex + 1);
        FileFactory interpreter;
        for (int i = 0; i < factories.length; i++) {
            interpreter = factories[i];
            if (interpreter.isInterpretable(mainPath)) {
                return interpreter.interpretFile(path);
            }
        }
        throw new WrongPathException(path);
    }

    /**
	 * ファイル名からファイルを取得する。
	 * @param fileName
	 * @return
	 * @throws VFSException
	 */
    public VFile getFile(FileName fileName) throws WrongPathException {
        FileFactory interpreter;
        for (int i = 0; i < factories.length; i++) {
            interpreter = factories[i];
            if (interpreter.isBelongingFileName(fileName)) {
                return interpreter.interpretFile(fileName);
            }
        }
        throw new WrongPathException(fileName.getAbsolutePath());
    }

    /**
	 * タグが一致するファイルを取得する
	 * @param tag
	 * @return
	 * @throws VFSException
	 */
    public List getFileByTag(String tag) throws VFSException {
        List rtn = new ArrayList();
        if (tagDataBase != null) {
            try {
                return tagDataBase.findFile(tag, this);
            } catch (SQLException e) {
                throw new VFSIOException(e);
            }
        }
        return rtn;
    }

    /**
	 * ユーザー情報管理クラスをセットする。
	 * @param userInfoManager
	 */
    public static void setUserInfoManager(UserInfoManager userInfoManager) {
        VFS.userInfoManager = userInfoManager;
    }

    /**
	 * ユーザー情報管理クラスを取得する。
	 * @return
	 */
    public static UserInfoManager getUserInfoManager() {
        return userInfoManager;
    }

    /**
	 * ファイルの内部ルートを取得する。
	 * もしも存在しない場合はnullを返す。
	 * @param file
	 * @return
	 */
    public VFile getInnerRoot(VFile file) {
        FileFactory interpreter;
        for (int i = 0; i < factories.length; i++) {
            interpreter = factories[i];
            VFile rtn = interpreter.getInnerRoot(file);
            if (rtn != null) {
                return rtn;
            }
        }
        return null;
    }

    /**
	 * このクラスのインスタンスを取得する。
	 * @return
	 */
    public static VFS getInstance() {
        return getInstance(DEFAULT_INSTANCE);
    }

    /**
	 * このクラスのインスタンスを取得する。
	 * @param key
	 * @return
	 */
    public static VFS getInstance(Object key) {
        VFS manager = (VFS) instanceMap.get(key);
        if (manager == null) {
            manager = new VFS();
            instanceMap.put(key, manager);
        }
        return manager;
    }

    public static void close(Object key) {
        VFS manager = (VFS) instanceMap.get(key);
        if (manager != null) {
            manager.close();
        }
        instanceMap.remove(key);
    }

    private void close() {
        Object[] keys = fileSystemsMap.keySet().toArray();
        for (int i = 0; i < keys.length; i++) {
            try {
                FileSystem fileSystem = (FileSystem) fileSystemsMap.get(keys[i]);
                if (fileSystem != null) {
                    fileSystem.close(null);
                }
            } catch (VFSException e) {
                e.printStackTrace();
            }
        }
        if (tagDataBase != null) {
            tagDataBase.close();
        }
        fileSystemsMap.clear();
        fileSystemsMap = null;
        factories = null;
    }

    public FileSystem[] getFileSystem() {
        FileSystem[] rtn = new FileSystem[fileSystemsMap.size()];
        return (FileSystem[]) fileSystemsMap.values().toArray(rtn);
    }

    /**
	 * アクティブなファイルシステムを追加する。
	 * @param fileSystem
	 */
    public void addFileSystem(FileSystem fileSystem) {
        fileSystemsMap.put(fileSystem.getRootName(), fileSystem);
    }

    /**
	 * ファイルシステムを削除する。
	 * @param fileSystem
	 */
    public void removeFileSystem(FileSystem fileSystem) {
        fileSystemsMap.remove(fileSystem.getRootName());
    }

    /**
	 * アクティブなファイルシステムを取得する。
	 * @param root
	 * @return
	 */
    public FileSystem getFileSystem(FileName root) {
        return (FileSystem) fileSystemsMap.get(root);
    }

    /**
	 * 現在使用されていない（使用者の登録されていない）ファイルシステムを全て閉じる。
	 * @throws VFSException
	 *
	 */
    public synchronized void closeUnusedFileSystem() throws VFSException {
        FileName[] roots = (FileName[]) fileSystemsMap.keySet().toArray(new FileName[0]);
        for (int i = 0; i < roots.length; i++) {
            FileSystem fileSystem = (FileSystem) fileSystemsMap.get(roots[i]);
            if (fileSystem != null && !fileSystem.isInUse()) {
                fileSystem.close(null);
            }
        }
    }

    /**
	 * 設定を取得する。
	 * @return
	 */
    public Configuration getConfiguration() {
        return config;
    }

    /**
	 * 設定を変更した際に呼び出すと、反映される。。
	 *
	 */
    public void configChanged() throws VFSException {
        for (int i = 0; i < factories.length; i++) {
            factories[i].configChanged();
        }
        if (tagDataBase != null) {
            tagDataBase.close();
            tagDataBase = null;
        }
        if ("true".equals(config.getDefaultConfig(TagDataBase.CONFIG_USE_TAG))) {
            tagDataBase = new TagDataBase((String) config.getDefaultConfig(TagDataBase.CONFIG_FILEFISH_DB_DIR));
        }
    }

    public FileFactory[] getFactories() {
        return factories;
    }

    public TagDataBase getTagDataBase() {
        return tagDataBase;
    }
}
