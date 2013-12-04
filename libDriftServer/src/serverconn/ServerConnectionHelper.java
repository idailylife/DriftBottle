package serverconn;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import serverconn.models.Bottle;
import serverconn.models.Data;

/**
 * 与服务器连接, 使用单例模式
 * 
 * @author BorisHe
 * 
 */
public class ServerConnectionHelper {
	private static ServerConnectionHelper INSTANCE = new ServerConnectionHelper();
	private static final String BASE_URI = "http://driftbottlezju.duapp.com/DriftBottle/";
	private String FILE_URI = null; // 文件读写路径(不含文件名)

	private int MAX_THROW_COUNT = 5; //最大丢弃次数

	private String SESSION_ID = null;

	private Bottle mBottle = null;
	//private Data mData = null;
	private Data mDataReceived = null; //最后一条收到的信息
	private Data mDataSent = null; //最后一条发送的信息
	
	private boolean isLogIn = false;
	private Object KEY_PHPSESSID = "PHPSESSID";
	private List<String> mLatestDataId = null;
	private String mLatestSender = null; //存储最新一条信息的发件人记录
	
	public final String DATA_ID_END_CONVERSATION = "1";  //终止会话用的特殊ID号
	
	public String getmLatestSender() {
		return mLatestSender;
	}

	public List<String> getLatestDataId() {
		return mLatestDataId;
	}
	
	/**
	 * 获得最近存储的文件的绝对路径
	 * @return 如果没有则返回null
	 */
	public String getLatestFileNameAndPath(){
		if(mDataReceived == null)
			return null;
		
		return mDataReceived.getFile().getAbsolutePath();
	}

	private ServerConnectionHelper() {
		FILE_URI = Environment.getExternalStorageDirectory().getPath() + "/soundrecord";
		File f = new File(FILE_URI);
		if (!f.exists()) {
			f.mkdir();
		}
		//FILE_URI += "/file.raw"; //文件名称存于DEFAULT_FILE_NAME
		Log.d("fileDir", FILE_URI);
	}

	/**
	 * 强制更改默认存储文件的<b>路径</b><s><i>及文件名</i></s> 注意安全...
	 * 
	 * @param filepath
	 *            完整文件路径(目录地址)
	 */
	public void setFileUri(String filepath) {
		FILE_URI = filepath;
	}

	public static ServerConnectionHelper getInstance() {
		if (INSTANCE == null) {
			synchronized (ServerConnectionHelper.class) {
				if (INSTANCE == null) {
					INSTANCE = new ServerConnectionHelper();
				}
			}
		}
		return INSTANCE;
	}

	public Bottle getBottle() {
		return mBottle;
	}

	public void setBottle(Bottle mBottle) {
		this.mBottle = mBottle;
	}

	/**
	 * 获取最后一次收到的Data
	 * @return
	 */
	public Data getDataReceived() {
		return mDataReceived;
	}

	/**
	 * 设置要发送的Data
	 * @param data
	 */
	public void setDataSent(Data data) {
		this.mDataSent = data;
	}

	/**
	 * 检查是否有瓶子的信息 没有的话抛出异常
	 * 
	 * @throws ServerException
	 */
	public void checkBottleState() throws ServerException {
		if (mBottle == null) {
			throw ServerException.makeBottleNotSetException();
		}
	}

	/**
	 * 尝试登陆到服务器
	 * 
	 * @throws ServerException
	 *             登陆错误的话抛出各种异常
	 */
	public boolean logIn() throws ServerException {
		checkBottleState();

		isLogIn = false;

		String uri = BASE_URI + "login.php";
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("bottleId", mBottle
				.getBottleId()));
		nameValuePairs.add(new BasicNameValuePair("password", mBottle
				.getPassword()));
		Log.d(this.getClass().getName(), nameValuePairs.toString());

		DefaultHttpClient httpClient = new DefaultHttpClient();
		JSONObject jsonObject = getPostRequestForJSONObject(uri,
				nameValuePairs, httpClient, null);
		String result;
		try {
			result = jsonObject.getString("result");
			if (result.equals("success")) {
				// 登陆成功，保存好SESSIONID
				CookieStore cookieStore = httpClient.getCookieStore();
				List<Cookie> cookies = cookieStore.getCookies();
				boolean haveSessionFlag = false;
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals(KEY_PHPSESSID)) {
						haveSessionFlag = true;
						isLogIn = true;
						SESSION_ID = cookie.getValue();
						break;
					}
				}
				if (!haveSessionFlag) {
					throw ServerException.makeGeneralException("session_err",
							"");
				}
			} else {
				throw ServerException.makeAuthFailureException();
			}
		} catch (JSONException e) {
			
			e.printStackTrace();
		}
		
		return isLogIn;
	}

	public boolean isLogIn() {
		return isLogIn;
	}

	/**
	 * 向服务器询问是否有未读的回复消息，多条消息按照时间戳升序排列
	 * 如果有终止会话的消息，那么会放在最后一个，消息ID=1 
	 * @param targetBottleId 指定只能接受这个ID的新回复消息，为null则不限制
	 * @return 如果没有则返回null，否则返回消息ID的数组，按照时间升序
	 * @throws ServerException
	 */
	public List<String> requestMessage(String targetBottleId) throws ServerException {
		checkBottleState();

		String uri = BASE_URI + "request_message.php";
		if(null != targetBottleId){
			uri += ("?target=" + targetBottleId);
		}
		
		JSONObject jsonObject = getGetResponseForJSONObject(uri, null, null);
		try {
			int state = jsonObject.getInt("state");
			switch (state) {
			case 0: // success
				JSONArray jsonArray = jsonObject.getJSONArray("content");
				mLatestDataId = new ArrayList<String>();;
				for(int i=0; i<jsonArray.length(); i++){
					mLatestDataId.add(jsonArray.getString(i));
				}
				return mLatestDataId;
			case 1:
				mLatestDataId = null;
				return mLatestDataId;
			case 2:
				throw ServerException.makeAuthFailureException();
			case 3:
				throw ServerException.makeServaeInternalException(jsonObject
						.getString("content"));
			default:
				Log.e("WHAT", "Couldn't be here!");
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * 从服务器取一条消息 如果不指定则返回随机推荐消息<br/>
	 * <b>如果需要保留服务器的消息及数据，则使用有变量keepDataAtServer的方法</b>
	 * 
	 * @param dataId
	 *            (可为null)指定一条消息
	 * @return 对应的Data
	 * @throws ServerException
	 */
	public Data getMessage(String dataId) throws ServerException{
		return getMessage(dataId, false);
	}

	/**
	 * 从服务器取一条消息 如果不指定则返回随机推荐消息
	 * 
	 * @param dataId
	 *            (可为null)指定一条消息
	 * @param keepDataAtServer 是否保留这条消息在服务器的记录（DEBUG用）
	 * @return 对应的Data
	 * @throws ServerException
	 */
	public Data getMessage(String dataId, boolean keepDataAtServer) throws ServerException {
		checkBottleState();

		List<NameValuePair> params = null;
		if (null != dataId) {
			if(dataId.equals(DATA_ID_END_CONVERSATION)){
				throw ServerException.makeConversationEndException();
			}
			params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("dataid", dataId));
		}
		if (true == keepDataAtServer){
			if(null == params)
				params = new ArrayList<NameValuePair>();
			params.add(new BasicNameValuePair("keep", "1"));
		}
		
		String uri = BASE_URI + "get_message.php";
		JSONObject jsonObject = getGetResponseForJSONObject(uri, params, null);
		try {
			int state = jsonObject.getInt("state");
			switch (state) {
			case 0: // SUCCESS
				byte[] rawData = Base64.decode(jsonObject.getString("rawdata"),
						Base64.DEFAULT);
				double[] loc = { jsonObject.getDouble("lon"),
						jsonObject.getDouble("lat") };
				String hexColor = jsonObject.getString("color");
				long timestamp = jsonObject.getLong("timestamp");
				boolean isReplyMsg = jsonObject.getBoolean("replymsg");
				//String remoteFileName = jsonObject.getString("filename"); 
				//留着以后有需要的时候用
				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.CHINA);
				String filename = dateFormat.format(new Date());
				File file = FileIOHelper.writeToFile(FILE_URI + 
						"/" + filename + ".amr"
						, rawData);

				mDataReceived = new Data(file, loc, hexColor, timestamp);
				mDataReceived.setSender(jsonObject.getString("sender"));
				mDataReceived.setReplyMsg(isReplyMsg);
				if (!jsonObject.getString("discards").equals("null")) {
					mDataReceived.setDiscards(jsonObject.getString("discards"));
				}
				if(null != dataId){
					mLatestSender = mDataReceived.getSender();
				}
				return mDataReceived;
			case 1:
				// not login
				throw ServerException.makeAuthFailureException();
			case 2:
				throw ServerException.makeServaeInternalException(jsonObject
						.getString("reason"));
			case 3:
				throw ServerException.makeDataExhaustedException();

			default:
				break;
			}

		} catch (JSONException e) {
			
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * 得到HttpResponse响应，如果有SESSION也会传SESSION过去
	 * 
	 * @param uri
	 * @param entity
	 * @param httpClient
	 *            如果需要自带client可以传入，否则传入null使用默认的即可
	 * @param file
	 *            文件，如果有文件需要上传，则放置，否则给null
	 * @return 如果连接异常，则返回 null
	 */
	private HttpResponse getPostResponse(String uri, List<NameValuePair> params,
			HttpClient httpClient, File file) {
		HttpPost httpPost = new HttpPost(uri);
		// UrlEncodedFormEntity httpEntity;
		MultipartEntity httpEntity;
		try {
			if (params != null) {
				// httpEntity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
				httpEntity = new MultipartEntity();
				for (NameValuePair param : params) {
					StringBody body = new StringBody(param.getValue());
					httpEntity.addPart(param.getName(), body);
				}
				if (null != file) {
					httpEntity.addPart("file", new FileBody(file));
				}
				httpPost.setEntity(httpEntity);

			}
			if (null != SESSION_ID) {
				httpPost.setHeader("cookie", KEY_PHPSESSID + "=" + SESSION_ID);
			}
			if (null == httpClient) {
				httpClient = new DefaultHttpClient();
			}
			HttpResponse httpResponse = httpClient.execute(httpPost);
			return httpResponse;
		} catch (UnsupportedEncodingException e) {
			
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * HTTP post请求，返回JSON对象
	 * 
	 * @param uri
	 * @param params
	 *            可为null
	 * 
	 * @return
	 */
	private JSONObject getPostRequestForJSONObject(String uri,
			List<NameValuePair> params, HttpClient httpClient, File file) {
		HttpResponse httpResponse = getPostResponse(uri, params, httpClient,
				file);
		if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			StringBuilder builder = new StringBuilder();
			BufferedReader reader;
			try {
				reader = new BufferedReader(new InputStreamReader(httpResponse
						.getEntity().getContent()));
				String line = reader.readLine();
				while (line != null) {
					builder.append(line);
					line = reader.readLine();
				}
				Log.i(this.getClass().getName(), ">>>>>>" + builder.toString());
				JSONObject jsonObject = new JSONObject(builder.toString());
				return jsonObject;

			} catch (IllegalStateException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		Log.e("ServerConn", "Bad http post response code = "
				+ httpResponse.getStatusLine().getStatusCode());
		return null;
	}

	/**
	 * HTTP Get 请求
	 * 
	 * @param uri
	 * @param params
	 *            可为null
	 * @param httpClient
	 *            （可传入null使用默认连接）
	 * @return
	 */
	private HttpResponse getGetResponse(String uri, List<NameValuePair> params,
			HttpClient httpClient) {
		StringBuilder stringBuilder = new StringBuilder();
		if (params != null) {
			for (NameValuePair pair : params) {
				if (stringBuilder.length() > 0) {
					stringBuilder.append("&");
				} else {
					stringBuilder.append("?");
				}
				stringBuilder.append(pair.getName() + "=" + pair.getValue());
			}
		}

		uri = uri + stringBuilder.toString();
		Log.d(this.getClass().getName(), uri);
		HttpGet httpGet = new HttpGet(uri);
		if (null != SESSION_ID) {
			httpGet.setHeader("cookie", KEY_PHPSESSID + "=" + SESSION_ID);
		}
		if (null == httpClient) {
			httpClient = new DefaultHttpClient();
		}

		try {
			HttpResponse httpResponse = httpClient.execute(httpGet);
			return httpResponse;

		} catch (ClientProtocolException e) {
			
			e.printStackTrace();
		} catch (IOException e) {
			
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * HTTP Get方法获得JSON对象
	 * 
	 * @param uri
	 * @param params
	 * @param httpClient
	 * @return 操作失败则返回null
	 */
	private JSONObject getGetResponseForJSONObject(String uri,
			List<NameValuePair> params, HttpClient httpClient) {
		HttpResponse httpResponse = getGetResponse(uri, params, httpClient);
		if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
			StringBuilder builder = new StringBuilder();
			BufferedReader reader;
			try {
				reader = new BufferedReader(new InputStreamReader(httpResponse
						.getEntity().getContent()));
				String line = reader.readLine();
				while (line != null) {
					builder.append(line);
					line = reader.readLine();
				}
				Log.i(this.getClass().getName(), ">>>>>>" + builder.toString());
				JSONObject jsonObject = new JSONObject(builder.toString());
				return jsonObject;

			} catch (IllegalStateException e) {
				
				e.printStackTrace();
			} catch (IOException e) {
				
				e.printStackTrace();
			} catch (JSONException e) {
				
				e.printStackTrace();
			}
		}
		return null;
	}

	/**
	 * 往服务器上传一条消息 Data对象会保存到类的mData中
	 * 
	 * @param data
	 *            如果data==null则使用上一次存储的data(Helper内的mData对象)
	 * @return
	 * @throws ServerException
	 */
	public boolean putMessage(Data data) throws ServerException {
		if (null != data) {
			mDataSent = data;
		}
		if (null == mDataSent) {
			Log.e("WTF", "Data unset!");
			return false;
		}

		String uri = BASE_URI + "put_message.php";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("timestamp", String.valueOf(mDataSent
				.getTimestamp())));
		params.add(new BasicNameValuePair("longitude", String.valueOf(mDataSent
				.getLocation()[0])));
		params.add(new BasicNameValuePair("latitude", String.valueOf(mDataSent
				.getLocation()[1])));
		if (mDataSent.haveTarget()) {
			params.add(new BasicNameValuePair("target", mDataSent.getTarget()));
		}
		if (mDataSent.haveDiscardRecords()) {
			params.add(new BasicNameValuePair("discards", mDataSent.getDiscards()));
		}
		if (mDataSent.isDiscardedMsg()){
			//被丢回的消息
			params.add(new BasicNameValuePair("orig_sender", mDataSent.getSender()));
		}

		JSONObject jsonObject = getPostRequestForJSONObject(uri, params, null,
				mDataSent.getFile());
		try {
			int state = jsonObject.getInt("errcode");
			switch (state) {
			case 0:
				return true; // succeed!
			case 1:
				throw ServerException.makeAuthFailureException();
			default:
				throw ServerException.makeServaeInternalException(jsonObject
						.getString("reason"));
			}

		} catch (JSONException e) {
			
			e.printStackTrace();
		}
		return false;
	}
	
	/**
	 * 将上一条消息丢回大海
	 * 如果消息被丢弃的记录达到5次，记录将不会回传到服务器，直接被销毁
	 * <br/>
	 * <b>[注意]回复消息不能被抛回大海，执行此操作消息将直接被销毁</b>
	 * <br/>
	 * <b>此方法不会删除本地存储的文件，如需删除请调用重构的带参数的方法
	 * 
	 * @return 操作结果
	 * @throws ServerException 
	 */
	public boolean throwMessage() throws ServerException{
		return throwMessage(false);
	}
	
	/**
	 * 将上一条消息丢回大海
	 * 如果消息被丢弃的记录达到5次，记录将不会回传到服务器，直接被销毁
	 * <br/>
	 * <b>[注意]回复消息不能被抛回大海，执行此操作消息将直接被销毁</b>
	 * @param removeLocalFile 是否删除本地存储的文件
	 * @return 操作结果
	 * @throws ServerException 
	 */
	public boolean throwMessage(boolean removeLocalFile) throws ServerException{
		if(mDataReceived == null || mBottle == null){
			Log.e("Empty data or bottle", "Please set them before throw it");
			return false;
		}
		
		if(mDataReceived.isReplyMsgReceived()){
			Log.w("mData will be dropped", "because it is a reply message !!!");
			if(mDataReceived.getFile().exists() && removeLocalFile){
				mDataReceived.getFile().delete();
			}
			mDataReceived = null;
			return true;
		}
		
		int throwCount = mDataReceived.getThrowCount();
		if(throwCount >= MAX_THROW_COUNT ){
			Log.w("mData will be dropped", "because it exceeds maximum drop count");
			if(mDataReceived.getFile().exists() && removeLocalFile){
				mDataReceived.getFile().delete();
			}
			mDataReceived = null;
			return true;
		}
		mDataReceived.setDiscardedMsg(true);
		mDataReceived.addDiscardRecord(mBottle);
		
		boolean putResult =  putMessage(mDataReceived);
		if(putResult){
			if(mDataReceived.getFile().exists() && removeLocalFile){
				mDataReceived.getFile().delete();
			}
		}
		mDataReceived = null;
		
		return putResult;
	}
	
	/**
	 * 准备一条回复的消息供填充内容，其中消息的target已设置好
	 * @param setToDataStore 是否存储在helper的内部数据对象mDataSent中
	 * @return 一条回复的消息
	 */
	public Data prepareReplyMessage(boolean setToDataStore){
		if(mDataReceived == null){
			Log.e("ServerConnectionHelper:prepareReplyMsg", "Could not find any data received.");
			return null;
		}
		
		Data data = new Data();
		data.setTarget(mDataReceived.getSender());//将原消息的发件人设置为当前消息的发送目标
		if(setToDataStore)
			mDataSent = data;
		return data;
	}
	
	/**
	 * 重构的方法，准备一条回复的消息供填充内容，其中消息的target已设置好
	 * 使用该不带参数的方法，不会影响helper的mData变量
	 * @return
	 */
	public Data prepareReplyMessage(){
		return prepareReplyMessage(false);
	}
	
	/**
	 * 结束当前对话
	 * @param target 对方的ID，如果设置为null则使用helper自己记录下的上次发件人的ID
	 * @throws ServerException 
	 */
	public void endConversation(String target) throws ServerException{
		if(null == mLatestSender && null == target){
			Log.e("endConversation", "Cannot find latest message sender... Have u got any msg before?");
		}
		
		if(null == target){
			target = mLatestSender;
		}
		
		String uri = BASE_URI + "end_conversation.php";
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("target", target));
		
		JSONObject jsonObject = getGetResponseForJSONObject(uri, params, null);
		try {
			int state = jsonObject.getInt("state");
			switch(state){
			case 0:
				Log.d("Yep..", "Conversation ended.");
				return;
			case 1:
				throw ServerException.makeAuthFailureException();
			case 2:
				throw ServerException.makeServaeInternalException(jsonObject.getString("content"));
			}
		} catch (JSONException e) {
			
			e.printStackTrace();
		}
		Log.e("!!!!!", "CANNOT END CONVERSATION.");
	}
	
	
}
