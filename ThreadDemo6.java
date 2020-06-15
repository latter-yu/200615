import jdk.nashorn.internal.ir.Block;

public class ThreadDemo6 {
    // 阻塞队列: 主要实现 生产者消费者 模型.(典型的并发编程)
    //           基于数组实现.
    static class Block {
        private int[] array = new int[1000];
        private int head = 0;
        private int tail = 0;
        // [head, tail)
        // 两者重合时, 可能表示队列空, 也可能表示队列满.
        // 所以引用 size 表示队列是空还是满
        private volatile int size = 0;

        // 阻塞队列只支持 入队列 和 出队列 操作.
        // 由 wait() 和 nitify() 实现阻塞队列.

        public void put(int value) throws InterruptedException {
            // 阻塞队列的入队列
            synchronized (this) {
                while (size == array.length) {
                    // 队列已满, 入队列操作不能继续执行.
                    wait();
                    // ps: 一般 wait(); 搭配 while() 使用.
                }
                array[tail] = value;
                tail++;
                if (tail == array.length) {
                    tail = 0;
                }
                size++;

                notify();
                // 对应出队列的 wait();
                // 通知队列内已有元素, 可以继续出队列操作.
            }
        }

        public int take() throws InterruptedException {
            // 阻塞队列的出队列
            int ret = -1;
            synchronized (this) {
                if (size == 0) {
                    wait();
                    // 队列是空的, 出队列操作不能继续执行.
                }
                ret = array[head];
                head++;
                if (head == array.length) {
                    head = 0;
                }
                size--;

                notify();
                // 对应入队列的 wait();
                // 通知 队列未满, 可以继续入队列操作.
            }
            return ret;
        }
    }

    public static void main(String[] args) {
        Block block = new Block();
        // 两个线程分别模拟 生产者 和 消费者.
        // 分别模拟两次情况:
        // 第一次, 让给消费者消费的快一些, 生产者生产的慢一些.(在线程一里加入 Thread.sleep(500);)
        // 预期情况: 消费者线程会阻塞等待. 每次有新生产的元素的时候, 消费者才能消费.
        // 实际情况: 只有生产者生产了新元素, 消费者才能进行消费(生产消费交替实现)

        // 第二次, 让消费者消费的慢一些, 生产者生产的快一些.(在线程二里加入 Thread.sleep(500);)
        // 预期情况: 生产者线程刚开始的时候会快速的往队列中插入元素, 插入满了之后就会阻塞等待.
        // 随后消费者线程每次消费一个元素, 生产者才能生产新的元素.
        // 实际情况: 与预期情况相同, 但最后消费再生产时会有抢占内存情况出现.

        Thread producer = new Thread() {
            @Override
            public void run() {
                for (int i = 0; i < 1000; i++) {
                    try {
                        block.put(i);
                        System.out.println("生产 " + i);
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        producer.start();

        Thread consumer = new Thread() {
            @Override
            public void run() {
                while (true) {
                    try {
                        int ret = block.take();
                        System.out.println("消费 " + ret);
                        //Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        consumer.start();
    }
}

