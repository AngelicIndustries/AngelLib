package industries.angelic.angellib.util;

import net.minecraft.world.WorldProvider;

import industries.angelic.angellib.AngelLib;

public class DimensionUtil {

    public static class SimpleDimensionObj {

        private final int id;
        private final String name;
        private final String leaveMessage;

        public SimpleDimensionObj(int id, String name, String leaveMessage) {
            this.id = id;
            this.name = name;
            this.leaveMessage = leaveMessage;
        }

        public int getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getLeaveMessage() {
            return leaveMessage;
        }
    }

    public static SimpleDimensionObj getSimpleDimensionObj(String name) {
        if (AngelLib.varInstanceCommon.providers == null) {
            return null;
        }
        for (int i : AngelLib.varInstanceCommon.providers.keySet()) {
            WorldProvider worldProvider = null;
            try {
                if (AngelLib.varInstanceCommon.providers.get(i) != null) {
                    worldProvider = AngelLib.varInstanceCommon.providers.get(i)
                        .newInstance();
                }
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (worldProvider == null) {
                continue;
            }
            if (name.equals(worldProvider.getDimensionName())) {
                return new SimpleDimensionObj(i, worldProvider.getDimensionName(), worldProvider.getDepartMessage());
            }
        }
        return null;
    }

    public static SimpleDimensionObj getSimpleDimensionObj(int id) {
        if (AngelLib.varInstanceCommon.providers == null) {
            return null;
        }
        for (int i : AngelLib.varInstanceCommon.providers.keySet()) {
            WorldProvider worldProvider = null;
            try {
                if (AngelLib.varInstanceCommon.providers.get(i) != null) {
                    worldProvider = AngelLib.varInstanceCommon.providers.get(i)
                        .newInstance();
                }
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (worldProvider == null) {
                continue;
            }
            if (i == id) {
                return new SimpleDimensionObj(i, worldProvider.getDimensionName(), worldProvider.getDepartMessage());
            }
        }
        return null;
    }

    public static void printDimensionNames() {
        AngelLib.LOG.info("=========Dimension List=========");
        for (int i : AngelLib.varInstanceCommon.providers.keySet()) {
            WorldProvider worldProvider = null;
            try {
                if (AngelLib.varInstanceCommon.providers.get(i) != null) {
                    worldProvider = AngelLib.varInstanceCommon.providers.get(i)
                        .newInstance();
                }
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (worldProvider == null) {
                continue;
            }
            AngelLib.LOG.info("{} ({})", worldProvider.getDimensionName(), i);
        }
        AngelLib.LOG.info("=============================");
    }
}
