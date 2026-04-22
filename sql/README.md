# EchoIM SQL 执行说明

## 执行顺序

请严格按以下顺序执行：

1. [数据库设计.sql](/Users/cheers/Desktop/workspace/EchoIM/sql/数据库设计.sql)
2. [初始化数据.sql](/Users/cheers/Desktop/workspace/EchoIM/sql/初始化数据.sql)

## 适用于当前 Docker MySQL 的执行命令

当前已确认容器名为 `mysql`，并且容器内 MySQL 服务状态正常。

执行建库建表：

```bash
docker exec -i mysql sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < sql/数据库设计.sql
```

执行初始化数据：

```bash
docker exec -i mysql sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD"' < sql/初始化数据.sql
```

## 验证命令

查看数据库：

```bash
docker exec mysql sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -e "SHOW DATABASES LIKE '\''echoim'\'';"'
```

查看核心表：

```bash
docker exec mysql sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -D echoim -e "SHOW TABLES;"'
```

查看初始化用户：

```bash
docker exec mysql sh -lc 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -D echoim -e "SELECT id, username, nickname FROM im_user;"'
```
