package industries.angelic.angellib.util;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.junit.Assert;
import org.junit.Test;

public class ApngUtilTest {

    @Test
    public void framesRoundTripExactly() throws IOException {
        BufferedImage[] frames = { pattern(0), pattern(1), pattern(1), pattern(2) };
        byte[] apng = ApngUtil.write(frames, new int[] { 50, 120, 120, 300 }, 0);

        Assert.assertTrue(ApngUtil.isPng(apng));
        Assert.assertTrue(ApngUtil.isAnimated(apng));
        GifUtil.GifData read = ApngUtil.read(apng);
        Assert.assertEquals(4, read.getFrameCount());
        Assert.assertEquals(50, read.getDelayMs(0));
        Assert.assertEquals(120, read.getDelayMs(1));
        Assert.assertEquals(300, read.getDelayMs(3));
        for (int i = 0; i < frames.length; i++) {
            assertSamePixels("frame " + i, frames[i], read.getFrame(i));
        }
    }

    @Test
    public void programsWithoutApngSeeTheFirstFrame() throws IOException {
        BufferedImage[] frames = { pattern(0), pattern(1) };
        byte[] apng = ApngUtil.write(frames, new int[] { 100, 100 }, 0);
        BufferedImage still = ImageIO.read(new java.io.ByteArrayInputStream(apng));
        assertSamePixels("default image", frames[0], still);
    }

    @Test
    public void onlyChangedRegionsAreStored() throws IOException {
        BufferedImage big = pattern(0);
        byte[] one = ApngUtil.write(new BufferedImage[] { big }, new int[] { 100 }, 0);
        byte[] many = ApngUtil
            .write(new BufferedImage[] { big, big, big, big, big }, new int[] { 100, 100, 100, 100, 100 }, 0);
        // Four unchanged frames cost a few dozen bytes each, not another copy of the picture.
        Assert.assertTrue(many.length - one.length < 4 * 200);
    }

    @Test
    public void largeApngsCanBeReadSmaller() throws IOException {
        BufferedImage[] frames = { scaled(pattern(0), 4), scaled(pattern(1), 4) };
        GifUtil.GifData read = ApngUtil.read(ApngUtil.write(frames, new int[] { 100, 100 }, 0), 64, 64);
        Assert.assertEquals(
            64,
            read.getFrame(0)
                .getWidth());
        Assert.assertEquals(
            32,
            read.getFrame(0)
                .getHeight());
    }

    @Test
    public void plainPngsAreNotAnimated() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(pattern(0), "png", out);
        byte[] png = out.toByteArray();
        Assert.assertTrue(ApngUtil.isPng(png));
        Assert.assertFalse(ApngUtil.isAnimated(png));
        Assert.assertEquals(
            1,
            ApngUtil.read(png)
                .getFrameCount());
    }

    /** A 64x32 picture with a transparent corner and a moving bar, so frames differ only in part. */
    private static BufferedImage pattern(int step) {
        BufferedImage image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 64; x++) {
                int argb = x > 56 && y > 26 ? 0 : 0xFF000000 | x * 4 << 16 | y * 8 << 8 | 0x40;
                if (x >= 4 + step * 6 && x < 8 + step * 6 && y >= 4 && y < 20) argb = 0x80FFFFFF;
                image.setRGB(x, y, argb);
            }
        }
        return image;
    }

    private static BufferedImage scaled(BufferedImage image, int factor) {
        BufferedImage out = new BufferedImage(
            image.getWidth() * factor,
            image.getHeight() * factor,
            BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < out.getHeight(); y++) {
            for (int x = 0; x < out.getWidth(); x++) {
                out.setRGB(x, y, image.getRGB(x / factor, y / factor));
            }
        }
        return out;
    }

    private static void assertSamePixels(String what, BufferedImage expected, BufferedImage actual) {
        Assert.assertEquals(what + " width", expected.getWidth(), actual.getWidth());
        Assert.assertEquals(what + " height", expected.getHeight(), actual.getHeight());
        for (int y = 0; y < expected.getHeight(); y++) {
            for (int x = 0; x < expected.getWidth(); x++) {
                int e = expected.getRGB(x, y);
                int a = actual.getRGB(x, y);
                if (e >>> 24 == 0 && a >>> 24 == 0) continue;
                Assert.assertEquals(what + " at " + x + "," + y, Integer.toHexString(e), Integer.toHexString(a));
            }
        }
    }
}
