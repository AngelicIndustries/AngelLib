package industries.angelic.angellib.mixins.late.catalogue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.cleanroommc.catalogue.client.data.IModData;
import com.cleanroommc.catalogue.client.screen.CatalogueModListScreen;

import industries.angelic.angellib.AngelLib;
import industries.angelic.angellib.util.GifUtil;

@Mixin(CatalogueModListScreen.class)
public abstract class MixinCatalogueModListScreenBackground {

    @Shadow(remap = false)
    private IModData selectedModData;

    @Unique
    private static final ResourceLocation angellib$backgroundGif = new ResourceLocation(
        AngelLib.MODID,
        "background.gif");

    @Unique
    private static volatile boolean angellib$backgroundLoading = false;

    @Unique
    private static volatile boolean angellib$backgroundLoadFailed = false;

    @Unique
    private static BackgroundAnimation angellib$backgroundAnimation;

    @Unique
    private static volatile int angellib$backgroundGeneration = 0;

    @Unique
    private static final int angellib$atlasMaxWidth = 8192;
    @Unique
    private static final int angellib$atlasFrameWidth = 128;
    @Unique
    private static final int angellib$atlasFrameHeight = 64;

    @Inject(method = "drawBackground", at = @At("HEAD"), cancellable = true, remap = false)
    private void angellib$drawAnimatedBackgroundIfLoaded(int width, int left, int top, CallbackInfo ci) {
        if (selectedModData == null || !AngelLib.MODID.equals(selectedModData.getModId())) {
            return;
        }

        if (angellib$backgroundAnimation != null && angellib$backgroundAnimation.pageLocations.length > 0) {
            angellib$renderAnimatedBackground(width, left, top);
            ci.cancel();
            return;
        }

        angellib$queueBackgroundLoad();
    }

    @Inject(method = "onGuiClosed", at = @At("HEAD"), remap = true)
    private void angellib$cleanupBackgroundOnClose(CallbackInfo ci) {
        AngelLib.debug("Cleaning up catalogue animated gif");
        angellib$backgroundGeneration++;
        angellib$backgroundLoading = false;
        angellib$backgroundLoadFailed = false;
        BackgroundAnimation anim = angellib$backgroundAnimation;
        if (anim != null) {
            Minecraft mc = Minecraft.getMinecraft();
            for (ResourceLocation location : anim.pageLocations) {
                mc.getTextureManager()
                    .deleteTexture(location);
            }
            angellib$backgroundAnimation = null;
        }
    }

    @Unique
    private void angellib$renderAnimatedBackground(int width, int left, int top) {
        BackgroundAnimation data = angellib$backgroundAnimation;
        int frameDelay = Math.max(1, data.frameDelayMs);
        int frameCount = Math.max(1, data.frameCount);
        int frameIndex = (int) ((Minecraft.getSystemTime() / frameDelay) % frameCount);

        int page = frameIndex / data.framesPerPage;
        int localFrame = frameIndex % data.framesPerPage;
        int pageFrameCount = data.pageFrameCounts[page];
        float u0 = (float) (localFrame * data.frameWidth) / (float) (data.frameWidth * pageFrameCount);
        float u1 = (float) ((localFrame + 1) * data.frameWidth) / (float) (data.frameWidth * pageFrameCount);

        Minecraft.getMinecraft()
            .getTextureManager()
            .bindTexture(data.pageLocations[page]);

        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glShadeModel(GL11.GL_SMOOTH);
        OpenGlHelper.glBlendFunc(770, 771, 1, 0);
        GL11.glPushMatrix();
        GL11.glTranslatef(left, top, 0.0F);
        GL11.glScalef(width / (float) data.frameWidth, 128.0F / (float) data.frameHeight, 1.0F);

        Tessellator t = Tessellator.instance;
        t.startDrawingQuads();
        t.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F);
        t.setTextureUV(u0, 0.0D);
        t.addVertex(0.0D, 0.0D, 0.0D);
        t.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.0F);
        t.setTextureUV(u0, 1.0D);
        t.addVertex(0.0D, data.frameHeight, 0.0D);
        t.setColorRGBA_F(0.0F, 0.0F, 0.0F, 0.0F);
        t.setTextureUV(u1, 1.0D);
        t.addVertex(data.frameWidth, data.frameHeight, 0.0D);
        t.setColorRGBA_F(1.0F, 1.0F, 1.0F, 1.0F);
        t.setTextureUV(u1, 0.0D);
        t.addVertex(data.frameWidth, 0.0D, 0.0D);
        t.draw();
        GL11.glPopMatrix();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glShadeModel(GL11.GL_FLAT);
    }

    @Unique
    private static void angellib$queueBackgroundLoad() {
        if (angellib$backgroundLoading || angellib$backgroundLoadFailed || AngelLib.varInstanceClient == null) {
            return;
        }

        angellib$backgroundLoading = true;
        final int expectedGeneration = angellib$backgroundGeneration;
        Minecraft mc = Minecraft.getMinecraft();
        AngelLib.varInstanceClient.gifloaderPool.submit(() -> {
            try (InputStream stream = mc.getResourceManager()
                .getResource(angellib$backgroundGif)
                .getInputStream()) {
                byte[] gifBytes = angellib$readAllBytes(stream);
                BackgroundAnimationData animationData = angellib$buildPagedBackgroundData(gifBytes);
                mc.func_152344_a(() -> {
                    try {
                        if (expectedGeneration != angellib$backgroundGeneration) {
                            return;
                        }
                        int pageCount = animationData.pages.size();
                        DynamicTexture[] pageTextures = new DynamicTexture[pageCount];
                        ResourceLocation[] pageLocations = new ResourceLocation[pageCount];
                        for (int i = 0; i < pageCount; i++) {
                            DynamicTexture tex = new DynamicTexture(animationData.pages.get(i));
                            pageTextures[i] = tex;
                            pageLocations[i] = mc.getTextureManager()
                                .getDynamicTextureLocation("angellib_catalogue_background_" + i, tex);
                        }
                        angellib$backgroundAnimation = new BackgroundAnimation(
                            pageTextures,
                            pageLocations,
                            animationData.pageFrameCounts,
                            animationData.frameWidth,
                            animationData.frameHeight,
                            animationData.frameCount,
                            animationData.frameDelayMs,
                            animationData.framesPerPage);
                    } finally {
                        angellib$backgroundLoading = false;
                    }
                });
            } catch (Exception e) {
                AngelLib.LOG.error("Failed to load Catalogue animated background GIF", e);
                angellib$backgroundLoadFailed = true;
                angellib$backgroundLoading = false;
            }
        });
    }

    @Unique
    private static byte[] angellib$readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) >= 0) {
            out.write(buffer, 0, n);
        }
        return out.toByteArray();
    }

    @Unique
    private static BackgroundAnimationData angellib$buildPagedBackgroundData(byte[] gifBytes) throws IOException {
        GifUtil.GifData gif = GifUtil.readGif(gifBytes);
        int frameWidth = angellib$atlasFrameWidth;
        int frameHeight = angellib$atlasFrameHeight;
        int frameCount = gif.getFrameCount();
        int frameDelayMs = GifUtil.getFrameDelay(gif);

        int framesPerPage = Math.max(1, angellib$atlasMaxWidth / frameWidth);
        int pageCount = (frameCount + framesPerPage - 1) / framesPerPage;
        int[] pageFrameCounts = new int[pageCount];
        List<BufferedImage> pages = new ArrayList<>(pageCount);

        for (int page = 0; page < pageCount; page++) {
            int pageStartFrame = page * framesPerPage;
            int pageFrames = Math.min(framesPerPage, frameCount - pageStartFrame);
            pageFrameCounts[page] = pageFrames;
            BufferedImage pageImage = new BufferedImage(
                frameWidth * pageFrames,
                frameHeight,
                BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = pageImage.createGraphics();
            try {
                for (int i = 0; i < pageFrames; i++) {
                    int srcFrame = pageStartFrame + i;
                    BufferedImage frame = GifUtil.scaleFrame(gif.getFrame(srcFrame), frameWidth, frameHeight);
                    g.drawImage(frame, i * frameWidth, 0, null);
                }
            } finally {
                g.dispose();
            }
            pages.add(pageImage);
        }
        return new BackgroundAnimationData(
            pages,
            pageFrameCounts,
            frameWidth,
            frameHeight,
            frameCount,
            frameDelayMs,
            framesPerPage);
    }

    @Unique
    private static class BackgroundAnimationData {

        private final List<BufferedImage> pages;
        private final int[] pageFrameCounts;
        private final int frameWidth;
        private final int frameHeight;
        private final int frameCount;
        private final int frameDelayMs;
        private final int framesPerPage;

        private BackgroundAnimationData(List<BufferedImage> pages, int[] pageFrameCounts, int frameWidth,
            int frameHeight, int frameCount, int frameDelayMs, int framesPerPage) {
            this.pages = pages;
            this.pageFrameCounts = pageFrameCounts;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            this.frameCount = frameCount;
            this.frameDelayMs = frameDelayMs;
            this.framesPerPage = framesPerPage;
        }
    }

    @Unique
    private static class BackgroundAnimation {

        private final DynamicTexture[] pageTextures;
        private final ResourceLocation[] pageLocations;
        private final int[] pageFrameCounts;
        private final int frameWidth;
        private final int frameHeight;
        private final int frameCount;
        private final int frameDelayMs;
        private final int framesPerPage;

        private BackgroundAnimation(DynamicTexture[] pageTextures, ResourceLocation[] pageLocations,
            int[] pageFrameCounts, int frameWidth, int frameHeight, int frameCount, int frameDelayMs,
            int framesPerPage) {
            this.pageTextures = pageTextures;
            this.pageLocations = pageLocations;
            this.pageFrameCounts = pageFrameCounts;
            this.frameWidth = frameWidth;
            this.frameHeight = frameHeight;
            this.frameCount = frameCount;
            this.frameDelayMs = frameDelayMs;
            this.framesPerPage = framesPerPage;
        }
    }
}
