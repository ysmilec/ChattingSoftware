package cn.ysmilec.ychat.server.ui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JTextPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JButton;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.sql.SQLException;
import java.util.Date;
import java.util.Enumeration;
import java.util.Properties;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import org.json.JSONObject;

import cn.ysmilec.ychat.io.JSONInputStream;
import cn.ysmilec.ychat.io.JSONOutputStream;
import cn.ysmilec.ychat.msg.AbstractMessage;
import cn.ysmilec.ychat.msg.ChangPassMessage;
import cn.ysmilec.ychat.msg.ChatMessage;
import cn.ysmilec.ychat.msg.FileTransferMessage;
import cn.ysmilec.ychat.msg.SigninMessage;
import cn.ysmilec.ychat.msg.SignoutMessage;
import cn.ysmilec.ychat.msg.SignupMessage;
import cn.ysmilec.ychat.msg.StateMessage;
import cn.ysmilec.ychat.msg.UserStateMessage;
import cn.ysmilec.ychat.server.db.DataBaseConnector;
import cn.ysmilec.ychat.server.db.DataBaseManager;
import cn.ysmilec.ychat.server.util.*;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

/**
 * 功能：
 * 服务器界面类，继承自JFrame类
 * 
 * @author ysmilec
 *
 */
public class YChatServer extends JFrame implements ChooseListener {
	
	private static final long serialVersionUID = 8482455133264907039L;
	
//	SSLServerSocket是用于支持安全通信的服务器Socket
	private SSLServerSocket serverSocket;
//	监听的端口
	private static int PORT;
//	配置文件
	private Properties properties = new Properties();
//	监听线程
	private Thread listenThread = null;
//	用户管理对象
	private final UserManager userManager = new UserManager();
//	表模型对象
	private final DefaultTableModel onlineUserDtm = new DefaultTableModel(new String[]{"用户名","登录时间","IP地址","端口"},0);
//	主页面Panel，界面元素
	private JPanel contentPane;
//	在线用户列表，界面元素
	private JTable tableOnlineUsers;
//	消息记录区域，界面元素
	private JTextPane textPaneLog;
	private JButton btnStart;
	private JButton btnStop;
	private JMenuItem mnItemStart;
	private JMenuItem mnItemStop;
	private JMenuItem mnItemSignup;
	private JMenuItem mnItemSignoutUser;

	/**
	 * 启动应用程序的主方法
	 * @param args
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(()->{
			try {
				YChatServer frame = new YChatServer();
				frame.setVisible(true);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

	/**
	 * 构造方法，创建窗体
	 */
	public YChatServer() {
		setTitle("YChat服务器 - 已停止");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 600, 400);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenu mnYChatserver = new JMenu("YChat");
		menuBar.add(mnYChatserver);
		
		JMenuItem mnItemAbout = new JMenuItem("关于YChat服务器");
		mnItemAbout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null, "YChat服务器\n作者：ysmilec", "关于YChat客户端", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		mnYChatserver.add(mnItemAbout);
		
		JMenuItem mnItemExit = new JMenuItem("退出YChat服务器");
		mnItemExit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopServer();
				System.exit(0);
			}
		});
		mnYChatserver.add(mnItemExit);
		
		JMenu menu_2 = new JMenu("服务器");
		menuBar.add(menu_2);
		
		mnItemStart = new JMenuItem("启动服务器");
		mnItemAbout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				prepareStartServer();
//				点击启动时，在新线程中启动服务器。
				listenThread = new Thread(()->{
					startServer();
				});
				listenThread.start();
			}
		});
		menu_2.add(mnItemStart);
		
		JMenuItem mnItemClearLog = new JMenuItem("清空日志记录");
		mnItemClearLog.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textPaneLog.setText("");
			}
		});
		menu_2.add(mnItemClearLog);
		
		mnItemStop = new JMenuItem("停止服务器");
		mnItemStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopServer();
			}
		});
		menu_2.add(mnItemStop);
		mnItemStop.setEnabled(false);
		
		JMenu mnNewMenu = new JMenu("用户");
		menuBar.add(mnNewMenu);
		
		mnItemSignup = new JMenuItem("注册用户");
		mnItemSignup.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new YChatSignup().setVisible(true);
			}
		});
		mnNewMenu.add(mnItemSignup);
		
		mnItemSignoutUser = new JMenuItem("下线用户");
		mnItemSignoutUser.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectUser();
			}
		});
		mnItemSignoutUser.setEnabled(false);
		mnNewMenu.add(mnItemSignoutUser);
		
		JMenu menu = new JMenu("数据库");
		menuBar.add(menu);
		
		JMenuItem mnItemTestDB = new JMenuItem("测试数据库");
		mnItemTestDB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				testDataBase();
			}
		});
		menu.add(mnItemTestDB);
		
		JMenuItem mnItemInitDB = new JMenuItem("初始化数据库");
		mnItemInitDB.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				initDataBase();
			}
		});
		menu.add(mnItemInitDB);
		
		JMenu menu_1 = new JMenu("配置文件");
		menuBar.add(menu_1);
		
		JMenuItem mnItemShowConfig = new JMenuItem("显示配置");
		mnItemShowConfig.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				showProperties();
			}
		});
		menu_1.add(mnItemShowConfig);
		
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JPanel centerPanel = new JPanel();
		contentPane.add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(1.0);
		centerPanel.add(splitPane);
		
		textPaneLog = new JTextPane();
		textPaneLog.setEditable(false);
		textPaneLog.setBorder(new TitledBorder(null, "日志记录", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		JScrollPane scrollPane_1 = new JScrollPane(textPaneLog);
		splitPane.setLeftComponent(scrollPane_1);
		
		tableOnlineUsers = new JTable(onlineUserDtm);
		JScrollPane scrollPane_2 = new JScrollPane(tableOnlineUsers);
		splitPane.setRightComponent(scrollPane_2);
		
		JPanel southPanel = new JPanel();
		contentPane.add(southPanel, BorderLayout.SOUTH);
		southPanel.setLayout(new FlowLayout());
		
		btnStart = new JButton("启动服务器");
		btnStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
//				点击启动时，在新线程中启动服务器。
				prepareStartServer();
				listenThread = new Thread(()->{
					startServer();
				});
				listenThread.start();
			}
		});
		southPanel.add(btnStart);
		
		btnStop = new JButton("停止服务器");
		btnStop.setEnabled(false);
		btnStop.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopServer();
			}
		});
		southPanel.add(btnStop);
		loadProperties();
		YChatServer.PORT = Integer.valueOf(properties.getProperty("Listen"));
	}// 构造方法end
	
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
	 * 测试数据库
	 * @return
	 */
	
	public boolean testDataBase() {
		String driver = properties.getProperty("DatabaseDriver");
		String host = properties.getProperty("DatabaseHost");
		String username = properties.getProperty("DatabaseUsername");
		String password = properties.getProperty("DatabasePassword");
//		创建数据库管理器
		DataBaseConnector dbConnector = new DataBaseConnector(driver,host,username,password.toCharArray());
		try {
			dbConnector.connect();
			JOptionPane.showMessageDialog(this, new StringBuilder("数据库连接成功，连接细节：\n类型：").append(host.split(":")[1]).append("\n主机名：").append(host.split("//")[1]).append("\n登录身份：").append(username).toString());
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, new StringBuilder("数据库错误：").append(e.getLocalizedMessage().toString()));
			return false;
		}
	}
	
	/**
	 * 初始化数据库
	 */
	public void initDataBase() {
		int reply = JOptionPane.showConfirmDialog(this, "将执行初始化数据库操作，您确定吗？", "操作确认", JOptionPane.YES_NO_OPTION);
		if (reply == JOptionPane.OK_OPTION) {
//			创建数据库管理器
			DataBaseManager dbManager = new DataBaseManager(properties.getProperty("DatabaseDriver"),
					properties.getProperty("DatabaseHost"),
					properties.getProperty("DatabaseUsername"),
					properties.getProperty("DatabasePassword").toCharArray());
			try {
				dbManager.connect();
				dbManager.initDatabase();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	
	/**
	 * 显示文件配置信息
	 */
	public void showProperties() {
		if (properties == null) {
			loadProperties();
		}
		Enumeration<Object> keys = properties.keys();
		StringBuilder message = new StringBuilder("配置文件信息：\n");
		while (keys.hasMoreElements()) {
			String key = (String) keys.nextElement();
			message.append(key);
			message.append(" = ");
			String value = properties.getProperty(key);
			message.append(value);
			message.append("\n");
		}
		JOptionPane.showMessageDialog(this, message);
	}
	
	/**
	 * 初始化服务器端SSL会话对象
	 * @return 服务器端SSL会话对象
	 * @throws Exception
	 */
	private SSLContext initSSLServerContext() throws Exception {
//		打开密钥库文件
		FileInputStream keystorefis = new FileInputStream(properties.getProperty("KeyStoreFile"));
//		密钥库的密码
		char[] password = properties.getProperty("KeyStorePassword").toCharArray();
//		创建密钥库对象并加载
		KeyStore ks = KeyStore.getInstance("PKCS12");
		ks.load(keystorefis, password);
//		获得密钥管理工厂对象
		KeyManagerFactory factory = KeyManagerFactory.getInstance("SunX509");
		factory.init(ks, password);
//		创建并用上面的密钥管理工厂对象初始化SSL会话对象
		SSLContext context = SSLContext.getInstance("TLS");
//		第一个参数表示提供的证书，因为是服务器模式所以只需要第一个参数
//		第二个参数是用于验证对方的证书，本例不需要客户端提供证书，故使用null
		context.init(factory.getKeyManagers(), null, null);
		return context;
	}
	
	/**
	 * 服务器启动前的准备工作
	 */
	private void prepareStartServer() {
//		将服务器启动的按钮禁用
		btnStart.setEnabled(false);
		mnItemStart.setEnabled(false);
//		启用下线用户按钮
		mnItemSignoutUser.setEnabled(true);
//		启用服务器停止的按钮
		btnStop.setEnabled(true);
		mnItemStop.setEnabled(true);
		setTitle("YChat服务器 - 正在运行");
	}
	
	/**
	 * 启动服务器线程的方法
	 */
	public void startServer() {
		try {
//			初始化SSL会话
			SSLContext context = initSSLServerContext();
//			从SSL会话中获得支持安全通信的服务器Socket
			serverSocket = (SSLServerSocket)context.getServerSocketFactory().createServerSocket(PORT);
//			设置启用的算法套件为所有支持的算法套件
			serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());
//			提交EDT线程输出服务器启动消息
			EventQueue.invokeLater(()->{
				String oriText = textPaneLog.getText();
				textPaneLog.setText(new StringBuilder(oriText).append('\n').append(new Date().toString()).append('\n').append("服务器启动").toString());
			});
//			持续监听连接
			while (true) {
//				接受新连接并获得Socket
				Socket socket = serverSocket.accept();
//				创建新线程为该客户端服务，同时服务器监听线程华丽转身等待下一个客户端
				Thread serveThread = new Thread(new UserHandler(socket));
				serveThread.setDaemon(true);
				serveThread.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 停止服务器
	 */
	public void stopServer() {
		int reply = JOptionPane.showConfirmDialog(this, "服务器将停止，您确定吗？", "操作确认", JOptionPane.YES_NO_OPTION);
		if(reply == JOptionPane.OK_OPTION) {
			try {
				serverSocket.close();
				signoutAllUsers();
//				将服务器启动的按钮启用
				btnStart.setEnabled(true);
				mnItemStart.setEnabled(true);
//				禁用下线用户按钮
				mnItemSignoutUser.setEnabled(false);
//				禁用服务器停止的按钮Y
				btnStop.setEnabled(false);
				mnItemStop.setEnabled(false);
				setTitle("YChat服务器 - 已停止");
//				输出服务器启动消息
				String oriText = textPaneLog.getText();
				textPaneLog.setText(new StringBuilder(oriText).append('\n').append(new Date().toString()).append('\n').append("服务器停止").toString());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 选择用户进行注销操作
	 */
	public void selectUser() {
		YChooseUser dialog = new YChooseUser();
		dialog.addChooseListener(this);
		dialog.setVisible(true);
	}
	
	@Override
	public DefaultComboBoxModel<String> provideComboBoxModel() {
		return new DefaultComboBoxModel<>(userManager.getAllOnlineUsers());
	}

	@Override
	public void didFinishChooseUser(String choice) {
		if (choice.equals(YChooseUser.ALL)) {
			signoutAllUsers();
		} else {
			signoutUser(choice);
		}
	}
	
	public void signoutUser(String targetUser) {
//		创建服务器注销状态回复用户消息
		StateMessage message = new StateMessage();
		message.setSrcUser("");
		message.setDstUser(targetUser);
		JSONOutputStream targetUserJos = userManager.getUserJSONOutputStream(targetUser);
//		判断用户是否在线呀，不在线怎么能注销呢？
		if (userManager.isUserOnline(targetUser)) {
//			在线的情况下就可以注销啦
			message.setStatus(StateMessage.SUCCESS);
			message.setError("");
//			注销状态消息
			UserStateMessage offlineMessage = new UserStateMessage();
			offlineMessage.setSrcUser(targetUser);
			offlineMessage.setUserState(UserStateMessage.OFFLINE);
//			同样还是通知大家这个用户已经注销啦
//			获得所有在线用户
			String [] users = userManager.getAllOnlineUsers();
//			遍历在线用户数组
			for (String user : users) {
//				判断消息发送着，来源于自己的消息当然不用发给自己啦				
				if (!message.getSrcUser().equals(user)) {
//					设置消息接收者
					offlineMessage.setDstUser(user);
					JSONObject send = new JSONObject(offlineMessage);
//					获得该用户的JSON输出流
					JSONOutputStream jos = userManager.getUserJSONOutputStream(user);
					synchronized (jos) {
						try {
//							向JSON输出流写入JSON对象
							jos.writeJSONObject(send);
							jos.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else {
					continue;
				}
			}
//			再提交给EDT线程，在服务器这边输出一下
			EventQueue.invokeLater(()->{
				String oriText = textPaneLog.getText();
				textPaneLog.setText(new StringBuilder(oriText).append('\n').append(new Date().toString()).append('\n').append(targetUser+" 已注销").toString());
			});
//			从用户管理器中去掉这个用户
			userManager.removeUser(onlineUserDtm, targetUser);
		} else {
//			没登录当然是不可以注销哒
			message.setStatus(StateMessage.FAILED);
			message.setError("登录状态异常：未登录用户不可以注销");
		}
//		两种情况都要回复一下客户端的，创建JSON对象来发送
		JSONObject send = new JSONObject(message);
		synchronized (targetUserJos) {
			try {
//				向JSON输出流写入JSON对象
				targetUserJos.writeJSONObject(send);
				targetUserJos.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 下线所有用户
	 */
	public void signoutAllUsers() {
//		创建服务器注销状态回复消息
		StateMessage message = new StateMessage();
		message.setSrcUser("");
		message.setStatus(StateMessage.SUCCESS);
		message.setError("");
//		获得所有在线用户
		String [] users = userManager.getAllOnlineUsers();
//		遍历在线用户数组
		for (String user : users) {
//			设置消息接收者
			message.setDstUser(user);
			JSONObject send = new JSONObject(message);
//			获得该用户的JSON输出流
			JSONOutputStream jos = userManager.getUserJSONOutputStream(user);
			synchronized (jos) {
				try {
//					向JSON输出流写入JSON对象
					jos.writeJSONObject(send);
					jos.flush();
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		userManager.removeAllUsers(onlineUserDtm);
	}

	
	/**
	 * 功能：
	 * 该类用于处理每个客户端的连接请求，继承Runnable类；
	 * 在服务器接受新的连接时被创建一个并提交到一个线程去运行
	 * 
	 * @author ysmilec
	 *
	 */	
	public class UserHandler implements Runnable {
//		当前客户端的Socket对象
		private final Socket currentUserSocket;
//		JSON输入流和输出流，非系统默认
//		是我自己封装的InputStream和OutputStream
//		可能封装的不是很标准，仅仅是在本程序能用而已

		private JSONInputStream jis;
		private JSONOutputStream jos;
		/**
		 * 构造函数，用于初始化JSON输入输出流
		 * @param socket 一个Socket对象，是当前用户的Socket对象
		 */
		public UserHandler(Socket socket) {
			currentUserSocket = socket;
			try {
//				用当前用户Socket中的输入输出流封装JSON输入输出流
				jis = new JSONInputStream(currentUserSocket.getInputStream());
				jos = new JSONOutputStream(currentUserSocket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		@Override
		public void run() {
			try {
				while(true) {
//					从JSON输入流中读取JSON对象
					JSONObject receive = jis.readJSONObject();
					AbstractMessage msg = null;
//					判断接收到的消息，由JSON对象反序列化为消息对象
//					这里在反序列化的时候仅仅处理了消息对象，并没有考虑任何其他情况
//					所以可能不是那么太好用
					if ((msg = AbstractMessage.fromJSONObject(receive, ChatMessage.class) )!=null) {
//						处理聊天消息
						processChatMessage((ChatMessage)msg);
					} else if ((msg = AbstractMessage.fromJSONObject(receive, FileTransferMessage.class) )!=null) {
//						处理文件传输消息
						processFileTransferMessage((FileTransferMessage)msg);
					} else if ((msg = AbstractMessage.fromJSONObject(receive, SignupMessage.class) )!=null) {
//						处理注册消息
						processSignupMessage((SignupMessage)msg);
					} else if ((msg = AbstractMessage.fromJSONObject(receive, SigninMessage.class) )!=null) {
//						处理登录消息
						processSigninMessage((SigninMessage)msg);
					} else if ((msg = AbstractMessage.fromJSONObject(receive, UserStateMessage.class) )!=null) {
//						处理用户状态消息
						processUserStateMessage((UserStateMessage)msg);
					} else if ((msg = AbstractMessage.fromJSONObject(receive, ChangPassMessage.class) )!=null) {
						processChangePasswd((ChangPassMessage)msg);
					} else if ((msg = AbstractMessage.fromJSONObject(receive, SignoutMessage.class) )!=null) {
//						处理注销消息，因为注销消息未作任何扩展，所以不能放在前面判断，会误认为所有消息都是注销消息
//						当然这里还有待改进
						processSignoutMessage((SignoutMessage)msg);
					}else {
//						如果不是任何一种消息，则提交EDT线程显示错误信息
						EventQueue.invokeLater(()->{
							String oriText = textPaneLog.getText();
							textPaneLog.setText(new StringBuilder(oriText).append('\n').append(new Date().toString()).append('\n').append("接收到无法解析的JSON对象："+receive.toString()).toString());
						});
					}
				}
//				出现异常之后需要关闭Socket
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				if (currentUserSocket != null) {
					try {
						currentUserSocket.close();
					} catch (IOException e2) {
						e2.printStackTrace();
					}
				}
			}
		}
		
		/**
		 * 处理聊天消息
		 * @param msg 聊天消息对象
		 */
		private void processChatMessage(ChatMessage msg) {
//			取得发送方和接收方，以及消息正文
			String srcUser = msg.getSrcUser();
			String dstUser = msg.getDstUser();
			String msgContent = msg.getMsgContent();
			if (dstUser.equals("")) {
//				如果接收方为空，则代表是公聊消息
				EventQueue.invokeLater(()->{
					String oriText = textPaneLog.getText();
					textPaneLog.setText(new StringBuilder(oriText).append('\n').append(new Date().toString()).append('\n').append("转发 "+srcUser+" 发送的公聊消息："+msgContent).toString());
				});
//				将该消息转发给所有其他在线客户端
				transferMsgToOtherUsers(msg);
			} else {
//				否则是私聊消息，仅转发给消息的接收者
				EventQueue.invokeLater(()->{
					String oriText = textPaneLog.getText();
					textPaneLog.setText(new StringBuilder(oriText).append('\n').append(new Date().toString()).append('\n').append("转发 "+srcUser+" 发给 "+dstUser+" 的私聊消息："+msgContent).toString());
				});
//				获得目的用户的JSON输出流
				JSONOutputStream jos = userManager.getUserJSONOutputStream(dstUser);
				synchronized (jos) {
					try {
						JSONObject send = new JSONObject(msg);
//						写入JSON对象到JSON输出流
						jos.writeJSONObject(send);
						jos.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		/**
		 * 处理登录消息
		 * @param msg 登录消息对象
		 */
		private void processSigninMessage(SigninMessage msg) {
//			登录状态标志
			int flag = 0;
//			获得要登录的用户以及登录密码
			String srcUser = msg.getSrcUser();
			String password = msg.getPassword();
//			创建数据库管理器
			DataBaseManager dbManager = new DataBaseManager(properties.getProperty("DatabaseDriver"),
					properties.getProperty("DatabaseHost"),
					properties.getProperty("DatabaseUsername"),
					properties.getProperty("DatabasePassword").toCharArray());
//			创建服务器登录状态回复消息
			StateMessage message = new StateMessage();
			message.setSrcUser("");
			message.setDstUser(srcUser);
			try {
//				连接到数据库
				dbManager.connect();
//				执行数据库登录操作，并获得结果
				if (dbManager.signin(srcUser, password)) {
					if (userManager.isUserOnline(srcUser)) {
//						如果用户已经登录则返回错误信息-1表示这个用户在线
						message.setStatus(StateMessage.FAILED);
						message.setError("-1");
					}else {
//						数据库回应称登录成功
//						设置返回消息为成功状态
						message.setStatus(StateMessage.SUCCESS);
						message.setError("");
//						创建用户登录消息
						UserStateMessage onlineMessage = new UserStateMessage();
						onlineMessage.setSrcUser(srcUser);
						onlineMessage.setDstUser("");
						onlineMessage.setUserState(UserStateMessage.ONLINE);
//						如果不是隐身方式登录，则将该用户登录的消息通知给所有其他客户端
						if (msg.isSecretSignin() == false) {
							transferMsgToOtherUsers(onlineMessage);
						}
//						提交EDT线程提示用户已登录
						EventQueue.invokeLater(()->{
							String oriText = textPaneLog.getText();
							textPaneLog.setText(new StringBuilder(oriText).append('\n').append(new Date().toString()).append('\n').append(srcUser+" 已登录").toString());
						});
//						将用户添加到用户管理器中
						userManager.addUser(onlineUserDtm, srcUser, currentUserSocket, jis, jos, msg.isSecretSignin());
//						设置登录成功标志位
						flag = 1;
					}		
				} else {
//					数据库回应称登录失败
					message.setStatus(StateMessage.FAILED);
					message.setError("用户名或密码错误，或数据库错误");
				}
			} catch (ClassNotFoundException | SQLException e) {
//				出现异常，登录失败
				message.setStatus(StateMessage.FAILED);
				message.setError(e.getLocalizedMessage());
				e.printStackTrace();
			}
//			两种情况都要回复一下客户端的，创建JSON对象来发送
			JSONObject send = new JSONObject(message);
			synchronized (jos) {
				try {
//					向JSON输出流写入JSON对象
					jos.writeJSONObject(send);
					jos.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
//			要先向用户发送登录成功消息，再发送在线用户列表，不然会出问题的哦
			if (flag == 1) {
				UserStateMessage onlineListMessage = new UserStateMessage();
				onlineListMessage.setDstUser(srcUser);
				onlineListMessage.setUserState(UserStateMessage.ONLINE);
//				向该登录用户发送当前的所有在线用户
				sendOnlineUserList(onlineListMessage);
			}
		}
		
		/**
		 * 处理注销消息
		 * @param msg 注销消息对象
		 */
		private void processSignoutMessage(SignoutMessage msg) {
			String srcUser = msg.getSrcUser();
//			创建服务器注销状态回复消息
			StateMessage message = new StateMessage();
			message.setSrcUser("");
			message.setDstUser(srcUser);
//			判断用户是否在线呀，不在线怎么能注销呢？
			if (userManager.isUserOnline(srcUser)) {
//				在线的情况下就可以注销啦
				message.setStatus(StateMessage.SUCCESS);
				message.setError("");
//				注销状态消息
				UserStateMessage offlineMessage = new UserStateMessage();
				offlineMessage.setSrcUser(srcUser);
				offlineMessage.setDstUser("");
				offlineMessage.setUserState(UserStateMessage.OFFLINE);
//				同样还是通知大家这个用户已经注销啦
				transferMsgToOtherUsers(offlineMessage);
//				再提交给EDT线程，在服务器这边输出一下
				EventQueue.invokeLater(()->{
					String oriText = textPaneLog.getText();
					textPaneLog.setText(new StringBuilder(oriText).append('\n').append(new Date().toString()).append('\n').append(srcUser+" 已注销").toString());
				});
//				从用户管理器中去掉这个用户
				userManager.removeUser(onlineUserDtm, srcUser);
			} else {
//				没登录当然是不可以注销哒
				message.setStatus(StateMessage.FAILED);
				message.setError("登录状态异常：未登录用户不可以注销");
			}
//			两种情况都要回复一下客户端的，创建JSON对象来发送
			JSONObject send = new JSONObject(message);
			synchronized (jos) {
				try {
//					向JSON输出流写入JSON对象
					jos.writeJSONObject(send);
					jos.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		/**
		 * 处理注册消息
		 * @param msg 注册消息对象
		 */
		private void processSignupMessage(SignupMessage msg) {
//			读取消息内容
			String srcUser = msg.getSrcUser();
			String name = msg.getName();
			String password = msg.getPassword();
//			创建服务器注册状态回复消息
			StateMessage message = new StateMessage();
			message.setSrcUser("");
			message.setDstUser(srcUser);
//			创建数据库管理器
			DataBaseManager dbManager = new DataBaseManager(properties.getProperty("DatabaseDriver"),
					properties.getProperty("DatabaseHost"),
					properties.getProperty("DatabaseUsername"),
					properties.getProperty("DatabasePassword").toCharArray());
			try {
//				连接到数据库
				dbManager.connect();
//				执行数据库注册操作，并获得结果
				if (dbManager.signup(srcUser, name, password)) {
//					数据库回应称注册成功
					message.setStatus(StateMessage.SUCCESS);
					message.setError("");
//					提交EDT线程输出注册成功
					EventQueue.invokeLater(()->{
						String oriText = textPaneLog.getText();
						textPaneLog.setText(new StringBuilder(oriText).append('\n').append(new Date().toString()).append('\n').append(srcUser+" 已注册").toString());
					});
				} else {
//					数据库回应称注册失败
					message.setStatus(StateMessage.FAILED);
					message.setError("无法注册");
				}
			} catch (ClassNotFoundException | SQLException e) {
//				因为服务器异常导致注册失败
				message.setStatus(StateMessage.FAILED);
				message.setError("无法注册");
				e.printStackTrace();
			}
//			要回复一下客户端的，创建JSON对象来发送
			JSONObject send = new JSONObject(message);
			synchronized (jos) {
				try {
//					向JSON输出流写入JSON对象
					jos.writeJSONObject(send);
					jos.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		/**
		 * 处理文件传输消息
		 * @param msg 文件传输消息对象
		 */
		private void processFileTransferMessage(FileTransferMessage msg) {
			String srcUser = msg.getSrcUser();
			String dstUser = msg.getDstUser();
			if (msg.getStatus()==FileTransferMessage.ACCEPT) {
//				接收到用户接收文件消息，则添加接收方的IP地址到消息中
				msg.setHost(userManager.getUserSocket(srcUser).getInetAddress().getHostAddress());
			}
//			刚开始用户的msg类型为REQUEST，则服务器将 转发给目的用户
			JSONOutputStream jos = userManager.getUserJSONOutputStream(dstUser);
//			两种情况都要回复一下客户端的，创建JSON对象来发送
			JSONObject send = new JSONObject(msg);
			synchronized (jos) {
				try {
//					向JSON输出流写入JSON对象
					jos.writeJSONObject(send);
					jos.flush();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		/**
		 * 处理修改密码消息
		 * @param msg 修改密码消息对象
		 */
		private void processChangePasswd(ChangPassMessage msg) {
			int flag = 0;
//			读取消息内容
			String srcUser = msg.getSrcUser();
			String newPassword = msg.getNewpasswd();
			String oldPassword = msg.getOldpaswd();
//			创建服务器修改密码状态回复消息
			StateMessage message = new StateMessage();
			message.setSrcUser("");
			message.setDstUser(srcUser);
//			创建数据库管理器
			DataBaseManager dbManager = new DataBaseManager(properties.getProperty("DatabaseDriver"),
					properties.getProperty("DatabaseHost"),
					properties.getProperty("DatabaseUsername"),
					properties.getProperty("DatabasePassword").toCharArray());
			try {
//				连接到数据库
				dbManager.connect();
//				执行数据库修改密码操作，并获得结果
				if (dbManager.changePassword(srcUser, oldPassword, newPassword)) {
					flag = 1;
//					数据库回应称修改密码成功
					message.setStatus(StateMessage.SUCCSEE_PASS);
					message.setError("");
//					提交EDT线程输出修改密码成功
					EventQueue.invokeLater(()->{
						String oriText = textPaneLog.getText();
						textPaneLog.setText(new StringBuilder(oriText).append('\n').append(new Date().toString()).append('\n').append(srcUser+" 已修改密码").toString());
					});

				} else {
//					数据库回应称修改密码失败
					message.setStatus(StateMessage.FAILED);
					message.setError("无法修改密码");
				}
			} catch (ClassNotFoundException | SQLException e) {
//				因为服务器异常导致修改密码失败
				message.setStatus(StateMessage.FAILED);
				message.setError("无法修改密码");
				e.printStackTrace();
			}
//			要回复一下客户端的，创建JSON对象来发送
			JSONObject send = new JSONObject(message);
			synchronized (jos) {
				try {
//					向JSON输出流写入JSON对象
					jos.writeJSONObject(send);
					jos.flush();
//					flag = 1说明该用户修改密码成功,将该用户下线并通知其他用户
					if (flag == 1) {
//						注销状态消息
						UserStateMessage offlineMessage = new UserStateMessage();
						offlineMessage.setSrcUser(srcUser);
						offlineMessage.setDstUser("");
						offlineMessage.setUserState(UserStateMessage.OFFLINE);
//						通知大家这个用户已经注销啦
						transferMsgToOtherUsers(offlineMessage);
//						修改密码成功后将该用户下线
						userManager.removeUser(onlineUserDtm, srcUser);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		/**
		 * 处理用户状态信息
		 * 判断用户切换的状态并进行相应的处理
		 * @param msg
		 */
		private void processUserStateMessage(UserStateMessage msg) {
			String srcUser = msg.getSrcUser();
			if (msg.getUserState() == UserStateMessage.OFFLINE) {
				return;
			} else if(msg.getUserState() == UserStateMessage.ONLINE) {
				if (userManager.isUserOnline(srcUser)) {
//					创建用户登录消息
					UserStateMessage onlineMessage = new UserStateMessage();
					onlineMessage.setSrcUser(srcUser);
					onlineMessage.setDstUser("");
					onlineMessage.setUserState(UserStateMessage.ONLINE);
//					将该用户登录的消息通知给所有其他客户端
					transferMsgToOtherUsers(onlineMessage);
				}
			} else if(msg.getUserState() == UserStateMessage.STEALTH) {
				if (userManager.isUserOnline(srcUser)) {
//					注销状态消息
					UserStateMessage offlineMessage = new UserStateMessage();
					offlineMessage.setSrcUser(srcUser);
					offlineMessage.setDstUser("");
					offlineMessage.setUserState(UserStateMessage.OFFLINE);
//					同样还是通知大家这个用户已经注销啦
					transferMsgToOtherUsers(offlineMessage);
				}
			}
		}
		
		/**
		 * 发送在线用户列表
		 * @param onlineListMessage 在线用户列表消息对象
		 */
		private void sendOnlineUserList(UserStateMessage onlineListMessage) {
//			从用户管理器中获得所有在线用户
			String[] users = userManager.getAllOnlineUsers();
//			遍历在线用户数组
			for (String user : users) {
				onlineListMessage.setSrcUser(user);
//				每个用户发送一次消息
				JSONObject send = new JSONObject(onlineListMessage);
				synchronized (jos) {
					try {
//						向JSON输出流写入JSON对象
						jos.writeJSONObject(send);
						jos.flush();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		/**
		 * 转发消息给其他所有在线用户
		 * @param msg 待转发的消息对象
		 */
		private void transferMsgToOtherUsers(AbstractMessage msg) {
//			获得所有在线用户
			String [] users = userManager.getAllOnlineUsers();
//			遍历在线用户数组
			for (String user : users) {
//				判断消息发送着，来源于自己的消息当然不用发给自己啦			
				if (!msg.getSrcUser().equals(user)) {
					JSONObject send = new JSONObject(msg);
//					获得该用户的JSON输出流
					JSONOutputStream jos = userManager.getUserJSONOutputStream(user);
					synchronized (jos) {
						try {
//							向JSON输出流写入JSON对象
							jos.writeJSONObject(send);
							jos.flush();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				} else {
					continue;
				}
			}
		}

		
	}// UserHandler end
}// YChatServer end


