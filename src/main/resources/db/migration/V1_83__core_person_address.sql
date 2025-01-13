create table core_person_address_mapping
(
    cpr_id         varchar(36)              not null PRIMARY KEY,
    nomis_id       bigint,
    when_created   timestamp with time zone not null default now(),
    label          varchar(20),
    mapping_type   varchar(20)              not null,
    constraint core_person_mapping_nomis_id_unique unique (nomis_id)
);
create index core_person_address_mapping_when_created_index on core_person_address_mapping (when_created);
create index core_person_address_mapping_label_index on core_person_address_mapping (label);
