# Tic-Tac-Toe
> java 网络编程实验

实现在线的井字棋网络程序

### 实现用例：

- 人人对战
- 人机对战（简单模式和困难模式）

![](https://github.com/HOLL4ND/tic-tac-toe/blob/master/Image/startwindow.png)

### 用例说明：

#### 简单模式

简单模式电脑采取随机下子的策略

#### 困难模式

困难模式电脑采用 Minimax Algorithm 进行最优落子选择

### 现存BUG：

- 人人对战中一方进入游戏后退出则下一个进入的玩家会处于缺少玩家的游戏房间中，且新进去的玩家由于处于新房间中，无法与该玩家组队。

