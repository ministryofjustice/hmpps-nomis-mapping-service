drop index prison_person_migration_pk_index;
create unique index prison_person_migration_pk_index on prison_person_migration_mapping (nomis_prisoner_number, migration_type, label);
