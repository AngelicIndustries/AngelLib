package industries.angelic.angellib;

import java.io.File;

import net.minecraft.launchwrapper.Launch;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import industries.angelic.angellib.varinstances.VarInstanceClient;
import industries.angelic.angellib.varinstances.VarInstanceCommon;
import industries.angelic.angellib.varinstances.VarInstanceServer;

@Mod(
    modid = AngelLib.MODID,
    version = Tags.VERSION,
    name = "AngelLib",
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*",
    customProperties = { @Mod.CustomProperty(k = "license", v = "LGPLv3"),
        @Mod.CustomProperty(k = "issueTrackerUrl", v = "https://github.com/AngelicIndustries/AngelLib/issues"),
        @Mod.CustomProperty(k = "iconFile", v = "assets/angellib/logo.png"),
        @Mod.CustomProperty(k = "backgroundFile", v = "assets/angellib/background.png") },
    guiFactory = "industries.angelic." + AngelLib.MODID + ".gui.GuiFactory")
public class AngelLib {

    public static final String MODID = "angellib";
    public static final String MODGROUP = "industries.angelic";
    public static final Logger LOG = LogManager.getLogger(MODID);

    public static File confFile;

    private static boolean DEBUG_MODE;

    public static VarInstanceClient varInstanceClient;
    public static VarInstanceServer varInstanceServer;
    public static VarInstanceCommon varInstanceCommon;

    public static File angellibDir;

    @SidedProxy(
        clientSide = MODGROUP + "." + MODID + ".ClientProxy",
        serverSide = MODGROUP + "." + MODID + ".CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        confFile = resolveConfigFile(event.getSuggestedConfigurationFile());
        String debugVar = System.getenv("MCMODDING_DEBUG_MODE");
        DEBUG_MODE = debugVar != null;
        AngelLib.LOG.info("MCMODDING_DEBUG_MODE env var: {}", DEBUG_MODE);
        AngelLib.LOG.info("Using config file {}", confFile);
        proxy.preInit(event);
        AngelLib.LOG.info("debugMode config option: {}", Config.debugMode);
        AngelLib.LOG.info("isDebugMode: {}", isDebugMode());
    }

    @Mod.EventHandler
    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    @Mod.EventHandler
    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event);
    }

    @Mod.EventHandler
    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event);
    }

    public static boolean isDebugMode() {
        return DEBUG_MODE || Config.debugMode;
    }

    public static void debug(String message) {
        if (isDebugMode()) {
            LOG.info("DEBUG: {}", message);
        }
    }

    public static File getConfigDir() {
        if (confFile != null && confFile.getParentFile() != null) {
            return confFile.getParentFile();
        }
        File minecraftHome = Launch.minecraftHome;
        if (minecraftHome == null) {
            minecraftHome = new File(".");
        }
        return new File(new File(minecraftHome, "config"), MODID);
    }

    private static File resolveConfigFile(File suggestedConfigFile) {
        File configDir = suggestedConfigFile.getParentFile();
        File modConfigDir = new File(configDir, MODID);
        return new File(modConfigDir, suggestedConfigFile.getName());
    }

    /*
     * ▖▖..........▗..▄▖..........
     * ▙▘▛▌▛▌▛▘█▌▛▘▜▘ ▗▘▌▌▛▘▀▌█▌▛▌
     * ▌▌▙▌▌▌▙▖▙▖▌ ▐▖ ▙▖▙▌▙▖▙▖▙▖▌▌
     * .................▄▌........
     */

    // TODO:
    // * Make the mod menu scrollable only in the list area. Don't forgor separate mixin for hodgepodge and vanilla
    // endercore
    // * Clickable MOTD links
}
