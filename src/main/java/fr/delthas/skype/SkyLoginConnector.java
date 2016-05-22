package fr.delthas.skype;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

class SkyLoginConnector {

  static {
    try {
      System.loadLibrary("skylogin");
    } catch (UnsatisfiedLinkError e1) {
      try {
        boolean isSystemUnix = isSystemUnix();
        String jarLibraryDirectory = isSystemUnix ? "/linux" : "/windows";
        jarLibraryDirectory += System.getProperty("sun.arch.data.model");
        Path tempLibraryDirectory = Files.createTempDirectory(null);
        String libraryName = isSystemUnix ? "libskylogin.so" : "skylogin.dll";
        loadFromFile(tempLibraryDirectory, jarLibraryDirectory + "/", libraryName);
      } catch (IOException e2) {
        // Library loading failed
        throw new RuntimeException(e2);
      }
    }
  }

  private static boolean isSystemUnix() {
    String osName = System.getProperty("os.name").toLowerCase();
    return !osName.startsWith("win");
  }

  private static void loadFromFile(Path tempLibraryDirectory, String libraryDirectory, String libraryName) throws IOException {
    Path tempLibraryFile = tempLibraryDirectory.resolve(libraryName);
    Files.copy(SkyLoginConnector.class.getResourceAsStream(libraryDirectory + libraryName), tempLibraryFile, StandardCopyOption.REPLACE_EXISTING);
    System.load(tempLibraryFile.toAbsolutePath().toString());
  }

  public static native String getUIC(String username, String password, String nonce);
}
