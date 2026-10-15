package industries.angelic.angellib.core;

import java.util.List;
import java.util.Set;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import industries.angelic.angellib.AngelLib;

@SuppressWarnings("unused")
@IFMLLoadingPlugin.MCVersion("1.7.10")
public class EarlyMixinLoader extends AngelEarlyMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins." + AngelLib.MODID + ".early.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedCoreMods) {
        return Mixins.getEarlyMixinsForLoader();
    }
}
