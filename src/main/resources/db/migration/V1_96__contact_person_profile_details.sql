create table contact_person_profile_detail_migration_mapping (
   nomis_prisoner_number          varchar(7) not null,
   domestic_status_dps_ids        varchar(65535) not null,
   number_of_children_dps_ids     varchar(65535) not null,
   label                          varchar(20)  not null,
   when_created                   timestamp with time zone not null default now()
);

create unique index contact_person_profile_details_migration_pk_index on contact_person_profile_detail_migration_mapping (nomis_prisoner_number, label);
create index contact_person_profile_details_migration_label_index on contact_person_profile_detail_migration_mapping (label);
create index contact_person_profile_details_migration_when_created_index on contact_person_profile_detail_migration_mapping (when_created);