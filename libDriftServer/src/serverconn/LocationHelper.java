package serverconn;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

/**
 * 负责获取地理位置信息
 * @author BorisHe
 *
 */
public class LocationHelper {
	private LocationManager mLocationManager;
	private Criteria mCriteria;
	private String mProviderStr;
	private Location mLatestLocation;
	
	/**
	 * 判断位置服务是否可用
	 * 需要GooglePlay位置服务可用并且启用才行
	 * @return
	 */
	public LocationHelper(Context c) {
		String contextLocation = Context.LOCATION_SERVICE;
		mLocationManager = (LocationManager)c.getSystemService(contextLocation);
		mCriteria = new Criteria();
		mCriteria.setAccuracy(Criteria.ACCURACY_LOW);
		mCriteria.setAltitudeRequired(false);
		mCriteria.setBearingAccuracy(Criteria.NO_REQUIREMENT);
		mCriteria.setBearingRequired(false);
		mCriteria.setCostAllowed(false);
		mCriteria.setPowerRequirement(Criteria.POWER_LOW);
		mProviderStr = mLocationManager.getBestProvider(mCriteria, true);
		startListeningLocationUpdate();
		updateWithNewLocation(getLastKnownLocation());
	}
	
	private void updateWithNewLocation(Location lastKnownLocation) {
		if(null != lastKnownLocation){
			mLatestLocation = lastKnownLocation;
			Log.d("LocationHelper", "Location updated :" + mLatestLocation.toString());
		} else {
			Log.d("LocationHelper", "Cannot retrieve location info.");
		}
	}

	/**
	 * 开始监听地理信息数据
	 */
	public void startListeningLocationUpdate(){
		mLocationManager.requestLocationUpdates(mProviderStr, 2000, 10, mLocationListener);
	}
	
	public final LocationListener mLocationListener =
			new LocationListener() {
				
				@Override
				public void onStatusChanged(String provider, int status, Bundle extras) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void onProviderEnabled(String provider) {
					// TODO Auto-generated method stub
					updateWithNewLocation(null);
				}
				
				@Override
				public void onProviderDisabled(String provider) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onLocationChanged(Location location) {
					// TODO Auto-generated method stub
					updateWithNewLocation(location);
				}
			};
	
	public Location getLastKnownLocation(){
		mLatestLocation = mLocationManager.getLastKnownLocation(mProviderStr);
		return mLatestLocation;
	}

	public Location getLatestLocation() {
		return mLatestLocation;
	}
	
	
}
