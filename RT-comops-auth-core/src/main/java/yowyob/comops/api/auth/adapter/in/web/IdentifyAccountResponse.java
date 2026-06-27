package yowyob.comops.api.auth.adapter.in.web;

import yowyob.comops.api.auth.application.port.in.IdentifyAccountResult;

public record IdentifyAccountResponse(
        String principal,
        boolean accountExists,
        String nextStep,
        long matchingAccountCount) {

    public static IdentifyAccountResponse from(IdentifyAccountResult result) {
        return new IdentifyAccountResponse(
                result.principal(),
                result.accountExists(),
                result.nextStep(),
                result.matchingAccountCount());
    }
}
