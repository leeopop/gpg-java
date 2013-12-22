package org.sparcs.gpgchat.channel;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
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

import org.sparcs.gpgchat.gpg.GPG;
import org.sparcs.gpgchat.message.MessageInterface;
import org.sparcs.gpgchat.message.MessageReceiver;

public class Channel implements MessageInterface, MessageReceiver {

	public static String transformation = "AES/CBC/PKCS5Padding";
	public static String msgFormat = "message:%s:%s";
	public static String helloFormat = "hello:%s:%s:%s";
	public Pattern msgPattern = Pattern.compile("message:([^()]+):(.*)");
	public Pattern helloPattern = Pattern.compile("hello:([^()]+):([^()]+):([^()]+)");

	private byte[] key = new byte[32];
	private byte[] ivKey = new byte[32];
	private Cipher encrypter;
	private String fakeID;

	private GPG gpg;

	private MessageInterface messager;
	private MessageReceiver receiver;

	private Map<String, UserKeyMap> userKeyMap = new HashMap<String, UserKeyMap>();

	public Channel(MessageInterface messager, GPG gpg) throws NoSuchAlgorithmException,
			NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
		this.messager = messager;
		this.gpg = gpg;

		Random r = new SecureRandom();
		r.nextBytes(key);
		r.nextBytes(ivKey);

		this.encrypter = Cipher.getInstance(transformation);
		IvParameterSpec iv = new IvParameterSpec(ivKey);

		SecretKeySpec k = new SecretKeySpec(key, "AES");
		this.encrypter.init(Cipher.ENCRYPT_MODE, k, iv);

		this.fakeID = Long.toHexString(r.nextLong());
	}

	public static Channel createChannel(MessageInterface messager, GPG gpg) throws Exception {
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

	public void addUser(String encyptedHelloMessage) throws InvalidKeyException,
			NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {

		String[] gpgProfile = this.gpg.decrypt(encyptedHelloMessage);
		if (gpgProfile == null) {
			// un-trusted user's message
			return;
		}

		Matcher matcher = helloPattern.matcher(gpgProfile[0]);
		if (!matcher.find()) {
			// invalid format message
			return;
		}

		String fakeID = matcher.group(1);
		String userKeyStr = matcher.group(2);
		String ivStr = matcher.group(3);
		String realName = gpgProfile[2];
		byte[] userKey = userKeyStr.getBytes();
		byte[] ivKey = ivStr.getBytes();

		UserKeyMap userMapKey = new UserKeyMap(realName, userKey, ivKey);
		userKeyMap.put(fakeID, userMapKey);
	}

	public void sendHello() {
		String keyStr = new String(this.key);
		String ivStr = new String(this.ivKey);
		String helloMessage = String.format(helloFormat, this.fakeID, keyStr, ivStr);
		// TODO
		// encrypted user message from gpg
		this.messager.sendMessage(helloMessage);
	}

	@Override
	public void sendMessage(String message) {
		try {
			String encyptMessage = new String(this.encrypter.doFinal(message.getBytes()));
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
			byte[] decyptedByte = decrypter.doFinal(encryptedMsg.getBytes());
			return new String(decyptedByte);
		} catch (BadPaddingException e) {
			return null;
		}
	}
}