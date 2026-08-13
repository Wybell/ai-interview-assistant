# 部署与验收手册

适用环境：腾讯云单机 Docker Compose 部署。本文不包含任何真实 IP、账号、密码或 API Key。

## 日常发布

日常发布使用 GitHub Actions，避免手工登录服务器执行构建命令：

1. 本地完成验证并将项目代码推送到 `main`。
2. 在 GitHub Actions 等待 `Continuous Integration` 通过。后端 CI 会使用隔离的 MySQL 8、Redis 和非敏感占位配置运行完整测试；前端 CI 会执行 lint、Vitest 和生产构建。
3. 手动运行 `Deploy Production`，勾选确认后开始发布。
4. 工作流会在腾讯云自动创建数据库备份、校验待发布提交、拉取代码、重建容器，并验证前端 `200` 与受保护后端接口 `401`。
5. 完成浏览器验收。工作流成功仅表示部署和基础健康检查成功，不替代用户流程验收。

详细的首次配置、Secret 说明和恢复边界见 [GitHub CI 与手动发布](ci-cd.md)。

## 发布前检查

1. 本地 `main` 已推送，且 `git status -sb` 干净。
2. 后端测试通过：

   ```powershell
   Set-Location backend
   .\mvnw.cmd '-Dspring.flyway.enabled=false' test
   ```

3. 前端检查通过：

   ```powershell
   Set-Location frontend
   pnpm lint
   pnpm test:run
   pnpm build
   ```

4. 服务器外部配置已存在并且不在 Git 中：

   ```text
   /opt/ai-interview/config/.env
   /opt/ai-interview/config/application-prod.properties
   ```

5. 新增配置时先检查键名，不打印值：

   ```bash
   awk -F= '/^[[:space:]]*app\.ai\./ { print $1 }' \
     /opt/ai-interview/config/application-prod.properties
   ```

## 手动应急发布

以下命令仅用于 GitHub Actions 不可用时的受控排障或恢复，不是日常发布流程。执行前应确认 CI 对目标提交已通过。

## 备份

在服务器执行。该步骤备份外部配置与数据库，不会停止服务。

```bash
set -e

backup_dir="/opt/ai-interview/config/backups/release-$(date +%Y%m%d-%H%M%S)"
sudo install -d -m 700 "$backup_dir"
sudo cp -p /opt/ai-interview/config/.env "$backup_dir/.env"
sudo cp -p /opt/ai-interview/config/application-prod.properties \
  "$backup_dir/application-prod.properties"

set -a
. /opt/ai-interview/config/.env
set +a

sudo docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" ai-interview-mysql \
  mysqldump -uroot --single-transaction --routines --events interview_db \
  > "$backup_dir/interview_db.sql"

sudo ls -lh "$backup_dir"
```

## 拉取与构建

使用仓库所有者拉取代码，避免 Git 属主问题：

```bash
sudo -u ubuntu -H bash -c '
cd /opt/ai-interview/app
git pull --ff-only origin main
git status -sb
'
```

检查 Compose 配置后再替换容器：

```bash
cd /opt/ai-interview/app

sudo docker compose -p backend \
  --env-file /opt/ai-interview/config/.env \
  -f backend/docker-compose.prod.yml \
  config --quiet

sudo docker compose -p backend \
  --env-file /opt/ai-interview/config/.env \
  -f backend/docker-compose.prod.yml \
  up -d --build backend frontend
```

Flyway 会在后端启动时执行新增迁移。不要通过 Navicat 或 MySQL 控制台手动执行已提交到 `db/migration` 的 SQL。

## 发布后检查

```bash
cd /opt/ai-interview/app

sudo docker compose -p backend \
  --env-file /opt/ai-interview/config/.env \
  -f backend/docker-compose.prod.yml ps

sudo docker compose -p backend \
  --env-file /opt/ai-interview/config/.env \
  -f backend/docker-compose.prod.yml logs --tail 120 backend

curl -sS -o /dev/null -w 'frontend=%{http_code}\n' http://127.0.0.1:8085/
curl -sS -o /dev/null -w 'backend-auth-check=%{http_code}\n' http://127.0.0.1:8082/api/ai/models
sudo nginx -t
```

期望：前端 `200`，未携带 JWT 的模型接口 `401`，Nginx 配置校验成功；后端日志应显示 Flyway 已到目标版本。

## 用户验收清单

- 登录后模型下拉能显示 DeepSeek、Terra、Luna，切换后可保存偏好。
- 三种练习出题模式都可生成中文题目；上一题/下一题行为符合预期。
- 能提交回答并获得评分、参考要点和建议。
- 能上传 10 MB 以内的 PDF、DOCX、TXT 简历并预览；重启后端容器后仍可预览已上传简历。
- 能完成一次模拟面试，验证公司选填、四种轮次、逐题评分、每题最多两次追问、语音转文字、提前结束、复盘、再来一场和重新设置。
- 在浏览器正常入口下检查另一个项目仍可访问。

## 回滚原则

- 先停止继续发布，保留现有容器日志与备份目录。
- 代码回滚到已验证提交后，使用相同 Compose 命令重建后端和前端。
- 数据库迁移不可通过删除 `flyway_schema_history` 回滚。只有确认数据问题时，才在停机窗口按本次备份恢复数据库，并重新验证应用版本兼容性。
