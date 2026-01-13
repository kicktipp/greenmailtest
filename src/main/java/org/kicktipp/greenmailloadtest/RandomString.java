package org.kicktipp.greenmailloadtest;

import java.util.Random;

public class RandomString {
    static int leftLimit = 97; // letter 'a'
    static int rightLimit = 122; // letter 'z'
    static Random random = new Random();

    public static String getText(int length) {
        return random.ints(leftLimit, rightLimit + 1)
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();

    }
}
