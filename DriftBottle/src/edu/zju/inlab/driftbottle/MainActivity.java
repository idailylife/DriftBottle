package edu.zju.inlab.driftbottle;

import java.io.File;
import java.util.Date;
import java.util.List;

import serverconn.ServerConnectionHelper;
import serverconn.ServerException;
import serverconn.models.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;

public class MainActivity extends Activity {
	private ServerConnectionHelper helper
		= ServerConnectionHelper.getInstance();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        new AsyncTestCase().execute();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    
    private class AsyncTestCase extends AsyncTask<Void, Void, Boolean>{
    	
    	

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			Log.d("postexec", ""+ result);
			
		}

		@Override
		protected Boolean doInBackground(Void... params) {
			Bottle bottle = new Bottle("10003", "0", "8080");
			helper.setBottle(bottle);
			try {
				helper.logIn();
				List<String> newMsgId = helper.requestMessage("10006");
				Data data = null;
				if(newMsgId != null){
					Log.d("newmsg", "id=" + newMsgId);
					for(String id: newMsgId){
						helper.getMessage(id);
					}
					
				} else {
					Log.d("newmsg", "not found");
					data = helper.getMessage(null, true);
				}
				
//				if(null != data)
//					helper.throwMessage();
				Data nData = helper.prepareReplyMessage();
				nData.setLocation(new double[]{1.2, 1.3});
				File dataFile = new File(helper.getLatestFileNameAndPath());
				nData.setFile(dataFile);
				nData.setTimestamp(System.currentTimeMillis()/1000);
				helper.putMessage(nData);
				
				
			} catch (ServerException e) {
				e.printStackTrace();
				switch(e.getmExceptionCode()){
				case ServerException.TYPE_GENERAL:
					break;
				case ServerException.TYPE_BOTTLE_ERROR:
					break;
					//More cases...
				default:
					break;
					
				}
			}
			return true;
		}
    	
    }
    
}
