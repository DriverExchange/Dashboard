package dx.dashboard.tools;

import dx.dashboard.Logger;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.util.UUID;

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
}
