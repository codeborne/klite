--changeset db_changelog
create table db_changelog(
  id text not null primary key,
  filePath text not null,
  context text,
  checksum bigint,
  statements text[],
  createdAt timestamptz not null default current_timestamp
);
