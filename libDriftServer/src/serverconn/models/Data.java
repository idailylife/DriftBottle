package serverconn.models;

import java.io.File;

/**
 * Data������ģ��
 * @author BorisHe
 *
 */
public class Data {
	//private byte[] rawdata;
	//private int dataId;
	private double[] location; //{����,γ��}
	private String color;
	private long timestamp;
	private String target; //�Ǹ�˭�ظ�����Ϣ
	private String discards; //������¼
	private String sender; //������(������Ϣʱ )
	
	private File file; //�洢�����ļ���¼
	
	public Data(){};
	
	public Data(File f, double[] loc, String colour, long time) {
		//setDataId(id);
		setFile(f);
		setLocation(loc);
		setColor(colour);
		setTimestamp(time);
	}
	
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
	 * @param location {����,γ��}
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
		return retStr;
	}

	public File getFile() {
		return file;
	}

	public void setFile(File file) {
		this.file = file;
	}
	
	/**
	 * �õ���ǰ��Ϣ�������Ĵ���
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
	 * ����һ�������ļ�¼
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
	
	public boolean isReplyMessage(){
		if(target == null)
			return false;
		if(target.isEmpty())
			return false;
		if(target.equals("null"))
			return false;
		return true;
	}
	
}
