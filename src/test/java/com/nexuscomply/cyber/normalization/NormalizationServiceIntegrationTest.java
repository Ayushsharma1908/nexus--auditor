package com.nexuscomply.cyber.normalization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mongodb.client.MongoClients;
import com.nexuscomply.cyber.parser.ParserService;
import com.nexuscomply.cyber.parser.ParserServiceImpl;
import com.nexuscomply.cyber.parser.cisco.CiscoIosParser;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.repository.support.MongoRepositoryFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class NormalizationServiceIntegrationTest {

    private static MongoServer mongoServer;
    private static MongoTemplate mongoTemplate;

    private NormalizedConfigurationRepository repository;
    private NormalizationService normalizationService;
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startInMemoryMongo() {
        mongoServer = new MongoServer(new MemoryBackend());
        InetSocketAddress serverAddress = mongoServer.bind();
        String connectionString = "mongodb://" + serverAddress.getHostString() + ":" + serverAddress.getPort() + "/testdb";
        SimpleMongoClientDatabaseFactory factory = new SimpleMongoClientDatabaseFactory(MongoClients.create(connectionString), "testdb");
        mongoTemplate = new MongoTemplate(factory);
    }

    @AfterAll
    static void stopInMemoryMongo() {
        if (mongoServer != null) {
            mongoServer.shutdown();
        }
    }

    @BeforeEach
    void setUp() {
        mongoTemplate.dropCollection(NormalizedConfigurationDocument.class);

        MongoRepositoryFactory repositoryFactory = new MongoRepositoryFactory(mongoTemplate);
        repository = repositoryFactory.getRepository(NormalizedConfigurationRepository.class);

        ParserService parserService = new ParserServiceImpl(List.of(new CiscoIosParser()));
        normalizationService = new NormalizationServiceImpl(parserService, repository);

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("Normalizes Cisco config and persists document to MongoDB matching schema1.md section 5")
    void testNormalizeAndPersistRoundTrip() throws Exception {
        String rawConfig = String.join("\n",
                "version 17.9.4a",
                "hostname RTR-01",
                "!",
                "ip ssh version 2",
                "ip http secure-server",
                "transport input ssh",
                "snmp-server community secret RO version 3",
                "aaa new-model",
                "logging on",
                "logging host 10.0.0.5",
                "logging buffered 16384",
                "ntp server 10.0.0.1",
                "!"
        );

        String deviceId = "device-uuid-101";
        String configurationId = "config-uuid-202";
        String versionId = "version-uuid-303";
        String vendor = "Cisco";
        String platform = "IOS-XE";
        String osVersion = "17.9.4a";

        // Persist through service
        NormalizedConfigurationDocument savedDoc = normalizationService.normalizeAndPersist(
                rawConfig, deviceId, configurationId, versionId, vendor, platform, osVersion
        );

        assertThat(savedDoc).isNotNull();
        assertThat(savedDoc.getId()).isNotBlank();
        assertThat(savedDoc.getDeviceId()).isEqualTo(deviceId);
        assertThat(savedDoc.getConfigurationId()).isEqualTo(configurationId);
        assertThat(savedDoc.getVersionId()).isEqualTo(versionId);
        assertThat(savedDoc.getVendor()).isEqualTo("Cisco");
        assertThat(savedDoc.getPlatform()).isEqualTo("IOS-XE");
        assertThat(savedDoc.getOsVersion()).isEqualTo("17.9.4a");
        assertThat(savedDoc.getSchemaVersion()).isEqualTo("1.0");
        assertThat(savedDoc.getNormalizationStatus()).isEqualTo("COMPLETED");
        assertThat(savedDoc.getCreatedAt()).isNotNull();
        assertThat(savedDoc.getUpdatedAt()).isNotNull();

        // Verify roundtrip read from MongoDB
        Optional<NormalizedConfigurationDocument> retrievedOpt = normalizationService.getById(savedDoc.getId());
        assertThat(retrievedOpt).isPresent();

        NormalizedConfigurationDocument retrievedDoc = retrievedOpt.get();
        assertThat(retrievedDoc.getVersionId()).isEqualTo(versionId);

        // Verify JSON serialization shape against schema1.md section 5
        String json = objectMapper.writeValueAsString(retrievedDoc);
        System.out.println("=== PART 5C: RAW SAVED DOCUMENT JSON ===");
        System.out.println(json);
        @SuppressWarnings("unchecked")
        Map<String, Object> jsonMap = objectMapper.readValue(json, Map.class);

        assertThat(jsonMap).containsKeys("id", "deviceId", "configurationId", "versionId", "vendor", "platform", "osVersion", "schemaVersion", "canonical", "sourceMap", "unknowns", "normalizationStatus", "createdAt", "updatedAt");

        @SuppressWarnings("unchecked")
        Map<String, Object> canonicalJson = (Map<String, Object>) jsonMap.get("canonical");
        assertThat(canonicalJson).containsKeys("security", "authentication", "logging", "ntp");

        @SuppressWarnings("unchecked")
        Map<String, Object> securityJson = (Map<String, Object>) canonicalJson.get("security");
        @SuppressWarnings("unchecked")
        Map<String, Object> sshJson = (Map<String, Object>) securityJson.get("ssh");
        assertThat(sshJson.get("version")).isEqualTo(2);
        assertThat(sshJson.get("enabled")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        List<?> sourceMapList = (List<?>) jsonMap.get("sourceMap");
        assertThat(sourceMapList).isNotEmpty();
    }
}
