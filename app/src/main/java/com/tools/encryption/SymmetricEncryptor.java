package com.tools.encryption;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import android.util.Base64;

public class SymmetricEncryptor {

	// public constants
	/**
	 * Message in IOException if encrypted file was not deleted
	 */
	public static final String ENCRYPTED_FILE_NOT_DELETED = "Encrypted file not deleted";
	
	// private constants
	private static final String SALT = "8hw7410kn!71/*(1)";
	private static final String ALGORITHM_NAME_HASH = "SHA-256";
	private static final int PASSWORD_TO_RAW_ITERATIONS = 1000;
	private static final String ALGORITHM_NAME_ENCRYPT = "AES";
	private static final int RANDOM_PASSWORD_LENGTH = 20;
	private static final String CHARSET_NAME = "UTF-8";
	private static final Charset CHARSET = Charset.forName(CHARSET_NAME);

	// private variables
	private CipherStore cipherStore;

	// contructors
	/**
	 * Generate an encryptor with a random key
	 * @throws EncryptionException 
	 */
	public SymmetricEncryptor() throws EncryptionException {
		// generate a random password
		String password = com.tools.Tools.randomString(RANDOM_PASSWORD_LENGTH);
		byte[] keyCode = getPasswordBytes(password);
		cipherStore = new CipherStore(keyCode, ALGORITHM_NAME_ENCRYPT);
	}

	/**
	 * Generate an encryptor with password that seeds the key
	 * @param password
	 * @throws EncryptionException 
	 */
	public SymmetricEncryptor(String password)
			throws EncryptionException{
		byte[] keyCode = getPasswordBytes(password);
		cipherStore = new CipherStore(keyCode, ALGORITHM_NAME_ENCRYPT);
	}

	/**
	 * Generate an encryptor with the given key
	 * @param key
	 */
	public SymmetricEncryptor(byte[] key) {
		cipherStore = new CipherStore(key, ALGORITHM_NAME_ENCRYPT);
	}
	
	/**
	 * Generate an encryptor with the given key
	 * @param key
	 * @throws EncryptionException 
	 */
	public SymmetricEncryptor(char[] keyChar) throws EncryptionException {
		this(new String(keyChar));
		//byte[] key = (new String(keyChar)).getBytes(CHARSET);
		//cipherStore = new CipherStore(key, ALGORITHM_NAME_ENCRYPT);
	}
	
//	public static byte[] convertToValidKey(char[] keyChar){
//		
//	}

	// public methods
	public byte[] getKeyCode(){
		return cipherStore.getKeyCode();
	}
	
	/**
	 * Encrypt a file
	 * @param fileNameInput The original file
	 * @param fileNameOutput The output file
	 * @param isKeepOldFile boolean to keep the old file (true) or delete the old un-encrypted file(false)
	 * @throws IOException
	 * @throws ClearTextFileNotDeletedException
	 * @throws EncryptionException 
	 */
	public void encryptFile(String fileNameInput, String fileNameOutput, boolean isKeepOldFile)
			throws IOException, ClearTextFileNotDeletedException, EncryptionException{

		// Here you read the cleartext.
		FileInputStream fis = new FileInputStream(fileNameInput);

		// This stream write the encrypted text. This stream will be wrapped by another stream.
		FileOutputStream fos = new FileOutputStream(fileNameOutput);

		// Wrap the output stream and write bytes
		CipherOutputStream cos = new CipherOutputStream(fos, cipherStore.getEncryptCipher());

		// first write hashed password
		cos.write(cipherStore.getKeyCode());

		// write rest of file
		int b;
		byte[] d = new byte[8];
		while((b = fis.read(d)) != -1) {
			cos.write(d, 0, b);
		}

		// Flush and close streams.
		cos.flush();
		cos.close();
		fis.close();

		// delete the old file if desired
		if (!isKeepOldFile){
			File file = new File(fileNameInput);
			boolean success = file.delete();
			if (!success)
				throw new ClearTextFileNotDeletedException();
		}
	}
	
	/**
	 * Decrypt a file and write to new file
	 * @param fileNameInput The original encrypted file
	 * @param fileNameOutput The new decrypted file
	 * @param isKeepOldFile boolean to keep old encrypted file (true) or delete (false)
	 * @throws IOException
	 * @throws IncorrectPasswordException 
	 * @throws EncryptionException 
	 */
	public void decryptFile(String fileNameInput, String fileNameOutput, boolean isKeepOldFile)
			throws IOException, IncorrectPasswordException, EncryptionException {
		
		// open original file
	    FileInputStream fis = new FileInputStream(fileNameInput);

	    // open output file
	    FileOutputStream fos = new FileOutputStream(fileNameOutput);		

	    // create the cipher input stream
	    CipherInputStream cis = new CipherInputStream(fis, cipherStore.getDecryptCipher());
	    
	    // first read password hash and check that it matches
	    byte[] passwordCheck = new byte[cipherStore.getKeyCode().length];
	    int check = cis.read(passwordCheck);
	    if (check == -1){
	    	cis.close();
	    	fos.close();
	    	throw new IncorrectPasswordException();
	    }
	    if (!Arrays.equals(cipherStore.getKeyCode(), passwordCheck)){
	    	fos.close();
	    	cis.close();
	    	throw new IncorrectPasswordException();
	    }
	    
	    // read the data and write to output file
	    int b;
	    byte[] d = new byte[8];
	    while((b = cis.read(d)) != -1) {
	        fos.write(d, 0, b);
	    }
	    fos.flush();
	    fos.close();
	    cis.close();
	    
	    // delete the old file if desired
	    if (!isKeepOldFile){
	    	File file = new File(fileNameInput);
	    	boolean success = file.delete();
	    	if (!success)
	    		throw new IOException(ENCRYPTED_FILE_NOT_DELETED);
	    }
	}
	
	/**
	 * Encrypt a file
	 * @param byteArray The original data to encrypt to file
	 * @param fileNameOutput The output file
	 * @throws IOException
	 * @throws EncryptionException 
	 */
	public void encryptByteArrayToFile(byte[] byteArray, String fileNameOutput)
			throws IOException, EncryptionException{

		// Here you read the cleartext.
		ByteArrayInputStream fis = new ByteArrayInputStream(byteArray);

		// This stream write the encrypted text. This stream will be wrapped by another stream.
		FileOutputStream fos = new FileOutputStream(fileNameOutput);

		// Wrap the output stream and write bytes
		CipherOutputStream cos = new CipherOutputStream(fos, cipherStore.getEncryptCipher());

		// first write hashed password
		cos.write(cipherStore.getKeyCode());

		// write rest of file
		int b;
		byte[] d = new byte[8];
		while((b = fis.read(d)) != -1) {
			cos.write(d, 0, b);
		}

		// Flush and close streams.
		cos.flush();
		cos.close();
		fis.close();
	}
	
	/**
	 * Decrypt a file and write to byte array
	 * @param fileNameInput The original encrypted file
	 * @param isKeepOldFile boolean to keep old encrypted file (true) or delete (false)
	 * @throws IOException
	 * @throws IncorrectPasswordException 
	 * @throws EncryptionException 
	 */
	public byte[] decryptFileToByteArray(String fileNameInput, boolean isKeepOldFile)
			throws IOException, IncorrectPasswordException, EncryptionException {
		
		// open original file
	    FileInputStream fis = new FileInputStream(fileNameInput);

	    // open output stream
	    ByteArrayOutputStream output = new ByteArrayOutputStream();	

	    // create the cipher input stream
	    CipherInputStream cis = new CipherInputStream(fis, cipherStore.getDecryptCipher());

	    // first read password hash and check that it matches
	    byte[] passwordCheck = new byte[cipherStore.getKeyCode().length];
	    int check = cis.read(passwordCheck);
	    if (check == -1){
	    	cis.close();
	    	throw new IncorrectPasswordException();
	    }
	    if (!Arrays.equals(cipherStore.getKeyCode(), passwordCheck)){
	    	cis.close();
	    	throw new IncorrectPasswordException();
	    }
	    
	    // read the data and write to output file
	    int b;
	    byte[] d = new byte[8];
	    while((b = cis.read(d)) != -1) {
	    	output.write(d, 0, b);
	    }
	    cis.close();
	    
	    // delete the old file if desired
	    if (!isKeepOldFile){
	    	File file = new File(fileNameInput);
	    	boolean success = file.delete();
	    	if (!success)
	    		throw new IOException(ENCRYPTED_FILE_NOT_DELETED);
	    }
	    
	    return output.toByteArray();
	}
	
	/**
	 * Encrypt a byte array to another byte array
	 * @param byteArray The original data to encrypt
	 * @throws IOException 
	 * @throws EncryptionException 
	 */
	public byte[] encryptByteArrayToByteArray(byte[] byteArray)
			throws IOException, EncryptionException{

		// Here you read the cleartext.
		ByteArrayInputStream fis = new ByteArrayInputStream(byteArray);

		// This stream write the encrypted data. This stream will be wrapped by another stream.
		ByteArrayOutputStream output = new ByteArrayOutputStream(byteArray.length);

		// Wrap the output stream and write bytes
		CipherOutputStream cos = new CipherOutputStream(output, cipherStore.getEncryptCipher());

		// first write hashed password
		cos.write(cipherStore.getKeyCode());

		// write rest of byte array
		int b;
		byte[] d = new byte[8];
		while((b = fis.read(d)) != -1) {
			cos.write(d, 0, b);
		}

		// Flush and close streams.
		cos.flush();
		cos.close();
		fis.close();
		
		return output.toByteArray();	
	}
	
	/**
	 * Decrypt a byte array to another byte array
	 * @param byteArray The original data to encrypt
	 * @throws IOException 
	 * @throws EncryptionException 
	 * @throws IncorrectPasswordException 
	 */
	public byte[] decryptByteArrayToByteArray(byte[] byteArrayToDecrypt)
			throws IOException, EncryptionException, IncorrectPasswordException{	    

		// open input byteArray
		ByteArrayInputStream bis = new ByteArrayInputStream(byteArrayToDecrypt);

		// open output stream
		ByteArrayOutputStream output = new ByteArrayOutputStream();	

		// create the cipher input stream
		CipherInputStream cis = new CipherInputStream(bis, cipherStore.getDecryptCipher());

		// first read password hash and check that it matchs
		byte[] passwordCheck = new byte[cipherStore.getKeyCode().length];
		int check = cis.read(passwordCheck);
		if (check == -1){
			cis.close();
			throw new IncorrectPasswordException();
		}
		if (!Arrays.equals(cipherStore.getKeyCode(), passwordCheck)){
			cis.close();
			throw new IncorrectPasswordException();
		}

		// read the data and write to output file
		int b;
		byte[] d = new byte[8];
		while((b = cis.read(d)) != -1) {
			output.write(d, 0, b);
		}
		cis.close();

		return output.toByteArray();
	}
	
	/**
	 * Encrypt a byte array to a base64 string
	 * @param byteArray The original data to encrypt
	 * @throws IOException 
	 * @throws EncryptionException 
	 */
	public String encryptByteArrayToBase64String(byte[] byteArray) throws IOException, EncryptionException{
		byte[] byteArrayOut = encryptByteArrayToByteArray(byteArray);
		return Base64.encodeToString(byteArrayOut, Base64.DEFAULT);		
	}
	
	/**
	 * Decrypt a base64 string to a byte array
	 * @param strintToDecrypt 
	 * @return the clear text byte array
	 * @throws IOException
	 * @throws EncryptionException
	 * @throws IncorrectPasswordException
	 */
	public byte[] decryptBase64StringToByteArray(String strintToDecrypt) throws IOException, EncryptionException, IncorrectPasswordException{
		byte[] byteArray = Base64.decode(strintToDecrypt, Base64.DEFAULT);
		return decryptByteArrayToByteArray(byteArray);
	}
	
	/**
	 * Encrypt a string to a base64 string
	 * @param input The original data to encrypt
	 * @throws IOException 
	 * @throws EncryptionException 
	 */
	public String encryptStringToBase64String(String input) throws IOException, EncryptionException{
		byte[] byteArrayOut = encryptStringToByteArray(input);
		return Base64.encodeToString(byteArrayOut, Base64.DEFAULT);	
	}
	
	/**
	 * Decrypt a base64 string and output a string
	 * @param input the string to decrypt
	 * @return the clear text string
	 * @throws IOException
	 * @throws EncryptionException
	 * @throws IncorrectPasswordException
	 */
	public String decryptBase64StringToString(String input) throws IOException, EncryptionException, IncorrectPasswordException{
		// decode base64 to byte
		byte[] byteArray = Base64.decode(input, Base64.DEFAULT);
		return decryptByteArrayToString(byteArray);
	}

	/**
	 * Write a string to file encrypted
	 * @param stringToEncrypt The string to encrypt
	 * @param fileNameOutput The filename to write to
	 * @throws IOException
	 * @throws EncryptionException 
	 */
	public void encryptStringToFile(String stringToEncrypt, String fileNameOutput)
			throws IOException, EncryptionException{
		
		// This stream write the encrypted text. This stream will be wrapped by another stream.
		FileOutputStream fos = new FileOutputStream(fileNameOutput);
		
		// Wrap the output stream and write bytes
		CipherOutputStream cos = new CipherOutputStream(fos, cipherStore.getEncryptCipher());

		// first write hashed password
		cos.write(cipherStore.getKeyCode());
		
		// write string
		cos.write(stringToEncrypt.getBytes(CHARSET));

		// Flush and close streams.
		cos.flush();
		cos.close();
	}
	
	/**
	 * Read contents of encrypted file into a string
	 * @param fileNameInput
	 * @return The decrypted contents of file as a string
	 * @throws IOException
	 * @throws IncorrectPasswordException 
	 * @throws EncryptionException 
	 */
	public String readEncryptedFile(String fileNameInput)
			throws IOException, IncorrectPasswordException, EncryptionException {
		
		// open original file
	    FileInputStream fis = new FileInputStream(fileNameInput);

	    // open output stream
	    StringBuffer fileContent = new StringBuffer("");
	    
	    // create the cipher input stream
	    CipherInputStream cis = new CipherInputStream(fis, cipherStore.getDecryptCipher());
	    
	    // first read password hash and check that it matchs
	    byte[] passwordCheck = new byte[cipherStore.getKeyCode().length];
	    int check = cis.read(passwordCheck);
	    if (check == -1){
	    	cis.close();
	    	throw new IncorrectPasswordException();
	    }
	    if (!Arrays.equals(cipherStore.getKeyCode(), passwordCheck)){
	    	cis.close();
	    	throw new IncorrectPasswordException();
	    }
	    
	    // read data
	    byte[] buffer = new byte[1024];
	    int b;
	    while ((b = cis.read(buffer)) != -1) {
	        fileContent.append(new String(buffer, 0, b, CHARSET));
	    }
	    cis.close();
	    
	    return fileContent.toString();
	}
	
	/**
	 * Encrypt a string to a byte array
	 * @param stringToEncrypt The string to encrypt
	 * @throws IOException
	 * @throws EncryptionException 
	 */
	public byte[] encryptStringToByteArray(String stringToEncrypt)
			throws IOException, EncryptionException{
		
		// open output stream
		ByteArrayOutputStream output = new ByteArrayOutputStream(stringToEncrypt.length()*2);
			
		// Wrap the output stream and write bytes
		CipherOutputStream cos = new CipherOutputStream(output, cipherStore.getEncryptCipher());
		
		// first write hashed password
		cos.write(cipherStore.getKeyCode());
		
		// write string
		cos.write(stringToEncrypt.getBytes(CHARSET));

		// Flush and close streams.
		cos.flush();
		cos.close();
		
		return output.toByteArray();
	}
	
	/**
	 * Decrypt a byte array to a string
	 * @param stringToDecrypt The string to decrypt
	 * @throws IOException
	 * @throws EncryptionException 
	 * @throws IncorrectPasswordException 
	 */
	public String decryptByteArrayToString(byte[] stringToDecrypt)
			throws IOException, EncryptionException, IncorrectPasswordException{

		// open original file
		ByteArrayInputStream bis = new ByteArrayInputStream(stringToDecrypt);

	    // open output stream
	    StringBuffer fileContent = new StringBuffer("");
	    
	    // create the cipher input stream
	    CipherInputStream cis = new CipherInputStream(bis, cipherStore.getDecryptCipher());
	    
	    // first read password hash and check that it matchs
	    byte[] passwordCheck = new byte[cipherStore.getKeyCode().length];
	    int check = cis.read(passwordCheck);
	    if (check == -1){
	    	cis.close();
	    	throw new IncorrectPasswordException();
	    }
	    if (!Arrays.equals(cipherStore.getKeyCode(), passwordCheck)){
	    	cis.close();
	    	throw new IncorrectPasswordException();
	    }
	    
	    // read data
	    byte[] buffer = new byte[256];
	    int b;
	    while ((b = cis.read(buffer)) != -1) {
	        fileContent.append(new String(buffer, 0, b, CHARSET));
	    }
	    cis.close();
	    
	    return fileContent.toString();
	}

	/**
	 * Generate the raw bytes using encryption algorithm from a password with the default salt added and a bit of lag to stop hackers
	 * @param password The password to get bytes from
	 * @return The bytes
	 * @throws EncryptionException 
	 */
	private static byte[] getPasswordBytes(String password) throws EncryptionException{

		// create password with salt
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance(ALGORITHM_NAME_HASH);
		} catch (NoSuchAlgorithmException e) {
			throw new EncryptionException(e);
		}
		digest.update(password.getBytes(CHARSET));
		digest.update(SALT.getBytes(CHARSET));

		// convert to bytes
		byte[] raw = digest.digest();

		// add a bit of lag
		for (int i = 0; i < PASSWORD_TO_RAW_ITERATIONS; i++) {
			digest.reset();
			digest.update(raw);
			raw = digest.digest();
		}

		return raw;
	}
}
