create table csip_attendee_mapping
(
    dps_csip_attendee_id      varchar(64)  not null PRIMARY KEY,
    nomis_csip_attendee_id    bigint       not null,
    label                   varchar(20),
    mapping_type            varchar(20)  not null,
    when_created            timestamp with time zone not null default now(),

    constraint csip_attendee_mapping_nomis_id_unique unique (nomis_csip_attendee_id)
);

create index csip_attendee_mapping_label_index on csip_attendee_mapping (label);
create index csip_attendee_mapping_when_created_index on csip_attendee_mapping (when_created);
