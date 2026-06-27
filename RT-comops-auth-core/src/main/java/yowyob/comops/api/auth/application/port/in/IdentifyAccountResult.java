package yowyob.comops.api.auth.application.port.in;

public record IdentifyAccountResult(
        String principal,
        boolean accountExists,
        String nextStep,
        long matchingAccountCount) {
}
