create table core_person_phone_mapping
(
    nomis_id       bigint                   not null PRIMARY KEY,
    cpr_id         varchar(36)              not null,
    cpr_phone_type varchar(20)              not null,
    when_created   timestamp with time zone not null default now(),
    label          varchar(20),
    mapping_type   varchar(20)              not null,
    constraint core_person_phone_mapping_cpr_id_unique unique (cpr_id, cpr_phone_type)
);
create index core_person_phone_mapping_when_created_index on core_person_phone_mapping (when_created);
create index core_person_phone_mapping_label_index on core_person_phone_mapping (label);
