package org.sparcs.gpgchat.channel;

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
	public static String msgFormat = "%s:%s:%s";
	public static String helloFormat = "hello:%s";
	public static String keyFormat = "%s:%s:%s:%s";
	public Pattern msgPattern = Pattern.compile("message:([^:\\s]+):([^:\\s]+)", Pattern.DOTALL);
	public Pattern askPattern = Pattern.compile("ask:([^:\\s]+):([^:\\s]+)", Pattern.DOTALL);
	public Pattern ansPattern = Pattern.compile("ans:([^:\\s]+):([^:\\s]+)", Pattern.DOTALL);
	public Pattern keyPattern = Pattern.compile("([^:\\s]+):([^:\\s]+):([^:\\s]+):([^:\\s]+)", Pattern.DOTALL);
	public Pattern helloPattern = Pattern.compile("hello:(.+)", Pattern.DOTALL);

	private byte[] key = new byte[16];
	private byte[] ivKey = new byte[16];
	private byte[] verification = new byte[16];
	private Cipher encrypter;
	private String fakeID;

	private GPG gpg;

	private MessageInterface messager;
	private MessageReceiver receiver;
	private MessageReceiver systemInfo;

	private Map<String, UserKeyMap> userKeyMap = new HashMap<String, UserKeyMap>();
	
	private String challenge;
	private void newChallenge()
	{
		byte[] temp = new byte[16];
		Random r = new SecureRandom();
		r.nextBytes(temp);
		this.challenge = Base64.encodeBase64String(temp);
	}

	
	public Channel(MessageInterface messager, GPG gpg, MessageReceiver system) {
		try {
			this.messager = messager;
			this.systemInfo = system;
			messager.registerReceiver(this);
			this.gpg = gpg;

			Random r = new SecureRandom();
			
			r.nextBytes(key);
			r.nextBytes(ivKey);
			r.nextBytes(verification);

			this.encrypter = Cipher.getInstance(transformation);
			
			IvParameterSpec iv = new IvParameterSpec(ivKey);

			SecretKeySpec k = new SecretKeySpec(key, "AES");
			this.encrypter.init(Cipher.ENCRYPT_MODE, k, iv);

			this.fakeID = Long.toHexString(r.nextLong());
			newChallenge();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static Channel createChannel(MessageInterface messager, GPG gpg) {
		return new Channel(messager, gpg, new MessageReceiver() {
			
			@Override
			public void receiveMessage(String message) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	
	public static Channel createChannel(MessageInterface messager, GPG gpg, MessageReceiver system) {
		return new Channel(messager, gpg, system);
	}

	private String decryptMessage(String message) throws IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {

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
		if(userMap == null)
			return null;
		String start = userMap.realUsername;
		if(!userMap.isTrusted)
		{
			//systemInfo.receiveMessage("[WARNING] Unverified message, this message may be a duplicated copy of previous messages.");
			sendAsk();
			start = "[Untrusted] " + start;
		}
		String decryptedMessage = userMap.decrypt(msg);
		if(decryptedMessage == null)
			return null;
		else
			return start + ": " + decryptedMessage;
	}
	
	private void decryptAns(String message) throws IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {

		if (message == null) {
			return;
		}

		Matcher matcher = ansPattern.matcher(message);
		if (!matcher.find()) {
			return;
		}

		String fakeID = matcher.group(1);
		String msg = matcher.group(2);

		UserKeyMap userMap = userKeyMap.get(fakeID);
		if(userMap == null)
			return;
		
		String ans = userMap.decrypt(msg);
		if(ans == null)
			return;
		
		if(userMap.isTrusted || this.challenge == null)
			return;
		
		if(!ans.equals(this.challenge))
			return;
		userMap.isTrusted = true;
		
		systemInfo.receiveMessage(userMap.realUsername + " has been trusted.");
	}
	
	private void replyAsk(String message) throws IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {

		if (message == null) {
			return;
		}

		Matcher matcher = askPattern.matcher(message);
		if (!matcher.find()) {
			return;
		}

		String fakeID = matcher.group(1);
		String msg = matcher.group(2);

		UserKeyMap userMap = userKeyMap.get(fakeID);
		if(userMap == null)
			return;
		String ans = userMap.decrypt(msg);
		if(ans == null)
			return;
		sendMessage("ans", ans);
		
		//systemInfo.receiveMessage("replied to " + userMap.realUsername + "'s message.");
	}
	
	private void sendAsk() throws IllegalBlockSizeException, InvalidKeyException, InvalidAlgorithmParameterException {
		newChallenge();
		sendMessage("ask", this.challenge);
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
		String verificationString = keyMatcher.group(4);
		String realName = gpgProfile[2];
		byte[] userKey = Base64.decodeBase64(userKeyStr);
		byte[] ivKey = Base64.decodeBase64(ivStr);
		byte[] verify = Base64.decodeBase64(verificationString);

		UserKeyMap userMapKey = new UserKeyMap(realName, userKey, ivKey, verify);
		if(!userKeyMap.containsKey(fakeID))
		{
			systemInfo.receiveMessage(userMapKey.realUsername + " with fake ID " + fakeID + " sent a hello message.");
			userKeyMap.put(fakeID, userMapKey);
		}
	}

	public synchronized void sendHello(List<Key> receivers) {
		String keyStr = Base64.encodeBase64String(this.key);
		String ivStr = Base64.encodeBase64String(this.ivKey);
		String verifiString = Base64.encodeBase64String(verification);
		String keyMessage = String.format(keyFormat, this.fakeID, keyStr, ivStr, verifiString);
		String helloMessage = String.format(helloFormat, Base64.encodeBase64String(StringUtils.getBytesUtf8(gpg.encrypt(keyMessage, receivers, null))));
		

		this.messager.sendMessage(helloMessage);
	}

	@Override
	public synchronized void sendMessage(String message) {
		sendMessage("message", message);
	}
	
	private void sendMessage(String type, String message) {
		try {
			byte[] rawByte = message.getBytes("UTF-8");
			byte[] total = new byte[verification.length + rawByte.length];
			
			for(int k=0; k<verification.length; k++)
				total[k] = verification[k];
			
			for(int k=0; k<rawByte.length; k++)
				total[k+verification.length] = rawByte[k];
			
			byte[] encByte = this.encrypter.doFinal(total);
			String encyptMessage = Base64.encodeBase64String(encByte);
			
			for(int k=0; k<ivKey.length; k++)
			{
				ivKey[k] = (byte) (ivKey[k] ^ encByte[k]);
			}
			
			IvParameterSpec iv = new IvParameterSpec(ivKey);
			SecretKeySpec k = new SecretKeySpec(key, "AES");
			
			this.encrypter.init(Cipher.ENCRYPT_MODE, k, iv);
			this.messager.sendMessage(String.format(msgFormat, type, this.fakeID, encyptMessage));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public synchronized void registerReceiver(MessageReceiver receiver) {
		this.receiver = receiver;
	}

	@Override
	public synchronized void receiveMessage(String message) {
		// check if message is hello or message
		if (message.startsWith("message")) {
			try {
				String msg = decryptMessage(message);
				if (msg == null) {
					return;
				} else {
					this.receiver.receiveMessage(msg);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(message.startsWith("ask")) {
			try {
				replyAsk(message);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else if(message.startsWith("ans")) {
			try {
				decryptAns(message);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		else if (message.startsWith("hello")) {
			try {
				addUser(message);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
			return;
		}

	}

	public synchronized String toString() {
		return "Channel which uses fake ID (" + this.fakeID + ")";
	}

	private class UserKeyMap {
		String realUsername;
		Cipher decrypter;
		SecretKeySpec k;
		IvParameterSpec iv;
		boolean isTrusted;
		byte[] verify;
		//int failed;

		public UserKeyMap(String username, byte[] userKey, byte[] ivKey, byte[] verify)
				throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
				InvalidAlgorithmParameterException {

			this.isTrusted = false;
			//this.failed = 0;
			this.realUsername = username;
			this.decrypter = Cipher.getInstance(Channel.transformation);
			this.k = new SecretKeySpec(userKey, "AES");
			this.iv = new IvParameterSpec(ivKey);
			this.decrypter.init(Cipher.DECRYPT_MODE, k, iv);
			this.verify = verify;
		}

		public String decrypt(String encryptedMsg) throws InvalidKeyException, InvalidAlgorithmParameterException {
			try {
				byte[] encByte = Base64.decodeBase64(encryptedMsg);
				byte[] decyptedByte = decrypter.doFinal(encByte);
				if(encByte == null || decyptedByte == null)
					return null;
				byte[] ivKey = iv.getIV();
				for(int k=0; k<ivKey.length; k++)
				{
					ivKey[k] = (byte) (ivKey[k] ^ encByte[k]);
				}
				
				for(int k=0; k<verify.length; k++)
				{
					if(decyptedByte[k] != verify[k])
						return null;
				}
				
				String ret = new String(decyptedByte, verify.length, decyptedByte.length-verify.length, "UTF-8");
				this.iv = new IvParameterSpec(ivKey);
				return ret;
			} catch (Exception e) {
				return null;
			}
			finally
			{
				this.decrypter.init(Cipher.DECRYPT_MODE, k, iv);
			}
		}
	}
}