package org.sparcs.gpgchat.gpg;

public class Key {

	public String keyID;
	public String uid;
	public String email;
	
	public String toString()
	{
		return keyID + "/" + uid + "/" + email;
	}
}
