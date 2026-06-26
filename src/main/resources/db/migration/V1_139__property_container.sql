create table property_container_mapping
(
    dps_property_container_id   uuid                     not null primary key,
    nomis_property_container_id bigint                   not null,
    booking_id                  bigint                   not null,
    offender_no                 varchar(10)              not null,
    label                       varchar(20),
    mapping_type                varchar(20)              not null,
    when_created                timestamp with time zone not null default now(),
    constraint property_containers_mapping_nomis_id_unique unique (nomis_property_container_id)
);

create index property_containers_mapping_booking on property_container_mapping (label);
create index property_containers_mapping_label_index on property_container_mapping (booking_id);
create index property_containers_mapping_when_created_index on property_container_mapping (when_created);
create index property_container_mapping_offender_no_index ON property_container_mapping (offender_no);
