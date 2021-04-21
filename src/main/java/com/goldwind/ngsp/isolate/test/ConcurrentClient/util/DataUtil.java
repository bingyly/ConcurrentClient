package com.goldwind.ngsp.isolate.test.ConcurrentClient.util;

import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
public class DataUtil {

    public static byte[] assembleData(int length) {
        byte[] groupId = getRandomByteArray();
        byte[] msgId = getRandomByteArray();
        byte[] payload = getRandomByteArray(length - groupId.length - msgId.length);
        return copyArray(groupId, msgId, payload, length);
    }

    public static byte[] assembleData(String filePath) {
        byte[] groupId = getRandomByteArray();
        byte[] msgId = getRandomByteArray();
        byte[] payload = fileToByteArray(filePath);
        int totalLength = groupId.length + msgId.length + payload.length;
        return copyArray(groupId, msgId, payload, totalLength);
    }

    private static byte[] fileToByteArray(String filePath) {
        byte[] payload = null;
        try (InputStream inputStream = new FileInputStream(filePath);
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            int length;
            byte[] buffer = new byte[1024];
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
            payload = outputStream.toByteArray();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return payload;
    }

    private static byte[] copyArray(byte[] groupId, byte[] msgId, byte[] payload, int length) {
        byte[] data = new byte[length];
        System.arraycopy(groupId, 0, data, 0, groupId.length);
        System.arraycopy(msgId, 0, data, groupId.length, msgId.length);
        System.arraycopy(payload, 0, data, groupId.length + msgId.length, payload.length);
        return data;
    }

    private static byte[] getRandomByteArray() {
        return getRandomByteArray(4);
    }

    private static byte[] getRandomByteArray(int length) {
        byte[] byteArray = new byte[length];
        ThreadLocalRandom.current().nextBytes(byteArray);
        return byteArray;
    }

    private static int byteArrayToInt(byte[] byteArray) {
        int value = 0;
        for (int i = 0; i < byteArray.length; i++) {
            int shift = (3 - i) * 8;
            value += (byteArray[i] & 0xFF) << shift;
        }
        return value;
    }

}