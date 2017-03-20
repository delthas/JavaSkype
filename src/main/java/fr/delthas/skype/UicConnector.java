package fr.delthas.skype;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"unchecked", "rawtypes"})
final class UicConnector {

  private static final Logger logger = Logger.getLogger("fr.delthas.skype.uic");
  private static final Random random = new Random();
  private static final String[] servers = {"91.190.216.17", "91.190.218.40"};
  private static final int port = 33033;
  private static final String skypePublicKeyModulus =
      "a8f223612f4f5fc81ef1ca5e310b0b21532a72df6c1af0fbec87304aec983aab5d74a14cc72e53ef7752a248c0e5abe09484b597692015e796350989c88b3cae140ca82ccd9914e540468cf0edb35dcba4c352890e7a9eafac550b3978627651ad0a804f385ef5f4093ac6ee66b23e1f8202c61c6c0375eeb713852397ced2e199492aa61a3eab163d4c2625c873e95cafd95b80dd2d8732c8e25638a2007acfa6c8f1ff31cc2bc4ca8f4446f51da404335a48c955aaa3a4b57250d7ba29700b";
  private static final int[] crc32_tab = {0x00000000, 0x77073096, 0xee0e612c, 0x990951ba, 0x076dc419, 0x706af48f, 0xe963a535, 0x9e6495a3, 0x0edb8832,
      0x79dcb8a4, 0xe0d5e91e, 0x97d2d988, 0x09b64c2b, 0x7eb17cbd, 0xe7b82d07, 0x90bf1d91, 0x1db71064, 0x6ab020f2, 0xf3b97148, 0x84be41de, 0x1adad47d,
      0x6ddde4eb, 0xf4d4b551, 0x83d385c7, 0x136c9856, 0x646ba8c0, 0xfd62f97a, 0x8a65c9ec, 0x14015c4f, 0x63066cd9, 0xfa0f3d63, 0x8d080df5, 0x3b6e20c8,
      0x4c69105e, 0xd56041e4, 0xa2677172, 0x3c03e4d1, 0x4b04d447, 0xd20d85fd, 0xa50ab56b, 0x35b5a8fa, 0x42b2986c, 0xdbbbc9d6, 0xacbcf940, 0x32d86ce3,
      0x45df5c75, 0xdcd60dcf, 0xabd13d59, 0x26d930ac, 0x51de003a, 0xc8d75180, 0xbfd06116, 0x21b4f4b5, 0x56b3c423, 0xcfba9599, 0xb8bda50f, 0x2802b89e,
      0x5f058808, 0xc60cd9b2, 0xb10be924, 0x2f6f7c87, 0x58684c11, 0xc1611dab, 0xb6662d3d, 0x76dc4190, 0x01db7106, 0x98d220bc, 0xefd5102a, 0x71b18589,
      0x06b6b51f, 0x9fbfe4a5, 0xe8b8d433, 0x7807c9a2, 0x0f00f934, 0x9609a88e, 0xe10e9818, 0x7f6a0dbb, 0x086d3d2d, 0x91646c97, 0xe6635c01, 0x6b6b51f4,
      0x1c6c6162, 0x856530d8, 0xf262004e, 0x6c0695ed, 0x1b01a57b, 0x8208f4c1, 0xf50fc457, 0x65b0d9c6, 0x12b7e950, 0x8bbeb8ea, 0xfcb9887c, 0x62dd1ddf,
      0x15da2d49, 0x8cd37cf3, 0xfbd44c65, 0x4db26158, 0x3ab551ce, 0xa3bc0074, 0xd4bb30e2, 0x4adfa541, 0x3dd895d7, 0xa4d1c46d, 0xd3d6f4fb, 0x4369e96a,
      0x346ed9fc, 0xad678846, 0xda60b8d0, 0x44042d73, 0x33031de5, 0xaa0a4c5f, 0xdd0d7cc9, 0x5005713c, 0x270241aa, 0xbe0b1010, 0xc90c2086, 0x5768b525,
      0x206f85b3, 0xb966d409, 0xce61e49f, 0x5edef90e, 0x29d9c998, 0xb0d09822, 0xc7d7a8b4, 0x59b33d17, 0x2eb40d81, 0xb7bd5c3b, 0xc0ba6cad, 0xedb88320,
      0x9abfb3b6, 0x03b6e20c, 0x74b1d29a, 0xead54739, 0x9dd277af, 0x04db2615, 0x73dc1683, 0xe3630b12, 0x94643b84, 0x0d6d6a3e, 0x7a6a5aa8, 0xe40ecf0b,
      0x9309ff9d, 0x0a00ae27, 0x7d079eb1, 0xf00f9344, 0x8708a3d2, 0x1e01f268, 0x6906c2fe, 0xf762575d, 0x806567cb, 0x196c3671, 0x6e6b06e7, 0xfed41b76,
      0x89d32be0, 0x10da7a5a, 0x67dd4acc, 0xf9b9df6f, 0x8ebeeff9, 0x17b7be43, 0x60b08ed5, 0xd6d6a3e8, 0xa1d1937e, 0x38d8c2c4, 0x4fdff252, 0xd1bb67f1,
      0xa6bc5767, 0x3fb506dd, 0x48b2364b, 0xd80d2bda, 0xaf0a1b4c, 0x36034af6, 0x41047a60, 0xdf60efc3, 0xa867df55, 0x316e8eef, 0x4669be79, 0xcb61b38c,
      0xbc66831a, 0x256fd2a0, 0x5268e236, 0xcc0c7795, 0xbb0b4703, 0x220216b9, 0x5505262f, 0xc5ba3bbe, 0xb2bd0b28, 0x2bb45a92, 0x5cb36a04, 0xc2d7ffa7,
      0xb5d0cf31, 0x2cd99e8b, 0x5bdeae1d, 0x9b64c2b0, 0xec63f226, 0x756aa39c, 0x026d930a, 0x9c0906a9, 0xeb0e363f, 0x72076785, 0x05005713, 0x95bf4a82,
      0xe2b87a14, 0x7bb12bae, 0x0cb61b38, 0x92d28e9b, 0xe5d5be0d, 0x7cdcefb7, 0x0bdbdf21, 0x86d3d2d4, 0xf1d4e242, 0x68ddb3f8, 0x1fda836e, 0x81be16cd,
      0xf6b9265b, 0x6fb077e1, 0x18b74777, 0x88085ae6, 0xff0f6a70, 0x66063bca, 0x11010b5c, 0x8f659eff, 0xf862ae69, 0x616bffd3, 0x166ccf45, 0xa00ae278,
      0xd70dd2ee, 0x4e048354, 0x3903b3c2, 0xa7672661, 0xd06016f7, 0x4969474d, 0x3e6e77db, 0xaed16a4a, 0xd9d65adc, 0x40df0b66, 0x37d83bf0, 0xa9bcae53,
      0xdebb9ec5, 0x47b2cf7f, 0x30b5ffe9, 0xbdbdf21c, 0xcabac28a, 0x53b39330, 0x24b4a3a6, 0xbad03605, 0xcdd70693, 0x54de5729, 0x23d967bf, 0xb3667a2e,
      0xc4614ab8, 0x5d681b02, 0x2a6f2b94, 0xb40bbe37, 0xc30c8ea1, 0x5a05df1b, 0x2d02ef8d};

  static {

    // ugly piece of code to bypass Oracle JRE stupid restriction on key lengths
    // Skype requires a 256-bit key AES cipher, but Oracle will only allow a key length <= 128-bit due to US export laws

    // the normal ways to fix this are:
    // a) to stop using Oracle JRE
    // b) to replace two files in the Oracle JRE folder (see http://stackoverflow.com/a/3864276)
    // c) to use a simple 128-bit key instead of a 256-bit one
    // d) to use an external Cipher implementation (like BouncyCastle)

    // however, none of these ways are practical, or lightweight enough
    // so we have to manually override the permissions on key lengths using reflection

    // ugly reflection hack start (we have to override a private static final field from a package-private class...)

    String errorString = "Failed manually overriding key-length permissions. "
        + "Please open an issue at https://github.com/Delthas/JavaSkype/issues/ if you see this message. "
        + "Try doing this to fix the problem: http://stackoverflow.com/a/3864276";

    logger.finest("Initializing UIC generation");

    int newMaxKeyLength;
    try {
      if ((newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES")) < 256) {
        logger.finer("Overriding AES key length because of limitation");
        Class c = Class.forName("javax.crypto.CryptoAllPermissionCollection");
        Constructor con = c.getDeclaredConstructor();
        con.setAccessible(true);
        Object allPermissionCollection = con.newInstance();
        Field f = c.getDeclaredField("all_allowed");
        f.setAccessible(true);
        f.setBoolean(allPermissionCollection, true);

        c = Class.forName("javax.crypto.CryptoPermissions");
        con = c.getDeclaredConstructor();
        con.setAccessible(true);
        Object allPermissions = con.newInstance();
        f = c.getDeclaredField("perms");
        f.setAccessible(true);
        ((Map) f.get(allPermissions)).put("*", allPermissionCollection);

        c = Class.forName("javax.crypto.JceSecurityManager");
        f = c.getDeclaredField("defaultPolicy");
        f.setAccessible(true);
        Field mf = Field.class.getDeclaredField("modifiers");
        mf.setAccessible(true);
        mf.setInt(f, f.getModifiers() & ~Modifier.FINAL);
        // override a final field
        // this field won't be optimized out by the compiler because it is set at run-time
        f.set(null, allPermissions);

        newMaxKeyLength = Cipher.getMaxAllowedKeyLength("AES");
      } else {
        logger.finest("Not overriding AES key length");
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Error when overriding AES key length", e);
      throw new RuntimeException(errorString, e);
    }
    if (newMaxKeyLength < 256) {
      // hack failed
      logger.severe("Failed overriding AES key length");
      throw new RuntimeException(errorString);
    }

    // ugly reflection hack end

  }

  private UicConnector() {
    // prevent instantiation
    throw new IllegalStateException("This class cannot be instantiated");
  }

  @SuppressWarnings({"resource", "null"})
  public static String getUIC(String username, String password, String nonce) throws IOException, GeneralSecurityException {
    logger.finest("Computing UIC token with username: " + username + " and nonce: " + nonce);
    byte[] sessionKey = new byte[0xC0];
    random.nextBytes(sessionKey);
    sessionKey[0] = 1;

    byte[] magic = {0x16, 0x03, 0x01};
    byte[] magicResponse = {0x17, 0x03, 0x01};

    Socket socket = null;
    DataInputStream dis = null;
    DataOutputStream dos = null;

    for (String server : servers) {
      try {
        socket = new Socket(server, port);
        dos = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        dos.write(magic);
        dos.writeShort(0);
        dos.flush();
        dis = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        byte[] response = new byte[3];
        dis.readFully(response);
        if (Arrays.equals(magicResponse, response)) {
          dis.readFully(response, 0, 2);
          break;
        }
        logger.fine("Wrong magic received from server during handshake");
        socket.close();
        socket = null;
      } catch (IOException ex) {
        // just log the error
        logger.log(Level.FINE, "Failed connecting to server " + server + " for handshake", ex);
      }
    }

    if (socket == null) {
      logger.severe("No server responded (correctly) to handshake");
      return null;
    }

    KeyPairGenerator pairGenerator = KeyPairGenerator.getInstance("RSA");
    pairGenerator.initialize(new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4));
    KeyPair keyPair = pairGenerator.generateKeyPair();

    Cipher rsaCipher = Cipher.getInstance("RSA/ECB/NOPADDING");
    Cipher aesCipher = Cipher.getInstance("AES/CTR/NOPADDING");
    MessageDigest shaCrypt = MessageDigest.getInstance("SHA-1");
    MessageDigest md5Crypt = MessageDigest.getInstance("MD5");

    dos.write(magic);
    dos.writeShort(0xCD);

    dos.writeByte(0x41);
    dos.writeByte(0x03);

    writeValue(dos, 0x00);
    writeValue(dos, 0x09);
    writeValue(dos, 0x2000);

    byte[] sha = new byte[32];
    shaCrypt.reset();
    shaCrypt.update(new byte[] {0x00, 0x00, 0x00, 0x00});
    shaCrypt.update(sessionKey);
    System.arraycopy(shaCrypt.digest(), 0, sha, 0, 20);
    shaCrypt.update(new byte[] {0x00, 0x00, 0x00, 0x01});
    shaCrypt.update(sessionKey);
    System.arraycopy(shaCrypt.digest(), 0, sha, 20, 12);

    Key publicKey =
        KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(new BigInteger(skypePublicKeyModulus, 16), BigInteger.valueOf(65537)));
    rsaCipher.init(Cipher.ENCRYPT_MODE, publicKey);
    byte[] encryptedSessionKey = rsaCipher.doFinal(sessionKey);
    writeValue(dos, 0x04);
    writeValue(dos, 0x08);
    writeValue(dos, 0xC0);
    dos.write(encryptedSessionKey, 0, 0xC0);

    writeValue(dos, 0x00);
    writeValue(dos, 0x0C);
    writeValue(dos, 0x01);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    DataOutputStream sink = new DataOutputStream(baos);

    sink.writeByte(0x41);
    sink.writeByte(0x04);

    writeValue(sink, 0x00);
    writeValue(sink, 0x00);
    writeValue(sink, 0x1399);

    writeValue(sink, 0x00);
    writeValue(sink, 0x02);
    writeValue(sink, 0x01);

    writeValue(sink, 0x03);
    writeValue(sink, 0x04);
    sink.write(username.getBytes(StandardCharsets.UTF_8));
    sink.writeByte(0x00);

    md5Crypt.reset();
    md5Crypt.update(username.getBytes(StandardCharsets.UTF_8));
    md5Crypt.update("\nskyper\n".getBytes(StandardCharsets.UTF_8));
    md5Crypt.update(password.getBytes(StandardCharsets.UTF_8));
    byte[] loginHash = md5Crypt.digest();

    writeValue(sink, 0x04);
    writeValue(sink, 0x05);
    writeValue(sink, 16);
    sink.write(loginHash, 0, 16);

    sink.writeByte(0x41);
    sink.writeByte(0x06);

    writeValue(sink, 0x04);
    writeValue(sink, 0x21);
    writeValue(sink, 128);
    byte[] publicModulus = ((RSAPublicKey) keyPair.getPublic()).getModulus().abs().toByteArray();
    sink.write(publicModulus, publicModulus.length - 128, 128);

    writeValue(sink, 0x01);
    writeValue(sink, 0x31);
    NetworkInterface n = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
    byte[] mac;
    if (n != null && (mac = n.getHardwareAddress()) != null) {
      shaCrypt.reset();
      shaCrypt.update(mac);
      sink.write(shaCrypt.digest(), 0, 8);
    } else {
      sink.writeLong(random.nextLong());
    }

    writeValue(sink, 0x03);
    writeValue(sink, 0x36);
    sink.write("en".getBytes(StandardCharsets.UTF_8));
    sink.writeByte(0x00);

    writeValue(sink, 0x06);
    writeValue(sink, 0x33);
    writeValue(sink, 0x05);

    for (int i = 0; i < 0x05; i++) {
      sink.writeByte(0);
    }

    writeValue(sink, 0x03);
    writeValue(sink, 0x0D);
    sink.write("0/7.44.0.104".getBytes(StandardCharsets.UTF_8));
    sink.writeByte(0x00);

    writeValue(sink, 0x00);
    writeValue(sink, 0x0E);
    writeValue(sink, 0x7F000001);

    byte[] bytes = baos.toByteArray();

    dos.write(magicResponse);
    dos.writeShort(bytes.length + 2);

    byte[] iv = new byte[16];
    Arrays.fill(iv, (byte) 0x00);

    aesCipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(sha, "AES"), new IvParameterSpec(iv));
    byte[] encrypted = aesCipher.doFinal(bytes);
    dos.write(encrypted);

    int crc = -1;
    for (int i = 0; i < encrypted.length; i++) {
      crc = crc32_tab[(crc ^ encrypted[i]) & 0xFF] ^ crc >>> 8;
    }

    dos.writeByte((byte) crc);
    dos.writeByte((byte) (crc >>> 8));

    dos.flush();

    logger.finest("Sent UIC payload to server");

    byte[] response = new byte[3];
    dis.readFully(response);
    if (!Arrays.equals(magicResponse, response)) {
      socket.close();
      logger.severe("Wrong magic received from server after payload");
      return null;
    }

    int size = dis.readShort() - 2;
    byte[] received = new byte[size];
    dis.readFully(received);

    iv[3] = 0x01;
    iv[7] = 0x01;
    aesCipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(sha, "AES"), new IvParameterSpec(iv));
    byte[] decrypted = aesCipher.doFinal(received);

    byte[] signedCredentials = null;

    int[] position = {0};
    outer: while (position[0] < decrypted.length) {
      position[0]++;
      int numberObjects = readValue(decrypted, position);
      for (int i = 0; i < numberObjects; i++) {
        int family = decrypted[position[0]++];
        int id = readValue(decrypted, position);
        switch (family) {
          case 0x00:
            int value = readValue(decrypted, position);
            if (id == 0x01 && value != 4200) {
              logger.severe("Received LOGIN_CODE != LOGIN_OK: received code " + value);
              break outer;
            }
            break;
          case 0x04:
            int blobSize = readValue(decrypted, position);
            if (id == 0x24) {
              signedCredentials = new byte[blobSize];
              System.arraycopy(decrypted, position[0], signedCredentials, 0, blobSize);
              break outer;
            }
            position[0] += blobSize;
            break;
          default:
            break outer;
        }
      }
    }

    socket.close();

    if (signedCredentials == null) {
      logger.severe("No credentials received after payload");
      return null;
    }

    byte[] salt = "WS-SecureConversationSESSION KEY TOKEN".getBytes(StandardCharsets.UTF_8);
    byte[] nonceBytes = nonce.getBytes(StandardCharsets.UTF_8);
    byte[] challenge = new byte[20 + salt.length + nonceBytes.length];

    shaCrypt.reset();
    shaCrypt.update(signedCredentials);
    shaCrypt.update(salt);

    System.arraycopy(shaCrypt.digest(), 0, challenge, 0, 20);
    System.arraycopy(salt, 0, challenge, 20, salt.length);
    System.arraycopy(nonceBytes, 0, challenge, 20 + salt.length, nonceBytes.length);

    byte[] challengeSigned = new byte[0x80];
    challengeSigned[0] = (byte) 0x4B;
    int challengePosition;
    for (challengePosition = 1; challengePosition < 0x80 - challenge.length - 20 - 2; challengePosition++) {
      challengeSigned[challengePosition] = (byte) 0xBB;
    }
    challengeSigned[challengePosition++] = (byte) 0xBA;
    System.arraycopy(challenge, 0, challengeSigned, challengePosition, challenge.length);
    challengePosition += challenge.length;

    shaCrypt.reset();
    shaCrypt.update(challenge);
    System.arraycopy(shaCrypt.digest(), 0, challengeSigned, challengePosition, 20);
    challengePosition += 20;

    challengeSigned[challengePosition] = (byte) 0xBC;

    byte[] challengeEncrypted = new byte[challengeSigned.length + 4 + signedCredentials.length];

    challengeEncrypted[0] = (byte) (signedCredentials.length >> 24);
    challengeEncrypted[1] = (byte) (signedCredentials.length >> 16);
    challengeEncrypted[2] = (byte) (signedCredentials.length >> 8);
    challengeEncrypted[3] = (byte) signedCredentials.length;

    System.arraycopy(signedCredentials, 0, challengeEncrypted, 4, signedCredentials.length);

    rsaCipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
    rsaCipher.doFinal(challengeSigned, 0, challengeSigned.length, challengeEncrypted, 4 + signedCredentials.length);

    String uic = new String(Base64.getEncoder().encode(challengeEncrypted), StandardCharsets.UTF_8);

    logger.finest("Computed UIC succesfully (uic length:" + uic.length() + ")");

    return uic;
  }

  private static int readValue(byte[] bytes, int[] position) {
    int result;
    int a;
    for (a = 0, result = 0; a == 0 || (bytes[position[0] - 1] & 0x80) != 0; a += 7, position[0]++) {
      result |= (bytes[position[0]] & 0x7F) << a;
    }
    return result;
  }

  private static void writeValue(DataOutputStream dos, int value) throws IOException {
    int a;
    for (a = value; a > 0x7F; a >>>= 7) {
      dos.writeByte(a & 0x7F | 0x80);
    }
    dos.writeByte(a & 0x7F);
  }

}
