package data;

import static data.Const.DefenseOffense.DEFENSE;
import static data.Const.Stats.NIN;
import static data.Const.Stats.TAI;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.TreeMap;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

public abstract class Const {

    public static final long SECOND = 1000;

    public static final long MINUTE = SECOND * 60;

    public static final String FIREFOX_HOME = "/home/stitch/.mozilla/firefox/";

    public static final String CHROME_HOME = "/home/stitch/.config/chromium/";

    public static final String COOKIES_FILE = "Cookies";

    public static final String MPOGR_COOKIE = "__utma=246882648.844737922.1254294382.1282545953.1284974416.155; __utmz=246882648.1284974416.155.139.utmccn=(referral)|utmcsr=theninja-rpg.com|utmcct=/|utmcmd=referral; PHPSESSID=cu0a927n8hasjsfhno09u6rdr5; __utmb=246882648; __utmc=246882648";

    public static final String CROMAN_COOKIE = "__utmz=255561037.1285764743.1.1.utmccn=(direct)|utmcsr=(direct)|utmcmd=(none); __utmc=255561037; PHPSESSID=4d45546889b5c8e016584324eb1186f1; __utmb=255561037; __utma=255561037.1215968425.1285764743.1285764743.1285766519.2";

    public static final String SCHYAKLS_COOKIE = "__utma=255561037.583484603.1237370048.1276599816.1276611967.597; __utmz=255561037.1270034261.481.1.utmccn=(direct)|utmcsr=(direct)|utmcmd=(none); PHPSESSID=f4055319c80bafb7a83db5c2ef2ba95a; __utmc=255561037; __utmb=255561037";

    public static final String CALE_COOKIE = "__utma=255561037.438335550.1254812747.1276783800.1276846610.552; __utmz=255561037.1270622283.405.1.utmccn=(direct)|utmcsr=(direct)|utmcmd=(none); PHPSESSID=278bba4b8e67dde32858a3f55e1bcba0; __utmc=255561037; __utmb=255561037";

    public static final String ACID_BATH_COOKIE = "__utma=255561037.1450872812.1267714068.1276599649.1276612021.155; __utmz=255561037.1267714068.1.1.utmccn=(direct)|utmcsr=(direct)|utmcmd=(none); PHPSESSID=20176ca664e9a93c72c337b399bed052; __utmc=255561037; __utmb=255561037";

    public static final String CROMAN = Croman.NAME;

    public static final String SCHYAKLS = Schyakls.NAME;

    public static final String ACID_BATH = AcidBath.NAME;

    public static final String CALE = Cale.NAME;

    public final Location glacierLocation = new Location(8, 15);

    public final Location shroudLocation = new Location(1, 14);

    protected Properties prop = new Properties() {

        private static final long serialVersionUID = 1L;

        @SuppressWarnings("unchecked")
        public synchronized Enumeration keys() {
            Enumeration keysEnum = super.keys();
            Vector keyList = new Vector();
            while (keysEnum.hasMoreElements()) {
                keyList.add(keysEnum.nextElement());
            }
            Collections.sort(keyList);
            return keyList.elements();
        }
    };

    public long NEXT_OCCUPATION_TIME = 0;

    protected static int SLEEP_LOOP = 231;

    protected int sleepBetweenLoop = SLEEP_LOOP;

    protected long hospitalSleep = MINUTE * 2;

    protected int numberSeriousCrimes = 1000;

    protected int missionTimer;

    protected boolean saveToFile = false;

    protected Logger log;

    protected String html;

    protected String name;

    protected String pass;

    protected String id = "39";

    protected String cookieBackup;

    protected String cookie;

    protected String logString;

    protected boolean asleep = false;

    protected int timeout;

    protected boolean camp;

    protected boolean checkIfFirstAttack = false;

    protected BattleAction battleAction;

    protected BattleAction battleActionSecond;

    protected BattleAction battleActionThird;

    protected BattleAction battleActionForCommander;

    protected BattleAction battleActionForPlayer;

    protected BattleAction battleActionForSparring;

    protected BattleAction battleActionForMissions;

    protected String opponent;

    protected String oponentSelf;

    protected String battleId;

    protected String battleLife;

    protected String battleChakra;

    protected String battleStamina;

    protected String enemyLife;

    protected String attackerDamage;

    protected String attackedDamage;

    protected String attackerName;

    protected String attackedName;

    protected String attackerVillage;

    protected String attackedVillage;

    protected String attackerLevel;

    protected String attackedLevel;

    protected String enemyHealth;

    protected boolean exhausted;

    protected String enemyName;

    protected String enemyVillage;

    protected String enemyLevel;

    protected long promotionTime;

    protected long gainStatTime;

    protected int logoutTime;

    protected List<String> bonusLinks;

    protected int logoutTimeMin = 180;

    protected int regenTimer;

    protected long money;

    protected long banked;

    protected String village;

    protected int moneyMin = 26000;

    protected double regenRate = 90;

    protected double healthMinRatio = 3;

    protected double healthLevel;

    protected double healthMax;

    protected double healthMin;

    protected double healthMinInBattle = 150000;

    protected double healthMinInSBattle = 70000;

    protected double chakraLevel;

    protected double chakraMin = 3500;

    protected double chakraMax;

    protected double staminaLevel;

    protected double staminaMax;

    protected double staminaMin = 3500;

    protected double taiDef;

    protected double ninDef;

    protected double genDef;

    protected double weapDef;

    protected double taiOff;

    protected double ninOff;

    protected double genOff;

    protected double weapOff;

    protected double strength;

    protected double intelligence;

    protected double speed;

    protected double willpower;

    protected boolean inBattle = false;

    protected Mission currentMission = Mission.BattleArena;

    protected boolean unreadPMs = false;

    protected boolean isAIEnemy;

    protected boolean isGolemEnemy;

    protected boolean checkSenbonAmount = false;

    protected boolean checkPillAmount = false;

    protected long sleep = MINUTE;

    protected long battleSleep = MINUTE / 6;

    protected String generals_train_step;

    protected String train_step;

    protected long sleep_between_mission;

    protected int attackCount = 0;

    protected DefenseOffense evenChakraType = DEFENSE;

    protected Stats evenChakra = Stats.NIN;

    protected DefenseOffense evenStaminaType = DEFENSE;

    protected Stats evenStamina = TAI;

    protected boolean evenStaminaGeneral = false;

    protected Generals evenStaminaGeneralType = Generals.SPEED;

    /**
     * 1 is drain with Stats (WEAP, NIN, TAI or GEN )<br /> 
     * 2 is drain with Stats for chakra and Generals for stamina (NIN or GEN and Speed or Strength)<br />
     * 3 is drain with Int or willpower <br />
     * 4 is drain only one Stat NIN, TAI or GEN<br />
     */
    protected int drainType = 1;

    protected DefenseOffense drainChakraType = DefenseOffense.DEFENSE;

    protected Stats drainChakra = NIN;

    protected DefenseOffense drainStaminaType = DEFENSE;

    protected Stats drainStamina = TAI;

    protected Generals drainStaminaGeneralType = Generals.SPEED;

    protected boolean checkOccupation = false;

    protected boolean allRamen = true;

    protected boolean doSurgery = false;

    public static boolean forceSurgery = false;

    protected int checkJutsuLevel;

    protected int minChakraLevelToDrain = 100;

    protected boolean logoutAfterDrain = true;

    protected int surgeryTime = 0;

    protected boolean doMissionsAtFirst = true;

    protected int minutesToTrain = 20;

    protected boolean trainShometsu = false;

    protected boolean doSmissions = true;

    protected boolean doMissionsBeforeLoop = false;

    protected Mission mission = Mission.BattleArena;

    protected Mission specMission = Mission.MissionS;

    protected int missionASleep = 31;

    protected int missionSSleep = 182;

    protected Location location = new Location(0, 0);

    protected Location sparDestination = new Location(5, 16);

    protected Location homeDestination = glacierLocation;

    protected boolean sparing = false;

    protected boolean sparrer = false;

    protected String sparingOpponent;

    protected Map<String, Opponent> opponents = new TreeMap<String, Opponent>(Collections.reverseOrder());

    protected boolean playing = false;

    protected boolean drainAtFirst = true;

    protected boolean isHunting = false;

    protected boolean isCriming = false;

    protected boolean shouldHunt = false;

    protected int huntingCount = 0;

    protected int huntingTimer = 15;

    public static enum Mission {

        BattleArena(35), TornArena(35), MissionA(62), MissionB(54), MissionC(40), MissionS(77), CrimeA(65), CrimeB(56), CrimeC(40);

        private int code;

        private Mission(int code) {
            this.code = code;
        }

        public static Mission get(String name) {
            for (Mission enumeration : Mission.values()) {
                if (name.equals(enumeration.name() + "")) {
                    return enumeration;
                }
            }
            return BattleArena;
        }

        public int code() {
            return code;
        }
    }

    public static enum DefenseOffense {

        DEFENSE("Defensive"), OFFENSE("Offensive");

        private String value;

        private DefenseOffense(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static DefenseOffense get(String name) {
            for (DefenseOffense enumeration : DefenseOffense.values()) {
                if (name.equals(enumeration.name() + "")) {
                    return enumeration;
                }
            }
            return DEFENSE;
        }
    }

    public static enum Generals {

        STRENGTH("strength"), INTELLIGENCE("intelligence"), WILLPOWER("willpower"), SPEED("speed");

        private String value;

        private Generals(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static Generals get(String name) {
            for (Generals enumeration : Generals.values()) {
                if (name.equals(enumeration.name() + "")) {
                    return enumeration;
                }
            }
            return WILLPOWER;
        }
    }

    public static enum Stats {

        WEAP("weap"), GEN("gen"), TAI("tai"), NIN("nin");

        private String value;

        private Stats(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }

        public static Stats get(String name) {
            for (Stats enumeration : Stats.values()) {
                if (name.equals(enumeration.name() + "")) {
                    return enumeration;
                }
            }
            return WEAP;
        }
    }

    public static enum Direction {

        EAST("east"), WEST("west"), SOUTH("south"), NORTH("north");

        private String value;

        private Direction(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    public class Location {

        public int xAxis;

        public int yAxis;

        public Location(int x, int y) {
            xAxis = x;
            yAxis = y;
        }
    }

    public class Opponent {

        public String name = "N/A";

        public String code = "N/A";

        public double health = 0;

        public double healthPercent = 100;

        public double healthLeft = 0;

        public double healthTaken = 0;

        public void countHealth() {
            if (healthPercent == 100) return;
            health = (healthTaken * 100) / (100 - healthPercent);
            healthLeft = health - healthTaken;
        }
    }

    public static final String REGEX_UNREAD_PM = "You have unread (PM)";

    public static final String REGEX_COOKIES = "theninja-rpg\\.com__utm(.*)/";

    public static final String REGEX_FIREFOX_PHPSESSID = "theninja-rpg.com\",\"value\":\"([^\"]*?)\",\"path\":\"/\",\"name\":\"PHPSESSID\"";

    public static final String REGEX_FIREFOX_UTMC = "theninja-rpg.com\",\"value\":\"([^\"]*?)\",\"path\":\"/\",\"name\":\"__utmc\"";

    public static final String REGEX_FIREFOX_COOKIES = "(__utm.*?).theninja-rpg.com/";

    public static final String REGEX_REGEN_RATE = "Regeneration rate: (.*?) (<|\\()";

    public static final String REGEX_ASLEEP = "Regeneration rate: .*? (asleep)";

    public static final String REGEX_REGEN_TIMER = "regentimer\\\",(\\d*),";

    public static final String REGEX_LOGOUT_TIME = "logouttime\\\",(\\d*),";

    public static final String REGEX_HOSPITAL_TIME = "hospitaltimer\\\",(\\d*),";

    public static final String REGEX_MISSION_TIME = "crimetimer\\\",(\\d*),";

    public static final String REGEX_PROMOTION_TIME = "promotiontime\\\",(\\d*),";

    public static final String REGEX_GAIN_STAT_TIME = "gaintime\\\",(\\d*),";

    public static final String REGEX_GET_PROMOTED = ">(Get promoted)</a>";

    public static final String REGEX_CLAIM_STATS = ">(Claim gain)</a>";

    public static final String REGEX_EXPERIENCE = "Experience: (.*?) </";

    public static final String REGEX_EXP_NEEDED = "Exp needed: (.*?) </";

    public static final String REGEX_CURR_HEALTH = "curheafield.*?>(.*?)</";

    public static final String REGEX_MAX_HEALTH = "maxheafield.*?>(.*?)</";

    public static final String REGEX_CURR_CHAKRA = "curchafield.*?>(.*?)</";

    public static final String REGEX_MAX_CHAKRA = "maxchafield.*?>(.*?)</";

    public static final String REGEX_CURR_STAMINA = "curstafield.*?>(.*?)</";

    public static final String REGEX_MAX_STAMINA = "maxstafield.*?>(.*?)</";

    public static final String REGEX_MONEY = "Money: (.*?) </";

    public static final String REGEX_BANKED = "Banked: (.*?) </";

    public static final String REGEX_VILLAGE = "Village: (.*?) </";

    public static final String REGEX_AI_ENEMY = "<i>(AI)</i>";

    public static final String REGEX_GOLEM_ENEMY = "(Golem)";

    public static final String REGEX_CAMP = ">(Bank)</a><";

    public static final String REGEX_TAI_DEFENSE = "Taijutsu defense: (\\d+\\.?\\d?)";

    public static final String REGEX_NIN_DEFENSE = "Ninjutsu defense: (\\d+\\.?\\d?)";

    public static final String REGEX_GEN_DEFENSE = "Genjutsu defense: (\\d+\\.?\\d?)";

    public static final String REGEX_WAEP_DEFENSE = "Weapon defense: (\\d+\\.?\\d?)";

    public static final String REGEX_TAI_STRENGTH = "Taijutsu strength: (\\d+\\.?\\d?)";

    public static final String REGEX_NIN_STRENGTH = "Ninjutsu strength: (\\d+\\.?\\d?)";

    public static final String REGEX_GEN_STRENGTH = "Genjutsu strength: (\\d+\\.?\\d?)";

    public static final String REGEX_WAEP_STRENGTH = "Weapon strength: (\\d+\\.?\\d?)";

    public static final String REGEX_STRENGTH = ">Strength: (\\d+\\.?\\d?)";

    public static final String REGEX_INTELLIGENCE = "Intelligence: (\\d+\\.?\\d?)";

    public static final String REGEX_SPEED = "Speed: (\\d+\\.?\\d?)";

    public static final String REGEX_WILLPOWER = "Willpower: (\\d+\\.?\\d?)";

    public static final String REGEX_BATTLE_ID = "battle_id\".*?value=\"(\\d+)";

    public static final String REGEX_SENBON_AMOUNT = "Senbon <i>\\((\\d+)\\)<";

    public static final String REGEX_PILL_AMOUNT = "Special Jounin Soldier Pill <i>\\((\\d+)\\)<";

    public static final String REGEX_SURGERY_COST = "You will gain approximately .*? HP for (\\d+) ryo.";

    public static final String REGEX_SURGERY_TIME = "surgerytimer\\\",(\\d*),";

    public static final String REGEX_BATTLE_SUMMERY = "Battle summary:</td>.*?<td(.*?)</td>";

    public static final String REGEX_OPPONENT = "opponent\".*?value=\"(\\d+).*?>(.*?)<br";

    public static final String REGEX_BATTLE_LIFE = "images/avatars.*?life_bar.*?width=\"(.*?).\"";

    public static final String REGEX_BATTLE_STAMINA = "sta_bar.*?width=\"(.*?).\"";

    public static final String REGEX_BATTLE_CHAKRA = "cha_bar.*?width=\"(.*?).\"";

    public static final String REGEX_ENEMY_LIFE = "images/avatars.*?life_bar.*?life_bar.*?width=\"(.*?).\"";

    public static final String REGEX_BATTLE_ARENA = "Will you enter and fight.*?<a href=\"(.*)\">.*?images/antibot/(7fj4k9sw|kiuhbs73|hd83j6ht|lokjrewh|jguhyewl).*?Torn Battle Arena";

    public static final String REGEX_TORN_ARENA = "Torn Battle Arena.*?<a href=\"(.*)\">.*?images/antibot/(7fj4k9sw|kiuhbs73|hd83j6ht|lokjrewh|jguhyewl)";

    public static final String REGEX_B_MISSION = "B-ranked Mission.*?a href=\"(.*?)\">Take the mission";

    public static final String REGEX_A_MISSION = "A-ranked Mission.*?a href=\"(.*?)\">Take the mission";

    public static final String REGEX_S_MISSION = "S-ranked Mission.*?a href=\"(.*?)\">Take the mission";

    public static final String REGEX_B_CRIME = "B-ranked crime.*?a href=\"(.*?)\">Do the crime";

    public static final String REGEX_A_CRIME = "A-ranked crime.*?a href=\"(.*?)\">Do the crime";

    public static final String REGEX_C_CRIME = "C-ranked crime.*?a href=\"(.*?)\">Do the crime";

    public static final String REGEX_C_MISSION = "C-ranked Mission.*?a href=\"(.*?)\">Take the mission";

    public static final String REGEX_BATTLE_OUTCOME = "Outcome:(.*?)Continue";

    public static final String REGEX_BATTLE_OUTCOME_DAMAGE = "<font color=\\\".*?>.*?</font>";

    public static final String REGEX_BATTLE_DAMAGE = "( \\d+\\.*\\d* ).*?<i>(.*?)</i>";

    public static final String REGEX_ATTACKER_NAME = "id=13.*?page=profile&name=(.*?)\">.*?id=13.*?page=profile&name=.*?\">";

    public static final String REGEX_ATTACKED_NAME = "id=13.*?page=profile&name=.*?\">.*?id=13.*?page=profile&name=(.*?)\">";

    public static final String REGEX_ATTACKER_VILLAGE = "id=13.*?page=profile&name=.*?\">.*?</a></b><br />.*?, (.*?)<br />.*?id=13.*?page=profile&name=.*?\">";

    public static final String REGEX_ATTACKED_VILLAGE = "id=13.*?page=profile&name=.*?\">.*?id=13.*?page=profile&name=.*?\">.*?</a></b><br />.*?, (.*?)<br />";

    public static final String REGEX_ATTACKER_LEVEL = "id=13.*?page=profile&name=.*?\">.*?</a></b><br />(.*?), .*?<br />.*?id=13.*?page=profile&name=.*?\">";

    public static final String REGEX_ATTACKED_LEVEL = "id=13.*?page=profile&name=.*?\">.*?id=13.*?page=profile&name=.*?\">.*?</a></b><br />(.*?), .*?<br />";

    public static final String REGEX_BATTLE_HEALTH = "name=(.*?)\".*?life_bar.jpg.*?width=\"(.*?)%";

    public static final String REGEX_EXHAUSTED = "too (exhausted) from.*?- - - - - - - - - - - -";

    public static final String REGEX_ATTACKED_ALREADY = "(- - - - - - - - - - -)";

    public static final String REGEX_BONUS = "<a href=\"([^>]*?)\" target=\"_blank\">";

    public static final String REGEX_BONUS_PERCENT = "Your chakra bar will be (.*?)% restored";

    public static final String REGEX_JUTSU_LEVEL = "Level:</td>.*?<td>(.*?)</td>";

    public static final String REGEX_JUTSU_EXPERIENCE = "Experience:</td>.*?<td>(.*?)</td>";

    public static final String REGEX_LOCATION = "Your location: <b>(-?\\d\\d?)\\.(-?\\d\\d?)";

    public static final String REGEX_BOUNTY_LOCATION = "Marked Target Location: <b>(-?\\d\\d?)\\.(-?\\d\\d?)";

    public static final Pattern unreadPMPattern = Pattern.compile(REGEX_UNREAD_PM, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern cookiesPattern = Pattern.compile(REGEX_COOKIES, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern firefoxCookiesPattern = Pattern.compile(REGEX_FIREFOX_COOKIES, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern firefoxPhpsessidPattern = Pattern.compile(REGEX_FIREFOX_PHPSESSID, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern firefoxUtmcdPattern = Pattern.compile(REGEX_FIREFOX_UTMC, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern logoutTimePattern = Pattern.compile(REGEX_LOGOUT_TIME, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern hospitalTimePattern = Pattern.compile(REGEX_HOSPITAL_TIME, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern missionTimePattern = Pattern.compile(REGEX_MISSION_TIME, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern promotionTimePattern = Pattern.compile(REGEX_PROMOTION_TIME, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern gainStatTimePattern = Pattern.compile(REGEX_GAIN_STAT_TIME, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern getPromotedPattern = Pattern.compile(REGEX_GET_PROMOTED, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern claimStatsPattern = Pattern.compile(REGEX_CLAIM_STATS, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern regenRatePattern = Pattern.compile(REGEX_REGEN_RATE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern regenTimerPattern = Pattern.compile(REGEX_REGEN_TIMER, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern currHealthPattern = Pattern.compile(REGEX_CURR_HEALTH, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern maxHealthPattern = Pattern.compile(REGEX_MAX_HEALTH, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern currChakraPattern = Pattern.compile(REGEX_CURR_CHAKRA, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern maxChakraPattern = Pattern.compile(REGEX_MAX_CHAKRA, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern currStaminaPattern = Pattern.compile(REGEX_CURR_STAMINA, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern maxStaminaPattern = Pattern.compile(REGEX_MAX_STAMINA, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern expNeededPattern = Pattern.compile(REGEX_EXP_NEEDED, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern expPattern = Pattern.compile(REGEX_EXPERIENCE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern moneyPattern = Pattern.compile(REGEX_MONEY, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern bankedPattern = Pattern.compile(REGEX_BANKED, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern villagePattern = Pattern.compile(REGEX_VILLAGE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern campPattern = Pattern.compile(REGEX_CAMP, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern aiEnemyPattern = Pattern.compile(REGEX_AI_ENEMY, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern golemEnemyPattern = Pattern.compile(REGEX_GOLEM_ENEMY, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern asleepPattern = Pattern.compile(REGEX_ASLEEP, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern taiDefPattern = Pattern.compile(REGEX_TAI_DEFENSE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern ninDefPattern = Pattern.compile(REGEX_NIN_DEFENSE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern genDefPattern = Pattern.compile(REGEX_GEN_DEFENSE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern weapDefPattern = Pattern.compile(REGEX_WAEP_DEFENSE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern taiOffPattern = Pattern.compile(REGEX_TAI_STRENGTH, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern ninOffPattern = Pattern.compile(REGEX_NIN_STRENGTH, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern genOffPattern = Pattern.compile(REGEX_GEN_STRENGTH, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern weapOffPattern = Pattern.compile(REGEX_WAEP_STRENGTH, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern strengthPattern = Pattern.compile(REGEX_STRENGTH, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern intelligencePattern = Pattern.compile(REGEX_INTELLIGENCE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern speedPattern = Pattern.compile(REGEX_SPEED, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern willpowerPattern = Pattern.compile(REGEX_WILLPOWER, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern senbonAmountPattern = Pattern.compile(REGEX_SENBON_AMOUNT, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern pillAmountPattern = Pattern.compile(REGEX_PILL_AMOUNT, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern tornArenaPattern = Pattern.compile(REGEX_TORN_ARENA, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern battleArenaPattern = Pattern.compile(REGEX_BATTLE_ARENA, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern missionBPattern = Pattern.compile(REGEX_B_MISSION, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern missionAPattern = Pattern.compile(REGEX_A_MISSION, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern missionSPattern = Pattern.compile(REGEX_S_MISSION, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern crimeBPattern = Pattern.compile(REGEX_B_CRIME, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern crimeAPattern = Pattern.compile(REGEX_A_CRIME, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern crimeCPattern = Pattern.compile(REGEX_C_CRIME, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern missionCPattern = Pattern.compile(REGEX_C_MISSION, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern opponentPattern = Pattern.compile(REGEX_OPPONENT, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern battleSummeryPattern = Pattern.compile(REGEX_BATTLE_SUMMERY, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern battleIdPattern = Pattern.compile(REGEX_BATTLE_ID, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern battleLifePattern = Pattern.compile(REGEX_BATTLE_LIFE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern battleChakraPattern = Pattern.compile(REGEX_BATTLE_CHAKRA, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern battleStaminaPattern = Pattern.compile(REGEX_BATTLE_STAMINA, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern enemyLifePattern = Pattern.compile(REGEX_ENEMY_LIFE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern outcomePattern = Pattern.compile(REGEX_BATTLE_OUTCOME, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern damagePattern = Pattern.compile(REGEX_BATTLE_OUTCOME_DAMAGE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern battleDamagePattern = Pattern.compile(REGEX_BATTLE_DAMAGE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern attackerNamePattern = Pattern.compile(REGEX_ATTACKER_NAME, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern attackedNamePattern = Pattern.compile(REGEX_ATTACKED_NAME, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern attackerVillagePattern = Pattern.compile(REGEX_ATTACKER_VILLAGE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern attackedVillagePattern = Pattern.compile(REGEX_ATTACKED_VILLAGE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern attackerLevelPattern = Pattern.compile(REGEX_ATTACKER_LEVEL, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern attackedLevelPattern = Pattern.compile(REGEX_ATTACKED_LEVEL, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern battleHealthPattern = Pattern.compile(REGEX_BATTLE_HEALTH, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern exhaustedPattern = Pattern.compile(REGEX_EXHAUSTED, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern attackedAlreadyPattern = Pattern.compile(REGEX_ATTACKED_ALREADY, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern surgeryCostPattern = Pattern.compile(REGEX_SURGERY_COST, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern surgeryTimePattern = Pattern.compile(REGEX_SURGERY_TIME, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern bonusPattern = Pattern.compile(REGEX_BONUS, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern bonusPercentPattern = Pattern.compile(REGEX_BONUS_PERCENT, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern jutsuLevelPattern = Pattern.compile(REGEX_JUTSU_LEVEL, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern jutsuExperiencePattern = Pattern.compile(REGEX_JUTSU_EXPERIENCE, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern locationPattern = Pattern.compile(REGEX_LOCATION, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Pattern bountylocationPattern = Pattern.compile(REGEX_BOUNTY_LOCATION, Pattern.MULTILINE | Pattern.CASE_INSENSITIVE);

    public static final Map<Mission, Pattern> missionPatterns = new HashMap<Mission, Pattern>() {

        private static final long serialVersionUID = 1L;

        {
            put(Mission.MissionA, missionAPattern);
            put(Mission.MissionB, missionBPattern);
            put(Mission.MissionC, missionCPattern);
            put(Mission.MissionS, missionSPattern);
            put(Mission.CrimeA, crimeAPattern);
            put(Mission.CrimeB, crimeBPattern);
            put(Mission.CrimeC, crimeCPattern);
            put(Mission.BattleArena, battleArenaPattern);
            put(Mission.TornArena, tornArenaPattern);
        }
    };

    public static final Map<String, String> names = new HashMap<String, String>() {

        private static final long serialVersionUID = 1L;

        {
            put(CROMAN, "661405");
            put(SCHYAKLS, "668570");
            put(ACID_BATH, "837816");
            put(CALE, "682618");
        }
    };

    public Const() {
    }

    protected Const(String name) {
        log = initLogger(name);
        initProps(name);
    }

    private void initProps(String name) {
        String propFileName = "./prop/" + name + ".properties";
        try {
            File props = new File("./prop");
            if (!props.exists()) props.mkdir();
            new File(propFileName).createNewFile();
            prop.load(new FileInputStream(propFileName));
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    protected void readEditableProps() {
        initProps(name);
        moneyMin = Integer.parseInt(prop.getProperty("~moneyMin"));
        minutesToTrain = Integer.parseInt(prop.getProperty("~minutesToTrain"));
        healthMinInBattle = Double.parseDouble(prop.getProperty("~healthMinInBattle"));
        healthMinInSBattle = Double.parseDouble(prop.getProperty("~healthMinInSBattle"));
        chakraMin = Double.parseDouble(prop.getProperty("~chakraMin"));
        staminaMin = Double.parseDouble(prop.getProperty("~staminaMin"));
        logoutAfterDrain = Boolean.parseBoolean(prop.getProperty("~logoutAfterDrain"));
        trainShometsu = Boolean.parseBoolean(prop.getProperty("~trainShometsu"));
        doSmissions = Boolean.parseBoolean(prop.getProperty("~doSmissions"));
        doMissionsBeforeLoop = Boolean.parseBoolean(prop.getProperty("~doMissionsBeforeLoop"));
        doMissionsAtFirst = Boolean.parseBoolean(prop.getProperty("~doMissionsAtFirst"));
        playing = Boolean.parseBoolean(prop.getProperty("~playing"));
        allRamen = Boolean.parseBoolean(prop.getProperty("~allRamen"));
        doSurgery = Boolean.parseBoolean(prop.getProperty("~doSurgery"));
        forceSurgery = Boolean.parseBoolean(prop.getProperty("~forceSurgery"));
        mission = Mission.get(prop.getProperty("~mission"));
        specMission = Mission.get(prop.getProperty("~specMission"));
        currentMission = Mission.get(prop.getProperty("~currentMission"));
        battleAction = battleAction.get(prop.getProperty("~battleAction"));
        battleActionSecond = battleAction.get(prop.getProperty("~battleActionSecond"));
        battleActionThird = battleAction.get(prop.getProperty("~battleActionThird"));
        battleActionForCommander = battleAction.get(prop.getProperty("~battleActionForCommander"));
        battleActionForSparring = battleAction.get(prop.getProperty("~battleActionForSparring"));
        battleActionForMissions = battleAction.get(prop.getProperty("~battleActionForMissions"));
        drainType = Integer.parseInt(prop.getProperty("~drainType"));
        evenStaminaGeneral = Boolean.parseBoolean(prop.getProperty("~evenStaminaGeneral"));
        drainStaminaGeneralType = Generals.get(prop.getProperty("~drainStaminaGeneralType"));
        evenChakra = Stats.get(prop.getProperty("~evenChakra"));
        drainChakra = Stats.get(prop.getProperty("~drainChakra"));
        evenStamina = Stats.get(prop.getProperty("~evenStamina"));
        drainStamina = Stats.get(prop.getProperty("~drainStamina"));
        evenChakraType = DefenseOffense.get(prop.getProperty("~evenChakraType"));
        drainChakraType = DefenseOffense.get(prop.getProperty("~drainChakraType"));
        evenStaminaType = DefenseOffense.get(prop.getProperty("~evenStaminaType"));
        drainStaminaType = DefenseOffense.get(prop.getProperty("~drainStaminaType"));
    }

    protected void saveEditableToProps() {
        prop.setProperty("~-------", "---------");
        prop.setProperty("~moneyMin", moneyMin + "");
        prop.setProperty("~minutesToTrain", minutesToTrain + "");
        prop.setProperty("~healthMinInBattle", healthMinInBattle + "");
        prop.setProperty("~healthMinInSBattle", healthMinInSBattle + "");
        prop.setProperty("~chakraMin", chakraMin + "");
        prop.setProperty("~staminaMin", staminaMin + "");
        prop.setProperty("~logoutAfterDrain", logoutAfterDrain + "");
        prop.setProperty("~trainShometsu", trainShometsu + "");
        prop.setProperty("~doSmissions", doSmissions + "");
        prop.setProperty("~doMissionsBeforeLoop", doMissionsBeforeLoop + "");
        prop.setProperty("~doMissionsAtFirst", doMissionsAtFirst + "");
        prop.setProperty("~playing", playing + "");
        prop.setProperty("~mission", mission + "");
        prop.setProperty("~specMission", specMission + "");
        prop.setProperty("~currentMission", currentMission + "");
        prop.setProperty("~battleAction", battleAction.name() + "");
        prop.setProperty("~battleActionSecond", battleActionSecond.name() + "");
        prop.setProperty("~battleActionThird", battleActionThird.name() + "");
        prop.setProperty("~battleActionForCommander", battleActionForCommander.name() + "");
        prop.setProperty("~battleActionForSparring", battleActionForSparring.name() + "");
        prop.setProperty("~battleActionForMissions", battleActionForMissions.name() + "");
        prop.setProperty("~allRamen", allRamen + "");
        prop.setProperty("~doSurgery", doSurgery + "");
        prop.setProperty("~forceSurgery", forceSurgery + "");
        prop.setProperty("~drainType", drainType + "");
        prop.setProperty("~evenStaminaGeneral", evenStaminaGeneral + "");
        prop.setProperty("~drainStaminaGeneralType", drainStaminaGeneralType + "");
        prop.setProperty("~evenChakra", evenChakra + "");
        prop.setProperty("~drainChakra", drainChakra + "");
        prop.setProperty("~evenStamina", evenStamina + "");
        prop.setProperty("~drainStamina", drainStamina + "");
        prop.setProperty("~evenChakraType", evenChakraType + "");
        prop.setProperty("~drainChakraType", drainChakraType + "");
        prop.setProperty("~evenStaminaType", evenStaminaType + "");
        prop.setProperty("~drainStaminaType", drainStaminaType + "");
    }

    public static Logger initLogger(String name) {
        File logDIr = new File("logs");
        if (!logDIr.exists()) logDIr.mkdir();
        FileHandler fh;
        Logger logger = Logger.getLogger(name);
        try {
            fh = new FileHandler("logs/" + name + ".log", true);
            fh.setFormatter(new Formatter() {

                @Override
                public String format(LogRecord record) {
                    StringBuffer buf = new StringBuffer(1000);
                    buf.append(new java.util.Date());
                    buf.append(' ');
                    buf.append(record.getLevel());
                    buf.append(' ');
                    buf.append(formatMessage(record));
                    buf.append('\n');
                    return buf.toString();
                }
            });
            logger.addHandler(fh);
            logger.setUseParentHandlers(false);
            logger.setLevel(Level.ALL);
        } catch (SecurityException e) {
        } catch (IOException e) {
        }
        return logger;
    }

    private void setRequestProperties(URLConnection conn) {
        this.cookie = fetchCookie();
        try {
            conn.setRequestProperty("cookie", this.cookie);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String fetchCookie() {
        if (cookie != null) {
            cookieBackup = cookie;
            return cookie;
        }
        String tmpCookie = prop.getProperty("!cookie");
        if (!"".equals(tmpCookie)) {
            cookieBackup = tmpCookie;
            return cookieBackup;
        }
        prop.setProperty("!cookie", cookieBackup + "");
        return cookieBackup;
    }

    protected String commonData() {
        String data = encode("id") + "=" + encode(id);
        data += "&" + encode("page") + "=" + encode("train");
        data += "&" + encode("Submit") + "=" + encode("Train");
        return data;
    }

    protected void wake() {
        print(" Wake up ... ");
        asleep = false;
        if (camp) {
            getHtml("http://www.theninja-rpg.com/index.php?id=19&act=wake");
        } else {
            getHtml("http://www.theninja-rpg.com/index.php?id=23&act=wake");
        }
        println(" awaken");
    }

    /**
     * Fell asleep for some fixed period of time.
     */
    protected void sleeping() {
        sleeping(sleep);
    }

    /**
     * Fell asleep for some period of time.
     * 
     * @param timeout how long to sleep
     */
    protected void sleeping(long timeout) {
        if (!asleep) {
            if (camp) {
                getHtml("http://www.theninja-rpg.com/index.php?id=19&act=sleep");
            } else {
                getHtml("http://www.theninja-rpg.com/index.php?id=23&act=sleep");
            }
            asleep = true;
            print("fall asleep ...");
        }
        print(" is sleeping");
        if (timeout == 0) {
            println("");
            return;
        }
        if (timeout / 1000 / 60 == 0) {
            println(" for " + timeout / 1000 + " sec");
        } else {
            println(" for " + timeout / 1000 / 60 + " min");
        }
        sleepRandom(timeout);
    }

    /**
     * Wait for some time.
     * 
     * @param timeout how long to wait
     */
    protected static void sleepRandom(long timeout) {
        try {
            Thread.sleep(timeout + new Random().nextInt((int) SECOND * 2));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Wait for some time.
     * 
     * @param timeout how long to wait
     */
    protected static void sleepInSec(int timeoutSeconds) {
        try {
            Thread.sleep(timeoutSeconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    protected void logout(int timeoutSeconds) {
        logout();
        sleepInSec(timeoutSeconds);
    }

    protected void logout() {
        send("http://www.theninja-rpg.com/index.php?id=1&act=logout");
    }

    protected long getNextOccupationTime() {
        NEXT_OCCUPATION_TIME = Long.parseLong(prop.getProperty("occupation_timer", "0"));
        return NEXT_OCCUPATION_TIME;
    }

    protected void setNextOccupationTime(long time) {
        NEXT_OCCUPATION_TIME = time;
        prop.setProperty("occupation_timer", time + "");
    }

    /**
     * Get response from url query. First it check if the ninja is asleep and
     * wakes him up.
     * 
     * @param uri
     * @param data
     */
    protected void getHtml(String uri, String data) {
        if (asleep) {
            wake();
        }
        send(uri, data);
        checkResponse(uri, data);
    }

    protected abstract void checkResponse(String uri, String data);

    protected void getHtml(String uri) {
        getHtml(uri, null);
    }

    public void login() {
        String loginData = encode("LoginSubmit") + "=" + encode("Submit");
        loginData += "&" + encode("lgn_usr_stpd") + "=" + encode(name);
        loginData += "&" + encode("id") + "=" + encode("1");
        loginData += "&" + encode("login_password") + "=" + encode(pass);
        String code = null;
        InputStream in = null;
        try {
            in = getInputStream("http://www.theninja-rpg.com/turing.php?sv=turnum", null);
            BufferedImage image = ImageIO.read(in);
            sleepRandom(2 * SECOND);
            code = LoginImg.getImgAsString(image, false);
            println(code);
        } catch (Exception e) {
            println(e.toString());
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
        if ((code != null) && (code.length() == 6)) {
            loginData += "&" + encode("login_code") + "=" + encode(code);
            send("http://www.theninja-rpg.com/?id=1", loginData);
        } else {
            login();
        }
        sleeping(0);
    }

    protected String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return "";
    }

    protected void send(String uri) {
        send(uri, null);
    }

    /**
     * Get response from url query. It sends the request and store it in
     * <code>html</code> for further use.
     * 
     * @param uri
     * @param data
     */
    protected void send(String uri, String data) {
        InputStream in = null;
        try {
            in = getInputStream(uri, data);
            html = getStringFromStream(in);
        } catch (IOException e) {
            send(uri, data);
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }
    }

    private String getStringFromStream(InputStream in) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        StringBuffer sb = new StringBuffer();
        String line = "";
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    protected void saveToFile() {
        saveToFile("");
    }

    protected void saveToFile(String string) {
        if (saveToFile) {
            boolean created;
            File logFile = new File("logs");
            if (!logFile.exists()) {
                created = logFile.mkdir();
            } else {
                created = true;
            }
            File file = new File("logs/" + name + "_" + enemyName + "_" + enemyVillage + "_" + string + "_" + attackCount + "_" + battleId + ".htm");
            if (created && file.exists()) {
                file = new File("logs/" + name + "_" + enemyName + "_" + enemyVillage + "_" + string + "_" + attackCount + "_" + attackCount + "_" + battleId + ".htm");
            }
            saveToFile(file, html);
        }
    }

    protected void saveToFile(File file, String content) {
        File logFile = new File("logs");
        if (!logFile.exists()) {
            logFile.mkdir();
        }
        FileWriter fw;
        try {
            fw = new FileWriter(file);
            fw.write(content);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected String getCookie(String uri) {
        URLConnection conn = null;
        try {
            conn = new URL(uri).openConnection();
            conn.connect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Map<String, List<String>> headers = conn.getHeaderFields();
        List<String> values = headers.get("Set-Cookie");
        String cookieValue = null;
        for (Iterator<String> iter = values.iterator(); iter.hasNext(); ) {
            String v = iter.next().toString();
            if (cookieValue == null) cookieValue = v; else cookieValue = cookieValue + ";" + v;
        }
        return cookieValue;
    }

    protected InputStream getInputStream(String uri, String data) {
        InputStream in = null;
        try {
            URLConnection conn = getConnection(uri, data);
            OutputStreamWriter wr = null;
            setRequestProperties(conn);
            if (data != null) {
                conn.setDoOutput(true);
                wr = new OutputStreamWriter(conn.getOutputStream());
                wr.write(data);
                wr.close();
            }
            in = conn.getInputStream();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return in;
    }

    protected URLConnection getConnection(String uri, String data) throws MalformedURLException, IOException {
        URL url = new URL(uri);
        URLConnection conn = url.openConnection();
        conn.setConnectTimeout((int) MINUTE / 2);
        conn.setReadTimeout((int) MINUTE / 2);
        return conn;
    }

    protected String searchFor(Pattern pattern) {
        Matcher matcher;
        matcher = pattern.matcher(html);
        while (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    protected List<String> searchForStrings(Pattern pattern) {
        Matcher matcher;
        matcher = pattern.matcher(html);
        List<String> strings = new LinkedList<String>();
        while (matcher.find()) {
            strings.add(matcher.group(1));
        }
        Collections.reverse(strings);
        return strings;
    }

    protected void writeToProp() {
        try {
            prop.store(new FileOutputStream("./prop/" + name + ".properties"), "");
        } catch (IOException e) {
        }
    }

    protected void println(Object string) {
        System.out.print(name + " : ");
        System.out.println(string.toString());
        if (logString == null) {
            logString = "";
        }
        log.info(logString + string.toString());
        logString = null;
    }

    protected void print(Object string) {
        if (logString == null) {
            logString = string.toString();
        } else {
            logString += string.toString();
        }
        System.out.print(name + " : ");
        System.out.print(string.toString());
    }
}
