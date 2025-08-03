package distrbutive.src;

import java.io.Serializable;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class DistributiveParticle implements Serializable {
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