create table csip_plan_mapping
(
    dps_csip_plan_id      varchar(64)  not null PRIMARY KEY,
    nomis_csip_plan_id    bigint       not null,
    label                   varchar(20),
    mapping_type            varchar(20)  not null,
    when_created            timestamp with time zone not null default now(),

    constraint csip_plan_mapping_nomis_id_unique unique (nomis_csip_plan_id)
);

create index csip_plan_mapping_label_index on csip_plan_mapping (label);
create index csip_plan_mapping_when_created_index on csip_plan_mapping (when_created);
