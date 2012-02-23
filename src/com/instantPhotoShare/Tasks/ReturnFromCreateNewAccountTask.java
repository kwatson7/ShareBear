package com.instantPhotoShare.Tasks;

public class ReturnFromCreateNewAccountTask {

	// static constants
	public static  String SUCCESS = CreateNewAccountTask.SUCCESS;
	
	/** The status of the return from server */
	private String status;
	/** If there was an error, the reason for the error, "" otherwise*/
	protected String reason = "";
	/** If there was a return, the status code, -1 otherwise */
	private int code = -1;
	/** The userId returned from server */
	protected long userId;
	/** the sercetCode returned from server */
	protected String secretCode;
	
	public ReturnFromCreateNewAccountTask
	(String status,
			String reason,
			int code){
		
		this.status = status;
		this.reason = reason;
		this.code = code;

	}
	
	public ReturnFromCreateNewAccountTask
	(String status,
			String reason){
		
		this.status = status;
		this.reason = reason;
	}
	
	public ReturnFromCreateNewAccountTask(
			String status,
			int code){
		this.status = status;
		this.code = code;
		this.reason = "StatusCode = " + code;
	}
	
	public ReturnFromCreateNewAccountTask(
			String status,
			String reason,
			int code,
			long userId,
			String secretCode){
		this.status = status;
		this.code = code;
		this.userId = userId;
		this.secretCode = secretCode;
		this.reason = reason;
	}
			
	/**
	 * If the return from the server is "success"
	 * @return
	 */
	public boolean isSuccess(){
		if (status.equals(SUCCESS))
			return true;
		else
			return false;
	}
}
