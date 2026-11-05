// git 事实采集：把"台账里的说法"和"仓库里发生过什么"对上账。
// 只读操作，不修改工作区。

import { execFileSync } from 'node:child_process';

function git(root, args) {
  return execFileSync('git', ['-C', root, ...args], { encoding: 'utf8' }).trim();
}

/**
 * 查询一组路径最近一次提交。
 * @param {string} root 仓库根目录
 * @param {string[]} paths git pathspec 列表，支持 * 与 **
 * @returns {{sha: string, shortSha: string, date: string, subject: string} | null}
 */
export function lastCommitForPaths(root, paths) {
  if (!paths || paths.length === 0) return null;
  try {
    const out = git(root, ['log', '-1', '--format=%H%x1f%aI%x1f%s', '--', ...paths]);
    if (!out) return null;
    const [sha, date, subject] = out.split('\x1f');
    return { sha, shortSha: sha.slice(0, 7), date, subject };
  } catch {
    return null;
  }
}

/**
 * 读取 HEAD 信息，用于看板页脚与"数据同步到哪个提交"。
 * @param {string} root 仓库根目录
 * @returns {{sha: string, shortSha: string, date: string, subject: string, branch: string}}
 */
export function headInfo(root) {
  const sha = git(root, ['rev-parse', 'HEAD']);
  const [date, subject] = git(root, ['log', '-1', '--format=%aI%x1f%s']).split('\x1f');
  let branch = '';
  try {
    branch = git(root, ['branch', '--show-current']);
  } catch {
    branch = '';
  }
  return { sha, shortSha: sha.slice(0, 7), date, subject, branch };
}

/**
 * 判断提交是否存在，避免台账引用失效的 sha。
 * @param {string} root 仓库根目录
 * @param {string} sha 提交号
 * @returns {boolean}
 */
export function commitExists(root, sha) {
  if (!sha) return false;
  try {
    execFileSync('git', ['-C', root, 'cat-file', '-e', `${sha}^{commit}`], { stdio: 'ignore' });
    return true;
  } catch {
    return false;
  }
}

/**
 * 单次提交的提交时间，用于补全任务的完成时间点。
 * @param {string} root 仓库根目录
 * @param {string} sha 提交号
 * @returns {string | null} ISO-8601 时间
 */
export function commitDate(root, sha) {
  if (!sha) return null;
  try {
    return git(root, ['log', '-1', '--format=%aI', sha]);
  } catch {
    return null;
  }
}

/**
 * 最近 N 天是否有提交落在指定路径。
 * @param {string} root 仓库根目录
 * @param {string[]} paths git pathspec 列表
 * @param {number} days 天数窗口
 * @returns {boolean}
 */
export function hasRecentCommit(root, paths, days) {
  if (!paths || paths.length === 0) return false;
  try {
    const out = git(root, ['log', `--since=${days}.days`, '--format=%H', '--', ...paths]);
    return out !== '';
  } catch {
    return false;
  }
}
