package com.tools.encryption;

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;

public class EncryptedMessage{
	public PublicKey sendersPublicKey;		// the sender's public key
	public PublicKey receiversPublicKey; 	// the receiver's public key
	public byte[] encryptedMessage;			// message was encrypted withs secret key
	public byte[] signedMessage; 			// the encrypted message was signed with senders private key
	public byte[] encryptedSecretKey; 		// the secret key is encrypted with receivers public key
	public byte[] encryptedSecretKeyForSender; // the secret key is ecnrypted with the senders public key too, so he can read later
	
	/**
	 * Create an encrypted message
	 * @param sendersPublicKey the senders public key
	 * @param receiversPublicKey The receivers public key
	 * @param message The message to encrypt
	 * @param sendersPrivateKey The private key of the sender
	 * @throws EncryptionException 
	 * @throws IOException
	 */
	public EncryptedMessage(PublicKey sendersPublicKey, PublicKey receiversPublicKey, String message, PrivateKey sendersPrivateKey)
			throws EncryptionException, IOException{
		this.sendersPublicKey = sendersPublicKey;
		this.receiversPublicKey = receiversPublicKey;
		
		// encrypt the message
		SymmetricEncryptor encryptor = new SymmetricEncryptor();
		encryptedMessage = encryptor.encryptStringToByteArray(message);
		
		// the non encyrpted secret key
		byte[] nonEncryptedSecretKey = encryptor.getKeyCode();
		
		// encrypt the secret key with receivers public key
		PublicPrivateEncryptor receiver = new PublicPrivateEncryptor(null, receiversPublicKey);
		this.encryptedSecretKey = receiver.encryptWithPublic(nonEncryptedSecretKey);
		
		// sign the message
		PublicPrivateEncryptor sender = new PublicPrivateEncryptor(sendersPrivateKey, sendersPublicKey);
		this.signedMessage = sender.signData(encryptedMessage);

		// encrypt the secret key with the senders public key
		this.encryptedSecretKeyForSender = sender.encryptWithPublic(nonEncryptedSecretKey);
	}
	
	/**
	 * Generate an encryptedMessage object from previously encoded data.
	 * @param sendersPublicKey
	 * @param receiversPublicKey
	 * @param encryptedMessage message was encrypted withs secret key
	 * @param signedMessage the encrypted message was signed with senders private key
	 * @param encryptedSecretKey the secret key is encrypted with receivers public key
	 * @param encryptedSecretKeyForSender the secret key is encrypted using the senders public key too.. so he can read it later
	 */
	public EncryptedMessage(
			PublicKey sendersPublicKey,
			PublicKey receiversPublicKey,
			byte[] encryptedMessage,
			byte[] signedMessage,
			byte[] encryptedSecretKey,
			byte[] 	encryptedSecretKeyForSender){	
		this.sendersPublicKey = sendersPublicKey;
		this.receiversPublicKey = receiversPublicKey;
		this.encryptedMessage = encryptedMessage;
		this.signedMessage = signedMessage;
		this.encryptedSecretKey = encryptedSecretKey;
		this.encryptedSecretKeyForSender= encryptedSecretKeyForSender; 
	}
	
	/**
	 * Verify the message was sent by who it says it was sent by
	 * @return true if a valid message
	 * @throws EncryptionException
	 */
	public boolean verifyMessage() throws EncryptionException{
		PublicPrivateEncryptor sender = new PublicPrivateEncryptor(null, sendersPublicKey);
		return sender.verifyData(encryptedMessage, signedMessage);
	}
	
	/**
	 * Decrypt the message with the receivers private key
	 * @param receiversPrivateKey
	 * @return the decrypted message
	 * @throws EncryptionException 
	 * @throws IncorrectPasswordException 
	 * @throws IOException 
	 */
	public String decryptMessageForReceiver(PrivateKey receiversPrivateKey)
			throws EncryptionException, IOException, IncorrectPasswordException{
		
		// first figure out the symmetric key
		PublicPrivateEncryptor receiver = new PublicPrivateEncryptor(receiversPrivateKey, null);
		byte[] secretKey = receiver.decryptWithPrivate(encryptedSecretKey);
		
		// now use symmetric key to decrypte
		SymmetricEncryptor symmetric = new SymmetricEncryptor(secretKey);
		return symmetric.decryptByteArrayToString(encryptedMessage);
	}
	
	/**
	 * Decrypt the message with the senders private key
	 * @param sendersPrivateKey
	 * @return the decrypted message
	 * @throws EncryptionException 
	 * @throws IncorrectPasswordException 
	 * @throws IOException 
	 */
	public String decryptMessageForSender(PrivateKey sendersPrivateKey)
			throws EncryptionException, IOException, IncorrectPasswordException{
		
		// first figure out the symmetric key
		PublicPrivateEncryptor receiver = new PublicPrivateEncryptor(sendersPrivateKey, null);
		byte[] secretKey = receiver.decryptWithPrivate(encryptedSecretKeyForSender);
		
		// now use symmetric key to decrypte
		SymmetricEncryptor symmetric = new SymmetricEncryptor(secretKey);
		return symmetric.decryptByteArrayToString(encryptedMessage);
	}
	
	/**
	 * Decrypt the message with either the senders or receivers private key. Will figure out which to use
	 * @param keyPair A pair of public and private keys
	 * @return The decrypted message
	 * @throws EncryptionException
	 * @throws IOException
	 * @throws IncorrectPasswordException
	 */
	public String decryptMessage(PublicPrivateEncryptor keyPair)
			throws EncryptionException, IOException, IncorrectPasswordException{
		// first determine if sender or receiver
		if(sendersPublicKey.equals(keyPair.getPublic()))
			return decryptMessageForSender(keyPair.getPrivate());
		else if (receiversPublicKey.equals(keyPair.getPublic()))
			return decryptMessageForReceiver(keyPair.getPrivate());
		else
			throw new EncryptionException("No Matching Private Key");
	}
}