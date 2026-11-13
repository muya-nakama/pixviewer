package jp.muya.pixivviewer;
import org.junit.Test;
import static org.junit.Assert.*;
public class UpdatePolicyTest {
 @Test public void ownReleaseOnly(){
  assertTrue(UpdatePolicy.trustedAsset("https://github.com/muya-nakama/pixviewer/releases/download/v0.2.1/pixviewer.apk"));
  assertFalse(UpdatePolicy.trustedAsset("https://github.com/other/pixviewer/releases/download/v1/pixviewer.apk"));
  assertFalse(UpdatePolicy.trustedAsset("https://github.com.evil.test/muya-nakama/pixviewer/releases/download/v1/pixviewer.apk"));
  assertFalse(UpdatePolicy.trustedAsset("https://evil.test@github.com/muya-nakama/pixviewer/releases/download/v1/pixviewer.apk"));
  assertFalse(UpdatePolicy.trustedAsset("http://github.com/muya-nakama/pixviewer/releases/download/v1/pixviewer.apk"));
  assertFalse(UpdatePolicy.trustedAsset("https://github.com/muya-nakama/pixviewer/releases/download/../pixviewer.apk"));
  assertFalse(UpdatePolicy.trustedAsset("https://github.com/muya-nakama/pixviewer/releases/download/v1/pixviewer.apk?elsewhere=1"));
 }
 @Test public void redirectsRemainHttpsAndGitHub(){
  assertTrue(UpdatePolicy.trustedTransfer("https://release-assets.githubusercontent.com/a?token=x"));
  assertFalse(UpdatePolicy.trustedTransfer("https://example.com/pixviewer.apk"));
  assertFalse(UpdatePolicy.trustedTransfer("http://release-assets.githubusercontent.com/a"));
  assertFalse(UpdatePolicy.trustedTransfer("https://release-assets.githubusercontent.com:444/a"));
 }
 @Test public void hashFormatting(){
  assertEquals("000fff",UpdatePolicy.hex(new byte[]{0,15,(byte)255}));
  assertTrue(UpdatePolicy.validHash("a".repeat(64)));
  assertFalse(UpdatePolicy.validHash("a".repeat(63)));
  assertFalse(UpdatePolicy.validHash("z".repeat(64)));
  assertFalse(UpdatePolicy.validHash(null));
 }
}
