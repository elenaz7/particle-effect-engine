package distrbutive.src;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public class DistributiveWorker {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 5001)) {
            ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream in = new ObjectInputStream(socket.getInputStream());

            while (true) {
                @SuppressWarnings("unchecked")
                List<DistributiveParticle> particles = (List<DistributiveParticle>) in.readObject();
                System.out.println("Worker received: " + particles.size() + " particles");
                for (DistributiveParticle p : particles) {
                    p.update();
                }
                out.writeObject(particles);
                out.flush();
                System.out.println("Worker sent: " + particles.size() + " particles");
            }
        } catch (Exception e) {
            System.err.println("Worker error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}