package dx.dashboard.tools;

import dx.dashboard.App;

import static javax.script.ScriptContext.*;

import java.io.*;
import javax.script.*;


public class CoffeeScriptCompiler {

	public static String compileWithNashorn(String source) {
		InputStream coffeeIs = ClassLoader.getSystemResourceAsStream("META-INF/resources/webjars/coffee-script/1.9.0/coffee-script.min.js");
		String coffeeJs = IO.readContentAsString(coffeeIs);
		ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
		try {
			CompiledScript compiledScript = ((Compilable) nashorn).compile(coffeeJs + "\nCoffeeScript.compile(__source, {bare: true});");
			Bindings bindings = nashorn.getBindings(ENGINE_SCOPE);
			bindings.put("__source", source);
			return compiledScript.eval(bindings).toString();
		} catch (ScriptException e) {
			throw new RuntimeException("Unable to compile script", e);
		}
	}

	public static String compileCoffee(String coffeeScriptName) {
		InputStream coffeeIs;
		File compiledFile = getCompiledCoffeeFile(coffeeScriptName);
		if (Configuration.isDevMode()) {
			File coffeeFile = new File("src/main/resources/scripts/" + coffeeScriptName + ".coffee");
			if (compiledFile.exists() && coffeeFile.lastModified() <= compiledFile.lastModified()) {
				return IO.readContentAsString(compiledFile);
			}
			try {
				coffeeIs = new FileInputStream(coffeeFile);
			} catch (FileNotFoundException e) {
				throw new RuntimeException(coffeeFile.getAbsolutePath() + " not found", e);
			}
		}
		else {
			coffeeIs = ClassLoader.getSystemResourceAsStream("scripts/" + coffeeScriptName + ".coffee");
		}

		String compiledCoffee = "";
		String coffeeNativeFullpath = Configuration.properties.getProperty("coffee.native", "");
		if (!coffeeNativeFullpath.isEmpty()) {
			File tmpCoffeeFile = Tools.tmpFile();
			IO.write(coffeeIs, tmpCoffeeFile);
			String[] command = { coffeeNativeFullpath, "-p", tmpCoffeeFile.getAbsolutePath() };
			ProcessBuilder pb = new ProcessBuilder(command);
			Process coffeeProcess = null;
			try {
				coffeeProcess = pb.start();
				BufferedReader compiledReader = new BufferedReader(new InputStreamReader(coffeeProcess.getInputStream()));
				String line;
				while ((line = compiledReader.readLine()) != null) {
					compiledCoffee += line + "\n";
				}
				String coffeeErrors = "";
				BufferedReader errorReader = new BufferedReader(new InputStreamReader(coffeeProcess.getErrorStream()));
				while ((line = errorReader.readLine()) != null) {
					coffeeErrors += line + "\n";
				}
				if (!coffeeErrors.isEmpty()) {
					App.logger.error("%s", coffeeErrors);
				}
				compiledReader.close();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (coffeeProcess != null) {
					coffeeProcess.destroy();
				}
			}
		}
		else {
			// Compile with default Java 8 JavaScript runtime
			compiledCoffee = compileWithNashorn(IO.readContentAsString(coffeeIs));
		}

		IO.writeContent(compiledCoffee, compiledFile);

		return compiledCoffee;
	}

	public static File getCompiledCoffeeFile(String coffeeFileName) {

		File compiledDirectory = new File("tmp/assets/coffee");

		if (!compiledDirectory.exists()) {
			compiledDirectory.mkdirs();
		}

		return new File(compiledDirectory, coffeeFileName + ".js");
	}

}
