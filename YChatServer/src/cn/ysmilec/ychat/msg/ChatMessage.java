package cn.ysmilec.ychat.msg;

/**
 * 功能：
 * 聊天消息
 * 
 * @author ysmilec
 *
 */
public class ChatMessage extends AbstractMessage {
	
	private static final long serialVersionUID = -2208082869128050072L;
//	消息正文
	protected String msgContent;

	public String getMsgContent() {
		return msgContent;
	}

	public void setMsgContent(String msgContent) {
		this.msgContent = msgContent;
	}

}
