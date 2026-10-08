package com.nexuscomply.simulation.repository;

import com.nexuscomply.simulation.model.Simulation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulationRepository extends MongoRepository<Simulation, String> {
}
