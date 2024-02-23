create table court_appearance_mapping
(
    nomis_court_appearance_id  bigint                   not null unique,
    dps_court_appearance_id    varchar                  not null primary key,
    when_created               timestamp with time zone not null default now(),
    label                      varchar(20),
    mapping_type               varchar(20)              not null
);
create index court_appearance_mapping_when_created_index on court_appearance_mapping (when_created);
create index court_appearance_mapping_label_index on court_appearance_mapping (label);
