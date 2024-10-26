import java.awt.Color;
import java.awt.Graphics;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.rsbot.event.listeners.PaintListener;
import org.rsbot.script.Script;
import org.rsbot.script.ScriptManifest;
import org.rsbot.script.methods.Equipment;
import org.rsbot.script.methods.Magic;
import org.rsbot.script.methods.Skills;
import org.rsbot.script.util.Timer;
import org.rsbot.script.wrappers.RSArea;
import org.rsbot.script.wrappers.RSTile;

/**
 * Arteymis' Law Rune Rusher main class.
 * @author Arteymis
 * @version 0.08
 */
@ScriptManifest(authors = "Arteymis", name = "Arteymis' Law Rune Rusher", version = 0.08, description = "A powerful Law rune crafter.")
public final class ArteymisLawRuneRusher extends Script implements PaintListener {

    /**
     * This method does a checkup of all initial parameters.
     * @return true to move to loop() or false to stop the process.
     */
    @Override
    public boolean onStart() {
        log(Color.BLUE, "Pre-tests are started.");
        if (!game.isLoggedIn()) {
            log(Color.RED, "Please, make sure your account is logged in before starting this script!");
            return false;
        }
        checkForItemsPrices();
        RunecraftingLevel = skills.getCurrentLevel(Skills.RUNECRAFTING);
        java.util.Timer t = new java.util.Timer();
        t.schedule(new java.util.TimerTask() {

            @Override
            public void run() {
                TimeRunning++;
            }
        }, 1000l);
        return RunecraftingLevel >= 54 && checkIfLocationIsMobilisingArmiesBank() && checkEquipmentForLawTiara() && checkEquipmentForExplorersRing() && checkEquipmentForForbiddenEquipments() && checkInventoryForLawTalisman() && checkInventoryForForbiddenEquipments() && checkInventoryForRunes() && checkBankForPureEssences();
    }

    /**
     * Checks for items prices. The method is a thread itself so it does not
     * interrupt the main process.
     */
    private void checkForItemsPrices() {
        new Thread() {

            @Override
            public void run() {
                AirRunePrice = grandExchange.lookup(AIR_RUNE_ID).getGuidePrice();
                log(Color.BLUE, "Air rune price : " + AirRunePrice + " gp.");
                WaterRunePrice = grandExchange.lookup(WATER_RUNE_ID).getGuidePrice();
                log(Color.BLUE, "Water rune price : " + WaterRunePrice + " gp.");
                PureEssencePrice = grandExchange.lookup(PURE_ESSENCE_ID).getGuidePrice();
                log(Color.BLUE, "Pure essence price : " + PureEssencePrice + " gp.");
                LawRunePrice = grandExchange.lookup(LAW_RUNE_ID).getGuidePrice();
                log(Color.BLUE, "Law rune price : " + LawRunePrice + " gp.");
            }
        }.start();
    }

    /**
     * This method contains the main process of Arteymis' Law Rune Rusher 
     * script. This loop() method is made out of an enums' switch. Every rounds,
     * it calls the method contained in possibilities variable that have been
     * determined from the last round or the initial parameters. Method it's
     * calling must return 0, 1 or -1 and must affect a new value to
     * possibilities, so the loop() method can be able to decide the next action
     * it's taking. If no actions are decided, it will simply launch the default
     * case, which will try to generate the next step. Otherwise, it is also 
     * possible to call the method generateNextStep(), which will generate a new 
     * step, considering the parameters.
     * @return -1 to stop, 0 to restart and x > 0 to wait x seconds before 
     * restarting the process.
     */
    @Override
    public int loop() {
        switch(this.NextStep) {
            case CHECK_IF_LOCATION_IS_MOBILISING_ARMIES:
                this.Status = possibilities.CHECK_IF_LOCATION_IS_MOBILISING_ARMIES;
                this.NextStep = this.checkIfLocationIsMobilisingArmiesBank() ? possibilities.GENERATE_NEXT_STEP : possibilities.EXIT;
                return 0;
            case CHECK_INVENTORY_FOR_LAW_TALISMAN:
                this.Status = possibilities.CHECK_INVENTORY_FOR_LAW_TALISMAN;
                this.NextStep = this.checkInventoryForLawTalisman() ? possibilities.GENERATE_NEXT_STEP : possibilities.EXIT;
                return 0;
            case CHECK_INVENTORY_FOR_FORBIDDEN_EQUIPMENTS:
                this.Status = possibilities.CHECK_INVENTORY_FOR_FORBIDDEN_EQUIPMENTS;
                this.NextStep = this.checkInventoryForForbiddenEquipments() ? possibilities.GENERATE_NEXT_STEP : possibilities.EXIT;
                return 0;
            case CHECK_INVENTORY_FOR_RUNES:
                this.Status = possibilities.CHECK_INVENTORY_FOR_RUNES;
                this.NextStep = this.checkInventoryForRunes() ? possibilities.GENERATE_NEXT_STEP : possibilities.EXIT;
                return 0;
            case CHECK_EQUIPMENT_FOR_LAW_TIARA:
                this.Status = possibilities.CHECK_EQUIPMENT_FOR_LAW_TIARA;
                this.NextStep = this.checkEquipmentForLawTiara() ? possibilities.GENERATE_NEXT_STEP : possibilities.EXIT;
                return 0;
            case CHECK_EQUIPMENT_FOR_EXPLORERS_RING_3_OR_4:
                this.Status = possibilities.CHECK_EQUIPMENT_FOR_EXPLORERS_RING_3_OR_4;
                this.NextStep = this.checkEquipmentForExplorersRing() ? possibilities.GENERATE_NEXT_STEP : possibilities.EXIT;
                return 0;
            case CHECK_EQUIPMENT_FOR_FORBIDDEN_EQUIPMENTS:
                this.Status = possibilities.CHECK_EQUIPMENT_FOR_FORBIDDEN_EQUIPMENTS;
                this.NextStep = this.checkEquipmentForForbiddenEquipments() ? possibilities.GENERATE_NEXT_STEP : possibilities.EXIT;
                return 0;
            case CHECK_BANK_FOR_PURE_ESSENCES:
                this.Status = possibilities.CHECK_BANK_FOR_PURE_ESSENCES;
                this.NextStep = this.checkBankForPureEssences() ? possibilities.GENERATE_NEXT_STEP : possibilities.EXIT;
                return 0;
            case WALK_FROM_CABBAGE_TELEPORT_TO_PORT_SARIM_DOCKS:
                this.Status = possibilities.WALK_FROM_CABBAGE_TELEPORT_TO_PORT_SARIM_DOCKS;
                return this.walkFromCabbageTeleportToPortSarimDocks();
            case USE_DOCKS_DEPOSIT_BOX:
                this.Status = possibilities.USE_DOCKS_DEPOSIT_BOX;
                return this.useDocksDepositBox();
            case TAKE_BOAT_TO_ENTRANA:
                this.Status = possibilities.TAKE_BOAT_TO_ENTRANA;
                return this.takeBoatToEntrana();
            case WALK_FROM_ENTRANA_DOCKS_TO_LAW_ALTAIR:
                this.Status = possibilities.WALK_FROM_ENTRANA_DOCKS_TO_LAW_ALTAIR;
                return this.walkFromEntranaDocksToAltair();
            case CRAFT_PURE_ESSENCE:
                this.Status = possibilities.CRAFT_PURE_ESSENCE;
                return this.craftPureEssence();
            case TELEPORT_AND_WALK_TO_MOBILISING_ARMIES_BANK:
                this.Status = possibilities.TELEPORT_AND_WALK_TO_MOBILISING_ARMIES_BANK;
                return this.teleportAndWalkToMobilisingArmiesBank();
            case USE_MOBILISING_ARMIES_BANK:
                this.Status = possibilities.USE_MOBILISING_ARMIES_BANK;
                return this.useMobilisingArmiesBank();
            case USE_CABBAGE_TELEPORT:
                this.Status = possibilities.USE_CABBAGE_TELEPORT;
                return this.useCabbageTeleport();
            case GENERATE_NEXT_STEP:
                this.Status = possibilities.GENERATE_NEXT_STEP;
                return this.generateNextStep();
            case EXIT:
                this.Status = possibilities.EXIT;
                this.log(Color.BLUE, "Thanks for using Arteymis' Law Rune Rusher!");
                return -1;
            default:
                this.log(Color.RED, "Case not taken in charge, please contact the developers of this script!");
                this.NextStep = possibilities.EXIT;
                return 0;
        }
    }

    private long TimeRunning = 0l;

    private int MinimumEnergyToRun = 40, RunecraftingLevel, AirRunePrice = -1, WaterRunePrice = -1, LawRunePrice = -1, PureEssencePrice = -1, ExplorersRing3State = -1, ExplorersRing4State = -1;

    private boolean LawTalismanState = false, LawTiaraState = false;

    private boolean[] Pouches = { false, false, false, false };

    private long[] RunesAmounts = new long[3];

    private possibilities NextStep = possibilities.USE_MOBILISING_ARMIES_BANK, Status = possibilities.START;

    private final int AIR_RUNE_ID = 556, WATER_RUNE_ID = 555, LAW_RUNE_ID = 563, PURE_ESSENCE_ID = 7936, EXPLORERS_RING_3_ID = 13562, EXPLORERS_RING_4_ID = 13563, LAW_TIARA_ID = 5545, LAW_TALISMAN_ID = 1458, SMALL_POUCH_ID = 5509, MEDIUM_POUCH_ID = 5510, MEDIUM_BROKEN_POUCH_ID = 5511, LARGE_POUCH_ID = 5512, LARGE_BROKEN_POUCH_ID = 5513, GIANT_POUCH_ID = 5514, GIANT_BROKEN_POUCH_ID = 5515;

    private final RSArea MOBILISING_ARMIES_BANK = new RSArea(new RSTile(2400, 2840), new RSTile(2405, 2843)), PORT_SARIM_DEPOSIT_BOX = new RSArea(new RSTile(3045, 3234), new RSTile(3051, 3237)), LAW_RUNE_ALTAIR = new RSArea(new RSTile(2856, 3377), new RSTile(2860, 3382)), MOBILISING_ARMIES_ENTRANCE = new RSArea(new RSTile(2409, 2842), new RSTile(2417, 2850)), PORT_SARIM_DOCK_ENTRANCE = new RSArea(new RSTile(3040, 3244), new RSTile(3043, 3249)), LAW_RUNE_ALTAIR_CRAFTING = new RSArea(new RSTile(2463, 4828), new RSTile(2465, 4834)), CABBAGE_PORT_FENCE = new RSArea(new RSTile(3051, 3283), new RSTile(3054, 3284));

    private final RSTile WALK_FROM_CABBAGE_TELEPORT_TO_PORT_SARIM_BEGIN = new RSTile(3054, 3289, 0), WALK_FROM_CABBAGE_TELEPORT_TO_PORT_SARIM_FENCE = new RSTile(3053, 3284, 0), WALK_FROM_CABBAGE_TELEPORT_TO_PORT_SARIM_END = new RSTile(3047, 3236, 0), WALK_FROM_ENTRANA_DOCKS_TO_ALTAIR_BEGIN = new RSTile(2834, 3335, 0), WALK_FROM_ENTRANA_DOCKS_TO_ALTAIR_END = new RSTile(2858, 3379, 0), CRAFT_PURE_ESSENCE_BEGIN = new RSTile(2464, 4819, 0), CRAFT_PURE_ESSENCE_END = new RSTile(2464, 4830, 0), TELEPORT_AND_WALK_TO_MOBILISING_ARMIES_END = new RSTile(2403, 2841, 0), WALK_FROM_CABBAGE_TELEPORT_TO_PORT_SARIM_DOCK_ENTRANCE = new RSTile(3041, 3247, 0);

    private final int GANG_PLANK_ID = 2415, LAW_ALTAIR_ID = 2485, LAW_MONUMENT_ID = 2459;

    private final int CONTINUE_CLICKER_TIME_OUT = 120000, TELEPORT_AND_MAP_CHANGING_TIME_OUT = 60000;

    /**
     * Method called to refresh the paint.
     * @param grphcs Graphics to draw on.
     */
    public void onRepaint(Graphics grphcs) {
        grphcs.drawString("Time running : " + this.TimeRunning, 30, 100);
        grphcs.drawString("Status : " + this.Status, 30, 115);
        grphcs.drawString("Explorer's ring 3 state : " + this.ExplorersRing3State, 30, 130);
        grphcs.drawString("Explorer's ring 4 state : " + this.ExplorersRing4State, 30, 145);
        grphcs.drawString("Runecrafting level : " + this.RunecraftingLevel, 30, 160);
        grphcs.drawString("Law talisman state : " + this.LawTalismanState, 30, 175);
        grphcs.drawString("Law tiara state : " + this.LawTiaraState, 30, 190);
    }

    /**
     * Enumeration of all possibilities; used in the loop()'s switch.
     */
    private enum possibilities {

        CHECK_IF_LOCATION_IS_MOBILISING_ARMIES("Check if location is mobilising armies", possibilities.PRE_TEST), CHECK_INVENTORY_FOR_LAW_TALISMAN("Check inventory for Law talisman", possibilities.PRE_TEST), CHECK_INVENTORY_FOR_FORBIDDEN_EQUIPMENTS("Check inventory for forbidden equipments", possibilities.PRE_TEST), CHECK_INVENTORY_FOR_RUNES("Check inventory for runes", possibilities.PRE_TEST), CHECK_INVENTORY_FOR_POUCHES("Check inventory for pouches", possibilities.PRE_TEST), CHECK_EQUIPMENT_FOR_LAW_TIARA("Check inventory for Law tiara", possibilities.PRE_TEST), CHECK_EQUIPMENT_FOR_EXPLORERS_RING_3_OR_4("Check inventory for Explorer's ring 3 or 4", possibilities.PRE_TEST), CHECK_EQUIPMENT_FOR_FORBIDDEN_EQUIPMENTS("Check inventory for forbidden equipments", possibilities.PRE_TEST), CHECK_BANK_FOR_PURE_ESSENCES("Check bank for Pure essences", possibilities.PRE_TEST), CHECK_BANK_FOR_EXPLORERS_RING_3_OR_4("Check bank for Explorer's ring 3 or 4", possibilities.PRE_TEST), WALK_FROM_CABBAGE_TELEPORT_TO_PORT_SARIM_DOCKS("Walk from cabbage teleport to Port Sarim's docks", possibilities.MAIN_PROCESS), USE_DOCKS_DEPOSIT_BOX("Use dock's deposit box", possibilities.MAIN_PROCESS), TAKE_BOAT_TO_ENTRANA("Take boat to Entrana", possibilities.MAIN_PROCESS), WALK_FROM_ENTRANA_DOCKS_TO_LAW_ALTAIR("Walk from Entrana docks to Law Al", possibilities.MAIN_PROCESS), CRAFT_PURE_ESSENCE("Craft Pure essence", possibilities.MAIN_PROCESS), TELEPORT_AND_WALK_TO_MOBILISING_ARMIES_BANK("Teleporting and walking to Mobilising Armies' bank.", possibilities.MAIN_PROCESS), USE_MOBILISING_ARMIES_BANK("Using Mobilising Armies' bank.", possibilities.MAIN_PROCESS), USE_CABBAGE_TELEPORT("Using cabbage teleport.", possibilities.MAIN_PROCESS), LOOK_AROUND("Looking around.", possibilities.ANTI_BAN), CHECK_RUNECRAFTING_LEVEL("Checking runecrafting level.", possibilities.ANTI_BAN), TAKE_A_BREAK("Taking a break.", possibilities.ANTI_BAN), GENERATE_NEXT_STEP("Generating the next step.", possibilities.REDIRECTOR), START("Starting Arteymis' Law Rune Rusher.", possibilities.MANAGER), EXIT("Exit", possibilities.MANAGER);

        /**
         * Name of the possibility
         */
        final String Name;

        final int Type;

        static final int ANTI_BAN = 0, MANAGER = 1, REDIRECTOR = 2, MAIN_PROCESS = 3, PRE_TEST = 4;

        /**
         * Enum for possibilities.
         * @param aName Name of the possibility.
         */
        possibilities(String aName, int aType) {
            this.Name = aName;
            this.Type = aType;
        }

        @Override
        public String toString() {
            return this.Name;
        }
    }

    /**
     * Check if location is Mobilising Armies bank.
     * @return true if location is Mobilising Armies bank, false otherwise.
     */
    private boolean checkIfLocationIsMobilisingArmiesBank() {
        if (this.MOBILISING_ARMIES_BANK.contains(this.getMyPlayer().getLocation())) {
            return true;
        } else {
            this.log(Color.RED, "You have to be in Mobilising Armies bank to start this script!");
            return false;
        }
    }

    /**
     * Check inventory for a Law talisman.
     * @return true if a Law talisman is found, false otherwise.
     */
    private boolean checkInventoryForLawTalisman() {
        if (this.LawTiaraState) {
            this.log(Color.BLUE, "Law tiara will be used.");
            return true;
        } else if (inventory.contains(this.LAW_TALISMAN_ID)) {
            this.LawTalismanState = true;
            this.log(Color.BLUE, "Law talisman will be used.");
            return true;
        } else {
            this.LawTalismanState = false;
            this.log(Color.RED, "No Law talisman detected in your inventory!");
            return false;
        }
    }

    /**
     * Check inventory for runes (Air, Water and Pure essence).
     * @return true if everything seems operational.
     */
    private boolean checkInventoryForRunes() {
        try {
            RunesAmounts[0] = inventory.getItem(AIR_RUNE_ID).getStackSize();
            log(Color.BLUE, RunesAmounts[0] + " Air runes detected in your inventory.");
        } catch (Exception e) {
            RunesAmounts[0] = 0;
            log(Color.RED, "No Air runes detected in your inventory!");
            return false;
        }
        try {
            RunesAmounts[1] = inventory.getItem(WATER_RUNE_ID).getStackSize();
            log(Color.BLUE, RunesAmounts[1] + " Water runes detected in your inventory.");
        } catch (Exception e) {
            RunesAmounts[1] = 0;
            log(Color.RED, "No Water runes detected in your inventory!");
            return false;
        }
        try {
            this.RunesAmounts[2] = this.inventory.getCount(PURE_ESSENCE_ID);
        } catch (Exception e) {
            RunesAmounts[2] = 0;
            log(Color.RED, "No Pure essences detected in your inventory!");
        }
        long NumberOfRounds = RunesAmounts[0] <= RunesAmounts[1] ? RunesAmounts[0] : RunesAmounts[1];
        this.log(Color.BLUE, NumberOfRounds + " rounds could be acheived.");
        return true;
    }

    /**
     * Check bank for Pure essences.
     * @return true if Pure essences are found, false otherwise.
     */
    private boolean checkBankForPureEssences() {
        this.bank.open();
        try {
            this.RunesAmounts[2] += this.bank.getItem(PURE_ESSENCE_ID).getStackSize();
            this.log(Color.BLUE, this.RunesAmounts[2] + " Pure essences detected in your bank account!");
            return true;
        } catch (Exception e) {
            this.log(Color.RED, "No Pure essences detected in your bank account!");
            return false;
        }
    }

    /**
     * Check equipment tab for a Law tiara.
     * @return true if Law tiara found, false otherwise
     */
    private boolean checkEquipmentForLawTiara() {
        if (this.equipment.containsAll(this.LAW_TIARA_ID)) {
            this.LawTiaraState = true;
            this.log(Color.BLUE, "Law tiara detected on you.");
            return true;
        } else {
            this.log(Color.RED, "No Law tiara detected on you!");
            return false;
        }
    }

    /**
     * Check equipment tab for Explorer's ring 3 or 4.
     * @return true if Explorer's ring 3 or 4 found, false otherwise.
     */
    private boolean checkEquipmentForExplorersRing() {
        if (equipment.getItem(Equipment.RING).getID() == EXPLORERS_RING_3_ID) {
            ExplorersRing3State = 1;
            log(Color.BLUE, "Explorer's ring 3 detected on you.");
            return true;
        } else if (equipment.getItem(Equipment.RING).getID() == EXPLORERS_RING_4_ID) {
            ExplorersRing4State = 1;
            log(Color.BLUE, "Explorer's ring 4 detected on you.");
            return true;
        } else {
            log(Color.RED, "Neither Explorer's ring 3 or 4 were detected in your inventory!");
            return false;
        }
    }

    /**
     * Check equipment tab for any forbidden equipments.
     * @return true if everyhing seems correct, false otherwise.
     */
    private boolean checkEquipmentForForbiddenEquipments() {
        if (this.equipment.getItem(Equipment.AMMO).getID() != -1 || equipment.getItem(Equipment.BODY).getID() != -1 || equipment.getItem(Equipment.CAPE).getID() != -1 || equipment.getItem(Equipment.FEET).getID() != -1 || equipment.getItem(Equipment.HANDS).getID() != -1 || equipment.getItem(Equipment.LEGS).getID() != -1 || equipment.getItem(Equipment.NECK).getID() != -1 || equipment.getItem(Equipment.SHIELD).getID() != -1 || equipment.getItem(Equipment.WEAPON).getID() != -1) {
            log(Color.RED, "An invalid equipment is detected! Just keep your Law tiara and Explorer's ring.");
            return false;
        } else {
            return true;
        }
    }

    /**
     * Check the inventory for any forbidden equipments.
     * @return true if everything seems correct, false otherwise.
     */
    private boolean checkInventoryForForbiddenEquipments() {
        int InvalidEquipmentsCount;
        if ((InvalidEquipmentsCount = inventory.getCountExcept(AIR_RUNE_ID, WATER_RUNE_ID, LAW_RUNE_ID, PURE_ESSENCE_ID, SMALL_POUCH_ID, MEDIUM_POUCH_ID, LARGE_POUCH_ID, GIANT_POUCH_ID, LAW_TALISMAN_ID)) > 0) {
            log(Color.RED, InvalidEquipmentsCount + " invalid equipments is detected! Just keep your Law tiara and Explorer's ring.");
            return false;
        }
        return true;
    }

    /**
     * Optimized walker with RSArea functionality.
     * @param rstile RSTile you would like to reach.
     * @param rsarea RSArea around the RSTile you would like to reach.
     */
    private void optimizedWalker(RSTile rstile, RSArea rsarea) {
        while (!rsarea.contains(this.getMyPlayer().getLocation())) {
            walking.walkTo(rstile);
            camera.getCharacterAngle(getMyPlayer());
            if (walking.isRunEnabled()) {
                ArteymisLawRuneRusher.sleep(1000);
            } else {
                if (walking.getEnergy() >= MinimumEnergyToRun) {
                    walking.setRun(true);
                    ArteymisLawRuneRusher.sleep(1000);
                } else {
                    ArteymisLawRuneRusher.sleep(1500);
                }
            }
        }
    }

    /**
     * Walk from cabbage teleport to Port Sarim docks.
     * @return see loop() return.
     */
    private int walkFromCabbageTeleportToPortSarimDocks() {
        Timer CabbageTeleportTimeOut = new Timer(TELEPORT_AND_MAP_CHANGING_TIME_OUT);
        while (!getMyPlayer().getLocation().equals(WALK_FROM_CABBAGE_TELEPORT_TO_PORT_SARIM_BEGIN)) {
            if (CabbageTeleportTimeOut.isRunning()) {
                sleep(1000);
            } else {
                log(Color.RED, "Cabbage teleport has timed out!");
                NextStep = possibilities.GENERATE_NEXT_STEP;
                return 0;
            }
        }
        optimizedWalker(WALK_FROM_CABBAGE_TELEPORT_TO_PORT_SARIM_FENCE, CABBAGE_PORT_FENCE);
        if (objects.getNearest("Gate").interact("Open")) {
            sleep(500);
        }
        optimizedWalker(WALK_FROM_CABBAGE_TELEPORT_TO_PORT_SARIM_DOCK_ENTRANCE, PORT_SARIM_DOCK_ENTRANCE);
        optimizedWalker(WALK_FROM_CABBAGE_TELEPORT_TO_PORT_SARIM_END, PORT_SARIM_DEPOSIT_BOX);
        NextStep = possibilities.USE_DOCKS_DEPOSIT_BOX;
        return 0;
    }

    /**
     * Use the dock's deposit box.
     * @return see loop() return.
     */
    private int useDocksDepositBox() {
        inventory.getItem(PURE_ESSENCE_ID).interact("Drop");
        equipment.getItem(Equipment.RING).interact("Remove");
        bank.openDepositBox();
        if (ExplorersRing3State == 1) {
            ExplorersRing3State = bank.deposit(EXPLORERS_RING_3_ID, 1) ? 1 : 0;
        } else if (ExplorersRing4State == 1) {
            ExplorersRing4State = bank.deposit(EXPLORERS_RING_4_ID, 1) ? 1 : 0;
        } else {
            log(Color.RED, "Explorer's ring 3 or 4 is not anymore detected!");
            NextStep = possibilities.GENERATE_NEXT_STEP;
            return 0;
        }
        bank.close();
        groundItems.getNearest(PURE_ESSENCE_ID).interact("Take");
        sleep(500);
        NextStep = possibilities.TAKE_BOAT_TO_ENTRANA;
        return 0;
    }

    /**
     * Take boat to Entrana.
     * @return see loop() return.
     */
    private int takeBoatToEntrana() {
        this.npcs.getNearest("Monk of Entrana").interact("Take-boat");
        Timer OptimizedContinueTimeOut = new Timer(this.CONTINUE_CLICKER_TIME_OUT);
        boolean GangplankIsReached = false;
        while (!GangplankIsReached) {
            boolean isLastContinue = this.interfaces.getAllContaining("The ship arrives at Entrana.").length == 1;
            if (OptimizedContinueTimeOut.isRunning() && interfaces.canContinue() && isLastContinue) {
                this.objects.getNearest(this.GANG_PLANK_ID).doClick();
                GangplankIsReached = true;
            } else if (OptimizedContinueTimeOut.isRunning() && interfaces.canContinue() && !isLastContinue) {
                this.interfaces.clickContinue();
                OptimizedContinueTimeOut.reset();
            } else if (OptimizedContinueTimeOut.isRunning()) {
                sleep(100);
            } else {
                this.log(Color.RED, "Continue clicker timed out!");
                this.NextStep = possibilities.GENERATE_NEXT_STEP;
                return 0;
            }
        }
        this.NextStep = possibilities.WALK_FROM_ENTRANA_DOCKS_TO_LAW_ALTAIR;
        return 0;
    }

    /**
     * Walk from Entrana docks to Law rune Altair.
     * @return see loop() return.
     */
    private int walkFromEntranaDocksToAltair() {
        Timer GangPlankTimeOut = new Timer(this.TELEPORT_AND_MAP_CHANGING_TIME_OUT);
        while (!this.getMyPlayer().getLocation().equals(this.WALK_FROM_ENTRANA_DOCKS_TO_ALTAIR_BEGIN)) {
            if (!GangPlankTimeOut.isRunning()) {
                this.log(Color.RED, "Gang plank has timed out!");
                this.NextStep = possibilities.GENERATE_NEXT_STEP;
                return 0;
            }
        }
        this.optimizedWalker(this.WALK_FROM_ENTRANA_DOCKS_TO_ALTAIR_END, this.LAW_RUNE_ALTAIR);
        this.log(this.LAW_RUNE_ALTAIR.contains(this.getMyPlayer().getLocation()));
        this.objects.getNearest(this.LAW_MONUMENT_ID).doClick();
        this.NextStep = possibilities.CRAFT_PURE_ESSENCE;
        return 0;
    }

    /**
     * Craft Law runes from Pure essences at Law rune Altair.
     * @return see loop() return.
     */
    private int craftPureEssence() {
        Timer LawRuneAltairTimeOut = new Timer(this.TELEPORT_AND_MAP_CHANGING_TIME_OUT);
        while (!this.getMyPlayer().getLocation().equals(this.CRAFT_PURE_ESSENCE_BEGIN)) {
            if (!LawRuneAltairTimeOut.isRunning()) {
                this.log(Color.RED, "Law rune altair teleport has timed out!");
                this.NextStep = possibilities.GENERATE_NEXT_STEP;
                return 0;
            }
        }
        this.optimizedWalker(this.CRAFT_PURE_ESSENCE_END, this.LAW_RUNE_ALTAIR_CRAFTING);
        this.objects.getNearest(this.LAW_ALTAIR_ID).doClick();
        objects.getNearest(LAW_ALTAIR_ID).doClick();
        if (this.Pouches[0]) {
            inventory.getItem(SMALL_POUCH_ID).interact("Empty");
            objects.getNearest(LAW_ALTAIR_ID).doClick();
        }
        if (this.Pouches[1]) {
            inventory.getItem(MEDIUM_POUCH_ID).interact("Empty");
            objects.getNearest(LAW_ALTAIR_ID).doClick();
        }
        if (this.Pouches[2]) {
            inventory.getItem(LARGE_POUCH_ID).interact("Empty");
            objects.getNearest(LAW_ALTAIR_ID).doClick();
        }
        if (this.Pouches[3]) {
            inventory.getItem(GIANT_POUCH_ID).interact("Empty");
            objects.getNearest(LAW_ALTAIR_ID).doClick();
        }
        this.NextStep = possibilities.TELEPORT_AND_WALK_TO_MOBILISING_ARMIES_BANK;
        return 0;
    }

    /**
     * Teleport and walk to Mobilising Armies.
     * @return see loop() return.
     */
    private int teleportAndWalkToMobilisingArmiesBank() {
        int n = 0;
        while (!magic.castSpell(Magic.SPELL_MOBILISING_ARMIES_TELEPORT) && n < 5) {
            sleep(1000);
            n++;
        }
        if (n == 5) {
            log(Color.RED, "Mobilising Armies teleport has timed out!1");
            NextStep = possibilities.GENERATE_NEXT_STEP;
            return 0;
        } else {
            Timer MobilisingArmiesTeleportTimeOut = new Timer(5000);
            while (!MOBILISING_ARMIES_ENTRANCE.contains(getMyPlayer().getLocation())) {
                if (!MobilisingArmiesTeleportTimeOut.isRunning()) {
                    log(Color.RED, "Mobilising Armies teleport has timed out!");
                    NextStep = possibilities.GENERATE_NEXT_STEP;
                    return 0;
                }
            }
        }
        this.optimizedWalker(this.TELEPORT_AND_WALK_TO_MOBILISING_ARMIES_END, this.MOBILISING_ARMIES_BANK);
        this.NextStep = possibilities.USE_MOBILISING_ARMIES_BANK;
        return 0;
    }

    /**
     * Use Mobilising Armies bank.
     * @return see loop() return.
     */
    private int useMobilisingArmiesBank() {
        if (!bank.isOpen()) {
            try {
                bank.open();
            } catch (Exception e) {
                log(Color.RED, "Couldn't open the bank!");
                NextStep = possibilities.GENERATE_NEXT_STEP;
                return 0;
            }
        }
        this.bank.depositAllExcept(AIR_RUNE_ID, WATER_RUNE_ID, LAW_TALISMAN_ID, SMALL_POUCH_ID, MEDIUM_POUCH_ID, this.LARGE_POUCH_ID, this.GIANT_POUCH_ID, this.PURE_ESSENCE_ID);
        if (this.ExplorersRing3State == 0 | this.ExplorersRing4State == 0) {
            log("I'm trying to withdraw explo rings...");
            int n = 0;
            while (!(bank.withdraw(EXPLORERS_RING_3_ID, 1) | !bank.withdraw(EXPLORERS_RING_4_ID, 1)) && n < 5) {
                n++;
            }
            if (n == 5) {
                log(Color.RED, "Neither Explorer's ring 3 or 4 have been found!");
                NextStep = possibilities.GENERATE_NEXT_STEP;
                return 0;
            } else {
                sleep(3000);
                bank.getEquipmentItem(EXPLORERS_RING_3_ID).interact("Wear");
            }
        }
        if (this.Pouches[0]) {
            withdrawPureEssence();
            bank.getEquipmentItem(this.SMALL_POUCH_ID).interact(null);
        }
        if (this.Pouches[1]) {
            withdrawPureEssence();
            bank.getEquipmentItem(this.MEDIUM_POUCH_ID).interact(null);
        }
        if (this.Pouches[2]) {
            withdrawPureEssence();
            bank.getEquipmentItem(this.LARGE_POUCH_ID).interact(null);
        }
        if (this.Pouches[3]) {
            withdrawPureEssence();
            bank.getEquipmentItem(this.GIANT_POUCH_ID).interact(null);
        }
        this.NextStep = possibilities.USE_CABBAGE_TELEPORT;
        return 0;
    }

    private int withdrawPureEssence() {
        int n = 0;
        while (!bank.withdraw(PURE_ESSENCE_ID, 26) && n < 5) {
            n++;
        }
        if (n == 5) {
            log(Color.RED, "Couldn't retrieve Pure essence from your bank!");
            NextStep = possibilities.GENERATE_NEXT_STEP;
            return 0;
        }
        return 1;
    }

    /**
     * Use Explorer's ring cabbage-port option.
     * @return see loop() return.
     */
    private int useCabbageTeleport() {
        try {
            int n = 0;
            while (!equipment.getItem(Equipment.RING).interact("Cabbage-port") && n < 5) {
                n++;
            }
            if (n == 5) {
                log(Color.RED, "Cabbage teleport has timed out.");
                NextStep = possibilities.GENERATE_NEXT_STEP;
                return 0;
            } else {
                NextStep = possibilities.WALK_FROM_CABBAGE_TELEPORT_TO_PORT_SARIM_DOCKS;
            }
        } catch (Exception e) {
            log(Color.RED, "Neither Explorer's ring 3 or 4 are available!");
            NextStep = possibilities.GENERATE_NEXT_STEP;
        }
        return 0;
    }

    /**
     * Generate the next step to recuperate the path instead of terminate.
     * @return see loop() return.
     */
    private int generateNextStep() {
        NextStep = possibilities.EXIT;
        return 0;
    }

    /**
     * Obtain the latest version of this script.
     * @throws MalformedURLException if the URL entered is wrong.
     * @throws IOException if errors occur in the buffer.
     */
    private void checkForLatestVersion() {
        log(Color.BLUE, "Checking for latest version.");
        try {
            double LatestVersion = 0.0;
            URL url = new URL("http://www.powerbot.org/vb/showthread.php?t=723144");
            URLConnection urlc = url.openConnection();
            BufferedReader bf = new BufferedReader(new InputStreamReader(urlc.getInputStream()));
            String CurrentLine;
            while ((CurrentLine = bf.readLine()) != null) {
                if (CurrentLine.contains("<pre class=\"bbcode_code\"style=\"height:48px;\"><i>Current version")) {
                    for (String s : CurrentLine.split(" ")) {
                        try {
                            LatestVersion = Double.parseDouble(s);
                        } catch (NumberFormatException nfe) {
                        }
                    }
                }
            }
            double CurrentVersion = getClass().getAnnotation(ScriptManifest.class).version();
            String Message = LatestVersion < CurrentVersion ? ", you should update to the latest version!" : ", you have the latest version of this script.";
            log(LatestVersion < CurrentVersion ? Color.RED : Color.BLUE, "Latest version available : " + LatestVersion + Message);
        } catch (IOException ioe) {
            log(Color.RED, "Couldn't retreive latest version due to a connection issue!");
        } catch (NumberFormatException nfe) {
            log(Color.RED, "Couldn't reveice latest version; no version were available on PowerBot website!.");
        } catch (Exception e) {
            log(Color.RED, "Couldn't retreive latest version due to an unknown reason!");
        }
    }
}
