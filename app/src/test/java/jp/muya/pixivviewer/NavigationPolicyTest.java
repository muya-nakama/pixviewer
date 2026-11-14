package jp.muya.pixivviewer;
import org.junit.Test;
import static org.junit.Assert.*;
public class NavigationPolicyTest {
 private static final String LOGIN="https://accounts.pixiv.net/login";
 @Test public void xLoginAndReturnRemainInside(){
  assertEquals(NavigationPolicy.Route.INTERNAL,NavigationPolicy.route("https://api.twitter.com/oauth/authorize?oauth_token=test",true,LOGIN));
  assertEquals(NavigationPolicy.Route.INTERNAL,NavigationPolicy.route("https://api.x.com/oauth/authorize",true,"https://api.twitter.com/oauth/authorize"));
  assertEquals(NavigationPolicy.Route.INTERNAL,NavigationPolicy.route("https://accounts.pixiv.net/callback?code=test",true,"https://api.x.com/oauth/authorize"));
  assertEquals(NavigationPolicy.Route.INTERNAL,NavigationPolicy.route("https://www.pixiv.net/",true,LOGIN));
 }
 @Test public void embeddedChallengesStayInside(){
  assertEquals(NavigationPolicy.Route.INTERNAL,NavigationPolicy.route("https://www.google.com/recaptcha/api2/anchor",false,LOGIN));
  assertEquals(NavigationPolicy.Route.INTERNAL,NavigationPolicy.route("https://www.recaptcha.net/recaptcha/api2/anchor",false,LOGIN));
 }
 @Test public void unsupportedAuthNeverLeaksToBrowser(){
  assertEquals(NavigationPolicy.Route.GOOGLE_UNSUPPORTED,NavigationPolicy.route("https://accounts.google.com/o/oauth2/auth?state=test",true,LOGIN));
  assertEquals(NavigationPolicy.Route.AUTH_UNSUPPORTED,NavigationPolicy.route("https://appleid.apple.com/auth/authorize",true,LOGIN));
 }
 @Test public void rejectSpoofedHostsAndUnsafeSchemes(){
  assertEquals(NavigationPolicy.Route.AUTH_UNSUPPORTED,NavigationPolicy.route("https://api.x.com.evil.test/",true,LOGIN));
  assertEquals(NavigationPolicy.Route.BLOCK,NavigationPolicy.route("https://user@api.x.com/",true,LOGIN));
  assertEquals(NavigationPolicy.Route.BLOCK,NavigationPolicy.route("http://api.x.com/",true,LOGIN));
  assertEquals(NavigationPolicy.Route.BLOCK,NavigationPolicy.route("intent://token",true,LOGIN));
  assertEquals(NavigationPolicy.Route.EXTERNAL,NavigationPolicy.route("https://example.com/",true,"https://www.pixiv.net/"));
 }
 @Test public void thirdPartyCookiesOnlyDuringAuth(){
  assertTrue(NavigationPolicy.isAuth(LOGIN));assertTrue(NavigationPolicy.isAuth("https://api.x.com/oauth/authorize"));
  assertFalse(NavigationPolicy.isAuth("https://www.pixiv.net/"));assertFalse(NavigationPolicy.isAuth("https://accounts.pixiv.net.evil.test/"));
 }
}
