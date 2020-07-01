package cn.ysmilec.ychat.ui;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JPasswordField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.EventQueue;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.util.Properties;
import java.awt.event.ActionEvent;

import org.json.JSONObject;

import cn.ysmilec.ychat.io.JSONInputStream;
import cn.ysmilec.ychat.io.JSONOutputStream;
import cn.ysmilec.ychat.msg.SignupMessage;
import cn.ysmilec.ychat.msg.StateMessage;

/**
 * 功能：
 * 注册界面类，继承自JFrame类
 * 
 * @author ysmilec
 *
 */
public class YChatSignup extends JFrame {
	
	private static final long serialVersionUID = 5047756297176762089L;
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
//	主页面Panel，界面元素
	private JPanel contentPane;
//	用户名输入框，界面元素
	private JTextField textFieldUser;
//	密码和密码确认输入框，界面元素
	private JPasswordField passwordField;
	private JPasswordField passwordFieldConfirm;
//	用户昵称输入框，界面元素
	private JTextField textFieldName;
//	注册按钮
	private JButton btnSignUp;
//	返回按钮
	private JButton btnBack;

	/**
	 * 构造方法，创建窗体
	 */
	public YChatSignup() {
		setResizable(false);
		setTitle("注册");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setBounds(100, 100, 450, 300);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		btnSignUp = new JButton("注册");
		btnSignUp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				signup();
			}
		});
		btnSignUp.setBounds(108, 223, 117, 29);
		contentPane.add(btnSignUp);
		
		textFieldUser = new JTextField();
		textFieldUser.setBounds(83, 40, 299, 26);
		contentPane.add(textFieldUser);
		textFieldUser.setColumns(10);
		
		passwordField = new JPasswordField();
		passwordField.setBounds(83, 120, 299, 26);
		contentPane.add(passwordField);
		
		passwordFieldConfirm = new JPasswordField();
		passwordFieldConfirm.setBounds(83, 160, 299, 26);
		contentPane.add(passwordFieldConfirm);
		
		textFieldName = new JTextField();
		textFieldName.setBounds(83, 80, 299, 26);
		contentPane.add(textFieldName);
		textFieldName.setColumns(10);
		
		JLabel label = new JLabel("用户名");
		label.setBounds(30, 40, 60, 26);
		contentPane.add(label);
		
		JLabel label_1 = new JLabel("昵称");
		label_1.setBounds(30, 80, 60, 26);
		contentPane.add(label_1);
		
		JLabel label_2 = new JLabel("密码");
		label_2.setBounds(30, 120, 52, 26);
		contentPane.add(label_2);
		
		JLabel label_3 = new JLabel("确认");
		label_3.setBounds(30, 160, 52, 26);
		contentPane.add(label_3);
		
		btnBack = new JButton("返回");
		btnBack.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				返回时关闭当前窗口
				dispose();
				new YChatLogin().setVisible(true);
			}
		});
		btnBack.setBounds(237, 223, 117, 29);
		contentPane.add(btnBack);
		loadProperties();
		YChatSignup.host = properties.getProperty("ServerHost");
		YChatSignup.PORT = Integer.valueOf(properties.getProperty("ServerPort"));
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
//		KeyManagers私钥公钥都用，TrustManagers只用公钥
		context.init(null, factory.getTrustManagers(), null);		
		return context;
	}
	
	/**
	 * 注册方法
	 */
	public void signup() {
//		获得用户名和用户昵称，去除结尾的空格
		String user = textFieldUser.getText().trim();
		String name = textFieldName.getText().trim();
//		获得用户密码和确认的密码
		String password = new String(passwordField.getPassword());
		String passwordConfirm = new String(passwordFieldConfirm.getPassword());
		if (user.length() > 0 && name.length() > 0 && password.length() > 0 && passwordConfirm.length() > 0) {
//			如果注册信息全部填写
			if (password.equals(passwordConfirm)) {
//				如果两次密码输入一致
				try {
//					初始化SSL会话
					SSLContext context = initClientSSLContext();
//					创建SSLSocket连接到服务器
					socket = (SSLSocket) context.getSocketFactory().createSocket(host,PORT);		
//					设置启用的算法套件为所有支持的算法套件
					socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
//					创建JSON输入输出流
					jos = new JSONOutputStream(socket.getOutputStream());
					jis = new JSONInputStream(socket.getInputStream());
				}catch (UnknownHostException e) {
					JOptionPane.showMessageDialog(this, "网络连接失败，找不到服务器");
					e.printStackTrace();
				} catch (Exception e) {
					JOptionPane.showMessageDialog(this, "网络连接失败，服务器正忙");
					e.printStackTrace();
				}
//				创建用户注册消息
				SignupMessage message = new SignupMessage();
				message.setSrcUser(user);
				message.setName(name);
				message.setDstUser("");
				message.setPassword(passwordConfirm);
//				使用用户注册消息创建JSON对象
				JSONObject send = new JSONObject(message);
//				开启新线程发送注册消息
				new Thread(()->{
					try {
//						向JSON输出流写入JSON对象
						jos.writeJSONObject(send);
						jos.flush();
//						当前线程等待服务器响应
						JSONObject receive = jis.readJSONObject();
//						完成注册操作
						EventQueue.invokeLater(()->{
							didFinishSignup(receive);
						});
					} catch (IOException e) {
						JOptionPane.showMessageDialog(this, "网络连接失败，无法连接到服务器");
						e.printStackTrace();
					}
				}).start();
			} else {
				JOptionPane.showMessageDialog(this, "两次密码输入不一致");
			}
		} else {
			JOptionPane.showMessageDialog(this, "请完善注册信息");
		}
	}

	
	/**
	 * 完成注册操作
	 * @param json 响应JSON对象
	 */
	private void didFinishSignup(JSONObject json) {
//		反序列化JSON对象
		StateMessage stateMessage = (StateMessage) StateMessage.fromJSONObject(json, StateMessage.class);
		if(stateMessage==null) {
			System.out.println("接收到无法解析的JSON对象："+json.toString());
		} else if(stateMessage.getStatus()==StateMessage.SUCCESS) {
//			如果接收到注册成功消息
			JOptionPane.showMessageDialog(this, "注册成功");
			dispose();
//			重新创建客户端窗口
			new YChatLogin().setVisible(true);
		} else {
			JOptionPane.showMessageDialog(this, "注册过程出错");
		}
	}
}
