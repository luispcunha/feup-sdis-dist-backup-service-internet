package filesystem;

import file.Chunk;
import file.ChunkKey;
import peer.Peer;
import util.Log;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.*;
import java.util.Collections;
import java.util.List;

public class FileSystem {
    private String fileSystemPrefix;
    private final String CHUNKS_PATH_PREFIX = "chunks/";
    private final String RECOVERED_PATH_PREFIX = "recovered/";
    private final String PERSISTENT_STATE_PATH = ".state";
    private Path chunks;
    private Path recovered;
    private Path state_path;

    public FileSystem(Peer peer) {
        this.fileSystemPrefix = "peer_" + peer.getID();

        chunks = Paths.get(this.fileSystemPrefix, CHUNKS_PATH_PREFIX);
        try {
            chunks = Files.createDirectories(chunks);
        } catch (IOException e) {
            Log.logError(e.getMessage());
        }

        recovered = Paths.get(this.fileSystemPrefix, RECOVERED_PATH_PREFIX);
        try {
            recovered = Files.createDirectories(recovered);
        } catch (IOException e) {
            Log.logError(e.getMessage());
        }

        state_path = Paths.get(this.fileSystemPrefix, PERSISTENT_STATE_PATH);
    }

    public int storeChunk(Chunk chunk) throws IOException {
        Path dir = chunks.resolve(chunk.getFileID());
        try {
            dir = Files.createDirectories(dir);
        } catch (FileAlreadyExistsException e) { }

        Path chunkFile = dir.resolve(Integer.toString(chunk.getNumber()));

        if (! Files.exists(chunkFile)) {
            Files.createFile(chunkFile);
        }

        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(chunkFile, StandardOpenOption.WRITE);

        ByteBuffer buffer = ByteBuffer.allocate(Chunk.MAX_SIZE);
        long position = 0;

        buffer.put(chunk.getContent());
        buffer.flip();

        fileChannel.write(buffer, position);

        return 0;
    }

    public int loadChunk(String fileID, int chunkNumber, CompletionHandler<Integer, ByteBuffer> completionHandler) throws IOException {
        Path file = chunks.resolve(fileID).resolve(Integer.toString(chunkNumber));

        if (! Files.exists(file)) {
            return -1;
        }

        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(file, StandardOpenOption.READ);
        ByteBuffer buffer = ByteBuffer.allocate(Chunk.MAX_SIZE);
        long position = 0;

        fileChannel.read(buffer, position, buffer, completionHandler);

        return 0;
    }

    public int deleteChunk(ChunkKey chunkKey) {
        Path path = chunks.resolve(chunkKey.getFileID()).resolve(Integer.toString(chunkKey.getNumber()));

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) { }

        Path parent = path.getParent();
        try {
            if (Files.isDirectory(parent) && ! Files.list(parent).findAny().isPresent())
                Files.deleteIfExists(parent);
        } catch (IOException e) { }

        return 0;
    }

    public int restoreFile(List<Chunk> chunks) {
        if (chunks.size() <= 0)
            return -1;

        Collections.sort(chunks);

        Path path = recovered.resolve(chunks.get(0).getFileID());

        try {
            Files.deleteIfExists(path);
        } catch (IOException e) { }

        AsynchronousFileChannel fileChannel = null;
        try {
            path = Files.createFile(path);
            fileChannel = AsynchronousFileChannel.open(path, StandardOpenOption.WRITE);
        } catch (IOException e) {
            Log.logError("Failed restoring file from chunks");
            return -1;
        }

        Log.log("Restored file " + chunks.get(0).getFileID());

        for (int i = 0; i < chunks.size(); i++) {
            ByteBuffer buffer = ByteBuffer.allocate(chunks.get(i).getContent().length);
            long position = Chunk.MAX_SIZE * i;

            buffer.put(chunks.get(i).getContent());
            buffer.flip();

            fileChannel.write(buffer, position);
            buffer.clear();
        }

        return 0;
    }

    public void storeState(PeerState state) {
        if (! state.modified())
            return;

        byte[] outBytes;
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out);
            oos.writeObject(state);
            out.close();
            outBytes = out.toByteArray();
        } catch (IOException e) {
            Log.logError(e.getMessage());
            return;
        }

        if (! Files.exists(this.state_path)) {
            try {
                if (! Files.exists(Paths.get(this.fileSystemPrefix))) {
                    Files.createDirectory(Paths.get(this.fileSystemPrefix));
                }
                Files.createFile(this.state_path);
            } catch (IOException e) {
                Log.logError(e.getMessage());
            }
        }

        AsynchronousFileChannel fileChannel = null;
        try {
            fileChannel = AsynchronousFileChannel.open(this.state_path, StandardOpenOption.WRITE);
        } catch (IOException e) {
            Log.logError(e.getMessage());
            return;
        }

        ByteBuffer buffer = ByteBuffer.allocate(outBytes.length);
        long position = 0;

        buffer.put(outBytes);
        buffer.flip();

        fileChannel.write(buffer, position);
    }

    public PeerState loadState() {

        if (! Files.exists(this.state_path))
            return null;

        try {
            FileInputStream fileIn = new FileInputStream(fileSystemPrefix + '/' + PERSISTENT_STATE_PATH);
            ObjectInputStream objectIn = new ObjectInputStream(fileIn);
            PeerState state = (PeerState) objectIn.readObject();
            objectIn.close();
            fileIn.close();
            return state;
        } catch (IOException | ClassNotFoundException e) {
            Log.logError("Failed loading state");
            return null;
        }
    }
}