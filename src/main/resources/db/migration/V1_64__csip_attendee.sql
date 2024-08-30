alter table csip_attendee_mapping add column
    dps_csip_report_id      varchar(64) not null;

alter table csip_attendee_mapping add constraint
    dps_csip_report_id_fk1 FOREIGN KEY (dps_csip_report_id)
    REFERENCES csip_mapping (dps_csip_id);

create index csip_attendee_mapping_dps_csip_report_id_index ON csip_attendee_mapping (dps_csip_report_id);
