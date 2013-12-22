package org.sparcs.gpgchat.channel;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Random;

public class Channel {
	private byte[] key;
	
	public Channel()
	{
		key = new byte[32];
		Random r = new SecureRandom();
		r.nextBytes(key);
	}
}
