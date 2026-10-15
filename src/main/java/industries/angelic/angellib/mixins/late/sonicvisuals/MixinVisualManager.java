package industries.angelic.angellib.mixins.late.sonicvisuals;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.angellib.AngelLib;

@Pseudo
@Mixin(targets = "com.sonicjumper.enhancedvisuals.visuals.VisualManager", remap = false)
public class MixinVisualManager {

    @Unique
    private static boolean angellib$loggedInvalidVisualLifetime = false;

    @Redirect(
        method = "addVisualsWithShading",
        at = @At(value = "INVOKE", target = "Ljava/util/Random;nextInt(I)I"),
        remap = false)
    private int angellib$sanitizeNextIntBound(Random random, int bound) {
        if (bound <= 0) {
            this.angellib$logInvalidVisualLifetime("random bound " + bound);
            return 0;
        }
        return random.nextInt(bound);
    }

    @ModifyArg(
        method = "addVisualsWithShading",
        at = @At(
            value = "INVOKE",
            target = "Lcom/sonicjumper/enhancedvisuals/visuals/Animation;<init>(Lcom/sonicjumper/enhancedvisuals/visuals/VisualType;ILjava/awt/Color;)V"),
        index = 1,
        remap = false)
    private int angellib$sanitizeAnimationLifetime(int lifetime) {
        return this.angellib$sanitizeLifetime(lifetime);
    }

    @ModifyArg(
        method = "addVisualsWithShading",
        at = @At(
            value = "INVOKE",
            target = "Lcom/sonicjumper/enhancedvisuals/visuals/Overlay;<init>(Lcom/sonicjumper/enhancedvisuals/visuals/VisualType;ILjava/awt/Color;)V"),
        index = 1,
        remap = false)
    private int angellib$sanitizeOverlayLifetime(int lifetime) {
        return this.angellib$sanitizeLifetime(lifetime);
    }

    @ModifyArg(
        method = "addVisualsWithShading",
        at = @At(
            value = "INVOKE",
            target = "Lcom/sonicjumper/enhancedvisuals/visuals/Splat;<init>(Lcom/sonicjumper/enhancedvisuals/visuals/VisualType;ILjava/awt/Color;)V"),
        index = 1,
        remap = false)
    private int angellib$sanitizeSplatLifetime(int lifetime) {
        return this.angellib$sanitizeLifetime(lifetime);
    }

    @ModifyArg(
        method = "addVisualsWithShading",
        at = @At(
            value = "INVOKE",
            target = "Lcom/sonicjumper/enhancedvisuals/visuals/Visual;<init>(Lcom/sonicjumper/enhancedvisuals/visuals/VisualType;ILjava/awt/Color;)V"),
        index = 1,
        remap = false)
    private int angellib$sanitizeBaseLifetime(int lifetime) {
        return this.angellib$sanitizeLifetime(lifetime);
    }

    @Unique
    private int angellib$sanitizeLifetime(int lifetime) {
        if (lifetime < 0) {
            this.angellib$logInvalidVisualLifetime("lifetime " + lifetime);
            return 0;
        }
        return lifetime;
    }

    @Unique
    private void angellib$logInvalidVisualLifetime(String detail) {
        if (angellib$loggedInvalidVisualLifetime) {
            return;
        }
        angellib$loggedInvalidVisualLifetime = true;
        AngelLib.LOG.warn("Sanitized invalid Enhanced Visuals value ({}) to avoid a crash", detail);
    }
}
