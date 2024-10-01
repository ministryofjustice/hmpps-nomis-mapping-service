create table person_contact_mapping
(
    dps_id       varchar(36)              not null PRIMARY KEY,
    nomis_id     bigint                   not null,
    when_created timestamp with time zone not null default now(),
    label        varchar(20),
    mapping_type varchar(20)              not null,
    constraint person_contact_mapping_nomis_id_unique unique (nomis_id)
);
create index person_contact_mapping_when_created_index on person_contact_mapping (when_created);
create index person_contact_mapping_label_index on person_contact_mapping (label);
