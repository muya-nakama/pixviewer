# pixviewer Kai版（個人検証用・別APK）

通常版 `pixviewer.apk` と別のアプリID `jp.muya.pixviewer.kai` を使用します。通常版の設定とログイン状態を上書きしません。

## APK取得方法

1. [Kai版ビルド](https://github.com/muya-nakama/pixviewer/actions/workflows/build-kai-test.yml) を開く。
2. **Run workflow** → **main** → **Run workflow** を押す。
3. 実行履歴が成功（緑のチェック）になった場合、[Releases](https://github.com/muya-nakama/pixviewer/releases) を開く。
4. 自分のGitHubアカウントでログインした状態なら、未公開（**Draft**）の `pixviewer Kai test` が表示されます。その中の **pixviewer-kai.apk** をダウンロードする。
5. GalaxyでAPKを開いてインストールする。

**重要：** 通常版がある公開Releasesには未検証のAPKを公開しないため、Kai版は**下書きのRelease**として保存します。他人は下書きを閲覧できません（リポジトリへの書き込み権限があるアカウントを除く）。GitHubアカウントのログインが必要です。

## 現在の制約

- APKのビルド成功、アプリの起動、ログイン、検索、画像取得、通信監視は未確認です。
- テスト専用pixivアカウントを使用し、メインアカウントではログインしないでください。
- 外部プロキシを遮断するため、未ログイン検索や一部機能は失敗する可能性があります。
- 認証情報は暫定的にWebViewセッション内だけに保持する構成です。
- デバッグ署名APKなので、次回テスト版を上書きできずアンインストールが必要になる場合があります。
- この改修は完全なセキュリティ監査ではありません。

元ソース： https://github.com/asadahimeka/pixiv-viewer-app （AGPL-3.0-only）。
