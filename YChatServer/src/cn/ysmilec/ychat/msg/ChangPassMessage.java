package cn.ysmilec.ychat.msg;

/**
 * 功能：
 * 修改密码类
 * @author ysmilec
 *
 */
public class ChangPassMessage extends AbstractMessage{
	private static final long serialVersionUID = 2541381575315410731L;
	String oldpaswd;
	String newpasswd;
	public String getOldpaswd() {
		return oldpaswd;
	}
	public void setOldpaswd(String oldpaswd) {
		this.oldpaswd = oldpaswd;
	}
	public String getNewpasswd() {
		return newpasswd;
	}
	public void setNewpasswd(String newpasswd) {
		this.newpasswd = newpasswd;
	}
}
