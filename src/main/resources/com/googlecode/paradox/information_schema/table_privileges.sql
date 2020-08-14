select distinct grantor,
                grantee,
                "catalog" as table_catalog,
                "schema"  as table_schema,
                "table"   as table_name,
                type      as privilege_type,
                is_grantable
from information_schema.pdx_column_privileges
