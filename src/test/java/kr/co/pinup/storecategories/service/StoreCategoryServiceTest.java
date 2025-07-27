package kr.co.pinup.storecategories.service;

import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storecategories.model.dto.StoreCategoryResponse;
import kr.co.pinup.storecategories.repository.StoreCategoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class StoreCategoryServiceTest {

    @Mock
    private StoreCategoryRepository storeCategoryRepository;

    @InjectMocks
    private StoreCategoryService storeCategoryService;

    @DisplayName("Store 카테고리 전체를 조회한다.")
    @Test
    void getCategories() {
        // Arrange
        final StoreCategory storeCategory1 = getStoreCategory("리빙&라이프스타일");
        final StoreCategory storeCategory2 = getStoreCategory("브랜드 체험");
        final StoreCategory storeCategory3 = getStoreCategory("뷰티");
        final StoreCategory storeCategory4 = getStoreCategory("패션");
        final StoreCategory storeCategory5 = getStoreCategory("아트&컬처");
        final StoreCategory storeCategory6 = getStoreCategory("F&B");
        final StoreCategory storeCategory7 = getStoreCategory("엔터테인먼트&캐릭터");
        final StoreCategory storeCategory8 = getStoreCategory("테크&가전");

        final List<StoreCategory> storeCategories = List.of(
                storeCategory1,
                storeCategory2,
                storeCategory3,
                storeCategory4,
                storeCategory5,
                storeCategory6,
                storeCategory7,
                storeCategory8
        );

        given(storeCategoryRepository.findAll()).willReturn(storeCategories);

        // Act
        final List<StoreCategoryResponse> result = storeCategoryService.getCategories();

        // Assert
        assertThat(result).hasSize(8)
                .extracting("name")
                .containsOnly(
                        "리빙&라이프스타일",
                        "브랜드 체험",
                        "뷰티",
                        "패션",
                        "아트&컬처",
                        "F&B",
                        "엔터테인먼트&캐릭터",
                        "테크&가전"
                );

        then(storeCategoryRepository).should(times(1))
                .findAll();
    }

    @DisplayName("카테고리 ID로 조회한다.")
    @Test
    void getCategory() {
        // Arrange
        final long categoryId = 1L;
        final String categoryItem = "패션";
        final StoreCategory category = getStoreCategory(categoryItem);

        given(storeCategoryRepository.findById(categoryId)).willReturn(Optional.ofNullable(category));

        // Act
        final StoreCategoryResponse result = storeCategoryService.getCategory(categoryId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo(categoryItem);

        then(storeCategoryRepository).should(times(1))
                .findById(categoryId);
    }

    private StoreCategory getStoreCategory(final String name) {
        return StoreCategory.builder()
                .name(name)
                .build();
    }
}