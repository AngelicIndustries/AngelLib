package industries.angelic.angellib.mixins.late.serverutilities;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.angellib.util.ClientUtil;

@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(targets = "serverutils.client.tab.ModernTabRenderer", priority = 999, remap = false)
public abstract class MixinModernTabRenderer {

    @Unique
    private ResourceLocation angellib$currentTabSkin;

    @Redirect(
        method = "render(Lnet/minecraft/client/gui/ScaledResolution;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V",
            ordinal = 0,
            remap = true),
        remap = false)
    private void angellib$captureTabSkin(TextureManager textureManager, ResourceLocation skin) {
        this.angellib$currentTabSkin = skin;
    }

    @Redirect(
        method = "render(Lnet/minecraft/client/gui/ScaledResolution;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;func_152125_a(IIFFIIIIFF)V",
            ordinal = 0,
            remap = true),
        remap = false)
    private void angellib$drawModernTabFace(int x, int y, float u, float v, int uWidth, int vHeight, int width,
        int height, float tileWidth, float tileHeight) {
        ClientUtil.drawPlayerFace(this.angellib$currentTabSkin, null, x, y, width, height, 1.0F);
    }

    @Redirect(
        method = "render(Lnet/minecraft/client/gui/ScaledResolution;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/Gui;func_152125_a(IIFFIIIIFF)V",
            ordinal = 1,
            remap = true),
        remap = false)
    private void angellib$skipLegacyTabHat(int x, int y, float u, float v, int uWidth, int vHeight, int width,
        int height, float tileWidth, float tileHeight) {
        this.angellib$currentTabSkin = null;
    }
}
