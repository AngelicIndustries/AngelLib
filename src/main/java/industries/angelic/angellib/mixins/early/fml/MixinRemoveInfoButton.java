package industries.angelic.angellib.mixins.early.fml;

import java.util.Iterator;
import java.util.List;

import net.minecraft.client.gui.GuiButton;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import cpw.mods.fml.client.GuiModList;
import industries.angelic.angellib.Config;
import industries.angelic.angellib.mixins.early.minecraft.AccessorGuiScreen;

@Mixin(GuiModList.class)
public class MixinRemoveInfoButton {

    @Inject(method = "initGui", at = @At("TAIL"))
    private void onInitGui(CallbackInfo ci) {
        if (!Config.disableEnderCoreInfoButton) {
            return;
        }

        List<GuiButton> buttons = ((AccessorGuiScreen) this).getButtonList();

        Iterator<GuiButton> iter = buttons.iterator();
        while (iter.hasNext()) {
            GuiButton button = iter.next();
            if (button.getClass()
                .getSimpleName()
                .equals("InfoButton")) {
                iter.remove();
                break;
            }
        }
    }
}
