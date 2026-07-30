package com.study.hub.partnerapp;

import com.study.hub.client.appregistry.AppRegistryApplicationConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface PartnerApplicationMapper {

    PartnerApplicationResponse toResponse(AppRegistryApplicationConfiguration source);
}
