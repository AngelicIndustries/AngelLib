package industries.angelic.angellib.util;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import javax.imageio.ImageIO;

/**
 * Animated PNG (APNG): reading into {@link GifUtil.GifData}, like GIFs and animated WebP, and writing.
 * <p>
 * An APNG is a PNG whose extra chunks hold more frames, so programs without APNG support show its first frame. Each
 * frame is decoded on its own by wrapping its image data in a minimal PNG, then composited onto the canvas as the
 * frame control chunk says. Written APNGs store only the part of each frame that changed since the frame before.
 */
public final class ApngUtil {

    private static final byte[] SIGNATURE = { (byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n' };
    private static final int DISPOSE_NONE = 0;
    private static final int DISPOSE_BACKGROUND = 1;
    private static final int DISPOSE_PREVIOUS = 2;
    private static final int BLEND_SOURCE = 0;
    /** Used for frames that ask for no delay, as browsers do. */
    private static final int DEFAULT_DELAY_MS = 100;

    private ApngUtil() {}

    public static boolean isPng(byte[] data) {
        if (data == null || data.length < SIGNATURE.length) return false;
        for (int i = 0; i < SIGNATURE.length; i++) {
            if (data[i] != SIGNATURE[i]) return false;
        }
        return true;
    }

    /** Whether the PNG is animated: it has an animation control chunk before its image data. */
    public static boolean isAnimated(byte[] data) {
        if (!isPng(data)) return false;
        int offset = SIGNATURE.length;
        while (offset + 8 <= data.length) {
            int length = readInt(data, offset);
            String type = new String(data, offset + 4, 4, StandardCharsets.ISO_8859_1);
            if ("acTL".equals(type)) return true;
            if ("IDAT".equals(type) || "IEND".equals(type) || length < 0) return false;
            offset += 12 + length;
        }
        return false;
    }

    /** Every frame at the APNG's size, with its delay. */
    public static GifUtil.GifData read(byte[] data) throws IOException {
        return read(data, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /**
     * Every frame, scaled down to fit {@code maxWidth} x {@code maxHeight} when larger, with its delay. Frames are
     * scaled as they are made, so a large APNG never holds all its frames at full size.
     */
    public static GifUtil.GifData read(byte[] data, int maxWidth, int maxHeight) throws IOException {
        if (!isPng(data)) throw new IOException("Not a PNG.");
        List<Chunk> chunks = chunks(data);
        Chunk header = null;
        List<Chunk> shared = new ArrayList<>();
        List<Frame> frames = new ArrayList<>();
        Frame current = null;
        boolean seenImageData = false;
        for (Chunk chunk : chunks) {
            switch (chunk.type) {
                case "IHDR":
                    header = chunk;
                    break;
                case "acTL":
                case "IEND":
                    break;
                case "fcTL":
                    current = new Frame(chunk.data);
                    frames.add(current);
                    break;
                case "IDAT":
                    seenImageData = true;
                    // Image data before any frame control is a still default image, not part of the animation.
                    if (current != null) current.data.write(chunk.data, 0, chunk.data.length);
                    break;
                case "fdAT":
                    if (current != null && chunk.data.length > 4) {
                        current.data.write(chunk.data, 4, chunk.data.length - 4);
                    }
                    break;
                default:
                    // Palette, transparency, color space: every frame's PNG needs the ones before the image data.
                    if (!seenImageData) shared.add(chunk);
            }
        }
        if (header == null) throw new IOException("PNG without a header.");
        int width = readInt(header.data, 0);
        int height = readInt(header.data, 4);
        if (frames.isEmpty()) {
            BufferedImage still = toArgb(ImageIO.read(new ByteArrayInputStream(data)));
            if (still == null) throw new IOException("Unreadable PNG.");
            return new GifUtil.GifData(
                new BufferedImage[] { fit(still, maxWidth, maxHeight) },
                new int[] { DEFAULT_DELAY_MS });
        }

        double scale = Math.min(1.0, Math.min((double) maxWidth / width, (double) maxHeight / height));
        int outWidth = Math.max(1, (int) Math.round(width * scale));
        int outHeight = Math.max(1, (int) Math.round(height * scale));
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage[] images = new BufferedImage[frames.size()];
        int[] delays = new int[frames.size()];
        for (int i = 0; i < frames.size(); i++) {
            Frame frame = frames.get(i);
            BufferedImage image = decodeFrame(header, shared, frame);
            int dispose = i == 0 && frame.dispose == DISPOSE_PREVIOUS ? DISPOSE_BACKGROUND : frame.dispose;
            BufferedImage saved = dispose == DISPOSE_PREVIOUS ? copy(canvas, frame) : null;
            draw(canvas, image, frame);
            images[i] = outWidth == width && outHeight == height ? copy(canvas)
                : GifUtil.scaleFrame(canvas, outWidth, outHeight);
            delays[i] = frame.delayMs();
            if (dispose == DISPOSE_BACKGROUND) {
                clear(canvas, frame);
            } else if (dispose == DISPOSE_PREVIOUS) {
                restore(canvas, saved, frame);
            }
        }
        return new GifUtil.GifData(images, delays);
    }

    /**
     * An APNG of full-size frames, all as large as the first; {@code plays} 0 loops forever. Each frame after the first
     * stores only the rectangle that changed and replaces it.
     */
    public static byte[] write(BufferedImage[] frames, int[] delaysMs, int plays) throws IOException {
        if (frames.length != delaysMs.length) {
            throw new IllegalArgumentException("frames and delays don't match");
        }
        return write(frames.length, index -> frames[index], delaysMs, plays);
    }

    /** Hands out frames one at a time, so a long APNG can be written without holding them all. */
    public interface FrameSource {

        BufferedImage frame(int index) throws IOException;
    }

    /**
     * As {@link #write(BufferedImage[], int[], int)}, asking for each frame when it is written; only the frame before
     * it is kept, as pixels, to find what changed.
     */
    public static byte[] write(int frameCount, FrameSource frames, int[] delaysMs, int plays) throws IOException {
        if (frameCount == 0 || frameCount != delaysMs.length) {
            throw new IllegalArgumentException("frames and delays don't match");
        }
        int width = -1;
        int height = -1;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.write(SIGNATURE);
        int sequence = 0;
        int[] previous = null;
        for (int i = 0; i < frameCount; i++) {
            BufferedImage frame = toArgb(frames.frame(i));
            if (i == 0) {
                width = frame.getWidth();
                height = frame.getHeight();
            }
            if (frame.getWidth() != width || frame.getHeight() != height) {
                throw new IllegalArgumentException("Frame " + i + " isn't " + width + "x" + height);
            }
            int[] pixels = frame.getRGB(0, 0, width, height, null, 0, width);
            int[] box = previous == null ? new int[] { 0, 0, width, height }
                : changedBox(previous, pixels, width, height);
            previous = pixels;
            BufferedImage region = frame.getSubimage(box[0], box[1], box[2], box[3]);
            List<Chunk> encoded = chunks(encodePng(region));
            if (i == 0) {
                writeChunk(out, "IHDR", headerChunk(encoded).data);
                ByteArrayOutputStream control = new ByteArrayOutputStream();
                DataOutputStream c = new DataOutputStream(control);
                c.writeInt(frameCount);
                c.writeInt(plays);
                writeChunk(out, "acTL", control.toByteArray());
            }
            writeChunk(out, "fcTL", frameControl(sequence++, box, delaysMs[i]));
            for (Chunk chunk : encoded) {
                if (!"IDAT".equals(chunk.type)) continue;
                if (i == 0) {
                    writeChunk(out, "IDAT", chunk.data);
                } else {
                    ByteArrayOutputStream frameData = new ByteArrayOutputStream();
                    new DataOutputStream(frameData).writeInt(sequence++);
                    frameData.write(chunk.data);
                    writeChunk(out, "fdAT", frameData.toByteArray());
                }
            }
        }
        writeChunk(out, "IEND", new byte[0]);
        out.flush();
        return bytes.toByteArray();
    }

    // -- Reading --

    private static final class Chunk {

        final String type;
        final byte[] data;

        Chunk(String type, byte[] data) {
            this.type = type;
            this.data = data;
        }
    }

    private static final class Frame {

        final int width;
        final int height;
        final int x;
        final int y;
        final int delayNumerator;
        final int delayDenominator;
        final int dispose;
        final int blend;
        final ByteArrayOutputStream data = new ByteArrayOutputStream();

        Frame(byte[] control) throws IOException {
            if (control.length < 26) throw new IOException("Short frame control chunk.");
            width = readInt(control, 4);
            height = readInt(control, 8);
            x = readInt(control, 12);
            y = readInt(control, 16);
            delayNumerator = (control[20] & 0xFF) << 8 | control[21] & 0xFF;
            delayDenominator = (control[22] & 0xFF) << 8 | control[23] & 0xFF;
            dispose = control[24];
            blend = control[25];
        }

        int delayMs() {
            int denominator = delayDenominator == 0 ? 100 : delayDenominator;
            int ms = Math.round(delayNumerator * 1000f / denominator);
            return ms <= 0 ? DEFAULT_DELAY_MS : ms;
        }
    }

    private static List<Chunk> chunks(byte[] data) throws IOException {
        List<Chunk> chunks = new ArrayList<>();
        int offset = SIGNATURE.length;
        while (offset + 12 <= data.length) {
            int length = readInt(data, offset);
            if (length < 0 || offset + 12 + length > data.length) throw new IOException("Truncated PNG chunk.");
            String type = new String(data, offset + 4, 4, StandardCharsets.ISO_8859_1);
            byte[] body = new byte[length];
            System.arraycopy(data, offset + 8, body, 0, length);
            chunks.add(new Chunk(type, body));
            offset += 12 + length;
            if ("IEND".equals(type)) break;
        }
        return chunks;
    }

    /** The frame's image data in a PNG of its own: the header at the frame's size and the shared chunks. */
    private static BufferedImage decodeFrame(Chunk header, List<Chunk> shared, Frame frame) throws IOException {
        byte[] frameHeader = header.data.clone();
        writeInt(frameHeader, 0, frame.width);
        writeInt(frameHeader, 4, frame.height);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.write(SIGNATURE);
        writeChunk(out, "IHDR", frameHeader);
        for (Chunk chunk : shared) {
            writeChunk(out, chunk.type, chunk.data);
        }
        writeChunk(out, "IDAT", frame.data.toByteArray());
        writeChunk(out, "IEND", new byte[0]);
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes.toByteArray()));
        if (image == null) throw new IOException("Unreadable APNG frame.");
        return toArgb(image);
    }

    private static void draw(BufferedImage canvas, BufferedImage image, Frame frame) {
        Graphics2D g = canvas.createGraphics();
        try {
            g.setComposite(frame.blend == BLEND_SOURCE ? AlphaComposite.Src : AlphaComposite.SrcOver);
            g.drawImage(image, frame.x, frame.y, null);
        } finally {
            g.dispose();
        }
    }

    private static void clear(BufferedImage canvas, Frame frame) {
        Graphics2D g = canvas.createGraphics();
        try {
            g.setComposite(AlphaComposite.Clear);
            g.fillRect(frame.x, frame.y, frame.width, frame.height);
        } finally {
            g.dispose();
        }
    }

    private static BufferedImage copy(BufferedImage canvas, Frame frame) {
        int w = Math.min(frame.width, canvas.getWidth() - frame.x);
        int h = Math.min(frame.height, canvas.getHeight() - frame.y);
        if (w <= 0 || h <= 0) return null;
        return copy(canvas.getSubimage(frame.x, frame.y, w, h));
    }

    private static void restore(BufferedImage canvas, BufferedImage saved, Frame frame) {
        if (saved == null) return;
        Graphics2D g = canvas.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(saved, frame.x, frame.y, null);
        } finally {
            g.dispose();
        }
    }

    private static BufferedImage fit(BufferedImage image, int maxWidth, int maxHeight) {
        double scale = Math
            .min(1.0, Math.min((double) maxWidth / image.getWidth(), (double) maxHeight / image.getHeight()));
        if (scale >= 1.0) return image;
        return GifUtil.scaleFrame(
            image,
            Math.max(1, (int) Math.round(image.getWidth() * scale)),
            Math.max(1, (int) Math.round(image.getHeight() * scale)));
    }

    // -- Writing --

    private static byte[] encodePng(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        if (!ImageIO.write(toArgb(image), "png", out)) throw new IOException("This Java has no PNG writer.");
        return out.toByteArray();
    }

    private static Chunk headerChunk(List<Chunk> chunks) throws IOException {
        for (Chunk chunk : chunks) {
            if ("IHDR".equals(chunk.type)) return chunk;
        }
        throw new IOException("Encoded PNG without a header.");
    }

    /** The smallest rectangle where the frames differ; one pixel when they don't. */
    private static int[] changedBox(int[] before, int[] after, int width, int height) {
        int minX = width, minY = height, maxX = -1, maxY = -1;
        for (int y = 0; y < height; y++) {
            int row = y * width;
            for (int x = 0; x < width; x++) {
                if (before[row + x] != after[row + x]) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) return new int[] { 0, 0, 1, 1 };
        return new int[] { minX, minY, maxX - minX + 1, maxY - minY + 1 };
    }

    /** The frame replaces its rectangle and stays when the next one is drawn. */
    private static byte[] frameControl(int sequence, int[] box, int delayMs) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        DataOutputStream out = new DataOutputStream(bytes);
        out.writeInt(sequence);
        out.writeInt(box[2]);
        out.writeInt(box[3]);
        out.writeInt(box[0]);
        out.writeInt(box[1]);
        out.writeShort(Math.max(1, Math.min(65535, delayMs)));
        out.writeShort(1000);
        out.writeByte(DISPOSE_NONE);
        out.writeByte(BLEND_SOURCE);
        return bytes.toByteArray();
    }

    // -- Bytes --

    private static void writeChunk(DataOutputStream out, String type, byte[] data) throws IOException {
        byte[] typeBytes = type.getBytes(StandardCharsets.ISO_8859_1);
        out.writeInt(data.length);
        out.write(typeBytes);
        out.write(data);
        CRC32 crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.writeInt((int) crc.getValue());
    }

    private static int readInt(byte[] data, int offset) {
        return (data[offset] & 0xFF) << 24 | (data[offset + 1] & 0xFF) << 16
            | (data[offset + 2] & 0xFF) << 8
            | data[offset + 3] & 0xFF;
    }

    private static void writeInt(byte[] data, int offset, int value) {
        data[offset] = (byte) (value >>> 24);
        data[offset + 1] = (byte) (value >>> 16);
        data[offset + 2] = (byte) (value >>> 8);
        data[offset + 3] = (byte) value;
    }

    private static BufferedImage toArgb(BufferedImage image) {
        if (image == null || image.getType() == BufferedImage.TYPE_INT_ARGB) return image;
        return copy(image);
    }

    private static BufferedImage copy(BufferedImage image) {
        BufferedImage out = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(image, 0, 0, null);
        } finally {
            g.dispose();
        }
        return out;
    }
}
