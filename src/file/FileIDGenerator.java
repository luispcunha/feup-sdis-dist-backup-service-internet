package file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class FileIDGenerator {
    public static String generateID(Path file) throws NoSuchAlgorithmException, IOException {
        String data = file.toAbsolutePath().toString() + Files.getLastModifiedTime(file) + Files.size(file);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] id = digest.digest(data.getBytes("UTF-8"));

        return bytesToHex(id);
    }

    public static String bytesToHex(byte[] hex) {

        StringBuilder ret = new StringBuilder();
        for (byte b : hex) {
            ret.append(String.format("%02x", b));
        }
        return ret.toString();
    }
}