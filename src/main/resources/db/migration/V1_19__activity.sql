create table activity_schedule_mapping (
    scheduled_instance_id          bigint       not null  PRIMARY KEY,
    nomis_course_schedule_id       bigint       not null,
    mapping_type                   varchar(20)  not null,
    when_created                   timestamp with time zone not null default now(),
    constraint activity_schedule_mapping_crs_sch_id_unique unique(nomis_course_schedule_id)
);

alter table activity_mapping add column when_created timestamp with time zone not null default now();

create index activity_when_created_index on activity_mapping (when_created);
create index activity_schedule_when_created_index on activity_schedule_mapping (when_created);
