create table court_charge_mapping
(
    nomis_court_charge_id      bigint                   not null unique,
    dps_court_charge_id        varchar                  not null primary key,
    when_created               timestamp with time zone not null default now(),
    label                      varchar(20),
    mapping_type               varchar(20)              not null
);
create index court_charge_mapping_when_created_index on court_charge_mapping (when_created);
create index court_charge_mapping_label_index on court_charge_mapping (label);
