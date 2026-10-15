package industries.angelic.angellib.varinstances;

import java.util.ArrayList;
import java.util.Hashtable;

import net.minecraft.entity.EntityList;
import net.minecraft.world.WorldProvider;

import industries.angelic.angellib.AngelLib;
import industries.angelic.angellib.Config;
import industries.angelic.angellib.mixins.early.minecraftforge.AccessorDimensionManager;
import industries.angelic.angellib.util.BiomeUtil;
import industries.angelic.angellib.util.DimensionUtil;
import industries.angelic.angellib.util.MobUtil;
import industries.angelic.angellib.util.PotionUtil;
import industries.angelic.angellib.util.XSTR;

public class VarInstanceCommon {

    public XSTR rand = new XSTR();
    public ArrayList<Class<? extends net.minecraft.entity.Entity>> passiveMobsWhichCanInflictDamage;
    public Hashtable<Integer, Class<? extends WorldProvider>> providers;

    public void postInitHook() {
        providers = AccessorDimensionManager.getProviders();
        buildPassiveMobList();

        if (Config.printPotions) {
            PotionUtil.printPotionNames();
        }
        if (Config.printMobs) {
            MobUtil.printMobNames();
        }
        if (Config.printBiomes) {
            BiomeUtil.printBiomeNames();
        }
        // Needs to happen after the post init hook
        if (Config.printDimensions) {
            DimensionUtil.printDimensionNames();
        }
    }

    public void buildPassiveMobList() {
        passiveMobsWhichCanInflictDamage = new ArrayList<>();
        for (String s : Config.passiveMobsWhichCanInflictDamage) {
            Class<? extends net.minecraft.entity.Entity> c = EntityList.stringToClassMapping.get(s);
            if (c == null) {
                AngelLib.LOG.error("Failed to get mob class for name {}", s);
            } else {
                passiveMobsWhichCanInflictDamage.add(c);
            }
        }
    }
}
