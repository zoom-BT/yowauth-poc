package yowyob.comops.api.common.domain.model;

import java.util.List;

public record PageResult<T>(List<T> content, long totalElements, int page, int size, int totalPages) {
}
