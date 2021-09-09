package nl.andrewl.concord_server.util;

import java.security.SecureRandom;
import java.util.Random;

public class StringUtils {

	public static String random(int length) {
		Random random = new SecureRandom();
		final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-=+[]{}()<>";
		StringBuilder sb = new StringBuilder(length);
		for (int i = 0; i < length; i++) {
			sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
		}
		return sb.toString();
	}
}
