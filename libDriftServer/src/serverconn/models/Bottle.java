package serverconn.models;

/**
 * bottle��ģ��
 * @author BorisHe
 *
 */
public class Bottle {
	private String bottleId;  //ƿ�ӵ�ID��
	private String bottleType; //ƿ�ӵ����ͣ�����ƿ��ʲô�ģ�Ŀǰû�ã�
	private String password; //����
	
	public Bottle (){};
	public Bottle(String id, String type, String pswd){
		setBottleId(id);
		setBottleType(type);
		setPassword(pswd);
	}
	
	public String getBottleId() {
		return bottleId;
	}
	public void setBottleId(String bottleId) {
		this.bottleId = bottleId;
	}
	public String getBottleType() {
		return bottleType;
	}
	public void setBottleType(String bottleType) {
		this.bottleType = bottleType;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	
	
}
