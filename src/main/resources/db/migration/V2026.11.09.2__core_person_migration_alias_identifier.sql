create table core_person_migration_alias_identifier
(
    cpr_id              varchar(36)              not null PRIMARY KEY,
    nomis_prison_number varchar(10)              not null,
    when_created        timestamp with time zone not null default now(),
    label               varchar(20),
    mapping_type        varchar(20)              not null,
    constraint core_person_migration_alias_identifier_prison_number_unique unique (nomis_prison_number)
);
create index core_person_migration_alias_identifier_when_created_index on core_person_migration_alias_identifier (when_created);
create index core_person_migration_alias_identifier_label_index on core_person_migration_alias_identifier (label);
