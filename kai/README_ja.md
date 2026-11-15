# pixviewer Kai 版（実験用・別APK）

通常版 **pixviewer.apk** とは異なるアプリID \`jp.muya.pixviewer.kai\` を持つ検証用APKです。通常版のデータは上書きしません。

## APKの取得手順（Galaxy）

1. https://github.com/muya-nakama/pixviewer/actions/workflows/build-kai-test.yml を開く。
2. **Run workflow（ワークフローを実行）** → Branch **main** → **Run workflow**。
3. 実行履歴の新しい行を開く。緑のチェックが付いたら、下部の **Artifacts** から \`pixviewer-kai-test-apk\` を選ぶ。
4. ダウンロードしたZIP内の \`pixviewer-kai.apk\` を取り出してインストールする。

※Artifactsは通常のReleasesとは別です。公開ダウンロードにせず、GitHubアカウントにログインした本人が取得する実験用の構成です。Artifacts保存期間は7日。

## 重要な注意

- APKのビルド・インストール・ログイン・検索結果・画像表示・実通信先は、成功が確認されるまで**未検証**です。
- **メインのpixivアカウントではログインしないこと。** テスト専用アカウントを使う。
- 第三者APIへのアクセスを制限した影響で、未ログイン検索や一部の補助機能は動かない可能性があります。
- 認証情報は永続化せず、そのWebViewセッション中だけ保持します。ログイン状態が再起動後に消える場合があります。
- 通信先のホワイトリストは安全性を高める初期対策で、APK全体の完全な監査ではありません。
- 元プロジェクトは https://github.com/asadahimeka/pixiv-viewer-app （AGPL-3.0-only）。このテストAPKは非公式です。
