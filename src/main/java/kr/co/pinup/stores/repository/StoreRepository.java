package kr.co.pinup.stores.repository;

import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findAllByIsDeletedFalse();

    List<Store> findAllByStoreStatusAndIsDeletedFalse(StoreStatus status);

    List<Store> findAllByLocation_SigunguAndStoreStatusAndIsDeletedFalse(String sigungu, StoreStatus selectedStatus);

    List<Store> findAllByLocation_SigunguAndIsDeletedFalse(String sigungu);
}
