package industries.angelic.angellib.mixininterfaces;

import net.minecraft.client.gui.GuiScreen;

public interface IModularPanel {

    void setGuiScreen(GuiScreen guiScreen);

    GuiScreen getGuiScreen();
}
