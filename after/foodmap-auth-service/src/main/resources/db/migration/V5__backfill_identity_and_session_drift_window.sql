-- 收口 V4 迁移与应用双写上线之间的窗口期：这段时间注册的账号只写了旧表，登录产生的 Refresh Token
-- 也没有对应会话，导致新表相对旧表存在漂移。本脚本按 V4 完全相同的语义补写一次，可重复执行。

insert into auth_identity_bindings (created_time, updated_time, identity_id, user_id, provider, identity_type,
                                    identity_hash, display_masked, verified, primary_identity, identity_status, bound_time)
select a.created_time, now(), nextval('auth_identity_binding_id_seq'), a.user_id, 'LOCAL', 'ACCOUNT_NAME',
       encode(sha256(lower(trim(a.account_name))::bytea), 'hex'), a.account_name, 1, 1, 'NORMAL', a.created_time
from auth_accounts a
where a.is_delete = 0 and a.account_name is not null and trim(a.account_name) <> ''
  and not exists (select 1 from auth_identity_bindings b
                  where b.is_delete = 0 and b.user_id = a.user_id
                    and b.provider = 'LOCAL' and b.identity_type = 'ACCOUNT_NAME');

insert into auth_identity_bindings (created_time, updated_time, identity_id, user_id, provider, identity_type,
                                    identity_hash, display_masked, verified, primary_identity, identity_status, bound_time)
select a.created_time, now(), nextval('auth_identity_binding_id_seq'), a.user_id, 'LOCAL', 'PHONE',
       encode(sha256(trim(a.phone)::bytea), 'hex'),
       case when length(trim(a.phone)) >= 7
            then substr(trim(a.phone), 1, 3) || '****' || right(trim(a.phone), 4)
            else '***' end,
       1, 0, 'NORMAL', a.created_time
from auth_accounts a
where a.is_delete = 0 and a.phone is not null and trim(a.phone) <> ''
  and not exists (select 1 from auth_identity_bindings b
                  where b.is_delete = 0 and b.user_id = a.user_id
                    and b.provider = 'LOCAL' and b.identity_type = 'PHONE');

insert into auth_identity_bindings (created_time, updated_time, identity_id, user_id, provider, identity_type,
                                    identity_hash, display_masked, verified, primary_identity, identity_status, bound_time)
select a.created_time, now(), nextval('auth_identity_binding_id_seq'), a.user_id, 'LOCAL', 'EMAIL',
       encode(sha256(lower(trim(a.email))::bytea), 'hex'),
       case when position('@' in trim(a.email)) > 1
            then left(trim(a.email), 1) || '***@' || substring(trim(a.email) from position('@' in trim(a.email)) + 1)
            else '***' end,
       1, 0, 'NORMAL', a.created_time
from auth_accounts a
where a.is_delete = 0 and a.email is not null and trim(a.email) <> ''
  and not exists (select 1 from auth_identity_bindings b
                  where b.is_delete = 0 and b.user_id = a.user_id
                    and b.provider = 'LOCAL' and b.identity_type = 'EMAIL');

select setval('auth_identity_binding_id_seq',
              greatest((select coalesce(max(identity_id), 600000) from auth_identity_bindings), 600000),
              true);

insert into auth_sessions (created_time, updated_time, session_id, user_id, refresh_token_hash,
                           expires_time, revoked_time, session_status)
select rt.created_time, now(), 'sess-' || rt.token_id, a.user_id, rt.token_hash,
       rt.expires_time, rt.revoked_time,
       case when rt.revoked_time is not null then 'REVOKED'
            when rt.expires_time <= now() then 'EXPIRED'
            else 'ACTIVE' end
from refresh_tokens rt
join auth_accounts a on a.account_id = rt.account_id and a.is_delete = 0
where rt.is_delete = 0
  and not exists (select 1 from auth_sessions s
                  where s.is_delete = 0 and s.refresh_token_hash = rt.token_hash);
