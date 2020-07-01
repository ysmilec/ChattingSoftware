package cn.ysmilec.ychat.server.util;

import java.net.Socket;
import java.util.Date;

import cn.ysmilec.ychat.io.JSONInputStream;
import cn.ysmilec.ychat.io.JSONOutputStream;

/**
 * 功能：
 * 用户类型，一个User对象代表一个在线用户
 * 
 * @author ysmilec
 *
 */
class User {
//	用户的Socket对象
	private final Socket socket;
//	用户的JSON输入输出流
	private JSONInputStream jis;
	private JSONOutputStream jos;
//	用户的登录时间
	private final Date loginTime;
	
	/**
	 * 构造函数，接受一个Socket对象以及JSON输入输出流
	 * @param socket Socket对象，将不会从Socket对象中获取输入输出流
	 * @param jis JSON输入流
	 * @param jos JSON输出流
	 */
	public User(Socket socket,JSONInputStream jis,JSONOutputStream jos) {
		this.socket = socket;
		this.jis = jis;
		this.jos = jos;
		loginTime = new Date();
	}
	
	/**
	 * 得到Socket对象
	 * @return Socket对象
	 */
	public Socket getSocket() {
		return socket;
	}
	
	/**
	 * 得到JSON输入流
	 * @return JSON输入流
	 */
	public JSONInputStream getJis() {
		return jis;
	}
	
	/**
	 * 得到JSON输出流
	 * @return JSON输出流
	 */
	public JSONOutputStream getJos() {
		return jos;
	}
	
	/**
	 * 得到登录时间
	 * @return 登录时间
	 */
	public Date getLoginTime() {
		return loginTime;
	}
	
}
