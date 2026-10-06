package com.nexuscomply.cyber.simulation;

/**
 * Service contract for What-If Simulation Engine (cyberlayer.pdf Section 20, 33 item 16, Criterion 10).
 */
public interface WhatIfSimulationService {

    /**
     * Executes a What-If impact simulation against a base configuration version.
     * Evaluates simulated canonical model in-memory without modifying audit history or core collections.
     */
    WhatIfSimulationResult simulate(WhatIfSimulationRequest request);
}
