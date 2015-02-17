package dx.dashboard.tools;

import dx.dashboard.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Tools {

	public static File tmpDir() {
		File tmpDir = new File("tmp/data");
		if (!tmpDir.exists()) {
			tmpDir.mkdirs();
		}
		return tmpDir;
	}

	public static File tmpFile() {
		return new File(tmpDir(), Codec.UUID());
	}

	public static String runProcess(String outputPrefix, String command) {
		Logger.info("> %s", command);
		ProcessBuilder pb = new ProcessBuilder(command.split("\\s+"));
		pb = pb.directory(new File("."));
		Process testProcess = null;
		String output = "";
		try {
			testProcess = pb.start();
			BufferedReader minifyReader = new BufferedReader(new InputStreamReader(testProcess.getInputStream()));
			String line;
			while ((line = minifyReader.readLine()) != null) {
				output += line + "\n";
				Logger.info("%s: %s", outputPrefix, line);
			}
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(testProcess.getErrorStream()));
			while ((line = errorReader.readLine()) != null) {
				output += line + "\n";
				Logger.error("%s: %s", outputPrefix, line);
			}
			minifyReader.close();

			int exitValue = -1;
			int attempts = 0;
			while(attempts < 5) {
				try {
					attempts++;
					Thread.sleep(500);
					exitValue = testProcess.exitValue();
					break;
				} catch (IllegalThreadStateException ise) {
					ise.printStackTrace();
				}
			}
			if (exitValue != 0) {
				Logger.error("%s: Exit value: %d", outputPrefix, exitValue);
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException ie) {
			ie.printStackTrace();
		} finally {
			if (testProcess != null) {
				testProcess.destroy();
			}
		}
		return output;
	}

	public static String[] getResourceListing(String path) {

		try {
			URL dirURL = ClassLoader.getSystemResource(path);
			if (dirURL != null && dirURL.getProtocol().equals("file")) {
				// A file path: easy enough
				return new File(dirURL.toURI()).list();
			}

			if (dirURL != null && dirURL.getProtocol().equals("jar")) {
				String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
				JarFile jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
				Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
				Set<String> result = new HashSet<>(); //avoid duplicates in case it is a subdirectory
				while (entries.hasMoreElements()) {
					String name = entries.nextElement().getName();
					if (name.startsWith(path)) { //filter according to the path
						String entry = name.substring(path.length());
						if (entry.startsWith("/")) {
							entry = entry.substring(1);
						}
						if (entry.isEmpty()) {
							continue;
						}
						int checkSubdir = entry.indexOf("/");
						if (checkSubdir >= 0) {
							// if it is a subdirectory, we just return the directory name
							entry = entry.substring(0, checkSubdir);
						}
						result.add(entry);
					}
				}
				return result.toArray(new String[result.size()]);
			}
		}
		catch (URISyntaxException | IOException e) {
			throw new RuntimeException(e);
		}

		return new String[] {};
	}

}
