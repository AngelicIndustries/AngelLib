package industries.angelic.angellib.mixins.early.minecraft;

import java.util.regex.Matcher;

import net.minecraft.command.ICommandSender;
import net.minecraft.command.server.CommandPardonIp;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.angellib.util.NetworkAddressUtil;

/** Relax vanilla's IPv4-only regex so /pardon-ip accepts IPv6 addresses. */
@Mixin(CommandPardonIp.class)
public abstract class MixinCommandPardonIpIpv6 {

    @Redirect(
        method = "processCommand",
        at = @At(value = "INVOKE", target = "Ljava/util/regex/Matcher;matches()Z", remap = false))
    private boolean angellib$looksLikeIp(Matcher matcher, ICommandSender sender, String[] args) {
        return NetworkAddressUtil.looksLikeIp(args[0]);
    }
}
