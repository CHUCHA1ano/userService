package innowise.spring.userService.model.repository.spec;

import innowise.spring.userService.model.entity.User;
import org.springframework.data.jpa.domain.Specification;

public final class UserSpecifications {

    private UserSpecifications() {
    }

    public static Specification<User> nameContains(String name) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<User> surnameContains(String surname) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("surname")), "%" + surname.toLowerCase() + "%");
    }

    public static Specification<User> isActive(Boolean active) {
        return (root, query, cb) -> cb.equal(root.get("active"), active);
    }
}
