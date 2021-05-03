import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Random;
// import java.util.Arrays;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

import javax.crypto.interfaces.PBEKey;
import javax.naming.ldap.SortKey;
import javax.swing.TransferHandler.TransferSupport;

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
    private final static Semaphore waitSetGameMode = new Semaphore(0);
    private static int gameDiff;

    // 扫描连接的
    public static int distriSocket(Socket socket) throws IOException {
        Scanner input;
        String command;
        input = new Scanner(socket.getInputStream());
        while (input.hasNextLine()) {
            command = input.nextLine();
            if (command.startsWith("GAMEMODE")) {
                Boolean isInPVC = command.substring(9, 12).equals("PVC");
                if (isInPVC) {
                    // input.close();
                    gameDiff = Integer.parseInt(command.substring(13));
                    // System.out.println(gameDifficulty);
                    System.out.println("socket distribute: PVC Mode");
                    return 2;
                } else {
                    // input.close();
                    System.out.println("socket distribute: PVP Mode");
                    return 1;
                }
            }
        }

        return 0;
    }

    public static void main(String[] args) throws Exception {
        final Semaphore pvpSemp = new Semaphore(0);
        final Semaphore pvcSemp = new Semaphore(0);

        int disResult;
        try (ServerSocket listener = new ServerSocket(58901)) {
            System.out.println("Tic Tac Toe Server is Running...");
            PVPGame pvpgame = new PVPGame(pvpSemp);
            PVCGame pvcgame = new PVCGame(pvcSemp);
            pvpgame.start();
            pvcgame.start();

            while (true) {
                Socket receiveSocket;
                receiveSocket = listener.accept();
                disResult = distriSocket(receiveSocket);
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
                }
            }
        }
    }
}

class PVPGame extends Thread {
    ExecutorService pool = Executors.newFixedThreadPool(200);
    Socket newInSocket;
    Semaphore PVP_WaitPlayer;

    public PVPGame(Semaphore waitPlayer) {
        this.PVP_WaitPlayer = waitPlayer;
    }

    public void run() {
        while (true) {
            Game game = new Game();
            game.setGameMode(1);
            try {
                PVP_WaitPlayer.acquire(1);
                Game.Player p1 = game.new Player(newInSocket, 'X');
                pool.execute(p1);

                PVP_WaitPlayer.acquire(1);
                Game.Player p2 = game.new Player(newInSocket, 'O');
                pool.execute(p2);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}

class PVCGame extends Thread {
    ExecutorService pool = Executors.newFixedThreadPool(200);
    Socket newInSocket;
    Semaphore PVC_WaitPlayer;
    int gameDiffi;

    public PVCGame(Semaphore PVC_WaitPlayer) {
        this.PVC_WaitPlayer = PVC_WaitPlayer;
    }

    public void run() {
        while (true) {
            Game game = new Game();
            game.setGameMode(2);
            game.setGameDifficulty(this.gameDiffi);

            try {
                PVC_WaitPlayer.acquire(1);
                Game.Player p1 = game.new Player(newInSocket, 'X');
                p1.gameDifficulty = gameDiffi;
                Game.Computer aipc = game.new Computer('O', p1);
                p1.opponent = aipc;
                pool.execute(p1);
                pool.execute(aipc);

            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }
    }
}

class Game {

    // Board cells numbered 0-8, top to bottom, left to right; null if empty
    private Player[] board = new Player[9];
    private int gameDifficulty = -1;// 游戏难度 简单1 中等2 困难3
    private int gameMode = 0;// 游戏模式 1:PVP 2:PVC

    Player currentPlayer;
    Semaphore pvcWait = new Semaphore(0);

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

    public boolean boardFilledUp() {
        for (int i = 0; i < 9; i++) {
            if (board[i] == null)
                return false;
        }
        return true;
    }

    public ArrayList<Integer> getEmptyPlace() {
        ArrayList<Integer> emptyList = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (board[i] == null) {
                emptyList.add(i);
            }
        }
        return emptyList;
    }

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

    class Computer extends Player {
        private char charBoard[][] = new char[3][3];

        class Move {
            int row, col;
        };

        char playerMark = 'O', opponentMark = 'X';
        char mark;
        int gameDifficulty;

        Scanner input;
        PrintWriter output;

        public Computer(char mark, Player opponent) {
            super(mark, opponent);
            this.gameDifficulty = opponent.gameDifficulty;
        }

        public void run() {
            try {
                provessMove();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void drawBoard(Player[] board) {
            int index = 0;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[index] != null) {
                        charBoard[i][j] = board[index].mark;
                    } else {
                        charBoard[i][j] = '_';
                    }
                    index++;
                }
            }
        }

        private void provessMove() throws IOException {
            while (true) {
                try {
                    System.out.println("wait for human player move");
                    pvcWait.acquire(1);
                    System.out.println("player has moved,computer processing..");
                    drawBoard(board);
                    // 电脑生成落子点
                    int bestLocation;
                    if (getGameDifficulty() == 3) {
                        bestLocation = findBestMove(charBoard);
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

        Boolean isMovesLeft(char board[][]) {
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 3; j++)
                    if (board[i][j] == '_')
                        return true;
            return false;
        }

        // This is the evaluation function as discussed
        // in the previous article ( http://goo.gl/sJgv68 )
        int evaluate(char b[][]) {
            // Checking for Rows for X or O victory.
            for (int row = 0; row < 3; row++) {
                if (b[row][0] == b[row][1] && b[row][1] == b[row][2]) {
                    if (b[row][0] == playerMark)
                        return +10;
                    else if (b[row][0] == opponentMark)
                        return -10;
                }
            }

            // Checking for Columns for X or O victory.
            for (int col = 0; col < 3; col++) {
                if (b[0][col] == b[1][col] && b[1][col] == b[2][col]) {
                    if (b[0][col] == playerMark)
                        return +10;

                    else if (b[0][col] == opponentMark)
                        return -10;
                }
            }

            // Checking for Diagonals for X or O victory.
            if (b[0][0] == b[1][1] && b[1][1] == b[2][2]) {
                if (b[0][0] == playerMark)
                    return +10;
                else if (b[0][0] == opponentMark)
                    return -10;
            }

            if (b[0][2] == b[1][1] && b[1][1] == b[2][0]) {
                if (b[0][2] == playerMark)
                    return +10;
                else if (b[0][2] == opponentMark)
                    return -10;
            }

            // Else if none of them have won then return 0
            return 0;
        }

        // This is the minimax function. It considers all
        // the possible ways the game can go and returns
        // the value of the board
        int minimax(char board[][], int depth, Boolean isMax) {
            int score = evaluate(board);

            // If Maximizer has won the game
            // return his/her evaluated score
            if (score == 10)
                return score;

            // If Minimizer has won the game
            // return his/her evaluated score
            if (score == -10)
                return score;

            // If there are no more moves and
            // no winner then it is a tie
            if (isMovesLeft(board) == false)
                return 0;

            // If this maximizer's move
            if (isMax) {
                int best = -1000;

                // Traverse all cells
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        // Check if cell is empty
                        if (board[i][j] == '_') {
                            // Make the move
                            board[i][j] = playerMark;

                            // Call minimax recursively and choose
                            // the maximum value
                            best = Math.max(best, minimax(board, depth + 1, !isMax));

                            // Undo the move
                            board[i][j] = '_';
                        }
                    }
                }
                return best;
            }

            // If this minimizer's move
            else {
                int best = 1000;

                // Traverse all cells
                for (int i = 0; i < 3; i++) {
                    for (int j = 0; j < 3; j++) {
                        // Check if cell is empty
                        if (board[i][j] == '_') {
                            // Make the move
                            board[i][j] = opponentMark;

                            // Call minimax recursively and choose
                            // the minimum value
                            best = Math.min(best, minimax(board, depth + 1, !isMax));

                            // Undo the move
                            board[i][j] = '_';
                        }
                    }
                }
                return best;
            }
        }

        // This will return the best possible
        // move for the player
        int findBestMove(char board[][]) {
            int bestVal = -1000;
            Move bestMove = new Move();
            bestMove.row = -1;
            bestMove.col = -1;

            // Traverse all cells, evaluate minimax function
            // for all empty cells. And return the cell
            // with optimal value.
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    // Check if cell is empty
                    if (board[i][j] == '_') {
                        // Make the move
                        board[i][j] = playerMark;

                        // compute evaluation function for this
                        // move.
                        int moveVal = minimax(board, 0, false);

                        // Undo the move
                        board[i][j] = '_';

                        // If the value of the current move is
                        // more than the best value, then update
                        // best/
                        if (moveVal > bestVal) {
                            bestMove.row = i;
                            bestMove.col = j;
                            bestVal = moveVal;
                        }
                    }
                }
            }
            return (bestMove.row) * 3 + bestMove.col;
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
                System.out.println("Mark is X");
                currentPlayer = this;
                if (1 == gameMode) {
                    output.println("MESSAGE Waiting for opponent to connect");
                }
            } else {
                System.out.println("Mark is O");
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
                System.out.println(gameMode);
                if (2 == gameMode) {
                    System.out.println("opponent mark:" + opponent.mark);
                    System.out.println("release semp");
                    if (hasWinner()) {
                        output.println("VICTORY");
                    } else if (boardFilledUp()) {
                        output.println("TIE");
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