package org.sparcs.gpgchat.gpg;

import java.io.OutputStream;
import java.util.ArrayList;
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
	
	private Process runCommand(List<String> args)
	{
		try
		{
			ArrayList<String> cmd = new ArrayList<>();
			cmd.add(this.gpg);
			cmd.add("--batch");
			cmd.add("--no-tty");
			cmd.addAll(args);

			String[] array = cmd.toArray(new String[cmd.size()]);
			Process gpg = Runtime.getRuntime().exec(array);
			return gpg;
		}
		catch(Exception e)
		{
			return null;
		}
	}

	public static GPG getInstance(String path)
	{
		try
		{
			if(path == null || path.length() == 0)
				path = "gpg";
			List<String> list = new LinkedList<String>();
			list.add("--version");
			GPG ret = new GPG(path);
			Process gpg = ret.runCommand(list);
			if(gpg.waitFor() == 0)
				return ret;
			else
				return null;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}

	public static void main(String[] args) //TODO just for testing
	{
		GPG gpg = GPG.getInstance(null);
		List<Key> all = gpg.getAllKeys();
		List<Key> rem = new LinkedList<>();
		Key me = null;
		for(Key k : all)
		{
			if(!(k.uid.equals("leeopop") || k.uid.equals("elaborate")))
				rem.add(k);
			if(k.uid.equals("leeopop"))
				me = k;
		}
		for(Key k : rem)
			all.remove(k);
		System.out.println(all);
		
		String temp = "ASDF";
		String message1 = gpg.encrypt(temp, all, me);
		System.out.println(message1);
	}

	public List<Key> getAllKeys()
	{
		try
		{
			Pattern keyIDPattern = Pattern.compile("pub\\s+\\w+/(\\w+)\\s+.*");
			Pattern uidPattern = Pattern.compile("uid\\s+(\\S+[^<>]*\\S+)\\s+<([^<>]*)>.*");


			List<Key> ret = new LinkedList<>();
			List<String> list = new LinkedList<String>();
			list.add("--list-keys");

			Process p = this.runCommand(list);
			
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
			List<String> list = new LinkedList<String>();
			list.add("--list-secret-keys");

			Process p = this.runCommand(list);

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
			List<String> list = new LinkedList<String>();
			list.add("--armor");
			list.add("--cipher-algo");
			list.add("AES256");
			list.add("--encrypt");
			
			if(signKey != null)
			{
				list.add("--sign");
				list.add("--digest-algo");
				list.add("SHA512");
				list.add("--default-key");
				list.add(signKey.keyID);
			}
			for(int k=0; k<receivers.size(); k++)
			{
				list.add("--recipient");
				list.add(receivers.get(k).keyID);
			}

			Process p = this.runCommand(list);
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
