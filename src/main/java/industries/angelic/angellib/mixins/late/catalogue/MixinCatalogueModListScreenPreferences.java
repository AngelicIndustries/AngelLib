package industries.angelic.angellib.mixins.late.catalogue;

import java.util.Comparator;

import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.catalogue.client.screen.CatalogueModListScreen;

import industries.angelic.angellib.Config;
import industries.angelic.angellib.Config.CatalogueSortOrder;

@Mixin(CatalogueModListScreen.class)
public abstract class MixinCatalogueModListScreenPreferences {

    @Shadow(remap = false)
    @Final
    private static Comparator<?> SORT_ALPHABETICALLY;

    @Shadow(remap = false)
    @Final
    private static Comparator<?> SORT_ALPHABETICALLY_REVERSED;

    @Shadow(remap = false)
    @Final
    private static Comparator<?> SORT_FAVOURITES_FIRST;

    @Shadow(remap = false)
    @Final
    private static MutableBoolean OPTION_CONFIGS_ONLY;

    @Shadow(remap = false)
    @Final
    private static MutableBoolean OPTION_FAVOURITES_ONLY;

    @Shadow(remap = false)
    @Final
    private static MutableBoolean OPTION_HIDE_LIBRARIES;

    @Shadow(remap = false)
    @Final
    private static MutableBoolean OPTION_HIDE_CHILD_MODS;

    @Shadow(remap = false)
    @Final
    private static MutableObject<Object> OPTION_SORT;

    @Inject(method = "initGui", at = @At("HEAD"), remap = true)
    private void angellib$restorePreferences(CallbackInfo ci) {
        Config.refreshCataloguePreferences();
        if (!Config.persistCataloguePreferences) {
            return;
        }

        OPTION_SORT.setValue(angellib$getComparator(Config.catalogueSortOrder));
        OPTION_CONFIGS_ONLY.setValue(Config.catalogueConfigsOnly);
        OPTION_FAVOURITES_ONLY.setValue(Config.catalogueFavouritesOnly);
        OPTION_HIDE_LIBRARIES.setValue(Config.catalogueHideLibraries);
        OPTION_HIDE_CHILD_MODS.setValue(Config.catalogueHideChildMods);
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"), remap = true)
    private void angellib$savePreferences(CallbackInfo ci) {
        angellib$savePreferencesToConfig();
    }

    @Inject(method = "mouseClicked", at = @At("TAIL"), remap = true)
    private void angellib$savePreferencesAfterClick(int mouseX, int mouseY, int mouseButton, CallbackInfo ci) {
        angellib$savePreferencesToConfig();
    }

    private static void angellib$savePreferencesToConfig() {
        if (!Config.persistCataloguePreferences) {
            return;
        }

        Config.saveCataloguePreferences(
            angellib$getSortOrder(),
            OPTION_CONFIGS_ONLY.booleanValue(),
            OPTION_FAVOURITES_ONLY.booleanValue(),
            OPTION_HIDE_LIBRARIES.booleanValue(),
            OPTION_HIDE_CHILD_MODS.booleanValue());
    }

    private static Comparator<?> angellib$getComparator(CatalogueSortOrder sortOrder) {
        switch (sortOrder) {
            case ALPHABETICALLY_REVERSED:
                return SORT_ALPHABETICALLY_REVERSED;
            case FAVOURITES_FIRST:
                return SORT_FAVOURITES_FIRST;
            case ALPHABETICALLY:
            default:
                return SORT_ALPHABETICALLY;
        }
    }

    private static CatalogueSortOrder angellib$getSortOrder() {
        Object selectedComparator = OPTION_SORT.getValue();
        if (selectedComparator == SORT_ALPHABETICALLY_REVERSED) {
            return CatalogueSortOrder.ALPHABETICALLY_REVERSED;
        }
        if (selectedComparator == SORT_FAVOURITES_FIRST) {
            return CatalogueSortOrder.FAVOURITES_FIRST;
        }
        if (selectedComparator == SORT_ALPHABETICALLY) {
            return CatalogueSortOrder.ALPHABETICALLY;
        }
        return Config.catalogueSortOrder;
    }
}
