package cn.ysmilec.ychat.server.util;

import java.awt.EventQueue;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.table.DefaultTableModel;
import cn.ysmilec.ychat.server.util.User;
import cn.ysmilec.ychat.io.JSONInputStream;
import cn.ysmilec.ychat.io.JSONOutputStream;


/**
 * 功能：
 * 在线用户管理器
 * 
 * @author ysmilec
 *
 */
public class UserManager {
	
//	一个Map对象用于存储用户名和用户对象之间的映射关系
	private final Map<String, User> onlineUsers = new HashMap<String, User>();
	
	/**
	 * 判断用户是否在线
	 * @param username 用户名
	 * @return 在线返回true，否则返回false
	 */
	public boolean isUserOnline(String username) {
		return onlineUsers.containsKey(username);
	}
	
	/**
	 * 判断在线用户列表是否为空
	 * @return 为空返回true，否则返回false
	 */
	public boolean isEmpty() {
		return onlineUsers.isEmpty();
	}
	
	/**
	 * 返回一个用户的JSON输出流
	 * @param username 用户名
	 * @return JSON输出流对象
	 */
	public JSONOutputStream getUserJSONOutputStream(String username) {
		if (isUserOnline(username)) {
			return onlineUsers.get(username).getJos();
		} else {
			return null;
		}
	}
	
	/**
	 * 返回一个用户的对象输入流
	 * @param username 用户名
	 * @return 对象输入流对象
	 */
	public JSONInputStream getUserJSONInputStream(String username) {
		if (isUserOnline(username)) {
			return onlineUsers.get(username).getJis();
		} else {
			return null;
		}
	}
	
	/**
	 * 返回一个用户的Socket对象
	 * @param username 用户名
	 * @return Socket对象
	 */
	public Socket getUserSocket(String username) {
		if (isUserOnline(username)) {
			return onlineUsers.get(username).getSocket();
		} else {
			return null;
		}
	}
	
	/**
	 * 增加一个在线用户
	 * @param username 用户名
	 * @param socket 用户的Socket对象
	 * @return 增加成功返回true，否则返回false
	 */
	public boolean addUser(DefaultTableModel dtm, String username, Socket socket, JSONInputStream jis,JSONOutputStream jos, boolean isSecretSignin) {
		if (username == null || socket == null) {
			return false;
		}
//		将在线用户放入Map映射中
		onlineUsers.put(username, new User(socket,jis,jos));
//		提交EDT线程将在线用户显示在界面上
		EventQueue.invokeLater(()->{
			dtm.addRow(new String[]{username,new Date().toString(),socket.getInetAddress().getHostAddress(),String.valueOf(socket.getPort())});
		});
		return true;
	}
	
	/**
	 * 删除一个在线用户
	 * @param username 用户名
	 * @return 删除成功返回true，否则返回false
	 */
	public boolean removeUser(DefaultTableModel dtm, String username) {
//		判断用户在线状态，如果都不在线就别删除啦
		if (isUserOnline(username)) {
//			从Map映射中获得Set集合
			Set<String> set = onlineUsers.keySet();
			int i = 0;
//			遍历Set集合
			for (String string : set) {
				if (string.equals(username)) {
					break;
				}
				i++;
			}
//			找到该用户之后从Map映射中删除这个用户
			onlineUsers.remove(username);
//			提交EDT线程再将这个用户从界面上删除
			final int i2 = i;
			EventQueue.invokeLater(()->{
				dtm.removeRow(i2);
			});
			return true;
		}
		return false;
	}
	
	/**
	 * 获得所有在线用户的数组
	 * @return 在线用户用户名数组
	 */
	public String[] getAllOnlineUsers() {
//		转换为数组的时候接受一个数组类型参数，用于指示转换成的数组类型
		return onlineUsers.keySet().toArray(new String[0]);
	}
	
	public void removeAllUsers(DefaultTableModel dtm) {
		onlineUsers.clear();
		while (dtm.getRowCount() != 0) {
			dtm.removeRow(0);
		}
	}
	
	/**
	 * 获得所有在线用户的个数
	 * @return 在线用户个数
	 */
	public int getOnlineUserCount() {
		return onlineUsers.size();
	}
}//UserManager
