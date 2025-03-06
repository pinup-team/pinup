package kr.co.pinup.store_images.repository;

import kr.co.pinup.stores.StoreImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreImageRepository extends JpaRepository<StoreImage, Long> {
    List<StoreImage> findByStoreId(Long storeId);
}
