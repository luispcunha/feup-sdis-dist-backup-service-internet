package file;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class FileHandler {
    public static final long MAX_NUM_CHUNKS = 1000000;

    private Path path;
    private String id;
    private List<Chunk> chunks = new ArrayList<Chunk>();

    public FileHandler(String path) throws IOException, FileSizeException, NoSuchAlgorithmException {
        this.path = Paths.get(path);

        if (Files.size(this.path) > Chunk.MAX_SIZE * MAX_NUM_CHUNKS) {
            throw new FileSizeException(path);
        }

        this.id = FileIDGenerator.generateID(this.path);
    }

    public String getAbsolutePath() {
        return this.path.toAbsolutePath().toString();
    }

    public void loadChunks(CompletionHandler<Integer, ByteBuffer> completionHandler) throws IOException {
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(this.path, StandardOpenOption.READ);
        ByteBuffer buffer = ByteBuffer.allocate((int) Files.size(this.path));
        long position = 0;

        fileChannel.read(buffer, position, buffer, completionHandler);
    }

    public List<Chunk> getChunks() {
        return this.chunks;
    }

    public String getID() {
        return this.id;
    }
}
