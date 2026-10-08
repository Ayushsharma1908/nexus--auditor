package com.nexuscomply.parser.controller;

import com.nexuscomply.common.api.ApiResponse;
import com.nexuscomply.common.security.RequestIdFilter;
import com.nexuscomply.parser.dto.ParseErrorResponse;
import com.nexuscomply.parser.dto.ParseJobResponse;
import com.nexuscomply.parser.dto.ParseRequest;
import com.nexuscomply.parser.dto.UnknownSyntaxResponse;
import com.nexuscomply.parser.model.ParseError;
import com.nexuscomply.parser.model.ParseJob;
import com.nexuscomply.parser.model.UnknownSyntaxItem;
import com.nexuscomply.parser.service.ParserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cyber")
@Tag(name = "Parser", description = "Parser operations through ParserService boundary")
public class ParserController {

    private final ParserService parserService;

    public ParserController(ParserService parserService) {
        this.parserService = parserService;
    }

    /**
     * B-001: POST /api/v1/cyber/parse
     * Start parser operation through ParserService boundary.
     * Returns 202 Accepted with async jobId.
     */
    @PostMapping("/parse")
    @Operation(summary = "Start parser operation", description = "Submits a configuration to be parsed asynchronously through the ParserService boundary.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "Parser job accepted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation or input failure")
    })
    public ResponseEntity<ApiResponse<ParseJobResponse>> parseConfiguration(@RequestBody(required = false) ParseRequest request) {
        ParseJob job = parserService.submitParseJob(request);
        ParseJobResponse response = ParseJobResponse.fromJob(job);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.of(response, requestId));
    }

    /**
     * B-002: GET /api/v1/cyber/parse-jobs/{jobId}
     * Retrieve parser job status.
     */
    @GetMapping("/parse-jobs/{jobId}")
    @Operation(summary = "Retrieve parser job status", description = "Gets status and progress of an existing parser job.")
    public ResponseEntity<ApiResponse<ParseJobResponse>> getParseJob(@PathVariable("jobId") String jobId) {
        ParseJob job = parserService.getJob(jobId);
        ParseJobResponse response = ParseJobResponse.fromJob(job);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(response, requestId));
    }

    /**
     * B-003: GET /api/v1/cyber/parse-jobs/{jobId}/errors
     * Retrieve parser errors.
     */
    @GetMapping("/parse-jobs/{jobId}/errors")
    @Operation(summary = "Retrieve parser errors", description = "Gets safe diagnostic parser errors for a job.")
    public ResponseEntity<ApiResponse<ParseErrorResponse>> getParseErrors(@PathVariable("jobId") String jobId) {
        List<ParseError> errors = parserService.getJobErrors(jobId);
        ParseErrorResponse response = new ParseErrorResponse(jobId, errors);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(response, requestId));
    }

    /**
     * B-004: GET /api/v1/cyber/parse-jobs/{jobId}/unknowns
     * Retrieve unknown syntax items.
     */
    @GetMapping("/parse-jobs/{jobId}/unknowns")
    @Operation(summary = "Retrieve unknown syntax items", description = "Gets unrecognized configuration syntax constructs.")
    public ResponseEntity<ApiResponse<UnknownSyntaxResponse>> getParseUnknowns(@PathVariable("jobId") String jobId) {
        List<UnknownSyntaxItem> unknowns = parserService.getJobUnknowns(jobId);
        UnknownSyntaxResponse response = new UnknownSyntaxResponse(jobId, unknowns);
        String requestId = RequestIdFilter.getRequestId();
        return ResponseEntity.ok(ApiResponse.of(response, requestId));
    }
}
