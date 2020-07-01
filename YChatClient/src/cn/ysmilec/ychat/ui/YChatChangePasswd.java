package cn.ysmilec.ychat.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.border.EmptyBorder;

import org.json.JSONObject;

import cn.ysmilec.ychat.io.JSONInputStream;
import cn.ysmilec.ychat.io.JSONOutputStream;
import cn.ysmilec.ychat.msg.ChangPassMessage;

/**
 * 功能：
 * 修改界面类，继承自JFrame类
 * @author ysmilec
 *
 */
public class YChatChangePasswd extends JFrame{
	private static final long serialVersionUID = 1663956804969400525L;
	private JSONOutputStream jos;
//	用户名和密码
	private String localUsername,localPassword;
//	主页面Panel，界面元素
	private JPanel contentPane;

//	用户名
	private JLabel usernameJLabel;
	private JLabel oldpasswdJLabel;
	private JLabel newpasswdJLabel;
	private JLabel newpasswdConfirmJLabel;
//	原密码、新密码和新密码确认输入框，界面元素
	private JPasswordField oldJPasswordField;
	private JPasswordField newpaJPasswordField;
	private JPasswordField newpaJPasswordConfirmField;
//	确认按钮
	private JButton btnChanpass;
	
	public YChatChangePasswd(String username, String passwd, JSONOutputStream jOutputStream) {
		this.localUsername = username;
		this.localPassword = passwd;
		this.jos = jOutputStream;
		setResizable(false);
		setTitle("修改密码");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setBounds(100, 100, 400, 280);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
//		用户名布局
		usernameJLabel = new JLabel("修改"+localUsername+"密码");
		usernameJLabel.setBounds(122, 15, 177, 35);
		contentPane.add(usernameJLabel);
//		修改密码布局
		oldpasswdJLabel = new JLabel("原密码:");
		oldJPasswordField = new JPasswordField();
		oldpasswdJLabel.setBounds(96, 65, 72, 27);
		oldJPasswordField.setBounds(172,65,145,27);
		contentPane.add(oldpasswdJLabel);
		contentPane.add(oldJPasswordField);
		
		newpasswdJLabel = new JLabel("新密码：");
		newpaJPasswordField = new JPasswordField();
		newpasswdJLabel.setBounds(96, 105, 72, 27);
		newpaJPasswordField.setBounds(172,105,145,27);
		contentPane.add(newpasswdJLabel);
		contentPane.add(newpaJPasswordField);
		
		newpasswdConfirmJLabel = new JLabel("确认密码：");
		newpaJPasswordConfirmField = new JPasswordField();
		newpasswdConfirmJLabel.setBounds(96, 145, 72, 27);
		newpaJPasswordConfirmField.setBounds(172,145,145,27);
		contentPane.add(newpasswdConfirmJLabel);
		contentPane.add(newpaJPasswordConfirmField);
		
		btnChanpass = new JButton("确认");
		btnChanpass.setBounds(161, 198, 78, 27);
		contentPane.add(btnChanpass);
		btnChanpass.addActionListener(new ActionListener() {		
			@Override
			public void actionPerformed(ActionEvent e) {	
				changePass();
			}
		});
			
	}
	
	/**
	 * 修改密码方法
	 */
	public void changePass() {
//		获得原密码，新密码和确认密码
		String oldpasswd = new String(oldJPasswordField.getPassword());
		String newpasswd = new String(newpaJPasswordField.getPassword());
		String newpasswdConfirm = new String(newpaJPasswordConfirmField.getPassword());
		if (oldpasswd.length()>0 && newpasswd.length()>0 && newpasswdConfirm.length()>0) {
//			如果密码信息全部填写
			if (oldpasswd.equals(localPassword)) {
//				如果原密码输入正确
				if (newpasswd.equals(newpasswdConfirm)) {
//					如果两次密码输入一致
//					创建用户注册消息
					ChangPassMessage message = new ChangPassMessage();
					message.setSrcUser(localUsername);
					message.setDstUser("");
					message.setNewpasswd(newpasswd);
					message.setOldpaswd(localPassword);
//					使用用户注册消息创建JSON对象
					JSONObject send = new JSONObject(message);
//					开启新线程发送注册消息
					new Thread(()->{
						try {
//							向JSON输出流写入JSON对象
							jos.writeJSONObject(send);
							jos.flush();
//							注销当前页面，将会在主页面中接收服务器响应
							dispose();
						} catch (IOException e) {
							JOptionPane.showMessageDialog(this, "网络连接失败，无法连接到服务器");
							e.printStackTrace();
						}
					}).start();
				} else {
					JOptionPane.showMessageDialog(this, "两次密码输入不一致");
				}
			} else {
				JOptionPane.showMessageDialog(this, "原密码输入不正确");
			}
		} else {
			JOptionPane.showMessageDialog(this, "请完善信息");
		}
	}

}