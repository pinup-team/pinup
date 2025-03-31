package kr.co.pinup.stores.service;

import kr.co.pinup.locations.Location;
import kr.co.pinup.store_categories.StoreCategory;
import kr.co.pinup.store_categories.repository.StoreCategoryRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class StoreServiceTest {

    @InjectMocks
    private StoreService storeService;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreCategoryRepository storeCategoryRepository;

    private Store store;
    private StoreCategory storeCategory;
    private Location location;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        storeCategory = new StoreCategory("팝업스토어");
        location = new Location("테스트 로케이션", "타임스퀘어", "07305", "서울특별시", 37.517081, 126.903357, "서울특별시 영등포구 영중로 15", "3층 팝업존 칼하트 옆");
//        store = new Store("[SOUP X TOTI] 숲 팬시스토어", "SOUP X TOTI 숲과 토티가 함께하는 팬시 팝업스토어", storeCategory, location, LocalDate.of(2025, 2, 3), LocalDate.of(2025, 2, 20), Status.RESOLVED, "");

    }

    @Test
    @DisplayName("팝업스토어 단일 조회 성공 테스트")
    void getStoreById_Success() {

    }


}
