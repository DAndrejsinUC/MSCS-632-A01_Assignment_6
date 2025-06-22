import java.util.*;

public class DataProcessingSystem {

    // Represents a unit of work
    static class Task {
        private final int id;

        public Task(int id) {
            this.id = id;
        }

        public void process() throws InterruptedException {
            System.out.println(Thread.currentThread().getName() + " is processing Task " + id);
            Thread.sleep(500); // Simulate processing delay
        }

        public int getId() {
            return id;
        }
    }

    // Shared queue with synchronized access
    static class TaskQueue {
        private final Queue<Task> queue = new LinkedList<>();

        public synchronized void addTask(Task task) {
            queue.add(task);
            notify(); // Wake up one waiting thread if any
        }

        public synchronized Task getTask() throws InterruptedException {
            while (queue.isEmpty()) {
                wait(); // Wait until a task is available
            }
            return queue.poll();
        }

        public synchronized boolean isEmpty() {
            return queue.isEmpty();
        }
    }

    // Worker thread that pulls and processes tasks
    static class Worker extends Thread {
        private final TaskQueue taskQueue;
        private final List<String> results;

        public Worker(TaskQueue taskQueue, List<String> results, String name) {
            super(name);
            this.taskQueue = taskQueue;
            this.results = results;
        }

        @Override
        public void run() {
            while (true) {
                Task task;
                try {
                    synchronized (taskQueue) {
                        if (taskQueue.isEmpty()) break;
                    }

                    task = taskQueue.getTask(); // May block if queue is empty
                    task.process();

                    synchronized (results) {
                        results.add(getName() + " processed Task " + task.getId());
                    }

                } catch (InterruptedException e) {
                    System.err.println(getName() + " was interrupted.");
                    break;
                } catch (Exception e) {
                    System.err.println(getName() + " encountered an error: " + e.getMessage());
                }
            }

            System.out.println(getName() + " finished processing.");
        }
    }

    // Main entry point
    public static void main(String[] args) {
        TaskQueue taskQueue = new TaskQueue();
        List<String> results = Collections.synchronizedList(new ArrayList<>());

        // Add some tasks
        for (int i = 1; i <= 20; i++) {
            taskQueue.addTask(new Task(i));
        }

        // Create and start worker threads
        int numWorkers = 4;
        List<Worker> workers = new ArrayList<>();
        for (int i = 0; i < numWorkers; i++) {
            Worker worker = new Worker(taskQueue, results, "Worker-" + (i + 1));
            workers.add(worker);
            worker.start();
        }

        // Wait for all workers to finish
        for (Worker worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                System.err.println("Main thread interrupted.");
            }
        }

        // Show results
        System.out.println("\nFinal Results:");
        for (String result : results) {
            System.out.println(result);
        }
    }
}
