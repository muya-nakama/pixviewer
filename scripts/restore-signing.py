"""Restore one repository secret without logging key material."""
import base64
import json
import os
from pathlib import Path

secret = os.environ.get('ANDROID_SIGNING', '')
if not secret:
    print('Signing secret not configured; validation build only.')
else:
    data = json.loads(secret)
    password = data['password']
    if not isinstance(password, str) or len(password) < 16 or '\n' in password or '\r' in password:
        raise ValueError('Invalid signing password format')
    print('::add-mask::' + password)
    destination = Path(os.environ['RUNNER_TEMP']) / 'pixviewer.p12'
    destination.write_bytes(base64.b64decode(data['keystoreBase64'], validate=True))
    destination.chmod(0o600)
    with open(os.environ['GITHUB_ENV'], 'a') as env:
        env.write(f'PIXVIEWER_KEYSTORE={destination}\nPIXVIEWER_STORE_PASSWORD={password}\n')
