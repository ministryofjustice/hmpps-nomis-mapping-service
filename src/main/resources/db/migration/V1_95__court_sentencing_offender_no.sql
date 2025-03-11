create table court_case_prisoner_migration
(
    offender_no  varchar(10)              not null PRIMARY KEY,
    count        bigint                   not null,
    when_created timestamp with time zone not null default now(),
    label        varchar(20),
    mapping_type varchar(20)              not null
);
create index court_case_prisoner_migration_label_index on court_case_prisoner_migration (label);
