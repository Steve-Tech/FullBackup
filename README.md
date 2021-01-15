# FullBackup
A light-weight plugin to zip up your server's files on a cron-like schedule.
## Config
```yaml
# Config File for FullBackup
backup-folder: 'backup'
schedule:
  enabled: true
  minute: '0'
  hour: '0'
  day: '*'
  month: '*'
  day-of-week: '*'
```
## Commands
- FullBackup:
  - Aliases: backup
  - Description: Backup the server
  - Usage: /\<command\> \[start|stop|reload\]
  - Permission: FullBackup
  - Default: op