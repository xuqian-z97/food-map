create sequence if not exists auth_account_id_seq start with 100001 increment by 1;
select setval('auth_account_id_seq', greatest((select coalesce(max(account_id), 100000) from auth_accounts), 100000), true);
comment on sequence auth_account_id_seq is '认证账号业务主键序列，用于 auth_accounts.account_id，避免服务重启后业务主键重复';

create sequence if not exists auth_user_id_seq start with 200001 increment by 1;
select setval('auth_user_id_seq', greatest((select coalesce(max(user_id), 200000) from auth_accounts), 200000), true);
comment on sequence auth_user_id_seq is '用户业务主键序列，用于 auth_accounts.user_id，并同步给用户服务';

create sequence if not exists auth_credential_id_seq start with 300001 increment by 1;
select setval('auth_credential_id_seq', greatest((select coalesce(max(credential_id), 300000) from auth_credentials), 300000), true);
comment on sequence auth_credential_id_seq is '认证凭证业务主键序列，用于 auth_credentials.credential_id';

create sequence if not exists auth_refresh_token_id_seq start with 400001 increment by 1;
select setval('auth_refresh_token_id_seq', greatest((select coalesce(max(token_id), 400000) from refresh_tokens), 400000), true);
comment on sequence auth_refresh_token_id_seq is 'Refresh Token 业务主键序列，用于 refresh_tokens.token_id';

create sequence if not exists auth_login_log_id_seq start with 500001 increment by 1;
select setval('auth_login_log_id_seq', greatest((select coalesce(max(login_log_id), 500000) from login_logs), 500000), true);
comment on sequence auth_login_log_id_seq is '登录日志业务主键序列，用于 login_logs.login_log_id，解决登录日志写入重复键问题';
