# pixviewer

Android 13以降向けの非公式pixivビューア。アプリ名・リポジトリ名ともに **pixviewer**。

## 初回だけ行う署名設定
1. リポジトリ **Settings → Secrets and variables → Actions → New repository secret** を開く。
2. Name は `ANDROID_SIGNING`。
3. Secret に、別途渡した `pixviewer-signing-secret.txt` の全文を貼り付けて保存。
4. **Actions → Build and publish APK → Run workflow** を実行する。
5. 完了したら **Releases → 最新版 → pixviewer.apk** をダウンロードして初回インストール。

秘密鍵をソース・Issue・公開コメントに貼らないでください。`pixviewer-signing-secret.txt` は署名鍵とパスワードの非公開バックアップです。今後も同じ鍵を利用します。
署名設定がない場合、コンパイル・テスト・Lintのみを実行し、配布版は公開しません。unsigned APKはインストールできません。

## アプリから更新
**設定 → アプリの更新 → APKをダウンロード → 更新をインストール**。
初回のみ「この提供元を許可」をONにして戻り、Androidの確認画面で更新します。
アプリIDと署名を固定しているため、アンインストールせず更新できます。
ダウンロード中の中止・通信失敗は再試行可能。アプリを終了した場合のダウンロード途中再開は未対応です。
「あとで」を選んだAPKは「設定 → ダウンロード済みの更新をインストール」から利用できます。

## 開発・配信
- JDK17、Gradle8.9、Android SDK35。
- mainへのpushでコンパイル・ユニットテスト・Lintを実行。
- 署名設定済みなら、固定鍵で署名したAPKとupdate.jsonをGitHub Releasesへ自動配信。
- versionCodeは100 + Actions実行番号、versionNameは0.2.実行番号。同じ実行の再実行は同じ版。
- 作業ブランチからは公開しない。PRは署名鍵を使わず検証のみ。
- 更新はこのリポジトリの最新Releaseを確認し、HTTPS配信、サイズ・SHA256一致、アプリID・署名一致、新しいバージョンであることを検査。
- 署名鍵がないと配信しない。鍵を作り直すと既存アプリへ上書きできなくなるので保管すること。
- ローカル実行：`gradle :app:assembleRelease :app:testReleaseUnitTest :app:lintRelease`。

## ビューア機能と制限
本家モバイルWebを利用したホーム、検索、詳細、ログイン、フォロー操作。
公開／非公開フォローは独立した入口。ログイン中の自分のユーザーIDを初回設定する。
無料の人気順はイラスト・漫画の新着先頭2ページから最大60件を取得してブックマーク数降順に並べるもの。検索全体の人気順とは異なる。小説の独自並べ替えは未実装。
本家の認証・Web画面・非公開APIに依存するため、pixiv側の変更や認証制限で使えない場合がある。
実機でのログイン、公開／非公開表示、インストール許可と上書き更新は別途確認が必要。

## 参照
- https://developer.android.com/studio/publish/app-signing
- https://developer.android.com/reference/androidx/core/content/FileProvider
- https://github.com/FreeNowOrg/PixivNow/blob/master/docs/pixiv-web-api.md

## ログイン経路の修正
メール・パスワード／Xの認証はWebView内で継続し、途中のOAuth URLを外部ブラウザへ渡さない。
認証画面の表示中だけサードパーティCookieを許可。埋め込みHTTPS認証画面の遷移を妨げない。
Googleなど未対応の認証は案内を表示。外部ブラウザはpixivトップから独立して利用し、アプリとログイン状態を共有しない。
設定の「ログインの案内・接続状態」で再開始できる。診断表示はホスト名・HTTPエラー・WebView版のみで、URLクエリ・認証コード・Cookieを表示しない。

実機確認：更新後、設定からログインを再開始 → Xで認証 → 外部ブラウザへ移動せずpixivへ戻ることを確認 → アプリ再起動後のログイン維持を確認。
この変更で認証経路の不具合は修正したが、実アカウントでのログイン成功は未検証。
