package com.xngl.manager.org;

public record CreateOrgResult(
    long orgId,
    long adminUserId,
    String adminUsername,
    String plainPassword) {}
