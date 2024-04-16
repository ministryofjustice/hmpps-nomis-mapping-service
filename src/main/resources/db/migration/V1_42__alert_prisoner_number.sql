alter table alert_mapping
    add column offender_no varchar(10);

create index alert_mapping_offender_no on alert_mapping (offender_no);

create table alert_prisoner_mapping
(
    offender_no  varchar(10)              not null PRIMARY KEY,
    count        bigint                   not null,
    when_created timestamp with time zone not null default now(),
    label        varchar(20),
    mapping_type varchar(20)              not null
);
create index alert_prisoner_mapping_label_index on alert_prisoner_mapping (label);
