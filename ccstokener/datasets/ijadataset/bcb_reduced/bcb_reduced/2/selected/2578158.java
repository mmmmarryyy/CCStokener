package com.usoog.hextd.server;

import com.usoog.commons.gamecore.map.Category;
import com.usoog.commons.gamecore.map.MapInfo;
import com.usoog.commons.gamecore.map.MapListener;
import com.usoog.commons.gamecore.map.MapLoader;
import com.usoog.commons.gamecore.message.MessageMapList;
import com.usoog.hextd.core.MapLoaderClient;
import com.usoog.hextd.map.GameMapImplementation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.jdo.Extent;
import javax.jdo.PersistenceManager;
import javax.jdo.PersistenceManagerFactory;
import javax.jdo.Query;
import javax.jdo.Transaction;

/**
 * MapLoaderServer loads maps from the webserver into the gameserver.
 *
 * @author hylke
 */
public class MapLoaderServer implements MapLoader {

    /**
	 * All categories as defined in the database.
	 */
    private Map<Integer, Category> categoryMap = new HashMap<Integer, Category>();

    /**
	 * The backup list of categories.
	 */
    private Map<Integer, Category> categoriesLocal = new LinkedHashMap<Integer, Category>();

    /**
	 * The cache for remote maps.
	 */
    private Map<Integer, MapInfo> mapCache = new HashMap<Integer, MapInfo>();

    /**
	 * The cache for local maps.
	 */
    private Map<String, MapInfo> mapCacheLocal = new HashMap<String, MapInfo>();

    /**
	 * The remote maplist fetched from database.
	 */
    private List<MapInfo> mapList = new ArrayList<MapInfo>();

    /**
	 * The local in-jar backup maplist
	 */
    private List<MapInfo> mapListLocal = new ArrayList<MapInfo>();

    /**
	 * The context for the local map url.
	 */
    private URL localMapContextUrl;

    /**
	 * The url for backup maps.
	 */
    private URL backupUrl;

    /**
	 * A cached message with the map list. Since the map list doesn't change
	 * all that much, we just keep it.
	 */
    private MessageMapList mapListMessage;

    /**
	 * Object that want to be informed of map changes.
	 */
    private List<MapListener> mapListeners = new ArrayList<MapListener>();

    /**
	 * The persistenceManagerFactory to use.
	 */
    PersistenceManagerFactory pmf;

    public MapLoaderServer(PersistenceManagerFactory pmf, String backupIndex, URL backupContextUrl) {
        this.pmf = pmf;
        try {
            backupUrl = new URL(backupContextUrl, backupIndex);
            loadLocalIndex(backupUrl);
        } catch (MalformedURLException ex) {
            Logger.getLogger(MapLoaderServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
	 * Fetch a map from the backup location.
	 *
	 * @param fileName
	 * @return
	 */
    public MapInfo loadLocalMapData(String fileName) {
        MapInfo info = mapCacheLocal.get(fileName);
        if (info != null && info.getContent() == null) {
            try {
                BufferedReader bufferedreader;
                URL fetchUrl = new URL(localMapContextUrl, fileName);
                URLConnection urlconnection = fetchUrl.openConnection();
                if (urlconnection.getContentEncoding() != null) {
                    bufferedreader = new BufferedReader(new InputStreamReader(urlconnection.getInputStream(), urlconnection.getContentEncoding()));
                } else {
                    bufferedreader = new BufferedReader(new InputStreamReader(urlconnection.getInputStream(), "utf-8"));
                }
                String line;
                StringBuilder mapContent = new StringBuilder();
                while ((line = bufferedreader.readLine()) != null) {
                    mapContent.append(line);
                    mapContent.append("\n");
                }
                info.setContent(mapContent.toString());
                GameMapImplementation gameMap = GameMapImplementation.createFromMapInfo(info);
            } catch (IOException _ex) {
                System.err.println("HexTD::readFile:: Can't read from " + fileName);
            }
        } else {
            System.err.println("HexTD::readFile:: file not in cache: " + fileName);
        }
        return info;
    }

    /**
	 * Tries to load a local map and adds it to the map cache if successful.
	 * @param fileName
	 * @param contextDir
	 */
    protected void tryLoadLocalFile(String fileName, URL contextDir) {
        MapInfo levelInfo = new MapInfo();
        String[] split = fileName.split(" ");
        fileName = split[0];
        if (split[0].length() == 2 && split[0].charAt(0) == 'c') {
            int catId = Integer.parseInt(split[0].substring(1));
            if (categoryMap == null) {
                categoryMap = new HashMap<Integer, Category>();
            }
            Category category = new Category(catId, split[1]);
            categoryMap.put(catId, category);
            categoriesLocal.put(catId, category);
        } else {
            int cat = 0;
            int id = 0;
            if (split.length > 1) {
                id = Integer.parseInt(split[1]);
                cat = Integer.parseInt(split[2]);
            }
            if (split.length > 3) {
                int parseInt = Integer.parseInt(split[3]);
                if (parseInt > 0) {
                    levelInfo.getPreReq().add(parseInt);
                }
            }
            if (split.length > 4) {
                int parseInt = Integer.parseInt(split[4]);
                if (parseInt > 0) {
                    levelInfo.setNextMap(parseInt);
                }
            }
            levelInfo.getInfo().put("fileName", fileName);
            levelInfo.setCategory(cat);
            levelInfo.setMapId(id);
            levelInfo.setRank(id);
            mapListLocal.add(levelInfo);
            mapCache.put(levelInfo.getMapId(), levelInfo);
            mapCacheLocal.put(fileName, levelInfo);
            loadLocalMapData(fileName);
            if (levelInfo.getPlayers() == 0) {
                levelInfo.setPlayers(1);
            }
            System.out.println("ClientMapLoader:tryLoadLocalFile: added " + levelInfo.getPlayers() + " player map " + levelInfo.getTitle() + " to index.");
        }
    }

    /**
	 * Fetch a message with the latest mapList.
	 * @return
	 */
    public MessageMapList getMapListMessage() {
        if (mapListMessage == null) {
            fetchRemoteIndex();
        }
        return mapListMessage;
    }

    /**
	 * Fetches all maps from the database. If any maps from the local backup
	 * are not in the database, they are added.
	 */
    public void fetchRemoteIndex() {
        PersistenceManager pm = pmf.getPersistenceManager();
        Map<Integer, Category> tempCategories = new HashMap<Integer, Category>();
        Transaction tx = pm.currentTransaction();
        try {
            tx.begin();
            Extent e = pm.getExtent(PersistantCategory.class, true);
            Query q = pm.newQuery(e);
            Collection<PersistantCategory> c = (Collection<PersistantCategory>) q.execute();
            for (PersistantCategory cat : c) {
                Category category = new Category(cat.getId(), cat.getParentId(), cat.getRank(), cat.getName());
                tempCategories.put(category.getId(), category);
            }
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
        System.out.println("MapLoaderServer::Loaded " + tempCategories.size() + " categories from database");
        for (Entry<Integer, Category> entry : categoriesLocal.entrySet()) {
            Integer key = entry.getKey();
            Category category = entry.getValue();
            if (!tempCategories.containsKey(key)) {
                tx = pm.currentTransaction();
                try {
                    tx.begin();
                    PersistantCategory cat = new PersistantCategory(category);
                    pm.makePersistent(cat);
                    tx.commit();
                    tempCategories.put(key, category);
                    System.out.println("MapLoaderServer::Added Category to database: " + cat.getId() + " " + cat.getName());
                } finally {
                    if (tx.isActive()) {
                        tx.rollback();
                    }
                }
            }
        }
        categoryMap = tempCategories;
        mapList.clear();
        Map<Integer, MapInfo> tempMapCache = new HashMap<Integer, MapInfo>();
        tx = pm.currentTransaction();
        try {
            tx.begin();
            Extent e = pm.getExtent(PersistantMapInfo.class, true);
            Query q = pm.newQuery(e);
            Collection<PersistantMapInfo> c = (Collection<PersistantMapInfo>) q.execute();
            for (PersistantMapInfo pmi : c) {
                int mapId = pmi.getMapId();
                MapInfo cached = mapCache.get(mapId);
                MapInfo mapInfo = pmi.getMapInfo(cached);
                mapList.add(mapInfo);
                tempMapCache.put(mapId, mapInfo);
                mapCache.put(mapId, mapInfo);
            }
            tx.commit();
        } finally {
            if (tx.isActive()) {
                tx.rollback();
            }
        }
        System.out.println("MapLoaderServer::Loaded " + tempMapCache.size() + " maps from database");
        for (MapInfo map : mapListLocal) {
            int mapId = map.getMapId();
            if (!tempMapCache.containsKey(mapId)) {
                tx = pm.currentTransaction();
                try {
                    tx.begin();
                    PersistantMapInfo pMap = new PersistantMapInfo();
                    pMap.fillWithMapInfo(map);
                    pMap.setMapId(mapId);
                    Map<String, String> info = pMap.getInfo();
                    info.remove("fileName");
                    pm.makePersistent(pMap);
                    mapCache.put(mapId, map);
                    mapList.add(map);
                    tx.commit();
                    System.out.println("MapLoaderServer::Added Map to database: " + pMap.getMapId() + " " + map.getTitle());
                } finally {
                    if (tx.isActive()) {
                        tx.rollback();
                    }
                }
            }
        }
        mapListMessage.setMapList(mapList);
        mapListMessage.setCategoriesById(categoryMap);
        pm.close();
    }

    /**
	 * Loads the index of local map files.
	 * @param fileUrl the url of the index file for the local maps
	 * @param contextUrl the context to which the index url is relative.
	 */
    private void loadLocalIndex(URL indexUrl) {
        localMapContextUrl = indexUrl;
        List<String> levelNames;
        levelNames = MapLoaderClient.getLevelIndex(indexUrl);
        for (String levelName : levelNames) {
            tryLoadLocalFile(levelName, indexUrl);
        }
        mapListMessage = new MessageMapList();
        mapListMessage.setMapList(mapListLocal);
        mapListMessage.setCategoriesById(categoryMap);
    }

    @Override
    public MapInfo getMap(int mapId) {
        MapInfo info = mapCache.get(mapId);
        if (info == null) {
            fetchRemoteIndex();
            info = mapCache.get(mapId);
        }
        if (info != null && info.getContent() == null) {
            System.out.println("MapLoaderServer::getMap:map without content!");
        }
        return info;
    }

    @Override
    public List<MapInfo> getMapList() {
        if (mapList != null) {
            return mapList;
        } else {
            return mapListLocal;
        }
    }

    @Override
    public void addMapListener(MapListener l) {
        this.mapListeners.add(l);
    }

    public void removeMapListener(MapListener l) {
        mapListeners.remove(l);
    }
}
