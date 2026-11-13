package jp.muya.pixivviewer;

import java.net.URI;
import java.util.Locale;

/** Network and identity rules shared by the updater and its tests. */
public final class UpdatePolicy {
 public static final String REPOSITORY = "muya-nakama/pixviewer";
 public static final String MANIFEST = "https://github.com/"+REPOSITORY+"/releases/latest/download/update.json";
 public static final long MAX_APK_BYTES = 100L * 1024 * 1024;
 private UpdatePolicy() {}
 public static boolean trustedAsset(String value) {
  try {
   URI u = URI.create(value);
   return "https".equals(u.getScheme()) && "github.com".equals(u.getHost())
    && u.getUserInfo()==null && u.getPort()==-1 && u.getQuery()==null && u.getFragment()==null
    && u.getPath().startsWith("/"+REPOSITORY+"/releases/download/")
    && u.getPath().endsWith("/pixviewer.apk") && !u.getPath().contains("..");
  } catch (IllegalArgumentException ex) { return false; }
 }
 public static boolean trustedTransfer(String value) {
  try {
   URI u=URI.create(value);String host=u.getHost();
   return "https".equals(u.getScheme()) && u.getUserInfo()==null && u.getPort()==-1
    && ("github.com".equals(host) || "release-assets.githubusercontent.com".equals(host)
      || "objects.githubusercontent.com".equals(host));
  } catch (IllegalArgumentException ex) { return false; }
 }
 public static boolean validHash(String hash) { return hash != null && hash.matches("[0-9a-fA-F]{64}"); }
 public static String hex(byte[] bytes) {
  StringBuilder result=new StringBuilder();for(byte b:bytes)result.append(String.format(Locale.ROOT,"%02x",b&255));return result.toString();
 }
}
