package industries.angelic.angellib.util;

import net.minecraft.potion.Potion;

import industries.angelic.angellib.AngelLib;

public class PotionUtil {

    public static Potion getPotionById(int id) {
        Potion res = null;
        for (Potion p : Potion.potionTypes) {
            if (p != null && p.getId() == id) {
                res = p;
                AngelLib.debug("Found potion with id " + id + "! (" + p.getName() + ")");
            }
        }
        return res;
    }

    public static Potion getPotionByName(String name) {
        Potion res = null;
        for (Potion p : Potion.potionTypes) {
            if (p != null && p.getName()
                .equals(name)) {
                res = p;
                AngelLib.debug("Found potion with id " + name + "! (" + p.getName() + ")");
            }
        }
        return res;
    }

    public static void printPotionNames() {
        AngelLib.LOG.info("=========Potion List=========");
        AngelLib.LOG.info(
            "The printing of this list is for you to know which enchantment has which id. You can disable this print in the configs.");
        for (Potion p : Potion.potionTypes) {
            if (p != null) {
                AngelLib.LOG.info(p.getId() + ": " + p.getName());
            }
        }
        AngelLib.LOG.info("=============================");
    }
}
