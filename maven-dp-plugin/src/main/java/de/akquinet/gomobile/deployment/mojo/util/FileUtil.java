package de.akquinet.gomobile.deployment.mojo.util;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.*;
 
/**
 * Utility class to copy jar files.
 * 
 * @author Thomas Leveque
 *
 */
public class FileUtil {
	
	private static File tempDir;
	
	public static File createTempFile(String prefix, String suffix) {
		if (tempDir == null) {
			tempDir = new File((new StringBuilder()).append(
	                System.getProperty("java.io.tmpdir")).append("/maven_dp_plugin").toString());
			tempDir.mkdirs();
		}
		try {
			return File.createTempFile(prefix, suffix, tempDir);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 
	 * Manifest file must not be null.
	 * 
	 * @param srcFile
	 * @param manipulatedBundleFile
	 * @param manipulatedMf
	 * @throws IOException
	 */
	public static void copyBundleFile(JarFile srcFile,
			File manipulatedBundleFile, File manifestFile) throws IOException {
		if (manipulatedBundleFile == null)
			throw new IllegalArgumentException("manifestFile cannot be null");

		JarOutputStream jos = new JarOutputStream(new FileOutputStream(
				manipulatedBundleFile));
		Enumeration<JarEntry> entries = srcFile.entries();

		// add manifest
		InputStream mfIs = new FileInputStream(manifestFile);
		putJarEntry("META-INF/MANIFEST.MF", jos, mfIs);

		while (entries.hasMoreElements()) {
			JarEntry entry = entries.nextElement();
			if (entry.getName().equals("META-INF/MANIFEST.MF"))
				continue;
			
			InputStream is = srcFile.getInputStream(entry);

			// create a new entry to avoid ZipException: invalid entry
			// compressed size
			putJarEntry(entry.getName(), jos, is);
		}
		jos.close();
	}

	private static void putJarEntry(String jarEntryName, JarOutputStream jos,
			InputStream is) throws IOException, FileNotFoundException {

		jos.putNextEntry(new JarEntry(jarEntryName));

		byte[] buffer = new byte[4096];
		int bytesRead = 0;
		while ((bytesRead = is.read(buffer)) != -1) {
			jos.write(buffer, 0, bytesRead);
		}

		is.close();
		jos.flush();
		jos.closeEntry();
	}
}