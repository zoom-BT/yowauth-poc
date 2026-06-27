package yowyob.comops.api.kernel.domain;

import yowyob.comops.api.common.domain.DomainException;

public class MissingTenantException extends DomainException {

    public MissingTenantException() {
        super("A tenant identifier is required for this request.");
    }
}
