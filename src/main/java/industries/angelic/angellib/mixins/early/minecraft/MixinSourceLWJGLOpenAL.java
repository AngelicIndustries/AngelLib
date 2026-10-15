package industries.angelic.angellib.mixins.early.minecraft;

import java.nio.IntBuffer;

import org.lwjgl.openal.AL10;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import industries.angelic.angellib.util.sound.SoundUtil;
import paulscode.sound.Channel;
import paulscode.sound.Source;
import paulscode.sound.libraries.ChannelLWJGLOpenAL;
import paulscode.sound.libraries.SourceLWJGLOpenAL;

@Mixin(value = SourceLWJGLOpenAL.class, remap = false)
public abstract class MixinSourceLWJGLOpenAL {

    @Unique
    private static final int angellib$AL_SOURCE_RADIUS = 0x1031;
    @Unique
    private static final String angellib$SOURCE_RADIUS_EXTENSION = "AL_EXT_SOURCE_RADIUS";
    @Unique
    private float angellib$sourceRadius = Float.NaN;

    @Inject(
        method = "play",
        at = @At(value = "INVOKE", target = "Lpaulscode/sound/Source;play(Lpaulscode/sound/Channel;)V"))
    private void angellib$resolveSourceRadius(Channel channel, CallbackInfo ci) {
        if (Float.isNaN(this.angellib$sourceRadius)) {
            String sourceName = ((Source) (Object) this).sourcename;
            this.angellib$sourceRadius = SoundUtil.takeSourceRadius(sourceName);
        }
    }

    @Inject(
        method = "play",
        at = @At(
            value = "INVOKE",
            target = "Lpaulscode/sound/Source;play(Lpaulscode/sound/Channel;)V",
            shift = At.Shift.AFTER))
    private void angellib$applySourceRadius(Channel channel, CallbackInfo ci) {
        if (!(channel instanceof ChannelLWJGLOpenAL)) {
            return;
        }

        IntBuffer alSource = ((ChannelLWJGLOpenAL) channel).ALSource;
        if (alSource == null || alSource.capacity() == 0) {
            return;
        }

        try {
            if (AL10.alIsExtensionPresent(angellib$SOURCE_RADIUS_EXTENSION)) {
                AL10.alSourcef(alSource.get(0), angellib$AL_SOURCE_RADIUS, this.angellib$sourceRadius);
            }
        } catch (Throwable ignored) {}
    }
}
