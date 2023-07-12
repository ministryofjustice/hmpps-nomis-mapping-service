create table adjudication_mapping (
    adjudication_number     bigint                      not null  primary key,
    when_created            timestamp with time zone    not null  default now(),
    label                   varchar(20),
    mapping_type            varchar(20)                 not null
);
create index adjudication_mapping_when_created_index on adjudication_mapping (when_created);
create index adjudication_mapping_label_index on adjudication_mapping (label);
