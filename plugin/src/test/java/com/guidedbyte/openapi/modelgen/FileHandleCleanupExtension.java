package com.guidedbyte.openapi.modelgen;

import com.guidedbyte.openapi.modelgen.services.RichFileLogger;
import org.junit.jupiter.api.extension.*;

/**
 * JUnit extension that ensures proper file handle cleanup to prevent
 * Windows temp directory deletion issues during tests.
 *
 * <p>This extension runs at multiple points in the test lifecycle to ensure
 * all RichFileLogger instances are closed before JUnit attempts to clean up
 * temp directories.</p>
 */
public class FileHandleCleanupExtension implements BeforeEachCallback, AfterEachCallback,
                                                  BeforeAllCallback, AfterAllCallback,
                                                  TestInstancePreDestroyCallback {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        // Close any existing file handles from previous tests
        RichFileLogger.closeAll();
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        performCleanup("afterEach");
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        // Clean up any leftover handles from previous test runs
        RichFileLogger.closeAll();
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        performCleanup("afterAll");
    }

    @Override
    public void preDestroyTestInstance(ExtensionContext context) throws Exception {
        // This runs just before the test instance is destroyed, which might be
        // the right timing to close files before temp directory cleanup
        performCleanup("preDestroyTestInstance");
    }

    /**
     * Performs aggressive file handle cleanup with multiple strategies.
     */
    private void performCleanup(String phase) throws InterruptedException {
        // Close all file handles before JUnit temp directory cleanup
        RichFileLogger.closeAll();

        // On Windows, be more aggressive about ensuring file handles are released
        if (org.apache.commons.lang3.SystemUtils.IS_OS_WINDOWS) {
            // Force garbage collection to help release file handles
            System.runFinalization();
            System.gc();

            // Give Windows additional time to release file handles
            // This is necessary because Windows file handle release can be asynchronous
            Thread.sleep(150);
        } else {
            // On other platforms, a shorter wait should suffice
            Thread.sleep(50);
        }

        // Debug output to see which phase is actually working
        System.out.println("FileHandleCleanup: " + phase + " completed");
    }
}