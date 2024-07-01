alter table csip_mapping
    add constraint csip_mapping_nomis_id_unique unique (nomis_csip_id);
