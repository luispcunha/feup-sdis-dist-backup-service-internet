package file;

import chord.ChordNode;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ChunkIDGenerator {
    public static long generateID(String fileID, int chunkNo) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        String data = String.format("%s%d", fileID, chunkNo);

        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] id = digest.digest(data.getBytes("UTF-8"));

        return bytesToLong(id);
    }

    private static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(ChordNode.m);
        buffer.put(bytes, 0, ChordNode.m);
        buffer.flip();
        return Math.abs(buffer.getLong()) % (long) Math.pow(2, ChordNode.m);
    }
}