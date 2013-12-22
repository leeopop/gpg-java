package org.sparcs.gpgchat;

import java.util.Scanner;

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
			System.out.println(message);
		}
		
	}

	public static void main(String[] args) {
		GPG gpg = GPG.getInstance(null);
		
		IRCInterface irc = IRCInterface.getInstance(gpg, "irc.ozinger.org", 6661, "#ella", null, new SimpleListener());
		Scanner in = new Scanner(System.in);
		System.out.print("Your private key id: ");
		String me = in.nextLine();
		System.out.print("Other private key id: ");
		String other = in.nextLine();
		for(Key key : gpg.getAllKeys())
		{
			if(key.uid.equals(me))
				gpg.setDefaultKey(key);
			if(key.uid.equals(other))
				gpg.addTrustedKey(key);
		}
		while(in.hasNextLine())
		{
			if(irc.getChannel() == null)
			{
				irc.createChannel(null);
			}
			
			irc.getChannel().sendMessage(in.nextLine());
		}
		in.close();
		irc.close();
	}

}
