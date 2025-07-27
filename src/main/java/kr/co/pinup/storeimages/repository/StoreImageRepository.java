package kr.co.pinup.storeimages.repository;

import kr.co.pinup.storeimages.StoreImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StoreImageRepository extends JpaRepository<StoreImage, Long> {

    List<StoreImage> findByStoreIdAndIsDeletedFalse(Long storeId);

    Optional<StoreImage> findByStoreIdAndIsThumbnailTrueAndIsDeletedFalse(Long storeId);
}
