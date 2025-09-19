create table prisoner_balance_mapping
(
    nomis_root_offender_id  bigint                   not null,
    dps_id                  varchar(36)              not null PRIMARY KEY,
    when_created            timestamp with time zone not null default now(),
    label                   varchar(20),
    mapping_type            varchar(20)              not null,

    constraint prisoner_balance_mapping_nomis_root_offender_id_unique unique (nomis_root_offender_id)
);

create index prisoner_balance_mapping_when_created_index on prisoner_balance_mapping (when_created);
create index prisoner_balance_mapping_label_index on prisoner_balance_mapping (label);
