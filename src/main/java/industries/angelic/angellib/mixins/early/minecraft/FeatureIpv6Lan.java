package industries.angelic.angellib.mixins.early.minecraft;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import net.minecraft.client.multiplayer.ThreadLanServerPing;
import net.minecraft.client.network.LanServerDetector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.angellib.util.NetworkAddressUtil;

/**
 * LAN world discovery in IPv6-only environments.
 *
 * Vanilla announces and listens on IPv4 multicast 224.0.2.60:4445 only, so
 * /publish is invisible without an IPv4 stack. AngelLib hosts announce on the
 * link-local IPv6 group ff02::2:60 (same port) in addition to the IPv4 group,
 * and AngelLib clients listen on both. Dual-stack clients therefore see two
 * list entries per world, one per address family, and can pick either route.
 */
public final class FeatureIpv6Lan {

    private FeatureIpv6Lan() {}

    @Mixin(ThreadLanServerPing.class)
    public abstract static class MixinThreadLanServerPing {

        @Redirect(
            method = "run",
            at = @At(
                value = "INVOKE",
                target = "Ljava/net/DatagramSocket;send(Ljava/net/DatagramPacket;)V",
                remap = false))
        private void angellib$announceBothFamilies(DatagramSocket socket, DatagramPacket packet) throws IOException {
            IOException ipv4Failure = null;
            try {
                socket.send(packet);
            } catch (IOException e) {
                ipv4Failure = e;
            }
            try {
                socket.send(
                    new DatagramPacket(
                        packet.getData(),
                        packet.getLength(),
                        NetworkAddressUtil.lanDiscoveryIpv6Group(),
                        NetworkAddressUtil.LAN_DISCOVERY_PORT));
            } catch (IOException e) {
                if (ipv4Failure != null) {
                    throw ipv4Failure;
                }
            }
        }
    }

    @Mixin(LanServerDetector.ThreadLanServerFind.class)
    public abstract static class MixinThreadLanServerFind {

        @Redirect(
            method = "<init>",
            at = @At(
                value = "INVOKE",
                target = "Ljava/net/MulticastSocket;joinGroup(Ljava/net/InetAddress;)V",
                remap = false))
        private void angellib$joinBothGroups(MulticastSocket socket, InetAddress ipv4Group) throws IOException {
            IOException ipv4Failure = null;
            try {
                socket.joinGroup(ipv4Group);
            } catch (IOException e) {
                ipv4Failure = e;
            }
            try {
                socket.joinGroup(NetworkAddressUtil.lanDiscoveryIpv6Group());
            } catch (IOException e) {
                if (ipv4Failure != null) {
                    throw ipv4Failure;
                }
            }
        }
    }

    @Mixin(LanServerDetector.LanServerList.class)
    public abstract static class MixinLanServerList {

        // Vanilla builds "host:port" by bare concatenation, which produces an
        // unparseable address for IPv6 senders. Bracket the host so the entry
        // survives ServerAddress parsing when joining.
        @Redirect(
            method = "func_77551_a",
            at = @At(
                value = "INVOKE",
                target = "Ljava/net/InetAddress;getHostAddress()Ljava/lang/String;",
                remap = false))
        private String angellib$bracketIpv6Host(InetAddress address) {
            return NetworkAddressUtil.formatHostPort(address.getHostAddress(), 0);
        }
    }
}
