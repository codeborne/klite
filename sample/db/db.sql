--substitute id=id uuid default uuid_generate_v4() primary key
--substitute createdAt=createdAt timestamptz default current_timestamp

--changeset uuid-support
create extension if not exists "uuid-ossp";

--include users.sql
