package org.sparcs.gpgchat.gpg;

public class Key {

	public String pub;
	public String uid;
	public String email;
	
	public String toString()
	{
		return pub + "/" + uid + "/" + email;
	}
}
