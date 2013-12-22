package org.sparcs.gpgchat;

import java.util.Scanner;

import org.sparcs.gpgchat.channel.Channel;
import org.sparcs.gpgchat.gpg.GPG;
import org.sparcs.gpgchat.gpg.Key;
import org.sparcs.gpgchat.message.MessageReceiver;
import org.sparcs.gpgchat.message.irc.IRCInterface;

public class Main {
	
	static class SimpleListener implements MessageReceiver
	{

		@Override
		public void receiveMessage(String message) {
			// TODO Auto-generated method stub
			System.err.println(message);
		}
		
	}

	public static void main(String[] args) {
		GPG gpg = GPG.getInstance(null);
		
		for(Key key : gpg.getAllKeys())
		{
			if(key.uid.equals("leeopop"))
			{
				gpg.setDefaultKey(key);
			}
			if(key.uid.equals("elaborate"))
				gpg.addTrustedKey(key);
		}
		
		
		IRCInterface irc = IRCInterface.getInstance(gpg, "irc.luatic.net", 6661, "#ella", null, new SimpleListener());
		boolean first = true;
		Scanner in = new Scanner(System.in);
		Channel channel = null;
		while(in.hasNextLine())
		{
			if(first)
			{
				first = false;
				channel = irc.createChannel(null);
			}
			channel.sendMessage(in.nextLine());
		}
		in.close();
		irc.close();
	}

}
