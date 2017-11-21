package sushi.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.ProviderNotFoundException;
import java.nio.file.StandardCopyOption;

public class NativeUtils {

	enum OS {
		LINUX,
		MAC,
		WINDOWS
	}

	private static File temporaryDir;

	private NativeUtils() { }

	public static void loadLibraryFromJar(String path, String filename) throws IOException {
		if (!path.startsWith("/")) {
			throw new IllegalArgumentException("The path must start with '/'");
		}

		if (temporaryDir == null) {
			// We create it once per execution
			temporaryDir = createTempDirectory("nativeutils");
			temporaryDir.deleteOnExit();
		}

		String loadPath = path + File.separator + (is64() ? "x86_64" : "x86") + "-";
		switch (getOS()) {
		case LINUX:
			loadPath += "linux" + File.separator + filename + ".so";
			break;
		case MAC:
			loadPath += "mac" + File.separator + filename + ".dylib";
			break;
		case WINDOWS:
		default:
			throw new IllegalArgumentException("Unsupported operating system");
		}

		File temp = new File(temporaryDir, filename);

		try (InputStream is = NativeUtils.class.getResourceAsStream(loadPath)) {
			Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			temp.delete();
			throw e;
		} catch (NullPointerException e) {
			temp.delete();
			throw new FileNotFoundException("File " + loadPath + " was not found inside JAR.");
		}

		try {
			System.load(temp.getAbsolutePath());
		} finally {
			if (isPosixCompliant()) {
				temp.delete();
			} else {
				temp.deleteOnExit();
			}
		}
	}

	private static boolean isPosixCompliant() {
		try {
			if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix"))
				return true;
			return false;
		} catch (FileSystemNotFoundException | ProviderNotFoundException | SecurityException e) {
			return false;
		}
	}

	private static File createTempDirectory(String prefix) throws IOException {
		String tempDir = System.getProperty("java.io.tmpdir");
		File generatedDir = new File(tempDir, prefix + System.nanoTime());

		if (!generatedDir.mkdir())
			throw new IOException("Failed to create temp directory " + generatedDir.getName());

		return generatedDir;
	}

	private static OS getOS() {
		String osName = System.getProperty("os.name").toLowerCase();
		if (osName.contains("windows")) {
			return OS.WINDOWS;
		} else if (osName.contains("linux")) {
			return OS.LINUX;
		} else if (osName.contains("mac")) {
			return OS.MAC;
		}
		return null;
	}

	private static boolean is64() {
		String arch = System.getProperty("sun.arch.data.model");
		if (arch.contains("64")) {
			return true;
		}
		return false;
	}
}
