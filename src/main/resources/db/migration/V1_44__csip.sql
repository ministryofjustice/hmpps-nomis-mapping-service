create table csip_mapping
(
    dps_csip_id     varchar(64)  not null PRIMARY KEY,
    nomis_csip_id   bigint       not null,
    label           varchar(20),
    mapping_type    varchar(20)  not null,
    when_created    timestamp with time zone not null default now()
);

create index csip_mapping_label_index on csip_mapping (label);
create index csip_mapping_when_created_index on csip_mapping (when_created);
