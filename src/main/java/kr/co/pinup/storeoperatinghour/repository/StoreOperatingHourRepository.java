package kr.co.pinup.storeoperatinghour.repository;

import kr.co.pinup.storeoperatinghour.StoreOperatingHour;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StoreOperatingHourRepository extends JpaRepository<StoreOperatingHour, Long> {
}
