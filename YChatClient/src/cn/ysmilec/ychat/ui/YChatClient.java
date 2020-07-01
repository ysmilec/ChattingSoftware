package cn.ysmilec.ychat.ui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;   
import javax.swing.JScrollPane;
import javax.swing.JButton;
import javax.swing.JTextPane;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.Box;
import javax.swing.JSplitPane;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import java.awt.event.ActionListener;
import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.awt.event.ActionEvent;

import org.json.JSONObject;

import cn.ysmilec.ychat.io.JSONInputStream;
import cn.ysmilec.ychat.io.JSONOutputStream;
import cn.ysmilec.ychat.msg.AbstractMessage;
import cn.ysmilec.ychat.msg.ChatMessage;
import cn.ysmilec.ychat.msg.FileTransferMessage;
import cn.ysmilec.ychat.msg.SignoutMessage;
import cn.ysmilec.ychat.msg.StateMessage;
import cn.ysmilec.ychat.msg.UserStateMessage;

import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

/**
 * 功能：
 * 客户端界面类，继承自JFrame类
 * 
 * @author ysmilec
 *
 */
public class YChatClient extends JFrame {
	
	private static final long serialVersionUID = 1957423647937591964L;
//	JSON输入输出流
	private JSONInputStream jis;
	private JSONOutputStream jos;
//	用户名和密码
	private String localUsername,localPassword;
//	用户状态
	private int userState = UserStateMessage.OFFLINE;
//	待发送的文件
	private File fileToSend;
//	在线用户列表的Model
	private DefaultListModel<String> onlineUserListModel = new DefaultListModel<>();
//	在线用户选择框的Model
	private DefaultComboBoxModel<String> onlineUserBoxModel = new DefaultComboBoxModel<>();
//	主页面Panel，界面元素
	private JPanel contentPane;
//	BorderLayout的三个Panel，界面元素
	private JPanel centerPanel,southPanel;
//	消息记录区，界面元素
	private JTextPane textPaneMsgRecord;
//	消息发送框，界面元素
	private JTextField textFieldMsgToSend;
	private JSplitPane splitPane;
//	发消息按钮，界面元素
	private JButton btnSendMsg;
//	传文件按钮，界面元素
	private JButton btnTransfer;
//	用户名选择下拉框，界面元素
	private JComboBox<String> comboBox;
	private Component horizontalGlue;
	private JMenuBar menuBar;
	private JMenu menuUser;
	private JMenu menuYChat;
	private JMenuItem mnItemAbout;
	private JMenuItem mnItemExit;
	private JMenu menuFeatures;
	private JMenuItem mnItemSend;
	private JMenuItem mnItemTransfer;
	private JMenu menuAss;
	private JMenuItem mnItemClearMsgRecord;
	private JMenu menuStatus;
	private JMenuItem mnItemOnline;
	private JMenuItem mnItemStealth;
	private JMenuItem mnItemOffline;
	private JMenuItem mnItemClearMsgSend;
	private JMenuItem mnItemChPassword;
	private JMenuItem mnItemLogout;
	

	/**
	 * 带参数的构造方法，创建窗体
	 */
	public YChatClient(String username, String passwd, int userState, JSONInputStream jInputStream, JSONOutputStream jOutputStream) {
		this.localUsername = username;
		this.localPassword = passwd;
		this.userState = userState;
		this.jis = jInputStream;
		this.jos = jOutputStream;
		init();		
		Thread userThread = new Thread(new ListeningHandler());
		userThread.setDaemon(true);
		userThread.start();
	}
	
	public void init() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 600, 400);
		
		menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		menuYChat = new JMenu("YChat");
		menuBar.add(menuYChat);
		
		mnItemAbout = new JMenuItem("关于YChat客户端");
		mnItemAbout.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JOptionPane.showMessageDialog(null, "YChat客户端\n作者：ysmilec\n许可证：GPLv3", "关于YChat客户端", JOptionPane.INFORMATION_MESSAGE);
			}
		});
		menuYChat.add(mnItemAbout);
		
		mnItemExit = new JMenuItem("退出YChat客户端");
		mnItemExit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
		});
		menuYChat.add(mnItemExit);
		
		menuUser = new JMenu("用户");
		menuBar.add(menuUser);
		
		mnItemLogout = new JMenuItem("注销");
		mnItemLogout.addActionListener(new ActionListener() {			
			@Override
			public void actionPerformed(ActionEvent e) {
				logout();		
			}
		});
		menuUser.add(mnItemLogout);
		
		mnItemChPassword = new JMenuItem("修改密码");
		mnItemChPassword.addActionListener(new ActionListener() {	
			@Override
			public void actionPerformed(ActionEvent arg0) {
				YChatChangePasswd yChangePasswd = new YChatChangePasswd(localUsername, localPassword, jos);
				yChangePasswd.setVisible(true);
			}
		});
		menuUser.add(mnItemChPassword);
		
		menuFeatures = new JMenu("功能");
		menuBar.add(menuFeatures);
		
		mnItemSend = new JMenuItem("发送消息");
		mnItemSend.setIcon(new ImageIcon(getClass().getResource("/cn/ysmilec/ychat/images/message.png")));
		mnItemSend.setEnabled(true);
		mnItemSend.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedUser = (String) comboBox.getSelectedItem();
				if (selectedUser.equals(localUsername)) {
//					不发送给自己
					JOptionPane.showMessageDialog(null,"不能发送给自己！");
					return;
				}
				if (selectedUser.equals("公开")) {
//					公聊消息接收者为空
					selectedUser = "";
				}
				sendMessage(selectedUser);
			}
		});
		menuFeatures.add(mnItemSend);
		
		mnItemTransfer = new JMenuItem("文件传输");
		mnItemTransfer.setIcon(new ImageIcon(getClass().getResource("/cn/ysmilec/ychat/images/file.png")));
		mnItemTransfer.setEnabled(true);
		mnItemTransfer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedUser = (String) comboBox.getSelectedItem();
				if (selectedUser.equals(localUsername) || selectedUser.equals("公开")) {
//					文件不发送给自己和服务器
					return;
				}
				sendFileRequest(selectedUser);
			}
		});
		menuFeatures.add(mnItemTransfer);
		
		menuStatus = new JMenu("状态");
		menuBar.add(menuStatus);
		
		mnItemOnline = new JMenuItem("在线");
		mnItemOnline.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(userState == UserStateMessage.ONLINE) {
					return;
				} else if(userState == UserStateMessage.OFFLINE) {
					dispose();
//					重新创建客户端窗口
					new YChatLogin().setVisible(true);
				} else if(userState == UserStateMessage.STEALTH) {
					sendOnlineMessage();
				}
			}
		});
		menuStatus.add(mnItemOnline);
		
		mnItemStealth = new JMenuItem("隐身");
		mnItemStealth.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(userState == UserStateMessage.STEALTH) {
					return;
				} else if(userState == UserStateMessage.OFFLINE) {
					dispose();
//					重新创建登录端窗口
					new YChatLogin().setVisible(true);
				} else if(userState == UserStateMessage.ONLINE) {
					sendStealthMessage();
				}
			}
		});
		menuStatus.add(mnItemStealth);
		
		mnItemOffline = new JMenuItem("离线");
		mnItemOffline.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(userState == UserStateMessage.OFFLINE) {
					return;
				} else if(userState == UserStateMessage.ONLINE || userState == UserStateMessage.STEALTH) {
					logout();
				}
			}
		});
		menuStatus.add(mnItemOffline);
		menuAss = new JMenu("辅助");
		menuBar.add(menuAss);
		
		mnItemClearMsgSend = new JMenuItem("清空发送框");
		mnItemClearMsgSend.setEnabled(true);
		mnItemClearMsgSend.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textFieldMsgToSend.setText("");
			}
		});
		menuAss.add(mnItemClearMsgSend);
		
		mnItemClearMsgRecord = new JMenuItem("清空消息列表");
		mnItemClearMsgRecord.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				textPaneMsgRecord.setText("");
			}
		});
		menuAss.add(mnItemClearMsgRecord);
		centerPanel = new JPanel();
		southPanel = new JPanel();
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.X_AXIS));
		
		splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.8);
		centerPanel.add(splitPane);
		
		textPaneMsgRecord = new JTextPane();
		textPaneMsgRecord.setEditable(false);
		textPaneMsgRecord.setBorder(new TitledBorder(null, "消息列表", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		textPaneMsgRecord.setBounds(16, 44, 232, 186);
		JScrollPane scrollPane_1 = new JScrollPane(textPaneMsgRecord);
		splitPane.setLeftComponent(scrollPane_1);
		
		JList<String> listOnlineUsers = new JList<>(onlineUserListModel);
		listOnlineUsers.setBorder(new TitledBorder(null, "在线用户列表", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		listOnlineUsers.setBounds(260, 44, 219, 186);
		JScrollPane scrollPane_2 = new JScrollPane(listOnlineUsers);
		splitPane.setRightComponent(scrollPane_2);
		southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.X_AXIS));
		
		comboBox = new JComboBox<>();
		comboBox.setEnabled(true);
		comboBox.setMaximumRowCount(100);
		comboBox.setModel(onlineUserBoxModel);
//		添加公聊选择
		onlineUserBoxModel.addElement("公开");	
		southPanel.add(comboBox);
		
		textFieldMsgToSend = new JTextField();
		textFieldMsgToSend.setEnabled(true);
		textFieldMsgToSend.setBounds(6, 246, 303, 26);
		southPanel.add(textFieldMsgToSend);
		textFieldMsgToSend.setColumns(20);
		
		btnSendMsg = new JButton("发消息");
//		发送消息的时候获得消息接收者
		btnSendMsg.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedUser = (String) comboBox.getSelectedItem();
				if (selectedUser.equals(localUsername)) {
//					不发送给自己
					JOptionPane.showMessageDialog(null,"不能发送给自己！");
					return;
				}
				if (selectedUser.equals("公开")) {
//					公聊消息接收者为空
					selectedUser = "";
				}
				sendMessage(selectedUser);
			}
		});
		
		horizontalGlue = Box.createHorizontalGlue();
		southPanel.add(horizontalGlue);
		btnSendMsg.setEnabled(true);
		btnSendMsg.setBounds(310, 246, 91, 29);
		southPanel.add(btnSendMsg);
		
		btnTransfer = new JButton("传文件");
		btnTransfer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String selectedUser = (String) comboBox.getSelectedItem();
				if (selectedUser.equals(localUsername) || selectedUser.equals("公开")) {
//					文件不发送给自己和服务器
					return;
				}
				sendFileRequest(selectedUser);
			}
		});
		btnTransfer.setEnabled(true);
		btnTransfer.setBounds(403, 246, 91, 29);
		southPanel.add(btnTransfer);
		contentPane.add(centerPanel, BorderLayout.CENTER);
		contentPane.add(southPanel, BorderLayout.SOUTH);
	}
	

	/**
	 * 注销方法
	 */
	public void logout() {
//		提示用户注销
		int reply = JOptionPane.showConfirmDialog(this, "是否真的注销？", "退出确认", JOptionPane.YES_NO_OPTION);
		if (reply == JOptionPane.OK_OPTION) {
//			调用无需确认的注销方法
			logoutWithoutConfirm();
		}
	}
	
	/**
	 * 在退出程序的时候自动执行注销，此操作不需要用户确认
	 */
	private void logoutWithoutConfirm() {
//		创建注销消息
		SignoutMessage message = new SignoutMessage();
		message.setSrcUser(localUsername);
		message.setDstUser("");
//		使用注销消息创建JSON对象
		JSONObject send = new JSONObject(message);
//		开启新线程发送注销消息
		new Thread(()->{
			synchronized (jos) {
				try {
//					向JSON输出流写入JSON对象
					jos.writeJSONObject(send);
					jos.flush();
				} catch (IOException e) {
					EventQueue.invokeLater(()->{
						JOptionPane.showMessageDialog(this, "网络连接失败，无法连接到服务器");
						e.printStackTrace();
					});
				}
			}
		}).start();
	}
	
	/**
	 * 发送文本消息的方法
	 * @param dstUser 目的用户
	 */
	public void sendMessage(String dstUser) {
//		获得用户输入的文本，去除结尾的空格
		String msgContent = textFieldMsgToSend.getText().trim();
		if (msgContent.length() > 0) {
//			创建聊天消息
			ChatMessage chatMessage = new ChatMessage();
			chatMessage.setSrcUser(localUsername);
			chatMessage.setDstUser(dstUser);
			chatMessage.setMsgContent(msgContent);
//			使用聊天消息创建JSON对象
			JSONObject send = new JSONObject(chatMessage);
//			开启新线程发送聊天消息
			new Thread(()->{
				synchronized (jos) {
					try {
//						向JSON输出流写入JSON对象
						jos.writeJSONObject(send);
						jos.flush();
//						提交EDT线程清空消息输入框
						EventQueue.invokeLater(()->{
							textFieldMsgToSend.setText("");
						});
					} catch (IOException e) {
						EventQueue.invokeLater(()->{
							JOptionPane.showMessageDialog(this, "网络连接失败，无法连接到服务器");
							e.printStackTrace();
						});
					}
				}
			}).start();
		}
	}
	
	/**
	 * 发送传送文件请求
	 * @param dstUser 目标用户
	 */
	public void sendFileRequest(String dstUser) {
//		创建文件选择窗口
		JFileChooser fileChooser = new JFileChooser();
//		显示文件打开窗口
		int reply = fileChooser.showOpenDialog(this);
		if (reply == JFileChooser.APPROVE_OPTION) {
//			如果用户点击确定按钮，则获得用户选择的文件
			fileToSend = fileChooser.getSelectedFile();
//			创建文件传输消息
			FileTransferMessage transferMessage = new FileTransferMessage();
			transferMessage.setSrcUser(localUsername);
			transferMessage.setDstUser(dstUser);
//			设置为文件传输请求消息
			transferMessage.setStatus(FileTransferMessage.REQUEST);
//			设置文件名和文件大小
			transferMessage.setFileName(fileToSend.getName());
			transferMessage.setFileSize(fileToSend.length());
//			文件传输请求此部分无效
			transferMessage.setHost("");
			transferMessage.setPort(-1);
//			使用文件传输消息创建JSON对象
			JSONObject send = new JSONObject(transferMessage);
//			开启新线程发送文件传输消息
			new Thread(()->{
				synchronized (jos) {
					try {
//						向JSON输出流写入JSON对象
						jos.writeJSONObject(send);
						jos.flush();
//						提交EDT线程提示用户文件等待接收
						EventQueue.invokeLater(()->{
							System.out.println("发送文件等待接收："+fileToSend.getName());
							String oriText = textPaneMsgRecord.getText();
							textPaneMsgRecord.setText(new StringBuilder(oriText).append('\n').append("发送文件等待接收："+fileToSend.getName()).toString());
						});
					} catch (IOException e) {
						EventQueue.invokeLater(()->{
							JOptionPane.showMessageDialog(this, "网络连接失败，无法连接到服务器");
							e.printStackTrace();
						});
					}
				}
			}).start();
		}
	}
	
	/**
	 * 发送上线消息，仅用于从隐身状态切换为在线状态
	 * 如果是离线状态，请发送登录消息
	 */
	public void sendOnlineMessage() {
		if (userState != UserStateMessage.STEALTH) {
			return;
		}
		UserStateMessage message = new UserStateMessage();
		message.setSrcUser(localUsername);
		message.setDstUser("");
		message.setUserState(UserStateMessage.ONLINE);
		JSONObject send = new JSONObject(message);
//		开启新线程发送消息
		new Thread(()->{
			synchronized (jos) {
				try {
//					向JSON输出流写入JSON对象
					jos.writeJSONObject(send);
					jos.flush();
					userState = UserStateMessage.ONLINE;
//					checkBoxSecretLogin.setSelected(false);
					setTitle("YChat客户端 - "+localUsername+" - 在线");
				} catch (IOException e) {
					EventQueue.invokeLater(()->{
						JOptionPane.showMessageDialog(this, "网络连接失败，无法连接到服务器");
						e.printStackTrace();
					});
				}
			}
		}).start();
	}
	
	/**
	 * 发送隐身消息，仅用于从在线状态切换为隐身状态
	 * 如果是离线状态，请发送登录消息
	 */
	public void sendStealthMessage() {
		if (userState != UserStateMessage.ONLINE) {
			return;
		}
		UserStateMessage message = new UserStateMessage();
		message.setSrcUser(localUsername);
		message.setDstUser("");
		message.setUserState(UserStateMessage.STEALTH);
		JSONObject send = new JSONObject(message);
//		开启新线程发送消息
		new Thread(()->{
			synchronized (jos) {
				try {
//					向JSON输出流写入JSON对象
					jos.writeJSONObject(send);
					jos.flush();
					userState = UserStateMessage.STEALTH;
//					checkBoxSecretLogin.setSelected(true);
					setTitle("YChat客户端 - "+localUsername+" - 隐身");
				} catch (IOException e) {
					EventQueue.invokeLater(()->{
						JOptionPane.showMessageDialog(this, "网络连接失败，无法连接到服务器");
						e.printStackTrace();
					});
				}
			}
		}).start();
	}
	
	/**
	 * 客户端监听类
	 * @author ysmilec 实现了Runnable
	 *
	 */
	class ListeningHandler implements Runnable{
		@Override
		public void run() {
			try {
//				持续监听消息
				while(true) {
//					读取JSON对象
					JSONObject receive = jis.readJSONObject();
//					反序列化消息
					AbstractMessage msg = null;
					if ((msg = AbstractMessage.fromJSONObject(receive, StateMessage.class) )!=null) {
//						处理状态消息
						processStateMessage((StateMessage)msg);
					} else if ((msg = AbstractMessage.fromJSONObject(receive, FileTransferMessage.class) )!=null) {
//						处理文件传输消息
						processFileTransferMessage((FileTransferMessage)msg);
					} else if ((msg = AbstractMessage.fromJSONObject(receive, UserStateMessage.class) )!=null) {
//						处理用户状态消息
						processUserStateMessage((UserStateMessage)msg);
					} else if ((msg = AbstractMessage.fromJSONObject(receive, ChatMessage.class) )!=null) {
//						处理聊天消息
						processChatMessage((ChatMessage)msg);
					} else {
						System.out.println("接收到无法解析的JSON对象："+receive.toString());
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
		}
		
		/**
		 * 处理状态消息
		 * @param msg 状态消息对象
		 */
		private void processStateMessage(StateMessage msg) {
			if(msg.getStatus()==StateMessage.SUCCESS) {
//				如果是成功状态，说明注销成功
				EventQueue.invokeLater(()->{
					JOptionPane.showMessageDialog(null, "您已经注销");
					dispose();
					new YChatLogin().setVisible(true);
				});
			} if (msg.getStatus()==StateMessage.SUCCSEE_PASS) {
//				说明修改密码成功
				EventQueue.invokeLater(()->{
					JOptionPane.showMessageDialog(null, "修改密码成功");
					dispose();
					new YChatLogin().setVisible(true);
				});
			}else {
				EventQueue.invokeLater(()->{
					JOptionPane.showMessageDialog(null, "状态消息异常");
				});
			}
		}
		
		/**
		 * 处理文件传输消息
		 * @param msg 文件传输消息对象
		 */
		private void processFileTransferMessage(FileTransferMessage msg) {
			if(msg.getStatus()==FileTransferMessage.REQUEST) {
//				如果接收到的消息是文件传输请求
//				这时候该客户端是文件接收者
				EventQueue.invokeLater(()->{
//					取出文件信息
					String srcUser = msg.getSrcUser();
					String fileName = msg.getFileName();
					double fileSize = msg.getFileSize();
//					创建文件传输消息
					FileTransferMessage transferMessage = new FileTransferMessage();
					transferMessage.setSrcUser(localUsername);
					transferMessage.setDstUser(srcUser);
					transferMessage.setFileName("");
					transferMessage.setFileSize(-1);
					transferMessage.setHost("");
//					弹窗询问用户是否接收文件
					int reply = JOptionPane.showConfirmDialog(null, new StringBuilder(srcUser).append(" 想要给您发送文件\n文件名：").append(fileName).append("\n大小：").append(fileSize), "文件发送请求", JOptionPane.YES_NO_OPTION);
					if (reply == JOptionPane.OK_OPTION) {
//						如果用户选择接受，则创建一个文件选择窗口
						JFileChooser fileChooser = new JFileChooser();
//						显示文件保存窗口
						int saveReply = fileChooser.showSaveDialog(null);
						if (saveReply == JFileChooser.APPROVE_OPTION) {
//							用户点击保存后获得用户选择的保存文件路径
							File selectedFile = fileChooser.getSelectedFile();
							System.out.println("确认接收文件，开始传输");
							String oriText = textPaneMsgRecord.getText();
							textPaneMsgRecord.setText(new StringBuilder(oriText).append('\n').append("确认接收文件，开始传输").toString());
//							创建文件传输窗口
							YCFileTransfer receiveFileDialog = new YCFileTransfer(YCFileTransfer.RECEIVE);
							receiveFileDialog.setVisible(true);
//							启动接收文件并获得监听的端口
							int transPort = receiveFileDialog.processReceiveFile(selectedFile.getAbsolutePath(), fileSize);
//							设置文件传输消息为接受文件
							transferMessage.setStatus(FileTransferMessage.ACCEPT);
//							通告文件接收所用的端口
							transferMessage.setPort(transPort);
						} else {
//							虽然选择接收文件，但是又在保存的时候点击取消，也相当于拒绝了
//							设置文件传输消息为拒绝文件
							transferMessage.setStatus(FileTransferMessage.REJECT);
							transferMessage.setPort(-1);
							System.out.println("拒绝接收文件");
							String oriText = textPaneMsgRecord.getText();
							textPaneMsgRecord.setText(new StringBuilder(oriText).append('\n').append("拒绝接收文件").toString());
						}
					} else {
//						直接拒绝了文件
//						设置文件传输消息为拒绝文件
						transferMessage.setStatus(FileTransferMessage.REJECT);
						transferMessage.setPort(-1);
						System.out.println("拒绝接收文件");
						String oriText = textPaneMsgRecord.getText();
						textPaneMsgRecord.setText(new StringBuilder(oriText).append('\n').append("拒绝接收文件").toString());
					}
//					用文件传输消息创建JSON对象
					JSONObject send = new JSONObject(transferMessage);
//					启动新线程发送文件传输消息
					new Thread(()->{
						synchronized (jos) {
							try {
//								向JSON输出流写入JSON对象
								jos.writeJSONObject(send);
								jos.flush();
							} catch (IOException e) {
								EventQueue.invokeLater(()->{
									JOptionPane.showMessageDialog(null, "网络连接失败，无法连接到服务器");
									e.printStackTrace();
								});
							}
						}
					}).start();
				});
			} else if(msg.getStatus()==FileTransferMessage.ACCEPT) {
//				如果接收到的是同意接收文件消息，则取出文件接收方的服务器信息
				String targetHost = msg.getHost();
				int targetPort = msg.getPort();
//				提交EDT线程显示文件传输窗口
				EventQueue.invokeLater(()->{
					YCFileTransfer sendFileDialog = new YCFileTransfer(YCFileTransfer.SEND);
//					设置要传输的文件
					sendFileDialog.setProcessFile(fileToSend);
					sendFileDialog.setVisible(true);
//					启动文件发送
					sendFileDialog.processSendFile(targetHost, targetPort);
				});
			} else if(msg.getStatus()==FileTransferMessage.REJECT) {
//				如果接收到的是拒绝接收文件消息
				EventQueue.invokeLater(()->{
					System.out.println("很抱歉，对方拒绝了您的文件。");
					String oriText = textPaneMsgRecord.getText();
					textPaneMsgRecord.setText(new StringBuilder(oriText).append('\n').append("很抱歉，对方拒绝了您的文件。").toString());
//					提示用户文件传输请求被拒绝
					JOptionPane.showMessageDialog(null, "很抱歉，对方拒绝了您的文件。");
					fileToSend = null;
				});
			} 
		}
		/**
		 * 处理用户状态消息
		 * @param msg 用户状态消息对象
		 */
		private void processUserStateMessage(UserStateMessage msg) {
			String srcUser = msg.getSrcUser();
			String dstUser = msg.getDstUser();
			if (msg.getUserState() == UserStateMessage.ONLINE) {
//				如果是用户上线消息，则添加到在线用户列表里
				if (dstUser.equals("") || dstUser.equals(localUsername)) {
					onlineUserListModel.addElement(srcUser);
					onlineUserBoxModel.addElement(srcUser);
				}
			} else if (msg.getUserState() == UserStateMessage.OFFLINE){
//				如果是用户下线消息，则从在线用户列表移除
				if (onlineUserListModel.contains(srcUser)) {
					onlineUserListModel.removeElement(srcUser);
					onlineUserBoxModel.removeElement(srcUser);
				}
			}
			
		}
		/**
		 * 处理聊天消息
		 * @param msg 聊天消息对象
		 */
		private void processChatMessage(ChatMessage msg) {
			String srcUser = msg.getSrcUser();
			String dstUser = msg.getDstUser();
			String msgContent = msg.getMsgContent();
			if (msg.isPublicMessage()) {
//				公聊消息
				EventQueue.invokeLater(()->{
					System.out.println("收到公聊消息："+msgContent);
					String oriText = textPaneMsgRecord.getText();
					textPaneMsgRecord.setText(new StringBuilder(oriText).append('\n').append("收到公聊消息："+msgContent).toString());
				});
			} else if (dstUser.equals(localUsername)) {
//				私聊消息
				EventQueue.invokeLater(()->{
					System.out.println("收到 "+srcUser+" 的私聊消息："+msg.getMsgContent());
					String oriText = textPaneMsgRecord.getText();
					textPaneMsgRecord.setText(new StringBuilder(oriText).append('\n').append("收到 "+srcUser+" 的私聊消息："+msg.getMsgContent()).toString());
				});
			} else {
				System.out.println("接收消息异常，丢弃消息。");
			}	
		}
		
	}
}
