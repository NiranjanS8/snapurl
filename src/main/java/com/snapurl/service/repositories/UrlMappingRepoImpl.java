package com.snapurl.service.repositories;

import com.snapurl.service.dtos.UrlMappingDTO;
import com.snapurl.service.dtos.UrlMappingPageDTO;
import com.snapurl.service.models.UrlMapping;
import com.snapurl.service.models.Users;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Order;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Repository;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

@Repository
public class UrlMappingRepoImpl implements UrlMappingRepoCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public UrlMappingPageDTO searchUserUrls(
            Users user,
            String query,
            String sortBy,
            String order,
            String cursor,
            int size,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Integer minClicks,
            Integer maxClicks,
            String status
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<UrlMapping> cq = cb.createQuery(UrlMapping.class);
        Root<UrlMapping> root = cq.from(UrlMapping.class);

        String normalizedSortBy = normalizeSortBy(sortBy);
        boolean ascending = "asc".equalsIgnoreCase(order);
        Expression<?> sortExpression = getSortExpression(cb, root, normalizedSortBy);
        CursorValue cursorValue = decodeCursor(cursor, normalizedSortBy);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("user"), user));

        if (query != null && !query.isBlank()) {
            String escaped = query.trim().toLowerCase(Locale.ROOT)
                    .replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
            String pattern = "%" + escaped + "%";
            predicates.add(cb.or(
                    cb.like(cb.lower(root.<String>get("shortUrl")), pattern, '\\'),
                    cb.like(cb.lower(root.<String>get("originalUrl")), pattern, '\\')
            ));
        }

        if (startDate != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.<LocalDateTime>get("createdAt"), startDate));
        }

        if (endDate != null) {
            predicates.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("createdAt"), endDate));
        }

        if (minClicks != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.<Integer>get("clickCount"), minClicks));
        }

        if (maxClicks != null) {
            predicates.add(cb.lessThanOrEqualTo(root.<Integer>get("clickCount"), maxClicks));
        }

        if (status != null && !status.isBlank()) {
            LocalDateTime now = LocalDateTime.now();
            if ("active".equalsIgnoreCase(status)) {
                predicates.add(cb.or(
                        cb.isNull(root.get("expiresAt")),
                        cb.greaterThan(root.<LocalDateTime>get("expiresAt"), now)
                ));
            } else if ("expired".equalsIgnoreCase(status)) {
                predicates.add(cb.isNotNull(root.get("expiresAt")));
                predicates.add(cb.lessThanOrEqualTo(root.<LocalDateTime>get("expiresAt"), now));
            }
        }

        if (cursorValue != null) {
            predicates.add(buildCursorPredicate(cb, root, sortExpression, normalizedSortBy, ascending, cursorValue));
        }

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(buildOrders(cb, root, sortExpression, ascending));

        TypedQuery<UrlMapping> typedQuery = entityManager.createQuery(cq);
        typedQuery.setMaxResults(size + 1);

        List<UrlMapping> results = typedQuery.getResultList();
        boolean hasNext = results.size() > size;
        List<UrlMapping> pageItems = hasNext ? results.subList(0, size) : results;

        String nextCursor = null;
        if (hasNext && !pageItems.isEmpty()) {
            UrlMapping lastItem = pageItems.get(pageItems.size() - 1);
            nextCursor = encodeCursor(lastItem, normalizedSortBy);
        }

        List<UrlMappingDTO> items = pageItems.stream().map(this::toDto).toList();
        return new UrlMappingPageDTO(items, nextCursor, hasNext);
    }

    private Predicate buildCursorPredicate(
            CriteriaBuilder cb,
            Root<UrlMapping> root,
            Expression<?> sortExpression,
            String sortBy,
            boolean ascending,
            CursorValue cursorValue
    ) {
        Predicate primaryComparison;
        Predicate tieBreaker;

        if ("clicks".equals(sortBy)) {
            Expression<Integer> expression = root.<Integer>get("clickCount");
            Integer cursorSortValue = Integer.parseInt(cursorValue.getSortValue());
            primaryComparison = ascending
                    ? cb.greaterThan(expression, cursorSortValue)
                    : cb.lessThan(expression, cursorSortValue);
            tieBreaker = ascending
                    ? cb.greaterThan(root.<Long>get("id"), cursorValue.getId())
                    : cb.lessThan(root.<Long>get("id"), cursorValue.getId());
            return cb.or(primaryComparison, cb.and(cb.equal(expression, cursorSortValue), tieBreaker));
        }

        Expression<LocalDateTime> expression = castDateExpression(sortExpression);
        LocalDateTime cursorSortValue = LocalDateTime.parse(cursorValue.getSortValue());
        primaryComparison = ascending
                ? cb.greaterThan(expression, cursorSortValue)
                : cb.lessThan(expression, cursorSortValue);
        tieBreaker = ascending
                ? cb.greaterThan(root.<Long>get("id"), cursorValue.getId())
                : cb.lessThan(root.<Long>get("id"), cursorValue.getId());

        return cb.or(primaryComparison, cb.and(cb.equal(expression, cursorSortValue), tieBreaker));
    }

    @SuppressWarnings("unchecked")
    private Expression<LocalDateTime> castDateExpression(Expression<?> expression) {
        return (Expression<LocalDateTime>) expression;
    }

    private List<Order> buildOrders(CriteriaBuilder cb, Root<UrlMapping> root, Expression<?> sortExpression, boolean ascending) {
        Path<Long> idPath = root.get("id");
        List<Order> orders = new ArrayList<>();
        orders.add(ascending ? cb.asc(sortExpression) : cb.desc(sortExpression));
        orders.add(ascending ? cb.asc(idPath) : cb.desc(idPath));
        return orders;
    }

    private Expression<?> getSortExpression(CriteriaBuilder cb, Root<UrlMapping> root, String sortBy) {
        return switch (sortBy) {
            case "clicks" -> root.<Integer>get("clickCount");
            case "lastAccessed" -> cb.<LocalDateTime>coalesce()
                    .value(root.<LocalDateTime>get("lastAccessed"))
                    .value(root.<LocalDateTime>get("createdAt"));
            default -> root.<LocalDateTime>get("createdAt");
        };
    }

    private String normalizeSortBy(String sortBy) {
        if ("clicks".equalsIgnoreCase(sortBy)) {
            return "clicks";
        }
        if ("lastAccessed".equalsIgnoreCase(sortBy)) {
            return "lastAccessed";
        }
        return "createdAt";
    }

    private UrlMappingDTO toDto(UrlMapping urlMapping) {
        UrlMappingDTO dto = new UrlMappingDTO();
        dto.setId(urlMapping.getId());
        dto.setOriginalUrl(urlMapping.getOriginalUrl());
        dto.setShortUrl(urlMapping.getShortUrl());
        dto.setClickCount(urlMapping.getClickCount());
        dto.setCreatedAt(urlMapping.getCreatedAt());
        dto.setLastAccessed(urlMapping.getLastAccessed());
        dto.setExpiresAt(urlMapping.getExpiresAt());
        dto.setStatus(isExpired(urlMapping) ? "expired" : "active");
        dto.setUsername(urlMapping.getUser() != null ? urlMapping.getUser().getUsername() : null);
        return dto;
    }

    private boolean isExpired(UrlMapping urlMapping) {
        return urlMapping.getExpiresAt() != null && !urlMapping.getExpiresAt().isAfter(LocalDateTime.now());
    }

    private String encodeCursor(UrlMapping urlMapping, String sortBy) {
        String sortValue = switch (sortBy) {
            case "clicks" -> String.valueOf(urlMapping.getClickCount());
            case "lastAccessed" -> (urlMapping.getLastAccessed() != null ? urlMapping.getLastAccessed() : urlMapping.getCreatedAt()).toString();
            default -> urlMapping.getCreatedAt().toString();
        };

        String rawCursor = sortValue + "|" + urlMapping.getId();
        return Base64.getUrlEncoder().encodeToString(rawCursor.getBytes(StandardCharsets.UTF_8));
    }

    private CursorValue decodeCursor(String cursor, String sortBy) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }

        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] parts = decoded.split("\\|", 2);
            if (parts.length != 2) {
                return null;
            }

            if ("clicks".equals(sortBy)) {
                Integer.parseInt(parts[0]);
            } else {
                LocalDateTime.parse(parts[0]);
            }

            return new CursorValue(parts[0], Long.parseLong(parts[1]));
        } catch (IllegalArgumentException | DateTimeParseException ex) {
            return null;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class CursorValue {
        private String sortValue;
        private Long id;
    }
}
