package com.supersohee.api.image.validation;

public final class ImageContentSignatures {
    private ImageContentSignatures() {
    }

    public static boolean isWebp(byte[] header) {
        return asciiAt(header, 0, "RIFF") && asciiAt(header, 8, "WEBP");
    }

    private static boolean asciiAt(byte[] bytes, int offset, String expected) {
        if (bytes.length < offset + expected.length()) return false;
        for (int index = 0; index < expected.length(); index++) {
            if (bytes[offset + index] != expected.charAt(index)) return false;
        }
        return true;
    }
}
