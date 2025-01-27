--changeset users
create table users(
  ${id},
  email varchar not null,
  firstName varchar not null,
  lastName varchar not null,
  passwordHash varchar,
  locale varchar not null default 'en',
  ${createdAt}
);

--changeset users_email_idx
create unique index users_email_idx on users (email);

--changeset test-user
insert into users (id, email, firstName, lastName) values ('9725b054-426b-11ee-92a5-0bd2a151eea2', 'test@codeborne.com', 'Test', 'User');

--changeset users.updatedAt
alter table users add column updatedAt timestamptz not null default now();

--changeset users.avatarUrl
alter table users add column avatarUrl text;
