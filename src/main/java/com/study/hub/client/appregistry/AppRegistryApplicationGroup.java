package com.study.hub.client.appregistry;

import java.util.List;

public record AppRegistryApplicationGroup(
        String groupId,
        String displayName,
        long version,
        List<AppRegistryApplicationConfiguration> applications,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
