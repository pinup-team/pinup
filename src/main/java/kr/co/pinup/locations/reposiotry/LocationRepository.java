package kr.co.pinup.locations.reposiotry;

import kr.co.pinup.locations.Location;
import org.springframework.data.repository.CrudRepository;

public interface LocationRepository extends CrudRepository<Location, Long> {

}
