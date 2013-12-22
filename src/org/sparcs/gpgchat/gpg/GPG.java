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
			if(k.uid.equals("invalid"))
				me = k;
		}
		for(Key k : rem)
			all.remove(k);
		System.out.println(all);
		
		String temp = "ASDF";
		String message1 = gpg.encrypt(temp, all, me);
		System.out.println(message1);
		
		String[] ret = gpg.decrypt(message1);
		System.out.println(ret[0]);
		System.out.println(ret[1]);
		System.out.println(ret[2]);
		System.out.println(ret[3]);
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
			
			Scanner in = new Scanner(p.getInputStream(), "UTF-8");
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

			Scanner in = new Scanner(p.getInputStream(), "UTF-8");
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
	
	//String[0]: message
	//String[1]: signer's key id
	//String[2]: signer's name
	//String[3]: signer's email
	public String[] decrypt(String message)
	{
		try
		{
			List<String> list = new LinkedList<String>();
			list.add("--decrypt");

			Process p = this.runCommand(list);
			OutputStream out = p.getOutputStream();
			out.write(message.getBytes("UTF-8"));
			out.close();
			
			String result = IOUtils.toString(p.getInputStream(), "UTF-8");
			Scanner in = new Scanner(p.getErrorStream(),"UTF-8");
			
			Pattern keyIDPattern = Pattern.compile("gpg:\\s+Signature\\s+made\\s+.*\\s+ID\\s+(\\w+)");
			Pattern signaturePattern = Pattern.compile("gpg:\\s+(\\w+)\\s+signature\\s+from\\s+\"(\\w+)\\s+<(\\S+)>\"");
			Pattern statusPattern = Pattern.compile("gpg:\\s+(\\w+):\\s+.*");
			int state = 0;
			String key = null;
			String signature = null;
			String name = null;
			String email = null;
			while(in.hasNextLine())
			{
				String line = in.nextLine().trim();
				if(line.length()==0)
					continue;
				switch(state)
				{
				case 0:
				{
					Matcher m = keyIDPattern.matcher(line);
					if(m.matches())
					{
						key = m.group(1);
						state++;
					}
					break;
				}
				case 1:
				{
					Matcher m = signaturePattern.matcher(line);

					
					if(m.matches())
					{
						signature = m.group(1);
						name = m.group(2);
						email = m.group(3);
						if(signature.equals("Good"))
							state++;
						else
							state=-1;
					}
					else
						state=-1;
					break;
				}
				case 2:
				{
					Matcher m = statusPattern.matcher(line);

					
					if(m.matches())
					{
						System.err.println(line);
						state=-1;
					}
					else
						state=3;
					break;
				}
				}
				if(state == -1 || state==3)
					break;
			}
			in.close();
			
			if(p.waitFor() == 0)
			{
				if(state == -1)
					return null;
				else
				{
					String[] ret = new String[4];
					ret[0] = result;
					ret[1] = key;
					ret[2] = name;
					ret[3] = email;
					return ret;
				}
			}
			else
				return null;
		}
		catch(Exception e)
		{
			return null;
		}
	}
}
