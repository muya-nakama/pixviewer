package jp.muya.pixivviewer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;

final class AppUpdater {
 private final Activity activity;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private volatile boolean cancelled=false,closed=false;
 private volatile HttpURLConnection connection;
 private boolean busy=false,awaitingPermission=false;
 private ProgressDialog progress;
 private final File apk;
 AppUpdater(Activity a){activity=a;apk=new File(a.getFilesDir(),"updates/pixviewer.apk");}
 void save(Bundle state){state.putBoolean("updatePermission",awaitingPermission);}
 void restore(Bundle state){awaitingPermission=state.getBoolean("updatePermission",false);}
 private void ui(Runnable task){activity.runOnUiThread(()->{if(!closed&&!activity.isFinishing()&&!activity.isDestroyed())task.run();});}
 private void message(String text){new AlertDialog.Builder(activity).setTitle("pixviewer の更新").setMessage(text).setPositiveButton("OK",null).show();}
 private void begin(String title,boolean determinate){
  busy=true;cancelled=false;progress=new ProgressDialog(activity);progress.setTitle(title);
  progress.setMessage("通信中…");progress.setProgressStyle(determinate?ProgressDialog.STYLE_HORIZONTAL:ProgressDialog.STYLE_SPINNER);
  progress.setMax(100);progress.setCancelable(false);progress.setButton(AlertDialog.BUTTON_NEGATIVE,"中止",(d,w)->cancel());progress.show();
 }
 private void cancel(){cancelled=true;HttpURLConnection c=connection;if(c!=null)c.disconnect();}
 private void done(){busy=false;if(progress!=null){progress.dismiss();progress=null;}}
 private void checkCancelled()throws IOException{if(cancelled||closed)throw new IOException("中止しました");}
 void check(){
  if(busy){Toast.makeText(activity,"更新処理中です",Toast.LENGTH_SHORT).show();return;}
  begin("更新を確認中",false);
  worker.execute(()->{
   try {
    JSONObject json=new JSONObject(new String(read(UpdatePolicy.MANIFEST,128*1024),StandardCharsets.UTF_8));
    long version=json.getLong("versionCode");String name=json.getString("versionName");
    String url=json.getString("apkUrl"),hash=json.getString("sha256");long bytes=json.getLong("size");
    String notes=json.optString("notes","更新版が公開されています。");
    if(!activity.getPackageName().equals(json.getString("applicationId")) || !UpdatePolicy.trustedAsset(url)
     || !UpdatePolicy.validHash(hash) || bytes<=0 || bytes>UpdatePolicy.MAX_APK_BYTES || version<=0)
     throw new IOException("更新情報の形式が正しくありません。");
    checkCancelled();
    ui(()->{done();if(cancelled)return;
     if(version<=BuildConfig.VERSION_CODE){message("最新版です（"+BuildConfig.VERSION_NAME+"）。");return;}
     new AlertDialog.Builder(activity).setTitle("pixviewer "+name).setMessage(notes+"\n\n現在："+BuildConfig.VERSION_NAME+"\n容量：約"+Math.max(1,bytes/1024/1024)+" MB")
      .setNegativeButton("あとで",null).setPositiveButton("APKをダウンロード",(d,w)->download(url,hash,bytes,version)).show();
    });
   }catch(Exception e){fail(e);}
  });
 }
 private void download(String url,String hash,long size,long version){
  if(busy)return;begin("APKをダウンロード",true);
  worker.execute(()->{
   File temp=new File(apk.getParentFile(),"download.part");
   try {
    if(!apk.getParentFile().isDirectory()&&!apk.getParentFile().mkdirs())throw new IOException("保存先を作成できません。");
    HttpURLConnection c=open(url);MessageDigest digest=MessageDigest.getInstance("SHA-256");long count=0;int previous=-1;
    try(InputStream in=c.getInputStream();OutputStream out=new FileOutputStream(temp)){
     byte[] buffer=new byte[32768];int n;
     while((n=in.read(buffer))!=-1){checkCancelled();count+=n;if(count>size)throw new IOException("APKのサイズが一致しません。");out.write(buffer,0,n);digest.update(buffer,0,n);
      int percent=(int)(count*100/size);if(percent!=previous){previous=percent;ui(()->{if(progress!=null){progress.setMessage("ダウンロード中");progress.setProgress(percent);}});}
     }
    }finally{c.disconnect();connection=null;}
    if(count!=size || !UpdatePolicy.hex(digest.digest()).equalsIgnoreCase(hash))throw new IOException("APKの検証に失敗しました。もう一度ダウンロードしてください。");
    validateApk(temp,version);checkCancelled();
    java.nio.file.Files.move(temp.toPath(),apk.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    ui(()->{done();if(cancelled)return;new AlertDialog.Builder(activity).setTitle("ダウンロード完了").setMessage("検証が完了しました。Androidの画面で更新を確定してください。ログイン情報や設定を引き継いで上書きします。")
     .setNegativeButton("あとで",null).setPositiveButton("更新をインストール",(d,w)->install()).show();});
   }catch(Exception e){temp.delete();fail(e);}
  });
 }
 private void validateApk(File file,long expectedVersion)throws Exception{
  PackageManager pm=activity.getPackageManager();
  PackageInfo candidate=pm.getPackageArchiveInfo(file.getAbsolutePath(),PackageManager.GET_SIGNING_CERTIFICATES);
  PackageInfo current=pm.getPackageInfo(activity.getPackageName(),PackageManager.GET_SIGNING_CERTIFICATES);
  if(candidate==null || !activity.getPackageName().equals(candidate.packageName) || candidate.getLongVersionCode()<=current.getLongVersionCode()
   || (expectedVersion>0 && candidate.getLongVersionCode()!=expectedVersion) || candidate.signingInfo==null || current.signingInfo==null)
   throw new IOException("このAPKはpixviewerの更新版ではありません。");
  Set<String> own=new HashSet<>(),next=new HashSet<>();
  for(Signature s:current.signingInfo.getApkContentsSigners())own.add(s.toCharsString());
  for(Signature s:candidate.signingInfo.getApkContentsSigners())next.add(s.toCharsString());
  if(own.isEmpty()||!own.equals(next))throw new IOException("APKの署名が一致しません。更新を中止しました。");
 }
 void installDownloaded(){if(busy){Toast.makeText(activity,"更新処理中です",Toast.LENGTH_SHORT).show();return;}install();}
 private void install(){
  if(!apk.isFile()){message("APKがありません。更新確認から再ダウンロードしてください。");return;}
  if(!activity.getPackageManager().canRequestPackageInstalls()){
   new AlertDialog.Builder(activity).setTitle("初回のみ許可が必要です").setMessage("次の画面でpixviewerの「この提供元を許可」をONにして戻ってください。")
    .setNegativeButton("あとで",null).setPositiveButton("設定を開く",(d,w)->{try{awaitingPermission=true;activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName())));}catch(Exception e){awaitingPermission=false;message("端末の設定からpixviewerのアプリインストールを許可してください。");}}).show();return;
  }
  try {
   validateApk(apk,0);
   Uri uri=FileProvider.getUriForFile(activity,activity.getPackageName()+".updates",apk);
   Intent intent=new Intent(Intent.ACTION_VIEW).setDataAndType(uri,"application/vnd.android.package-archive").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
   intent.setClipData(android.content.ClipData.newRawUri("pixviewer APK",uri));activity.startActivity(intent);
  }catch(Exception e){message("インストールを開始できませんでした："+e.getMessage());}
 }
 void onResume(){if(awaitingPermission){awaitingPermission=false;if(activity.getPackageManager().canRequestPackageInstalls())install();else message("インストールは許可されていません。更新確認からやり直せます。");}}
 private HttpURLConnection open(String url)throws IOException{
  for(int redirects=0;redirects<6;redirects++){
   checkCancelled();if(!UpdatePolicy.trustedTransfer(url))throw new IOException("許可されていない配信先です。");
   HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();connection=c;c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(20000);
   c.setRequestProperty("User-Agent","pixviewer/"+BuildConfig.VERSION_NAME);c.setRequestProperty("Cache-Control","no-cache");
   int code=c.getResponseCode();
   if(code==301||code==302||code==303||code==307||code==308){String target=c.getHeaderField("Location");c.disconnect();if(target==null)throw new IOException("更新先を確認できません。");url=new URL(new URL(url),target).toString();continue;}
   if(code!=200){c.disconnect();if(code==404)throw new IOException("更新版はまだ公開されていません。");if(code==403||code==429)throw new IOException("接続が制限されています。時間をおいて再試行してください。");throw new IOException("通信エラー HTTP "+code);}
   return c;
  }
  throw new IOException("更新先への転送回数が多すぎます。");
 }
 private byte[] read(String url,int limit)throws IOException{
  HttpURLConnection c=open(url);try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
   byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){checkCancelled();if(out.size()+n>limit)throw new IOException("更新情報が大きすぎます。");out.write(b,0,n);}return out.toByteArray();
  }finally{c.disconnect();connection=null;}
 }
 private void fail(Exception e){ui(()->{done();if(!cancelled)message("更新できませんでした。\n"+e.getMessage());});}
 void close(){closed=true;cancel();worker.shutdownNow();if(progress!=null)progress.dismiss();}
}
