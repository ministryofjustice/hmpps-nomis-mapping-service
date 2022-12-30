create table activity_mapping (
    activity_schedule_id           bigint       not null  PRIMARY KEY,
    nomis_course_activity_id       bigint       not null,
    mapping_type                   varchar(20)  not null,
    constraint activity_mapping_nomis_id_unique unique(nomis_course_activity_id)
);
