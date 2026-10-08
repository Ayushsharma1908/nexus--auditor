package com.nexuscomply;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;

import java.net.InetSocketAddress;
import java.util.Map;

public class MongoTestInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static MongoServer mongoServer;
    private static String mongoUri;

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        synchronized (MongoTestInitializer.class) {
            if (mongoServer == null) {
                mongoServer = new MongoServer(new MemoryBackend());
                InetSocketAddress address = mongoServer.bind();
                mongoUri = "mongodb://" + address.getHostString() + ":" + address.getPort() + "/nexus_comply_test";
                System.setProperty("spring.data.mongodb.uri", mongoUri);
                System.setProperty("MONGODB_URI", mongoUri);
                System.setProperty("SPRING_DATA_MONGODB_URI", mongoUri);
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    if (mongoServer != null) {
                        mongoServer.shutdown();
                    }
                }));
            }
        }

        applicationContext.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("inMemoryMongoTestProperties", Map.of(
                        "spring.data.mongodb.uri", mongoUri,
                        "MONGODB_URI", mongoUri,
                        "SPRING_DATA_MONGODB_URI", mongoUri
                ))
        );
    }
}
