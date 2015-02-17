package dx.dashboard.tools;

import dx.dashboard.App;
import dx.dashboard.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StylusCompiler {

	public static final File compileDir = new File("tmp/assets/stylus");

	private static final Pattern imports = Pattern.compile("@import\\s+[\"']?([^\"']+)[\"']?");

	private static void findDependencies(File stylusFile, List<File> deps) {
		try {
			if (stylusFile.exists()) {
				Matcher m = imports.matcher(IO.readContentAsString(stylusFile));
				while (m.find()) {
					String fileName = m.group(1);
					File depStylus = new File("public/stylesheets/" + fileName + (fileName.endsWith(".styl") ? "" : ".styl"));
					if (depStylus.exists()) {
						deps.add(depStylus);
						findDependencies(depStylus, deps);
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException("in StylusCompiler.findDependencies", e);
		}
	}

	public static long lastModified(File stylusFile) {
		List<File> deps = new ArrayList<File>();
		findDependencies(stylusFile, deps);
		Long lastModified = stylusFile.lastModified();
		for (File dep : deps) {
			if (lastModified < dep.lastModified()) {
				lastModified = dep.lastModified();
			}
		}
		return lastModified;
	}

	public static File getCompiledFile(String sourceRelativePathWithoutExt) {
		return new File(compileDir, sourceRelativePathWithoutExt + ".css");
	}

	public static String compile(String sourceRelativePath) {
		return compile(sourceRelativePath, false);
	}

	public static String compile(String fileNameWithoutExt, boolean force) {
		try {
			File compiledFile = getCompiledFile(fileNameWithoutExt);
			File sourceFile = App.isDevMode() ? new File("src/main/resources/stylus", fileNameWithoutExt + ".styl") : null;
			if (!force && sourceFile != null && compiledFile.exists() && compiledFile.lastModified() > lastModified(sourceFile)) {
				return IO.readContentAsString(compiledFile);
			}
			else {
				String stylusContent = App.isDevMode()
					? IO.readContentAsString(sourceFile)
					: IO.readContentAsString(ClassLoader.getSystemResourceAsStream("stylus/" + fileNameWithoutExt + ".styl"));

				String compiled = compileProcess(stylusContent);
				if (!compiledFile.exists()) {
					if (!compiledFile.getParentFile().exists()) {
						compiledFile.getParentFile().mkdirs();
					}
					compiledFile.createNewFile();
				}
				IO.writeContent(compiled, compiledFile);

				return compiled;
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static String compileProcess(String stylusContent) {
		String compiled = "";
		String coffeeNativeFullpath = App.configuration.getProperty("stylus.path", "");
		List<String> command = new ArrayList<String>();
		command.add(coffeeNativeFullpath);
		// TODO
		// command.add("--include-css");
		// command.add("--include");
		// command.add(sourceDir.getAbsolutePath());
		if (!App.isDevMode()) {
			command.add("-c");
		}
		ProcessBuilder pb = new ProcessBuilder(command);
		Process stylusProcess = null;
		try {
			stylusProcess = pb.start();
			OutputStream os = stylusProcess.getOutputStream();
			os.write(stylusContent.getBytes());
			os.flush();
			os.close();
			BufferedReader minifyReader = new BufferedReader(new InputStreamReader(stylusProcess.getInputStream()));
			String line;
			while ((line = minifyReader.readLine()) != null) {
				compiled += line + "\n";
			}
			String processErrors = "";
			BufferedReader errorReader = new BufferedReader(new InputStreamReader(stylusProcess.getErrorStream()));
			while ((line = errorReader.readLine()) != null) {
				processErrors += line + "\n";
			}
			if (!processErrors.isEmpty()) {
				Logger.error("%s", processErrors);
				throw new RuntimeException("Stylus compilation error");
			}
			minifyReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (stylusProcess != null) {
				stylusProcess.destroy();
			}
		}
		return compiled;
	}
}
