package org.sparcs.gpgchat.gpg;

import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
		System.out.println(gpg.getAllKeys());
	}

	public List<Key> getAllKeys()
	{
		try
		{
			Pattern pubPattern = Pattern.compile("pub\\s+\\w+/(\\w+)\\s+.*");
			Pattern uidPattern = Pattern.compile("uid\\s+(\\S+[^<>]*\\S+)\\s+<([^<>]*)>.*");


			List<Key> ret = new LinkedList<>();
			String [] command = new String[2];
			command[0] = gpg;
			command[1] = "--list-keys";

			Process p = Runtime.getRuntime().exec(command);

			Scanner in = new Scanner(p.getInputStream());
			String pub = null;
			String uid = null;
			String email = null;
			while(in.hasNextLine())
			{
				String data = in.nextLine();
				if(data.length() == 0)
				{
					if (pub == null)
						break;
					Key k = new Key();
					k.pub = pub;
					k.uid = uid;
					k.email = email;
					ret.add(k);
					pub = null;
					uid = null;
					email = null;
				}

				Matcher m = pubPattern.matcher(data);
				if(pub == null && m.matches())
				{
					pub = m.group(1);
				}

				m = uidPattern.matcher(data);
				if(uid == null && m.matches())
				{
					uid = m.group(1);
					email = m.group(2);
				}
			}

			in.close();
			p.waitFor();
			return ret;
		}
		catch(Exception e)
		{
			return null;
		}
	}
}
