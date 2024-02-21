create table court_case_mapping
(
    nomis_court_case_id bigint                   not null unique,
    dps_court_case_id   varchar                  not null primary key,
    when_created        timestamp with time zone not null default now(),
    label               varchar(20),
    mapping_type        varchar(20)              not null
);
create index court_case_mapping_nomis_id_index on court_case_mapping (nomis_court_case_id);
create index court_case_mapping_when_created_index on court_case_mapping (when_created);
create index court_case_mapping_label_index on court_case_mapping (label);
