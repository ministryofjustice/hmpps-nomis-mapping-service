create table core_person_contact_mapping
(
    cpr_id              varchar(36)              not null PRIMARY KEY,
    nomis_id            bigint                   not null,
    nomis_contact_type  varchar(5)               not null,
    nomis_prison_number varchar(10)              not null,
    when_created        timestamp with time zone not null default now(),
    label               varchar(20),
    mapping_type        varchar(20)              not null,
    constraint core_person_contact_mapping_nomis_id_unique unique (nomis_id, nomis_contact_type)
);
create index core_person_contact_mapping_when_created_index on core_person_contact_mapping (when_created);
create index core_person_contact_mapping_label_index on core_person_contact_mapping (label);
create index core_person_contact_mapping_nomis_prison_number_index on core_person_contact_mapping (nomis_prison_number);
