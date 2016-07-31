package com.tools.encryption;

public class IncorrectPasswordException
extends Exception{

	// public constants
	/**
	 * Exception message when incorrect password is generated
	 */
	public static final String INCORRECT_PASSWORD = "Incorrect password";

	// private constants
	private static final long serialVersionUID = 8329010111608820388L;

	public IncorrectPasswordException(String text){
		super(text);
	}

	public IncorrectPasswordException(){
		super(INCORRECT_PASSWORD);
	}
}