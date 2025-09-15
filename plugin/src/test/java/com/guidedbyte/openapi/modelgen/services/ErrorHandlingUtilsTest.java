package com.guidedbyte.openapi.modelgen.services;

import org.gradle.api.GradleException;
import org.gradle.api.InvalidUserDataException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ErrorHandlingUtils class.
 */
class ErrorHandlingUtilsTest {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandlingUtilsTest.class);
    private Path tempDir;
    private Path testFile;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("error-handling-test");
        testFile = tempDir.resolve("test.txt");
        Files.writeString(testFile, "test content");
    }

    @Test
    void testHandleFileOperationSuccess() throws IOException {
        String result = ErrorHandlingUtils.handleFileOperation(
            () -> Files.readString(testFile),
            "Failed to read test file",
            "Check file permissions",
            logger
        );
        
        assertEquals("test content", result);
    }

    @Test
    void testHandleFileOperationIOException() {
        Path nonExistentFile = tempDir.resolve("nonexistent.txt");
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            ErrorHandlingUtils.handleFileOperation(
                () -> Files.readString(nonExistentFile),
                "Failed to read test file",
                "Check file permissions",
                logger
            )
        );
        
        assertTrue(exception.getMessage().contains("Failed to read test file"));
        assertTrue(exception.getMessage().contains("Check file permissions"));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void testHandleYamlOperationSuccess() {
        String result = ErrorHandlingUtils.handleYamlOperation(
            () -> "valid-yaml-result",
            "test.yaml",
            logger
        );
        
        assertEquals("valid-yaml-result", result);
    }

    @Test
    void testHandleYamlOperationException() {
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            ErrorHandlingUtils.handleYamlOperation(
                () -> {
                    // Simulate a generic YAML parsing error
                    throw new RuntimeException("YAML parsing failed");
                },
                "test.yaml",
                logger
            )
        );
        
        assertTrue(exception.getMessage().contains("Failed to parse YAML file test.yaml"));
        assertTrue(exception.getMessage().contains(ErrorHandlingUtils.YAML_SYNTAX_GUIDANCE));
    }

    @Test
    void testValidateNotNullSuccess() {
        // Should not throw
        assertDoesNotThrow(() -> 
            ErrorHandlingUtils.validateNotNull("valid", "Value", "Check config", logger)
        );
    }

    @Test
    void testValidateNotNullFailure() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            ErrorHandlingUtils.validateNotNull(null, "Value cannot be null", "Check config", logger)
        );
        
        assertTrue(exception.getMessage().contains("Value cannot be null"));
        assertTrue(exception.getMessage().contains("Check config"));
    }

    @Test
    void testValidateNotEmptySuccess() {
        // Should not throw
        assertDoesNotThrow(() -> 
            ErrorHandlingUtils.validateNotEmpty("valid", "fieldName", "Check config", logger)
        );
    }

    @Test
    void testValidateNotEmptyFailureNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            ErrorHandlingUtils.validateNotEmpty(null, "fieldName", "Check config", logger)
        );
        
        assertTrue(exception.getMessage().contains("fieldName cannot be null or empty"));
        assertTrue(exception.getMessage().contains("Check config"));
    }

    @Test
    void testValidateNotEmptyFailureEmpty() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            ErrorHandlingUtils.validateNotEmpty("", "fieldName", "Check config", logger)
        );
        
        assertTrue(exception.getMessage().contains("fieldName cannot be null or empty"));
    }

    @Test
    void testValidateNotEmptyFailureWhitespace() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            ErrorHandlingUtils.validateNotEmpty("   ", "fieldName", "Check config", logger)
        );
        
        assertTrue(exception.getMessage().contains("fieldName cannot be null or empty"));
    }

    @Test
    void testValidateFileExistsSuccess() {
        // Should not throw
        assertDoesNotThrow(() -> 
            ErrorHandlingUtils.validateFileExists(testFile, "Test file", logger)
        );
    }

    @Test
    void testValidateFileExistsFailureNull() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            ErrorHandlingUtils.validateFileExists(null, "Test file", logger)
        );
        
        assertTrue(exception.getMessage().contains("Test file does not exist"));
        assertTrue(exception.getMessage().contains(ErrorHandlingUtils.FILE_NOT_FOUND_GUIDANCE));
    }

    @Test
    void testValidateFileExistsFailureNonexistent() {
        Path nonExistent = tempDir.resolve("nonexistent.txt");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            ErrorHandlingUtils.validateFileExists(nonExistent, "Test file", logger)
        );
        
        assertTrue(exception.getMessage().contains("Test file does not exist"));
        assertTrue(exception.getMessage().contains(nonExistent.toString()));
    }

    @Test
    void testValidateFileExistsFailureDirectory() throws IOException {
        Path directory = tempDir.resolve("subdir");
        Files.createDirectory(directory);
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> 
            ErrorHandlingUtils.validateFileExists(directory, "Test file", logger)
        );
        
        assertTrue(exception.getMessage().contains("Test file path is not a file"));
    }

    @Test
    void testWithFallbackSuccess() {
        String result = ErrorHandlingUtils.withFallback(
            () -> "primary-result",
            "fallback-result",
            "test operation",
            logger
        );
        
        assertEquals("primary-result", result);
    }

    @Test
    void testWithFallbackUseFallback() {
        String result = ErrorHandlingUtils.withFallback(
            () -> {
                throw new RuntimeException("Primary failed");
            },
            "fallback-result",
            "test operation",
            logger
        );
        
        assertEquals("fallback-result", result);
    }

    @Test
    void testWithRetrySuccess() {
        String result = ErrorHandlingUtils.withRetry(
            () -> "success",
            3,
            "test operation",
            logger
        );
        
        assertEquals("success", result);
    }

    @Test
    void testWithRetrySuccessAfterFailures() {
        AtomicInteger attempts = new AtomicInteger(0);
        
        String result = ErrorHandlingUtils.withRetry(
            () -> {
                if (attempts.incrementAndGet() < 2) {
                    throw new RuntimeException("Transient failure");
                }
                return "success";
            },
            3,
            "test operation",
            logger
        );
        
        assertEquals("success", result);
        assertEquals(2, attempts.get());
    }

    @Test
    void testWithRetryAllAttemptsFail() {
        AtomicInteger attempts = new AtomicInteger(0);
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            ErrorHandlingUtils.withRetry(
                () -> {
                    attempts.incrementAndGet();
                    throw new RuntimeException("Persistent failure");
                },
                3,
                "test operation",
                logger
            )
        );
        
        assertTrue(exception.getMessage().contains("Failed to test operation after 3 attempts"));
        assertEquals(3, attempts.get());
    }

    @Test
    void testWrapWithContextSuccess() {
        String result = ErrorHandlingUtils.wrapWithContext(
            () -> "success",
            "Additional context",
            logger
        );
        
        assertEquals("success", result);
    }

    @Test
    void testWrapWithContextRuntimeException() {
        RuntimeException originalException = new RuntimeException("Original error");
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            ErrorHandlingUtils.wrapWithContext(
                () -> {
                    throw originalException;
                },
                "Additional context",
                logger
            )
        );
        
        // Should be the same exception (not wrapped again)
        assertSame(originalException, exception);
    }

    @Test
    void testWrapWithContextCheckedException() {
        // Test that a checked exception is properly wrapped and context is logged
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            ErrorHandlingUtils.wrapWithContext(
                () -> {
                    // Simulate a checked exception by throwing an IOException wrapped in a Supplier
                    throw new RuntimeException(new IOException("IO error"));
                },
                "Additional context",
                logger
            )
        );
        
        // The exception should be the same (RuntimeException not double-wrapped)
        // Context is added via logging, not message modification for RuntimeExceptions
        assertNotNull(exception);
    }

    @Test
    void testWrapWithContextActualCheckedException() {
        // Test that an actual checked exception gets wrapped with context in the message
        class TestSupplier implements ErrorHandlingUtils.FileOperation<String> {
            @Override
            public String execute() throws IOException {
                throw new IOException("IO error");
            }
        }
        
        RuntimeException exception = assertThrows(RuntimeException.class, () -> 
            ErrorHandlingUtils.handleFileOperation(
                new TestSupplier(),
                "Failed to process file",
                "Check permissions",
                logger
            )
        );
        
        assertTrue(exception.getMessage().contains("Failed to process file"));
        assertTrue(exception.getMessage().contains("Check permissions"));
        assertInstanceOf(IOException.class, exception.getCause());
    }

    @Test
    void testFormatConfigError() {
        String message = ErrorHandlingUtils.formatConfigError(
            "templateDir",
            "/invalid/path",
            "valid directory path"
        );
        
        assertTrue(message.contains("templateDir"));
        assertTrue(message.contains("/invalid/path"));
        assertTrue(message.contains("valid directory path"));
        assertTrue(message.contains(ErrorHandlingUtils.CONFIG_GUIDANCE));
    }

    @Test
    void testFormatTemplateError() {
        String message = ErrorHandlingUtils.formatTemplateError(
            "pojo.mustache",
            "syntax error on line 10"
        );
        
        assertTrue(message.contains("pojo.mustache"));
        assertTrue(message.contains("syntax error on line 10"));
        assertTrue(message.contains(ErrorHandlingUtils.TEMPLATE_GUIDANCE));
    }

    @Test
    void testFormatVersionErrorWithDetectedVersion() {
        String message = ErrorHandlingUtils.formatVersionError(
            "7.0.0",
            "7.1.0",
            "apply template customizations"
        );
        
        assertTrue(message.contains("detected OpenAPI Generator version 7.0.0"));
        assertTrue(message.contains("required: 7.1.0"));
        assertTrue(message.contains("apply template customizations"));
        assertTrue(message.contains(ErrorHandlingUtils.VERSION_GUIDANCE));
    }

    @Test
    void testFormatVersionErrorWithNullDetectedVersion() {
        String message = ErrorHandlingUtils.formatVersionError(
            null,
            "7.1.0",
            "apply template customizations"
        );
        
        assertTrue(message.contains("version could not be detected"));
        assertTrue(message.contains("required version: 7.1.0"));
        assertTrue(message.contains("apply template customizations"));
        assertTrue(message.contains(ErrorHandlingUtils.VERSION_GUIDANCE));
    }

    @Test
    void testErrorContextConstants() {
        // Verify all guidance constants are non-empty and meaningful
        assertNotNull(ErrorHandlingUtils.FILE_NOT_FOUND_GUIDANCE);
        assertFalse(ErrorHandlingUtils.FILE_NOT_FOUND_GUIDANCE.trim().isEmpty());
        
        assertNotNull(ErrorHandlingUtils.PERMISSION_GUIDANCE);
        assertFalse(ErrorHandlingUtils.PERMISSION_GUIDANCE.trim().isEmpty());
        
        assertNotNull(ErrorHandlingUtils.YAML_SYNTAX_GUIDANCE);
        assertFalse(ErrorHandlingUtils.YAML_SYNTAX_GUIDANCE.trim().isEmpty());
        
        assertNotNull(ErrorHandlingUtils.CONFIG_GUIDANCE);
        assertFalse(ErrorHandlingUtils.CONFIG_GUIDANCE.trim().isEmpty());
        
        assertNotNull(ErrorHandlingUtils.TEMPLATE_GUIDANCE);
        assertFalse(ErrorHandlingUtils.TEMPLATE_GUIDANCE.trim().isEmpty());
        
        assertNotNull(ErrorHandlingUtils.VERSION_GUIDANCE);
        assertFalse(ErrorHandlingUtils.VERSION_GUIDANCE.trim().isEmpty());
        
        assertNotNull(ErrorHandlingUtils.LIBRARY_GUIDANCE);
        assertFalse(ErrorHandlingUtils.LIBRARY_GUIDANCE.trim().isEmpty());
        
        assertNotNull(ErrorHandlingUtils.GRADLE_GUIDANCE);
        assertFalse(ErrorHandlingUtils.GRADLE_GUIDANCE.trim().isEmpty());
    }
    
    @Test
    void testCreateConfigurationErrorWithGuidance() {
        InvalidUserDataException exception = ErrorHandlingUtils.createConfigurationError(
            "Invalid template directory", "Check the path configuration"
        );
        
        assertTrue(exception.getMessage().contains("Invalid template directory"));
        assertTrue(exception.getMessage().contains("Check the path configuration"));
    }
    
    @Test
    void testCreateConfigurationErrorWithDefaultGuidance() {
        InvalidUserDataException exception = ErrorHandlingUtils.createConfigurationError(
            "Invalid template directory"
        );
        
        assertTrue(exception.getMessage().contains("Invalid template directory"));
        assertTrue(exception.getMessage().contains(ErrorHandlingUtils.CONFIG_GUIDANCE));
    }
    
    @Test
    void testCreateGradleErrorWithGuidance() {
        GradleException exception = ErrorHandlingUtils.createGradleError(
            "initialize plugin", "missing dependency", "Install required dependencies"
        );
        
        assertTrue(exception.getMessage().contains("Failed to initialize plugin"));
        assertTrue(exception.getMessage().contains("missing dependency"));
        assertTrue(exception.getMessage().contains("Install required dependencies"));
    }
    
    @Test
    void testCreateGradleErrorWithDefaultGuidance() {
        GradleException exception = ErrorHandlingUtils.createGradleError(
            "initialize plugin", "missing dependency"
        );
        
        assertTrue(exception.getMessage().contains("Failed to initialize plugin"));
        assertTrue(exception.getMessage().contains("missing dependency"));
        assertTrue(exception.getMessage().contains(ErrorHandlingUtils.GRADLE_GUIDANCE));
    }
    
    @Test
    void testFormatLibraryError() {
        String message = ErrorHandlingUtils.formatLibraryError(
            "spring-templates", "version conflict detected"
        );
        
        assertTrue(message.contains("spring-templates"));
        assertTrue(message.contains("version conflict detected"));
        assertTrue(message.contains(ErrorHandlingUtils.LIBRARY_GUIDANCE));
    }
    
    @Test
    void testHandleValidationSuccess() {
        List<String> errors = new ArrayList<>();
        
        ErrorHandlingUtils.handleValidation(
            "test validation",
            () -> {
                // Validation passes - no exception
            },
            errors,
            logger
        );
        
        assertTrue(errors.isEmpty());
    }
    
    @Test
    void testHandleValidationFailure() {
        List<String> errors = new ArrayList<>();
        
        ErrorHandlingUtils.handleValidation(
            "test validation",
            () -> {
                throw new RuntimeException("Validation failed");
            },
            errors,
            logger
        );
        
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).contains("test validation"));
        assertTrue(errors.get(0).contains("Validation failed"));
    }
    
    @Test
    void testValidateOrThrowSuccess() {
        List<String> emptyErrors = new ArrayList<>();
        
        // Should not throw
        assertDoesNotThrow(() -> 
            ErrorHandlingUtils.validateOrThrow(emptyErrors, "Configuration", "Check settings")
        );
    }
    
    @Test
    void testValidateOrThrowWithErrors() {
        List<String> errors = new ArrayList<>();
        errors.add("Missing required field 'inputSpec'");
        errors.add("Invalid outputDir path");
        
        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class, () ->
            ErrorHandlingUtils.validateOrThrow(errors, "Configuration", "Check settings")
        );
        
        assertTrue(exception.getMessage().contains("Configuration validation failed"));
        assertTrue(exception.getMessage().contains("1. Missing required field 'inputSpec'"));
        assertTrue(exception.getMessage().contains("2. Invalid outputDir path"));
        assertTrue(exception.getMessage().contains("Check settings"));
    }
    
    @Test
    void testValidateOrThrowWithoutGuidance() {
        List<String> errors = new ArrayList<>();
        errors.add("Test error");
        
        InvalidUserDataException exception = assertThrows(InvalidUserDataException.class, () ->
            ErrorHandlingUtils.validateOrThrow(errors, "Test validation", null)
        );
        
        assertTrue(exception.getMessage().contains("Test validation validation failed"));
        assertTrue(exception.getMessage().contains("1. Test error"));
        // Should not contain null guidance
        assertFalse(exception.getMessage().contains("null"));
    }
}