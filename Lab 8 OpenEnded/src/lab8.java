import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.io.FileWriter;
import java.io.IOException;
// I HAVE ADD THIS SENTENCE FOR PULL AND MERGE
//I HAVE ADDED THIS COMMENT TO TEST GIT
class LaundrySystem {
    private final Queue<String> laundryQueue = new LinkedList<>();
    private final Queue<String> washedQueue = new LinkedList<>();
    private boolean stop = false;

    private final ReentrantLock lock = new ReentrantLock();
    private final Condition laundryAvailable = lock.newCondition();
    private final Condition washedAvailable = lock.newCondition();

    private FileWriter logger;

    public LaundrySystem() {
        try {
            logger = new FileWriter("laundry_log.txt");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void log(String message) {
        System.out.println(message);
        try {
            logger.write(message + "\n");
            logger.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addLaundry(String item) {
        lock.lock();
        try {
            laundryQueue.add(item);
            log("Loader: Added " + item + " to laundry queue.");
            laundryAvailable.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public void washLaundry() throws InterruptedException {
        String item = null;
        lock.lock();
        try {
            while (laundryQueue.isEmpty() && !stop) {
                laundryAvailable.await();
            }
            if (!laundryQueue.isEmpty()) {
                item = laundryQueue.poll();
                log("Washer: Washing " + item);
            }
        } finally {
            lock.unlock();
        }

        if (item != null) {
            Thread.sleep(1000); // simulate washing
            lock.lock();
            try {
                washedQueue.add(item);
                log("Washer: Finished washing " + item);
                washedAvailable.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }

    public void dryLaundry() throws InterruptedException {
        String item = null;
        lock.lock();
        try {
            while (washedQueue.isEmpty() && !stop) {
                washedAvailable.await();
            }
            if (!washedQueue.isEmpty()) {
                item = washedQueue.poll();
                log("Dryer: Drying " + item);
            }
        } finally {
            lock.unlock();
        }

        if (item != null) {
            Thread.sleep(1000); // simulate drying
            log("Dryer: Finished drying " + item);
        }
    }

    public void stopSystem() {
        stop = true;
        lock.lock();
        try {
            laundryAvailable.signalAll();
            washedAvailable.signalAll();
        } finally {
            lock.unlock();
        }
        try {
            if (logger != null)
                logger.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

public class lab8 {
    public static void main(String[] args) throws InterruptedException {
        LaundrySystem system = new LaundrySystem();

        Thread loader = new Thread(() -> {
            String[] items = { "Shirt", "Pants", "Socks", "Jacket" };
            for (String item : items) {
                system.addLaundry(item);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        Thread washer = new Thread(() -> {
            try {
                for (int i = 0; i < 4; i++)
                    system.washLaundry();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread dryer = new Thread(() -> {
            try {
                for (int i = 0; i < 4; i++)
                    system.dryLaundry();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        Thread loggerThread = new Thread(() -> {
            try {
                System.out.println("Logger: Laundry system started.");
                Thread.sleep(3000);
                System.out.println("Logger: Laundry system running...");
                Thread.sleep(3000);
                System.out.println("Logger: Laundry system completed.");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

        // Start threads
        loader.start();
        washer.start();
        loader.join(); // Ensure loader finishes before dryer starts fully
        dryer.start();
        loggerThread.start();

        // Wait for all threads
        loader.join();
        washer.join();
        dryer.join();
        loggerThread.join();

        system.stopSystem();
        System.out.println("Automated Laundry System: All tasks completed. Check laundry_log.txt for detailed logs.");
    }
}
