package jp.muya.pixivviewer;

import java.net.URI;
import java.util.Set;

/** Keep one authentication transaction in one WebView cookie store. */
final class NavigationPolicy {
 enum Route { INTERNAL, EXTERNAL, GOOGLE_UNSUPPORTED, AUTH_UNSUPPORTED, BLOCK }
 private static final Set<String> PIXIV=Set.of("pixiv.net","www.pixiv.net","accounts.pixiv.net","touch.pixiv.net");
 private static final Set<String> X=Set.of("api.twitter.com","api.x.com","twitter.com","www.twitter.com","mobile.twitter.com","x.com","www.x.com");
 static String host(String value){try{String h=URI.create(value).getHost();return h==null?"":h.toLowerCase(java.util.Locale.ROOT);}catch(Exception e){return "";}}
 static boolean isAuth(String value){String h=host(value);return h.equals("accounts.pixiv.net")||X.contains(h);}
 static boolean isPixiv(String value){return PIXIV.contains(host(value));}
 static Route route(String url,boolean mainFrame,String current){
  URI u;try{u=URI.create(url);}catch(Exception e){return Route.BLOCK;}
  if(!"https".equalsIgnoreCase(u.getScheme()) || u.getUserInfo()!=null || (u.getPort()!=-1&&u.getPort()!=443))return Route.BLOCK;
  // reCAPTCHA and other embedded HTTPS challenges must not be treated as app exits.
  if(!mainFrame)return Route.INTERNAL;
  String h=host(url);
  if(PIXIV.contains(h))return Route.INTERNAL;
  if(h.equals("accounts.google.com"))return Route.GOOGLE_UNSUPPORTED;
  if(X.contains(h)&&(isAuth(current)||isPixiv(current)))return Route.INTERNAL;
  // Do not leak a partially-started OAuth transaction to another cookie store.
  if(isAuth(current))return Route.AUTH_UNSUPPORTED;
  return Route.EXTERNAL;
 }
}
