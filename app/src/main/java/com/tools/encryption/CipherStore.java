package com.tools.encryption;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class CipherStore {

	// private variables
	private Cipher encryptCipher;
	private Cipher decryptCipher;
	private Key sks;
	private byte[] keyCode;
	private String algorithmName;

	/**
	 * Store an encryption cipher and decryption cipher. Will only be created when first needed.
	 * Every time the cipher is requested it will be reset with the given keyCode.
	 * @param keyCode The keyCode used to create the cipher
	 * @param algorithmName The name of the algorithm to use
	 */
	public CipherStore(byte[] keyCode, String algorithmName){
		this.keyCode = keyCode;
		this.algorithmName = algorithmName;
	}
	
	public CipherStore(Key key, String algorithmName){
		sks = key;
		this.algorithmName = algorithmName;
	}
	
	/**
	 * Return the keyCode when requested
	 * @return
	 */
	public byte[] getKeyCode(){
		return keyCode;
	}
	
	/**
	 * Grab the encryption cipher. If it is null, will generate a new one with fresh encryption initialization
	 * @return
	 * @throws EncryptionException
	 */
	public Cipher getEncryptCipher() throws EncryptionException{
		// if null, then creat a new one
		if (encryptCipher == null)
			initializeEncryptCipher();

		// initialize it to be fresh
		Key sks = getSecretKeySpec();
		try {
			encryptCipher.init(Cipher.ENCRYPT_MODE, sks);
		} catch (InvalidKeyException e) {
			throw new EncryptionException(e);
		}

		// return it
		return encryptCipher;	
	}

	/**
	 * Grab the decryption cipher. If it is null, will generate a new one with fresh encryption initialization
	 * @return
	 * @throws EncryptionException
	 */
	public Cipher getDecryptCipher() throws EncryptionException{
		// if null, then creat a new one
		if (decryptCipher == null)
			initializeDecryptCipher();

		// initialize it to be fresh
		Key sks = getSecretKeySpec();
		try {
			decryptCipher.init(Cipher.DECRYPT_MODE, sks);
		} catch (InvalidKeyException e) {
			throw new EncryptionException(e);
		}

		// return it
		return decryptCipher;
	}

	/**
	 * Return the secretKeySpec. If it is null, will create one
	 * @return
	 */
	private Key getSecretKeySpec(){
		if (sks == null)
			sks = new SecretKeySpec(keyCode, algorithmName);
		return sks;

	}

	/**
	 * Initialize the encryption cipher
	 * @throws EncryptionException
	 */
	private void initializeEncryptCipher() throws EncryptionException{
		try {
			encryptCipher = Cipher.getInstance(algorithmName);
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e);
		} catch (NoSuchPaddingException e) {
			throw new EncryptionException(e);
		}
	}

	/**
	 * Initialize the decryption cipher
	 * @throws EncryptionException
	 */
	private void initializeDecryptCipher() throws EncryptionException{
		try {
			decryptCipher = Cipher.getInstance(algorithmName);
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e);
		} catch (NoSuchPaddingException e) {
			throw new EncryptionException(e);
		}
	}
}
