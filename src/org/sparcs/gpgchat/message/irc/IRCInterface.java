package org.sparcs.gpgchat.message.irc;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jerklib.ConnectionManager;
import jerklib.Profile;
import jerklib.Session;
import jerklib.events.IRCEvent;
import jerklib.events.JoinCompleteEvent;
import jerklib.events.MessageEvent;
import jerklib.listeners.IRCEventListener;

import org.sparcs.gpgchat.message.MessageInterface;
import org.sparcs.gpgchat.message.MessageReceiver;

public class IRCInterface implements MessageInterface, IRCEventListener {
	private static final int MAX_LEN = 100;
	
	private ConnectionManager conn;
	private String initialMessage;
	private String IRCChannelName;
	private MessageReceiver listener;
	private Map<String, StringBuffer> channelBuffer;
	private jerklib.Channel IRCChannel;
	MessageReceiver messageListener;
	
	private IRCInterface(ConnectionManager conn, String channel, String initialMessage, MessageReceiver messageListener)
	{
		this.conn = conn;
		this.initialMessage = initialMessage;
		this.IRCChannelName = channel;
		this.channelBuffer = new HashMap<>();
		this.listener = null;
		this.messageListener = messageListener;
	}
	
	public static IRCInterface getInstance(String host, int port, String channelID, String initialMessage, MessageReceiver messageListener)
	{
		Random r = new SecureRandom();
		String id = "a" + Long.toOctalString(r.nextLong());
		ConnectionManager conn = new ConnectionManager(new Profile(id));
		Session session = null;
		if(port > 0)
			session = conn.requestConnection(host, port);
		else
			session = conn.requestConnection(host);
		
		IRCInterface ret = new IRCInterface(conn, channelID, initialMessage, messageListener);
		session.addIRCEventListener(ret);
		return ret;
	}

	public void close()
	{
		conn.quit();
		conn = null;
		this.IRCChannel = null;
	}
	
	@Override
	public void finalize()
	{
		if(this.conn != null)
			this.close();
	}
	
	@Override
	public synchronized void sendMessage(String message) {
		message = message.replaceAll("[\r\n]+", " ");
		int remaining = message.length();
		int current = 0;
		while(remaining > 0)
		{
			int readLen = remaining > MAX_LEN ? MAX_LEN : remaining;
			String part = message.substring(current, current + readLen);
			remaining -= readLen;
			String mode = "GPGChat:";
			if(remaining == 0)
				mode += "e: ";
			else if(current == 0)
				mode += "s: ";
			else
				mode += "c: ";
			current += readLen;
			
			if(IRCChannel != null)
			{
				String send = mode + part;
				IRCChannel.say(send);
			}
		}
	}

	@Override
	public void registerReceiver(MessageReceiver receiver) {
		this.listener = receiver;
	}

	@Override
	public synchronized void receiveEvent(IRCEvent rawEvent) {
		Session session = rawEvent.getSession();
		switch(rawEvent.getType())
		{
		case CONNECT_COMPLETE:
		{
			if(initialMessage != null)
				session.sayRaw(initialMessage);
			session.join(this.IRCChannelName);
			
			break;
		}
		case JOIN_COMPLETE:
		{
			JoinCompleteEvent event = (JoinCompleteEvent)rawEvent;
			this.IRCChannel = event.getChannel();
		}
		case JOIN:
		{
			//JoinEvent event = (JoinEvent)rawEvent;
			break;
		}
		case QUIT:
		{
			//QuitEvent event = (QuitEvent)rawEvent;
			break;
		}
		case CHANNEL_MESSAGE:
		{
			MessageEvent event = (MessageEvent)rawEvent;
			String sender = event.getUserName();
			
			StringBuffer buffer = channelBuffer.get(sender);
			if(buffer == null)
			{
				buffer = new StringBuffer();
				channelBuffer.put(sender, buffer);
			}
			
			String message = event.getMessage();
			Pattern line = Pattern.compile("GPGChat:([sce]): (.*)", Pattern.DOTALL);
			Matcher m = line.matcher(message);
			if(m.matches())
			{
				String type = m.group(1);
				String content = m.group(2);
				
				switch(type)
				{
				case "s"://start
				{
					buffer.delete(0, buffer.length());
					buffer.append(content);
					break;
				}
				case "c":
				{
					buffer.append(content);
					break;
				}
				case "e":
				{
					buffer.append(content);
					String result = buffer.toString();
					buffer.delete(0, buffer.length());
					this.processMessage(result);
					break;
				}
				}
			}
			break;
		}
		case CONNECTION_LOST:
		{
			this.close();
			break;
		}
		default:
			break;
		}
	}

	private void processMessage(String content)
	{
		String channelData = content;

		if(this.listener != null)
		{
			this.listener.receiveMessage(channelData);
		}
	}
}
