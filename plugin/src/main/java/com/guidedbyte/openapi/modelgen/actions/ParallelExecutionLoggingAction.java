package com.guidedbyte.openapi.modelgen.actions;


import org.gradle.api.Action;
import org.gradle.api.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serial;
import java.io.Serializable;

/**
 * Configuration-cache compatible logging action for parallel execution.
 * This action logs parallel execution information without capturing non-serializable references.
 */
public class ParallelExecutionLoggingAction implements Action<Task>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(ParallelExecutionLoggingAction.class);
    
    private final int specCount;
    private final boolean isParallel;
    private final String phase; // "start" or "end"
    
    public ParallelExecutionLoggingAction(int specCount, boolean isParallel, String phase) {
        this.specCount = specCount;
        this.isParallel = isParallel;
        this.phase = phase != null ? phase : "unknown";
    }
    
    @Override
    public void execute(Task task) {
        if ("start".equalsIgnoreCase(phase)) {
            logExecutionStart();
        } else if ("end".equalsIgnoreCase(phase)) {
            logExecutionEnd();
        }
    }
    
    private void logExecutionStart() {
        if (isParallel) {
            logger.info("Parallel processing enabled for {} specifications", specCount);
            
            int availableProcessors = Runtime.getRuntime().availableProcessors();
            logger.debug("System has {} available processors for parallel execution", availableProcessors);
        } else {
            logger.info("Sequential processing configured for {} specifications", specCount);
        }
    }
    
    private void logExecutionEnd() {
        if (isParallel) {
            logger.info("Parallel execution completed for {} specifications", specCount);
        } else {
            logger.debug("Sequential execution completed for {} specifications", specCount);
        }
    }
}