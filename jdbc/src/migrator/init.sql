--changeset db_changelog
create table db_changelog(
  id text not null primary key,
  filePath text,
  context text,
  checksum bigint,
  statements text[],
  rowsAffected integer not null default 0,
  createdAt timestamptz not null default current_timestamp
);
