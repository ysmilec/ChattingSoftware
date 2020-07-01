package cn.ysmilec.ychat.ui;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.Properties;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import org.json.JSONException;
import org.json.JSONObject;

import cn.ysmilec.ychat.io.JSONInputStream;
import cn.ysmilec.ychat.io.JSONOutputStream;
import cn.ysmilec.ychat.msg.SigninMessage;
import cn.ysmilec.ychat.msg.StateMessage;
import cn.ysmilec.ychat.msg.UserStateMessage;
import cn.ysmilec.ychat.ui.YChatClient.ListeningHandler;

/**
 * 功能：
 * 登录界面继承JFrame
 * @author ysmilec
 *
 */

public class YChatLogin extends JFrame {
	private static final long serialVersionUID = -150496232371678337L;
	//	服务器主机名
	private static String host;
//	服务器端口
	private static int PORT;
//	配置文件
	private Properties properties = new Properties();
//	支持安全通信的Socket对象
	private SSLSocket socket;
//	JSON输入输出流
	private JSONInputStream jis;
	private JSONOutputStream jos;
//	用户名和密码
	private String localUsername,localPassword;
//	用户登录状态
	private int userState;
	
	private JPanel container;
	private JLabel userIdJLabel;
	private JTextField username;
	private JLabel passwdJLabel;
	private JPasswordField password;
	private JButton loginButton;
	private JButton registerButton;
	private JButton cleanButton;
	private JButton memorizeButton;
	private JLabel imageJLabel;
	private JCheckBox checkBoxSecretLogin;
	
	
	/**
	 * 启动应用程序的主方法
	 * @param args
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(()->{
			try {
				new YChatLogin().setVisible(true);;
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}
	/**
	 * 构造方法：创建窗体
	 */
	public YChatLogin() {	
		loadProperties();
		YChatLogin.host = properties.getProperty("ServerHost");
		YChatLogin.PORT = Integer.valueOf(properties.getProperty("ServerPort"));
		init();
	}
	public void init() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//设置窗体的位置及大小
		setBounds(100, 200, 500, 350);
		container = new JPanel();
		container.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(container);
		//设置一层相当于桌布的东西
		container.setLayout(new BorderLayout());//布局管理器
		//初始化--往窗体里放其他控件
		/*标题部分--North*/
		JPanel titlePanel = new JPanel();
		titlePanel.setLayout(new FlowLayout());
		titlePanel.add(new JLabel("YChat登录系统"));
		container.add(titlePanel, "North"); 
		/*输入部分--Center*/
		JPanel fieldPanel = new JPanel();
		fieldPanel.setLayout(null);
		imageJLabel = new JLabel();
		imageJLabel.setIcon(new ImageIcon(getClass().getResource("/cn/ysmilec/ychat/images/Login.png"))); 
		imageJLabel.setBounds(25, 50, 130, 130);
		fieldPanel.add(imageJLabel);
		userIdJLabel = new JLabel("用户名");
		userIdJLabel.setFont(new Font("宋体", 1, 18));
		userIdJLabel.setBounds(200, 60, 80, 28);
		passwdJLabel = new JLabel("密   码");
		passwdJLabel.setFont(new Font("宋体", 1, 18));
		passwdJLabel.setBounds(200, 100, 80, 28);
		fieldPanel.add(userIdJLabel);
		fieldPanel.add(passwdJLabel);
		username = new JTextField();
		username.setBounds(300, 60, 140, 28);
		password = new JPasswordField();
		password.setBounds(300, 100, 140, 28);
		cleanButton = new JButton("清空输入");
		cleanButton.setBounds(200, 160, 90, 25);
		memorizeButton = new JButton("记住密码");
		memorizeButton.setBounds(300, 160, 90, 25);
		checkBoxSecretLogin = new JCheckBox("隐身登录");
		checkBoxSecretLogin.setBounds(390, 160, 90, 30);
		fieldPanel.add(cleanButton);
		fieldPanel.add(memorizeButton);
		fieldPanel.add(username);
		fieldPanel.add(password);
		fieldPanel.add(checkBoxSecretLogin);
		container.add(fieldPanel, "Center");    
		/*按钮部分--South*/
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout());
		loginButton = new JButton("登录");
		registerButton = new JButton("注册");
		buttonPanel.add(loginButton);
		buttonPanel.add(registerButton);
		container.add(buttonPanel, "South");
		
//	          点击登录按钮	
		loginButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				login();
			}
		});
//      点击注册按钮
		registerButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				dispose();
				new YChatSignup().setVisible(true);			
			}
		});
//		点击清空按钮
		cleanButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				username.setText("");
				password.setText("");
			}
		});
	}
	

	/**
	 * 登录方法，由actionPerformed调用
	 */
	public void login() {
//		取出用户名，去除结尾的空格
		localUsername = username.getText().trim();
//		取出密码
		localPassword = new String(password.getPassword()).trim();
		if (localUsername.length() > 0 && localPassword.length() > 0) {
//			开启新线程进行登录认证操作
			new Thread(()->{
				dologinAuth(localUsername, localPassword, checkBoxSecretLogin.isSelected());
			}).start();
		} else {
			JOptionPane.showMessageDialog(this, "请输入用户名和密码");
		}
	}
	
	/**
	 * 加载配置文件
	 */
	private void loadProperties() {
		try {
//			获得资源的输入流
			InputStream inputStream = getClass().getResourceAsStream("/config/config.properties");
//			加载配置文件
			properties.load(inputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 初始化客户端SSL会话对象
	 * @return 客户端SSL会话对象
	 * @throws Exception
	 */
	private SSLContext initClientSSLContext() throws Exception {
//		打开密钥库文件
		FileInputStream truststorefis = new FileInputStream(properties.getProperty("KeyStoreFile"));
//		密钥库的密码
		char[] password = properties.getProperty("KeyStorePassword").toCharArray();
//		创建密钥库对象并加载
		KeyStore ts = KeyStore.getInstance("PKCS12");
		ts.load(truststorefis, password);
//		获得密钥管理工厂对象
		TrustManagerFactory factory = TrustManagerFactory.getInstance("SunX509");
		factory.init(ts);
//		创建并用上面的密钥管理工厂对象初始化SSL会话对象
		SSLContext context = SSLContext.getInstance("TLS");
//		第一个参数表示提供的证书，本例不需要客户端提供证书，故使用null
//		第二个参数是用于验证对方的证书，因为是客户端器模式所以只需要第二个参数
		context.init(null, factory.getTrustManagers(), null);		
		return context;
	}
	
	/**
	 * 进行登录验证的方法
	 * @param user 用户名
	 * @param password 密码
	 */
	private void dologinAuth(String user,String password,boolean isSecretLogin) {
		try {
//			初始化SSL会话
			SSLContext context = initClientSSLContext();
//			创建SSLSocket对象并连接到服务器
			socket = (SSLSocket) context.getSocketFactory().createSocket(host,PORT);		
//			设置启用的算法套件为所有支持的算法套件
			socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
//			创建JSON输入输出流
			jos = new JSONOutputStream(socket.getOutputStream());
			jis = new JSONInputStream(socket.getInputStream());
		} catch (UnknownHostException e) {
			JOptionPane.showMessageDialog(this, "网络连接失败，找不到服务器");
			e.printStackTrace();
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "网络连接失败，服务器正忙");
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
//		创建登录消息
		SigninMessage message = new SigninMessage();
		message.setSrcUser(user);
		message.setDstUser("");
		message.setPassword(password);
		message.setSecretSignin(isSecretLogin);
//		使用登录消息创建JSON对象
		JSONObject send = new JSONObject(message);
		try {
//			向JSON输出流写入JSON对象
			jos.writeJSONObject(send);
			jos.flush();
//			验证线程继续等待服务器响应
			JSONObject receive = jis.readJSONObject();
			EventQueue.invokeLater(()->{
//				获得响应后，执行完成登录认证操作
				didFinishloginAuth(receive,isSecretLogin);
			});
		} catch (IOException | JSONException e) {
			EventQueue.invokeLater(()->{
				JOptionPane.showMessageDialog(this, "网络连接失败，无法连接到服务器");
				e.printStackTrace();
			});
		}
	}
	
	/**
	 * 完成
	 * @param json
	 */
	private void didFinishloginAuth(JSONObject json,boolean isSecretLogin) {
//		解析JSON消息
		StateMessage stateMessage = (StateMessage) StateMessage.fromJSONObject(json, StateMessage.class);
		if(stateMessage==null) {
			System.out.println("接收到无法解析的JSON对象："+json.toString());
		} else if(stateMessage.getStatus()==StateMessage.SUCCESS) {
//			提示用户登录成功
			JOptionPane.showMessageDialog(this, "登录成功！\n 欢迎您："+localUsername);			
			if (isSecretLogin) {
				userState = UserStateMessage.STEALTH;
				
			} else {
				userState = UserStateMessage.ONLINE;
				
			}
			dispose();
			YChatClient yChatClient = new YChatClient(localUsername, localPassword, userState, jis, jos);
			if(userState == 200)
				yChatClient.setTitle("YChat客户端 -"+localUsername+"- 在线");
			else 
				yChatClient.setTitle("YChat客户端 -"+localUsername+"- 隐身");
			yChatClient.setVisible(true);
		} else {
//			该用户已登录
			if (stateMessage.getError().equals("-1")) {
				JOptionPane.showMessageDialog(this, "该用户已登录！不能重复登录！");
			}else {
				JOptionPane.showMessageDialog(this, "用户名或密码错误，或者登录错误");
			}			
		}
	}
		
	
}
