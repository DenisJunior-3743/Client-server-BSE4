"""Verify and collect a signed TrustBank APK without reading signing passwords."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import zipfile
from urllib.parse import urlsplit

ROOT = Path(__file__).resolve().parents[3]

def run(args):
    result = subprocess.run([str(arg) for arg in args], capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError("APK inspection command failed: " + str(args[0]))
    return result.stdout

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--apk', required=True, type=Path)
    parser.add_argument('--version-name', required=True)
    parser.add_argument('--version-code', required=True, type=int)
    parser.add_argument('--channel', choices=['testing', 'production'], required=True)
    parser.add_argument('--api-base-url', required=True)
    parser.add_argument('--store-base-url', default='https://reviden-store.onrender.com')
    parser.add_argument('--output-dir', type=Path, default=ROOT/'mobile/releases')
    parser.add_argument('--github-env', type=Path)
    args = parser.parse_args()
    assert re.fullmatch(r'[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z]+(?:[.-][0-9A-Za-z]+)*)?', args.version_name), 'Invalid version name'
    assert 1 <= args.version_code <= 2_100_000_000, 'Invalid version code'
    backend=urlsplit(args.api_base_url)
    assert backend.scheme=='https' and backend.hostname and backend.hostname not in ['localhost','127.0.0.1','10.0.2.2'], 'Release backend must be public HTTPS'
    sdk=os.environ.get('ANDROID_HOME') or os.environ.get('ANDROID_SDK_ROOT')
    if not sdk:
        local=ROOT/'mobile/local.properties'
        for line in local.read_text(encoding="utf-8-sig").splitlines():
            if line.startswith('sdk.dir='): sdk=line.partition('=')[2].replace('\\:',':').replace('\\\\','\\')
    assert sdk, 'Android SDK path is missing'
    tools=Path(sdk)/'build-tools/34.0.0'
    suffix='.exe' if os.name=='nt' else ''
    java=Path(os.environ['JAVA_HOME'])/'bin'/('java'+suffix)
    badging=run([tools/('aapt2'+suffix),'dump','badging',args.apk])
    package_line=next(line for line in badging.splitlines() if line.startswith('package:'))
    attributes=dict(re.findall(r"([A-Za-z]+)='([^']*)'",package_line))
    assert attributes.get('name')=='com.trustbank.loanapp', 'Unexpected package name'
    assert attributes.get('versionCode')==str(args.version_code), 'Unexpected version code'
    assert attributes.get('versionName')==args.version_name, 'Unexpected version name'
    assert 'application-debuggable' not in badging, 'APK is debuggable'
    certs=run([java,'-jar',tools/'lib/apksigner.jar','verify','--print-certs',args.apk])
    assert 'CN=Android Debug' not in certs, 'APK uses the debug signing key'
    fingerprint=re.search(r'certificate SHA-256 digest:\s*([a-fA-F0-9]+)',certs).group(1).lower()
    expected=(ROOT/'mobile/release-signing-cert.sha256').read_text(encoding="utf-8-sig").strip().lower()
    assert fingerprint==expected, 'APK signing key differs from the published app'
    config=ROOT/'mobile/app/build/generated/source/buildConfig/release/com/trustbank/loanapp/BuildConfig.java'
    generated=config.read_text(encoding="utf-8-sig")
    for key,value in [('API_BASE_URL',args.api_base_url),('REVIDEN_CHANNEL',args.channel),('REVIDEN_BASE_URL',args.store_base_url.rstrip('/'))]:
        assert re.search(r'\b'+key+r'\s*=\s*"'+re.escape(value)+r'"',generated), 'Wrong compiled '+key
    with zipfile.ZipFile(args.apk) as archive:
        dex=b''.join(archive.read(name) for name in archive.namelist() if re.fullmatch(r'classes[0-9]*\.dex',name))
        assert args.api_base_url.rstrip('/').encode() in dex, 'Configured backend is absent from APK bytecode'
        assert args.store_base_url.rstrip('/').encode() in dex, 'Configured store is absent from APK bytecode'
        assert not any(name.endswith(('.jks','.keystore')) or name.endswith('/.env') for name in archive.namelist()), 'Private files found in APK'
    mapping=ROOT/'mobile/app/build/outputs/mapping/release/mapping.txt'
    assert mapping.is_file() and mapping.stat().st_size, 'R8 mapping is missing'
    args.output_dir.mkdir(parents=True,exist_ok=True)
    stem=f'trustbank_{args.version_name}+{args.version_code}_{args.channel}'
    output=args.output_dir/(stem+'.apk')
    shutil.copyfile(args.apk,output)
    sha=hashlib.sha256(output.read_bytes()).hexdigest()
    (args.output_dir/(stem+'.apk.sha256.txt')).write_text(sha+'  '+output.name+'\n',encoding='utf-8')
    shutil.copyfile(mapping,args.output_dir/(stem+'.mapping.txt'))
    metadata=dict(packageName='com.trustbank.loanapp',versionCode=args.version_code,versionName=args.version_name,
                  channel=args.channel,backendUrl=args.api_base_url,storeUrl=args.store_base_url.rstrip('/'),
                  certificateSha256=fingerprint,sha256=sha,sizeBytes=output.stat().st_size)
    (args.output_dir/(stem+'.json')).write_text(json.dumps(metadata,indent=2)+'\n',encoding='utf-8')
    if args.github_env:
        relative=output.relative_to(ROOT).as_posix()
        with args.github_env.open('a',encoding='utf-8') as target: target.write('APK_PATH='+relative+'\n')
    print(json.dumps(dict(apk=str(output),**metadata),indent=2))

if __name__=='__main__':main()
