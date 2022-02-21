
alter table visit_id add column when_created timestamp with time zone not null default now();

create index visit_id_when_created_index on visit_id (when_created);
