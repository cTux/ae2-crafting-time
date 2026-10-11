"""Check wizard prompts without opening browsers or accessing credentials.

Run: python test-template.py /path/to/bash [template.sh]
"""
from pathlib import Path
import os
import subprocess
import sys
import tempfile

bash = sys.argv[1]
template = Path(sys.argv[2]) if len(sys.argv) > 2 else Path(__file__).with_name('template.sh')
library = template.read_text(encoding='utf-8').split('run_wizard() {', 1)[0]

with tempfile.TemporaryDirectory(prefix='wizard-input-test-') as directory:
    env_file = Path(directory) / '.env'
    for helper in ('ask', 'ask_secret'):
        for saved in ('', "DUMMY='saved-value'\n"):
            env_file.write_text(saved, encoding='utf-8')
            for supplied, expected in (('\n', 'saved-value' if saved else None),
                                       ('', None), ('new-value\n', 'new-value')):
                action = f'{helper} DUMMY "Dummy value:"\nprintf "RESULT=%s" "$DUMMY"\n'
                (Path(directory) / 'check.sh').write_text(library + action, encoding='utf-8', newline='\n')
                result = subprocess.run([bash, 'check.sh'], cwd=directory,
                                        input=supplied.encode(), capture_output=True,
                                        env={**os.environ, 'ENV_FILE': '.env'})
                if expected is None:
                    assert result.returncode != 0, (helper, saved, repr(supplied))
                    assert b'RESULT=' not in result.stdout
                else:
                    assert result.returncode == 0, result.stderr
                    assert result.stdout.endswith(('RESULT=' + expected).encode()), result.stdout
                assert env_file.read_text(encoding='utf-8') == saved
print('PASS: both prompts reject missing input, keep saved defaults, and accept replacement values.')

with tempfile.TemporaryDirectory(prefix='wizard-summary-test-') as directory:
    script = Path(directory) / 'check.sh'
    script.write_text(library + '\ngh() { printf \'%s\\n\' "$*" >> gh-calls; }\nset_var DUMMY value\nset_secret DUMMY dummy-secret\nfinish\n',
                      encoding='utf-8', newline='\n')
    result = subprocess.run([bash, 'check.sh'], cwd=directory, capture_output=True,
                            env={**os.environ, 'ENV_FILE': '.env', 'WIZARD_REPO': 'owner/intended'})
    assert result.returncode == 0, result.stderr
    summary = result.stdout.split(b'Setup complete', 1)[1]
    assert b'GitHub variable(s): DUMMY' in summary, summary
    calls = (Path(directory) / 'gh-calls').read_text()
    assert 'variable set DUMMY --repo owner/intended --body value' in calls
    assert 'secret set DUMMY --repo owner/intended' in calls
    (Path(directory) / 'gh-calls').unlink()
    environment = {key: value for key, value in os.environ.items() if key != 'WIZARD_REPO'}
    environment['ENV_FILE'] = '.env'
    result = subprocess.run([bash, 'check.sh'], cwd=directory, capture_output=True, env=environment)
    assert result.returncode != 0
    assert not (Path(directory) / 'gh-calls').exists()
print('PASS: successful GitHub variables appear in the closing summary; gh was stubbed.')

with tempfile.TemporaryDirectory(prefix='wizard-location-test-') as directory:
    root = Path(directory)
    project = root / 'project'
    scripts = project / 'scripts'
    scripts.mkdir(parents=True)
    subprocess.run(['git', 'init', '-q', str(project)], check=True)
    script = scripts / 'check.sh'
    script.write_text(library + '\n_env_path\nprintf \'%s\\n\' "$ENV_FILE"\n', encoding='utf-8', newline='\n')
    environment = {key: value for key, value in os.environ.items() if key != 'ENV_FILE'}
    result = subprocess.run([bash, 'project/scripts/check.sh'], cwd=root,
                            capture_output=True, env=environment)
    assert result.returncode == 0, result.stderr
    assert result.stdout.strip().replace(b'\\', b'/').endswith(b'/project/.env'), result.stdout
    assert not (root / '.env').exists()
    (root / 'outside.sh').write_text(script.read_text(), encoding='utf-8', newline='\n')
    result = subprocess.run([bash, 'outside.sh'], cwd=root,
                            capture_output=True, env=environment)
    assert result.returncode != 0
    assert not (root / '.env').exists()
print('PASS: default env path belongs to the script project; scratch scripts require an explicit path.')

with tempfile.TemporaryDirectory(prefix='wizard-permissions-test-') as directory:
    target = Path(directory) / '.env'
    script = Path(directory) / 'check.sh'
    target.write_text('EXISTING=value\n')
    target.chmod(0o644)
    script.write_text(library + '\nwrite_env DUMMY value\n', encoding='utf-8', newline='\n')
    result = subprocess.run([bash, 'check.sh'], cwd=directory, capture_output=True,
                            env={**os.environ, 'ENV_FILE': '.env'})
    if os.name == 'nt':
        assert result.returncode != 0  # Git Bash cannot establish private Windows ACLs.
        assert target.read_text() == 'EXISTING=value\n'
    else:
        assert result.returncode == 0, result.stderr
        assert target.stat().st_mode & 0o777 == 0o600
        assert "DUMMY='value'" in target.read_text()
    target.write_text('EXISTING=value\n')
    script.write_text(library + "\nstat() { printf '644\\n'; }\nwrite_env DUMMY value\n",
                      encoding='utf-8', newline='\n')
    result = subprocess.run([bash, 'check.sh'], cwd=directory, capture_output=True,
                            env={**os.environ, 'ENV_FILE': '.env'})
    assert result.returncode != 0
    assert target.read_text() == 'EXISTING=value\n'
print('PASS: env writes require verified owner-only permissions and preserve contents when refused.')

with tempfile.TemporaryDirectory(prefix='wizard-action-test-') as directory:
    script = Path(directory) / 'check.sh'
    script.write_text(library + '\nsay "Dummy manual action"\nfinish\n', encoding='utf-8', newline='\n')
    environment = {key: value for key, value in os.environ.items()
                   if key not in {'ENV_FILE', 'WIZARD_REPO'}}
    result = subprocess.run([bash, 'check.sh'], cwd=directory, capture_output=True, env=environment)
    assert result.returncode == 0, result.stderr
    assert b'Setup complete' in result.stdout
    assert not (Path(directory) / '.env').exists()
print('PASS: action-only workflows require neither a Git checkout nor a GitHub or dotenv target.')
