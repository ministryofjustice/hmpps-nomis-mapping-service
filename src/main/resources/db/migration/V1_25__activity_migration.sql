alter table activity_mapping drop column label;

create table activity_migration_mapping (
    nomis_course_activity_id       bigint       not null primary key,
    activity_schedule_id           bigint       not null,
    activity_schedule_id2          bigint,
    label                          varchar(20)  not null,
    when_created                   timestamp with time zone not null default now()
);

create index activity_migration_label_index on activity_migration_mapping (label);
create index activity_migration_when_created_index on activity_migration_mapping (when_created);
