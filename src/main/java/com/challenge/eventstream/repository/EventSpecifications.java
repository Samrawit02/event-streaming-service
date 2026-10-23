package com.challenge.eventstream.repository;

import com.challenge.eventstream.dto.EventSearchCriteria;
import com.challenge.eventstream.model.Event;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class EventSpecifications {

    public static Specification<Event> withCriteria(EventSearchCriteria criteria) {
        return (root, query, criteriaBuilder) -> {
            if (criteria == null) {
                return criteriaBuilder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            if (criteria.getUserId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("userId"), criteria.getUserId()));
            }

            if (criteria.getEventType() != null && !criteria.getEventType().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.upper(root.get("eventType")),
                        criteria.getEventType().toUpperCase()
                ));
            }

            if (criteria.getCategory() != null && !criteria.getCategory().isBlank()) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("category")),
                        criteria.getCategory().toLowerCase()
                ));
            }

            if (criteria.getProductId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("productId"), criteria.getProductId()));
            }

            if (criteria.getFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), criteria.getFrom()));
            }

            if (criteria.getTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), criteria.getTo()));
            }

            query.orderBy(criteriaBuilder.desc(root.get("timestamp")));

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
