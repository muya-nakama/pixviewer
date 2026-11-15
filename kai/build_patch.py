#!/usr/bin/env python3
"""Private Kai test APK transformation; fail on unexpected upstream."""
import re, shutil, sys
from pathlib import Path
root=Path(sys.argv[1]).resolve()
here=Path(__file__).resolve().parent
if not (root/'package.json').is_file(): raise RuntimeError('Not an upstream checkout')
def read(p): return (root/p).read_text(encoding='utf8').replace('\r\n','\n')
def write(p,s): (root/p).write_text(s,encoding='utf8')
def rep(p,a,b):
    s=read(p); n=s.count(a)
    if n!=1: raise RuntimeError(f'{p}: expected one match for {a[:55]!r}, got {n}')
    write(p,s.replace(a,b,1))
def reg(p,pat,new):
    s,n=re.subn(pat,lambda _:new,read(p),flags=re.S)
    if n!=1: raise RuntimeError(f'{p}: regex match count {n}')
    write(p,s)
def drop(p,*items):
    for a in items: rep(p,a,'')

drop('src/App.vue',"import { fetchNotices } from '@/store/actions/fetch-notice'\n",'    fetchNotices()\n')
p='public/index.html'
reg(p,r'\s*<% if \(process\.env\.NODE_ENV === \'production\' && process\.env\.VUE_APP_UMAMI_SRC\) \{ %>.*?<% \} %>','')
drop(p,'<link rel="dns-prefetch" href="https://api.cocomi.eu.org">','<link rel="preconnect" href="https://api.cocomi.eu.org" crossorigin="anonymous" />')
rep(p,'  <link rel="shortcut icon"', """  <meta http-equiv="Content-Security-Policy" content="default-src 'self' data: blob:; connect-src 'self' https://app-api.pixiv.net https://oauth.secure.pixiv.net https://www.pixiv.net https://accounts.pixiv.net https://i.pximg.net https://s.pximg.net; img-src 'self' data: blob: https://*.pximg.net https://*.pixiv.net; media-src 'self' blob: https://*.pximg.net; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; font-src 'self' data:; frame-src 'self' https://*.pixiv.net; object-src 'none'; base-uri 'self'; upgrade-insecure-requests" />
  <link rel="shortcut icon" """.rstrip())
p='src/consts/index.js'
rep(p,"|| 'i.pixiv.re'","|| 'i.pximg.net'")
rep(p,"export const PXIMG_PROXY_BASE = LocalStorage.get('PXIMG_PROXY', DEF_PXIMG_MAIN)","export const PXIMG_PROXY_BASE = DEF_PXIMG_MAIN")
rep(p,"export const PXIMG_PID_BASE = 'https://i.loli.best/'","export const PXIMG_PID_BASE = 'https://i.pximg.net/'")
rep(p,"export const DEF_HIBIAPI_MAIN = (process.env.VUE_APP_DEF_HIBIAPI_MAIN || 'https://api.cocomi.eu.org').replace('/api/pixiv', '')","export const DEF_HIBIAPI_MAIN = ''")
rep(p,"export const PIXIV_NEXT_URL = LocalStorage.get('PXVEAPI_BASE', DEF_HIBIAPI_MAIN)","export const PIXIV_NEXT_URL = ''")
p='src/api/client/pixiv-auth.js'
rep(p,"import { LocalStorage } from '@/utils/storage'","import { SessionStorage } from '@/utils/storage'")
s=read(p).replace('LocalStorage.','SessionStorage.').replace('  directMode: false,','  directMode: true,')
s=s.replace('SessionStorage.set(CONFIG_FILE, config)','SessionStorage.set(CONFIG_FILE, { ...config, directMode: true, useApiProxy: false })')
write(p,s)
p='src/api/client/pixiv-api.js'
rep(p,"import { LocalStorage } from '@/utils/storage'","import { LocalStorage, SessionStorage } from '@/utils/storage'")
s=read(p).replace("LocalStorage.get('PXV_CLIENT_AUTH')","SessionStorage.get('PXV_CLIENT_AUTH')").replace("LocalStorage.set('PXV_CLIENT_AUTH'","SessionStorage.set('PXV_CLIENT_AUTH'")
write(p,s)
rep(p,'if (platform.isAndroid && window.p_api_hosts) {','if (platform.isAndroid) {')
rep(p,'  const fUrl = new URL(finalUrl)\n',"""  const fUrl = new URL(finalUrl)
  if (fUrl.protocol !== 'https:' || !['app-api.pixiv.net','oauth.secure.pixiv.net'].includes(fUrl.hostname)) throw new Error('Blocked non-Pixiv API')
  if (window.p_api_proxy) throw new Error('Blocked API proxy')
""")
drop(p,"  console.log('callApi Url: ', finalUrl)\n","  console.log('callApi options: ', options)\n","      console.log('callApi res: ', res)\n","      console.log('callApi err: ', err)\n","        console.log('tokenRequest data: ', data)\n","        console.log('tokenRequest callApi err, fallback to in-app proxy: ', err)\n","      console.log('tokenRequest err: ', err)\n")
drop('src/api/client/login.js',"  console.log('code: ', code)\n","  console.log('code_verifier: ', code_verifier)\n","    console.log('\\nLogin URL:', login_url)\n")
p='src/platform/capacitor/init.js'
rep(p,"import { getActionMap, setPximgIP } from '@/api/client/action'","import { getActionMap, setPximgIP } from '@/api/client/action'\nimport PixivAuth from '@/api/client/pixiv-auth'")
rep(p,"import { LocalStorage } from '@/utils/storage'","import { LocalStorage, SessionStorage } from '@/utils/storage'")
rep(p,"const config = LocalStorage.get('PXV_CLIENT_CONFIG', {})","const config = PixivAuth.readConfig()")
rep(p,"LocalStorage.get('PXV_LOGIN_CODEV')","SessionStorage.get('PXV_LOGIN_CODEV')")
p='src/views/Account/Login.vue'
rep(p,"import { LocalStorage } from '@/utils/storage'","import { SessionStorage } from '@/utils/storage'")
rep(p,"LocalStorage.set('PXV_LOGIN_CODEV'","SessionStorage.set('PXV_LOGIN_CODEV'")
reg(p,r'\s*<van-cell size="large" center :title="\$t\(\'3ZvAP-w7q7teBcLoqOgCc\'\)" is-link to="/account/session" />','')
p='src/api/client/action.js'
reg(p,r'function dnsQuery\(domain\) \{.*?(?=// async function setApiHosts\(config\) \{)',"async function setPximgIP() {\n  window.p_pximg_ip = 'i.pximg.net'\n}\n\n")
reg(p,r'  if \(config\.useApiProxy\) \{\s*window\.p_api_proxy = config\.apiProxy \|\| DEF_API_PROXY\s*\} else if \(config\.directMode\) \{\s*// await setApiHosts\(config\)\s*window\.p_api_hosts = DEF_API_HOSTS\s*\}',"  if (config.useApiProxy) throw new Error('Proxy disabled')\n  window.p_api_proxy = null\n  window.p_api_hosts = {}")
rep(p,"    console.log('Start err:', err)\n  }\n}","    console.warn('Not signed in yet')\n    return {}\n  }\n}")
p='src/platform/capacitor/utils.js'
reg(p,r'export async function getPximgUriAndroid\(url\) \{.*?(?=\nexport async function share\()',"""export async function getPximgUriAndroid(url) {
  if (url.protocol !== 'https:' || url.hostname !== 'i.pximg.net') throw new Error('Image host blocked')
  const path = 'pximg/' + url.pathname.replace(/^\/+/, '')
  const directory = Directory.Cache
  const stats = await Filesystem.stat({ path, directory }).catch(() => ({ uri: null }))
  if (stats.uri) return Capacitor.convertFileSrc(stats.uri)
  const res = await Filesystem.downloadFile({ url: url.href, path, directory, recursive: true, headers: { Referer: 'https://www.pixiv.net/' } })
  return Capacitor.convertFileSrc(res.path)
}
""")
reg(p,r'export async function setSkipSslSetting\(shouldSkip = false\) \{.*?\n\}',"export async function setSkipSslSetting(shouldSkip = false) {\n  return false\n}")
p='src/platform/capacitor/polyfill.js'
rep(p,'  window.__httpRequest__ = (url, configStr) => {',"""  window.__httpRequest__ = (url, configStr) => {
    const u = new URL(url)
    if (u.protocol !== 'https:' || !['app-api.pixiv.net','oauth.secure.pixiv.net','i.pximg.net','s.pximg.net'].includes(u.hostname)) return Promise.reject(new Error('External native HTTP blocked'))""")
p='src/api/user.js'
s=read(p).replace("      'x-auth': LocalStorage.get('PXV_NOW_COOKIE') || '',\n",'').replace("      'x-csrf-token': sessionStorage.getItem('PXV_NOW_CSRFTOKEN') || '',\n",'')
write(p,s)
reg(p,r"  LocalStorage\.set\('PXV_NOW_COOKIE', .*?, 180 \* 86400\)","  return Promise.reject(new Error('Cookie-proxy login disabled'))")
p='src/views/Setting/OtherSetting.vue'
reg(p,r'\s*<van-cell center title="Enable Umami Analytics">.*?</van-cell>','')
reg(p,r'\s*<van-cell center :title="\$t\(\'sync\.config_title\'\)" is-link @click="syncDialogShow = true" />','')
reg(p,r'\s*<SyncDialog v-model="syncDialogShow" />','')
reg(p,r'\s*<van-cell v-if="clientConfig\.refreshToken" center :title="\$t\(\'setting\.other\.cp_token_title\'\)" is-link :label="\$t\(\'setting\.other\.cp_token_label\'\)" @click="copyToken" />','')
drop(p,"import SyncDialog from './SyncDialog.vue'\n","    SyncDialog,\n","      console.log('settings: ', settings)\n")
p='src/views/Setting/index.vue'
rep(p,"import { LocalStorage } from '@/utils/storage'","import { LocalStorage, SessionStorage } from '@/utils/storage'")
rep(p,"LocalStorage.remove('PXV_CLIENT_AUTH')","SessionStorage.remove('PXV_CLIENT_AUTH')")
rep(p,"        localApi.APP_CONFIG.useLocalAppApi = false","        localApi.APP_CONFIG.useLocalAppApi = false\n        localApi.APP_CONFIG.refreshToken = ''")
rep('src/store/index.js','      isDirectPximg: false,','      isDirectPximg: true,')
rep('src/views/Setting/About.vue','    this.checkUpdate()','    // KaiSafe disables automatic external update checks')
p='android/app/src/main/java/org/eu/cocomi/pxvek/MainActivity.java'
if 'disableSslVerification' not in read(p): raise RuntimeError('SSL code changed')
write(p,'package org.eu.cocomi.pxvek;\nimport com.getcapacitor.BridgeActivity;\npublic class MainActivity extends BridgeActivity {}\n')
p='android/app/src/main/AndroidManifest.xml'
rep(p,'android:allowBackup="true"','android:allowBackup="false"')
rep(p,'android:usesCleartextTraffic="true"','android:usesCleartextTraffic="false"')
drop(p,'        android:requestLegacyExternalStorage="true"\n','    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"/>\n','    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />\n')
base='packages/capacitor-plugin-pixiv-login/android/'
jbase=base+'src/main/java/com/pixiv/login/'
for name in ['LoginProxyPlugin.java','LoginWebViewActivity.java']:
    if not (root/jbase/name).is_file(): raise RuntimeError('Login plugin missing')
    write(jbase+name,(here/'native'/name).read_text(encoding='utf8'))
shutil.rmtree(root/jbase/'network')
p=base+'build.gradle'
drop(p,"        classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24'\n","apply plugin: 'kotlin-android'\n","    implementation 'org.jetbrains.kotlin:kotlin-stdlib:1.9.24'\n","    implementation 'org.bouncycastle:bcprov-jdk15on:1.70'\n","    implementation 'org.bouncycastle:bcpkix-jdk15on:1.70'\n")
reg(p,r"    kotlinOptions \{\n        jvmTarget = '17'\n    \}\n",'')
p='packages/capacitor-plugin-pixiv-cronet/android/src/main/java/com/pixiv/cronet/PixivCronetPlugin.java'
rep(p,'    @PluginMethod\n    public void request(PluginCall call) {',"""    private static boolean trusted(String target) {
        try {
            java.net.URI u = new java.net.URI(target);
            return "https".equalsIgnoreCase(u.getScheme()) && u.getUserInfo()==null && (u.getPort()==-1 || u.getPort()==443)
                && ("app-api.pixiv.net".equalsIgnoreCase(u.getHost()) || "oauth.secure.pixiv.net".equalsIgnoreCase(u.getHost()));
        } catch (Exception e) { return false; }
    }
    @PluginMethod
    public void request(PluginCall call) {""")
rep(p,'        String method = call.getString("method", "GET");',"""        if (!trusted(url)) { call.reject("External API blocked"); return; }
        String method = call.getString("method", "GET");""")
rep(p,'                    Log.d(TAG, "  ↳ redirect → " + newUrl);\n                    req.followRedirect();',"""                    if (!trusted(newUrl)) { error.set(new SecurityException("Redirect blocked")); req.cancel(); return; }
                    req.followRedirect();""")
drop(p,'        Log.d(TAG, "──→ " + method + " " + url);\n','                Log.d(TAG, "Adding headers: " + headersObj.toString());\n','        Log.d(TAG, "←── " + method + " " + url + " " + info.getHttpStatusCode()\n                + " " + info.getNegotiatedProtocol() + " [" + totalSize + " bytes]");\n')
p='packages/capacitor-plugin-pixiv-cronet/android/src/main/java/com/pixiv/cronet/CronetManager.java'
reg(p,r'        // HostResolverRules: 域名 → IP 直接映射\n.*?(?=        // 创建 Cronet 缓存目录)','        // Use system DNS and normal HTTPS checks.\n')
drop(p,'        Log.d(TAG, "Cronet config - experimental: " + experimental);\n')
rep(p,'                .addQuicHint("oauth.secure.pixiv.net", 443, 443)\n                .setExperimentalOptions(experimental);','                .addQuicHint("oauth.secure.pixiv.net", 443, 443);')
rep('android/app/build.gradle','applicationId "org.eu.cocomi.pxvek"','applicationId "jp.muya.pixviewer.kai"')
rep('android/app/build.gradle','versionCode 20261001','versionCode Integer.parseInt(System.getenv("KAI_VERSION_CODE") ?: "20261002")')
rep('android/app/build.gradle','versionName "1.37.6"','versionName (System.getenv("KAI_VERSION_NAME") ?: "1.37.6-kai.1")')
p='capacitor.config.json'
rep(p,'"appId": "org.eu.cocomi.pxvek"','"appId": "jp.muya.pixviewer.kai"')
rep(p,'"appName": "Pixiv Viewer"','"appName": "pixviewer Kai (test)"')
rep(p,'"CapacitorHttp": {\n      "enabled": true','"CapacitorHttp": {\n      "enabled": false')
for f,needle in {'src/App.vue':'fetchNotices()','public/index.html':'VUE_APP_UMAMI_SRC','src/api/client/login.js':"console.log('code: ', code)",'android/app/src/main/java/org/eu/cocomi/pxvek/MainActivity.java':'disableSslVerification','packages/capacitor-plugin-pixiv-login/android/src/main/java/com/pixiv/login/LoginWebViewActivity.java':'handler.proceed()'}.items():
    if needle in read(f): raise RuntimeError('Audit gate failed: '+f)
print('Hardening patch applied. Runtime safety and functionality remain unverified.')


# KaiSafe v2: prevent guest startup errors; page until the official API stops.
p='src/api/http.js'
drop(p,"  console.log('=== get: ', url, params)\n","    console.log('=== get res: ', res)\n","    console.error('=== get error: ', error)\n")
rep(p,"  try {\n    const res = await retry(async () => {","""  try {
    // There is no anonymous backend: never fall back to a third-party service.
    if (!localApi.APP_CONFIG.useLocalAppApi) {
      return { error: new Error('KaiSafe: Pixiv login required for direct API search') }
    }
    const res = await retry(async () => {""")
rep(p,"        resp = await localApi.actionMap[url]({ query: params, ...config })","""        const action = localApi.actionMap && localApi.actionMap[url]
        if (typeof action !== 'function') throw new Error('KaiSafe: unsupported direct API endpoint')
        resp = await action({ query: params, ...config })""")
rep(p,"      resp = await axios.get(url, { params, ...config })\n      return resp.data","      throw new Error('KaiSafe: external fallback disabled')")
p='src/views/Home/index.vue'
rep(p,'    <div class="home-i">',"""    <div v-if="!isAppLogin" class="kai-guest-notice" style="padding:40px 24px 24px;text-align:center;">
      <h2 style="font-size:20px;margin-bottom:18px;">KaiSafe テスト版</h2>
      <p style="font-size:15px;line-height:1.8;">外部検索プロキシを停止しています。検索・ランキングを利用するには、検証専用のpixivアカウントで直接ログインしてください。</p>
      <van-button style="margin-top:20px;" type="primary" @click="$router.push('/account/login')">検証用アカウントでログイン</van-button>
    </div>
    <div v-else class="home-i">""")
drop(p,'        <SpotlightCard />\n')
rep(p,"    async initSearch() {\n      const res = await api.getTags()", "    async initSearch() {\n      if (!this.isAppLogin) return\n      const res = await api.getTags()")
p='src/utils/filter/index.js'
reg(p,r'export const BLOCK_SEARCH_WORD_RE = /[^\n]+','export const BLOCK_SEARCH_WORD_RE = /(?!)')
reg(p,r'export const BLOCK_RESULT_RE = /[^\n]+','export const BLOCK_RESULT_RE = /(?!)')
reg(p,r"export async function mintVerify\(word = '', forceCheck = false\) \{.*?\n\}","export async function mintVerify(word = '', forceCheck = false) {\n  return true\n}")
rep(p,"const res = await fetchTxt('https://hibiapi.cocomi.eu.org/sensitive-words-filter/words.txt', 1e4)","const res = '' // No third-party filter download in KaiSafe")
p='src/api/index.js'
reg(p,r'  async search\(word, page = 1, params = \{\}\) \{.*?(?=\n  async searchNovel\()',"""  async search(word, page = 1, params = {}) {
    const cacheKey = 'searchList_kai_v2_' + word + '_' + page + '_' + JSON.stringify(params)
    const cached = SessionStorage.get(cacheKey)
    let searchList = cached?.list
    let hasNext = cached?.hasNext
    if (!searchList) {
      const res = await get('/search', { word, page, ...params })
      if (Array.isArray(res.illusts)) {
        searchList = res.illusts.map(art => parseIllust(art))
        hasNext = Object.prototype.hasOwnProperty.call(res, 'next_url')
          ? Boolean(res.next_url)
          : res.illusts.length >= 30
        SessionStorage.set(cacheKey, { list: searchList, hasNext }, 60 * 60)
      } else if (res.error) {
        return { status: -1, msg: handleErrMsg(res) }
      } else {
        return { status: -1, msg: i18n.t('tip.unknown_err') }
      }
    }
    return { status: 0, data: filterCensoredIllusts(searchList), rawLen: searchList.length, hasNext }
  },
""")
p='src/views/Search/SearchRes.vue'
rep(p,'      totalPages: 166,','      totalPages: 1,')
rep(p,'      this.finished = false\n    },','      this.finished = false\n      this.totalPages = 1\n    },')
rep(p,'      console.log(' + chr(96) + 'doSearch: ${val}' + chr(96) + ')\n',"""      if (!this.$store.state.user) {
        this.$toast('KaiSafe: 検索には検証用pixivアカウントでのログインが必要です')
        this.loading = false
        this.finished = true
        return
      }
""")
rep(p,'      if (res.status === 0) {\n        if (res.rawLen) {',"""      if (res.status === 0) {
        this.totalPages = Math.max(this.totalPages, this.curPage + (res.hasNext ? 1 : 0))
        if (res.rawLen) {""")
rep(p,'            if (!this.isLoggedIn && this.curPage > 5) this.finished = true','            if (res.hasNext === false) this.finished = true')
p='src/views/Search/SearchNovel.vue'
rep(p,'          if (!this.isLoggedIn && this.curPage > 5) this.finished = true','          // The official API next_url, not a hard-coded 5-page cap, controls completion.')
print('KaiSafe v2 startup and complete pagination patch applied; device testing still needed.')
