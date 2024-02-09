drop table location_mapping;

create table location_mapping (
                                  dps_location_id     varchar(40)              not null   primary key ,
                                  nomis_location_id   bigint                   not null,
                                  label               varchar(20),
                                  mapping_type        varchar(30)              not null,
                                  when_created        timestamp with time zone not null   default now(),
                                  constraint location_mapping_nomis_id_unique unique (nomis_location_id)
);

create index location_mapping_label_index on location_mapping (label);
create index location_mapping_when_created_index on location_mapping (when_created);
create unique index location_mapping_nomis_location_id_index on location_mapping (nomis_location_id);
