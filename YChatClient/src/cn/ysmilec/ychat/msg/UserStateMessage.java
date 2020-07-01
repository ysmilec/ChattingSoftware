package cn.ysmilec.ychat.msg;

/**
 * 功能：
 * 用户状态消息
 * 
 * @author ysmilec
 *
 */
public class UserStateMessage extends AbstractMessage {
	private static final long serialVersionUID = 2308566134889508488L;
//  在线ONLINE 离线 OFFLINE 隐身STEALTH
	public static int ONLINE = 200, OFFLINE = 201, STEALTH = 202;
	protected int userState;
	
	public int getUserState() {
		return userState;
	}
	public void setUserState(int userState) {
		this.userState = userState;
	}

}