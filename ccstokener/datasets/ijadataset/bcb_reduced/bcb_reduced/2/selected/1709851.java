package tw.bennu.feeler.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import tw.bennu.feeler.apps.packet.core.EchoEntityStatusChangeAppsPacket;
import tw.bennu.feeler.apps.packet.core.EntityEchoAppsPacket;
import tw.bennu.feeler.apps.packet.core.EntityHelloAppsPacket;
import tw.bennu.feeler.apps.packet.core.EntityStatusChangeAppsPacket;
import tw.bennu.feeler.apps.packet.lcanorus.FightAppsPacket;
import tw.bennu.feeler.apps.packet.lcanorus.HoldAppsPacket;
import tw.bennu.feeler.apps.service.AppsService;
import tw.bennu.feeler.apps.service.IAppsPacket;
import tw.bennu.feeler.entity.service.EntityService;
import tw.bennu.feeler.log.service.LogService;
import tw.bennu.feeler.net.DefaultNetPacket;
import tw.bennu.feeler.net.service.INetPacket;
import tw.bennu.feeler.net.service.NetService;
import tw.bennu.feeler.preference.service.PreferenceService;
import tw.bennu.feeler.util.json.JSONArray;
import tw.bennu.feeler.util.json.JSONException;
import tw.bennu.feeler.util.json.JSONObject;
import tw.bennu.feeler.util.service.UtilService;
import tw.bennu.feeler.vote.apps.packet.VoteTopicAppsPacket;

public class UtilServiceImpl implements UtilService {

    private LogService logger = null;

    private NetService netServ = null;

    private PreferenceService preferenceServ = null;

    private EntityService entityServ = null;

    private AppsService appsServ = null;

    @Override
    public List<String> listUUIDTag(UUID boxUUID) {
        List<String> retTagList = new ArrayList<String>();
        try {
            URL url = new URL("http://feeler.bennu.tw/feeler-webapp/mvc/uuid/tags/listtag?box=" + boxUUID.toString().trim());
            BufferedReader urlBr = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
            String retJsonString = urlBr.readLine();
            urlBr.close();
            if (retJsonString != null && !retJsonString.isEmpty()) {
                JSONArray jsonArr = new JSONArray(retJsonString);
                for (int i = 0; i < jsonArr.length(); i++) {
                    retTagList.add(jsonArr.getString(i));
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return retTagList;
    }

    @Override
    public void modUUIDTag(UUID boxUUID, String type, String tag) {
        try {
            URL url = new URL("http://feeler.bennu.tw/feeler-webapp/mvc/uuid/tags/modtag?box=" + boxUUID.toString().trim() + "&type=" + type.toString() + "&tag=" + URLEncoder.encode(tag.trim(), "utf-8"));
            BufferedReader urlBr = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
            urlBr.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<UUID> searchUUIDWithTag(String tag) {
        List<UUID> retUUIDList = new ArrayList<UUID>();
        try {
            URL url = new URL("http://feeler.bennu.tw/feeler-webapp/mvc/uuid/tags/listuuid?tag=" + tag.trim());
            BufferedReader urlBr = new BufferedReader(new InputStreamReader(url.openStream(), "utf-8"));
            String retJsonString = urlBr.readLine();
            urlBr.close();
            if (retJsonString != null && !retJsonString.isEmpty()) {
                JSONArray jsonArr = new JSONArray(retJsonString);
                for (int i = 0; i < jsonArr.length(); i++) {
                    UUID uuid = UUID.fromString(jsonArr.getString(i));
                    retUUIDList.add(uuid);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return retUUIDList;
    }

    @Override
    public IAppsPacket recvAppsPacket(UUID targetDestEntityUUID) {
        INetPacket netPack = this.getNetServ().recv();
        if (netPack != null) {
            UUID srcEntityUUID = UUID.fromString(netPack.getSourceEntityUUID());
            UUID destEntityUUID = UUID.fromString(netPack.getDestinationEntityUUID());
            String appPacketJsonString = netPack.getAppPacketJsonString();
            if (!destEntityUUID.equals(targetDestEntityUUID)) {
                this.getNetServ().returnBackNetPacket(netPack);
                return null;
            }
            this.getEntityServ().addItemEntityToBox(targetDestEntityUUID, srcEntityUUID);
            this.getEntityServ().raiseItemEntityGloryPoint(srcEntityUUID);
            IAppsPacket appsPack = null;
            try {
                JSONObject jsonObj = new JSONObject(appPacketJsonString);
                String appsPackClazz = jsonObj.getString("appsPacketImplClassName");
                if (appsPackClazz.equals(FightAppsPacket.class.getName())) {
                    this.getEntityServ().setItemEntityStatus(targetDestEntityUUID, srcEntityUUID, EntityService.ENTITY_STATUS_ONLINE);
                    FightAppsPacket fightPacket = new FightAppsPacket(jsonObj.getString("situation"));
                    appsPack = fightPacket;
                } else if (appsPackClazz.equals(HoldAppsPacket.class.getName())) {
                    this.getEntityServ().setItemEntityStatus(targetDestEntityUUID, srcEntityUUID, EntityService.ENTITY_STATUS_OFFLINE);
                    HoldAppsPacket holdPacket = new HoldAppsPacket(jsonObj.getString("situation"));
                    appsPack = holdPacket;
                } else if (appsPackClazz.equals(EntityStatusChangeAppsPacket.class.getName())) {
                    EntityStatusChangeAppsPacket eStChPacket = new EntityStatusChangeAppsPacket(jsonObj.getInt("statusCode"));
                    this.getEntityServ().setItemEntityStatus(targetDestEntityUUID, srcEntityUUID, eStChPacket.getStatusCode());
                    this.sendAppsPacket(targetDestEntityUUID, srcEntityUUID, new EchoEntityStatusChangeAppsPacket(EntityService.ENTITY_STATUS_ONLINE));
                    appsPack = eStChPacket;
                } else if (appsPackClazz.equals(EchoEntityStatusChangeAppsPacket.class.getName())) {
                    EchoEntityStatusChangeAppsPacket echoEStChPacket = new EchoEntityStatusChangeAppsPacket(jsonObj.getInt("statusCode"));
                    this.getEntityServ().setItemEntityStatus(targetDestEntityUUID, srcEntityUUID, echoEStChPacket.getStatusCode());
                } else if (appsPackClazz.equals(VoteTopicAppsPacket.class.getName())) {
                    JSONArray jsonOptsArr = jsonObj.getJSONArray("options");
                    Set<String> opts = new HashSet<String>();
                    for (int i = 0; i < jsonOptsArr.length(); i++) {
                        opts.add(jsonOptsArr.getString(i));
                    }
                    VoteTopicAppsPacket voteTopicPacket = new VoteTopicAppsPacket(jsonObj.getString("ownerUUID"), jsonObj.getString("topic"), opts, jsonObj.getInt("viewedTimes"), jsonObj.getInt("doneValue"), jsonObj.getInt("dumpOffset"));
                    appsPack = voteTopicPacket;
                } else if (appsPackClazz.equals(EntityHelloAppsPacket.class.getName())) {
                    this.sendAppsPacket(targetDestEntityUUID, srcEntityUUID, new EntityEchoAppsPacket());
                } else if (appsPackClazz.equals(EntityEchoAppsPacket.class.getName())) {
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            this.getAppsServ().appendAppsPacket(appsPack);
            return appsPack;
        } else {
            return null;
        }
    }

    @Override
    public void sendAppsPacket(UUID srcEntityUUID, UUID destEnitityUUID, IAppsPacket appsPack) {
        INetPacket netPacket = new DefaultNetPacket();
        netPacket.setAppPacketJsonString(new JSONObject(appsPack).toString());
        netPacket.setDestinationEntityUUID(destEnitityUUID.toString());
        netPacket.setSourceEntityUUID(srcEntityUUID.toString());
        this.getNetServ().send(netPacket);
    }

    @Override
    public void addEmptyVoteTopicAppsPacket(String ownerUUID, String topic, int doneValue, int dumpOffset) {
        Set<String> opts = new HashSet<String>();
        VoteTopicAppsPacket voteTopicPacket = new VoteTopicAppsPacket(ownerUUID, topic, opts, 0, doneValue, dumpOffset);
        this.getAppsServ().appendAppsPacket(voteTopicPacket);
    }

    @Override
    public void updateLocalUUIDList(List<UUID> localUUIDList) {
        this.getNetServ().setLocalEntityUUIDList(localUUIDList);
    }

    @Override
    public void updateItemUUIDList(List<UUID> itemUUIDList) {
        this.getNetServ().setItemEntityUUIDList(itemUUIDList);
    }

    public LogService getLogger() {
        return logger;
    }

    public void setLogger(LogService logger) {
        this.logger = logger;
    }

    public NetService getNetServ() {
        return netServ;
    }

    public void setNetServ(NetService netServ) {
        this.netServ = netServ;
    }

    public PreferenceService getPreferenceServ() {
        return preferenceServ;
    }

    public void setPreferenceServ(PreferenceService preferenceServ) {
        this.preferenceServ = preferenceServ;
    }

    public EntityService getEntityServ() {
        return entityServ;
    }

    public void setEntityServ(EntityService entityServ) {
        this.entityServ = entityServ;
    }

    public AppsService getAppsServ() {
        return appsServ;
    }

    public void setAppsServ(AppsService appsServ) {
        this.appsServ = appsServ;
    }
}
