package sequentilal.src;

import javafx.scene.canvas.GraphicsContext;
import java.util.ArrayList;
import java.util.List;

public class SequentialEmitter {
    private final double x;
    private final double y;
    protected List<SequentialParticle> particles;

    public SequentialEmitter(double x, double y) {
        this.x = x;
        this.y = y;
        this.particles = new ArrayList<>();
    }

    public void emit(int count) {
        for (int i = 0; i < count; i++) {
            double dx = (Math.random() - 0.5) * 7;
            double dy = (Math.random() - 0.5) * 7;
            double ttl = 80;
            particles.add(new SequentialParticle(x, y, dx, dy, ttl));
        }
    }

    public void update() {
        for (int i = particles.size() - 1; i >= 0; i--) {
            SequentialParticle p = particles.get(i);
            p.update();
            if (!p.isAlive()) {
                particles.remove(i);
            }
        }
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