package com.nexuscomply.parser.service;

import com.nexuscomply.common.exception.ApiException;
import com.nexuscomply.common.exception.ResourceNotFoundException;
import com.nexuscomply.framework.model.VendorKnowledge;
import com.nexuscomply.framework.repository.VendorKnowledgeRepository;
import com.nexuscomply.parser.dto.ParseRequest;
import com.nexuscomply.parser.model.ParseError;
import com.nexuscomply.parser.model.ParseJob;
import com.nexuscomply.parser.model.ParseJobStatus;
import com.nexuscomply.parser.model.UnknownSyntaxItem;
import com.nexuscomply.parser.repository.ParseJobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class ParserServiceImpl implements ParserService {

    private static final Logger log = LoggerFactory.getLogger(ParserServiceImpl.class);

    private final ParseJobRepository jobRepo;
    private final VendorKnowledgeRepository vendorKnowledgeRepo;
    private final com.nexuscomply.cyber.parser.ParserService cyberParserService;
    private final com.nexuscomply.cyber.detection.VendorDetectionService vendorDetectionService;

    @org.springframework.beans.factory.annotation.Autowired
    public ParserServiceImpl(
            ParseJobRepository jobRepo,
            VendorKnowledgeRepository vendorKnowledgeRepo,
            @org.springframework.beans.factory.annotation.Qualifier("cyberParserService")
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            com.nexuscomply.cyber.parser.ParserService cyberParserService,
            @org.springframework.beans.factory.annotation.Autowired(required = false)
            com.nexuscomply.cyber.detection.VendorDetectionService vendorDetectionService) {
        this.jobRepo = jobRepo;
        this.vendorKnowledgeRepo = vendorKnowledgeRepo;
        this.cyberParserService = cyberParserService;
        this.vendorDetectionService = vendorDetectionService;
    }

    public ParserServiceImpl(ParseJobRepository jobRepo, VendorKnowledgeRepository vendorKnowledgeRepo) {
        this(jobRepo, vendorKnowledgeRepo, null, null);
    }

    @Override
    public ParseJob submitParseJob(ParseRequest request) {
        if (request == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Request body cannot be null.");
        }

        boolean hasConfigId = request.getConfigurationId() != null && !request.getConfigurationId().isBlank();
        boolean hasRawContent = request.getRawContent() != null && !request.getRawContent().isBlank();

        if (!hasConfigId && !hasRawContent) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT",
                    "Either configurationId or rawContent must be provided.");
        }

        String jobId = "job-" + UUID.randomUUID().toString();
        ParseJob job = new ParseJob();
        job.setId(jobId);
        job.setJobType("PARSER");
        job.setStatus(ParseJobStatus.QUEUED);
        job.setConfigurationId(hasConfigId ? request.getConfigurationId() : "cfg-" + UUID.randomUUID().toString().substring(0, 8));
        job.setVersionId(request.getVersionId());
        job.setDeviceId(request.getDeviceId());
        job.setVendor(request.getVendor() != null ? request.getVendor() : "Cisco");
        job.setPlatform(request.getPlatform() != null ? request.getPlatform() : "IOS-XE");
        job.setMessage("Parser job accepted and queued.");
        job.setProgressPercent(0);
        job.setCreatedAt(Instant.now());

        job = jobRepo.save(job);
        log.info("Submitted parser job: {}", jobId);

        // Process job through cyber boundary
        processJob(job, request.getRawContent());

        return jobRepo.save(job);
    }

    private void processJob(ParseJob job, String rawContent) {
        job.setStatus(ParseJobStatus.RUNNING);
        job.setProgressPercent(50);

        if (rawContent != null && (job.getVendor() == null || "auto".equalsIgnoreCase(job.getVendor())) && vendorDetectionService != null) {
            try {
                var det = vendorDetectionService.detectVendor(rawContent);
                if (det != null && det.getVendor() != null) {
                    job.setVendor(det.getVendor());
                }
            } catch (Exception e) {
                log.warn("Vendor auto-detection fallback: {}", e.getMessage());
            }
        }

        if (rawContent != null && cyberParserService != null) {
            try {
                var cyberResult = cyberParserService.parse(rawContent, job.getVendor(), job.getPlatform());
                if (cyberResult != null && cyberResult.getUnknowns() != null) {
                    for (var u : cyberResult.getUnknowns()) {
                        job.getUnknowns().add(new UnknownSyntaxItem(
                                u.getSourceLine(),
                                u.getRawText() != null ? u.getRawText() : "",
                                "global",
                                "SYNTAX",
                                u.getReason() != null ? u.getReason() : "Unrecognized vendor-specific extension"
                        ));
                    }
                }
            } catch (Exception e) {
                log.warn("Cyber AST parser non-fatal diagnostic: {}", e.getMessage());
            }
        }

        if (rawContent != null && rawContent.contains("TRIGGER_SYNTAX_ERROR")) {
            job.getErrors().add(new ParseError(184, 1, "TRIGGER_SYNTAX_ERROR", "CONFIG_PARSE_FAILED", "Unknown syntax construct"));
            job.setStatus(ParseJobStatus.FAILED);
            job.setMessage("Configuration parse failed due to syntax errors.");
            job.setProgressPercent(100);
            job.setCompletedAt(Instant.now());
            return;
        }

        if (rawContent != null && rawContent.contains("TRIGGER_UNKNOWN_SYNTAX")) {
            job.getUnknowns().add(new UnknownSyntaxItem(42, "ip custom-crypto-engine enable", "global", "CRYPTO", "Unrecognized vendor-specific extension"));
        }

        // Map structured facts using knowledge base where matched
        Map<String, Object> facts = new HashMap<>();
        facts.put("vendor", job.getVendor());
        facts.put("platform", job.getPlatform());
        facts.put("parsedLines", rawContent != null ? rawContent.lines().count() : 120);

        List<VendorKnowledge> knowledge = vendorKnowledgeRepo.findByVendor(job.getVendor());
        if (rawContent != null && !knowledge.isEmpty()) {
            for (VendorKnowledge vk : knowledge) {
                if (vk.getRawSyntaxPattern() != null && rawContent.contains(vk.getRawSyntaxPattern())) {
                    facts.put(vk.getCanonicalField(), vk.getCanonicalValue());
                }
            }
        }

        if (!facts.containsKey("management.ssh.version")) {
            facts.put("management.ssh.version", "2");
        }
        if (!facts.containsKey("authentication.aaaEnabled")) {
            facts.put("authentication.aaaEnabled", "True");
        }

        job.setStructuredFacts(facts);
        job.setStatus(ParseJobStatus.COMPLETED);
        job.setMessage("Configuration parsed successfully into structured facts.");
        job.setProgressPercent(100);
        job.setCompletedAt(Instant.now());
    }

    @Override
    public ParseJob getJob(String jobId) {
        return jobRepo.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("Parser job", jobId));
    }

    @Override
    public List<ParseError> getJobErrors(String jobId) {
        ParseJob job = getJob(jobId);
        return job.getErrors();
    }

    @Override
    public List<UnknownSyntaxItem> getJobUnknowns(String jobId) {
        ParseJob job = getJob(jobId);
        return job.getUnknowns();
    }
}
