package kr.co.pinup.stores.controller;

import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.locations.model.dto.LocationResponse;
import kr.co.pinup.storecategories.model.dto.StoreCategoryResponse;
import kr.co.pinup.storecategories.service.StoreCategoryService;
import kr.co.pinup.stores.exception.StoreNotFoundException;
import kr.co.pinup.stores.model.dto.StoreResponse;
import kr.co.pinup.stores.model.dto.StoreThumbnailResponse;
import kr.co.pinup.stores.model.enums.StoreStatus;
import kr.co.pinup.stores.service.StoreService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static kr.co.pinup.stores.model.enums.StoreStatus.RESOLVED;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = StoreController.class,
        excludeAutoConfiguration = {
                ThymeleafAutoConfiguration.class,
                SecurityAutoConfiguration.class,
                OAuth2ClientAutoConfiguration.class
        })
public class StoreControllerTest {

    private static final String VIEW_PATH = "views/stores";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StoreService storeService;

    @MockitoBean
    private StoreCategoryService categoryService;

    @MockitoBean
    private AppLogger appLogger;

    @DisplayName("팝업스토어 list 페이지 뷰를 반환한다")
    @Test
    void listStores() throws Exception {
        // Arrange
        final StoreStatus status = RESOLVED;
        final String sigungu = "all";
        final List<StoreThumbnailResponse> stores = List.of();

        given(storeService.getStoresByStatus(status)).willReturn(stores);

        // Act & Assert
        mockMvc.perform(get("/stores")
                        .param("status", "resolved")
                        .param("sigungu", sigungu))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/list"))
                .andExpect(model().attributeExists("selectedStatus"))
                .andExpect(model().attributeExists("stores"));

        then(storeService).should(times(1))
                .findAll(status, sigungu);
    }

    @DisplayName("팝업스토어 detail 페이지 뷰를 반환한다")
    @Test
    void storeDetail() throws Exception {
        // Arrange
        final long id = 1L;

        final StoreResponse storeResponse = getStoreResponse();

        given(storeService.getStoreById(id)).willReturn(storeResponse);

        // Act & Assert
        mockMvc.perform(get("/stores/{id}", id))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/detail"))
                .andExpect(model().attributeExists("store"))
                .andExpect(model().attributeExists("location"))
                .andExpect(model().attributeExists("storeImages"));

        then(storeService).should(times(1))
                .getStoreById(id);
    }

    @DisplayName("존재하지 않는 ID로 detail 페이지를 요청시에 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void storeDetailWithNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        final long id = Long.MAX_VALUE;

        given(storeService.getStoreById(id)).willThrow(new StoreNotFoundException());

        // Act & Assert
        mockMvc.perform(get("/stores/{id}", id))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(storeService).should(times(1))
                .getStoreById(id);
    }

    @DisplayName("팝업스토어 create 페이지 뷰를 반환한다")
    @Test
    void createStoreForm() throws Exception {
        // Arrange
        given(categoryService.getCategories()).willReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/stores/create"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/create"))
                .andExpect(model().attributeExists("categories"));

        then(categoryService).should(times(1))
                .getCategories();
    }

    @DisplayName("팝업스토어 update 페이지 뷰를 반환한다")
    @Test
    void editStoreForm() throws Exception {
        // Arrange
        final long id = 1L;
        final StoreResponse storeResponse = getStoreResponse();

        given(storeService.getStoreById(id)).willReturn(storeResponse);
        given(categoryService.getCategories()).willReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/stores/{id}/update", id))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name(VIEW_PATH + "/update"))
                .andExpect(model().attributeExists("store"))
                .andExpect(model().attributeExists("categories"));

        then(storeService).should(times(1))
                .getStoreById(id);
        then(categoryService).should(times(1))
                .getCategories();
    }

    @DisplayName("존재하지 않는 ID로 update 페이지를 요청시에 404 NOT_FOUND와 error 페이지를 반환한다.")
    @Test
    void editStoreFormWithNonExistIdReturnNotFoundAndErrorView() throws Exception {
        // Arrange
        final long id = Long.MAX_VALUE;

        given(storeService.getStoreById(id)).willThrow(new StoreNotFoundException());

        // Act & Assert
        mockMvc.perform(get("/stores/{id}/update", id))
                .andDo(print())
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attributeExists("error"));

        then(storeService).should(times(1))
                .getStoreById(id);
    }

    private StoreResponse getStoreResponse() {
        return new StoreResponse(
                1L,
                "store",
                "description",
                RESOLVED,
                LocalDate.now(),
                LocalDate.now().plusDays(10),
                "",
                "https://instgram.com/test",
                0,
                new StoreCategoryResponse(1L, "뷰티", LocalDateTime.now(), null),
                LocationResponse.builder()
                        .name("서울 송파구 올림픽로 300")
                        .zonecode("05551")
                        .sido("서울")
                        .sigungu("송파구")
                        .address("서울 송파구 올림픽로 300")
                        .longitude(127.104302)
                        .latitude(37.513713)
                        .build(),
                List.of(),
                List.of(),
                LocalDateTime.now(),
                null
        );
    }

}
