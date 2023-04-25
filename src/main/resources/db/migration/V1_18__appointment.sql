alter table appointment_mapping add column mapping_type varchar(20) not null default 'APPOINTMENT_CREATED';
alter table appointment_mapping add column label        varchar(20);
