package industries.angelic.angellib.mixins.late.modularui2;

import net.minecraft.client.gui.GuiScreen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import com.cleanroommc.modularui.screen.ModularPanel;

import industries.angelic.angellib.mixininterfaces.IModularPanel;

@Mixin(ModularPanel.class)
public class MixinModularPanel implements IModularPanel {

    @Unique
    GuiScreen guiScreen;

    @Override
    public void setGuiScreen(GuiScreen guiScreen) {
        this.guiScreen = guiScreen;
    }

    @Override
    public GuiScreen getGuiScreen() {
        return this.guiScreen;
    }
}
