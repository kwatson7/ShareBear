package com.tools.encryption;

public class ClearTextFileNotDeletedException
extends Exception{
	
	private static final long serialVersionUID = -3874189374524084293L;
	private static final String CLEAR_FILE_NOT_DELETED = "Clear text file not deleted";

	public ClearTextFileNotDeletedException(String text){
		super(text);
	}
	
	public ClearTextFileNotDeletedException(){
		super(CLEAR_FILE_NOT_DELETED);
	}
}