create table prison_balance_mapping
(
    nomis_id               varchar(6)               not null,
    dps_id                  varchar(6)               not null PRIMARY KEY,
    when_created            timestamp with time zone not null default now(),
    label                   varchar(20),
    mapping_type            varchar(20)              not null,

    constraint prison_balance_mapping_nomis_id_unique unique (nomis_id)
);

create index prison_balance_mapping_when_created_index on prison_balance_mapping (when_created);
create index prison_balance_mapping_label_index on prison_balance_mapping (label);
