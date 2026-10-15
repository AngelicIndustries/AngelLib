package industries.angelic.angellib.mixins.early.minecraft;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import industries.angelic.angellib.util.NetworkAddressUtil;

@Mixin(value = EntityPlayerMP.class, priority = 999)
public class MixinEntityPlayerMPIpv6 {

    @Shadow
    public NetHandlerPlayServer playerNetServerHandler;

    /**
     * @author AngelLib
     * @reason IPv6-safe player IP extraction
     */
    @Overwrite
    public String getPlayerIP() {
        return NetworkAddressUtil.extractIp(this.playerNetServerHandler.netManager.getSocketAddress());
    }
}
