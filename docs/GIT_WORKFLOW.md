# Git 与 GitHub 实战工作流

## 1. 工作模型

项目采用轻量 GitHub Flow：

```text
Issue → 功能分支 → 小步提交 → Push → Pull Request → 自检/评审 → 合并到 main → Tag
```

`main` 必须始终保持可构建。日常开发不直接在 `main` 上堆积多个功能。

## 2. 分支命名

```text
feat/001-scene-assessment
feat/002-android-shell
fix/014-stale-radar-state
docs/006-model-card
chore/009-ci
```

编号对应 GitHub Issue，便于从产品需求追踪到代码。

## 3. 提交格式

采用 Conventional Commits：

```text
feat(core): add assessment freshness policy
fix(radar): reject out-of-order observation frames
docs(prd): define MVP acceptance criteria
test(core): cover expired radar data
chore(ci): add CMake test workflow
```

一次提交只表达一个完整意图。不要提交密钥、数据大文件、IDE缓存或不可复现的构建产物。

## 4. 每个功能的实战命令

```powershell
# 1. 从最新main创建任务分支
git switch main
git pull --ff-only
git switch -c feat/001-scene-assessment

# 2. 查看和暂存
git status
git diff
git add core docs/PRD_MVP.md

# 3. 提交
git commit -m "feat(core): add scene assessment contract"

# 4. 推送并建立PR
git push -u origin feat/001-scene-assessment
```

没有安装 GitHub CLI 时，可以在 GitHub 网页创建 Pull Request；安装后可使用：

```powershell
gh pr create --fill
gh pr checks
gh pr merge --squash --delete-branch
```

## 5. 高频排错

```powershell
# 查看未暂存差异
git diff

# 查看已暂存差异
git diff --staged

# 取消暂存，但保留文件修改
git restore --staged <file>

# 放弃单个文件尚未提交的修改（有数据丢失风险）
git restore <file>

# 修正最近一次提交信息
git commit --amend

# 查看简洁历史
git log --oneline --graph --decorate --all

# 安全撤销已经共享的提交
git revert <commit>
```

不要使用 `git reset --hard` 解决普通问题；共享提交优先用 `git revert`，因为历史清晰且可恢复。

## 6. 连接 GitHub

在 GitHub 创建一个空仓库 `SkyCast`，不要勾选自动生成 README，然后执行：

```powershell
git remote add origin https://github.com/<你的用户名>/SkyCast.git
git push -u origin main
```

如果使用SSH：

```powershell
git remote add origin git@github.com:<你的用户名>/SkyCast.git
git push -u origin main
```

首次连接前用 `git remote -v` 确认地址，避免把代码推到错误仓库。

## 7. PR自检清单

- 对应的 Issue 和验收标准是什么？
- 变化是否只覆盖本次范围？
- 是否新增/更新测试？
- 缺测、过期、异常路径是否处理？
- 是否泄露密钥或真实私人数据？
- 文档和截图是否更新？
- 构建和测试命令是否通过？

## 8. 本项目的Git学习路线

1. M0：`init/status/add/commit/log`；
2. M1：分支、diff、restore、合并；
3. M2：remote、push、Pull Request；
4. M3：冲突解决、rebase、revert；
5. M4：CI、tag、release、changelog。
