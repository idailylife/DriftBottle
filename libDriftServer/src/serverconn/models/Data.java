package serverconn.models;

import java.io.File;

/**
 * Data的数据模型
 * @author BorisHe
 *
 */
public class Data {
	//private byte[] rawdata;
	//private int dataId;
	private double[] location; //{经度,纬度}
	private String color; //颜色信息
	private long timestamp; //UNIX时间戳
	private String target; //是给谁回复的消息
	private String discards; //丢弃记录
	private String sender; //发送人(接收消息时)
	
	private File file; //存储到的文件记录
	
	private boolean isDiscardedMsg;  //这条消息是不是被丢弃的消息（被丢弃消息传回服务器时，发件人的ID为原始发送者的ID）
	private boolean isReplyMsg = false; //是否是别人回复给我的消息
	
	public boolean isDiscardedMsg() {
		return isDiscardedMsg;
	}

	public void setDiscardedMsg(boolean isDiscardedMsg) {
		this.isDiscardedMsg = isDiscardedMsg;
	}

	public Data(){};
	
	public Data(File f, double[] loc, String colour, long time) {
		//setDataId(id);
		setFile(f);
		setLocation(loc);
		setColor(colour);
		setTimestamp(time);
	}
	
	/**
	 * 是否有丢弃的记录
	 * @return
	 */
	public boolean haveDiscardRecords(){
		if(null == discards)
			return false;
		if(discards.isEmpty())
			return false;
		return true;
	}
	
	public boolean haveTarget(){
		if(null == target)
			return false;
		if(target.isEmpty())
			return false;
		return true;
	}
	
	public double[] getLocation() {
		return location;
	}
	
	/**
	 * 
	 * @param location {经度,纬度}
	 */
	public void setLocation(double[] location) {
		this.location = location;
	}

	public String getColor() {
		return color;
	}

	public void setColor(String color) {
		this.color = color;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

//	public byte[] getRawdata() {
//		return rawdata;
//	}
//	public void setRawdata(byte[] rawdata) {
//		this.rawdata = rawdata;
//	}
//	public int getDataId() {
//		return dataId;
//	}
//	public void setDataId(int dataId) {
//		this.dataId = dataId;
//	}

	public String getTarget() {
		return target;
	}

	public void setTarget(String target) {
		this.target = target;
	}

	public String getDiscards() {
		return discards;
	}

	public void setDiscards(String discards) {
		this.discards = discards;
	}

	@Override
	public String toString() {
		String retStr = "Data object \n"
				+ "\nrawSize = " + file.getTotalSpace()
				+ "\ntime = " + timestamp
				+ "\nlocation = " + location[0] + "," + location[1];
		if(target != null)
			retStr += "\ntarget = " + target;
		if(discards != null)
			retStr += "\ndiscards = " + discards;
		retStr += "\nfilepath = " + file.getAbsolutePath();
		return retStr;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}
	
	/**
	 * 得到当前消息被丢弃的次数
	 * @return
	 */
	public int getThrowCount(){
		if(null == discards){
			return 0;
		}
		if(discards.isEmpty()){
			return 0;
		}
		String[] splitted = discards.split(" ");
		return splitted.length;
	}
	
	/**
	 * 增加一条丢弃的记录
	 * @param bottle
	 */
	public void addDiscardRecord(Bottle bottle){
		if(null == discards || discards.isEmpty()){
			discards = bottle.getBottleId();
		} else {
			discards += " " + bottle.getBottleId();
		}
	}

	public String getSender() {
		return sender;
	}
	
	public void setSender(String s){
		sender = s;
	}
	
	/**
	 * 这条消息是否是回复消息(target是否有效)
	 * @return
	 */
	public boolean isReplyMessage(){
		if(target == null)
			return false;
		if(target.isEmpty())
			return false;
		if(target.equals("null"))
			return false;
		return true;
	}

	/**
	 * 是否是收到的别人的回复消息
	 * @return
	 */
	public boolean isReplyMsgReceived() {
		return isReplyMsg;
	}

	public void setReplyMsg(boolean isReplyMsg) {
		this.isReplyMsg = isReplyMsg;
	}
	
}
