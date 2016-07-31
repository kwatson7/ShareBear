package com.tools;

/**
 * simple class to store success boolean and reason if success==false
 * */
public class SuccessReason {

	private Boolean mSuccess;
	private String mReason;
	
	public SuccessReason(Boolean SUCCESS, String REASON){
		mSuccess = SUCCESS;
		mReason = REASON; 
		
		if (mReason==null)
			mReason = "";
	}
	
	public SuccessReason(Boolean SUCCESS){
		mSuccess = SUCCESS;
		mReason = ""; 
	}
	
	@Override
	public String toString(){
		return mReason;
	}
	
	public Boolean getSuccess(){
		return mSuccess;
	}
	
	public String getReason(){
		return mReason;
	}
}
