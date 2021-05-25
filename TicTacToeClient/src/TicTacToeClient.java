import java.awt.*;
import java.awt.Font;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.GridBagLayout;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Scanner;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.xml.crypto.Data;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import jdk.internal.platform.Container;

/**
 * A client for a multi-player tic tac toe game. Loosely based on an example in
 * Deitel and Deitel's "Java How to Program" book. For this project I created a
 * new application-level protocol called TTTP (for Tic Tac Toe Protocol), which
 * is entirely plain text. The messages of TTTP are:
 *
 * Client -> Server MOVE <n> QUIT
 *
 * 
 * Server -> Client WELCOME <char> VALID_MOVE OTHER_PLAYER_MOVED <n>
 * OTHER_PLAYER_LEFT VICTORY DEFEAT TIE MESSAGE <text>
 */
public class TicTacToeClient extends Thread {

	static TicTacToeClient client;

	private JFrame frame = new JFrame("Tic Tac Toe");
	private JLabel messageLabel = new JLabel("...");

	private Square[] board = new Square[9];
	private Square currentSquare;

	private Socket socket;
	private Scanner in;
	private PrintWriter out;

	private static String gameMode = "";
	private static int gameDifficulty = 0;
	private static boolean isWindowClosed;
	private static int RegisterOrLogin = 0;

	public TicTacToeClient(String serverAddress) throws Exception {

		socket = new Socket(serverAddress, 58901);
		in = new Scanner(socket.getInputStream());
		out = new PrintWriter(socket.getOutputStream(), true);

		messageLabel.setBackground(Color.lightGray);
		frame.getContentPane().add(messageLabel, BorderLayout.SOUTH);

		JPanel boardPanel = new JPanel();
		boardPanel.setBackground(Color.black);
		boardPanel.setLayout(new GridLayout(3, 3, 2, 2));
		for (int i = 0; i < board.length; i++) {
			final int j = i;
			board[i] = new Square();
			board[i].addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					currentSquare = board[j];
					out.println("MOVE " + j);
				}
			});
			boardPanel.add(board[i]);
		}
		frame.getContentPane().add(boardPanel, BorderLayout.CENTER);
	}

	/**
	 * The main thread of the client will listen for messages from the server. The
	 * first message will be a "WELCOME" message in which we receive our mark. Then
	 * we go into a loop listening for any of the other messages, and handling each
	 * message appropriately. The "VICTORY", "DEFEAT", "TIE", and
	 * "OTHER_PLAYER_LEFT" messages will ask the user whether or not to play another
	 * game. If the answer is no, the loop is exited and the server is sent a "QUIT"
	 * message.
	 */
	public void run() {
		System.out.println("start game");
		try {
			out.println("GAMEMODE " + gameMode + " " + gameDifficulty);
			if (gameMode == "PVC") {
				out.println("GAMEDIFFICULTY " + gameDifficulty);
			}
			String response = in.nextLine();
			char mark = response.charAt(8);
			char opponentMark = mark == 'X' ? 'O' : 'X';
			frame.setTitle("Tic Tac Toe: Player " + mark);
			while (in.hasNextLine() && !isWindowClosed) {
				response = in.nextLine();
				if (response.startsWith("VALID_MOVE")) {
					messageLabel.setText("Valid move, please wait");
					currentSquare.setText(mark);
					currentSquare.repaint();
				} else if (response.startsWith("OPPONENT_MOVED")) {
					int loc = Integer.parseInt(response.substring(15));
					board[loc].setText(opponentMark);
					board[loc].repaint();
					messageLabel.setText("Opponent moved, your turn");
				} else if (response.startsWith("MESSAGE")) {
					messageLabel.setText(response.substring(8));
				} else if (response.startsWith("VICTORY")) {
					JOptionPane.showMessageDialog(frame, "Winner Winner");
					break;
				} else if (response.startsWith("DEFEAT")) {
					JOptionPane.showMessageDialog(frame, "Sorry you lost");
					break;
				} else if (response.startsWith("TIE")) {
					JOptionPane.showMessageDialog(frame, "Tie");
					break;
				} else if (response.startsWith("OTHER_PLAYER_LEFT")) {
					JOptionPane.showMessageDialog(frame, "Other player left");
					break;
				}
			}
			System.out.println("player quit");
			out.println("QUIT");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println("Player thread finally execute.");
			try {
				socket.close();
			} catch (IOException e) {
				// TODO 自动生成的 catch 块
				e.printStackTrace();
			}
			frame.dispose();
		}
	}

	static class Square extends JPanel {
		private static final long serialVersionUID = 1L;
		JLabel label = new JLabel();

		public Square() {
			setBackground(Color.white);
			setLayout(new GridBagLayout());
			label.setFont(new Font("Arial", Font.BOLD, 40));
			add(label);
		}

		public void setText(char text) {
			label.setForeground(text == 'X' ? Color.BLUE : Color.RED);
			label.setText(text + "");
		}
	}

	static void startGame(final String address) throws Exception {
		client = new TicTacToeClient(address);
		client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		client.frame.setSize(320, 320);
		client.frame.setResizable(false);
		client.frame.setVisible(true);// 显示游戏界面
		client.frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				System.out.println("game windows is closing");
				isWindowClosed = true;
				client.out.println("QUIT");
			}
		});
		isWindowClosed = false;
		client.start();

	}

	static class startFrame {

		public startFrame(final String address) {

			final JFrame startUp = new JFrame("井字棋");
			startUp.setSize(300, 300);
			startUp.setResizable(false);
			startUp.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			startUp.getContentPane().setLayout(new GridLayout(5, 1));
			startUp.invalidate();

			JLabel gameName = new JLabel("井字棋");

			String pvp = "人人对战";
			final JButton pvpbutton = new JButton(pvp);
			pvpbutton.setMnemonic(KeyEvent.VK_B);
			pvpbutton.setActionCommand(pvp);

			String pvc = "人机对战";
			final JButton pvcbutton = new JButton(pvc);
			pvcbutton.setMnemonic(KeyEvent.VK_B);
			pvcbutton.setActionCommand(pvc);

			ButtonGroup group = new ButtonGroup();
			group.add(pvcbutton);

			JPanel gamename = new JPanel();
			JPanel gameMode1 = new JPanel();
			JPanel gameMode2 = new JPanel();
			final JPanel difficultyLevels = new JPanel();

			gamename.add(gameName);
			startUp.getContentPane().add(gamename, BorderLayout.CENTER);
			gameMode1.add(pvpbutton);
			startUp.getContentPane().add(gameMode1, BorderLayout.CENTER);
			gameMode2.add(pvcbutton);
			startUp.getContentPane().add(gameMode2, BorderLayout.CENTER);

			JLabel selectDiffTips = new JLabel("请选择难度");
			final JPanel sft = new JPanel();
			sft.add(selectDiffTips);
			startUp.getContentPane().add(sft, BorderLayout.CENTER);
			sft.setVisible(false);

			final JButton easy = new JButton("简单");
			final JButton difficult = new JButton("困难");

			difficultyLevels.add(easy);
			difficultyLevels.add(difficult);
			startUp.getContentPane().add(difficultyLevels, BorderLayout.CENTER);

			difficultyLevels.setVisible(false);
			startUp.setVisible(true);
			pvpbutton.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					System.out.println("pvp button click");
					gameMode = "PVP";
					startUp.setVisible(false);// 模式选择界面隐藏

					try {
						startGame(address);
					} catch (Exception e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			});
			pvcbutton.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					System.out.println("pvc button click");
					gameMode = "PVC";
					sft.setVisible(true);
					difficultyLevels.setVisible(true);
				}
			});

			easy.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					System.out.println("difficulty: easy");
					gameDifficulty = 1;
					startUp.setVisible(false);// 模式选择界面隐藏
					try {
						startGame(address);
					} catch (Exception e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			});

			difficult.addMouseListener(new MouseAdapter() {
				public void mousePressed(MouseEvent e) {
					System.out.println("difficulty: difficult");
					gameDifficulty = 3;
					startUp.setVisible(false);// 模式选择界面隐藏
					try {
						startGame(address);
					} catch (Exception e1) {
						// TODO 自动生成的 catch 块
						e1.printStackTrace();
					}
				}
			});

		}

	}

	// 登录类
	static class startLogin {
		public startLogin() {
			// 登录窗口：
			final JFrame login = new JFrame("登录");
			login.setSize(350, 170);
			// login.setResizable(false);
			login.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			JPanel panel = new JPanel();
			login.add(panel);

			// 设置布局为null
			panel.setLayout(null);

			// 创建JLabel组件与设置位置
			JLabel userLabel = new JLabel("Username: ");
			userLabel.setBounds(10, 20, 80, 25);
			panel.add(userLabel);

			// 用户文本域
			final JTextField userText = new JTextField(20);
			userText.setBounds(100, 20, 165, 25);
			panel.add(userText);

			// 密码文本域
			JLabel passwordLable = new JLabel("Password: ");
			passwordLable.setBounds(10, 50, 80, 25);
			panel.add(passwordLable);

			// 类似输入的文本域，但会用*代替
			final JPasswordField passwordText = new JPasswordField();
			passwordText.setBounds(100, 50, 165, 25);
			panel.add(passwordText);

			// login 按钮
			final JButton loginButton = new JButton("Login");
			loginButton.setBounds(100, 80, 80, 25);

			// 登录按钮监听
			loginButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						// 获取输入框的账户密码
						String username = userText.getText();
						String password = new String(passwordText.getPassword());

						if (LoginMain(username, password) == 1) {
							JOptionPane.showMessageDialog(null, "登录成功！");
							System.out.println("登录成功！");
							login.setVisible(false);

							startFrame st = new startFrame(IP_Adr);
						} else {
							JOptionPane.showMessageDialog(null, "账号或密码错误！");
							System.out.println("账号或密码错误！");
						}
					} catch (Exception ex) {
						System.out.println(ex.toString());
					}
				}
			});

			panel.add(loginButton);

			// register 按钮
			JButton registerButton = new JButton("Register");
			registerButton.setBounds(185, 80, 80, 25);

			// 注册按钮监听
			registerButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					try {
						// 获取输入框的账户密码
						String username = userText.getText();
						String password = new String(passwordText.getPassword());
						if (username.equals("") || password.equals("")) {
							JOptionPane.showMessageDialog(null, "账号或密码不能为空！");
							System.out.println("账号或密码不能为空！");
							return;
						}
						ResgisMain(username, password);
						JOptionPane.showMessageDialog(null, "注册成功！");
						System.out.println("注册成功！");

					} catch (Exception ex) {
						System.out.println(ex.toString());
					}
				}
			});

			panel.add(registerButton);

			login.setVisible(true);
		}

		public final static int Port = 58901;
		public final static String IP_Adr = "127.0.0.1";

		// 注册通信
		private static void TmpRegister(DataInputStream is, DataOutputStream os, Socket socket, String ur, String ps)
				throws IOException {
			PrintWriter out;
			out = new PrintWriter(socket.getOutputStream(), true);

			String mes = ur + "=" + ps;

			out.println("REGIS " + mes);

			os.writeUTF(mes);
			String ret = is.readUTF();

			System.out.println(ret);
		}

		// 登录通信验证
		private static int TmpLogin(DataInputStream is, DataOutputStream os, Socket socket, String ur, String ps)
				throws IOException {
			PrintWriter out;
			out = new PrintWriter(socket.getOutputStream(), true);
			String mes = ur + "=" + ps;
			// os.writeUTF("LOGIN " + mes + '\n');
			out.println("LOGIN " + mes);
			// os.writeUTF(mes);
			String ret = is.readUTF();
			System.out.println(ret);

			socket.close();
			if (ret.equals("登录成功！")) {
				return 1;
			} else {
				return 0;
			}
		}

		static public int LoginMain(String ur, String ps) throws UnknownHostException, IOException {
			DataInputStream is = null;
			DataOutputStream os = null;

			Socket socket = new Socket(IP_Adr, Port);

			is = new DataInputStream(socket.getInputStream());
			os = new DataOutputStream(socket.getOutputStream());

			return TmpLogin(is, os, socket, ur, ps); // 是否登录成功
		}

		static public void ResgisMain(String ur, String ps) throws UnknownHostException, IOException {
			DataInputStream is = null;
			DataOutputStream os = null;

			Socket socket = new Socket(IP_Adr, Port);

			is = new DataInputStream(socket.getInputStream());
			os = new DataOutputStream(socket.getOutputStream());

			TmpRegister(is, os, socket, ur, ps);
		}

	}

	public static void main(String[] args) throws Exception {
		String tmp = "127.0.0.1";
		// if (args.length != 1) {
		// System.err.println("Pass the server IP as the sole command line argument");
		// return;
		// }

		// startFrame startWindow = new startFrame(tmp);
		startLogin SL = new startLogin();
	}
}