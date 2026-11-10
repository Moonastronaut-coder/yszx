import { existsSync, readdirSync } from 'node:fs';
import { resolve, dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { spawn, spawnSync } from 'node:child_process';

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const command = process.argv[2];
const args = process.argv.slice(3);
const frontend = join(root, 'frontend');
const pnpm = join(root, '.tools/pnpm/node_modules/pnpm/bin/pnpm.cjs');
if (Number(process.versions.node.split('.')[0]) !== 24) {
  throw new Error('请使用 Node.js 24 LTS。');
}

// 使用 Node 原生 dotenv 解析，正确处理引号；密钥只传给后端子进程。
const inheritedEnv = { ...process.env, PATH: process.env.PATH ?? process.env.Path ?? '' };
if (process.platform === 'win32') delete inheritedEnv.Path;
if (existsSync(join(root, '.env'))) process.loadEnvFile(join(root, '.env'));
const backendEnv = { ...process.env };
process.env = inheritedEnv;
const commonEnv = {
  ...inheritedEnv,
  PATH: join(root, '.tools/pnpm/node_modules/.bin') + (process.platform === 'win32' ? ';' : ':') + inheritedEnv.PATH,
  PLAYWRIGHT_BROWSERS_PATH: join(root, '.tools/browsers'),
  BACKEND_PORT: backendEnv.BACKEND_PORT ?? '8080',
};

function run(program, options, cwd, env = commonEnv) {
  const child = spawn(program, options, { cwd, env, stdio: 'inherit', windowsHide: true });
  child.on('error', (error) => { console.error(error.message); process.exitCode = 1; });
  child.on('exit', (code) => { process.exitCode = code ?? 1; });
}

if (command === 'setup') {
  const npm = join(dirname(process.execPath), 'node_modules/npm/bin/npm-cli.js');
  const result = spawnSync(process.execPath, [npm, 'install', '--prefix', '.tools/pnpm', '--cache', '.tools/npm-cache', '--no-audit', '--no-fund', 'pnpm@10.30.3'], { cwd: root, stdio: 'inherit', env: commonEnv, windowsHide: true });
  if (result.status !== 0) process.exit(result.status ?? 1);
  run(process.execPath, [pnpm, 'install', '--frozen-lockfile', '--store-dir', join(root, '.tools/pnpm-store')], frontend);
} else if (command === 'backend' || command === 'backend:check') {
  const toolRoot = join(root, '.tools');
  const portable = existsSync(toolRoot) ? readdirSync(toolRoot).find((name) => name.startsWith('jdk-21')) : undefined;
  const javaHome = portable ? join(toolRoot, portable) : inheritedEnv.JAVA_HOME;
  const env = { ...backendEnv, MAVEN_USER_HOME: join(root, '.tools/maven'), ...(javaHome ? { JAVA_HOME: javaHome, PATH: join(javaHome, 'bin') + (process.platform === 'win32' ? ';' : ':') + inheritedEnv.PATH } : {}) };
  const version = spawnSync(javaHome ? join(javaHome, 'bin', process.platform === 'win32' ? 'java.exe' : 'java') : 'java', ['-version'], { encoding: 'utf8', env, windowsHide: true });
  if (!/version "21\./.test(version.stderr ?? '')) throw new Error('需要 JDK 21；请设置 JAVA_HOME，或在 .tools 放置 jdk-21* 便携目录。');
  const goals = command === 'backend:check' ? ['verify', '-Pintegration'] : ['spring-boot:run'];
  const options = ['-B', '-ntp', `-Dmaven.repo.local=${join(root, '.tools/m2')}`, ...goals, ...args];
  if (process.platform === 'win32') run('cmd.exe', ['/d', '/c', 'mvnw.cmd', ...options], join(root, 'backend'), env);
  else run('/bin/sh', ['mvnw', ...options], join(root, 'backend'), env);
} else if (command === 'portal' || command === 'admin') {
  if (!existsSync(pnpm)) throw new Error('请先运行 node scripts/dev.mjs setup。');
  run(process.execPath, [pnpm, '--filter', `@yunshu/${command}`, 'dev', ...args], frontend);
} else if (command === 'frontend') {
  if (!existsSync(pnpm)) throw new Error('请先运行 node scripts/dev.mjs setup。');
  run(process.execPath, [pnpm, ...args], frontend);
} else {
  console.log('用法：node scripts/dev.mjs setup | backend | backend:check | portal | admin | frontend <pnpm 参数>');
  process.exitCode = 1;
}
