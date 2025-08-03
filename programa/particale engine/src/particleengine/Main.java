package particleengine;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main extends Application {
    private Canvas canvas = new Canvas(800, 600);
    private GraphicsContext gc = canvas.getGraphicsContext2D();
    private Emitter currentEmitter;
    private Thread animationThread;
    private ServerSocket workerServer;
    private List<Thread> workerThreads = new ArrayList<>();

    @Override
    public void start(Stage primaryStage) {
        Button sequentialBtn = new Button("Sequential Mode");
        Button parallelBtn = new Button("Parallel Mode");
        Button distributiveBtn = new Button("Distributive Mode");

        setSequentialMode();

        sequentialBtn.setOnAction(e -> setSequentialMode());
        parallelBtn.setOnAction(e -> setParallelMode());
        distributiveBtn.setOnAction(e -> setDistributiveMode());

        VBox root = new VBox(10, canvas, sequentialBtn, parallelBtn, distributiveBtn);
        primaryStage.setScene(new Scene(root));
        primaryStage.setTitle("Unified Particle Engine");
        primaryStage.show();

        startAnimation();
        startWorkerServer();
    }

    private void setSequentialMode() {
        currentEmitter = new SequentialEmitter(400, 250);
        System.out.println("Switched to Sequential Mode");
    }

    private void setParallelMode() {
        currentEmitter = new ParallelEmitter(400, 250);
        System.out.println("Switched to Parallel Mode");
    }

    private void setDistributiveMode() {
        currentEmitter = new DistributiveEmitter(400, 250);
        System.out.println("Switched to Distributive Mode");
    }

    private void startAnimation() {
        animationThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (currentEmitter != null) {
                    currentEmitter.emit(10);
                    currentEmitter.update();
                    gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    currentEmitter.draw(gc);
                }

                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    System.err.println("Animation thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        animationThread.setDaemon(true);
        animationThread.start();
    }

    private void startWorkerServer() {
        new Thread(() -> {
            try {
                workerServer = new ServerSocket(5001);
                while (!Thread.currentThread().isInterrupted()) {
                    Socket worker = workerServer.accept();
                    Thread workerThread = new Thread(() -> handleWorker(worker));
                    workerThreads.add(workerThread);
                    workerThread.start();
                }
            } catch (IOException e) {
                System.err.println("Worker server error: " + e.getMessage());
            }
        }).start();
    }

    private void handleWorker(Socket worker) {
        try (var out = new ObjectOutputStream(worker.getOutputStream());
             var in = new ObjectInputStream(worker.getInputStream())) {
            while (!Thread.currentThread().isInterrupted()) {
                if (currentEmitter instanceof DistributiveEmitter) {
                    List<DistributiveParticle> particles = ((DistributiveEmitter) currentEmitter).getParticles();
                    out.writeObject(new ArrayList<>(particles));
                    out.flush();
                    @SuppressWarnings("unchecked")
                    List<DistributiveParticle> updated = (List<DistributiveParticle>) in.readObject();
                    synchronized (particles) {
                        particles.clear();
                        particles.addAll(updated);
                    }
                }
                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    System.err.println("Worker thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Worker handling error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    interface Emitter {
        void emit(int count);
        void update();
        void draw(GraphicsContext gc);
        List<? extends Particle> getParticles();
    }

    interface Particle {
        void update();
        void draw(GraphicsContext gc);
        boolean isAlive();
    }

    class SequentialEmitter implements Emitter {
        private double x, y;
        private List<SequentialParticle> particles = new ArrayList<>();

        public SequentialEmitter(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void emit(int count) {
            for (int i = 0; i < count; i++) {
                double dx = (Math.random() - 0.5) * 7;
                double dy = (Math.random() - 0.5) * 7;
                particles.add(new SequentialParticle(x, y, dx, dy, 80));
            }
        }

        public void update() {
            List<SequentialParticle> toRemove = new ArrayList<>();
            for (SequentialParticle p : particles) {
                p.update();
                if (!p.isAlive()) toRemove.add(p);
            }
            particles.removeAll(toRemove);
        }

        public void draw(GraphicsContext gc) {
            for (SequentialParticle p : particles) {
                p.draw(gc);
            }
        }

        public List<SequentialParticle> getParticles() {
            return particles;
        }
    }

    class SequentialParticle implements Particle {
        private double x, y, dx, dy, ttl;
        private Color color = Color.HOTPINK;

        public SequentialParticle(double x, double y, double dx, double dy, double ttl) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.ttl = ttl;
        }

        public void update() {
            x += dx;
            y += dy;
            dy += 0.1;
            ttl--;
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, ttl / 100));
        }

        public void draw(GraphicsContext gc) {
            gc.setFill(color);
            gc.fillOval(x, y, 6, 6);
        }

        public boolean isAlive() {
            return ttl > 0;
        }
    }

    class ParallelEmitter implements Emitter {
        private double x, y;
        private List<ParallelParticle> particles = new ArrayList<>();
        private final int THREAD_COUNT = Math.min(4, Runtime.getRuntime().availableProcessors());

        public ParallelEmitter(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void emit(int count) {
            for (int i = 0; i < count; i++) {
                double dx = (Math.random() - 0.5) * 7;
                double dy = (Math.random() - 0.5) * 7;
                particles.add(new ParallelParticle(x, y, dx, dy, 80));
            }
        }

        public void update() {
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            int chunkSize = particles.size() / THREAD_COUNT + (particles.size() % THREAD_COUNT == 0 ? 0 : 1);
            List<ParallelParticle> toRemove = new ArrayList<>();

            for (int i = 0; i < THREAD_COUNT; i++) {
                final int start = i * chunkSize;
                final int end = Math.min(start + chunkSize, particles.size());
                if (start < particles.size()) {
                    executor.submit(() -> {
                        for (int j = start; j < end; j++) {
                            ParallelParticle p = particles.get(j);
                            p.update();
                            if (!p.isAlive()) synchronized (toRemove) { toRemove.add(p); }
                        }
                    });
                }
            }

            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.err.println("Parallel update interrupted: " + e.getMessage());
            }
            synchronized (particles) { particles.removeAll(toRemove); }
        }

        public void draw(GraphicsContext gc) {
            synchronized (particles) {
                for (ParallelParticle p : particles) {
                    p.draw(gc);
                }
            }
        }

        public List<ParallelParticle> getParticles() {
            return particles;
        }
    }

    class ParallelParticle implements Particle {
        private double x, y, dx, dy, ttl;
        private Color color = Color.BLUEVIOLET;

        public ParallelParticle(double x, double y, double dx, double dy, double ttl) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.ttl = ttl;
        }

        public void update() {
            x += dx;
            y += dy;
            dy += 0.1;
            ttl--;
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, ttl / 100));
        }

        public void draw(GraphicsContext gc) {
            gc.setFill(color);
            gc.fillOval(x, y, 10, 10);
        }

        public boolean isAlive() {
            return ttl > 0;
        }
    }

    class DistributiveEmitter implements Emitter {
        private double x, y;
        private List<DistributiveParticle> particles = new ArrayList<>();
        private final int THREAD_COUNT = Math.min(4, Runtime.getRuntime().availableProcessors());

        public DistributiveEmitter(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public void emit(int count) {
            for (int i = 0; i < count; i++) {
                double dx = (Math.random() - 0.5) * 7;
                double dy = (Math.random() - 0.5) * 7;
                particles.add(new DistributiveParticle(x, y, dx, dy, 80));
            }
        }

        public void update() {
            ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
            int chunkSize = particles.size() / THREAD_COUNT + (particles.size() % THREAD_COUNT == 0 ? 0 : 1);
            List<DistributiveParticle> toRemove = new ArrayList<>();

            for (int i = 0; i < THREAD_COUNT; i++) {
                final int start = i * chunkSize;
                final int end = Math.min(start + chunkSize, particles.size());
                if (start < particles.size()) {
                    executor.submit(() -> {
                        for (int j = start; j < end; j++) {
                            DistributiveParticle p = particles.get(j);
                            p.update();
                            if (!p.isAlive()) synchronized (toRemove) { toRemove.add(p); }
                        }
                    });
                }
            }

            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                System.err.println("Distributive update interrupted: " + e.getMessage());
            }
            synchronized (particles) { particles.removeAll(toRemove); }
        }

        public void draw(GraphicsContext gc) {
            synchronized (particles) {
                for (DistributiveParticle p : particles) {
                    p.draw(gc);
                }
            }
        }

        public List<DistributiveParticle> getParticles() {
            return particles;
        }
    }

    class DistributiveParticle implements Particle {
        private double x, y, dx, dy, ttl;

        public DistributiveParticle(double x, double y, double dx, double dy, double ttl) {
            this.x = x;
            this.y = y;
            this.dx = dx;
            this.dy = dy;
            this.ttl = ttl;
        }

        public void update() {
            x += dx;
            y += dy;
            dy += 0.1;
            ttl = Math.max(0, ttl - 1);
        }

        public void draw(GraphicsContext gc) {
            if (ttl > 0) {
                double alpha = ttl / 80.0;
                gc.setFill(Color.rgb(255, 0, 0, alpha));
                gc.fillOval(x, y, 5, 5);
            }
        }

        public boolean isAlive() {
            return ttl > 0;
        }
    }
}
