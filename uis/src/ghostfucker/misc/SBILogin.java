package ghostfucker.misc;

/**
 * @deprecated SB-Innovation tracker login — REMOVED.
 * <p>
 * This class authenticated against {@code www.sb-innovation.de/login.php} for the
 * SB-Innovation (SBI) private tracker. It has been deprecated and gutted because:
 * <ul>
 *   <li>The SBI tracker endpoint returns HTTP 403 and is no longer operational (verified 2026-03-01).</li>
 *   <li>The feature was already permanently disabled: {@code deactivateLogin} was hardcoded to {@code true},
 *       and {@code TOTorrentChecker.fuckSeba()} re-set it to {@code true} on every startup.</li>
 *   <li>The SSL implementation used a trust-all certificate manager (MITM vulnerability).</li>
 *   <li>No configuration UI was ever wired for SBILoginName / SBILoginPassword / SBILoginSSL.</li>
 *   <li>No menu integration existed.</li>
 * </ul>
 * <p>
 * Static fields are retained as no-ops for binary compatibility with any code that
 * references {@code SBILogin.loginValid} or {@code SBILogin.deactivateLogin}.
 * <p>
 * Config keys that can be cleaned up from {@code COConfigurationManager}:
 * {@code SBILoginName}, {@code SBILoginPassword}, {@code SBILoginSSL}.
 *
 * @since Extreme Mod (deprecated 2026-03-01)
 */
@Deprecated
public class SBILogin implements Runnable {

   /** Always {@code false} — login is permanently disabled. */
   public static final boolean loginValid = false;

   /** Always {@code true} — login is permanently deactivated. */
   public static final boolean deactivateLogin = true;

   /** Always empty — no login result will ever be produced. */
   public static final String loginResult = "";

   /**
    * @deprecated No-op constructor retained for source compatibility.
    */
   @Deprecated
   public SBILogin(Runnable sbiBlock) {
      // no-op: SBI tracker is defunct
   }

   /**
    * No-op. SBI tracker authentication has been removed.
    */
   @Override
   public void run() {
      // no-op: SBI tracker is defunct
   }

   /**
    * Converts raw MD5 bytes to a lowercase hex string.
    * Retained as a utility — this is the only non-dead functionality.
    *
    * @deprecated Use {@code String.format("%032x", new java.math.BigInteger(1, md5))} instead.
    */
   @Deprecated
   public static String md5ToString(byte[] md5) {
      StringBuilder result = new StringBuilder();
      for (int i = 0; i < md5.length; i++) {
         String hex = Integer.toHexString(md5[i] & 0xFF);
         if (hex.length() == 1) {
            result.append("0");
         }
         result.append(hex);
      }
      return result.toString();
   }
}
