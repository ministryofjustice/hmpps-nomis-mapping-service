drop index temporary_absence_address_mapping_unique;

-- each DPS address can map to 1 CORP address (null nomis_offender_no), or multiple OFF addresses (1 per nomis_offender_no)
-- as offender_no is part of the unique constraint we need to treat nulls as not distinct, e.g. there can be only 1 null value when other fields are the same (which is not the default behaviour)
create unique index temporary_absence_address_mapping_unique
  on temporary_absence_address_mapping
    (nomis_address_id, nomis_address_owner_class, nomis_offender_no, dps_uprn, dps_address_text)
  nulls not distinct;