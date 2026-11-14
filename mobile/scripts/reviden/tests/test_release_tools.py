"""Offline release-script tests. HTTP is replaced by fixtures; nothing is uploaded."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[4]
SCRIPTS = ROOT / "mobile/scripts/reviden"
BASH = os.environ.get("BASH_TOOL") or (r"C:\Program Files\Git\bin\bash.exe" if os.name == "nt" else shutil.which("bash"))
PROJECT = "6aa2456a0dba506c0b97fdc3"

FAKE_CURL = r'''import json,os,sys
from pathlib import Path
p=Path(os.environ['CURL_FIXTURES'])
state=json.loads(p.read_text()); reply=state['replies'].pop(0)
args=sys.argv[1:]
method=args[args.index('--request')+1] if '--request' in args else ('POST' if '-F' in args else 'GET')
state['calls'].append(method)
if reply.get('channel') and 'channel='+reply['channel'] not in args: raise SystemExit('wrong channel lookup')
p.write_text(json.dumps(state))
body=json.dumps(reply['body'])
if '--output' in args: Path(args[args.index('--output')+1]).write_text(body)
else: print(body)
if '--write-out' in args: print(reply.get('status',200),end='')
if '--fail-with-body' in args and reply.get('status',200)>=400: raise SystemExit(22)
'''

class ReleaseToolsTest(unittest.TestCase):
    def setUp(self):
        if not BASH or not Path(BASH).exists() and not shutil.which(str(BASH)):
            self.skipTest('Bash not installed')
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.folder = Path(self.temp.name)
        self.out = self.folder / 'output'
        self.env = os.environ.copy()
        self.env.update(GITHUB_ENV=self.out.as_posix(), GITHUB_OUTPUT=self.out.as_posix(),
                        EVENT_NAME='push', REF_NAME='v1.2.0', CHANNEL='testing',
                        REVIDEN_BASE_URL='https://store.example', REVIDEN_PROJECT_ID=PROJECT,
                        REVIDEN_API_TOKEN='offline-test-token', EXPECT_VERSION_NAME='1.2.0-beta.1',
                        VERSION_CODE_FLOOR='3', STORE_RETRY='0')

    def run_script(self, name, success=True, **values):
        env=self.env | values
        command='if [ -n "${TEST_BIN:-}" ]; then test_bin="$(cygpath -u "$TEST_BIN" 2>/dev/null || printf "%s" "$TEST_BIN")"; export PATH="$test_bin:$PATH"; fi; exec "$BASH" "$1"'
        result=subprocess.run([str(BASH),'-c',command,'release-test',(SCRIPTS/name).as_posix()],cwd=ROOT,env=env,
                              text=True,capture_output=True,timeout=180 if os.name == "nt" else 30)
        if success: self.assertEqual(result.returncode,0,result.stdout+result.stderr)
        else: self.assertNotEqual(result.returncode,0,'Invalid release unexpectedly succeeded')
        return self.out.read_text() if self.out.exists() else ''

    def fixtures(self, replies):
        if not shutil.which('jq'):
            self.skipTest('jq not installed; Ubuntu CI includes jq')
        self.state=self.folder/'fixtures.json'
        self.state.write_text(json.dumps({'replies':replies,'calls':[]}))
        fake=self.folder/'fake-curl.py';fake.write_text(FAKE_CURL)
        tools=self.folder/'bin';tools.mkdir()
        for name,text in [('curl','#!/usr/bin/env bash\nexec "$TEST_PYTHON" "$TEST_CURL" "$@"\n'),
                          ('sleep','#!/usr/bin/env bash\nexit 0\n')]:
            script=tools/name;script.write_text(text);script.chmod(0o755)
        self.env.update(PATH=str(tools)+os.pathsep+self.env['PATH'],CURL_FIXTURES=self.state.as_posix(),
                        TEST_PYTHON=Path(sys.executable).as_posix(),TEST_CURL=fake.as_posix(),TEST_BIN=tools.as_posix())

    def release(self, channel='production', code=2, status='published', name='1.1.1'):
        return dict(projectId=PROJECT,channel=channel,versionCode=code,versionName=name,status=status)

    def test_shell_syntax(self):
        for script in SCRIPTS.glob('*.sh'):
            result=subprocess.run([str(BASH),'-n',script.as_posix()],capture_output=True,text=True)
            self.assertEqual(result.returncode,0,result.stderr)

    def test_stable_tag_is_optional_production(self):
        result=self.run_script('select-release.sh')
        for value in ['CHANNEL=production','VERSION_NAME=1.2.0','UPDATE_TYPE=optional','GRACE_DAYS=0']:
            self.assertIn(value,result)

    def test_beta_and_legacy_beta_tags_select_testing(self):
        for tag in ['v1.2.0-beta.1','V1.2.0.beta.1']:
            self.out.unlink(missing_ok=True)
            result=self.run_script('select-release.sh',REF_NAME=tag)
            self.assertIn('CHANNEL=testing',result);self.assertIn('VERSION_NAME=1.2.0-beta.1',result)

    def test_forced_tag_and_immediate_manual_policy(self):
        result=self.run_script('select-release.sh',REF_NAME='v1.2.0+forced',DEFAULT_GRACE_DAYS='4')
        self.assertIn('UPDATE_TYPE=forced',result);self.assertIn('GRACE_DAYS=4',result)
        self.out.unlink()
        result=self.run_script('select-release.sh',EVENT_NAME='workflow_dispatch',INPUT_VERSION='1.2.0',
                               INPUT_CHANNEL='testing',INPUT_UPDATE_TYPE='forced',INPUT_GRACE_DAYS='0')
        self.assertIn('CHANNEL=testing',result);self.assertIn('GRACE_DAYS=0',result)

    def test_repository_default_forced_policy(self):
        result=self.run_script('select-release.sh',DEFAULT_UPDATE_TYPE='forced',DEFAULT_GRACE_DAYS='3')
        self.assertIn('UPDATE_TYPE=forced',result);self.assertIn('GRACE_DAYS=3',result)

    def test_invalid_tags_and_policy_are_rejected(self):
        for tag in ['v1.2.0.7','v1.2.0+unknown','v1.2.0;echo-bad','vnext']:
            self.run_script('select-release.sh',False,REF_NAME=tag)
        self.run_script('select-release.sh',False,DEFAULT_GRACE_DAYS='-1')

    def test_allocator_includes_both_channels_and_unpublished_codes(self):
        self.fixtures([{'channel':'production','body':{'releases':[self.release(code=5,status='uploaded')]}},
                       {'channel':'testing','body':{'releases':[self.release('testing',8,'draft','old-beta')]}}])
        result=self.run_script('next-version-code.sh')
        self.assertIn('version_code=9',result)

    def test_empty_history_respects_distributed_apk_floor(self):
        self.fixtures([{'body':{'releases':[]}},{'body':{'releases':[]}}])
        self.assertIn('version_code=4',self.run_script('next-version-code.sh'))

    def test_allocator_rejects_wrong_project_and_unauthorized_history(self):
        wrong=self.release();wrong['projectId']='a'*24
        self.fixtures([{'body':{'releases':[wrong]}}])
        self.run_script('next-version-code.sh',False)

    def test_allocator_rejects_an_already_published_version(self):
        self.fixtures([{'body':{'releases':[]}},{'body':{'releases':[self.release('testing',8,'published','1.2.0-beta.1')]}}])
        self.run_script('next-version-code.sh',False)

    def publish_setup(self, mismatch=False, sync_failed=False):
        apk=self.folder/'test.apk';apk.write_bytes(b'offline test APK')
        self.env.update(APK_PATH=apk.as_posix(),VERSION_CODE='4',VERSION_NAME='1.2.0-beta.1',APP_PACKAGE='com.trustbank.loanapp')
        metadata=dict(id='a'*24,projectId=PROJECT,channel='testing',packageName='com.trustbank.loanapp',
                      versionName='1.2.0-beta.1',versionCode=4,sha256=hashlib.sha256(apk.read_bytes()).hexdigest())
        if mismatch: metadata['sha256']='0'*64
        replies=[{'body':metadata|{'status':'syncing'}}]
        if not mismatch:
            replies.append({'body':metadata|{'status':'sync_failed' if sync_failed else 'uploaded'}})
            if not sync_failed: replies.append({'body':metadata|{'status':'published'}})
        self.fixtures(replies)

    def test_upload_waits_for_storage_and_explicitly_publishes(self):
        self.publish_setup();self.run_script('publish-apk.sh')
        self.assertEqual(json.loads(self.state.read_text())['calls'],['POST','GET','PATCH'])

    def test_wrong_hash_never_reaches_publication(self):
        self.publish_setup(mismatch=True);self.run_script('publish-apk.sh',False)
        self.assertEqual(json.loads(self.state.read_text())['calls'],['POST'])

    def test_sync_failure_never_reaches_publication(self):
        self.publish_setup(sync_failed=True);self.run_script('publish-apk.sh',False)
        self.assertEqual(json.loads(self.state.read_text())['calls'],['POST','GET'])

if __name__=='__main__':unittest.main()
