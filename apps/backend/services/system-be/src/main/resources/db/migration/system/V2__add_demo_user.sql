-- Demo user (password: demo123)
-- Demo user has read-only privileges enforced in the application layer
-- (UserServiceImpl blocks modifications of any user other than self).
INSERT INTO sys_user (id, tenant_id, username, password, nickname, status, can_switch_tenant, create_time)
VALUES (2, 1, 'demo', '$2a$10$RMjyJxeJsUHCEO3zX/zP.OcqIeBlUOS02kEuODfnyNMjhSos3aOOK', 'Demo User', 1, 0, NOW())
ON DUPLICATE KEY UPDATE username = VALUES(username);

-- Assign Administrator role to demo user so it can browse the system
INSERT INTO sys_user_role (user_id, role_id, tenant_id) VALUES (2, 1, 1)
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id);
