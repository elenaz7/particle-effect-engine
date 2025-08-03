package distrbutive.src;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class DistributiveMaster extends Application {
    private DistributiveEmitter emiter;
    private List<DistributiveParticle> particles;
    private int workerCount = 2;
    private List<Integer> particleCounts = new ArrayList<>();

    @Override
    public void start(Stage theWindow) {
        Canvas canvas = new Canvas(800, 600);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        emiter = new DistributiveEmitter(400, 250);
        particles = new ArrayList<>();
        ImageView chartView = new ImageView();

        new Thread(() -> {
            try (ServerSocket server = new ServerSocket(5001)) {
                List<Socket> workers = new ArrayList<>();
                List<ObjectOutputStream> outs = new ArrayList<>();
                List<ObjectInputStream> ins = new ArrayList<>();
                for (int i = 0; i < workerCount; i++) {
                    Socket worker = server.accept();
                    workers.add(worker);
                    outs.add(new ObjectOutputStream(worker.getOutputStream()));
                    ins.add(new ObjectInputStream(worker.getInputStream()));
                    System.out.println("Connected to worker " + i);
                }

                while (true) {
                    particles.clear();
                    emiter.emit(100);
                    particles = emiter.getParticles();
                    int chunkSize = particles.size() / workerCount;
                    List<Thread> threads = new ArrayList<>();
                    for (int i = 0; i < workerCount; i++) {
                        final int workerIndex = i;
                        final int start = workerIndex * chunkSize;
                        final int end = (workerIndex == workerCount - 1) ? particles.size() : start + chunkSize;
                        final ObjectOutputStream out = outs.get(workerIndex);
                        final ObjectInputStream in = ins.get(workerIndex);
                        threads.add(new Thread(() -> {
                            try {
                                List<DistributiveParticle> subset = new ArrayList<>(particles.subList(start, end));
                                out.writeObject(subset);
                                out.flush();
                                @SuppressWarnings("unchecked")
                                List<DistributiveParticle> updated = (List<DistributiveParticle>) in.readObject();
                                for (int j = 0; j < updated.size(); j++) {
                                    particles.set(start + j, updated.get(j));
                                }
                            } catch (Exception e) {
                                System.err.println("Worker " + workerIndex + " error: " + e.getMessage());
                                try {
                                    out.reset();
                                } catch (Exception resetEx) {
                                    resetEx.printStackTrace();
                                }
                            }
                        }));
                        threads.get(workerIndex).start();
                    }
                    for (Thread t : threads) {
                        try {
                            t.join(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    particleCounts.add(particles.size());
                    gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
                    emiter.draw(gc);

                    try {
                        Thread.sleep(16);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } catch (Exception e) {
                System.err.println("Master error: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();

        StackPane root = new StackPane();
        root.getChildren().addAll(canvas, chartView);
        theWindow.setScene(new Scene(root));
        theWindow.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}