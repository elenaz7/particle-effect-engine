package sequentilal.src;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;

public class SequentialMain extends Application {
    private SequentialEmitter emiter;
    private List<Integer> particleCounts = new ArrayList<>();

    @Override
    public void start(Stage theWindow) {
        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        emiter = new SequentialEmitter(400, 250);

        new Thread(() -> {
            while (true) {
                emiter.emit(25);
                emiter.update();
                particleCounts.add(emiter.getParticles().size());
                gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                emiter.draw(gc);

                try {
                    Thread.sleep(16);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        StackPane root = new StackPane();
        root.getChildren().addAll(canvas);
        theWindow.setScene(new Scene(root));
        theWindow.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}