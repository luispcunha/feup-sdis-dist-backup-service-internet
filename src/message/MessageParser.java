package message;

import message.backup.PutChunkMessage;
import message.backup.RedirectMessage;
import message.backup.StoredMessage;
import message.delete.DeleteMessage;
import message.delete.DeletedMessage;
import message.notify.NotifyMessage;
import message.ping.PingMessage;
import message.ping.PongMessage;
import message.predecessor.GetPredMessage;
import message.predecessor.SendPredMessage;
import message.reclaim.MovedMessage;
import message.reclaim.RemovedMessage;
import message.restore.ChunkMessage;
import message.restore.GetChunkMessage;
import message.shift.*;
import message.successor.GetListMessage;
import message.successor.GetSuccMessage;
import message.successor.SendListMessage;
import message.successor.SendSuccMessage;

import java.util.Arrays;

public class MessageParser {
    public static Message parseMessage(byte[] message) {
        int body_start = getBodyStart(message);

        if(body_start < 0)
            return null;

        byte[] header_bytes = Arrays.copyOfRange(message, 0, body_start - 4);
        byte[] body_bytes = Arrays.copyOfRange(message, body_start, message.length);

        String header = new String(header_bytes);
        String[] fields = header.split("\\s+");


        if (fields.length < 1) //FIXME: may change due to CTMessages
            return null;

        switch(fields[0]) {
            case "PUTCHUNK":
                if (fields.length != 5) return null;

                return new PutChunkMessage(fields[1], fields[2], fields[3], fields[4], body_bytes);

            case "REDIRECT":
                if (fields.length != 5) return null;

                return new RedirectMessage(fields[1], fields[2], fields[3], fields[4], body_bytes);

            case "STORED":
                if (fields.length != 4) return null;

                return new StoredMessage(fields[1], fields[2], fields[3]);

            case "DELETE":
                if(fields.length != 4) return null;

                return new DeleteMessage(fields[1], fields[2], fields[3]);

            case "DELETED":
                if(fields.length != 4) return null;

                return new DeletedMessage(fields[1], fields[2], fields[3]);

            case "GETCHUNK":
                if(fields.length != 4) return null;

                return new GetChunkMessage(fields[1], fields[2], fields[3]);

            case "CHUNK":
                if(fields.length != 4) return null;

                return new ChunkMessage(fields[1], fields[2], fields[3], body_bytes);

            case "REMOVED":
                if(fields.length != 5) return null;

                return new RemovedMessage(fields[1], fields[2], fields[3], fields[4], body_bytes);

            case "MOVED":
                if(fields.length != 4) return null;

                return new MovedMessage(fields[1], fields[2], fields[3]);

            case "SHIFT":
                if(fields.length != 5) return null;

                return new ShiftMessage(fields[1], fields[2], fields[3], fields[4]);

            case "NSHIFTED":
                if(fields.length != 5) return null;

                return new NotShiftedMessage(fields[1], fields[2], fields[3], fields[4]);

            case "SHIFTED":
                if(fields.length != 5) return null;

                return new ShiftedMessage(fields[1], fields[2], fields[3], fields[4]);

            case "SHIFT_RIGHT":
                if(fields.length != 5) return null;

                return new ShiftRightMessage(fields[1], fields[2], fields[3], fields[4], body_bytes);

            case "SHIFT_LEFT":
                if(fields.length != 5) return null;

                return new ShiftLeftMessage(fields[1], fields[2], fields[3], fields[4], body_bytes);

            case "SHIFT_CHUNK":
                if(fields.length != 5) return null;

                return new ShiftChunkMessage(fields[1], fields[2], fields[3], fields[4], body_bytes);

            case "SHIFT_GETCHUNK":
                if(fields.length != 4) return null;

                return new ShiftGetChunkMessage(fields[1], fields[2], fields[3]);

            case "NOTIFY":
                if(fields.length != 5) return null;

                return new NotifyMessage(fields[1], fields[2], fields[3], fields[4]);

            case "PONG":
                if(fields.length != 2) return null;

                return new PongMessage(fields[1]);

            case "PING":
                if(fields.length != 2) return null;

                return new PingMessage(fields[1]);

            case "GET_PREDECESSOR":
                if(fields.length != 2) return null;

                return new GetPredMessage(fields[1]);

            case "PREDECESSOR":
                if(fields.length != 2) return null;

                return new SendPredMessage(fields[1], body_bytes);

            case "GET_SUCCESSOR":
                if(fields.length != 3) return null;

                return new GetSuccMessage(fields[1], fields[2]);

            case "SUCCESSOR":
                if(fields.length != 3) return null;

                return new SendSuccMessage(fields[1], fields[2], body_bytes);

            case "GET_LIST":
                if(fields.length != 2) return null;

                return new GetListMessage(fields[1]);

            case "LIST":
                if(fields.length != 2) return null;

                return new SendListMessage(fields[1], body_bytes);

            default:
                return null;
        }
    }

    private static int getBodyStart(byte[] message) {
        int body_start = -1;
        boolean crlf_detected = false;

        for (int i = 0; i < message.length - 1; i++){
            if(message[i] == (byte) '\r' && message[i + 1] == (byte) '\n'){
                if(crlf_detected){
                    body_start = i + 2;
                    break;
                } else {
                    crlf_detected = true;
                    i++;
                }
            } else
                crlf_detected = false;
        }

        return body_start;
    }
}