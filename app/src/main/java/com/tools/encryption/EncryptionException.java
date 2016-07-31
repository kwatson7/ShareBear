package com.tools.encryption;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Wrapper around a bunch of different encryption exceptions.
 * @author Kyle
 *
 */
public class EncryptionException
extends Exception{

	private static final long serialVersionUID = 8329010111608820388L;

	public EncryptionException(String text){
		super(text);
	}
	
	public EncryptionException(NoSuchAlgorithmException e){
		super("NoSuchAlgorithmException"+e.getMessage());
	}

	public EncryptionException (NoSuchPaddingException e){
		super("NoSuchPaddingException" + e.getMessage());
	}
	
	public EncryptionException (InvalidKeyException e){
		super("InvalidKeyException" + e.getMessage());
	}
	
	public EncryptionException (InvalidKeySpecException e){
		super("InvalidKeySpecException" + e.getMessage());
	}
	
	public EncryptionException (IllegalBlockSizeException e){
		super("IllegalBlockSizeException" + e.getMessage());
	}
	
	public EncryptionException (BadPaddingException e){
		super("BadPaddingException" + e.getMessage());
	}
	
	public EncryptionException (SignatureException e){
		super("SignatureException" + e.getMessage());
	}
	
}