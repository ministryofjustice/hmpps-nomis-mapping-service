create table incident_mapping
(
    incident_id         varchar(64)  not null PRIMARY KEY,
    nomis_incident_id   bigint       not null,
    label               varchar(20),
    mapping_type        varchar(20)  not null,
    when_created        timestamp with time zone not null default now()
);

create index incident_mapping_label_index on incident_mapping (label);
create index incident_mapping_when_created_index on incident_mapping (when_created);
