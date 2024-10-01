create table person_employment_mapping
(
    dps_id                varchar(36)              not null PRIMARY KEY,
    nomis_person_id       bigint                   not null,
    nomis_sequence_number bigint                   not null,
    when_created          timestamp with time zone not null default now(),
    label                 varchar(20),
    mapping_type          varchar(20)              not null,
    constraint person_employment_mapping_nomis_id_unique unique (nomis_person_id, nomis_sequence_number)
);
create index person_employment_mapping_when_created_index on person_employment_mapping (when_created);
create index person_employment_mapping_label_index on person_employment_mapping (label);
