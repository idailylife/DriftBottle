package serverconn.models;

/**
 * bottle的模型
 * @author BorisHe
 *
 */
public class Bottle {
	private String bottleId;  //瓶子的ID号
	private String bottleType; //瓶子的类型（声音瓶啊什么的，目前没用）
	private String password; //密码
	
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
