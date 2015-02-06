package dx.dashboard.tools;

import static javax.script.ScriptContext.*;

import java.io.*;

import javax.script.*;


public class CoffeeScriptCompiler {

	private final CompiledScript compiledScript;
	private final Bindings bindings;

	public CoffeeScriptCompiler() {
		String script = readScript("META-INF/resources/webjars/coffee-script/1.9.0/coffee-script.min.js");

		ScriptEngine nashorn = new ScriptEngineManager().getEngineByName("nashorn");
		try {
			compiledScript = ((Compilable) nashorn).compile(script + "\nCoffeeScript.compile(__source, {bare: true});");
			bindings = nashorn.getBindings(ENGINE_SCOPE);
		} catch (ScriptException e) {
			throw new RuntimeException("Unable to compile script", e);
		}
	}

	private static String readScript(String path) {
		try (InputStream input = ClassLoader.getSystemResourceAsStream(path)) {
			return IO.readContentAsString(input);
		} catch (IOException e) {
			throw new RuntimeException("Unable to read " + path, e);
		}
	}

	public synchronized String toJs(String coffee) throws ScriptException {
		bindings.put("__source", coffee);

		return compiledScript.eval(bindings).toString();
	}

}
