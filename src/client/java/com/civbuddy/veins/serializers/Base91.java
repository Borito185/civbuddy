package com.civbuddy.veins.serializers;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public final class Base91 {
    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789" +
                    "!#$%&()*+,./:;<=>?@[]^_`{|}~\"";

    private static final int[] DEC = new int[256];

    static {
        Arrays.fill(DEC, -1);
        for (int i = 0; i < ALPHABET.length(); i++) {
            DEC[ALPHABET.charAt(i)] = i;
        }
    }

    public static String encode(byte[] data) {
        StringBuilder out = new StringBuilder((int) (data.length * 1.23));

        int b = 0, n = 0;

        for (byte value : data) {
            b |= (value & 0xFF) << n;
            n += 8;

            if (n > 13) {
                int v = b & 8191;

                if (v > 88) {
                    b >>= 13;
                    n -= 13;
                } else {
                    v = b & 16383;
                    b >>= 14;
                    n -= 14;
                }

                out.append(ALPHABET.charAt(v % 91));
                out.append(ALPHABET.charAt(v / 91));
            }
        }

        if (n > 0) {
            out.append(ALPHABET.charAt(b % 91));
            if (n > 7 || b > 90) {
                out.append(ALPHABET.charAt(b / 91));
            }
        }

        return out.toString();
    }

    public static byte[] decode(String s) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        int b = 0, n = 0, v = -1;

        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i) < 256 ? DEC[s.charAt(i)] : -1;
            if (c == -1) continue;

            if (v == -1) {
                v = c;
            } else {
                v += c * 91;
                b |= v << n;

                if ((v & 8191) > 88) {
                    n += 13;
                } else {
                    n += 14;
                }

                while (n >= 8) {
                    out.write(b & 0xFF);
                    b >>= 8;
                    n -= 8;
                }

                v = -1;
            }
        }

        if (v != -1) {
            out.write((b | v << n) & 0xFF);
        }

        return out.toByteArray();
    }
}