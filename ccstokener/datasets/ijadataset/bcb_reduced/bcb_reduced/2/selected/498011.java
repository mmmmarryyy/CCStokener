package games.midhedava.server.entity.player;

import games.midhedava.common.Debug;
import games.midhedava.server.MidhedavaRPAction;
import games.midhedava.server.MidhedavaRPWorld;
import games.midhedava.server.MidhedavaRPZone;
import games.midhedava.server.actions.AdministrationAction;
import games.midhedava.server.entity.Outfit;
import games.midhedava.server.entity.creature.Pet;
import games.midhedava.server.entity.creature.Sheep;
import games.midhedava.server.entity.item.Item;
import games.midhedava.server.entity.item.StackableItem;
import games.midhedava.server.entity.slot.BankSlot;
import games.midhedava.server.entity.slot.Banks;
import games.midhedava.server.entity.slot.EntitySlot;
import games.midhedava.server.entity.slot.KeyedSlot;
import games.midhedava.server.entity.slot.PlayerSlot;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import marauroa.common.Configuration;
import marauroa.common.game.IRPZone;
import marauroa.common.game.RPClass;
import marauroa.common.game.RPObject;
import marauroa.common.game.RPSlot;
import org.apache.log4j.Logger;

/**
 * Handles the RPClass registration and updating old Player objects created by
 * an older version of Midhedava.
 */
class PlayerRPClass {

    private static Logger logger = Logger.getLogger(PlayerRPClass.class);

    /** list of super admins read from admins.list */
    private static List<String> adminNames = null;

    /** only log the first exception while reading welcome URL */
    private static boolean firstWelcomeException = true;

    /** these items should be bound */
    private static final List<String> itemsToBind = Arrays.asList("dungeon_silver_key", "lich_gold_key", "trophy_helmet");

    private static final List<String> itemNamesOld = Arrays.asList("flail_+2", "leather_armor_+1", "tibiere_armor_+1", "leather_cuirass_+1", "chain_armor_+1", "scale_armor_+1", "chain_armor_+3", "scale_armor_+2", "twoside_axe_+3", "elf_cloak_+2", "mace_+1", "mace_+2", "hammer_+3", "chain_helmet_+2", "golden_helmet_+3", "longbow_+1", "lion_shield_+1");

    private static final List<String> itemNamesNew = Arrays.asList("morning_star", "leather_scale_armor", "pauldroned_leather_cuirass", "enhanced_chainmail", "iron_scale_armor", "golden_chainmail", "pauldroned_iron_cuirass", "golden_twoside_axe", "blue_elf_cloak", "enhanced_mace", "golden_mace", "golden_hammer", "aventail", "horned_golden_helmet", "composite_bow", "enhanced_lion_shield");

    /**
	 * Generates the RPClass and specifies slots and attributes.
	 */
    static void generateRPClass() {
        RPClass player = new RPClass("player");
        player.isA("rpentity");
        player.add("text", RPClass.LONG_STRING, RPClass.VOLATILE);
        player.add("private_text", RPClass.LONG_STRING, (byte) (RPClass.PRIVATE | RPClass.VOLATILE));
        player.add("poisoned", RPClass.SHORT, RPClass.VOLATILE);
        player.add("eating", RPClass.SHORT, RPClass.VOLATILE);
        player.add("dead", RPClass.FLAG, RPClass.PRIVATE);
        player.add("outfit", RPClass.INT);
        player.add("outfit_org", RPClass.INT);
        player.add("race", RPClass.STRING);
        player.add("home", RPClass.STRING);
        player.add("sex", RPClass.STRING);
        player.add("away", RPClass.LONG_STRING, RPClass.VOLATILE);
        player.add("admin", RPClass.FLAG);
        player.add("adminlevel", RPClass.INT);
        player.add("invisible", RPClass.FLAG, RPClass.HIDDEN);
        player.add("ghostmode", RPClass.FLAG);
        player.add("teleclickmode", RPClass.FLAG, RPClass.HIDDEN);
        player.add("release", RPClass.STRING, RPClass.HIDDEN);
        player.add("age", RPClass.INT);
        player.addRPSlot("#flock", 1, RPClass.HIDDEN);
        player.add("sheep", RPClass.INT);
        player.addRPSlot("#pets", 1, RPClass.HIDDEN);
        player.add("pet", RPClass.INT);
        player.add("cat", RPClass.INT);
        player.addRPSlot("bank", 30, RPClass.HIDDEN);
        player.addRPSlot("bank_ados", 30, RPClass.HIDDEN);
        player.addRPSlot("zaras_chest_ados", 30, RPClass.HIDDEN);
        player.addRPSlot("bank_fado", 30, RPClass.HIDDEN);
        player.addRPSlot("bank_nalwor", 30, RPClass.HIDDEN);
        player.addRPSlot("!kills", 1, RPClass.HIDDEN);
        player.addRPSlot("!buddy", 1, RPClass.PRIVATE);
        player.addRPSlot("!ignore", 1, RPClass.HIDDEN);
        player.add("online", RPClass.LONG_STRING, (byte) (RPClass.PRIVATE | RPClass.VOLATILE));
        player.add("offline", RPClass.LONG_STRING, (byte) (RPClass.PRIVATE | RPClass.VOLATILE));
        player.addRPSlot("!quests", 1, RPClass.HIDDEN);
        player.addRPSlot("!tutorial", 1, RPClass.HIDDEN);
        player.add("karma", RPClass.FLOAT, RPClass.PRIVATE);
        player.addRPSlot("skills", 1, RPClass.HIDDEN);
        player.addRPSlot("!skills", 1, (byte) (RPClass.HIDDEN | RPClass.VOLATILE));
        player.addRPSlot("!visited", 1, RPClass.HIDDEN);
        player.addRPSlot("spells", 9, RPClass.PRIVATE);
        player.add("guild", RPClass.STRING);
        player.add("fullghostmode", RPClass.INT);
        player.add("features", RPClass.LONG_STRING, RPClass.PRIVATE);
    }

    /**
	 * Updates a player RPObject from an old version of Midhedava.
	 * 
	 * @param object
	 *            RPObject representing a player
	 */
    static void updatePlayerRPObject(RPObject object) {
        String[] slotsNormal = { "bag", "rhand", "lhand", "head", "armor", "legs", "feet", "finger", "cloak", "bank", "bank_ados", "zaras_chest_ados", "bank_fado", "bank_nalwor", "spells", "keyring" };
        String[] slotsSpecial = { "!quests", "!kills", "!buddy", "!ignore", "!visited", "skills", "!tutorial" };
        if (!object.has("base_hp")) {
            object.put("base_hp", "100");
            object.put("hp", "100");
        }
        if (!object.has("outfit")) {
            object.put("outfit", new Outfit().getCode());
        }
        for (String slotName : slotsNormal) {
            if (!object.hasSlot(slotName)) {
                object.addSlot(new EntitySlot(slotName));
            }
        }
        for (String slotName : slotsSpecial) {
            if (!object.hasSlot(slotName)) {
                object.addSlot(new KeyedSlot(slotName));
            }
            RPSlot slot = object.getSlot(slotName);
            if (slot.size() == 0) {
                RPObject singleObject = new RPObject();
                slot.assignValidID(singleObject);
                slot.add(singleObject);
            }
        }
        if (!object.has("atk_xp")) {
            object.put("atk_xp", "0");
            object.put("def_xp", "0");
        }
        if (object.has("devel")) {
            object.remove("devel");
        }
        if (!object.has("release")) {
            object.put("release", "0.00");
            object.put("atk", "10");
            object.put("def", "10");
        }
        if (!object.has("age")) {
            object.put("age", "0");
        }
        if (!object.has("karma")) {
            object.put("karma", 10);
        }
        if (!object.has("mana")) {
            object.put("mana", 0);
        }
        if (!object.has("base_mana")) {
            object.put("base_mana", 0);
        }
        if (object.has("!skills")) {
            object.remove("!skills");
        }
        if (!object.has("race")) object.put("race", "dacian");
        if (!object.has("home")) object.put("home", "First road");
        if (!object.has("sex")) object.put("sex", "male");
    }

    /**
	 * reads the admins from admins.list
	 * 
	 * @param player
	 *            Player to check for super admin status.
	 */
    static void readAdminsFromFile(Player player) {
        if (adminNames == null) {
            adminNames = new LinkedList<String>();
            String adminFilename = "data/conf/admins.list";
            try {
                InputStream is = player.getClass().getClassLoader().getResourceAsStream(adminFilename);
                if (is == null) {
                    logger.info("data/conf/admins.list does not exist.");
                } else {
                    BufferedReader in = new BufferedReader(new InputStreamReader(is));
                    try {
                        String line;
                        while ((line = in.readLine()) != null) {
                            adminNames.add(line);
                        }
                    } catch (Exception e) {
                        logger.error("Error loading admin names from: " + adminFilename, e);
                    }
                    in.close();
                }
            } catch (Exception e) {
                logger.error("Error loading admin names from: " + adminFilename, e);
            }
        }
        boolean isAdmin = adminNames.contains(player.getName());
        if (isAdmin) {
            player.put("adminlevel", AdministrationAction.REQUIRED_ADMIN_LEVEL_FOR_SUPER);
        } else {
            if (!player.has("adminlevel")) {
                player.put("adminlevel", "0");
            }
        }
    }

    public static final String DEFAULT_ENTRY_ZONE = "forest clearing";

    /**
	 * Places the player (and his/her sheep if there is one) into the world on
	 * login
	 * 
	 * @param object
	 *            RPObject representing the player
	 * @param player
	 *            Player-object
	 */
    static void placePlayerIntoWorldOnLogin(RPObject object, Player player) {
        MidhedavaRPWorld world = MidhedavaRPWorld.get();
        boolean firstVisit = false;
        try {
            if (!object.has("zoneid") || !object.has("x") || !object.has("y")) {
                firstVisit = true;
            }
            boolean newReleaseHappened = !object.get("release").equals(Debug.VERSION);
            if (newReleaseHappened) {
                firstVisit = true;
                player.put("release", Debug.VERSION);
            }
            IRPZone tempZone = MidhedavaRPWorld.get().getZone(object.get("zoneid"));
            if (tempZone == null) {
                firstVisit = true;
            }
            if (firstVisit) {
                if (tempZone == null) {
                    player.put("zoneid", player.getHome());
                }
            }
            world.add(player);
        } catch (Exception e) {
            logger.warn("cannot place player at its last position. reseting to the default entry zone", e);
            firstVisit = true;
            player.put("zoneid", player.getHome());
            player.notifyWorldAboutChanges();
        }
        MidhedavaRPAction.transferContent(player);
        MidhedavaRPZone zone = player.getZone();
        if (firstVisit) {
            zone.setEntryPoint(23, 5);
            zone.placeObjectAtEntryPoint(player);
        }
        int x = player.getX();
        int y = player.getY();
        try {
            if (player.hasSheep()) {
                logger.debug("Player has a sheep");
                Sheep sheep = player.getPlayerSheepManager().retrieveSheep();
                sheep.put("zoneid", player.get("zoneid"));
                if (!sheep.has("base_hp")) {
                    sheep.put("base_hp", "10");
                    sheep.put("hp", "10");
                }
                world.add(sheep);
                player.setSheep(sheep);
                MidhedavaRPAction.placeat(zone, sheep, x, y);
            }
        } catch (Exception e) {
            logger.error("Pre 1.00 Marauroa sheep bug. (player = " + player.getName() + ")", e);
            if (player.has("sheep")) {
                player.remove("sheep");
            }
            if (player.hasSlot("#flock")) {
                player.removeSlot("#flock");
            }
        }
        if (player.hasPet()) {
            logger.debug("Player has a cat");
            Pet pet = player.getPlayerPetManager().retrievePet();
            pet.put("zoneid", player.get("zoneid"));
            if (!pet.has("base_hp")) {
                pet.put("base_hp", "200");
                pet.put("hp", "200");
            }
            world.add(pet);
            player.setPet(pet);
            MidhedavaRPAction.placeat(zone, pet, x, y);
        }
        MidhedavaRPAction.placeat(zone, player, x, y);
    }

    /**
	 * Loads the items into the slots of the player on login.
	 * 
	 * @param player
	 *            Player
	 */
    static void loadItemsIntoSlots(Player player) {
        String[] slotsItems = { "bag", "rhand", "lhand", "head", "armor", "legs", "feet", "finger", "cloak", "zaras_chest_ados", "keyring" };
        try {
            for (String slotName : slotsItems) {
                RPSlot slot = player.getSlot(slotName);
                RPSlot newSlot = new PlayerSlot(slotName);
                loadSlotContent(player, slot, newSlot);
            }
            for (Banks bank : Banks.values()) {
                RPSlot slot = player.getSlot(bank.getSlotName());
                RPSlot newSlot = new BankSlot(bank);
                loadSlotContent(player, slot, newSlot);
            }
        } catch (RuntimeException e) {
            logger.error("cannot create player", e);
        }
    }

    /**
	 * Loads the items into the slots of the player on login.
	 * 
	 * @param player
	 *            Player
	 * @param slot
	 *            original slot
	 * @param newSlot
	 *            new Midhedava specific slot
	 */
    private static void loadSlotContent(Player player, RPSlot slot, RPSlot newSlot) {
        MidhedavaRPWorld world = MidhedavaRPWorld.get();
        List<RPObject> objects = new LinkedList<RPObject>();
        for (RPObject objectInSlot : slot) {
            objects.add(objectInSlot);
        }
        slot.clear();
        player.removeSlot(slot.getName());
        player.addSlot(newSlot);
        for (RPObject item : objects) {
            try {
                if (item.get("type").equals("item")) {
                    int oldatk = 0;
                    if (item.has("atk")) {
                        oldatk = item.getInt("atk");
                    }
                    String name = item.get("name");
                    if (itemNamesOld.indexOf(name) > -1) {
                        name = itemNamesNew.get(itemNamesOld.indexOf(name));
                    }
                    Item entity = world.getRuleManager().getEntityManager().getItem(name);
                    if (entity.has("atk")) {
                        entity.put("atk", oldatk);
                    }
                    Player.removeTransientItems(player, false);
                    if (entity == null) {
                        int quantity = 1;
                        if (item.has("quantity")) {
                            quantity = item.getInt("quantity");
                        }
                        logger.warn("Cannot restore " + quantity + " " + item.get("name") + " on login of " + player.get("name") + " because this item" + " was removed from items.xml");
                        continue;
                    }
                    entity.setID(item.getID());
                    if (item.has("persistent") && (item.getInt("persistent") == 1)) {
                        entity.fill(item);
                    }
                    if (entity instanceof StackableItem) {
                        int quantity = 1;
                        if (item.has("quantity")) {
                            quantity = item.getInt("quantity");
                        } else {
                            logger.warn("Adding quantity=1 to " + item + ". Most likly cause is that this item was not stackable in the past");
                        }
                        ((StackableItem) entity).setQuantity(quantity);
                        if (quantity <= 0) {
                            logger.warn("Ignoring item " + item.get("name") + " on login of player " + player.get("name") + " because this item has an invalid quantity: " + quantity);
                            continue;
                        }
                    }
                    String[] individualAttributes = { "infostring", "description", "bound" };
                    for (String attribute : individualAttributes) {
                        if (item.has(attribute)) {
                            entity.put(attribute, item.get(attribute));
                        }
                    }
                    boundOldItemsToPlayer(player, entity);
                    newSlot.add(entity);
                }
            } catch (Exception e) {
                logger.error("Error adding " + item + " to player slot" + slot, e);
            }
        }
    }

    /**
	 * binds special items to the player.
	 * 
	 * @param player
	 *            Player
	 * @param item
	 *            Item
	 */
    private static void boundOldItemsToPlayer(Player player, Item item) {
        if (item.has("bound")) {
            return;
        }
        if (itemsToBind.contains(item.getName())) {
            item.put("bound", player.getName());
        }
    }

    /**
	 * send a welcome message to the player which can be configured in
	 * marauroa.ini file as "server_welcome". If the value is an http:// adress,
	 * the first line of that adress is read and used as the message
	 * 
	 * @param player
	 *            Player
	 */
    static void welcome(Player player) {
        String msg = "This is release " + Debug.VERSION + " of Midhedava. Please report problems, suggestions and bugs at www.midhedava.org. Note: remember to keep your password completely secret, never tell it to another friend, player, or even admin.";
        try {
            Configuration config = Configuration.getConfiguration();
            if (config.has("server_welcome)")) {
                msg = config.get("server_welcome");
                if (msg.startsWith("http://")) {
                    URL url = new URL(msg);
                    HttpURLConnection.setFollowRedirects(false);
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    msg = br.readLine();
                    br.close();
                    connection.disconnect();
                }
            }
        } catch (Exception e) {
            if (PlayerRPClass.firstWelcomeException) {
                logger.warn("Can't read server_welcome from marauroa.ini", e);
                PlayerRPClass.firstWelcomeException = false;
            }
        }
        if (msg != null) {
            player.sendPrivateText(msg);
        }
    }
}
