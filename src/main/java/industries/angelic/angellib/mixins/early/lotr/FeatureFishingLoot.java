package industries.angelic.angellib.mixins.early.lotr;

import java.util.Random;

import net.minecraft.entity.projectile.EntityFishHook;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import industries.angelic.angellib.Config;
import industries.angelic.angellib.services.fishing.FishingLootInterop;

@Pseudo
public class FeatureFishingLoot {

    @Pseudo
    @Mixin(targets = "lotr.common.entity.projectile.LOTREntityFishHook", remap = false)
    public abstract static class MixinLOTREntityFishHook extends EntityFishHook {

        protected MixinLOTREntityFishHook(World world) {
            super(world);
        }

        @Inject(method = "func_146034_e()I", at = @At("HEAD"), remap = false)
        private void angellib$captureLotrFishingBiome(CallbackInfoReturnable<Integer> cir) {
            if (!Config.enableFishingLootTable || this.worldObj == null) {
                return;
            }

            BiomeGenBase biome = this.worldObj
                .getBiomeGenForCoords(MathHelper.floor_double(this.posX), MathHelper.floor_double(this.posZ));
            FishingLootInterop.setLotrFishingBiome(biome);
        }

        @Inject(method = "func_146034_e()I", at = @At("RETURN"), remap = false)
        private void angellib$clearLotrFishingBiome(CallbackInfoReturnable<Integer> cir) {
            FishingLootInterop.clearLotrFishingBiome();
        }
    }

    @Pseudo
    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Mixin(targets = "lotr.common.entity.projectile.LOTRFishing", remap = false)
    public abstract static class MixinLOTRFishing {

        @Inject(
            method = "getFishResult(Ljava/util/Random;FIIZ)Llotr/common/entity/projectile/LOTRFishing$FishResult;",
            at = @At("RETURN"),
            cancellable = true,
            remap = false)
        private static void angellib$overrideLotrFishingLoot(Random rand, float chance, int luck, int speed,
            boolean allowJunkTreasure, CallbackInfoReturnable cir) {
            if (!Config.enableFishingLootTable) {
                return;
            }

            Object overriddenResult = FishingLootInterop.overrideLotrFishResult(cir.getReturnValue(), rand);
            if (overriddenResult != null) {
                cir.setReturnValue(overriddenResult);
            }
        }
    }
}
