--changeset klite:users
create table users(
  ${id},
  email varchar not null,
  firstName varchar not null,
  lastName varchar not null,
  passwordHash varchar,
  locale varchar not null default 'en',
  ${createdAt}
);

--changeset klite:users_email_idx
create unique index users_email_idx on users (email);

--changeset klite:test-user
insert into users (id, email, firstName, lastName) values ('9725b054-426b-11ee-92a5-0bd2a151eea2', 'test@codeborne.com', 'Test', 'User');
