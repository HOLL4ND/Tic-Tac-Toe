
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ProcessBuilder.Redirect.Type;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
// import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.Semaphore;

import javax.crypto.interfaces.PBEKey;
import javax.naming.ldap.SortKey;
import javax.swing.TransferHandler.TransferSupport;
import javax.xml.crypto.Data;

import jdk.javadoc.internal.doclets.formats.html.resources.standard;

/**
 * A server for a multi-player tic tac toe game. Loosely based on an example in
 * Deitel and Deitel"Java How to Program" application-le proto call TT (f T T
 * Protocol) whic i entirely plain text. The messages of TTTP are:
 *
 * Client -> Server MOVE <n> QUIT
 *
 * Server -> Client WELCOME <char> VALID_MOVE OTHER_PLAYER_MOVED <n>
 * OTHER_PLAYER_LEFT VICTORY DEFEAT TIE MESSAGE <text>
 */
public class TicTacToeServer {
    private static int gameDiff;

    // 扫描连接的Socket,并进行分发处理
    public static int distriSocket(Socket socket) throws IOException {
        Scanner input;
        String command;
        input = new Scanner(socket.getInputStream());
        while (input.hasNextLine()) {
            command = input.nextLine();
            System.out.println(command);
            if (command.startsWith("LOGIN")) {

                String path = "D:\\user.txt";
                DataInputStream is = null;
                DataOutputStream os = null;

                is = new DataInputStream(socket.getInputStream());
                os = new DataOutputStream(socket.getOutputStream());

                login(is, os, path, socket, command);

                os.close();
                is.close();
                return 4;
            }
            if (command.startsWith("REGIS")) {
                String path = "D:\\user.txt";
                DataInputStream is = null;
                DataOutputStream os = null;

                is = new DataInputStream(socket.getInputStream());
                os = new DataOutputStream(socket.getOutputStream());

                register(is, os, path, command);

                os.close();
                is.close();
                return 4;

            }
            if (command.startsWith("GAMEMODE")) {
                Boolean isInPVC = command.substring(9, 12).equals("PVC");
                if (isInPVC) {
                    gameDiff = Integer.parseInt(command.substring(13));
                    System.out.println("socket distribute: PVC Mode");
                    return 2;
                } else {
                    System.out.println("socket distribute: PVP Mode");
                    return 1;
                }
            }
        }

        return 0;
    }

    public static void register(DataInputStream is, DataOutputStream os, String path, String ur_pwd) {
        BufferedWriter writer;
        try {
            writer = new BufferedWriter(new FileWriter(path));
            // 读取客户端输入的信息,输出到 user.txt 数据库中
            String[] str = ur_pwd.split("=");
            String name = str[0].substring(6);
            String pwd = str[1];
            writer.write("user=" + name);
            writer.newLine();
            writer.write("pass=" + pwd);
            writer.close();// 关闭缓冲流，才会写出数据
            // 像客户端反馈注册结果消息
            os.writeUTF("恭喜注册成功！如果需要重新登录");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void login(DataInputStream is, DataOutputStream os, String path, Socket socket, String ur_pwd) {
        try {
            // 读取客户端输入的信息
            String[] str = ur_pwd.split("=");

            String name = str[0].substring(6);

            String pwd = str[1];
            System.out.println("name:" + name + "&");
            System.out.println("pwd:" + pwd + "&");
            // 读取文本数据信息
            FileInputStream fis = new FileInputStream(path);
            Properties pros = new Properties();
            pros.load(fis);
            String user = pros.getProperty("user");
            String pass = pros.getProperty("pass");

            // 验证客户端输入的信息和文本数据信息是否一致。
            if (name.equals(user) && pwd.equals(pass)) {
                os.writeUTF("登录成功！");
            } else {
                // 向客户端发送消息，询问是否需要注册。
                os.writeUTF("账户或密码错误或不存在，登录失败! 请输入用户名和密码 并点击注册按钮");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        // Game等待用户的进入再开始设置游戏
        final Semaphore pvpSemp = new Semaphore(0);
        final Semaphore pvcSemp = new Semaphore(0);

        int disResult;
        try (ServerSocket listener = new ServerSocket(58901)) {
            System.out.println("Tic Tac Toe Server is Running...");

            // 创建两个线程,分别用于处理PVP和PVC的游戏
            PVPGame pvpgame = new PVPGame(pvpSemp);
            PVCGame pvcgame = new PVCGame(pvcSemp);
            pvpgame.start();
            pvcgame.start();

            int cnt = 0;

            while (true) {
                Socket receiveSocket;
                receiveSocket = listener.accept();// 等待客户端的连接
                disResult = distriSocket(receiveSocket);// 判断用户选择的游戏模式,并将收到的socket传入对应的线程

                switch (disResult) {
                    case 1:// 人人对战
                        pvpgame.newInSocket = receiveSocket;
                        pvpSemp.release(1);
                        break;
                    case 2:// 人机对战
                        pvcgame.newInSocket = receiveSocket;
                        pvcgame.gameDiffi = gameDiff;
                        pvcSemp.release(1);
                        break;
                    case 4:

                        break;
                }
            }
        }
    }
}

// 这里的类从内部类改成了外部类,删除外部类后取消注释可用

// 处理PVP游戏的线程
// class PVPGame extends Thread {
// ExecutorService pool = Executors.newFixedThreadPool(200);
// Socket newInSocket;
// Semaphore PVP_waitPlayer;// 信号量 作用:等待用户进入 释放源:由main线程释放
//
// public PVPGame(Semaphore waitPlayer) {
// this.PVP_waitPlayer = waitPlayer;
// }
//
// public void run() {
// while (true) {
// Game game = new Game();
// game.setGameMode(1);
// try {
// PVP_waitPlayer.acquire(1);
// Game.Player p1 = game.new Player(newInSocket, 'X');
// pool.execute(p1);
//
// PVP_waitPlayer.acquire(1);
// Game.Player p2 = game.new Player(newInSocket, 'O');
// pool.execute(p2);
//
// } catch (InterruptedException e) {
// e.printStackTrace();
// }
//
// }
// }
// }

// 处理PVC游戏的线程
// class PVCGame extends Thread {
// ExecutorService pool = Executors.newFixedThreadPool(200);
// Socket newInSocket;
// Semaphore PVC_WaitPlayer;// 信号量 作用:等待用户进入 释放源:由main线程释放
// int gameDiffi;
//
// public PVCGame(Semaphore PVC_WaitPlayer) {
// this.PVC_WaitPlayer = PVC_WaitPlayer;
// }
//
// public void run() {
// while (true) {
// Game game = new Game();
// try {
// PVC_WaitPlayer.acquire(1);
// game.setIsFinish(false);
// game.setGameMode(2);
// System.out.println("PVCGame Class-> game difficulty" + gameDiffi);
// game.setGameDifficulty(gameDiffi);
//
// Game.Player p1 = game.new Player(newInSocket, 'X');
// p1.gameDifficulty = gameDiffi;
// Game.Computer aipc = game.new Computer('O', p1);
// p1.opponent = aipc;
// pool.execute(p1);
// pool.execute(aipc);
// } catch (InterruptedException e) {
// // TODO Auto-generated catch block
// e.printStackTrace();
// }
//
// }
// }
// }

class Game {

    // Board cells numbered 0-8, top to bottom, left to right; null if empty
    private Player[] board = new Player[9];// 游戏棋盘,记录双方落子点
    private int gameDifficulty = -1;// 游戏难度 简单1 中等2 困难3
    private int gameMode = 0;// 游戏模式 1:PVP 2:PVC
    private Boolean isFinish;// 用于Computer类判断游戏是否结束

    Player currentPlayer;// 记录当前下子玩家
    Semaphore pvcWait = new Semaphore(0);// 信号量 用于电脑等待玩家的行为(下子&退出游戏)

    public Boolean getIsFinish() {
        return isFinish;
    }

    public void setIsFinish(Boolean isFinish) {
        this.isFinish = isFinish;
    }

    public int getGameDifficulty() {
        return gameDifficulty;
    }

    public int getGameMode() {
        return gameMode;
    }

    public void setGameMode(int gameMode) {
        this.gameMode = gameMode;
    }

    public void setGameDifficulty(int difficulty) {
        this.gameDifficulty = difficulty;
    }

    // 判赢函数
    public boolean hasWinner() {
        return (board[0] != null && board[0] == board[1] && board[0] == board[2])
                || (board[3] != null && board[3] == board[4] && board[3] == board[5])
                || (board[6] != null && board[6] == board[7] && board[6] == board[8])
                || (board[0] != null && board[0] == board[3] && board[0] == board[6])
                || (board[1] != null && board[1] == board[4] && board[1] == board[7])
                || (board[2] != null && board[2] == board[5] && board[2] == board[8])
                || (board[0] != null && board[0] == board[4] && board[0] == board[8])
                || (board[2] != null && board[2] == board[4] && board[2] == board[6]);
    }

    // 用于在判和中判断棋盘是否被填满
    public boolean boardFilledUp() {
        for (int i = 0; i < 9; i++) {
            if (board[i] == null)
                return false;
        }
        return true;
    }

    // 用于寻找空位置，用于简单模式下电脑随机下子
    public ArrayList<Integer> getEmptyPlace() {
        ArrayList<Integer> emptyList = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (board[i] == null) {
                emptyList.add(i);
            }
        }
        return emptyList;
    }

    // 系统用于处理玩家移动（玩家在鼠标点击棋盘后的处理）
    public synchronized void move(int location, Player player) {
        if (player != currentPlayer) {
            throw new IllegalStateException("Not your turn");
        } else if (this.gameMode == 1 && player.opponent == null) {
            throw new IllegalStateException("You don't have an opponent yet");
        } else if (board[location] != null) {
            throw new IllegalStateException("Cell already occupied");
        }
        board[location] = currentPlayer;
        // move后输出棋盘
        // printBoard();
        currentPlayer = currentPlayer.opponent;

    }

    // 输出不同格式棋盘的函数
    public void printBoard() {
        for (int i = 0; i < 9; i++) {
            System.out.print(board[i] == null ? '_' : board[i].mark);
            System.out.print(' ');
            if ((i + 1) % 3 == 0) {
                System.out.print('\n');
            }
        }
    }

    public void printIntBoard(int[] intBoard) {
        for (int i = 0; i < 9; i++) {
            System.out.print(intBoard[i] == 0 ? '_' : 'X');
            System.out.print(' ');
            if ((i + 1) % 3 == 0) {
                System.out.print('\n');
            }
        }
    }

    public void printCharBoard(char[][] charBoard) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                System.out.print(charBoard[i][j]);
                System.out.print(' ');
            }
            System.out.print('\n');
        }
    }

    // 电脑类 继承Player
    class Computer extends Player {

        char mark;

        public Computer(char mark, Player opponent) {
            super(mark, opponent);
        }

        public void run() {
            try {
                provessMove();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                System.out.println("computer thread finished.");// 验证电脑线程是否退出
            }
        }

        private char[][] drawCharBoard(Player[] board) {
            /**
             * 因为GFG类中查找最优下棋位置的函数接受的是一个二维Char数组 所以要将 当前棋盘的状态转换为二维Char数组
             * 
             * @param board 当前Game中的棋盘board.
             * @return 表示棋盘的二维Char数组.
             */
            char[][] charBoardTemp = new char[3][3];
            int index = 0;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[index] != null) {
                        charBoardTemp[i][j] = board[index].mark;
                    } else {
                        charBoardTemp[i][j] = '_';
                    }
                    index++;
                }
            }
            return charBoardTemp;
        }

        private void provessMove() throws IOException {
            while (true) {
                try {
                    System.out.println("wait for player action...");
                    pvcWait.acquire(1);
                    if (getIsFinish()) {
                        break;
                    }
                    // 电脑生成落子点
                    int bestLocation;
                    if (getGameDifficulty() == 3) {
                        char charBoard[][] = new char[3][3];
                        charBoard = drawCharBoard(board);
                        bestLocation = GFG.getBestMove(charBoard);
                        // System.out.println("bestMove is: " + bestLocation);
                    } else {
                        ArrayList<Integer> empty = getEmptyPlace();
                        Random r = new Random();
                        bestLocation = empty.get(r.nextInt(empty.size()));
                    }
                    // 电脑走棋
                    move(bestLocation, this);
                    printBoard();
                    opponent.output.println("OPPONENT_MOVED " + bestLocation);
                    if (hasWinner()) {
                        opponent.output.println("DEFEAT");
                        break;
                    } else if (boardFilledUp()) {
                        opponent.output.println("TIE");
                        break;
                    }

                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }
    }

    /**
     * A Player is identified by a character mark which is either 'X' or 'O'. For
     * communication with the client the player has a socket and associated Scanner
     * and PrintWriter.
     */
    class Player implements Runnable {
        char mark;
        int gameDifficulty = -1;// 游戏难度 简单1 中等2 困难3
        int gameMode = 0;// 游戏模式 1:PVP 2:PVC

        Player opponent;
        Socket socket;

        Scanner input;
        PrintWriter output;

        public Player(Socket socket, char mark) {
            this.socket = socket;
            this.mark = mark;
            this.gameMode = getGameMode();
        }

        public Player(char mark2, Game.Player opponent2) {
            this.mark = mark2;
            this.opponent = opponent2;
        }

        public void run() {
            try {
                setup();
                processCommands();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (gameMode == 1) {
                    if (opponent != null && opponent.output != null) {
                        opponent.output.println("OTHER_PLAYER_LEFT");
                    }
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            }
        }

        private void setup() throws IOException {
            input = new Scanner(socket.getInputStream());
            output = new PrintWriter(socket.getOutputStream(), true);
            output.println("WELCOME " + mark);// char mark = response.charAt(8);客户端代码
            if (mark == 'X') {
                // System.out.println("Mark is X");
                currentPlayer = this;
                if (1 == gameMode) {
                    output.println("MESSAGE Waiting for opponent to connect");
                }
            } else {
                // System.out.println("Mark is O");
                opponent = currentPlayer;
                opponent.opponent = this;
                opponent.output.println("MESSAGE Your move");
            }
        }

        private void processCommands() {
            String command = "";
            while (input.hasNextLine()) {
                command = input.nextLine();
                if (command.startsWith("QUIT")) {
                    System.out.println("player left");
                    setIsFinish(true);
                    pvcWait.release(1);
                    return;
                } else if (command.startsWith("MOVE")) {// out.println("MOVE " + j);客户端代码
                    processMoveCommand(Integer.parseInt(command.substring(5)));
                }
            }
        }

        private void processMoveCommand(int location) {
            try {
                move(location, this);
                output.println("VALID_MOVE");
                if (2 == gameMode) {
                    System.out.println("release semp");
                    if (hasWinner()) {
                        setIsFinish(true);
                        output.println("VICTORY");
                        pvcWait.release(1);

                    } else if (boardFilledUp()) {
                        setIsFinish(true);
                        output.println("TIE");
                        pvcWait.release(1);

                    } else {
                        pvcWait.release(1);
                    }

                }
                // 如果是人人对战则需进行下面的操作
                if (1 == gameMode) {
                    opponent.output.println("OPPONENT_MOVED " + location);
                    if (hasWinner()) {
                        output.println("VICTORY");
                        opponent.output.println("DEFEAT");
                    } else if (boardFilledUp()) {
                        output.println("TIE");
                        opponent.output.println("TIE");
                    }
                }
            } catch (IllegalStateException e) {
                output.println("MESSAGE " + e.getMessage());
            }
        }
    }
}
