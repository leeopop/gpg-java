package org.sparcs.gpgchat.message;

public interface MessageInterface {

	public void sendMessage(String message);
	
	public void registerReceiver(MessageReceiver receiver);
}
