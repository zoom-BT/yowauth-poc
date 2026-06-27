package yowyob.comops.api.kernel.adapter.in.web;

public record UpdateClientApplicationRequest(
        String name,
        String description,
        String planCode,
        java.util.List<String> allowedServices) {
}
