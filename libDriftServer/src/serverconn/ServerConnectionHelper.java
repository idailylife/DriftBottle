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
 * �����������, ʹ�õ���ģʽ
 * 
 * @author BorisHe
 * 
 */
public class ServerConnectionHelper {
	private static ServerConnectionHelper INSTANCE = new ServerConnectionHelper();
	private static final String BASE_URI = "http://driftbottlezju.duapp.com/DriftBottle/";
	private String FILE_URI = null; // �ļ���д·��(�����ļ���)

	private int MAX_THROW_COUNT = 5; //���������

	private String SESSION_ID = null;

	private Bottle mBottle = null;
	//private Data mData = null;
	private Data mDataReceived = null; //���һ���յ�����Ϣ
	private Data mDataSent = null; //���һ�����͵���Ϣ
	
	private boolean isLogIn = false;
	private Object KEY_PHPSESSID = "PHPSESSID";
	private List<String> mLatestDataId = null;
	private String mLatestSender = null; //�洢����һ����Ϣ�ķ����˼�¼
	
	public final String DATA_ID_END_CONVERSATION = "1";  //��ֹ�Ự�õ�����ID��
	
	public String getmLatestSender() {
		return mLatestSender;
	}

	public List<String> getLatestDataId() {
		return mLatestDataId;
	}
	
	/**
	 * �������洢���ļ��ľ���·��
	 * @return ���û���򷵻�null
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
		//FILE_URI += "/file.raw"; //�ļ����ƴ���DEFAULT_FILE_NAME
		Log.d("fileDir", FILE_URI);
	}

	/**
	 * ǿ�Ƹ���Ĭ�ϴ洢�ļ���<b>·��</b><s><i>���ļ���</i></s> ע�ⰲȫ...
	 * 
	 * @param filepath
	 *            �����ļ�·��(Ŀ¼��ַ)
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
	 * ��ȡ���һ���յ���Data
	 * @return
	 */
	public Data getDataReceived() {
		return mDataReceived;
	}

	/**
	 * ����Ҫ���͵�Data
	 * @param data
	 */
	public void setDataSent(Data data) {
		this.mDataSent = data;
	}

	/**
	 * ����Ƿ���ƿ�ӵ���Ϣ û�еĻ��׳��쳣
	 * 
	 * @throws ServerException
	 */
	public void checkBottleState() throws ServerException {
		if (mBottle == null) {
			throw ServerException.makeBottleNotSetException();
		}
	}

	/**
	 * ���Ե�½��������
	 * 
	 * @throws ServerException
	 *             ��½����Ļ��׳������쳣
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
				// ��½�ɹ��������SESSIONID
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
	 * �������ѯ���Ƿ���δ���Ļظ���Ϣ��������Ϣ����ʱ�����������
	 * �������ֹ�Ự����Ϣ����ô��������һ������ϢID=1 
	 * @param targetBottleId ָ��ֻ�ܽ������ID���»ظ���Ϣ��Ϊnull������
	 * @return ���û���򷵻�null�����򷵻���ϢID�����飬����ʱ������
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
	 * �ӷ�����ȡһ����Ϣ �����ָ���򷵻�����Ƽ���Ϣ<br/>
	 * <b>�����Ҫ��������������Ϣ�����ݣ���ʹ���б���keepDataAtServer�ķ���</b>
	 * 
	 * @param dataId
	 *            (��Ϊnull)ָ��һ����Ϣ
	 * @return ��Ӧ��Data
	 * @throws ServerException
	 */
	public Data getMessage(String dataId) throws ServerException{
		return getMessage(dataId, false);
	}

	/**
	 * �ӷ�����ȡһ����Ϣ �����ָ���򷵻�����Ƽ���Ϣ
	 * 
	 * @param dataId
	 *            (��Ϊnull)ָ��һ����Ϣ
	 * @param keepDataAtServer �Ƿ���������Ϣ�ڷ������ļ�¼��DEBUG�ã�
	 * @return ��Ӧ��Data
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
				//�����Ժ�����Ҫ��ʱ����
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
	 * �õ�HttpResponse��Ӧ�������SESSIONҲ�ᴫSESSION��ȥ
	 * 
	 * @param uri
	 * @param entity
	 * @param httpClient
	 *            �����Ҫ�Դ�client���Դ��룬������nullʹ��Ĭ�ϵļ���
	 * @param file
	 *            �ļ���������ļ���Ҫ�ϴ�������ã������null
	 * @return ��������쳣���򷵻� null
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
	 * HTTP post���󣬷���JSON����
	 * 
	 * @param uri
	 * @param params
	 *            ��Ϊnull
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
	 * HTTP Get ����
	 * 
	 * @param uri
	 * @param params
	 *            ��Ϊnull
	 * @param httpClient
	 *            ���ɴ���nullʹ��Ĭ�����ӣ�
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
	 * HTTP Get�������JSON����
	 * 
	 * @param uri
	 * @param params
	 * @param httpClient
	 * @return ����ʧ���򷵻�null
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
	 * ���������ϴ�һ����Ϣ Data����ᱣ�浽���mData��
	 * 
	 * @param data
	 *            ���data==null��ʹ����һ�δ洢��data(Helper�ڵ�mData����)
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
			//�����ص���Ϣ
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
	 * ����һ����Ϣ���ش�
	 * �����Ϣ�������ļ�¼�ﵽ5�Σ���¼������ش�����������ֱ�ӱ�����
	 * <br/>
	 * <b>[ע��]�ظ���Ϣ���ܱ��׻ش󺣣�ִ�д˲�����Ϣ��ֱ�ӱ�����</b>
	 * <br/>
	 * <b>�˷�������ɾ�����ش洢���ļ�������ɾ��������ع��Ĵ������ķ���
	 * 
	 * @return �������
	 * @throws ServerException 
	 */
	public boolean throwMessage() throws ServerException{
		return throwMessage(false);
	}
	
	/**
	 * ����һ����Ϣ���ش�
	 * �����Ϣ�������ļ�¼�ﵽ5�Σ���¼������ش�����������ֱ�ӱ�����
	 * <br/>
	 * <b>[ע��]�ظ���Ϣ���ܱ��׻ش󺣣�ִ�д˲�����Ϣ��ֱ�ӱ�����</b>
	 * @param removeLocalFile �Ƿ�ɾ�����ش洢���ļ�
	 * @return �������
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
	 * ׼��һ���ظ�����Ϣ��������ݣ�������Ϣ��target�����ú�
	 * @param setToDataStore �Ƿ�洢��helper���ڲ����ݶ���mDataSent��
	 * @return һ���ظ�����Ϣ
	 */
	public Data prepareReplyMessage(boolean setToDataStore){
		if(mDataReceived == null){
			Log.e("ServerConnectionHelper:prepareReplyMsg", "Could not find any data received.");
			return null;
		}
		
		Data data = new Data();
		data.setTarget(mDataReceived.getSender());//��ԭ��Ϣ�ķ���������Ϊ��ǰ��Ϣ�ķ���Ŀ��
		if(setToDataStore)
			mDataSent = data;
		return data;
	}
	
	/**
	 * �ع��ķ�����׼��һ���ظ�����Ϣ��������ݣ�������Ϣ��target�����ú�
	 * ʹ�øò��������ķ���������Ӱ��helper��mData����
	 * @return
	 */
	public Data prepareReplyMessage(){
		return prepareReplyMessage(false);
	}
	
	/**
	 * ������ǰ�Ի�
	 * @param target �Է���ID���������Ϊnull��ʹ��helper�Լ���¼�µ��ϴη����˵�ID
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
