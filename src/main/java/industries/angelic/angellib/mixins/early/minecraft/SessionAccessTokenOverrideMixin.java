package industries.angelic.angellib.mixins.early.minecraft;

import net.minecraft.util.Session;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Session.class)
public abstract class SessionAccessTokenOverrideMixin {

    private static final String TOKEN_OVERRIDE_PROPERTY = "angellib.accessTokenOverride";

    @Shadow
    @Final
    private String token;

    @Inject(method = "getToken", at = @At("HEAD"), cancellable = true)
    private void angellib$overrideAccessToken(CallbackInfoReturnable<String> cir) {
        String override = System.getProperty(TOKEN_OVERRIDE_PROPERTY);
        if (override == null || override.isEmpty()) {
            return;
        }
        cir.setReturnValue(override);
    }
}
