package industries.angelic.angellib;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import industries.angelic.angellib.command.CommandDumpThaumonomicon;
import industries.angelic.angellib.command.CommandReloadServerIcon;
import industries.angelic.angellib.command.CommandWarpDim;
import industries.angelic.angellib.compat.LoadedMods;
import industries.angelic.angellib.packet.PacketHandler;
import industries.angelic.angellib.services.S00PacketServerInfoModifyService;
import industries.angelic.angellib.services.fishing.FishingLootConfig;
import industries.angelic.angellib.services.http.HttpPortProxyConfig;
import industries.angelic.angellib.util.MiscUtil;
import industries.angelic.angellib.varinstances.VarInstanceCommon;
import industries.angelic.angellib.varinstances.VarInstanceServer;

public class CommonProxy {

    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        LoadedMods.init();
        AngelLib.varInstanceCommon = new VarInstanceCommon();
        if (MiscUtil.isServer()) {
            AngelLib.varInstanceServer = new VarInstanceServer();

            S00PacketServerInfoModifyService.registerHandler((response, angelLibPresent) -> {
                if (angelLibPresent) {
                    AngelLib.debug("AngelLib detected");
                    if (AngelLib.varInstanceServer.animatedFaviconBlob != null) {
                        AngelLib.debug("Animated favicon not null, serving it");
                        response.func_151320_a(AngelLib.varInstanceServer.animatedFaviconBlob);
                    } else {
                        AngelLib.debug(
                            "Animated favicon null, serving static"
                                + (AngelLib.varInstanceServer.staticFaviconBlob == null ? " (which is also null)"
                                    : ""));
                        response.func_151320_a(AngelLib.varInstanceServer.staticFaviconBlob);
                    }
                } else {
                    AngelLib.debug("Data has no animated feature, serving static");
                    response.func_151320_a(AngelLib.varInstanceServer.staticFaviconBlob);
                }
                return null;
            });

            S00PacketServerInfoModifyService.registerHandler((s, j) -> { return AngelLib.MODID; });
        }
        Config.loadConfig(AngelLib.confFile);
        AngelLib.angellibDir = AngelLib.getConfigDir();
        if (!AngelLib.angellibDir.exists()) {
            AngelLib.angellibDir.mkdirs();
        }
        AngelLib.LOG.info("angellibDir is located at {}", AngelLib.angellibDir);
        FishingLootConfig.ensureExists();
        if (MiscUtil.isServer()) {
            HttpPortProxyConfig.ensureExists();
        }

        PacketHandler.initPackets();

        AngelLib.LOG.info("I am AngelLib at version " + Tags.VERSION);
    }

    // load "Do your mod setup. Build whatever data structures you care about. Register recipes." (Remove if not needed)
    public void init(FMLInitializationEvent event) {

    }

    // postInit "Handle interaction with other mods, complete your setup based on this." (Remove if not needed)
    public void postInit(FMLPostInitializationEvent event) {
        AngelLib.varInstanceCommon.postInitHook();
    }

    // register server commands in this event handler (Remove if not needed)
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandReloadServerIcon());
        event.registerServerCommand(new CommandDumpThaumonomicon());
        event.registerServerCommand(new CommandWarpDim());
    }

    public void serverStarting(FMLServerStartedEvent event) {
        System.out.println("SERVER STARTED");
    }
}
