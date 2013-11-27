package edu.zju.inlab.driftbottle;

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
			Bottle bottle = new Bottle("10001", "0", "8080");
			helper.setmBottle(bottle);
			try {
				helper.logIn();
				String newMsgId = helper.requestMessage();
				Data data = null;
				if(newMsgId != null){
					Log.d("newmsg", "id=" + newMsgId);
					data = helper.getMessage(newMsgId);
				} else {
					Log.d("newmsg", "not found");
					data = helper.getMessage(null);
				}
				Log.d("blabla", data.toString());
				data.setTimestamp(System.currentTimeMillis() / 1000); // millisecond to second
				
				data.setTarget(helper.prepareReplyMessage(false).getTarget());
				helper.throwMessage();
				boolean b = helper.putMessage(data);
				if(b){
					Log.d("yaeh", "data transmitted!");
				} else {
					Log.d("no", "wtf");
				}
				
				
			} catch (ServerException e) {
				e.printStackTrace();
			}
			return true;
		}
    	
    }
    
}
