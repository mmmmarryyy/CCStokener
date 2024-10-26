package com.usoog.hextd.core;

import com.usoog.commons.gamecore.map.Category;
import com.usoog.commons.gamecore.map.MapInfo;
import com.usoog.commons.gamecore.map.MapListener;
import com.usoog.commons.gamecore.map.MapLoader;
import com.usoog.commons.gamecore.message.MessageFetch;
import com.usoog.commons.gamecore.message.MessageMapData;
import com.usoog.commons.gamecore.message.MessageMapList;
import com.usoog.commons.network.NetworkConnection;
import com.usoog.hextd.Constants.FetchType;
import com.usoog.hextd.map.GameMapImplementation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * MapLoaderClient loads maps from the gameserver.
 * @author hylke
 */
public class MapLoaderClient implements MapLoader {

    private Map<Integer, Category> categoryMap;

    private Map<Integer, MapInfo> mapCache = new HashMap<Integer, MapInfo>();

    private Map<String, MapInfo> mapCacheLocal = new HashMap<String, MapInfo>();

    private List<MapInfo> mapList = new ArrayList<MapInfo>();

    private List<MapInfo> mapListLocal = new ArrayList<MapInfo>();

    private URL localMapContextUrl;

    private NetworkConnection serverConnection;

    /**
	 * Listeners that want to know when mapLists or Maps are loaded.
	 */
    private List<MapListener> mapListeners = new ArrayList<MapListener>();

    /**
	 * The last map requested from the server.
	 */
    private int lastRequestedMap = -1;

    public MapLoaderClient(NetworkConnection serverConnection) {
        this.serverConnection = serverConnection;
    }

    /**
	 * Loads the given map from the jar file.
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
                    levelInfo.getPreReq().add(-parseInt);
                }
            }
            if (split.length > 4) {
                int parseInt = Integer.parseInt(split[4]);
                if (parseInt > 0) {
                    levelInfo.setNextMap(-parseInt);
                }
            }
            levelInfo.getInfo().put("fileName", fileName);
            levelInfo.setCategory(cat);
            levelInfo.setMapId(-id);
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
	 * Loads the index of local map files.
	 * @param fileUrl the url of the index file for the local maps
	 * @param contextUrl the context to which the index url is relative.
	 */
    public void loadlocalIndex(String fileUrl, URL contextUrl) {
        URL indexUrl;
        List<String> levelNames;
        try {
            indexUrl = new URL(contextUrl, fileUrl);
            localMapContextUrl = indexUrl;
            levelNames = getLevelIndex(indexUrl);
            for (String levelName : levelNames) {
                tryLoadLocalFile(levelName, indexUrl);
            }
        } catch (MalformedURLException ex) {
            Logger.getLogger(MapLoaderClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        fireMapListChanged(mapListLocal);
    }

    /**
	 * Create a mapobject for a custom map.
	 * @param mapContent the map definition.
	 */
    public MapInfo parseCustomMap(String mapContent) {
        MapInfo mapInfo = mapCache.get(0);
        if (mapInfo == null) {
            mapInfo = new MapInfo();
            mapInfo.setMapId(0);
            mapListLocal.add(mapInfo);
            mapCache.put(mapInfo.getMapId(), mapInfo);
        }
        mapInfo.setContent(mapContent);
        if (mapInfo.getPlayers() == 0) {
            mapInfo.setPlayers(1);
        }
        return mapInfo;
    }

    /**
	 * Parses a map-index send by the gameserver.
	 * @param mml the message containing the maplist
	 */
    public void parseRemoteIndex(MessageMapList mml) {
        categoryMap = mml.getCategoriesById();
        Map<Integer, MapInfo> maps = mml.getMapCache();
        mapCache.clear();
        mapList.clear();
        for (Entry<Integer, MapInfo> entry : maps.entrySet()) {
            Integer id = entry.getKey();
            MapInfo cached = mapCache.get(id);
            MapInfo info = (MapInfo) entry.getValue();
            if (cached != null && cached.getContent() != null && cached.getLastChange() == info.getLastChange()) {
                info.setContent(cached.getContent());
            }
            mapCache.put(id, info);
            mapList.add(info);
        }
        System.out.println("ClientMapLoader:parseRemoteIndex: Got " + maps.size() + " maps from server.");
        fireMapListChanged(mapList);
    }

    /**
	 * Parses a map send by the game server.
	 * @param mm the message containing the map.
	 */
    public void parseRemoteMap(MessageMapData mm) {
        MapInfo info = mapCache.get(mm.getMapId());
        if (info != null) {
            System.out.println("ClientMapLoader::parseMap: Received map (" + mm.getMapId() + ") from server.");
            info.setContent(mm.getMapData());
            fireMapChanged(info);
        } else {
            System.out.println("ClientMapLoader::parseMap: Received map (" + mm.getMapId() + ") that is not in the mapCache.");
        }
    }

    @Override
    public MapInfo getMap(int mapId) {
        MapInfo info = mapCache.get(mapId);
        if (info != null && info.getContent() == null) {
            if (info.getInfo().get("fileName") == null) {
                if (mapId != lastRequestedMap) {
                    lastRequestedMap = mapId;
                    System.out.println("MapLoaderClient::getMap:requesting map from server " + mapId);
                    serverConnection.sendMessage(new MessageFetch(FetchType.map.name(), mapId));
                }
            } else {
                try {
                    System.out.println("MapLoaderClient::getMap:loading map from file " + info.getInfo().get("fileName"));
                    BufferedReader bufferedreader;
                    URL fetchUrl = new URL(localMapContextUrl, info.getInfo().get("fileName"));
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
                    fireMapChanged(info);
                } catch (IOException _ex) {
                    System.err.println("MapLoaderClient::getMap:: Can't read from " + info.getInfo().get("fileName"));
                }
            }
        }
        return info;
    }

    @Override
    public List<MapInfo> getMapList() {
        return mapList;
    }

    public Category getCategory(Integer catId) {
        if (categoryMap != null) {
            Category cat = categoryMap.get(catId);
            if (cat != null) {
                return cat;
            }
        }
        return new Category(catId, "Category " + catId);
    }

    private void fireMapChanged(MapInfo info) {
        for (MapListener l : mapListeners) {
            l.mapLoaded(info);
        }
    }

    private void fireMapListChanged(List<MapInfo> list) {
        for (MapListener l : mapListeners) {
            l.mapIndexLoaded(list);
        }
    }

    @Override
    public void addMapListener(MapListener l) {
        this.mapListeners.add(l);
    }

    public void removeMapListener(MapListener l) {
        mapListeners.remove(l);
    }

    public static List<String> getLevelIndex(URL fetchUrl) {
        List<String> levelNames = new ArrayList<String>();
        BufferedReader bufferedreader;
        try {
            URLConnection urlconnection = fetchUrl.openConnection();
            urlconnection.setConnectTimeout(30000);
            if (urlconnection.getContentEncoding() != null) {
                bufferedreader = new BufferedReader(new InputStreamReader(urlconnection.getInputStream(), urlconnection.getContentEncoding()));
            } else {
                bufferedreader = new BufferedReader(new InputStreamReader(urlconnection.getInputStream(), "utf-8"));
            }
        } catch (IOException _ex) {
            System.err.println("HexTD::readFile:: Can't read from " + fetchUrl);
            return levelNames;
        }
        String sLine1;
        try {
            while ((sLine1 = bufferedreader.readLine()) != null) {
                if (sLine1.trim().length() != 0) {
                    levelNames.add(sLine1);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MapLoaderClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        return levelNames;
    }
}
