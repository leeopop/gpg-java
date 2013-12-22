package org.sparcs.gpgchat.channel;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import org.sparcs.gpgchat.gpg.GPG;
import org.sparcs.gpgchat.gpg.Key;
import org.sparcs.gpgchat.message.MessageInterface;
import org.sparcs.gpgchat.message.MessageReceiver;

public class Channel implements MessageInterface, MessageReceiver {

	public static String transformation = "AES/CBC/PKCS5Padding";
	public static String msgFormat = "message:%s:%s";
	public static String helloFormat = "hello:%s";
	public static String keyFormat = "%s:%s:%s";
	public Pattern msgPattern = Pattern.compile("message:(\\w+):(\\w+)", Pattern.DOTALL);
	public Pattern keyPattern = Pattern.compile("(\\w+):(\\w+):(\\w+)", Pattern.DOTALL);
	public Pattern helloPattern = Pattern.compile("hello:(.+)", Pattern.DOTALL);

	private byte[] key = new byte[16];
	private byte[] ivKey = new byte[16];
	private Cipher encrypter;
	private String fakeID;

	private GPG gpg;

	private MessageInterface messager;
	private MessageReceiver receiver;

	private Map<String, UserKeyMap> userKeyMap = new HashMap<String, UserKeyMap>();

	public Channel(MessageInterface messager, GPG gpg) {
		try {
			this.messager = messager;
			messager.registerReceiver(this);
			this.gpg = gpg;

			Random r = new SecureRandom();
			r.nextBytes(key);
			r.nextBytes(ivKey);

			this.encrypter = Cipher.getInstance(transformation);
			
			IvParameterSpec iv = new IvParameterSpec(ivKey);

			SecretKeySpec k = new SecretKeySpec(key, "AES");
			this.encrypter.init(Cipher.ENCRYPT_MODE, k, iv);

			this.fakeID = Long.toHexString(r.nextLong());

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Channel createChannel(MessageInterface messager, GPG gpg) {
		return new Channel(messager, gpg);
	}

	public String decryptMessage(String message) throws IllegalBlockSizeException {

		if (message == null) {
			return null;
		}

		Matcher matcher = msgPattern.matcher(message);
		if (!matcher.find()) {
			return null;
		}

		String fakeID = matcher.group(1);
		String msg = matcher.group(2);

		UserKeyMap userMap = userKeyMap.get(fakeID);
		return userMap.decrypt(msg);
	}

	private void addUser(String encyptedHelloMessage) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {

		Matcher helloMatcher = helloPattern.matcher(encyptedHelloMessage);
		if (!helloMatcher.find()) {
			// invalid format message
			return;
		}
		String encryptedKey = helloMatcher.group(1);

		String[] gpgProfile = this.gpg.decrypt(StringUtils.newStringUtf8(Base64.decodeBase64(encryptedKey)));
		if (gpgProfile == null) {
			// un-trusted user's message
			return;
		}

		Matcher keyMatcher = keyPattern.matcher(gpgProfile[0]);
		if (!keyMatcher.find()) {
			// invalid format message
			return;
		}

		String fakeID = keyMatcher.group(1);
		String userKeyStr = keyMatcher.group(2);
		String ivStr = keyMatcher.group(3);
		String realName = gpgProfile[2];
		byte[] userKey = Base64.decodeBase64(userKeyStr);
		byte[] ivKey = Base64.decodeBase64(ivStr);

		UserKeyMap userMapKey = new UserKeyMap(realName, userKey, ivKey);
		userKeyMap.put(fakeID, userMapKey);
	}

	public void sendHello(List<Key> receivers) {
		String keyStr = Base64.encodeBase64String(this.key);
		String ivStr = Base64.encodeBase64String(this.ivKey);
		String keyMessage = String.format(keyFormat, this.fakeID, keyStr, ivStr);
		String helloMessage = String.format(helloFormat, Base64.encodeBase64String(StringUtils.getBytesUtf8(gpg.encrypt(keyMessage, receivers, null))));
		

		this.messager.sendMessage(helloMessage);
	}

	@Override
	public void sendMessage(String message) {
		try {
			String encyptMessage = Base64.encodeBase64String(this.encrypter.doFinal(message.getBytes("UTF-8")));
			this.messager.sendMessage(String.format(msgFormat, this.fakeID, encyptMessage));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void registerReceiver(MessageReceiver receiver) {
		this.receiver = receiver;
	}

	@Override
	public void receiveMessage(String message) {
		// check if message is hello or message
		if (message.startsWith("message")) {
			try {
				String msg = decryptMessage(message);
				if (msg == null) {
					return;
				} else {
					this.receiver.receiveMessage(msg);
				}
			} catch (IllegalBlockSizeException e) {
				e.printStackTrace();
			}
		} else if (message.startsWith("hello")) {
			try {
				addUser(message);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			return;
		}

	}

	public String toString() {
		return "Channel which uses fake ID (" + this.fakeID + ")";
	}

}

class UserKeyMap {
	String realUsername;
	Cipher decrypter;

	public UserKeyMap(String username, byte[] userKey, byte[] ivKey)
			throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
			InvalidAlgorithmParameterException {

		this.realUsername = username;
		this.decrypter = Cipher.getInstance(Channel.transformation);
		SecretKeySpec k = new SecretKeySpec(userKey, "AES");
		IvParameterSpec iv = new IvParameterSpec(ivKey);
		this.decrypter.init(Cipher.DECRYPT_MODE, k, iv);
	}

	public String decrypt(String encryptedMsg) throws IllegalBlockSizeException {
		try {
			byte[] decyptedByte = decrypter.doFinal(Base64.decodeBase64(encryptedMsg));
			return new String(decyptedByte, "UTF-8");
		} catch (BadPaddingException | UnsupportedEncodingException e) {
			return null;
		}
	}
}