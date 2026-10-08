package com.nexuscomply.parser.service;

import com.nexuscomply.parser.dto.ParseRequest;
import com.nexuscomply.parser.model.ParseError;
import com.nexuscomply.parser.model.ParseJob;
import com.nexuscomply.parser.model.UnknownSyntaxItem;

import java.util.List;

/**
 * Stable boundary interface for parser operations per Cyber Engine integration contract.
 */
public interface ParserService {
    ParseJob submitParseJob(ParseRequest request);
    ParseJob getJob(String jobId);
    List<ParseError> getJobErrors(String jobId);
    List<UnknownSyntaxItem> getJobUnknowns(String jobId);
}
