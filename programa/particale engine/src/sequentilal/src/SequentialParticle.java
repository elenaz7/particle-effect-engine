package sequentilal.src;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class SequentialParticle {
    private double x, y;
    private final double dx;
    private double dy;
    private double ttl;
    private Color color;

    public SequentialParticle(double x, double y, double dx, double dy, double ttl) {
        this.x = x;
        this.y = y;
        this.dx = dx;
        this.dy = dy;
        this.ttl = ttl;
        this.color = Color.HOTPINK;
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