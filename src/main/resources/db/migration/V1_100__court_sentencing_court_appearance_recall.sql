create table court_appearance_recall_mapping
(
    nomis_court_appearance_id bigint                   not null primary key,
    dps_recall_id             varchar                  not null,
    when_created              timestamp with time zone not null default now(),
    label                     varchar(20),
    mapping_type              varchar(20)              not null
);
create index court_appearance_recall_mapping_recall_id on court_appearance_recall_mapping (dps_recall_id);
