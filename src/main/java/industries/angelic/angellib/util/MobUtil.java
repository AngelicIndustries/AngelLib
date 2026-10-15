package industries.angelic.angellib.util;

import java.lang.reflect.Constructor;

import net.minecraft.entity.EntityList;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMonsterPlacer;

import cpw.mods.fml.common.registry.GameRegistry;
import industries.angelic.angellib.AngelLib;

public class MobUtil {

    public static void printMobNames() {
        AngelLib.LOG.info("=========Mob List=========");
        AngelLib.LOG.info(
            "The printing of this list is for you to know which mob has which name. You can disable this print in the configs.");
        for (Object e : EntityList.stringToClassMapping.keySet()) {
            AngelLib.LOG.info(e + " (" + EntityList.stringToClassMapping.get(e) + ")");
        }
        AngelLib.LOG.info("=============================");
    }

    public static <T extends ItemMonsterPlacer> void registerSpawnEgg(Class<T> eggClass, String name, int primaryColor,
        int secondaryColor, String modid) {
        try {
            // Create instance using a constructor that accepts (String, int, int)
            Constructor<T> constructor = eggClass.getConstructor(String.class, int.class, int.class);
            Item itemSpawnEgg = constructor.newInstance(name, primaryColor, secondaryColor)
                .setUnlocalizedName("spawn_egg_" + name.toLowerCase())
                .setTextureName(modid + ":spawn_egg_" + name.toLowerCase());

            GameRegistry.registerItem(itemSpawnEgg, "spawnEgg_" + name);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to register spawn egg for " + name, e);
        }
    }
}
