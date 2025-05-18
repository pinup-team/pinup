package kr.co.pinup.stores.repository;

import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {
    List<Store> findByStatusAndDeletedFalse(Status status);

    List<Store> findByDeletedFalse();
}
