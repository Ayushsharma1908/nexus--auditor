package com.nexuscomply;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

import java.net.InetSocketAddress;
import java.net.Socket;

@SpringBootApplication(exclude = {
        UserDetailsServiceAutoConfiguration.class
})
public class NexusComplyApplication {

    private static final Logger log = LoggerFactory.getLogger(NexusComplyApplication.class);
    private static MongoServer embeddedServer;

    public static void main(String[] args) {
        ensureMongoAvailable();
        SpringApplication.run(NexusComplyApplication.class, args);
    }

    private static void ensureMongoAvailable() {
        String mongoUri = System.getenv("SPRING_DATA_MONGODB_URI");
        if (mongoUri == null) {
            mongoUri = System.getenv("MONGODB_URI");
        }
        if (mongoUri == null || mongoUri.contains("localhost:27017") || mongoUri.contains("127.0.0.1:27017")) {
            if (!isPortListening("127.0.0.1", 27017)) {
                try {
                    embeddedServer = new MongoServer(new MemoryBackend());
                    embeddedServer.bind("127.0.0.1", 27017);
                    log.info("Started local embedded in-memory MongoDB on 127.0.0.1:27017");
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        if (embeddedServer != null) {
                            embeddedServer.shutdown();
                        }
                    }));
                } catch (Exception e) {
                    log.warn("Could not start embedded MongoDB: {}", e.getMessage());
                }
            } else {
                log.info("Detected active MongoDB listening on 127.0.0.1:27017");
            }
        }
    }

    private static boolean isPortListening(String host, int port) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 400);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}

