create table csip_factor_mapping
(
    dps_csip_factor_id      varchar(64)  not null PRIMARY KEY,
    nomis_csip_factor_id    bigint       not null,
    label                   varchar(20),
    mapping_type            varchar(20)  not null,
    when_created            timestamp with time zone not null default now(),

    constraint csip_factor_mapping_nomis_id_unique unique (nomis_csip_factor_id)
);

create index csip_factor_mapping_label_index on csip_factor_mapping (label);
create index csip_factor_mapping_when_created_index on csip_factor_mapping (when_created);
