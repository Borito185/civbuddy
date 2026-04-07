package com.civbuddy.veins.serializers;

import com.civbuddy.veins.VeinShareClient;
import com.civbuddy.veins.data.markings.VeinMarkingRow;
import org.joml.Vector3i;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.*;

public final class ShareMarkingSerializer {

    public static String encode(Collection<VeinShareClient.ShareMarking> markings) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        writeVarInt(out, markings.size());

        Vector3i prevPos = new Vector3i(0, 0, 0);
        Vector3i prevRange = new Vector3i(0, 0, 0);

        for (VeinShareClient.ShareMarking s : markings) {
            Vector3i p = s.pos;
            Vector3i r = s.range;

            // 🔥 delta pos
            writeVarInt(out, zigzagEncode(p.x - prevPos.x));
            writeVarInt(out, zigzagEncode(p.y - prevPos.y));
            writeVarInt(out, zigzagEncode(p.z - prevPos.z));

            // 🔥 flags packed into 1 byte
            boolean uniform = (r.x == r.y && r.y == r.z);
            int flags = (s.isRemove ? 1 : 0) | (uniform ? 2 : 0);
            out.write(flags);

            // 🔥 delta range
            if (uniform) {
                writeVarInt(out, zigzagEncode(r.x - prevRange.x));
            } else {
                writeVarInt(out, zigzagEncode(r.x - prevRange.x));
                writeVarInt(out, zigzagEncode(r.y - prevRange.y));
                writeVarInt(out, zigzagEncode(r.z - prevRange.z));
            }

            prevPos.set(p);
            prevRange.set(r);
        }

        byte[] raw = out.toByteArray();
        byte[] compressed = deflate(raw);

        return Base91.encode(compressed);
    }

    public static List<VeinShareClient.ShareMarking> decode(String e) {
        byte[] compressed = Base91.decode(e);
        byte[] data = inflate(compressed);
        ByteBuffer buf = ByteBuffer.wrap(data);

        int count = readVarInt(buf);
        List<VeinShareClient.ShareMarking> out = new ArrayList<>(count);

        Vector3i prevPos = new Vector3i(0, 0, 0);
        Vector3i prevRange = new Vector3i(0, 0, 0);

        for (int i = 0; i < count; i++) {
            // 🔥 delta pos
            int x = prevPos.x + zigzagDecode(readVarInt(buf));
            int y = prevPos.y + zigzagDecode(readVarInt(buf));
            int z = prevPos.z + zigzagDecode(readVarInt(buf));

            int flags = buf.get();
            boolean isRemove = (flags & 1) != 0;
            boolean uniform = (flags & 2) != 0;

            int rx, ry, rz;

            if (uniform) {
                int dx = zigzagDecode(readVarInt(buf));
                rx = prevRange.x + dx;
                ry = prevRange.y + dx;
                rz = prevRange.z + dx;
            } else {
                rx = prevRange.x + zigzagDecode(readVarInt(buf));
                ry = prevRange.y + zigzagDecode(readVarInt(buf));
                rz = prevRange.z + zigzagDecode(readVarInt(buf));
            }

            Vector3i pos = new Vector3i(x, y, z);
            Vector3i range = new Vector3i(rx, ry, rz);

            prevPos.set(pos);
            prevRange.set(range);

            out.add(new VeinShareClient.ShareMarking(
                    new VeinMarkingRow(-1L, pos, range),
                    isRemove
            ));
        }

        return out;
    }

    private static int zigzagEncode(int n) {
        return (n << 1) ^ (n >> 31);
    }

    private static int zigzagDecode(int n) {
        return (n >>> 1) ^ -(n & 1);
    }

    private static void writeVarInt(ByteArrayOutputStream out, int value) {
        while ((value & ~0x7F) != 0) {
            out.write((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.write(value);
    }

    private static int readVarInt(ByteBuffer buf) {
        int value = 0;
        int position = 0;
        byte current;

        do {
            current = buf.get();
            value |= (current & 0x7F) << position;
            position += 7;
        } while ((current & 0x80) != 0);

        return value;
    }

    private static byte[] deflate(byte[] input) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true); // true = no zlib header
        deflater.setInput(input);
        deflater.finish();

        byte[] buffer = new byte[512];
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        while (!deflater.finished()) {
            int len = deflater.deflate(buffer);
            out.write(buffer, 0, len);
        }

        return out.toByteArray();
    }

    private static byte[] inflate(byte[] input) {
        Inflater inflater = new Inflater(true); // raw mode
        inflater.setInput(input);

        byte[] buffer = new byte[512];
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            while (!inflater.finished()) {
                int len = inflater.inflate(buffer);
                out.write(buffer, 0, len);
            }
        } catch (DataFormatException e) {
            throw new RuntimeException(e);
        }

        return out.toByteArray();
    }
}