--substitute id=id uuid default gen_random_uuid() primary key
--substitute createdAt=createdAt timestamptz default current_timestamp

--include users.sql
