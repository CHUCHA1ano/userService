package innowise.spring.userService.model.repository;

import innowise.spring.userService.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID>, JpaSpecificationExecutor<User> {

    //named (derived) methods
    Optional<User> findByEmail(String email);

    List<User> findAllByActiveTrue();

    boolean existsByEmail(String email);

    //JPQL
    @Query("select u from User u left join fetch u.cards where u.id = :id")
    Optional<User> findByIdWithCards(UUID id);

    @Query("select u from User u where lower(u.name) like lower(concat('%', :name, '%'))"
            + " and lower(u.surname) like lower(concat('%', :surname, '%'))")
    List<User> searchByNameAndSurname(String name, String surname);

    //native SQL
    @Query(value = "select * from users where active = true order by created_at desc limit :limit",
            nativeQuery = true)
    List<User> findRecentActiveUsers(int limit);
}
