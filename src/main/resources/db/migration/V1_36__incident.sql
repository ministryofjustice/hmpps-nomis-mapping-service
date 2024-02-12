alter table incident_mapping
    add constraint incident_mapping_nomis_id_unique unique (nomis_incident_id);
