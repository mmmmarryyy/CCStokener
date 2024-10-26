package se.sics.mspsim.platform;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import se.sics.mspsim.cli.CommandHandler;
import se.sics.mspsim.cli.DebugCommands;
import se.sics.mspsim.cli.FileCommands;
import se.sics.mspsim.cli.MiscCommands;
import se.sics.mspsim.cli.NetCommands;
import se.sics.mspsim.cli.ProfilerCommands;
import se.sics.mspsim.cli.StreamCommandHandler;
import se.sics.mspsim.cli.WindowCommands;
import se.sics.mspsim.core.Chip;
import se.sics.mspsim.core.EmulationException;
import se.sics.mspsim.core.EmulationLogger;
import se.sics.mspsim.core.MSP430;
import se.sics.mspsim.core.MSP430Config;
import se.sics.mspsim.core.MSP430Constants;
import se.sics.mspsim.extutil.highlight.HighlightSourceViewer;
import se.sics.mspsim.ui.ControlUI;
import se.sics.mspsim.ui.JFrameWindowManager;
import se.sics.mspsim.ui.StackUI;
import se.sics.mspsim.util.ArgumentManager;
import se.sics.mspsim.util.ComponentRegistry;
import se.sics.mspsim.util.ConfigManager;
import se.sics.mspsim.util.DefaultEmulationLogger;
import se.sics.mspsim.util.ELF;
import se.sics.mspsim.util.IHexReader;
import se.sics.mspsim.util.MapTable;
import se.sics.mspsim.util.OperatingModeStatistics;
import se.sics.mspsim.util.PluginRepository;
import se.sics.mspsim.util.StatCommands;

public abstract class GenericNode extends Chip implements Runnable {

    private static final String PROMPT = "MSPSim>";

    protected final MSP430 cpu;

    protected final ComponentRegistry registry;

    protected ConfigManager config;

    protected String firmwareFile = null;

    protected ELF elf;

    protected OperatingModeStatistics stats;

    public GenericNode(String id, MSP430Config config) {
        super(id, new MSP430(0, new ComponentRegistry(), config));
        this.cpu = (MSP430) super.cpu;
        this.registry = cpu.getRegistry();
    }

    public ComponentRegistry getRegistry() {
        return registry;
    }

    public MSP430 getCPU() {
        return cpu;
    }

    public abstract void setupNode();

    public void setCommandHandler(CommandHandler handler) {
        registry.registerComponent("commandHandler", handler);
    }

    public void setupArgs(ArgumentManager config) throws IOException {
        String[] args = config.getArguments();
        if (args.length == 0) {
            System.out.println("Usage: " + getClass().getName() + " <firmware>");
            System.exit(1);
        }
        firmwareFile = args[0];
        if (config.getProperty("nogui") == null) {
            config.setProperty("nogui", "false");
        }
        if (config.getProperty("autorun") == null) {
            File fp = new File("scripts/autorun.sc");
            if (fp.exists()) {
                config.setProperty("autorun", "scripts/autorun.sc");
            } else {
                try {
                    File dir = new File(GenericNode.class.getProtectionDomain().getCodeSource().getLocation().toURI()).getParentFile();
                    fp = new File(dir, "scripts/autorun.sc");
                    if (fp.exists()) {
                        config.setProperty("autorun", fp.getAbsolutePath());
                    }
                } catch (URISyntaxException e) {
                }
            }
        }
        int[] memory = cpu.getMemory();
        if (args[0].endsWith("ihex")) {
            IHexReader reader = new IHexReader();
            reader.readFile(memory, firmwareFile);
        } else {
            loadFirmware(firmwareFile, memory);
        }
        if (args.length > 1) {
            MapTable map = new MapTable(args[1]);
            cpu.getDisAsm().setMap(map);
            registry.registerComponent("mapTable", map);
        }
        setup(config);
        if (!config.getPropertyAsBoolean("nogui", false)) {
            ControlUI control = new ControlUI();
            registry.registerComponent("controlgui", control);
            registry.registerComponent("stackchart", new StackUI(cpu));
            HighlightSourceViewer sourceViewer = new HighlightSourceViewer();
            if (firmwareFile != null) {
                File fp = new File(firmwareFile).getParentFile();
                if (fp != null) {
                    try {
                        fp = fp.getCanonicalFile();
                    } catch (Exception e) {
                    }
                    sourceViewer.addSearchPath(fp);
                }
            }
            control.setSourceViewer(sourceViewer);
        }
        String script = config.getProperty("autorun");
        if (script != null) {
            File fp = new File(script);
            if (fp.canRead()) {
                CommandHandler ch = (CommandHandler) registry.getComponent("commandHandler");
                script = script.replace('\\', '/');
                System.out.println("Autoloading script: " + script);
                config.setProperty("autoloadScript", script);
                if (ch != null) {
                    ch.lineRead("source \"" + script + '"');
                }
            }
        }
        config.setProperty("firmwareFile", firmwareFile);
        System.out.println("-----------------------------------------------");
        System.out.println("MSPSim " + MSP430Constants.VERSION + " starting firmware: " + firmwareFile);
        System.out.println("-----------------------------------------------");
        System.out.print(PROMPT);
        System.out.flush();
    }

    public void setup(ConfigManager config) throws IOException {
        this.config = config;
        EmulationLogger logger = (EmulationLogger) registry.getComponent("logger");
        if (logger == null) {
            logger = new DefaultEmulationLogger(cpu, System.out);
            registry.registerComponent("logger", logger);
        }
        registry.registerComponent("cpu", cpu);
        registry.registerComponent("node", this);
        registry.registerComponent("config", config);
        cpu.setEmulationLogger(logger);
        CommandHandler ch = (CommandHandler) registry.getComponent("commandHandler");
        if (ch == null) {
            ch = new StreamCommandHandler(System.in, System.out, System.err, PROMPT);
            registry.registerComponent("commandHandler", ch);
        }
        stats = new OperatingModeStatistics(cpu);
        registry.registerComponent("pluginRepository", new PluginRepository());
        registry.registerComponent("debugcmd", new DebugCommands());
        registry.registerComponent("misccmd", new MiscCommands());
        registry.registerComponent("filecmd", new FileCommands());
        registry.registerComponent("statcmd", new StatCommands(cpu, stats));
        registry.registerComponent("wincmd", new WindowCommands());
        registry.registerComponent("profilecmd", new ProfilerCommands());
        registry.registerComponent("netcmd", new NetCommands());
        registry.registerComponent("windowManager", new JFrameWindowManager());
        cpu.setMonitorExec(true);
        setupNode();
        registry.start();
        cpu.reset();
    }

    public void run() {
        if (!cpu.isRunning()) {
            try {
                cpu.cpuloop();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void start() {
        if (!cpu.isRunning()) {
            Thread thread = new Thread(this);
            thread.setPriority(Thread.NORM_PRIORITY);
            thread.start();
        }
    }

    public void stop() {
        cpu.setRunning(false);
    }

    public void step() throws EmulationException {
        if (!cpu.isRunning()) {
            cpu.step();
        }
    }

    public ELF loadFirmware(URL url, int[] memory) throws IOException {
        DataInputStream inputStream = new DataInputStream(url.openStream());
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        byte[] firmwareData = new byte[2048];
        int read;
        while ((read = inputStream.read(firmwareData)) != -1) {
            byteStream.write(firmwareData, 0, read);
        }
        inputStream.close();
        ELF elf = new ELF(byteStream.toByteArray());
        elf.readAll();
        return loadFirmware(elf, memory);
    }

    public ELF loadFirmware(String name, int[] memory) throws IOException {
        return loadFirmware(ELF.readELF(firmwareFile = name), memory);
    }

    public ELF loadFirmware(ELF elf, int[] memory) {
        stop();
        this.elf = elf;
        elf.loadPrograms(memory);
        MapTable map = elf.getMap();
        cpu.getDisAsm().setMap(map);
        cpu.setMap(map);
        registry.registerComponent("elf", elf);
        registry.registerComponent("mapTable", map);
        return elf;
    }

    public void step(int nr) throws EmulationException {
        if (!cpu.isRunning()) {
            cpu.stepInstructions(nr);
        }
    }

    public int getConfiguration(int param) {
        return 0;
    }
}
