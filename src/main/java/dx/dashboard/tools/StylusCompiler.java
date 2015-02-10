package dx.dashboard.tools;

import dx.dashboard.App;
import dx.dashboard.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StylusCompiler {

	public static final File compileDir = new File("tmp/assets/stylus");
	public static final File sourceDir = getStylusSourceDir();

	private static final Pattern imports = Pattern.compile("@import\\s+[\"']?([^\"']+)[\"']?");

	public static File getStylusSourceDir() {
		if (App.isDevMode()) {
			return new File("src/main/resources/stylus");
		}
		else {
			return Tools.getResourceAsFile("stylus");
		}
	}

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

	public static File getCompiledFile(File stylusFile) {
		return new File(compileDir,
			stylusFile.getAbsolutePath()
				.replace(sourceDir.getAbsolutePath() + "/", "")
				.replace(".styl", ".css"));
	}

	public static String compile(String fileNameWithoutExt) {
		return compile(new File(getStylusSourceDir(), fileNameWithoutExt + ".styl"), false);
	}

	public static String compile(File stylusFile) {
		return compile(stylusFile, false);
	}

	public static String compile(File stylusFile, boolean force) {
		try {
			File compiledFile = getCompiledFile(stylusFile);
			if (!force && compiledFile.exists() && compiledFile.lastModified() > lastModified(stylusFile)) {
				return IO.readContentAsString(compiledFile);
			}
			else {
				String stylusContent = IO.readContentAsString(stylusFile);
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

	public static String getCompiled(String styleName) {
		return IO.readContentAsString(new File(compileDir, styleName + ".styl"));
	}

	public static void compileAll() {

		try {
			FileUtils.deleteDirectory(compileDir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		compileDir.mkdirs();

		Collection<File> stylusFiles = FileUtils.listFiles(sourceDir,
			new RegexFileFilter(".+\\.styl$"),
			DirectoryFileFilter.DIRECTORY);

		Logger.info("Compile all stylus files...");
		for (File stylusFile : stylusFiles) {
			compile(stylusFile, true);
			Logger.info("%s compiled", stylusFile.getAbsoluteFile());
		}
		Logger.info("Done.");
	}

	public static String compileStyle(String styleName) {
		File stylusFile = new File(sourceDir, styleName + ".styl");
		File cssFile = new File(sourceDir, styleName + ".css");
		String compiledCss = "";
		if (stylusFile.exists()) {
			compiledCss = compile(stylusFile);
		} else if (cssFile.exists()) {
			compiledCss = IO.readContentAsString(cssFile);
		}
		return compiledCss;
	}

	public static String compileProcess(String stylusContent) {
		String compiled = "";
		String coffeeNativeFullpath = App.configuration.getProperty("stylus.path", "");
		List<String> command = new ArrayList<String>();
		command.add(coffeeNativeFullpath);
		command.add("--include-css");
		command.add("--include");
		command.add(sourceDir.getAbsolutePath());
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
