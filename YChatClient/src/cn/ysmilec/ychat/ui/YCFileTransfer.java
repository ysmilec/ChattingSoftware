package cn.ysmilec.ychat.ui;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JProgressBar;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.JButton;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.awt.event.ActionEvent;

/**
 * 功能：
 * 文件传输界面，继承自JFrame类
 * 
 * @author ysmilec
 *
 */
public class YCFileTransfer extends JFrame {
	
	private static final long serialVersionUID = 5959937988708424265L;
//	工作状态是发送模式还是接收模式
	public static final int SEND = 0, RECEIVE = 1;
//	配置文件
	private Properties properties = new Properties();
//	发送或接收的文件对象
	private File processFile = null;
//	文件大小
	private double fileSize = 0;
//	已处理的字节数和上一秒已处理的字节数
	private double total = 0, previousTotal = total;
//	计时器
	private Timer timer = new Timer(true);
//	接收文件线程对象
	private ReceiveFileThread receiveThread = null;
//	发送文件线程对象
	private SendFileThread sendThread = null;
//	主页面Panel，界面元素
	private JPanel contentPane;
//	进度条，界面元素
	private JProgressBar progressBar;
//	传输速度，界面元素
	private JLabel labelSpeed;
//	进度百分比，界面元素
	private JLabel labelProcess;
//	传输的文件名称，界面元素
	private JLabel labelFileName;
//	取消按钮，界面元素
	private JButton btnCancel;

	/**
	 * 构造方法，创建窗体
	 */
	public YCFileTransfer(int state) {
		setTitle("YChat客户端 - 文件传输");
		setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
		setBounds(100, 100, 500, 150);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JPanel northPanel = new JPanel();
		contentPane.add(northPanel, BorderLayout.NORTH);
		
		JLabel labelState = new JLabel("正在发送/接收文件：");
//		根据状态设置文字
		if (state==SEND) {
			labelState.setText("正在发送文件：");
		} else if (state==RECEIVE) {
			labelState.setText("正在接收文件：");
		}
		northPanel.add(labelState);
		
		labelFileName = new JLabel();
		northPanel.add(labelFileName);
		
		JPanel centerPanel = new JPanel();
		contentPane.add(centerPanel, BorderLayout.CENTER);
		
		labelProcess = new JLabel("100%");
		centerPanel.add(labelProcess);
		
		progressBar = new JProgressBar();
		centerPanel.add(progressBar);
		
		labelSpeed = new JLabel("0000.00KB/s");
		centerPanel.add(labelSpeed);
		
		JPanel southPanel = new JPanel();
		contentPane.add(southPanel, BorderLayout.SOUTH);
		
		btnCancel = new JButton("取消传输");
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
//				点击取消按钮中断线程
				if (receiveThread != null) {
					receiveThread.interrupt();
				} else if(sendThread != null) {
					sendThread.interrupt();
				}
			}
		});
		southPanel.add(btnCancel);
		loadProperties();
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
	 * 取得正在处理的文件
	 * @return 文件对象
	 */
	public File getProcessFile() {
		return processFile;
	}
	
	/**
	 * 设置要处理的文件
	 * @param processFile 文件对象
	 */
	public void setProcessFile(File processFile) {
		this.processFile = processFile;
	}

	/**
	 * 发送文件的方法
	 * @param host 接收者的主机名
	 * @param port 接收者的端口
	 */
	public void processSendFile(String host, int port) {
		if (processFile == null || !processFile.exists() || processFile.isDirectory()) {
			JOptionPane.showMessageDialog(this, "文件出错无法发送");
			dispose();
			return;
		}
//		设置本地文件路径显示
		labelFileName.setText(processFile.getAbsolutePath());
//		设置文件大小
		fileSize = processFile.length();
		try {
//			启动发送文件线程
			sendThread = new SendFileThread(host, port);
//			设置为后台线程
			sendThread.setDaemon(true);
			sendThread.start();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(this, "文件发送失败");
			processFile = null;
			fileSize = -1;
			e.printStackTrace();
			dispose();
		}
	}
	
	/**
	 * 功能：
	 * 发送文件线程，继承自Thread类
	 * 
	 * @author ysmilec
	 *
	 */
	class SendFileThread extends Thread {
		
//		SSLSocket对象，用于支持安全通信
		private SSLSocket socket;
//		文件输入流，读取本地文件
		private FileInputStream fis;
//		带缓冲的字节输出流，向网络上发送数据
		private BufferedOutputStream bos;
		
		/**
		 * 初始化客户端SSL会话对象
		 * @return 客户端SSL会话对象
		 * @throws Exception
		 */
		private SSLContext initClientSSLContext() throws Exception {
//			打开密钥库文件
			FileInputStream truststorefis = new FileInputStream(properties.getProperty("KeyStoreFile"));
//			密钥库的密码
			char[] password = properties.getProperty("KeyStorePassword").toCharArray();
//			创建密钥库对象并加载
			KeyStore ts = KeyStore.getInstance("PKCS12");
			ts.load(truststorefis, password);
//			获得密钥管理工厂对象
			TrustManagerFactory factory = TrustManagerFactory.getInstance("SunX509");
			factory.init(ts);
//			创建并用上面的密钥管理工厂对象初始化SSL会话对象
			SSLContext context = SSLContext.getInstance("TLS");
//			第一个参数表示提供的证书，本例不需要客户端提供证书，故使用null
//			第二个参数是用于验证对方的证书，因为是客户端器模式所以只需要第二个参数
			context.init(null, factory.getTrustManagers(), null);		
			return context;
		}
		
		/**
		 * 构造方法
		 * @param host 接收者的主机名
		 * @param port 接收者的端口
		 * @throws Exception
		 */
		public SendFileThread(String host, int port) throws Exception {
//			初始化SSL会话
			SSLContext context = this.initClientSSLContext();
//			打开SSLSocket并连接到接收方
			socket = (SSLSocket) context.getSocketFactory().createSocket(host,port);
			System.out.println("已连接到："+host+":"+port);
//			设置启用的算法套件为所有支持的算法套件
			socket.setEnabledCipherSuites(socket.getSupportedCipherSuites());
//			创建本地文件输入流
			fis = new FileInputStream(processFile);
//			创建带缓冲的字节输出流
			bos = new BufferedOutputStream(socket.getOutputStream());
		}
		
		@Override
		public void run() {
			resolveSpeed();
//			创建一个100KB的缓冲区
			byte[] buffer = new byte[102400];
//			每一轮读取的字节数
			int n = 0;
//			已发送的字节数
			total = 0;
			try {
//				循环读取文件
				while((n = fis.read(buffer)) != -1) {
//					发送已读取的所有字节
					bos.write(buffer, 0, n);
					total += n;
//					显示发送进度
					setPrograssBar();
//					线程中断处理
					if (Thread.currentThread().isInterrupted()) {
						EventQueue.invokeLater(()->{
							JOptionPane.showMessageDialog(null, "用户终止了文件发送");
							dispose();
						});
//						中断线程时，清理所有流并释放连接
						clean();
						return;
					}
				}
				bos.flush();
				EventQueue.invokeLater(()->{
					JOptionPane.showMessageDialog(null, "文件发送成功");
					dispose();
				});
//				清理所有流并释放连接
				clean();
			} catch (Exception e) {
				EventQueue.invokeLater(()->{
					JOptionPane.showMessageDialog(null, "文件发送失败");
					e.printStackTrace();
					processFile = null;
					fileSize = -1;
					dispose();
				});
//				清理所有流并释放连接
				clean();
			}
		}
		
		/**
		 * 清理所有流并释放连接
		 */
		public void clean() {
			try {
				finishTransfer();
				socket.close();
				fis.close();
				bos.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * 接收文件的方法
	 * @param fileName 本地保存的文件名
	 * @param fileSize 远程文件大小
	 * @return 监听的端口号
	 */
	public int processReceiveFile(String fileName, double fileSize) {
//		设置处理的文件
		processFile = new File(fileName);
		labelFileName.setText(fileName);
		this.fileSize = fileSize;
		try {
//			开启接收文件线程
			receiveThread = new ReceiveFileThread();
//			设置为后台线程
			receiveThread.setDaemon(true);
			receiveThread.start();
			return receiveThread.getLocalPort();
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(this, "接收文件失败");
			dispose();
			return -1;
		}
	}
	

	/**
	 * 功能：
	 * 接收文件线程，继承自Thread类
	 * 
	 * @author ysmilec
	 *
	 */
	class ReceiveFileThread extends Thread {
		
//		SSL服务器Socket，用于支持安全通信
		private SSLServerSocket serverSocket;
//		带缓冲的字节输入流，用于从网络上接收文件
		private BufferedInputStream bis;
//		文件输出流，用于写入文件到本地
		private FileOutputStream fos;
		
		/**
		 * 初始化服务器端SSL会话对象
		 * @return 服务器端SSL会话对象
		 * @throws Exception
		 */
		private SSLContext initSSLServerContext() throws Exception {
//			打开密钥库文件
			FileInputStream keystorefis = new FileInputStream(properties.getProperty("KeyStoreFile"));
//			密钥库的密码
			char[] password = properties.getProperty("KeyStorePassword").toCharArray();
//			创建密钥库对象并加载
			KeyStore ks = KeyStore.getInstance("PKCS12");
			ks.load(keystorefis, password);
//			获得密钥管理工厂对象
			KeyManagerFactory factory = KeyManagerFactory.getInstance("SunX509");
			factory.init(ks, password);
//			创建并用上面的密钥管理工厂对象初始化SSL会话对象
			SSLContext context = SSLContext.getInstance("TLS");
//			第一个参数表示提供的证书，因为是服务器模式所以只需要第一个参数
//			第二个参数是用于验证对方的证书，本例不需要客户端提供证书，故使用null
			context.init(factory.getKeyManagers(), null, null);
			return context;
		}
		
		/**
		 * 构造方法
		 * @throws Exception
		 */
		public ReceiveFileThread() throws Exception{
//			初始化SSL会话
			SSLContext context = initSSLServerContext();
//			从SSL会话中获得支持安全通信的服务器Socket
			serverSocket = (SSLServerSocket)context.getServerSocketFactory().createServerSocket(0);
//			设置启用的算法套件为所有支持的算法套件
			serverSocket.setEnabledCipherSuites(serverSocket.getSupportedCipherSuites());
//			打开本地文件输出流
			fos = new FileOutputStream(processFile);
			System.out.println("服务器启动，端口："+serverSocket.getLocalPort());
		}
		
		public int getLocalPort() {
			return serverSocket.getLocalPort();
		}

		@Override
		public void run() {
			try {
//				开始监听远程连接
				Socket socket = serverSocket.accept();
				System.out.println("已建立连接，开始传输文件");
				resolveSpeed();
//				获得带缓冲的字节输入流
				bis = new BufferedInputStream(socket.getInputStream());
//				创建一个100KB的缓冲区
				byte[] buffer = new byte[102400];
//				已接收的字节数
				int n = 0;
//				已经写入的字节数
				total = 0;
//				循环接收数据
				while((n = bis.read(buffer)) != -1) {
//					写入数据到本地文件
					fos.write(buffer, 0, n);
					total += n;
//					显示接收进度
					setPrograssBar();
//					线程中断处理
					if (Thread.currentThread().isInterrupted()) {
						EventQueue.invokeLater(()->{
							JOptionPane.showMessageDialog(null, "用户终止了文件接收");
							dispose();
						});
//						中断线程时，清理所有流并释放连接
						clean();
						return;
					}
				}
				fos.flush();
				EventQueue.invokeLater(()->{
					JOptionPane.showMessageDialog(null, "文件接收成功");
					dispose();
				});
//				中断线程时，清理所有流并释放连接
				clean();
			} catch (IOException e) {
				e.printStackTrace();
				EventQueue.invokeLater(()->{
					JOptionPane.showMessageDialog(null, "接收文件失败");
				});
//				中断线程时，清理所有流并释放连接
				clean();
			}
			
		}
		
		/**
		 * 中断线程时，清理所有流并释放连接
		 */
		public void clean() {
			try {
				serverSocket.close();
				fos.close();
				bis.close();
				finishTransfer();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * 设置进度
	 * @param sended 已经发送或者接收的数据量
	 */
	private void setPrograssBar() {
		EventQueue.invokeLater(()->{
			int persent = (int)(total*100 / fileSize);
//			设置进度条
			progressBar.setValue(persent);
//			设置百分比显示
			labelProcess.setText(persent+"%");
		});
	}
	
	private void resolveSpeed() {
		TimerTask task = new TimerTask() {
			@Override
			public void run() {
				double perSecTransferByte = total - previousTotal;
				labelSpeed.setText((perSecTransferByte/1024)+"KB/s");
				previousTotal = total;
			}
		};
		timer.schedule(task, 1000);
	}
	
	private void finishTransfer() {
		timer.cancel();
	}

}
