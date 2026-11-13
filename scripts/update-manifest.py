import hashlib
import json
import os
from pathlib import Path

name = os.environ['APP_VERSION_NAME']
repo = os.environ['GITHUB_REPOSITORY']
if repo != 'muya-nakama/pixviewer':
    raise SystemExit('Update channel is configured for muya-nakama/pixviewer only.')
apk = Path('dist/pixviewer.apk')
manifest = {
    'applicationId': 'jp.muya.pixivviewer',
    'versionCode': int(os.environ['APP_VERSION_CODE']),
    'versionName': name,
    'apkUrl': f'https://github.com/{repo}/releases/download/v{name}/pixviewer.apk',
    'sha256': hashlib.sha256(apk.read_bytes()).hexdigest(),
    'size': apk.stat().st_size,
    'notes': Path('RELEASE_NOTES.md').read_text(encoding='utf-8'),
}
Path('dist/update.json').write_text(json.dumps(manifest, ensure_ascii=False, indent=2), encoding='utf-8')
