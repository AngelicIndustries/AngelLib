package industries.angelic.angellib.util;

import java.awt.FileDialog;
import java.awt.Frame;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;

import org.lwjgl.Sys;

import industries.angelic.angellib.AngelLib;

public class FileUtil {

    private static final String POJAV_CALLBACK_BRIDGE = "org.lwjgl.glfw.CallbackBridge";
    private static final int POJAV_OPEN_ACTION = 2002;

    public static void writeFileBytes(File file, byte[] data) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(data);
        fos.close();
    }

    public static void writeStringToFile(File file, String data) throws IOException {
        FileWriter writer = new FileWriter(file);
        writer.write(data);
        writer.close();
    }

    public static File createFolderIfNotExists(File folder) {
        if (!folder.exists()) {
            boolean created = folder.mkdirs();
            if (!created) {
                AngelLib.LOG.error("Failed to create directory {}", folder.getName());
                return null;
            }
        }
        return folder;
    }

    public static File createFolderIfNotExists(String path) {
        return createFolderIfNotExists(new File(path));
    }

    public static File createFolderIfNotExists(String path1, String path2) {
        return createFolderIfNotExists(
            Paths.get(path1, path2)
                .toString());
    }

    public static File createFolderIfNotExists(File path1, String path2) {
        return createFolderIfNotExists(
            Paths.get(path1.getPath(), path2)
                .toString());
    }

    public static void deleteDirectory(File directory) {
        if (directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        directory.delete();
    }

    public static String hashStringBlob(String blob) {
        // Simple hash - SHA-1 of the string bytes
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(blob.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // Fallback to simple hashCode if SHA-1 fails
            return Integer.toHexString(blob.hashCode());
        }
    }

    public static File getMinecraftDir() {
        if (MiscUtil.isServer()) {
            MinecraftServer server = MinecraftServer.getServer();
            if (server != null) {
                File serverBaseDir = server.getFile("");
                if (serverBaseDir != null) {
                    return serverBaseDir;
                }
            }
            if (Launch.minecraftHome != null) {
                return Launch.minecraftHome;
            }
            String userDir = System.getProperty("user.dir");
            if (userDir != null && !userDir.isEmpty()) {
                return new File(userDir);
            }
            return new File(".");
        } else {
            return Minecraft.getMinecraft().mcDataDir;
        }
    }

    public static boolean openFolder(File folder) {
        if (folder == null) {
            return false;
        }
        if (!folder.exists() && !folder.mkdirs()) {
            return false;
        }

        String absolutePath = folder.getAbsolutePath();

        if (Util.getOSType() == Util.EnumOS.OSX) {
            try {
                Runtime.getRuntime()
                    .exec(new String[] { "/usr/bin/open", absolutePath });
                return true;
            } catch (IOException ioexception) {
                AngelLib.LOG.error("Problem opening folder", ioexception);
            }
        } else if (Util.getOSType() == Util.EnumOS.WINDOWS) {
            String openCommand = String.format("cmd.exe /C start \"Open file\" \"%s\"", absolutePath);
            try {
                Runtime.getRuntime()
                    .exec(openCommand);
                return true;
            } catch (IOException ioexception) {
                AngelLib.LOG.error("Problem opening folder", ioexception);
            }
        }

        String folderUrl = "file://" + absolutePath;
        if (openWithPojavBridge(folderUrl)) {
            return true;
        }

        if (openFolderWithAwtDesktop(folder)) {
            return true;
        }

        AngelLib.LOG.info("Opening folder via system class fallback");
        try {
            Class<?> sysX = Class.forName("org.lwjglx.Sys");
            Object ok = sysX.getMethod("openURL", String.class)
                .invoke(null, folderUrl);
            if (ok instanceof Boolean) {
                return (Boolean) ok;
            }
            return true;
        } catch (Throwable ignored) {
            try {
                Sys.openURL(folderUrl);
                return true;
            } catch (Throwable t) {
                AngelLib.LOG.error("Failed to open folder via Sys fallback", t);
            }
        }

        return false;
    }

    public static final class FilePickerResult {

        public enum Status {
            SELECTED,
            CANCELLED,
            UNAVAILABLE,
            ERROR
        }

        private final Status status;
        private final File file;
        private final String message;

        private FilePickerResult(Status status, File file, String message) {
            this.status = status;
            this.file = file;
            this.message = message;
        }

        public static FilePickerResult selected(File file) {
            return new FilePickerResult(Status.SELECTED, file, null);
        }

        public static FilePickerResult cancelled() {
            return new FilePickerResult(Status.CANCELLED, null, null);
        }

        public static FilePickerResult unavailable(String message) {
            return new FilePickerResult(Status.UNAVAILABLE, null, message);
        }

        public static FilePickerResult error(String message) {
            return new FilePickerResult(Status.ERROR, null, message);
        }

        public Status getStatus() {
            return status;
        }

        public File getFile() {
            return file;
        }

        public String getMessage() {
            return message;
        }
    }

    public static boolean openUri(URI uri) {
        if (uri == null) return false;

        if (openWithAwtDesktop(uri)) return true;
        if (openWithLwjgl3ifyDesktop(uri)) return true;
        if (openWithSys("org.lwjglx.Sys", uri.toString())) return true;
        return openWithSys("org.lwjgl.Sys", uri.toString());
    }

    public static FilePickerResult pickFile(String title, File initialDirectory, String... extensions) {
        if (extensions == null || extensions.length == 0) {
            extensions = new String[] { "png", "gif" };
        }
        FilePickerResult scripted = takeScriptedAnswer();
        if (scripted != null) {
            return validateSelection(scripted, extensions);
        }

        // Pojav-based launchers run Minecraft in a regular Linux JVM, but desktop picker
        // implementations such as TinyFD and AWT cannot display an Android document picker.
        // Report this as unavailable so callers can present their manual/in-game fallback
        // instead of interpreting TinyFD's null result as a user cancellation.
        if (hasPojavBridge()) {
            return FilePickerResult.unavailable(GuiText.tr("angellib.gui.file_picker.unavailable"));
        }

        if (isMacOs()) {
            FilePickerResult macResult = pickWithMacOsaScript(title, initialDirectory, extensions);
            if (macResult.getStatus() != FilePickerResult.Status.UNAVAILABLE) {
                return validateSelection(macResult, extensions);
            }
        }

        if (!isWindows()) {
            FilePickerResult tinyfdResult = pickWithTinyfd(title, initialDirectory, extensions);
            if (tinyfdResult.getStatus() != FilePickerResult.Status.UNAVAILABLE) {
                return validateSelection(tinyfdResult, extensions);
            }
        }

        FilePickerResult awtResult = pickWithAwt(title, initialDirectory, extensions);
        if (awtResult.getStatus() != FilePickerResult.Status.UNAVAILABLE) {
            return validateSelection(awtResult, extensions);
        }

        return FilePickerResult.unavailable(GuiText.tr("angellib.gui.file_picker.unavailable"));
    }

    /**
     * Asks where to save a file, with the native dialog where there is one. The file need not exist yet; when the name
     * typed has no extension, {@code extension} is added.
     */
    public static FilePickerResult pickSaveFile(String title, File initialDirectory, String defaultName,
        String extension) {
        FilePickerResult scripted = takeScriptedAnswer();
        if (scripted == null && hasPojavBridge()) {
            return FilePickerResult.unavailable(GuiText.tr("angellib.gui.file_picker.unavailable"));
        }
        String name = defaultName != null && !defaultName.trim()
            .isEmpty() ? defaultName.trim() : "file." + extension;

        FilePickerResult result = scripted != null ? scripted
            : FilePickerResult.unavailable(GuiText.tr("angellib.gui.file_picker.unavailable"));
        if (scripted == null && isMacOs()) {
            result = saveWithMacOsaScript(title, initialDirectory, name);
        }
        if (scripted == null && result.getStatus() == FilePickerResult.Status.UNAVAILABLE && !isWindows()) {
            result = saveWithTinyfd(title, initialDirectory, name, extension);
        }
        if (scripted == null && result.getStatus() == FilePickerResult.Status.UNAVAILABLE) {
            result = saveWithAwt(title, initialDirectory, name);
        }
        if (result.getStatus() != FilePickerResult.Status.SELECTED || result.getFile() == null) {
            return result;
        }
        File file = result.getFile();
        if (extension != null && !extension.isEmpty()
            && !hasSupportedExtension(file.getName(), new String[] { extension })) {
            file = new File(file.getParentFile(), file.getName() + "." + extension);
        }
        return FilePickerResult.selected(file);
    }

    private static FilePickerResult saveWithAwt(String title, File initialDirectory, String defaultName) {
        try {
            FileDialog dialog = new FileDialog((Frame) null, safeTitle(title), FileDialog.SAVE);
            dialog.setDirectory(defaultPickerPath(initialDirectory));
            dialog.setFile(defaultName);
            dialog.setVisible(true);
            String file = dialog.getFile();
            if (file == null || file.isEmpty()) {
                return FilePickerResult.cancelled();
            }
            return FilePickerResult.selected(new File(dialog.getDirectory(), file));
        } catch (Throwable t) {
            AngelLib.LOG.error("Native save dialog failed in AWT path", t);
            String message = t.getMessage() != null ? t.getMessage()
                : t.getClass()
                    .getSimpleName();
            return FilePickerResult.error(GuiText.tr("angellib.gui.common.failed_message", message));
        }
    }

    private static FilePickerResult saveWithTinyfd(String title, File initialDirectory, String defaultName,
        String extension) {
        try {
            Class<?> pointerBufferClass = Class.forName("org.lwjgl.PointerBuffer");
            Class<?> tinyDialogsClass = Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
            String[] extensions = extension != null && !extension.isEmpty() ? new String[] { extension }
                : new String[0];
            Object filters = createTinyfdFilters(pointerBufferClass, extensions);
            File directory = initialDirectory != null ? initialDirectory : getDefaultFileSelectionDirectory();
            Object path = tinyDialogsClass
                .getMethod(
                    "tinyfd_saveFileDialog",
                    CharSequence.class,
                    CharSequence.class,
                    pointerBufferClass,
                    CharSequence.class)
                .invoke(
                    null,
                    safeTitle(title),
                    new File(directory, defaultName).getAbsolutePath(),
                    filters,
                    extensions.length > 0 ? buildFilterLabel(extensions) : null);
            if (path == null || path.toString()
                .isEmpty()) {
                return FilePickerResult.cancelled();
            }
            return FilePickerResult.selected(new File(path.toString()));
        } catch (ClassNotFoundException e) {
            return FilePickerResult.unavailable(GuiText.tr("angellib.gui.file_picker.tinyfd_missing"));
        } catch (Throwable t) {
            AngelLib.LOG.warn("TinyFD save dialog failed, trying AWT", t);
            return FilePickerResult.unavailable(GuiText.tr("angellib.gui.file_picker.unavailable"));
        }
    }

    private static FilePickerResult saveWithMacOsaScript(String title, File initialDirectory, String defaultName) {
        List<String> command = new ArrayList<>();
        command.add("osascript");
        command.add("-e");
        command.add("try");
        command.add("-e");
        String location = initialDirectory != null
            ? " default location POSIX file \"" + escapeAppleScript(initialDirectory.getAbsolutePath()) + "\""
            : "";
        command.add(
            "set _picked to choose file name with prompt \"" + escapeAppleScript(
                safeTitle(title)) + "\" default name \"" + escapeAppleScript(defaultName) + "\"" + location);
        command.add("-e");
        command.add("return POSIX path of _picked");
        command.add("-e");
        command.add("on error number -128");
        command.add("-e");
        command.add("return \"\"");
        command.add("-e");
        command.add("end try");
        try {
            Process process = new ProcessBuilder(command).start();
            String output = readAll(process.getInputStream());
            if (process.waitFor() != 0) {
                return FilePickerResult.error(GuiText.tr("angellib.gui.file_picker.macos_failed"));
            }
            String path = output != null ? output.trim() : "";
            return path.isEmpty() ? FilePickerResult.cancelled() : FilePickerResult.selected(new File(path));
        } catch (IOException e) {
            return FilePickerResult.unavailable(GuiText.tr("angellib.gui.file_picker.osascript_missing"));
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
            return FilePickerResult.error(GuiText.tr("angellib.gui.file_picker.interrupted"));
        }
    }

    /**
     * For automated tests, which can't click through a native dialog: the system property
     * {@value #SCRIPTED_ANSWER_PROPERTY} answers the next file or save dialog with its path, once.
     */
    public static final String SCRIPTED_ANSWER_PROPERTY = "angellib.filePicker.next";

    private static synchronized FilePickerResult takeScriptedAnswer() {
        String path = System.getProperty(SCRIPTED_ANSWER_PROPERTY);
        if (path == null || path.isEmpty()) {
            return null;
        }
        System.clearProperty(SCRIPTED_ANSWER_PROPERTY);
        AngelLib.LOG.info("[FileUtil] Answering a file dialog with {} ({})", path, SCRIPTED_ANSWER_PROPERTY);
        return FilePickerResult.selected(new File(path));
    }

    public static File getDefaultFileSelectionDirectory() {
        String userHome = System.getProperty("user.home", "");
        if (userHome != null && !userHome.trim()
            .isEmpty()) {
            File home = new File(userHome);
            if (home.isDirectory()) {
                return home;
            }
        }
        return new File(".");
    }

    public static FilePickerResult pickWithAwt(String title, File initialDirectory, String[] extensions) {
        try {
            FileDialog dialog = new FileDialog((Frame) null, safeTitle(title), FileDialog.LOAD);

            dialog.setDirectory(defaultPickerPath(initialDirectory));
            dialog.setFile(buildAwtFilter(extensions));
            dialog.setVisible(true);

            String directory = dialog.getDirectory();
            String file = dialog.getFile();

            if (file == null || file.isEmpty()) {
                return FilePickerResult.cancelled();
            }

            File selectedFile = new File(directory, file);
            return FilePickerResult.selected(selectedFile);
        } catch (Throwable t) {
            AngelLib.LOG.error("Native file picker failed in AWT path", t);
            String message = t.getMessage() != null ? t.getMessage()
                : t.getClass()
                    .getSimpleName();
            return FilePickerResult.error(GuiText.tr("angellib.gui.common.failed_message", message));
        }
    }

    private static FilePickerResult pickWithTinyfd(String title, File initialDirectory, String[] extensions) {
        try {
            Class<?> pointerBufferClass = Class.forName("org.lwjgl.PointerBuffer");
            Class<?> tinyDialogsClass = Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
            Object filters = createTinyfdFilters(pointerBufferClass, extensions);
            Object selectedPath = tinyDialogsClass
                .getMethod(
                    "tinyfd_openFileDialog",
                    CharSequence.class,
                    CharSequence.class,
                    pointerBufferClass,
                    CharSequence.class,
                    boolean.class)
                .invoke(
                    null,
                    safeTitle(title),
                    defaultPickerPath(initialDirectory),
                    filters,
                    buildFilterLabel(extensions),
                    false);

            if (selectedPath == null) {
                return FilePickerResult.cancelled();
            }
            String path = selectedPath.toString()
                .trim();
            if (path.isEmpty()) {
                return FilePickerResult.cancelled();
            }
            return FilePickerResult.selected(new File(path));
        } catch (ClassNotFoundException e) {
            return FilePickerResult.unavailable(GuiText.tr("angellib.gui.file_picker.tinyfd_missing"));
        } catch (Throwable t) {
            AngelLib.LOG.error("Native file picker failed in tinyfd path", t);
            String message = t.getMessage() != null ? t.getMessage()
                : t.getClass()
                    .getSimpleName();
            return FilePickerResult.error(GuiText.tr("angellib.gui.common.failed_message", message));
        }
    }

    private static Object createTinyfdFilters(Class<?> pointerBufferClass, String[] extensions) {
        try {
            Class<?> bufferUtilsClass = Class.forName("org.lwjgl.BufferUtils");
            Class<?> memoryUtilClass = Class.forName("org.lwjgl.system.MemoryUtil");
            Object filters = bufferUtilsClass.getMethod("createPointerBuffer", int.class)
                .invoke(null, extensions.length);
            for (String ext : extensions) {
                Object pattern = memoryUtilClass.getMethod("memUTF8", CharSequence.class)
                    .invoke(null, "*." + ext);
                pointerBufferClass.getMethod("put", java.nio.ByteBuffer.class)
                    .invoke(filters, pattern);
            }
            pointerBufferClass.getMethod("flip")
                .invoke(filters);
            return filters;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static FilePickerResult pickWithMacOsaScript(String title, File initialDirectory, String[] extensions) {
        if (!isMacOs()) {
            return FilePickerResult.unavailable(GuiText.tr("angellib.gui.file_picker.not_macos"));
        }

        String typeList = buildAppleScriptTypeList(extensions);
        String initialPath = initialDirectory != null ? initialDirectory.getAbsolutePath() : "";
        List<String> command = new ArrayList<>();
        command.add("osascript");
        command.add("-e");
        command.add("try");
        command.add("-e");
        if (!initialPath.isEmpty()) {
            command.add(
                "set _picked to choose file with prompt \"" + escapeAppleScript(safeTitle(title))
                    + "\" default location POSIX file \""
                    + escapeAppleScript(initialPath)
                    + "\" of type {"
                    + typeList
                    + "}");
        } else {
            command.add(
                "set _picked to choose file with prompt \"" + escapeAppleScript(safeTitle(title))
                    + "\" of type {"
                    + typeList
                    + "}");
        }
        command.add("-e");
        command.add("return POSIX path of _picked");
        command.add("-e");
        command.add("on error number -128");
        command.add("-e");
        command.add("return \"\"");
        command.add("-e");
        command.add("end try");

        Process process = null;
        try {
            process = new ProcessBuilder(command).start();
            String output = readAll(process.getInputStream());
            int code = process.waitFor();
            if (code != 0) {
                String err = readAll(process.getErrorStream());
                if (err != null && !err.trim()
                    .isEmpty()) {
                    return FilePickerResult.error(GuiText.tr("angellib.gui.common.failed_message", err.trim()));
                }
                return FilePickerResult.error(GuiText.tr("angellib.gui.file_picker.macos_failed"));
            }

            String path = output != null ? output.trim() : "";
            if (path.isEmpty()) {
                return FilePickerResult.cancelled();
            }
            return FilePickerResult.selected(new File(path));
        } catch (IOException e) {
            return FilePickerResult.unavailable(GuiText.tr("angellib.gui.file_picker.osascript_missing"));
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
            return FilePickerResult.error(GuiText.tr("angellib.gui.file_picker.interrupted"));
        } catch (Throwable t) {
            AngelLib.LOG.error("Native file picker failed in macOS osascript path", t);
            String message = t.getMessage() != null ? t.getMessage()
                : t.getClass()
                    .getSimpleName();
            return FilePickerResult.error(GuiText.tr("angellib.gui.common.failed_message", message));
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static FilePickerResult validateSelection(FilePickerResult result, String[] extensions) {
        if (result == null) {
            return FilePickerResult.error(GuiText.tr("angellib.gui.file_picker.unknown_error"));
        }

        if (result.getStatus() != FilePickerResult.Status.SELECTED) {
            return result;
        }

        File file = result.getFile();
        if (file == null) {
            return FilePickerResult.error(GuiText.tr("angellib.gui.file_picker.no_file"));
        }
        if (!file.isFile()) {
            return FilePickerResult.error(GuiText.tr("angellib.gui.file_picker.not_a_file"));
        }
        if (!file.canRead()) {
            return FilePickerResult.error(GuiText.tr("angellib.gui.file_picker.not_readable"));
        }
        if (!hasSupportedExtension(file.getName(), extensions)) {
            return FilePickerResult
                .error(GuiText.tr("angellib.gui.file_picker.unsupported_type", buildExtensionList(extensions)));
        }
        return result;
    }

    private static boolean hasSupportedExtension(String fileName, String[] extensions) {
        if (fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        for (String ext : extensions) {
            if (lower.endsWith("." + ext.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    private static String buildAwtFilter(String[] extensions) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < extensions.length; i++) {
            if (i > 0) sb.append(';');
            sb.append("*.")
                .append(extensions[i]);
        }
        return sb.toString();
    }

    private static String buildFilterLabel(String[] extensions) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        for (int i = 0; i < extensions.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("*.")
                .append(extensions[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    private static String buildExtensionList(String[] extensions) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < extensions.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(".")
                .append(extensions[i]);
        }
        return sb.toString();
    }

    private static String buildAppleScriptTypeList(String[] extensions) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < extensions.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append("\"")
                .append(extensions[i])
                .append("\"");
        }
        return sb.toString();
    }

    private static String defaultPickerPath(File initialDirectory) {
        if (initialDirectory == null) {
            return "";
        }
        File target = initialDirectory;
        while (target != null && !target.exists()) {
            target = target.getParentFile();
        }
        if (target == null) {
            return "";
        }
        if (target.isFile()) {
            File parent = target.getParentFile();
            return parent != null ? parent.getAbsolutePath() : "";
        }
        return target.getAbsolutePath();
    }

    private static String safeTitle(String title) {
        return title != null && !title.trim()
            .isEmpty() ? title.trim() : GuiText.tr("angellib.gui.file_picker.select_file");
    }

    private static boolean isMacOs() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase()
            .contains("mac");
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase()
            .contains("win");
    }

    private static String escapeAppleScript(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    private static String readAll(java.io.InputStream stream) throws IOException {
        if (stream == null) return "";
        StringBuilder out = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
        String line;
        while ((line = reader.readLine()) != null) {
            if (out.length() > 0) {
                out.append('\n');
            }
            out.append(line);
        }
        return out.toString();
    }

    private static boolean openWithAwtDesktop(URI uri) {
        try {
            Class<?> desktopCls = Class.forName("java.awt.Desktop");
            Object desktop = desktopCls.getMethod("getDesktop")
                .invoke(null);
            desktopCls.getMethod("browse", URI.class)
                .invoke(desktop, uri);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean openFolderWithAwtDesktop(File folder) {
        try {
            Class<?> desktopCls = Class.forName("java.awt.Desktop");
            Boolean desktopSupported = (Boolean) desktopCls.getMethod("isDesktopSupported")
                .invoke(null);
            if (!desktopSupported) {
                return false;
            }

            Object desktop = desktopCls.getMethod("getDesktop")
                .invoke(null);
            Class<?> actionCls = Class.forName("java.awt.Desktop$Action");
            Object openAction = Enum.valueOf((Class<Enum>) actionCls.asSubclass(Enum.class), "OPEN");
            if ((Boolean) desktopCls.getMethod("isSupported", actionCls)
                .invoke(desktop, openAction)) {
                desktopCls.getMethod("open", File.class)
                    .invoke(desktop, folder);
                return true;
            }

            Object browseAction = Enum.valueOf((Class<Enum>) actionCls.asSubclass(Enum.class), "BROWSE");
            if ((Boolean) desktopCls.getMethod("isSupported", actionCls)
                .invoke(desktop, browseAction)) {
                desktopCls.getMethod("browse", URI.class)
                    .invoke(desktop, folder.toURI());
                return true;
            }
        } catch (UnsupportedOperationException ignored) {
            return false;
        } catch (Throwable throwable) {
            AngelLib.LOG.debug("AWT Desktop could not open folder", throwable);
        }
        return false;
    }

    private static boolean openWithLwjgl3ifyDesktop(URI uri) {
        try {
            Class<?> desktopCls = Class.forName("me.eigenraven.lwjgl3ify.redirects.Desktop");
            Object desktop = desktopCls.getMethod("getDesktop")
                .invoke(null);
            desktopCls.getMethod("browse", URI.class)
                .invoke(desktop, uri);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean hasPojavBridge() {
        try {
            Class<?> bridgeCls = Class.forName(POJAV_CALLBACK_BRIDGE, false, FileUtil.class.getClassLoader());
            bridgeCls.getMethod("nativeClipboard", int.class, byte[].class);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean openWithPojavBridge(String url) {
        if (url == null || !hasPojavBridge()) {
            return false;
        }

        try {
            byte[] utf8 = url.getBytes("UTF-8");
            // Pojav's JNI clipboard/open bridge consumes a C string, so its byte array must
            // include the trailing NUL that LWJGL's own callers normally provide.
            byte[] nulTerminated = new byte[utf8.length + 1];
            System.arraycopy(utf8, 0, nulTerminated, 0, utf8.length);

            Class<?> bridgeCls = Class.forName(POJAV_CALLBACK_BRIDGE);
            bridgeCls.getMethod("nativeClipboard", int.class, byte[].class)
                .invoke(null, POJAV_OPEN_ACTION, nulTerminated);
            return true;
        } catch (Throwable throwable) {
            AngelLib.LOG.debug("Pojav Android bridge could not open URI", throwable);
            return false;
        }
    }

    private static boolean openWithSys(String className, String url) {
        try {
            Class<?> sysCls = Class.forName(className);
            Object result = sysCls.getMethod("openURL", String.class)
                .invoke(null, url);
            if (result instanceof Boolean) {
                return (Boolean) result;
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
