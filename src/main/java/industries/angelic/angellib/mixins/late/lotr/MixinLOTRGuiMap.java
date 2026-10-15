package industries.angelic.angellib.mixins.late.lotr;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.angellib.util.ClientUtil;

@SuppressWarnings("UnresolvedMixinReference")
@Pseudo
@Mixin(targets = "lotr.client.gui.LOTRGuiMap", remap = false)
public abstract class MixinLOTRGuiMap {

    @Unique
    private static final String ANGELLIB$RENDER_PLAYER_ICON = "renderPlayerIcon(Lcom/mojang/authlib/GameProfile;Ljava/lang/String;DDII)D";

    @Unique
    private boolean angellib$legacyMapIconSkin = true;

    @Redirect(
        method = ANGELLIB$RENDER_PLAYER_ICON,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/texture/TextureManager;bindTexture(Lnet/minecraft/util/ResourceLocation;)V",
            remap = true),
        remap = false)
    private void angellib$bindAndDetectSkinLayout(TextureManager textureManager, ResourceLocation skin) {
        textureManager.bindTexture(skin);

        int texWidth = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
        int texHeight = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
        if (texWidth > 0 && texHeight > 0) {
            this.angellib$legacyMapIconSkin = texWidth == texHeight * 2;
            return;
        }

        this.angellib$legacyMapIconSkin = !ClientUtil.useNewSkinFormat();
    }

    @ModifyArg(
        method = ANGELLIB$RENDER_PLAYER_ICON,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;addVertexWithUV(DDDDD)V",
            ordinal = 0,
            remap = true),
        index = 4,
        remap = false)
    private double angellib$modernFaceVMax0(double original) {
        return this.angellib$modernFaceVMax(original);
    }

    @ModifyArg(
        method = ANGELLIB$RENDER_PLAYER_ICON,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;addVertexWithUV(DDDDD)V",
            ordinal = 1,
            remap = true),
        index = 4,
        remap = false)
    private double angellib$modernFaceVMax1(double original) {
        return this.angellib$modernFaceVMax(original);
    }

    @ModifyArg(
        method = ANGELLIB$RENDER_PLAYER_ICON,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;addVertexWithUV(DDDDD)V",
            ordinal = 2,
            remap = true),
        index = 4,
        remap = false)
    private double angellib$modernFaceVMin2(double original) {
        return this.angellib$modernFaceVMin(original);
    }

    @ModifyArg(
        method = ANGELLIB$RENDER_PLAYER_ICON,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;addVertexWithUV(DDDDD)V",
            ordinal = 3,
            remap = true),
        index = 4,
        remap = false)
    private double angellib$modernFaceVMin3(double original) {
        return this.angellib$modernFaceVMin(original);
    }

    @ModifyArg(
        method = ANGELLIB$RENDER_PLAYER_ICON,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;addVertexWithUV(DDDDD)V",
            ordinal = 4,
            remap = true),
        index = 4,
        remap = false)
    private double angellib$modernHatVMax4(double original) {
        return this.angellib$modernFaceVMax(original);
    }

    @ModifyArg(
        method = ANGELLIB$RENDER_PLAYER_ICON,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;addVertexWithUV(DDDDD)V",
            ordinal = 5,
            remap = true),
        index = 4,
        remap = false)
    private double angellib$modernHatVMax5(double original) {
        return this.angellib$modernFaceVMax(original);
    }

    @ModifyArg(
        method = ANGELLIB$RENDER_PLAYER_ICON,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;addVertexWithUV(DDDDD)V",
            ordinal = 6,
            remap = true),
        index = 4,
        remap = false)
    private double angellib$modernHatVMin6(double original) {
        return this.angellib$modernFaceVMin(original);
    }

    @ModifyArg(
        method = ANGELLIB$RENDER_PLAYER_ICON,
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/Tessellator;addVertexWithUV(DDDDD)V",
            ordinal = 7,
            remap = true),
        index = 4,
        remap = false)
    private double angellib$modernHatVMin7(double original) {
        return this.angellib$modernFaceVMin(original);
    }

    @Unique
    private double angellib$modernFaceVMin(double original) {
        return this.angellib$legacyMapIconSkin ? original : 0.125D;
    }

    @Unique
    private double angellib$modernFaceVMax(double original) {
        return this.angellib$legacyMapIconSkin ? original : 0.25D;
    }
}
