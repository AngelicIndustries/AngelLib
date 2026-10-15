package industries.angelic.angellib.varinstances;

import java.io.File;
import java.util.concurrent.ExecutorService;

import net.minecraft.client.Minecraft;

import industries.angelic.angellib.AngelLib;
import industries.angelic.angellib.AngelLibThreadPool;

public class VarInstanceClient {

    public ExecutorService gifloaderPool = AngelLibThreadPool.createSingleThreadPool("AngelLib-GifLoader");
    public Minecraft minecraftRef = Minecraft.getMinecraft();
    public File serverIconDir;

    public void preinitHook() {
        this.serverIconDir = new File(AngelLib.angellibDir, "server_icons");
        if (!this.serverIconDir.exists()) {
            this.serverIconDir.mkdirs();
        }
    }
}
