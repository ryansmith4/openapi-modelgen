package com.guidedbyte.openapi.modelgen.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A file-based logger that writes rich contextual information to a dedicated log file,
 * bypassing Gradle's logging limitations.
 * 
 * <p>This logger creates detailed logs with full MDC context that users can monitor
 * in real-time or review after build completion. The log file includes:</p>
 * <ul>
 *   <li>Timestamp</li>
 *   <li>Log level</li>  
 *   <li>Component name</li>
 *   <li>Spec being processed</li>
 *   <li>Template being processed (if applicable)</li>
 *   <li>Detailed message</li>
 * </ul>
 * 
 * <p>Log file location: {@code build/logs/openapi-modelgen-debug.log}</p>
 * 
 * @author GuidedByte Technologies Inc.
 * @since 2.1.0
 */
public class RichFileLogger {
    private static final Logger logger = LoggerFactory.getLogger(RichFileLogger.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final Map<String, RichFileLogger> instances = new ConcurrentHashMap<>();
    
    private final File logFile;
    private FileWriter writer;
    private boolean enabled;
    
    private RichFileLogger(File logFile) {
        this.logFile = logFile;
        this.enabled = initializeLogFile();
    }
    
    /**
     * Gets or creates a RichFileLogger for the specified build directory.
     * 
     * @param buildDir the build directory where the log file should be created
     * @return the RichFileLogger instance
     */
    public static RichFileLogger forBuildDir(File buildDir) {
        String key = buildDir.getAbsolutePath();
        return instances.computeIfAbsent(key, k -> {
            File logsDir = new File(buildDir, "logs");
            if (!logsDir.exists() && !logsDir.mkdirs()) {
                throw new RuntimeException("Failed to create logs directory: " + logsDir.getAbsolutePath());
            }
            File logFile = new File(logsDir, "openapi-modelgen-debug.log");
            return new RichFileLogger(logFile);
        });
    }
    
    /**
     * Logs a debug message with full MDC context to the file.
     * 
     * @param message the message to log
     * @param args optional message arguments
     */
    public void debug(String message, Object... args) {
        if (enabled) {
            writeLogEntry("DEBUG", formatMessage(message, args));
        }
    }
    
    /**
     * Logs an info message with full MDC context to the file.
     * 
     * @param message the message to log
     * @param args optional message arguments  
     */
    public void info(String message, Object... args) {
        if (enabled) {
            writeLogEntry("INFO", formatMessage(message, args));
        }
    }
    
    /**
     * Logs a warning message with full MDC context to the file.
     * 
     * @param message the message to log
     * @param args optional message arguments
     */
    public void warn(String message, Object... args) {
        if (enabled) {
            writeLogEntry("WARN", formatMessage(message, args));
        }
    }
    
    /**
     * Logs an error message with full MDC context to the file.
     * 
     * @param message the message to log
     * @param args optional message arguments
     */
    public void error(String message, Object... args) {
        if (enabled) {
            writeLogEntry("ERROR", formatMessage(message, args));
        }
    }
    
    /**
     * Writes a section header to help organize the log file.
     * 
     * @param section the section name
     */
    public void section(String section) {
        if (enabled && writer != null) {
            try {
                writeRawLine("");
                writeRawLine("=== " + section + " ===");
                writer.flush();
            } catch (IOException e) {
                logger.warn("Failed to write section header: {}", e.getMessage());
                enabled = false;
            }
        }
    }
    
    /**
     * Closes the log file and flushes any remaining content.
     */
    public void close() {
        if (writer != null) {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                logger.warn("Failed to close rich log file: {}", e.getMessage());
            }
        }
    }
    
    private boolean initializeLogFile() {
        try {
            writer = new FileWriter(logFile, StandardCharsets.UTF_8, false); // Overwrite existing file
            writeRawLine("OpenAPI Model Generator Rich Debug Log");
            writeRawLine("Started: " + LocalDateTime.now().format(TIMESTAMP_FORMAT));
            writeRawLine("Log file: " + logFile.getAbsolutePath());
            writeRawLine("");
            writer.flush();
            
            // Add shutdown hook to ensure file is closed
            Runtime.getRuntime().addShutdownHook(new Thread(this::close));
            
            return true;
        } catch (IOException e) {
            logger.warn("Failed to initialize rich debug log file at {}: {}", 
                logFile.getAbsolutePath(), e.getMessage());
            return false;
        }
    }
    
    private synchronized void writeLogEntry(String level, String message) {
        if (writer == null) return;
        
        try {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMAT);
            String component = LoggingContext.getCurrentComponent();
            String spec = LoggingContext.getCurrentSpec();
            String template = LoggingContext.getCurrentTemplate();
            
            StringBuilder entry = new StringBuilder();
            entry.append(timestamp)
                 .append(" [").append(level).append("]");
            
            if (component != null) {
                entry.append(" [").append(component).append("]");
            }
            
            if (spec != null) {
                entry.append(" [").append(spec);
                if (template != null) {
                    entry.append(":").append(template);
                }
                entry.append("]");
            }
            
            entry.append(" - ").append(message);
            
            writeRawLine(entry.toString());
            writer.flush(); // Ensure immediate visibility
        } catch (IOException e) {
            logger.warn("Failed to write to rich debug log: {}", e.getMessage());
            enabled = false; // Disable logging on write failure
        }
    }
    
    private void writeRawLine(String line) throws IOException {
        writer.write(line);
        writer.write(System.lineSeparator());
    }
    
    private String formatMessage(String message, Object... args) {
        if (args.length == 0) {
            return message;
        }
        
        try {
            return String.format(message.replace("{}", "%s"), args);
        } catch (Exception e) {
            // Fallback if formatting fails
            return message + " " + java.util.Arrays.toString(args);
        }
    }
}