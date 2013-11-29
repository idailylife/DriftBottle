package serverconn;

import android.util.Log;

/**
 * 服务器产生的错误
 * @author BorisHe
 *
 */
public class ServerException extends Exception {

	private static final long serialVersionUID = 1L;

	public static final int TYPE_GENERAL = 0;
	public static final int TYPE_BOTTLE_ERROR = 1;
	public static final int TYPE_CONN_ERROR = 2;
	public static final int TYPE_AUTH_ERROR = 3;
	public static final int TYPE_INTERNAL_ERR = 4;
	public static final int TYPE_DATA_EXHAUSTED = 5;
	
	private String mExceptionName;
	private String mExceptionDetail;
	private int mExceptionCode;
	
	public String getmExceptionName() {
		return mExceptionName;
	}

	public void setmExceptionName(String mExceptionName) {
		this.mExceptionName = mExceptionName;
	}

	public String getmExceptionDetail() {
		return mExceptionDetail;
	}

	public void setmExceptionDetail(String mExceptionDetail) {
		this.mExceptionDetail = mExceptionDetail;
	}

	public int getmExceptionCode() {
		return mExceptionCode;
	}

	public void setmExceptionCode(int mExceptionCode) {
		this.mExceptionCode = mExceptionCode;
	}

	private ServerException(){};
	
	public static ServerException makeGeneralException(String name, String detail){
		ServerException e = new ServerException();
		e.mExceptionCode = TYPE_GENERAL ;
		e.mExceptionName = name;
		e.mExceptionDetail = detail;
		return e;
	}
	
	/**
	 * 瓶子尚未定义
	 * @return
	 */
	public static ServerException makeBottleNotSetException(){
		ServerException e = 
				makeGeneralException("BottleNotSetException"
						, "Call setmBottle method before login!");
		e.mExceptionCode = TYPE_BOTTLE_ERROR;
		return e;
	}
	
	public static ServerException makeServerConnException(){
		ServerException e = 
				makeGeneralException("ServerConnException"
						, "Server internal error!");
		e.mExceptionCode = TYPE_CONN_ERROR ;
		return e;
	}
	
	/**
	 * 鉴权失败
	 * @return
	 */
	public static ServerException makeAuthFailureException(){
		ServerException e = 
				makeGeneralException("AuthenticationError"
						, "Invalid username or password");
		e.mExceptionCode = TYPE_AUTH_ERROR  ;
		return e;
	}
	
	public static ServerException makeServaeInternalException(String reason){
		ServerException e = 
				makeGeneralException("ServerInternalException"
						, reason);
		e.mExceptionCode = TYPE_INTERNAL_ERR   ;
		return e;
	}
	
	/**
	 * 再也找不到数据了
	 * @return
	 */
	public static ServerException makeDataExhaustedException(){
		ServerException e = 
				makeGeneralException("ServerInternalException"
						, "Data record exhausted!");
		e.mExceptionCode = TYPE_DATA_EXHAUSTED   ;
		return e;
	}

	@Override
	public void printStackTrace() {
		Log.e("ServerException", mExceptionName + " : " + mExceptionDetail);
		super.printStackTrace();
	}
	
}
