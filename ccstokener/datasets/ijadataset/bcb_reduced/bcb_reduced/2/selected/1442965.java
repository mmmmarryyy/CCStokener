package vacuum.noSpam;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class NoSpam extends JavaPlugin {

    private static final String frequencies = "http://dev.bukkit.org/media/files/567/365/frequencies.txt";

    private static final String defaultConfig = "http://vacuum-nospam.googlecode.com/svn/trunk/resources/config.yml";

    static final String dictionary = "http://docs.oracle.com/javase/tutorial/collections/interfaces/examples/dictionary.txt";

    private boolean verbose;

    SpamDetector sd;

    boolean whitelist;

    ArrayList<String> observeList;

    @Override
    public void onDisable() {
        sd.shutdown(getDataFolder());
    }

    @Override
    public void onEnable() {
        log("NoSpam by FalseVacuum is initializing...");
        File config = new File(getDataFolder().toString() + File.separatorChar + "config.yml");
        File muted = new File(getDataFolder().toString() + File.separatorChar + "muted.txt");
        File observe = new File(getDataFolder().toString() + File.separatorChar + "observe.txt");
        log("Initializing config files");
        if (!config.exists()) {
            config.getParentFile().mkdirs();
            try {
                config.createNewFile();
                download(defaultConfig, config);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!muted.exists()) {
            muted.getParentFile().mkdirs();
            try {
                muted.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!observe.exists()) {
            try {
                observe.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        log("Loading config files");
        YamlConfiguration yc = YamlConfiguration.loadConfiguration(config);
        verbose = yc.getBoolean("tests.observation.verbose");
        Dictionary dict = new Dictionary(new File(getDataFolder().toString() + File.separatorChar + "dictionary.txt"), yc.getBoolean("tests.dictionary.preload"), verbose);
        StatsAnalyzer stats = new StatsAnalyzer(verbose, getFrequencyTable());
        boolean[] testsOptions = { yc.getBoolean("tests.dictionary.use"), yc.getBoolean("tests.wordsize.use"), yc.getBoolean("tests.characterfrequency.use"), yc.getBoolean("tests.lastTime.use"), yc.getBoolean("tests.distance.use") };
        float[] pointValues = { (float) yc.getDouble("tests.dictionary.points"), (float) yc.getDouble("tests.wordsize.points"), (float) yc.getDouble("tests.characterfrequency.points"), (float) yc.getDouble("tests.lastTime.points"), (float) yc.getDouble("tests.distance.points") };
        ;
        float maxPoints = (float) yc.getDouble("tests.maxPoints");
        int maxTime = yc.getInt("tests.messagePersistTime");
        whitelist = yc.getBoolean("tests.observation.aswhitelist");
        observeList = new ArrayList<String>();
        String color = ChatColor.BLACK.toString();
        try {
            color = ChatColor.valueOf(yc.getString("tests.warning.color").toUpperCase()).toString();
        } catch (IllegalArgumentException iae) {
            log("Could not identify color " + yc.getString("tests.warning.color"));
        }
        String warningMessage = color + yc.getString("tests.warning.message");
        int warningPoints = yc.getInt("tests.warning.points");
        log("Loading white/black list");
        try {
            Scanner s = new Scanner(observe);
            while (s.hasNext()) {
                observeList.add(s.nextLine().toLowerCase());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        log("Loading mute list");
        ArrayList<String> mutedList = new ArrayList<String>();
        try {
            Scanner s = new Scanner(muted);
            while (s.hasNext()) {
                mutedList.add(s.nextLine().toLowerCase());
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        color = ChatColor.BLACK.toString();
        try {
            color = ChatColor.valueOf(yc.getString("tests.muteColor").toUpperCase()).toString();
        } catch (IllegalArgumentException iae) {
            log("Could not identify color " + yc.getString("tests.warning.color"));
        }
        String muteMessage = color + yc.getString("tests.muteMessage");
        log("Initializing SpamDetector");
        sd = new SpamDetector(mutedList, verbose, testsOptions, pointValues, maxPoints, maxTime, dict, stats, warningMessage, warningPoints, muteMessage);
        log("Registering join/leave events");
        getServer().getPluginManager().registerEvents(sd, this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        log("Loading commands...");
        log("Adding players...");
        for (Player p : getServer().getOnlinePlayers()) {
            String name = p.getName().toLowerCase();
            boolean contains = observeList.contains(name);
            if (whitelist) {
                if (contains) {
                    log("Adding player " + p.getName());
                    sd.addPlayer(name);
                }
            } else {
                if (!contains) {
                    log("Adding player " + p.getName());
                    sd.addPlayer(name);
                }
            }
        }
        log("Finished loading!");
    }

    private HashMap<Character, Float> getFrequencyTable() {
        File location = new File(getDataFolder().toString() + File.separatorChar + "frequencies.txt");
        if (!location.exists()) {
            download(frequencies, location);
        }
        HashMap<Character, Float> data = new HashMap<Character, Float>();
        try {
            Scanner s = new Scanner(location);
            while (s.hasNext()) {
                String line = s.nextLine();
                char c = line.charAt(0);
                float val = Float.parseFloat(line.substring(line.indexOf('|') + 1, line.length()));
                data.put(c, val);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return data;
    }

    static void download(String from, File to) {
        try {
            URL url = new URL(from);
            InputStream is = url.openStream();
            OutputStream os = new FileOutputStream(to);
            int i;
            System.out.println("[NoSpam] Downloading file from " + from + " to " + to);
            while ((i = is.read()) != -1) {
                os.write(i);
            }
            os.flush();
            is.close();
            os.close();
            System.out.println("[NoSpam] Downloading finished!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void addPlayer(String name) throws IOException {
        removePlayer(name);
        FileWriter fw = new FileWriter(new File(getDataFolder().toString() + File.separatorChar + "observe.txt"), true);
        fw.write(name);
        fw.close();
        if (whitelist) sd.messages.put(name.toLowerCase(), new ArrayList<Message>()); else sd.messages.remove(name.toLowerCase());
    }

    private boolean removePlayer(String name) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(new File(getDataFolder().toString() + File.separatorChar + "observe.txt"), "rw");
        int i = 0;
        StringBuffer buf = new StringBuffer();
        long pos = -1;
        while ((i = raf.read()) != -1) {
            if ('\n' == (char) i) {
                System.out.println("Comparing " + buf.toString() + " to " + name);
                if (buf.toString().equalsIgnoreCase(name)) {
                    pos = raf.getFilePointer() - buf.length();
                    break;
                }
            } else {
                buf.append((char) i);
            }
        }
        if (pos == -1) {
            raf.close();
            return false;
        }
        buf = new StringBuffer();
        log("Removing player " + name);
        while ((i = raf.read()) != -1) {
            buf.append((char) i);
        }
        raf.setLength(pos + 1);
        raf.seek(pos);
        for (char c : buf.toString().toCharArray()) {
            raf.write(c & 0xFF);
        }
        raf.close();
        if (whitelist) sd.messages.remove(name.toLowerCase()); else sd.messages.put(name.toLowerCase(), new ArrayList<Message>());
        return true;
    }

    void log(String s) {
        if (verbose) {
            System.out.println("[NoSpam] " + s);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("nospam")) {
            if (args.length != 2) {
                return false;
            }
            Player player = getServer().getPlayer(args[1]);
            if (player != null) args[1] = player.getName();
            if (args[0].equals("mute")) {
                if (!sd.muted.contains(args[1].toLowerCase())) {
                    player.sendMessage(ChatColor.RED + "You have been muted.");
                    sd.muted.add(args[1].toLowerCase());
                }
                return true;
            } else if (args[0].equals("forgive")) {
                System.out.println("Forgiving " + args[1]);
                player.sendMessage(ChatColor.GREEN + "You have been unmuted.");
                return sd.muted.remove(args[1].toLowerCase());
            } else if (args[0].equals("observe")) {
                try {
                    addPlayer(args[1].toLowerCase());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            } else if (args[0].equals("unobserve")) {
                try {
                    return removePlayer(args[1].toLowerCase());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return false;
    }
}
