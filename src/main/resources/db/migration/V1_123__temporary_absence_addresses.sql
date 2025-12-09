alter table temporary_absence_schedule_mapping add column dps_description varchar(255);
alter table temporary_absence_schedule_mapping add column dps_postcode varchar(10);

alter table temporary_absence_movement_mapping add column dps_description varchar(255);
alter table temporary_absence_movement_mapping add column dps_postcode varchar(10);

alter table temporary_absence_address_mapping add column dps_description varchar(255);
alter table temporary_absence_address_mapping add column dps_postcode varchar(10);