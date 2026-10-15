package industries.angelic.angellib.mixins.early.minecraft;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.angellib.AngelLib;
import industries.angelic.angellib.Config;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftGuiOpenLogger {

    @Shadow
    public GuiScreen currentScreen;

    @Inject(method = "displayGuiScreen", at = @At("HEAD"))
    private void angellib$logOpenedGui(GuiScreen guiScreen, CallbackInfo ci) {
        if (!Config.logOpenedGuis) {
            return;
        }

        AngelLib.LOG.info(
            "[GUI Debug] displayGuiScreen: {} -> {}",
            angellib$getGuiName(this.currentScreen),
            angellib$getGuiName(guiScreen));
    }

    @Unique
    private static String angellib$getGuiName(GuiScreen guiScreen) {
        return guiScreen == null ? "null"
            : guiScreen.getClass()
                .getName();
    }
}
