package jp.muya.pixivviewer;

import android.app.*;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
 private WebView web;
 private AppUpdater updater;
 private TextView status;
 private LinearLayout follows;
 private static final String HOME="https://www.pixiv.net/";
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  updater=new AppUpdater(this);
  if(state!=null)updater.restore(state);
  LinearLayout root=new LinearLayout(this); root.setOrientation(1); root.setBackgroundColor(Color.WHITE);
  root.setOnApplyWindowInsetsListener((v,i)->{ android.graphics.Insets x=i.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout()); v.setPadding(x.left,x.top,x.right,x.bottom); return i; });
  setContentView(root);
  LinearLayout toolbar=new LinearLayout(this); root.addView(toolbar);
  button(toolbar,"検索",()->search()); button(toolbar,"人気順",()->sort()); button(toolbar,"設定",()->settings());
  status=new TextView(this); status.setTextSize(12); status.setPadding(16,4,16,4); root.addView(status);
  follows=new LinearLayout(this); follows.setVisibility(View.GONE); root.addView(follows);
  button(follows,"公開フォロー",()->follow(false)); button(follows,"🔒 非公開フォロー",()->follow(true));
  web=new WebView(this); root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
  WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(false); s.setAllowContentAccess(false); s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  CookieManager.getInstance().setAcceptCookie(true); CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){
    Uri u=r.getUrl(); String h=u.getHost();
    if("https".equals(u.getScheme()) && ("www.pixiv.net".equals(h)||"accounts.pixiv.net".equals(h))) return false;
    if(r.isForMainFrame() && ("https".equals(u.getScheme())||"http".equals(u.getScheme()))) { try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception e){toast("対応するブラウザがありません");} }
    return true;
   }
   @Override public void onPageStarted(WebView v,String url,android.graphics.Bitmap icon){status.setText("読み込み中 · "+Uri.parse(url).getHost());}
   @Override public void onPageFinished(WebView v,String url){status.setText("非公式ビューア · "+Uri.parse(url).getHost()); CookieManager.getInstance().flush();}
   @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())status.setText("読み込めませんでした。設定 → 再読み込み");}
  });
  LinearLayout nav=new LinearLayout(this); root.addView(nav);
  button(nav,"ホーム",()->{follows.setVisibility(View.GONE); web.loadUrl(HOME);});
  button(nav,"新着",()->{follows.setVisibility(View.GONE); web.loadUrl(HOME+"bookmark_new_illust.php");});
  button(nav,"フォロー",()->{follows.setVisibility(View.VISIBLE);follow(false);});
  if(state==null || web.restoreState(state)==null)web.loadUrl(HOME);
 }
 private void button(LinearLayout row,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setTextSize(12);row.addView(b,new LinearLayout.LayoutParams(0,(int)(48*getResources().getDisplayMetrics().density),1));b.setOnClickListener(v->action.run());}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 private void search(){
  LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(24,8,24,8);
  EditText query=new EditText(this);query.setSingleLine(true);query.setHint("タグ・タイトルなど");box.addView(query);
  Spinner kind=new Spinner(this);kind.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"イラスト・漫画","イラスト","漫画","小説"}));box.addView(kind);
  Spinner match=new Spinner(this);match.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,new String[]{"タグ部分一致","タグ完全一致","タイトル・キャプション"}));box.addView(match);
  new AlertDialog.Builder(this).setTitle("作品検索").setView(box).setNegativeButton("閉じる",null).setPositiveButton("検索",(d,w)->{
   String q=query.getText().toString().trim();if(q.isEmpty())return;
   String[] types={"artworks","illustrations","manga","novels"};String[] modes={"s_tag","s_tag_full","s_tc"};
   follows.setVisibility(View.GONE);web.loadUrl(HOME+"tags/"+Uri.encode(q)+"/"+types[kind.getSelectedItemPosition()]+"?s_mode="+modes[match.getSelectedItemPosition()]+"&order=date_d");
  }).show();
 }
 private void follow(boolean hidden){
  String id=getPreferences(0).getString("userId","");
  if(id.isEmpty()){userId();return;}
  web.loadUrl(HOME+"users/"+id+"/following?rest="+(hidden?"hide":"show"));
 }
 private void userId(){EditText id=new EditText(this);id.setInputType(2);id.setHint("自分のプロフィールURLの数字");id.setText(getPreferences(0).getString("userId",""));new AlertDialog.Builder(this).setTitle("自分のpixivユーザーID").setMessage("公開／非公開を正しく開くため、ログイン中の自分のIDを指定してください。アカウント変更時は更新が必要です。").setView(id).setNegativeButton("閉じる",null).setPositiveButton("保存",(d,w)->{String value=id.getText().toString().trim();if(value.matches("[1-9][0-9]*")){getPreferences(0).edit().putString("userId",value).apply();follow(false);}else toast("数字のユーザーIDを入力してください");}).show();}
 private void settings(){new AlertDialog.Builder(this).setTitle("設定").setItems(new String[]{"アプリの更新（"+BuildConfig.VERSION_NAME+"）","公式ログイン画面","自分のユーザーID","再読み込み","端末のログイン情報を消去","ダウンロード済みの更新をインストール"},(d,w)->{switch(w){case 5:updater.installDownloaded();break;case 0:updater.check();break;case 1:web.loadUrl("https://accounts.pixiv.net/login");break;case 2:userId();break;case 3:web.reload();break;case 4:new AlertDialog.Builder(this).setMessage("このアプリのログイン情報とキャッシュを消去しますか？").setNegativeButton("キャンセル",null).setPositiveButton("消去",(a,b)->{web.stopLoading();web.loadUrl("about:blank");CookieManager.getInstance().removeAllCookies(ok->{CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();web.clearCache(true);web.clearHistory();getPreferences(0).edit().clear().apply();web.loadUrl(HOME);});}).show();}}).show();}
 private void sort(){
  String url=web.getUrl();if(url==null || !"www.pixiv.net".equals(Uri.parse(url).getHost()) || !Uri.parse(url).getPath().startsWith("/tags/")){toast("作品の検索結果を開いてから選んでください");return;}
  new AlertDialog.Builder(this).setTitle("取得範囲内の人気順（無料）").setMessage("現在の検索条件で新着から最大60作品を取得し、ブックマーク数順に並べます。全検索結果の人気順ではありません。小説は初版では未対応です。").setNegativeButton("閉じる",null).setPositiveButton("並べ替える",(d,w)->{try(java.io.InputStream in=getAssets().open("sort.js")){web.evaluateJavascript(new String(in.readAllBytes(),StandardCharsets.UTF_8),null);}catch(Exception e){toast("並べ替え処理を読み込めませんでした");}}).show();
 }
 @Override protected void onSaveInstanceState(Bundle s){super.onSaveInstanceState(s);web.saveState(s);updater.save(s);}
 @Override public void onBackPressed(){web.evaluateJavascript("!!document.getElementById('muya-sort')",result->{if("true".equals(result))web.evaluateJavascript("document.getElementById('muya-sort').dispatchEvent(new Event('muya-close'))",null);else if(web.canGoBack())web.goBack();else finish();});}
 @Override protected void onResume(){super.onResume();if(updater!=null)updater.onResume();}
 @Override protected void onDestroy(){updater.close();web.destroy();super.onDestroy();}
}
