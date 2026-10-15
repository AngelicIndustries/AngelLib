package industries.angelic.angellib.packet;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import industries.angelic.angellib.AngelLib;
import industries.angelic.angellib.packet.packets.ThaumcraftDumpPacket;

public class PacketHandler {

    public static SimpleNetworkWrapper net;

    public static void initPackets() {
        net = NetworkRegistry.INSTANCE.newSimpleChannel(AngelLib.MODID.toUpperCase());
        registerMessage(ThaumcraftDumpPacket.class, ThaumcraftDumpPacket.SimpleMessage.class);
    }

    private static int nextPacketId = 0;

    private static void registerMessage(Class packet, Class message) {
        net.registerMessage(packet, message, nextPacketId, Side.CLIENT);
        net.registerMessage(packet, message, nextPacketId, Side.SERVER);
        nextPacketId++;
    }
}
