package parallel.src;

import javafx.scene.canvas.GraphicsContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ParallelEmitter {
    private final double x;
    private final double y;
    protected List<ParallelParticle> particles;
    private final int THREAD_COUNT = Runtime.getRuntime().availableProcessors();

    public ParallelEmitter(double x, double y) {
        this.x = x;
        this.y = y;
        this.particles = new ArrayList<>();
    }

    public void emit(int count) {
        for (int i = 0; i < count; i++) {
            double dx = (Math.random() - 0.5) * 7;
            double dy = (Math.random() - 0.5) * 7;
            double ttl = 80;
            particles.add(new ParallelParticle(x, y, dx, dy, ttl));
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
                final int threadId = i;
                executor.submit(() -> {
                    System.out.println("Thread " + threadId + " updating from " + start + " to " + end);
                    for (int j = start; j < end; j++) {
                        ParallelParticle p = particles.get(j);
                        p.update();
                        if (!p.isAlive()) {
                            synchronized (toRemove) {
                                toRemove.add(p);
                            }
                        }
                    }
                });
            }
        }

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        synchronized (particles) {
            particles.removeAll(toRemove);
        }
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