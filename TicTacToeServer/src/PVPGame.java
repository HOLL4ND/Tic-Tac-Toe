import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class PVPGame extends Thread {
    ExecutorService pool = Executors.newFixedThreadPool(200);
    Socket newInSocket;
    Semaphore PVP_waitPlayer;// 信号量 作用:等待用户进入 释放源:由main线程释放

    public PVPGame(Semaphore waitPlayer) {
        this.PVP_waitPlayer = waitPlayer;
    }

    public void run() {
        while (true) {
            Game game = new Game();
            game.setGameMode(1);
            try {
                PVP_waitPlayer.acquire(1);
                Game.Player p1 = game.new Player(newInSocket, 'X');
                pool.execute(p1);

                PVP_waitPlayer.acquire(1);
                Game.Player p2 = game.new Player(newInSocket, 'O');
                pool.execute(p2);

            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}
