create table corporate_address_mapping
(
    dps_id       varchar(36)              not null PRIMARY KEY,
    nomis_id     bigint                   not null,
    when_created timestamp with time zone not null default now(),
    label        varchar(20),
    mapping_type varchar(20)              not null,
    constraint corporate_address_mapping_nomis_id_unique unique (nomis_id)
);
create index corporate_address_mapping_when_created_index on corporate_address_mapping (when_created);
create index corporate_address_mapping_label_index on corporate_address_mapping (label);
