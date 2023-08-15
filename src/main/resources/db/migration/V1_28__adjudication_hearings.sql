create table adjudication_hearing_mapping
(
    nomis_hearing_id bigint                   not null,
    dps_hearing_id   varchar(30)              not null primary key,
    when_created     timestamp with time zone not null default now(),
    label            varchar(20),
    mapping_type     varchar(20)              not null
);
create index adjudication_hearing_mapping_nomis_id_index on adjudication_hearing_mapping (nomis_hearing_id);
create index adjudication_hearing_mapping_when_created_index on adjudication_hearing_mapping (when_created);
create index adjudication_hearing_mapping_label_index on adjudication_hearing_mapping (label);
