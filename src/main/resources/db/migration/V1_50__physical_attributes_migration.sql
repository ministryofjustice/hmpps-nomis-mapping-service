create table prison_person_migration_mapping (
    nomis_prisoner_number          varchar(7)   not null primary key,
    label                          varchar(20)  not null,
    when_created                   timestamp with time zone not null default now()
);

create index prison_person_migration_label_index on prison_person_migration_mapping (label);
create index prison_person_migration_when_created_index on prison_person_migration_mapping (when_created);
