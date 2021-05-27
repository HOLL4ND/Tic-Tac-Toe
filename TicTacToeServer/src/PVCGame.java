import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class PVCGame extends Thread {
    ExecutorService pool = Executors.newFixedThreadPool(200);
    Socket newInSocket;
    Semaphore PVC_WaitPlayer;// 信号量 作用:等待用户进入 释放源:由main线程释放
    int gameDiffi;

    public PVCGame(Semaphore PVC_WaitPlayer) {
        this.PVC_WaitPlayer = PVC_WaitPlayer;
    }

    public void run() {
        while (true) {
            Game game = new Game();
            try {
                PVC_WaitPlayer.acquire(1);
                game.setIsFinish(false);
                game.setGameMode(2);
                System.out.println("PVCGame Class-> game difficulty" + gameDiffi);
                game.setGameDifficulty(gameDiffi);

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