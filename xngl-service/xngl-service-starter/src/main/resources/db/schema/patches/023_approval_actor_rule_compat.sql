ALTER TABLE sys_approval_actor_rule ADD COLUMN process_key VARCHAR(64) NULL AFTER tenant_id;
ALTER TABLE sys_approval_actor_rule ADD COLUMN rule_name VARCHAR(128) NULL AFTER process_key;
ALTER TABLE sys_approval_actor_rule ADD COLUMN rule_type VARCHAR(32) NULL AFTER rule_name;
ALTER TABLE sys_approval_actor_rule ADD COLUMN rule_expression VARCHAR(255) NULL AFTER rule_type;
ALTER TABLE sys_approval_actor_rule ADD COLUMN status VARCHAR(32) NOT NULL DEFAULT 'ENABLED' AFTER priority;

UPDATE sys_approval_actor_rule
SET process_key = biz_type
WHERE process_key IS NULL;

UPDATE sys_approval_actor_rule
SET rule_name = CONCAT(biz_type, '-', node_code)
WHERE rule_name IS NULL;

UPDATE sys_approval_actor_rule
SET rule_type = actor_type
WHERE rule_type IS NULL;

UPDATE sys_approval_actor_rule
SET rule_expression = actor_ref_id
WHERE rule_expression IS NULL;

UPDATE sys_approval_actor_rule
SET status = 'ENABLED'
WHERE status IS NULL OR status = '';
