--random comment
--substitute test=exec hello
-- and another random

--changeset test onChange:RUN onFail:SKIP separator:xxx context:!prod
begin; ${test}($${json}$$); password='${APP_PASS}'; text='''--'; end;

--changeset multiline
create table something(
  id text primary key,
  -- some comment
  createdAt date
);

-- changeset test2 checksum:123
checksum overridden;
--hello world
