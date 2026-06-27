package yowyob.comops.api.kernel.application.service;

public final class RequestContextKeys {

    public static final String TENANT_CONTEXT = RequestContextKeys.class.getName() + ".tenantContext";
    public static final String REQUEST_CORRELATION = RequestContextKeys.class.getName() + ".requestCorrelation";

    private RequestContextKeys() {
    }
}
