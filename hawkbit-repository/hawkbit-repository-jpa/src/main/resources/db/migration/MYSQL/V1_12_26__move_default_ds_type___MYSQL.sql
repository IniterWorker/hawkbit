INSERT INTO sp_tenant_configuration (created_at, created_by, last_modified_at, last_modified_by, optlock_revision, tenant, conf_key, conf_value)
SELECT created_at, created_by, last_modified_at, last_modified_by, optlock_revision, tenant, 'default.ds.type', default_ds_type
FROM sp_tenant;

/*
Check how to add constraint for existence of ds_type id--

alter table sp_tenant
    add constraint fk_tenant_md_default_ds_type
        foreign key (default_ds_type)
            references sp_distribution_set_type (id);
*/