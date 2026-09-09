package com.study.hub.catalog;

import com.study.hub.client.appregistry.AppRegistryClient;
import com.study.hub.client.appregistry.dto.CatalogApplicationResponse;
import com.study.hub.client.appregistry.dto.CatalogGroupResponse;
import com.study.hub.client.appregistry.dto.LocalizedTextResponse;
import com.study.hub.client.appregistry.dto.MaintenanceMessagesResponse;
import com.study.hub.client.appregistry.model.ApplicationStatus;
import com.study.hub.client.appregistry.model.GroupLayout;
import com.study.hub.exception.AppRegistryUnavailableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AppRegistryClient appRegistryClient;

    @Test
    void returnsCatalogWithoutChangingAppRegistryContract() throws Exception {
        when(appRegistryClient.getCatalog()).thenReturn(catalog());

        mockMvc.perform(get("/hub/api/v1/catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("ecosystem"))
                .andExpect(jsonPath("$.groupId").doesNotExist())
                .andExpect(jsonPath("$.type").value("GROUP"))
                .andExpect(jsonPath("$.layout").value("GRID"))
                .andExpect(jsonPath("$.title.ru").value("Экосистема"))
                .andExpect(jsonPath("$.items[0].id").value("insurance"))
                .andExpect(jsonPath("$.items[0].items[0].id")
                        .value("insurance-app"))
                .andExpect(jsonPath("$.items[0].items[0].appId").doesNotExist())
                .andExpect(jsonPath("$.items[0].items[0].status")
                        .value("MAINTENANCE"))
                .andExpect(jsonPath("$.items[0].items[0].maintenanceMessages.en")
                        .value("Maintenance"));

        verify(appRegistryClient).getCatalog();
    }

    @Test
    void hidesAppRegistryFailureBehindDependencyError() throws Exception {
        when(appRegistryClient.getCatalog()).thenThrow(new AppRegistryUnavailableException());

        mockMvc.perform(get("/hub/api/v1/catalog"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("DEPENDENCY_UNAVAILABLE"));
    }

    private CatalogGroupResponse catalog() {
        return new CatalogGroupResponse(
                "ecosystem",
                GroupLayout.GRID,
                text("Экосистема", "Экожүйе", "Ecosystem"),
                null,
                List.of(new CatalogGroupResponse(
                        "insurance",
                        GroupLayout.LIST,
                        text("Страхование", "Сақтандыру", "Insurance"),
                        null,
                        List.of(new CatalogApplicationResponse(
                                "insurance-app",
                                text("КАСКО", "КАСКО", "CASCO"),
                                null,
                                ApplicationStatus.MAINTENANCE,
                                new MaintenanceMessagesResponse(
                                        "Технические работы",
                                        "Техникалық жұмыстар",
                                        "Maintenance"
                                )
                        ))
                ))
        );
    }

    private LocalizedTextResponse text(String ru, String kk, String en) {
        return new LocalizedTextResponse(ru, kk, en);
    }
}
