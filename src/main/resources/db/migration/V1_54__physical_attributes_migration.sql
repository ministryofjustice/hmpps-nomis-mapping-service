drop table prison_person_migration_mapping;

create table prison_person_migration_mapping (
  nomis_prisoner_number          varchar(7) not null,
  migration_type                 varchar(20)  not null,
  dps_ids                        varchar(65535) not null,
  label                          varchar(20)  not null,
  when_created                   timestamp with time zone not null default now()
);

create unique index prison_person_migration_pk_index on prison_person_migration_mapping (nomis_prisoner_number, migration_type);
create index prison_person_migration_label_index on prison_person_migration_mapping (label);
create index prison_person_migration_when_created_index on prison_person_migration_mapping (when_created);