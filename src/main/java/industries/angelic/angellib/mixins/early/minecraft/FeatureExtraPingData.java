package industries.angelic.angellib.mixins.early.minecraft;

import java.lang.reflect.Type;

import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.OldServerPinger;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.ServerStatusResponse;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.NetHandlerHandshakeTCP;
import net.minecraft.server.network.NetHandlerStatusServer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.llamalad7.mixinextras.sugar.Local;

import cpw.mods.fml.client.FMLClientHandler;
import industries.angelic.angellib.AngelLib;
import industries.angelic.angellib.mixininterfaces.INetworkManager;
import industries.angelic.angellib.mixininterfaces.IServerStatusResponse;
import industries.angelic.angellib.services.S00PacketServerInfoModifyService;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

public class FeatureExtraPingData {

    private static final String ANGEL_TOKEN = "\0" + AngelLib.MODID + "\0";

    @Mixin(NetHandlerStatusServer.class)
    public static class NetHandlerStatusServerMixin {

        @Shadow
        @Final
        NetworkManager field_147313_b;

        @Shadow
        @Final
        MinecraftServer field_147314_a;

        @Inject(method = "processServerQuery", at = @At("HEAD"))
        private void onProcessServerQuery(C00PacketServerQuery packetIn, CallbackInfo ci) {
            AngelLib.debug("[Mixin] Received Server Query Packet: " + packetIn);

            boolean angelLibPresent = ((INetworkManager) field_147313_b).isAngelClient();

            if (angelLibPresent) {
                AngelLib.debug("Query packet comes from a angellib client");
            } else {
                AngelLib.debug("Query packet does not come from a angellib client");
            }
            AngelLib.debug("[Mixin] Modifying outbound packet");
            S00PacketServerInfoModifyService.modify(field_147314_a.func_147134_at(), angelLibPresent);
        }

        /*
         * @Inject(method = "processPing", at = @At("HEAD"))
         * private void onProcessPing(C01PacketPing packetIn, CallbackInfo ci) {
         * System.out.println("[Mixin] Received Ping Packet: " + packetIn);
         * }
         */
    }

    @Mixin(ServerStatusResponse.class)
    public static class MixinServerStatusResponse implements IServerStatusResponse {

        @Unique
        private JsonElement extraData;

        @Override
        public void setExtraData(JsonElement extraData) {
            this.extraData = extraData;
        }

        @Override
        public JsonElement getExtraData() {
            if (this.extraData == null) {
                return new JsonObject();
            }
            return this.extraData;
        }
    }

    @Mixin(ServerStatusResponse.Serializer.class)
    public static class ServerStatusResponseSerializerMixin {

        @Inject(method = "serialize", at = @At("RETURN"), cancellable = true)
        private void injectAngelLibEnhancement(ServerStatusResponse response, Type type,
            JsonSerializationContext context, CallbackInfoReturnable<JsonElement> cir) {
            JsonElement returnValue = cir.getReturnValue();
            if (!(returnValue instanceof JsonObject)) return;
            JsonObject json = (JsonObject) returnValue;
            JsonElement extraData = ((IServerStatusResponse) response).getExtraData();
            if (extraData != null) {
                json.add(AngelLib.MODID, extraData);
            }
            cir.setReturnValue(json);
        }

        @Inject(method = "deserialize", at = @At("RETURN"))
        private void interceptAngelLibEnhancement(JsonElement jsonElement, Type type,
            com.google.gson.JsonDeserializationContext context, CallbackInfoReturnable<ServerStatusResponse> cir) {
            if (!(jsonElement instanceof JsonObject)) return;
            JsonObject rootJson = (JsonObject) jsonElement;
            JsonElement angellibData = rootJson.get(AngelLib.MODID);
            if (angellibData == null || !angellibData.isJsonObject()) return;
            ServerStatusResponse response = cir.getReturnValue();
            JsonObject angellibJson = angellibData.getAsJsonObject();
            ((IServerStatusResponse) response).setExtraData(angellibJson);
        }

    }

    @Mixin(value = FMLClientHandler.class, remap = false)
    public static class MixinFMLClientHandler {

        @Inject(method = "bindServerListData", at = @At("TAIL"))
        private void angellib$handleAngelLibDeserialization(ServerData data, ServerStatusResponse originalResponse,
            CallbackInfo ci) {
            AngelLib.debug("angellib$handleAngelLibDeserialization hook");
            JsonElement extra = ((IServerStatusResponse) originalResponse).getExtraData();
            AngelLib.debug(extra == null ? "null" : ((JsonElement) extra).toString());

            if (extra != null && extra.isJsonObject()) {
                S00PacketServerInfoModifyService.callDeserializeHandlers(originalResponse, data);
            }
        }
    }

    @Mixin(OldServerPinger.class)
    public static class MixinOldServerPinger {

        /*
         * @Inject(
         * method = "func_147224_a",
         * at = @At(
         * value = "INVOKE",
         * target =
         * "Lnet/minecraft/network/NetworkManager;scheduleOutboundPacket(Lnet/minecraft/network/Packet;[Lio/netty/util/concurrent/GenericFutureListener;)V",
         * ordinal = 1),
         * locals = LocalCapture.CAPTURE_FAILHARD)
         * private void injectAngelPing(ServerData server, CallbackInfo ci, ServerAddress serverAddress,
         * NetworkManager networkManager) {
         * // Construct your custom AngelPing data
         * String extra = "{\"angellib\":true,\"version\":\"1.0.0\"}";
         * AngelPing angelPing = new AngelPing(extra);
         * // Send your packet before the standard ServerQuery
         * ByteBuf wrapped = Unpooled.buffer();
         * PacketBuffer buf = new PacketBuffer(wrapped);
         * try {
         * buf.writeStringToBuffer("sneed");
         * } catch (IOException e) {
         * throw new RuntimeException(e);
         * }
         * networkManager.scheduleOutboundPacket(new C17PacketCustomPayload("sneed", buf));
         * }
         */

        @Redirect(
            method = "func_147224_a",
            at = @At(
                value = "INVOKE",
                target = "Lnet/minecraft/network/NetworkManager;scheduleOutboundPacket(Lnet/minecraft/network/Packet;[Lio/netty/util/concurrent/GenericFutureListener;)V"))
        private void redirectScheduleOutboundHandshake(NetworkManager instance, Packet packet,
            GenericFutureListener<? super Future<? super Void>>[] listeners, @Local ServerAddress serveraddress) {
            if (packet instanceof C00Handshake handshake) {
                AngelLib.debug("Sending modified C00Handshake ");
                String modifiedIp = serveraddress.getIP() + ANGEL_TOKEN;
                C00Handshake modified = new C00Handshake(
                    5,
                    modifiedIp,
                    serveraddress.getPort(),
                    EnumConnectionState.STATUS);

                instance.scheduleOutboundPacket(modified, listeners);
            } else {
                instance.scheduleOutboundPacket(packet, listeners);
            }
        }
    }

    @Mixin(NetHandlerHandshakeTCP.class)
    public static class MixinNetHandlerHandshakeTCP {

        @Shadow
        @Final
        NetworkManager field_147386_b;

        @Inject(method = "processHandshake", at = @At("HEAD"))
        public void onProcessHandshake(C00Handshake packet, CallbackInfo ci) {
            String serverIpField = ((AccessorC00Handshake) packet).getField_149598_b();

            if (serverIpField.contains(ANGEL_TOKEN)) {
                AngelLib.debug("Received AngelLib client!");
                ((INetworkManager) field_147386_b).setAngelClient();
            }
        }
    }

    @Mixin(NetworkManager.class)
    public static class MixinNetworkManager implements INetworkManager {

        @Unique
        private boolean isAngelClient = false;

        @Override
        public boolean isAngelClient() {
            return isAngelClient;
        }

        @Override
        public void setAngelClient() {
            isAngelClient = true;
        }
    }
}
