package kr.co.pinup.stores;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storecategories.repository.StoreCategoryRepository;
import kr.co.pinup.stores.model.enums.StoreStatus;
import kr.co.pinup.stores.repository.StoreRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static kr.co.pinup.stores.model.enums.StoreStatus.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Disabled
@SpringBootTest
@ActiveProfiles("test")
public class StoreTransactionIsolationTest {

    @Autowired
    StoreRepository storeRepository;

    @Autowired
    LocationRepository locationRepository;

    @Autowired
    StoreCategoryRepository storeCategoryRepository;

    @Autowired
    PlatformTransactionManager transactionManager;

    @PersistenceContext
    EntityManager entityManager;

    @AfterEach
    void tearDown() {
        storeRepository.deleteAllInBatch();
        storeCategoryRepository.deleteAllInBatch();
        locationRepository.deleteAllInBatch();
    }

    @DisplayName("Read Committed level에서 Non-Repeatable read 현상 발생")
    @Test
    void readCommittedNonRepeatableRead() throws Exception {
        // Arrange
        final Long storeId = getStoreId();

        CountDownLatch readLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);

        AtomicReference<StoreStatus> firstRead = new AtomicReference<>();
        AtomicReference<StoreStatus> secondReadd = new AtomicReference<>();

        // Act
        CompletableFuture<Void> reader = CompletableFuture.runAsync(() -> {
            TransactionTemplate transaction1 = new TransactionTemplate(transactionManager);

            transaction1.executeWithoutResult(status -> {
                final Store store = storeRepository.findById(storeId)
                        .orElseThrow();
                firstRead.set(store.getStoreStatus());
                readLatch.countDown();

                try {
                    updateLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                entityManager.refresh(store);
                secondReadd.set(store.getStoreStatus());
            });
        });

        CompletableFuture<Void> updater = CompletableFuture.runAsync(() -> {
            try {
                readLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            TransactionTemplate transaction2 = new TransactionTemplate(transactionManager);

            transaction2.executeWithoutResult(status -> {
                final Store store = storeRepository.findById(storeId)
                        .orElseThrow();
                store.updateStatus(DISMISSED);
                storeRepository.save(store);
            });
            updateLatch.countDown();
        });

        reader.get();
        updater.get();

        // Assert
        assertThat(firstRead.get()).isEqualTo(RESOLVED);
        assertThat(secondReadd.get()).isEqualTo(DISMISSED);
        assertThat(firstRead.get()).isNotEqualTo(secondReadd.get());
    }

    @DisplayName("Repeatable Read level에서 non-repeatable read 현상 방지")
    @Test
    void repeatableReadWithoutNonRepeatableRead() throws Exception {
        // Arrange
        final Long storeId = getStoreId();

        CountDownLatch readLatch = new CountDownLatch(1);
        CountDownLatch updateLatch = new CountDownLatch(1);

        AtomicReference<StoreStatus> firstRead = new AtomicReference<>();
        AtomicReference<StoreStatus> secondReadd = new AtomicReference<>();

        // Act
        CompletableFuture<Void> reader = CompletableFuture.runAsync(() -> {
            TransactionTemplate transaction1 = new TransactionTemplate(transactionManager);
            transaction1.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

            transaction1.executeWithoutResult(status -> {
                final Store store = storeRepository.findById(storeId)
                        .orElseThrow();
                firstRead.set(store.getStoreStatus());
                readLatch.countDown();

                try {
                    updateLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                entityManager.refresh(store);
                secondReadd.set(store.getStoreStatus());
            });
        });

        CompletableFuture<Void> updater = CompletableFuture.runAsync(() -> {
            try {
                readLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            TransactionTemplate transaction2 = new TransactionTemplate(transactionManager);
            transaction2.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

            transaction2.executeWithoutResult(status -> {
                final Store store = storeRepository.findById(storeId)
                        .orElseThrow();
                store.updateStatus(DISMISSED);
                storeRepository.save(store);
            });
            updateLatch.countDown();
        });

        reader.get();
        updater.get();

        // Assert
        assertThat(firstRead.get()).isEqualTo(RESOLVED);
        assertThat(secondReadd.get()).isEqualTo(RESOLVED);
        assertThat(firstRead.get()).isEqualTo(secondReadd.get());
    }

    @DisplayName("Read Committed level에서 Phantom Read 현상 재현")
    @Test
    void readCommittedPhantomRead() throws Exception {
        // Arrange
        final Location location = createLocation();
        locationRepository.save(location);

        final StoreCategory category = createCategory();
        storeCategoryRepository.save(category);

        storeRepository.save(createStore(
                "Store1",
                "Store Description1",
                RESOLVED,
                category,
                location
        ));

        CountDownLatch readLatch = new CountDownLatch(1);
        CountDownLatch insertLatch = new CountDownLatch(1);

        AtomicReference<List<Store>> firstRead = new AtomicReference<>();
        AtomicReference<List<Store>> secondRead = new AtomicReference<>();

        // Act
        CompletableFuture<Void> reader = CompletableFuture.runAsync(() -> {
            TransactionTemplate transaction1 = new TransactionTemplate(transactionManager);

            transaction1.executeWithoutResult(status -> {
                final List<Store> stores1 = storeRepository.findAll();
                firstRead.set(stores1);
                readLatch.countDown();

                try {
                    insertLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                final List<Store> stores2 = storeRepository.findAll();
                secondRead.set(stores2);
            });
        });

        CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
            try {
                readLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            TransactionTemplate transaction2 = new TransactionTemplate(transactionManager);

            transaction2.executeWithoutResult(status -> {
                storeRepository.save(createStore(
                        "Store2",
                        "Store Description2",
                        RESOLVED,
                        category,
                        location
                ));
            });
            insertLatch.countDown();
        });

        reader.get();
        writer.get();

        // Assert
        assertThat(firstRead.get()).hasSize(1);
        assertThat(secondRead.get()).hasSize(2);
    }

    @DisplayName("Repeatable Read level에서 Phantom Read 현상 방지")
    @Test
    void repeatableReadWithoutPhantomRead() throws Exception {
        // Arrange
        final Location location = createLocation();
        locationRepository.save(location);

        final StoreCategory category = createCategory();
        storeCategoryRepository.save(category);

        storeRepository.save(createStore(
                "Store1",
                "Store Description1",
                RESOLVED,
                category,
                location
        ));

        CountDownLatch readLatch = new CountDownLatch(1);
        CountDownLatch insertLatch = new CountDownLatch(1);

        AtomicReference<List<Store>> firstRead = new AtomicReference<>();
        AtomicReference<List<Store>> secondRead = new AtomicReference<>();

        // Act
        CompletableFuture<Void> reader = CompletableFuture.runAsync(() -> {
            TransactionTemplate transaction1 = new TransactionTemplate(transactionManager);
            transaction1.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

            transaction1.executeWithoutResult(status -> {
                final List<Store> stores1 = storeRepository.findAll();
                firstRead.set(stores1);

                readLatch.countDown();

                try {
                    insertLatch.await();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                final List<Store> stores2 = storeRepository.findAll();
                secondRead.set(stores2);
            });
        });

        CompletableFuture<Void> writer = CompletableFuture.runAsync(() -> {
            try {
                readLatch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            TransactionTemplate transaction2 = new TransactionTemplate(transactionManager);
            transaction2.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);

            transaction2.executeWithoutResult(status -> {
                storeRepository.save(createStore(
                        "Store2",
                        "Store Description2",
                        RESOLVED,
                        category,
                        location
                ));
            });
            insertLatch.countDown();
        });

        reader.get();
        writer.get();

        // Assert
        assertThat(firstRead.get()).hasSize(1);
        assertThat(secondRead.get()).hasSize(1);
        assertThat(firstRead.get().size()).isEqualTo(secondRead.get().size());
    }

    @DisplayName("Read Committed level에서 Lost Update 현상 재현")
    @Test
    void readCommittedLostUpdate() throws Exception {
        // Arrange
        final Long storeId = getStoreId();

        CountDownLatch updateLatch1 = new CountDownLatch(1);
        CountDownLatch updateLatch2 = new CountDownLatch(1);

        // Act
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
           TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

           transactionTemplate.executeWithoutResult(status -> {
               final Store store = storeRepository.findById(storeId)
                       .orElseThrow();
               store.updateStatus(PENDING);

               updateLatch1.countDown();

               try {
                   updateLatch2.await();
               } catch (InterruptedException e) {
                   throw new RuntimeException(e);
               }

               storeRepository.saveAndFlush(store);
           });
        });

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                updateLatch1.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            transactionTemplate.executeWithoutResult(status -> {
                final Store store = storeRepository.findById(storeId)
                        .orElseThrow();
                store.updateStatus(DISMISSED);
                storeRepository.saveAndFlush(store);
            });
            updateLatch2.countDown();
        });

        future1.get();
        future2.get();

        // Assert
        final Store result = storeRepository.findById(storeId)
                .orElseThrow();
        assertThat(result.getStoreStatus()).isEqualTo(PENDING);
    }

    /**
     *  TODO : 낙관적 락 사용해 Lost Update 현상 방지를 위한 테스트
     *  TODO : Store Entity에 Version을 사용해 Lost Update 현상 방지 가능
     */
    @Disabled("낙관적 락 사용하기 위해선 엔티티에 Version이 존재해야 하지만 존재하지 않음.")
    @DisplayName("Read Committed level에서 Optimistic Lock 사용해 Lost Update 현상 방지")
    @Test
    void readCommittedOptimisticLockWithoutLostUpdate() throws Exception {
        // Arrange
        final Long storeId = getStoreId();

        CountDownLatch updateLatch1 = new CountDownLatch(1);
        CountDownLatch updateLatch2 = new CountDownLatch(1);

        // Act
        CompletableFuture<Void> future1 = CompletableFuture.runAsync(() -> {
            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            assertThatThrownBy(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        final Store store = storeRepository.findById(storeId)
                                .orElseThrow();
                        store.updateStatus(PENDING);

                        updateLatch1.countDown();

                        try {
                            updateLatch2.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        storeRepository.saveAndFlush(store);
                    })
            ).isInstanceOf(ObjectOptimisticLockingFailureException.class);
        });

        CompletableFuture<Void> future2 = CompletableFuture.runAsync(() -> {
            try {
                updateLatch1.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

            transactionTemplate.executeWithoutResult(status -> {
                final Store store = storeRepository.findById(storeId)
                        .orElseThrow();
                store.updateStatus(DISMISSED);
                storeRepository.saveAndFlush(store);
            });
            updateLatch2.countDown();
        });

        future1.get();
        future2.get();

        // Assert
        final Store result = storeRepository.findById(storeId)
                .orElseThrow();
        assertThat(result.getStoreStatus()).isEqualTo(DISMISSED);
    }

    private Long getStoreId() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);

        return transactionTemplate.execute(status -> {
            final Location location = createLocation();
            locationRepository.save(location);

            final StoreCategory category = createCategory();
            storeCategoryRepository.save(category);

            final Store store = createStore(
                    "Store1",
                    "Store Description1",
                    RESOLVED,
                    category,
                    location
            );
            storeRepository.save(store);

            return store.getId();
        });
    }

    private Store createStore(
            final String name,
            final String description,
            final StoreStatus status,
            final StoreCategory category,
            final Location location
    ) {
        return Store.builder()
                .name(name)
                .description(description)
                .storeStatus(status)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .snsUrl("https://instgram.com/test")
                .category(category)
                .location(location)
                .build();
    }

    private StoreCategory createCategory() {
        return StoreCategory.builder()
                .name("뷰티")
                .build();
    }

    private Location createLocation() {
        return Location.builder()
                .name("서울 송파구 올림픽로 300")
                .zonecode("05551")
                .sido("서울")
                .sigungu("송파구")
                .address("서울 송파구 올림픽로 300")
                .longitude(127.104302)
                .latitude(37.513713)
                .build();
    }
}
