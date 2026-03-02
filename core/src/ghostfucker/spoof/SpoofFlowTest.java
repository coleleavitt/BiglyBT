package ghostfucker.spoof;

import ghostfucker.spoof.client.PSClient;
import ghostfucker.spoof.client.Client;
import ghostfucker.spoof.client.ClientXmlParser;
import com.biglybt.core.peermanager.messaging.bittorrent.BTHandshake;

import java.io.File;
import java.util.Arrays;

/**
 * Simple test to verify the spoofing flow works correctly.
 * Run this after building to verify all components are wired up properly.
 * 
 * Usage: java -cp <classpath> ghostfucker.spoof.SpoofFlowTest
 */
public class SpoofFlowTest {

    public static void main(String[] args) {
        System.out.println("=== PerfectSpoof Flow Test ===\n");

        int passed = 0;
        int failed = 0;

        // Test 1: Verify PSClient can be instantiated
        System.out.println("Test 1: PSClient instantiation");
        try {
            PSClient client = new PSClient();
            System.out.println("  ✓ PSClient instantiated successfully");
            passed++;
        } catch (Exception e) {
            System.out.println("  ✗ PSClient instantiation failed: " + e.getMessage());
            failed++;
        }

        // Test 2: Verify client files directory can be found
        System.out.println("\nTest 2: Client files directory discovery");
        File clientFilesDir = findClientFilesDir();
        if (clientFilesDir != null && clientFilesDir.exists()) {
            System.out.println("  ✓ Found clientfiles at: " + clientFilesDir.getAbsolutePath());
            passed++;
        } else {
            System.out.println("  ✗ Could not find clientfiles directory");
            failed++;
        }

        // Test 3: Verify qBitTorrent client files exist
        System.out.println("\nTest 3: qBitTorrent client profile existence");
        if (clientFilesDir != null) {
            File qbtDir = new File(clientFilesDir, "qBitTorrent");
            if (qbtDir.exists() && qbtDir.isDirectory()) {
                String[] versions = qbtDir.list((dir, name) -> name.endsWith(".client"));
                if (versions != null && versions.length > 0) {
                    System.out.println("  ✓ Found " + versions.length + " qBitTorrent versions");
                    System.out.println("    Sample: " + versions[0]);
                    passed++;
                } else {
                    System.out.println("  ✗ No .client files in qBitTorrent directory");
                    failed++;
                }
            } else {
                System.out.println("  ✗ qBitTorrent directory not found");
                failed++;
            }
        } else {
            System.out.println("  ✗ Skipped (no clientfiles dir)");
            failed++;
        }

        // Test 4: Parse a qBitTorrent client file
        System.out.println("\nTest 4: Client XML parsing");
        Client parsedClient = null;
        if (clientFilesDir != null) {
            File qbtDir = new File(clientFilesDir, "qBitTorrent");
            File[] clientFiles = qbtDir.listFiles((dir, name) -> name.endsWith(".client"));
            if (clientFiles != null && clientFiles.length > 0) {
                File testFile = clientFiles[0];
                try {
                    parsedClient = ClientXmlParser.parseClient(testFile);
                    System.out.println("  ✓ Parsed: " + testFile.getName());
                    System.out.println("    PeerId prefix: " + parsedClient.peerId.preFix);
                    passed++;
                } catch (Exception e) {
                    System.out.println("  ✗ Parse failed: " + e.getMessage());
                    failed++;
                }
            } else {
                System.out.println("  ✗ No client files to parse");
                failed++;
            }
        } else {
            System.out.println("  ✗ Skipped (no clientfiles dir)");
            failed++;
        }

        // Test 5: Initialize PSClient with parsed client
        System.out.println("\nTest 5: PSClient initialization with profile");
        PSClient psClient = null;
        if (parsedClient != null) {
            try {
                psClient = new PSClient();
                psClient.init(parsedClient, "qBitTorrent/test");
                byte[] peerId = psClient.getPeerId();
                String userAgent = psClient.getUserAgent();
                
                System.out.println("  ✓ PSClient initialized");
                System.out.println("    Peer ID (" + peerId.length + " bytes): " + new String(peerId));
                System.out.println("    User-Agent: " + userAgent);
                
                if (peerId.length == 20) {
                    System.out.println("    ✓ Peer ID length correct (20 bytes)");
                    passed++;
                } else {
                    System.out.println("    ✗ Peer ID length incorrect: " + peerId.length);
                    failed++;
                }
            } catch (Exception e) {
                System.out.println("  ✗ PSClient init failed: " + e.getMessage());
                e.printStackTrace();
                failed++;
            }
        } else {
            System.out.println("  ✗ Skipped (no parsed client)");
            failed++;
        }

        // Test 6: Verify BTHandshake.updateReservedBytes() works
        System.out.println("\nTest 6: BTHandshake reserved bytes update");
        try {
            byte[] beforeAz = BTHandshake.getAzReserved();
            System.out.println("  Before: " + Arrays.toString(beforeAz));
            
            BTHandshake.updateReservedBytes();
            
            byte[] afterAz = BTHandshake.getAzReserved();
            System.out.println("  After:  " + Arrays.toString(afterAz));
            System.out.println("  ✓ BTHandshake.updateReservedBytes() executed without error");
            passed++;
        } catch (Exception e) {
            System.out.println("  ✗ BTHandshake update failed: " + e.getMessage());
            failed++;
        }

        // Test 7: Verify dynamic PerfectSpoof.getClient() returns null when not active
        System.out.println("\nTest 7: PerfectSpoof.getClient() when inactive");
        try {
            PSClient dynamicClient = PerfectSpoof.getClient();
            boolean isActive = PerfectSpoof.isActive();
            System.out.println("  isActive: " + isActive);
            System.out.println("  getClient(): " + (dynamicClient != null ? "non-null" : "null"));
            
            if (!isActive && dynamicClient == null) {
                System.out.println("  ✓ Correctly returns null when not active");
                passed++;
            } else if (isActive && dynamicClient != null) {
                System.out.println("  ✓ Correctly returns client when active");
                passed++;
            } else {
                System.out.println("  ✗ Inconsistent state: isActive=" + isActive + " but client=" + dynamicClient);
                failed++;
            }
        } catch (Exception e) {
            System.out.println("  ✗ PerfectSpoof.getClient() failed: " + e.getMessage());
            failed++;
        }

        // Summary
        System.out.println("\n=== Test Summary ===");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total:  " + (passed + failed));
        
        if (failed == 0) {
            System.out.println("\n✓ All tests passed! Spoofing flow is working correctly.");
        } else {
            System.out.println("\n✗ Some tests failed. Review output above.");
            System.exit(1);
        }
    }

    /**
     * Find the clientfiles directory - same logic as PerfectSpoof.findClientFilesDir()
     */
    private static File findClientFilesDir() {
        // Check common locations
        String[] possiblePaths = {
            "clientfiles",
            "../clientfiles",
            "core/clientfiles",
            System.getProperty("user.dir") + "/clientfiles",
            System.getProperty("user.home") + "/.biglybt/clientfiles"
        };

        for (String path : possiblePaths) {
            File dir = new File(path);
            if (dir.exists() && dir.isDirectory()) {
                File qbt = new File(dir, "qBitTorrent");
                if (qbt.exists()) {
                    return dir;
                }
            }
        }

        // Try to find via classpath
        try {
            java.net.URL url = SpoofFlowTest.class.getProtectionDomain().getCodeSource().getLocation();
            File codeLocation = new File(url.toURI());
            File parent = codeLocation.getParentFile();
            while (parent != null) {
                File clientfiles = new File(parent, "clientfiles");
                if (clientfiles.exists() && clientfiles.isDirectory()) {
                    return clientfiles;
                }
                parent = parent.getParentFile();
            }
        } catch (Exception e) {
            // Ignore
        }

        return null;
    }
}
