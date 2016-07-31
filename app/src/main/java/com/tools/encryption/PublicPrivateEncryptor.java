package com.tools.encryption;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import test.android.util.Base64;

public class PublicPrivateEncryptor {
	
	// private constants
	private static final String ALGORITHM_FOR_SIGNING = "SHA512withRSA";
	private static final String ALGORITHM_KEY_FACTORY = "RSA";
	private static final String ALGORITHM_RSA_ENCRYPTION = "RSA/ECB/PKCS1Padding";
	private static final int KEY_LENGTH = 2048;
	private static final String PRIVATE_SIGNIFIER = "private:";
	private static final String PUBLIC_SIGNIFIER = "public:";
	private static final String CHARSET_NAME = "UTF-8";
	private static final Charset CHARSET = Charset.forName(CHARSET_NAME);

	// private variables
	PrivateKey privateKey;
	PublicKey publicKey;
	CipherStore cipherStorePublic;
	CipherStore cipherStorePrivate;
	
	/**
	 * Create an Encryption object with a public and private key
	 * @param privateKey
	 * @param publicKey
	 */
	public PublicPrivateEncryptor(PrivateKey privateKey, PublicKey publicKey){
		this.privateKey = privateKey;
		this.publicKey = publicKey;
		cipherStorePublic = new CipherStore(publicKey, ALGORITHM_RSA_ENCRYPTION);
		cipherStorePrivate = new CipherStore(privateKey, ALGORITHM_RSA_ENCRYPTION);
	}
	
	/**
	 * Create an encryption object from base64 encoded keys. They keys can be null, but then will get null pointer exceptions if
	 * methods are later called that would require said data
	 * @param privateKeyBase64 private key encoded as base64
	 * @param publicKeyBase64 public key encoded as base64
	 * @throws EncryptionException
	 */
	public PublicPrivateEncryptor(String privateKeyBase64, String publicKeyBase64) throws EncryptionException{
		// create the key factory
		KeyFactory factory;
		try {
			factory = KeyFactory.getInstance(ALGORITHM_KEY_FACTORY);
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e);
		}
		
		// decode private key as base64
		if (privateKeyBase64 != null){
			EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.decode(privateKeyBase64, Base64.DEFAULT));
			try {
				privateKey = factory.generatePrivate(privateKeySpec);
			} catch (InvalidKeySpecException e) {
				throw new EncryptionException(e);
			}
		}
		
		// decode public key as base64
		if (publicKeyBase64 != null){
			EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.decode(publicKeyBase64, Base64.DEFAULT));
			try {
				publicKey = factory.generatePublic(publicKeySpec);
			} catch (InvalidKeySpecException e) {
				throw new EncryptionException(e);
			}
		}
	}
	
	/**
	 * Append a new NOnce to the the original byte array
	 * @param original The original byte array
	 * @param nBytesNOnce the number of bytes to append to the original byte array
	 * @return The new byte array
	 */
	public static byte[] appendNOnceToByteArray(byte[] original, int nBytesNOnce){
		// append nBytesNOnce to bytes
		SecureRandom random = new SecureRandom();
		byte[] nOnce = new byte[nBytesNOnce];
		random.nextBytes(nOnce);
		byte[] both = new byte[original.length + nOnce.length];
		System.arraycopy(original, 0, both, 0, original.length);
		System.arraycopy(nOnce, 0, both, original.length, nOnce.length);
		return both;
	}
	
	/**
	 * Append a new NOnce to the the original string
	 * @param original The original string
	 * @param nBytesNOnce the number of bytes to append to the original byte array
	 * @return The new byte array
	 */
	public static byte[] appendNOnceToString(String originalString, int nBytesNOnce){
		// append nBytesNOnce to bytes
		SecureRandom random = new SecureRandom();
		byte[] nOnce = new byte[nBytesNOnce];
		random.nextBytes(nOnce);
		byte[] original = originalString.getBytes(CHARSET);
		byte[] both = new byte[original.length + nOnce.length];
		System.arraycopy(original, 0, both, 0, original.length);
		System.arraycopy(nOnce, 0, both, original.length, nOnce.length);
		return both;
	}
	
	/**
	 * Return the private key
	 * @return
	 * @throws EncryptionException if no private key available
	 */
	public PrivateKey getPrivate() throws EncryptionException{
		if (privateKey == null)
			throw new EncryptionException("No Private Key");
		else
			return privateKey;
	}
	
	/**
	 * Return teh public key
	 * @return
	 */
	public PublicKey getPublic(){
		return publicKey;
	}
	
	/**
	 * Return the public key as a base64 string
	 * @return
	 */
	public String getPublicAsBase64(){
		return Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT);
	}
	
	/**
	 * Save the keypair to an encrypted file. Creating file and folders if necessary
	 * @param filename the file to save to
	 * @param password The password used to save file
	 * @throws IOException 
	 * @throws EncryptionException 
	 */
	public void saveToFile(String filename, String password)
			throws IOException, EncryptionException{
		// get private and public as strings first
		
		StringBuilder builder = new StringBuilder(KEY_LENGTH*3);
		builder.append(PRIVATE_SIGNIFIER);
		builder.append(Base64.encodeToString(privateKey.getEncoded(), Base64.DEFAULT));
		builder.append(PUBLIC_SIGNIFIER);
		builder.append(Base64.encodeToString(publicKey.getEncoded(), Base64.DEFAULT));
		
		// make sure file exists
		File file = new File(filename);
		if (!file.exists()){
			com.tools.Tools.writeRequiredFolders(filename);
			file.createNewFile();
		}
		
		// write keys to file
		SymmetricEncryptor enc = new SymmetricEncryptor(password);
		enc.encryptStringToFile(builder.toString(), filename);
	}
	
	/**
	 * Generate a signed hash for byte array
	 * @param data
	 * @return the signed hash
	 * @throws EncryptionException
	 */
	public byte[] signData(byte[] data) throws EncryptionException{
		Signature signature;
		try {
			signature = Signature.getInstance(ALGORITHM_FOR_SIGNING);
			signature.initSign(privateKey);
			signature.update(data);
			return signature.sign();
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e);
		} catch (SignatureException e) {
			throw new EncryptionException(e);
		} catch (InvalidKeyException e) {
			throw new EncryptionException(e);
		} 
	}
	
	/**
	 * Generate a signed hash for byte array
	 * @param data
	 * @return the signed hash
	 * @throws EncryptionException
	 */
	public byte[] signData(String data) throws EncryptionException{
		return signData(data.getBytes(CHARSET));
	}
	
	/**
	 * Verify that given byte array matches the signed byte array
	 * @param data the raw data
	 * @param sigBytes The signature of the raw data
	 * @return true if they match, false otherwise
	 * @throws EncryptionException
	 */
	public boolean verifyData(byte[] data, byte[] sigBytes) throws EncryptionException{
		try {
			Signature signature = Signature.getInstance(ALGORITHM_FOR_SIGNING);
			signature.initVerify(publicKey);
			signature.update(data);
			return signature.verify(sigBytes);
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e);
		} catch (SignatureException e) {
			throw new EncryptionException(e);
		} catch (InvalidKeyException e) {
			throw new EncryptionException(e);
		} 
	}
	
	/**
	 * Encrypt a byte array with the public key
	 * @param bytesToEncrypt the byte array to decrypt
	 * @return The resultant byte array
	 * @throws EncryptionException
	 */
	public byte[] encryptWithPublic(byte[] bytesToEncrypt) throws EncryptionException{
		Cipher cipher = cipherStorePublic.getEncryptCipher();
		try {
			return cipher.doFinal(bytesToEncrypt);
		} catch (IllegalBlockSizeException e) {
			throw new EncryptionException(e);
		} catch (BadPaddingException e) {
			throw new EncryptionException(e);
		}
	}
	
	/**
	 * Encrypt a byte array with the public key and add an NOnce
	 * @param bytesToEncrypt the byte array to decrypt
	 * @param nBytesNOnce number of bytes for NOnce to add
	 * @return The resultant byte array
	 * @throws EncryptionException
	 */
	public byte[] encryptWithPublic(byte[] bytesToEncrypt, int nBytesNOnce) throws EncryptionException{
		// append nBytesNOnce to bytes
		byte[] both = appendNOnceToByteArray(bytesToEncrypt, nBytesNOnce);
		
		// now encrypt
		Cipher cipher = cipherStorePublic.getEncryptCipher();
		try {
			return cipher.doFinal(both);
		} catch (IllegalBlockSizeException e) {
			throw new EncryptionException(e);
		} catch (BadPaddingException e) {
			throw new EncryptionException(e);
		}
	}

	/**
	 * Decypt a byte array with the private key
	 * @param bytesToEncrypt the byte array to decrypt
	 * @return The resultant byte array
	 * @throws EncryptionException
	 */
	public byte[] decryptWithPrivate(byte[] bytesToDecrypt) throws EncryptionException{
		Cipher cipher = cipherStorePrivate.getDecryptCipher();
		
		try {
			return cipher.doFinal(bytesToDecrypt);
		} catch (IllegalBlockSizeException e) {
			throw new EncryptionException(e);
		} catch (BadPaddingException e) {
			throw new EncryptionException(e);
		}
	}
	
	/**
	 * Decypt a byte array with the private key
	 * @param bytesToEncrypt the byte array to decrypt
	 * @return The resultant byte array
	 * @throws EncryptionException
	 */
	public byte[] decryptWithPrivate(byte[] bytesToDecrypt, int nBytesNOnce) throws EncryptionException{
		Cipher cipher = cipherStorePrivate.getDecryptCipher();
		
		// decrypt
		byte[] decrypted;
		try {
			decrypted = cipher.doFinal(bytesToDecrypt);
		} catch (IllegalBlockSizeException e) {
			throw new EncryptionException(e);
		} catch (BadPaddingException e) {
			throw new EncryptionException(e);
		}
		
		// strip nOnce off the end
		return Arrays.copyOfRange(decrypted, 0, decrypted.length-nBytesNOnce);
	}
	
	/**
	 * Load an encryption object from file
	 * @param filename the file to save to
	 * @param password The password used to save file
	 * @return a new encryption object loaded from this file
	 * @throws IOException 
	 * @throws IncorrectPasswordException 
	 * @throws EncryptionException 
	 */
	public static PublicPrivateEncryptor loadFromFile(String filename, String password)
			throws IOException, IncorrectPasswordException, EncryptionException{

		// read the encrypted file
		SymmetricEncryptor enc = new SymmetricEncryptor(password);
		String fileContents = enc.readEncryptedFile(filename);

		// create the key factory
		KeyFactory factory;
		try {
			factory = KeyFactory.getInstance(ALGORITHM_KEY_FACTORY);
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e);
		}
		
		// parse out data
		// read private first and verify it is there
		String privateSig = fileContents.substring(0, PRIVATE_SIGNIFIER.length());
		if (!privateSig.equalsIgnoreCase(PRIVATE_SIGNIFIER))
			throw new IOException("Not a valid Encryption file");	
		
		// find where public key is located
		int publicStart = fileContents.indexOf(PUBLIC_SIGNIFIER);
		
		// now read the private key string and convert to a key
		String privateString = fileContents.substring(PRIVATE_SIGNIFIER.length(), publicStart);
		EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.decode(privateString, Base64.DEFAULT));
		PrivateKey privateKey;
		try {
			privateKey = factory.generatePrivate(privateKeySpec);
		} catch (InvalidKeySpecException e) {
			throw new EncryptionException(e);
		}

		// make sure we have a public key signifier now
		String publicSig = fileContents.substring(publicStart, publicStart+PUBLIC_SIGNIFIER.length());
		if (!publicSig.equalsIgnoreCase(PUBLIC_SIGNIFIER))
			throw new IOException("Not a valid Encryption file");
		
		// now read the public key string and convert to a key
		String publicString = fileContents.substring(publicStart+PUBLIC_SIGNIFIER.length());
		EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.decode(publicString, Base64.DEFAULT));
		PublicKey publicKey;
		try {
			publicKey = factory.generatePublic(publicKeySpec);
		} catch (InvalidKeySpecException e) {
			throw new EncryptionException(e);
		}
		
		return new PublicPrivateEncryptor(privateKey, publicKey);
	}
	
	/**
	 * Create a new keypair and seed Encryption with it
	 * @return a new encryption object
	 * @throws EncryptionException 
	 */
	public static PublicPrivateEncryptor createFromNewKeyPair() throws EncryptionException{
		KeyPair kp = generateKeyPair();
		return new PublicPrivateEncryptor(kp.getPrivate(), kp.getPublic());
	}

	/**
	 * Convert a base64 encoded private key to a normal private key
	 * @param privateKeyBase64
	 * @return
	 * @throws EncryptionException
	 */
	private static PrivateKey base64ToPrivateKey(String privateKeyBase64) throws EncryptionException{
		PrivateKey privateKey;
		
		// create the key factory
		KeyFactory factory;
		try {
			factory = KeyFactory.getInstance(ALGORITHM_KEY_FACTORY);
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e);
		}

		// convert private key from base64 back to a real key
		EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.decode(privateKeyBase64, Base64.DEFAULT));
		try {
			privateKey = factory.generatePrivate(privateKeySpec);
		} catch (InvalidKeySpecException e) {
			throw new EncryptionException(e);
		}

		return privateKey;
	}
	
	/**
	 * Convert a base64 encoded public key to a normal public key
	 * @param publicKeyBase64
	 * @return
	 * @throws EncryptionException
	 */
	private static PublicKey base64ToPublicKey(String publicKeyBase64) throws EncryptionException{
		
		// create the key factory
		KeyFactory factory;
		try {
			factory = KeyFactory.getInstance(ALGORITHM_KEY_FACTORY);
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e);
		}

		EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.decode(publicKeyBase64, Base64.DEFAULT));
		PublicKey publicKey;
		try {
			publicKey = factory.generatePublic(publicKeySpec);
		} catch (InvalidKeySpecException e) {
			throw new EncryptionException(e);
		}

		return publicKey;
	}
	
	/**
	 * Generates a keypair
	 * @return the keypair
	 * @throws EncryptionException 
	 */
	private static final KeyPair generateKeyPair() throws EncryptionException{
		KeyPairGenerator kpg;
		try {
			kpg = KeyPairGenerator.getInstance(ALGORITHM_KEY_FACTORY);
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e);
		}
		kpg.initialize(KEY_LENGTH);
		KeyPair kp = kpg.genKeyPair();
		return kp;
	}
}
