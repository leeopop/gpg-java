package org.sparcs.gpgchat.gpg;

import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

public class GPG {

	private String gpg;

	private GPG(String gpg)
	{
		this.gpg = gpg;
	}

	public static GPG getInstance(String path)
	{
		try
		{
			if(path == null)
				path = "gpg";
			String [] command = new String[2];
			command[0] = path;
			command[1] = "--version";
			Process gpg = Runtime.getRuntime().exec(command);
			if(gpg.waitFor() == 0)
				return new GPG(command[0]);
			else
				return null;
		}
		catch(Exception e)
		{
			//e.printStackTrace();
			return null;
		}
	}

	public static void main(String[] args) //TODO just for testing
	{
		GPG gpg = GPG.getInstance("C:\\Program Files (x86)\\GNU\\GnuPG\\gpg2.exe");
		List<Key> all = gpg.getAllKeys();
		List<Key> rem = new LinkedList<>();
		for(Key k : all)
		{
			if(!(k.uid.equals("leeopop") || k.uid.equals("elaborate")))
				rem.add(k);
		}
		for(Key k : rem)
			all.remove(k);
		System.out.println(all);
		
		String message1 = gpg.encrypt("Message", all, null);
		String message2 = gpg.encrypt("Message", all, all.get(0));
		String message3 = gpg.encrypt("Message", all, all.get(1));
		System.out.println(message1);
		System.out.println(message2);
		System.out.println(message3);
	}

	public List<Key> getAllKeys()
	{
		try
		{
			Pattern keyIDPattern = Pattern.compile("pub\\s+\\w+/(\\w+)\\s+.*");
			Pattern uidPattern = Pattern.compile("uid\\s+(\\S+[^<>]*\\S+)\\s+<([^<>]*)>.*");


			List<Key> ret = new LinkedList<>();
			String [] command = new String[2];
			command[0] = gpg;
			command[1] = "--list-keys";

			Process p = Runtime.getRuntime().exec(command);

			Scanner in = new Scanner(p.getInputStream());
			String keyID = null;
			String uid = null;
			String email = null;
			while(in.hasNextLine())
			{
				String data = in.nextLine();
				if(data.length() == 0)
				{
					if (keyID == null)
						break;
					Key k = new Key();
					k.keyID = keyID;
					k.uid = uid;
					k.email = email;
					ret.add(k);
					keyID = null;
					uid = null;
					email = null;
				}

				Matcher m = keyIDPattern.matcher(data);
				if(keyID == null && m.matches())
				{
					keyID = m.group(1);
				}

				m = uidPattern.matcher(data);
				if(uid == null && m.matches())
				{
					uid = m.group(1);
					email = m.group(2);
				}
			}

			in.close();
			if(p.waitFor() == 0)
				return ret;
			else
				return null;
		}
		catch(Exception e)
		{
			return null;
		}
	}
	
	public List<Key> getSecretKeys()
	{
		try
		{
			Pattern keyIDPattern = Pattern.compile("sec\\s+\\w+/(\\w+)\\s+.*");
			Pattern uidPattern = Pattern.compile("uid\\s+(\\S+[^<>]*\\S+)\\s+<([^<>]*)>.*");


			List<Key> ret = new LinkedList<>();
			String [] command = new String[2];
			command[0] = gpg;
			command[1] = "--list-secret-keys";

			Process p = Runtime.getRuntime().exec(command);

			Scanner in = new Scanner(p.getInputStream());
			String keyID = null;
			String uid = null;
			String email = null;
			while(in.hasNextLine())
			{
				String data = in.nextLine();
				if(data.length() == 0)
				{
					if (keyID == null)
						break;
					Key k = new Key();
					k.keyID = keyID;
					k.uid = uid;
					k.email = email;
					ret.add(k);
					keyID = null;
					uid = null;
					email = null;
				}

				Matcher m = keyIDPattern.matcher(data);
				if(keyID == null && m.matches())
				{
					keyID = m.group(1);
				}

				m = uidPattern.matcher(data);
				if(uid == null && m.matches())
				{
					uid = m.group(1);
					email = m.group(2);
				}
			}

			in.close();
			if(p.waitFor() == 0)
				return ret;
			else
				return null;
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public String encrypt(String message, List<Key> receivers, Key signKey)
	{
		try
		{
			int base = 5;
			if(signKey != null)
				base = 10;
			String [] command = new String[receivers.size()*2 + base];
			command[0] = gpg;
			command[1] = "--armor";
			command[2] = "--cipher-algo";
			command[3] = "AES256";
			command[4] = "--encrypt";
			
			if(signKey != null)
			{
				command[5] = "--sign";
				command[6] = "--digest-algo";
				command[7] = "SHA512";
				command[8] = "--default-key";
				command[9] = signKey.keyID;
			}
			for(int k=0; k<receivers.size(); k++)
			{
				command[base+k*2] = "--recipient";
				command[base+1+k*2] = receivers.get(k).keyID;
			}

			Process p = Runtime.getRuntime().exec(command);
			OutputStream out = p.getOutputStream();
			out.write(message.getBytes("UTF-8"));
			out.close();
			
			String result = IOUtils.toString(p.getInputStream(), "UTF-8");
			
			if(p.waitFor() == 0)
				return result;
			else
				return null;
		}
		catch(Exception e)
		{
			return null;
		}
	}
}
