package dx.dashboard.tools;

import java.io.File;
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
		return new File(tmpDir(), UUID());
	}

	public static String UUID() {
		return UUID.randomUUID().toString();
	}

}
