alter table activity_schedule_mapping add column activity_schedule_id bigint not null;

create index activity_schedule_activity_schedule_id on activity_schedule_mapping (activity_schedule_id);

