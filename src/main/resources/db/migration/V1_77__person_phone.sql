drop table person_phone_mapping;

create table person_phone_mapping
(
    nomis_id       bigint                   not null PRIMARY KEY,
    dps_id         varchar(36)              not null,
    dps_phone_type varchar(20)              not null,
    when_created   timestamp with time zone not null default now(),
    label          varchar(20),
    mapping_type   varchar(20)              not null,
    constraint person_phone_mapping_dps_id_unique unique (dps_id, dps_phone_type)
);
create index person_phone_mapping_when_created_index on person_phone_mapping (when_created);
create index person_phone_mapping_label_index on person_phone_mapping (label);
