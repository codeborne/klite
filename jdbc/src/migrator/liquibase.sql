--changeset migrate-from-liquibase onFail:SKIP
--a sample migration from Liquibase, will work if author:id pairs were unique across different files
insert into db_changelog (id, filePath, context, createdAt)
  select author || ':' || id, filename, contexts, dateexecuted from databasechangelog;
