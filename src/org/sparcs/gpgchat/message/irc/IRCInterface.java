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

import org.sparcs.gpgchat.channel.Channel;
import org.sparcs.gpgchat.gpg.GPG;
import org.sparcs.gpgchat.message.MessageInterface;
import org.sparcs.gpgchat.message.MessageReceiver;

public class IRCInterface implements MessageInterface, IRCEventListener {
	private static final int MAX_LEN = 200;
	
	private ConnectionManager conn;
	private String initialMessage;
	private String IRCChannelName;
	private MessageReceiver listener;
	private String channelName;
	private Map<String, StringBuffer> channelBuffer;
	private jerklib.Channel IRCChannel;
	private GPG gpg;
	private Channel myChannel = null;
	MessageReceiver messageListener;
	
	private IRCInterface(GPG gpg, ConnectionManager conn, String channel, String initialMessage, MessageReceiver messageListener)
	{
		this.conn = conn;
		this.initialMessage = initialMessage;
		this.IRCChannelName = channel;
		this.channelBuffer = new HashMap<>();
		this.listener = null;
		this.channelName = null;
		this.gpg = gpg;
		this.messageListener = messageListener;
	}
	
	public static IRCInterface getInstance(GPG gpg, String host, int port, String channelID, String initialMessage, MessageReceiver messageListener)
	{
		Random r = new SecureRandom();
		String id = "a" + Long.toOctalString(r.nextLong());
		ConnectionManager conn = new ConnectionManager(new Profile(id));
		Session session = null;
		if(port > 0)
			session = conn.requestConnection(host, port);
		else
			session = conn.requestConnection(host);
		
		IRCInterface ret = new IRCInterface(gpg, conn, channelID, initialMessage, messageListener);
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
		if(this.channelName == null)
		{
			System.err.println("Channel not configured yet");
			return;
		}
		message = "GPGChannelData:" + this.channelName + ": " + message;
		message = message.replaceAll("[\r\n]+", "");
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
	
	public Channel createChannel(String name)
	{
		if(name == null)
		{
			Random r = new SecureRandom();
			this.channelName = Long.toHexString(r.nextLong());
		}
		else
			this.channelName = name;
		try {
			Channel channel = Channel.createChannel(this, gpg);
			channel.registerReceiver(this.messageListener);
			channel.sendHello(gpg.getTruestedKey());
			this.myChannel = channel;
			return channel;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Channel getChannel()
	{
		return myChannel;
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
		System.out.println(rawEvent.getRawEventData());
	}
	
	private void processMessage(String content)
	{
		Pattern line = Pattern.compile("GPGChannelData:(\\w+): (.*)", Pattern.DOTALL);
		Pattern pgp = Pattern.compile("hello:.*", Pattern.DOTALL);
		
		Matcher lineMatcher = line.matcher(content);
		if(lineMatcher.matches())
		{
			String channelName = lineMatcher.group(1);
			String channelData = lineMatcher.group(2);
			
			if(this.channelName == null)
			{
				Matcher pgpMatcher = pgp.matcher(channelData);
				if(pgpMatcher.matches())
				{
					this.createChannel(channelName);
				}
			}
			
			if(this.listener != null && this.channelName != null && this.channelName.equals(channelName))
			{
				this.listener.receiveMessage(channelData);
			}
		}
	}
}
