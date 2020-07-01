package cn.ysmilec.ychat.msg;

/**
 * 功能：
 * 注销消息
 * 
 * @author ysmilec
 *
 */
public class SignoutMessage extends AbstractMessage {

	private static final long serialVersionUID = -4290233543928590957L;
	
	@Override
	public boolean isPublicMessage() {
		return false;
	}
}
