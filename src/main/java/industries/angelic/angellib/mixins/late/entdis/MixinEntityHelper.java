package industries.angelic.angellib.mixins.late.entdis;

import java.util.List;

import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import industries.angelic.angellib.Config;
import industries.angelic.angellib.compat.EntityDismembermentCompat;

@Pseudo
@Mixin(targets = "com.robotsquid.entdis.core.EntityHelper", remap = false)
public abstract class MixinEntityHelper {

    // ModelModRenderer assumes cubeList.get(0) exists. In production its fallback catches
    // that failure and rethrows the earlier, misleading NoSuchFieldException: textureOffsetX.
    @Redirect(
        method = "dismember",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/model/ModelBase;boxList:Ljava/util/List;",
            remap = true),
        require = 2)
    private static List<ModelRenderer> angellib$skipEmptyParts(ModelBase model) {
        return Config.fixEntityDismembermentEmptyParts ? EntityDismembermentCompat.renderableParts(model.boxList)
            : model.boxList;
    }

    @Redirect(
        method = "dismember",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/model/ModelRenderer;childModels:Ljava/util/List;",
            remap = true),
        require = 4)
    private static List<ModelRenderer> angellib$skipEmptyChildren(ModelRenderer part) {
        return Config.fixEntityDismembermentEmptyParts ? EntityDismembermentCompat.renderableParts(part.childModels)
            : part.childModels;
    }
}
