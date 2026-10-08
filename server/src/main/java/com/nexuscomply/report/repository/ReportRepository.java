package com.nexuscomply.report.repository;

import com.nexuscomply.report.model.Report;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends MongoRepository<Report, String> {
}
