package cn.ysmilec.ychat.msg;

/**
 * 功能：
 * 状态消息
 * 
 * @author ysmilec
 *
 */
public class StateMessage extends AbstractMessage {
	
	private static final long serialVersionUID = 2420995190931098975L;
//	SUCCSEE_PASS代表修改密码成功
	public static final int SUCCESS = 0, FAILED = -1, SUCCSEE_PASS = 2;
//	消息状态码
	protected int status;
//	错误消息
	protected String error;
	
	public int getStatus() {
		return status;
	}
	public void setStatus(int status) {
		this.status = status;
	}
	public String getError() {
		return error;
	}
	public void setError(String error) {
		this.error = error;
	}

	@Override
	public boolean isPublicMessage() {
		return false;
	}
}
